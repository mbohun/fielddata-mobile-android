package au.org.ala.fielddata.mobile.validation;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;
import au.org.ala.fielddata.mobile.model.Attribute;
import au.org.ala.fielddata.mobile.model.Species;
import au.org.ala.fielddata.mobile.model.SurveyViewModel;
import au.org.ala.fielddata.mobile.ui.SpeciesListAdapter;
import au.org.ala.fielddata.mobile.ui.SpeciesViewHolder;

public class SpeciesBinder extends AbsBinder implements OnClickListener {

	private Context ctx;
	private SurveyViewModel model;
	private SpeciesViewHolder viewHolder;

	public SpeciesBinder(Context viewCtx, Attribute attribute, View speciesView, SurveyViewModel model) {
		super(attribute, speciesView);
		this.ctx = viewCtx;
		this.model = model;
		viewHolder = new SpeciesViewHolder(speciesView); 
		speciesView.setOnClickListener(this);

		Species species = model.getSelectedSpecies();
		if (species != null) {
			viewHolder.populate(species);
		}

	}

	public void onAttributeChange(Attribute attribute) {
		// TODO Auto-generated method stub

	}

	public void bind() {
		// TODO Auto-generated method stub

	}

	public void onClick(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		final SpeciesListAdapter adapter = new SpeciesListAdapter(ctx, model.getSurvey().getId());
		builder.setAdapter(adapter, new android.content.DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				Species selected = adapter.getItem(which);
				model.speciesSelected(selected);
				viewHolder.populate(selected);
			}
		});
		builder.setTitle("Select a species");
		builder.show();

	}

}
