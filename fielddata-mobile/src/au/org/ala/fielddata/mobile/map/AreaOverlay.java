package au.org.ala.fielddata.mobile.map;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.widget.ImageView;

import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class AreaOverlay extends SingleSelectionOverlay {

	

	public AreaOverlay(MapView mapView, Drawable marker, ImageView dragImage,
			LocationListener listener) {
		super(mapView, marker, dragImage, listener);
		
	}

	
	
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean arg2) {
		
		super.draw(canvas, mapView, arg2);
		Paint paint = new Paint();
		Point point = new Point();
		for (OverlayItem item : items) {
			point = mapView.getProjection().toPixels(item.getPoint(), point);
			canvas.drawLine(0,  0, point.x, point.y, paint);
		}
	}
}
	
	

