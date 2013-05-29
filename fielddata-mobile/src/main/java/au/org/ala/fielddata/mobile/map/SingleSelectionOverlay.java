package au.org.ala.fielddata.mobile.map;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import au.org.ala.fielddata.mobile.nrmplus.R;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.readystatesoftware.mapviewballoons.BalloonItemizedOverlay;
/**
 * Allows the selection of a single point and enables that point to be
 * touched and dragged to a different location on the map.
 */
public class SingleSelectionOverlay extends BalloonItemizedOverlay<OverlayItem> {

	private LocationListener listener;
	protected List<OverlayItem> items = new ArrayList<OverlayItem>();
	private Drawable marker = null;
	private OverlayItem inDrag = null;
	private ImageView dragImage = null;
	private int xDragImageOffset = 0;
	private int yDragImageOffset = 0;
	private int xDragTouchOffset = 0;
	private int yDragTouchOffset = 0;

	public SingleSelectionOverlay(MapView mapView, Drawable marker, ImageView dragImage, LocationListener listener) {
		super(marker, mapView);
		this.marker = marker;
		this.listener = listener;
		this.dragImage = dragImage;
		xDragImageOffset = dragImage.getDrawable().getIntrinsicWidth() / 2;
		yDragImageOffset = dragImage.getDrawable().getIntrinsicHeight();
		setBalloonBottomOffset(marker.getIntrinsicHeight());
		populate();
	}

	public void selectLocation(Location location) {
		selectLocation(locationToPoint(location));
		showHelp();
	}
	
	public void selectLocation(GeoPoint location) {
		if (items.size() > 0) {
			items.clear();
		}
		addItem(new OverlayItem(location, dragImage.getResources().getString(R.string.mapHelp), ""));
		listener.onLocationChanged(pointToLocation(location));
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		return (items.get(i));
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		super.draw(canvas, mapView, shadow);

		boundCenterBottom(marker);
	}

	@Override
	public int size() {
		return (items.size());
	}

	protected void addItem(OverlayItem item) {
		items.add(item);
		populate();
	}
	
	public GeoPoint getSelectedPoint() {
		return items.get(0).getPoint();
	}
	
	
	private void showHelp() {
		if (size() == 1) {
			onTap(0);
		}
	}
	
	private void hideHelp() {
		hideBalloon();
	}
	
	protected boolean onBalloonTap(int index, Item item) {
		hideHelp();
		return false;
	}
	
	@Override
	public boolean onTap(GeoPoint arg0, MapView arg1) {
		if (size() == 0) {
			String helpText = arg1.getResources().getString(R.string.mapHelp);
			updateSelection(new OverlayItem(arg0, helpText, ""));
			showHelp();
		}
		else {
			return super.onTap(arg0, arg1);
		}
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, MapView mapView) {
		final int action = event.getAction();
		final int x = (int) event.getX();
		final int y = (int) event.getY();
		boolean result = false;

		if (action == MotionEvent.ACTION_DOWN) {

			for (OverlayItem item : items) {
				Point p = new Point(0, 0);

				mapView.getProjection().toPixels(item.getPoint(), p);

				if (hitTest(item, marker, x - p.x, y - p.y)) {
					
					hideHelp();
					
					result = true;
					inDrag = item;
					items.remove(inDrag);
					populate();

					xDragTouchOffset = 0;
					yDragTouchOffset = 0;

					setDragImagePosition(p.x, p.y);
					dragImage.setVisibility(View.VISIBLE);

					xDragTouchOffset = x - p.x;
					yDragTouchOffset = y - p.y;

					break;
				}
			}
		} else if (action == MotionEvent.ACTION_MOVE && inDrag != null) {
			setDragImagePosition(x, y);
			result = true;
		} else if (action == MotionEvent.ACTION_UP && inDrag != null) {
			dragImage.setVisibility(View.GONE);

			GeoPoint pt = mapView.getProjection().fromPixels(
					x - xDragTouchOffset, y - yDragTouchOffset);
			OverlayItem toDrop = new OverlayItem(pt, inDrag.getTitle(),
					inDrag.getSnippet());

			updateSelection(toDrop);
			inDrag = null;
			result = true;
		}
		return (result || super.onTouchEvent(event, mapView));
	}
	
	private void updateSelection(OverlayItem point) {
		addItem(point);
		listener.onLocationChanged(pointToLocation(point.getPoint()));
		
	}
	
	private Location pointToLocation(GeoPoint point) {
		Location selectedLocation = new Location("On-screen map");
		selectedLocation.setTime(System.currentTimeMillis());
		selectedLocation.setLatitude(point.getLatitudeE6()/1000000d);
		selectedLocation.setLongitude(point.getLongitudeE6()/1000000d);
		
		return selectedLocation;
	}

	private GeoPoint locationToPoint(Location location) {
		int latitudeE6 = (int)Math.round(location.getLatitude()*1000000);
		int longitudeE6 = (int)Math.round(location.getLongitude()*1000000);
		
		GeoPoint point = new GeoPoint(latitudeE6, longitudeE6);
		return point;
	
	}
	
	private void setDragImagePosition(int x, int y) {
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dragImage
				.getLayoutParams();

		lp.setMargins(x - xDragImageOffset - xDragTouchOffset, y
				- yDragImageOffset - yDragTouchOffset, 0, 0);
		dragImage.setLayoutParams(lp);
	}

}
