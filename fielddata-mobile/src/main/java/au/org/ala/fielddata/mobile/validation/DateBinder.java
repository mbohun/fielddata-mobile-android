package au.org.ala.fielddata.mobile.validation;

import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;

public class DateBinder extends AbsBinder implements OnClickListener, DatePickerDialog.OnDateSetListener {

	private SurveyViewModel model;
	private DateFieldHolder holder;
	private Context ctx;
	private Date date;
	
	static class DateFieldHolder {
		public Button dateButton;
		public DateFieldHolder(View container) {
			dateButton = (Button)container.findViewById(R.id.dateDisplay);
		}
	}
	
	public DateBinder(Context ctx, View view, Attribute attribute, SurveyViewModel model) {
		super(attribute, view);
		
		this.ctx = ctx;
		this.attribute = attribute;
		this.model = model;
		
		holder = new DateFieldHolder(view);
		holder.dateButton.setOnClickListener(this);
		
		date = model.getRecord().getDate(attribute);
		updateDisplay();
	}
	
	private void updateDisplay() {
		if (date != null) {
			holder.dateButton.setText(DateFormat.getMediumDateFormat(ctx).format(date));
		}
		else {
			holder.dateButton.setText("");
		}
	}
	
	public void onClick(View v) {
		final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
		new DatePickerDialog(ctx, this, year, month, day).show();
	}
	
	public void onDateSet(DatePicker view, int year, int month, int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day);
		
		date = calendar.getTime();
		bind();
	}
	
	public void onAttributeChange(Attribute attribute) {
		if (attribute.getServerId() != this.attribute.getServerId()) {
			return;
		}
		date = model.getRecord().getDate(attribute);
		updateDisplay();
	}

	public void bind() {
		
		model.setValue(attribute, date);
	}
}
