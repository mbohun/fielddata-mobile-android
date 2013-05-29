package au.org.ala.fielddata.mobile.validation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import au.org.ala.fielddata.mobile.CollectSurveyData;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;
import au.org.ala.fielddata.mobile.service.StorageManager;

public class ImageBinder extends AbsBinder {

	private ImageView thumb;
	private CollectSurveyData ctx;
	private Uri thumbUri;
	private SurveyViewModel model;
	private boolean thumbnailRendered;

	public ImageBinder(CollectSurveyData ctx, Attribute attribute, View imageView) {
		super(attribute, imageView);
		this.ctx = ctx;
		thumb = (ImageView) imageView.findViewById(R.id.photoThumbnail);
		model = ctx.getViewModel();
		thumbUri = model.getRecord().getUri(attribute);

		updateThumbnail();

		addEventHandlers(imageView);
	}

	private void addEventHandlers(View view) {
		ImageButton takePhoto = (ImageButton) view.findViewById(R.id.takePhoto);

		takePhoto.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				ctx.takePhoto(attribute);
			}
		});

		ImageButton selectFromGallery = (ImageButton) view.findViewById(R.id.selectFromGallery);
		selectFromGallery.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				ctx.selectFromGallery(attribute);

			}
		});

		thumb.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (thumbUri == null) {
					return;
				}
				
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(thumbUri, "image/*");
				ctx.startActivity(intent);
			}
		});

		thumb.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

			public void onGlobalLayout() {
				if (!thumbnailRendered) {
					updateThumbnail();
				}
			}
		});

	}

	private void updateThumbnail() {
		if (thumbUri != null) {

			view.findViewById(R.id.noPhotoText).setVisibility(View.GONE);
			view.findViewById(R.id.photoThumbnail).setVisibility(View.VISIBLE);
			try {

				// Get the dimensions of the View
				int targetW = thumb.getWidth();
				int targetH = thumb.getHeight();

				// The view is created when the previous page is displayed
				// so the imageview size is 0 at that point.
				if (targetW == 0 || targetH == 0) {
					thumbnailRendered = false;
					return;
				}
				Bitmap bitmap = new StorageManager(ctx).bitmapFromUri(thumbUri, targetW, targetH);
				thumb.setImageBitmap(bitmap);
				thumbnailRendered = true;

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		else {
			view.findViewById(R.id.noPhotoText).setVisibility(View.VISIBLE);
			view.findViewById(R.id.photoThumbnail).setVisibility(View.GONE);
			
		}
	}

	

	public void bind() {
		ctx.getViewModel().getRecord().setUri(attribute, thumbUri);
	}

	public void onAttributeChange(Attribute attribute) {
		if (attribute.getServerId() != this.attribute.getServerId()) {	
			return;
		}
		thumbUri = model.getRecord().getUri(attribute);
		
		updateThumbnail();
	}

}
