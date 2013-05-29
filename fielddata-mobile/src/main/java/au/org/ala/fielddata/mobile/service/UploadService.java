package au.org.ala.fielddata.mobile.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import au.org.ala.fielddata.mobile.MobileFieldDataDashboard;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.Utils;
import au.org.ala.fielddata.mobile.dao.GenericDAO;
import au.org.ala.fielddata.mobile.dao.RecordDAO;
import au.org.ala.fielddata.mobile.model.Record;
import au.org.ala.fielddata.mobile.model.User;
import au.org.ala.fielddata.mobile.pref.Preferences;

/**
 * Uploads Records to the Field Data server.
 */
public class UploadService extends Service {

	public static final String UPLOADED = "Upload";
	public static final String UPLOAD_FAILED = "UploadFailed";
	public static final String STATUS_CHANGE = "StatusChange";
	
	public static final String RECORD_IDS_EXTRA = "RecordIds";
	
	private int SUCCESS = 0;
	private int FAILED_INVALID = 1;
	private int FAILED_SERVER = 2;

	private static final String START_ID = "startId";
	
	
	private UploadServiceHandler serviceHandler;
	private Looper serviceLooper;
	private UploadWakeup networkStatusReceiver;
	
	private List<Bundle> deferredWorkQueue;
		
	private final class UploadServiceHandler extends Handler {
		public UploadServiceHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			Bundle msgData = msg.getData();
			int[] recordIds = msgData.getIntArray(RECORD_IDS_EXTRA);
			RecordDAO dao = new RecordDAO(UploadService.this);
			
			dao.updateStatus(recordIds, Record.Status.SCHEDULED_FOR_UPLOAD);
			broadcastStatusChange(STATUS_CHANGE);
			
			if (canUpload()) {
				int startId = msgData.getInt(START_ID);
				uploadRecords(recordIds);
				stopSelf(startId);
			}
			else {
				if (Utils.DEBUG) {
					Log.i("UploadService", "Unable to upload, re-queuing message");
				}
				notifyQueued();
				synchronized(deferredWorkQueue) {
					deferredWorkQueue.add(msgData);
				}
			}
		}
		
		
	}
	
	class UploadWakeup extends BroadcastReceiver {
		
		public void onReceive(Context context, Intent intent) {
			synchronized(deferredWorkQueue) {
				for (int i=0; i<deferredWorkQueue.size(); i++) {
					Message msg = serviceHandler.obtainMessage();
					msg.setData(deferredWorkQueue.get(i));
					serviceHandler.sendMessage(msg);
				}
				deferredWorkQueue.clear();
			}
			
			
		}
	}
	
	@Override
	public Binder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		
		deferredWorkQueue = new ArrayList<Bundle>();
		
		HandlerThread thread = new HandlerThread("UploadThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		
		serviceLooper = thread.getLooper();
		serviceHandler = new UploadServiceHandler(serviceLooper);
		
		networkStatusReceiver = new UploadWakeup();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(networkStatusReceiver, filter);
		
		if (canUpload()) {
			Toast.makeText(UploadService.this, "Uploading record...", Toast.LENGTH_LONG).show();
			
		}
		else {
			Toast.makeText(UploadService.this, "Record(s) scheduled for upload when a network is available.", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onDestroy() {
		serviceLooper.quit();
		unregisterReceiver(networkStatusReceiver);
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int[] recordIds = intent.getIntArrayExtra(RECORD_IDS_EXTRA);
		
		if (Utils.DEBUG) {
			Log.i("UploadService", "Upload requested for "+ (recordIds == null ? "all" : recordIds.length) +" records, startId="+startId);
		}
		Bundle messageArgs = new Bundle();
		messageArgs.putInt(START_ID, startId);
		messageArgs.putIntArray(RECORD_IDS_EXTRA, recordIds);
		Message message = serviceHandler.obtainMessage();
		message.setData(messageArgs);
		serviceHandler.sendMessage(message);
		
		return START_REDELIVER_INTENT;
	}
	
	/**
	 * Uploads the records identified by the supplied array of ids.
	 * @param recordIds the ids of the records to upload.
	 * @return the action to broadcast after the upload is complete
	 * (SUCCESS, FAILED_INVALID or FAILED_SERVER).
	 */
	private void uploadRecords(int[] recordIds) {
		RecordDAO recordDao = new RecordDAO(this);
		List<Record> records = new ArrayList<Record>();
		if (recordIds == null) {
			records.addAll(recordDao.loadAll(Record.class));
		}
		else {
			for (int id : recordIds) {
				Record record = recordDao.loadIfExists(Record.class, id);
				if (record != null) {
					records.add(record);
				}
			}
		}
		List<Integer> success = new ArrayList<Integer>();
		List<Integer> failed = new ArrayList<Integer>();
		
		int invalidCount = 0;
		for (Record record : records) {
			int result = upload(record);
			if (result == SUCCESS) {
				success.add(record.getId());
				recordDao.delete(Record.class, record.getId());
			}
			else if (result == FAILED_INVALID) {
				invalidCount++;
			}
			else if (result == FAILED_SERVER) {
				failed.add(record.getId());
			}
		}
		String action = null;
		if (failed.size() > 0) {
			action = UPLOAD_FAILED;
			recordDao.updateStatus(failed, Record.Status.FAILED_TO_UPLOAD);
			notifyFailed(failed.size());
		}
		if (success.size() > 0){
			action = UPLOADED;
			notifiySuccess(success.size());
		}
		
		
		broadcastStatusChange(action);
	}
	
	private void broadcastStatusChange(String change) {
		Intent broadcastIntent = new Intent(change);
		LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
	}
	
	private boolean canUpload() {
		
		Preferences prefs = new Preferences(this);
		ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		
		boolean needsWifi = prefs.getUploadOverWifiOnly();
		
		NetworkInfo networkInfo;
		if (needsWifi) {
			networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		}
		else {
			networkInfo = connectivityManager.getActiveNetworkInfo();
		}
		return networkInfo != null && networkInfo.isConnected();
	}
	
	/**
	 * Uploads a single record to the server.
	 * @param record the Record to upload.
	 * @return true if the upload succeeded, false otherwise.
	 */
	private int upload(Record record) {
	
		int resultCode = SUCCESS;
		FieldDataServiceClient service = new FieldDataServiceClient(getApplicationContext());
		
		List<Record> tmp = new ArrayList<Record>();
		tmp.add(record);
		try {
			
			if (record.isValid()) {
				service.sync(tmp);
				resultCode = SUCCESS;
			}
			else {
				resultCode = FAILED_INVALID;
			}
		}
		catch (Exception e) {
			Log.e("UploadService", "Error calling the field data service: ", e);
			resultCode = FAILED_SERVER;
		}
		return resultCode;
	}
	
	private void notifiySuccess(int numSuceeded) {
		Preferences prefs = new Preferences(this);
		GenericDAO<User> userDao = new GenericDAO<User>(this);
		List<User> user = userDao.loadAll(User.class);
		
		String reviewUrl = String.format(prefs.getReviewUrl(), user.get(0).server_id);
		Uri records = Uri.parse(reviewUrl);
		Intent viewRecords = new Intent(Intent.ACTION_VIEW, records);
		PendingIntent intent = PendingIntent.getActivity(this, START_NOT_STICKY, viewRecords, PendingIntent.FLAG_CANCEL_CURRENT);
		
		String message = numSuceeded == 1 ? " record uploaded" : " records uploaded";
		
		notify(numSuceeded + message, reviewUrl, intent, true);
		
	}
	
	private void notifyFailed(int numFailed) {
		Intent savedActivity = new Intent(this, MobileFieldDataDashboard.class);
		savedActivity.putExtra(MobileFieldDataDashboard.SELECTED_TAB_BUNDLE_KEY, MobileFieldDataDashboard.RECORDS_TAB_INDEX);
		
		PendingIntent intent = PendingIntent.getActivity(this, START_NOT_STICKY, savedActivity, PendingIntent.FLAG_CANCEL_CURRENT);
		
		notify(numFailed + " records failed to upload", "Touch to view saved records", intent, false);
	}
	
	private void notifyQueued() {
		Intent savedActivity = new Intent(this, MobileFieldDataDashboard.class);
		savedActivity.putExtra(MobileFieldDataDashboard.SELECTED_TAB_BUNDLE_KEY, MobileFieldDataDashboard.RECORDS_TAB_INDEX);
		PendingIntent intent = PendingIntent.getActivity(this, START_NOT_STICKY, savedActivity, PendingIntent.FLAG_CANCEL_CURRENT);
		
		Preferences prefs = new Preferences(this);
		if (prefs.getUploadOverWifiOnly()) {
			notify("Upload pending - no WIFI network", "Records will be uploaded when a WIFI network service is available.", intent, true);
		}
		else {
			notify("Upload pending - no network", "Records will be uploaded when network service is available.", intent, true);
		}
		
	}
	
	private void notify(String title, String subject, PendingIntent intent, boolean success) {
		
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle(title)
		       .setContentText(subject)
		       .setTicker(title)
		       .setAutoCancel(true)
		       .setSmallIcon(R.drawable.ala_notification)
			  .setContentIntent(intent);
		
		Notification notification = builder.getNotification();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notificationManager.notify(success ? SUCCESS : FAILED_SERVER, notification);
		
	}
	
}
