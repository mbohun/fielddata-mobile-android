package au.org.ala.fielddata.mobile;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import au.org.ala.fielddata.mobile.dao.SpeciesDAO;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.pref.Preferences;

import com.actionbarsherlock.app.SherlockActivity;

public class FieldGuideActivity extends SherlockActivity {

	private ProgressDialog pd;
	private Species selectedSpecies;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_guide);
        
        Intent i = getIntent();
		int speciesId = i.getIntExtra(CollectSurveyData.SPECIES, 0);
		if (speciesId > 0) {
			SpeciesDAO speciesDao = new SpeciesDAO(this);
			selectedSpecies = speciesDao.load(Species.class, speciesId);
			showFieldGuide(selectedSpecies);
		}
    }

    private void showFieldGuide(final Species species) {
		
		pd = ProgressDialog.show(FieldGuideActivity.this, "", 
				"Loading Field Guide", true, false, null);
		
		new AsyncTask<Void, Void, Void>() {

			String fieldDataUrl = new Preferences(FieldGuideActivity.this).getFieldDataServerUrl() +
					"/survey/fieldguide/"+species.server_id;
			
			StringBuffer contentBuffer = new StringBuffer(
					"<html><body><link rel=\"stylesheet\" href=\"/bdrs-core/css2.1.2/bdrs/bdrs.css\" type=\"text/css\"/>");
			
			IOException ioException;

			protected Void doInBackground(Void... params) {

				try {
					Connection con = Jsoup.connect(fieldDataUrl);
					Document doc = con.timeout(100000).get();
					Elements forms = doc.select("form");
					forms.remove();
					
					Elements contentDiv = doc.select("#content");
					
					contentBuffer.append(contentDiv.html());

				} catch (IOException e) {
					ioException = e;
					contentBuffer.append("Network error loading field guide, please try to reload.");
				}
				contentBuffer.append("</body></html>");
				
				return null;
			}

			protected void onPostExecute(Void result) {

				if (ioException != null) {
					Log.e(FieldGuideActivity.class.getCanonicalName(),
							ioException.getMessage(), ioException);
				}

				WebView webview = new WebView(FieldGuideActivity.this);
				setContentView(webview);
				webview.loadDataWithBaseURL(fieldDataUrl, contentBuffer.toString(), "text/html", "UTF-8", null);

				pd.dismiss();

			}
		}.execute();
			
	}
    
	/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_field_guide, menu);
        return true;
    }*/
}
