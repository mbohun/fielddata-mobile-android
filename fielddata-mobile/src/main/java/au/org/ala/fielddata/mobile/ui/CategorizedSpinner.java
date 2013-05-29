package au.org.ala.fielddata.mobile.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import au.org.ala.fielddata.mobile.nrmplus.R;
import au.org.ala.fielddata.mobile.model.Attribute.AttributeOption;

import com.commonsware.cwac.merge.MergeAdapter;

/**
 * Displays the spinner popup as a list containing dividers with a header
 * on them.
 */
public class CategorizedSpinner extends NoDefaultSpinner implements OnClickListener {

	private MergeAdapter listAdapter;
	private ArrayAdapter<String> spinnerAdpater;
	
	public CategorizedSpinner(Context context) {
		super(context);
	}

	public CategorizedSpinner(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
	}

	public CategorizedSpinner(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
	}
	
	@Override
	public boolean performClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		ListView list = new ListView(getContext());
		list.setAdapter(listAdapter);
		builder.setTitle(hint);
		builder.setInverseBackgroundForced(true);
		builder.setView(list);
		final Dialog dialog = builder.create();
		list.setOnItemClickListener(new OnItemClickListener() {
		
			
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				setSelection(position+1);
				performItemClick(view, position+1, listAdapter.getItemId(position));
				dialog.dismiss();
			}
		});
		dialog.show();
		return true;
	}

	public void setItems(AttributeOption[] items) {
		
		// We maintain two listAdapters, one with the actual values, used by the
		// spinner itself and one that is used to display the items with 
		// headings.  The adapter used by the spinner needs the header 
		// items to keep the item postitions in sync, however these will
		// never be displayed.
		listAdapter = new MergeAdapter();
		spinnerAdpater = new ArrayAdapter<String>(getContext(), R.layout.multiline_spinner_item);
		List<String> strings = new ArrayList<String>(items.length*2);
		String currentHeading = "";
		for (AttributeOption option : items) {
			
			String value = option.value == null ? "" : option.value;
			int hyphenPos = value.indexOf("-");
			
			String shortValue = value;
			if (hyphenPos >= 0) {
				String tmpHeading = value.substring(0, hyphenPos).trim();
				if (!tmpHeading.equals(currentHeading)) {
					currentHeading = tmpHeading;
					if (strings.size() > 0) {
						 addValues(strings);
						 strings = new ArrayList<String>(8);
					}
					listAdapter.addView(inflateHeader(currentHeading), false);
					spinnerAdpater.add(currentHeading);
				}
				if (hyphenPos < value.length()-1) {
					shortValue = value.substring(hyphenPos+1, value.length()).trim();
				}
				else {
					shortValue = "";
				}
			}
			
			strings.add(shortValue);
			spinnerAdpater.add(value);
			
		}
		if (strings.size() > 0) {
			addValues(strings);
			
		}
		
		setAdapter(spinnerAdpater);
		
	}

	private void addValues(List<String> strings) {
		ArrayAdapter<String> valueAdapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_dropdown_item, strings);
		listAdapter.addAdapter(valueAdapter);
		
	}
	
	private View inflateHeader(String headerText) {
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		TextView header = (TextView)inflater.inflate(R.layout.category_header_view, null);
		header.setText(headerText);
		return header;
	}
	
	
}
