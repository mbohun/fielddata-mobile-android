package au.org.ala.fielddata.mobile;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;
import au.org.ala.fielddata.mobile.ui.MultiSpinner;
import au.org.ala.fielddata.mobile.validation.Binder;
import au.org.ala.fielddata.mobile.validation.DateBinder;
import au.org.ala.fielddata.mobile.validation.ImageBinder;
import au.org.ala.fielddata.mobile.validation.LocationBinder;
import au.org.ala.fielddata.mobile.validation.MultiSpinnerBinder;
import au.org.ala.fielddata.mobile.validation.SingleCheckboxBinder;
import au.org.ala.fielddata.mobile.validation.SpeciesBinder;
import au.org.ala.fielddata.mobile.validation.SpinnerBinder;
import au.org.ala.fielddata.mobile.validation.TextViewBinder;

/**
 * The BinderManager is responsible for creating and attaching an appropriate
 * binder that will be responsible for keeping the record in sync with the
 * data displayed in a widget.
 */
class BinderManager {
	private List<Binder> binders;

	private SurveyViewModel surveyViewModel;
	private CollectSurveyData ctx;

	public BinderManager(CollectSurveyData activity) {
		this.ctx = activity;
		binders = new ArrayList<Binder>();
		surveyViewModel = activity.getViewModel();
	}

	public Binder configureBindings(View view, Attribute attribute) {

		Binder binder = null;
		// Some attribute types require special bindings.
		switch (attribute.getType()) {
		case WHEN:
		case TIME:
			binder = new DateBinder(ctx, view, attribute, surveyViewModel);
			break;
		case POINT:
			binder = new LocationBinder(ctx, view, attribute, surveyViewModel);
			break;
		case IMAGE:
			binder = new ImageBinder(ctx, attribute, view);
			break;
		case SPECIES_P:
			binder = new SpeciesBinder(ctx, attribute, view, surveyViewModel);
			break;
		case SINGLE_CHECKBOX:
			binder = new SingleCheckboxBinder(ctx, (CheckBox) view, attribute, surveyViewModel);
			break;
		case MULTI_CHECKBOX:
			binder = new MultiSpinnerBinder(ctx, (MultiSpinner) view, attribute,
					surveyViewModel);
			break;
		default:
			binder = bindByViewClass(view, attribute);
			break;
		}

		add(attribute, binder);

		return binder;
	}

	private void add(Attribute attribute, Binder binder) {
		if (binder != null) {
			binders.add(binder);
			surveyViewModel.setAttributeChangeListener(binder, attribute);
		}
	}

	private Binder bindByViewClass(View view, Attribute attribute) {

		Binder binder = null;
		if (view instanceof TextView) {
			binder = new TextViewBinder(ctx, (TextView) view, attribute, surveyViewModel);

		} else if (view instanceof Spinner) {
			binder = new SpinnerBinder(ctx, (Spinner) view, attribute, surveyViewModel);
		}
		return binder;
	}

	public void bindAll() {
		for (Binder binder : binders) {
			binder.bind();
		}
	}

	public void clearBindings() {
		for (Binder binder : binders) {
			surveyViewModel.removeAttributeChangeListener(binder);
		}
		binders.clear();
	}

	public View getView(Attribute attribute) {
		for (Binder binder : binders) {
			if (binder.getAttribute().equals(attribute)) {
				return binder.getView();
			}
		}
		return null;
	}
}