package au.org.ala.fielddata.mobile.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import au.org.ala.fielddata.mobile.nrmplus.R;

/**
 * The purpose of the NoDefaultSpinner is to present a spinner with 
 * an initial empty selection (with value null) but when the user 
 * selects an item, the empty item is unavailable for selection.
 * This is achieved by wrapping the supplied SpinnerAdapter twice,
 * once for the main spinner (which adds an empty / null item to the 
 * available selections) and once for the selection view.
 * The spinner drop down mode is not supported, choices will always be presented
 * in a dialog.
 */
public class NoDefaultSpinner extends Spinner implements DialogInterface.OnClickListener {
	private SpinnerAdapter originalAdpater;
	protected String hint;
	
	public NoDefaultSpinner(Context context) {
		super(context);
		init();
	}

	public NoDefaultSpinner(Context arg0, AttributeSet arg1) {
		super(arg0, arg1);
		init();
	}

	public NoDefaultSpinner(Context arg0, AttributeSet arg1, int arg2) {
		super(arg0, arg1, arg2);
		init();
	}
	
	private void init() {
		hint = getResources().getString(R.string.default_spinner_hint);
	}
	
	@Override
	public void setAdapter(SpinnerAdapter adapter) {
		originalAdpater = adapter;
		super.setAdapter(new BlankItemAdapter(adapter));
	}

	@Override
	public boolean performClick() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(hint);
		builder.setInverseBackgroundForced(true);
		builder.setSingleChoiceItems(new DelegatingAdatper(originalAdpater),
                getSelectedItemPosition()-1, this).show();
		return true;
	}
	
	public void onClick(DialogInterface dialog, int position) {
		setSelection(position+1);
		dialog.dismiss();
	}

	class DelegatingAdatper implements SpinnerAdapter, ListAdapter {
		protected SpinnerAdapter adapter;
		protected ListAdapter listAdapter;
       
        /**
         * <p>Creates a new ListAdapter wrapper for the specified adapter.</p>
         *
         * @param adapter the Adapter to transform into a ListAdapter
         */
        public DelegatingAdatper(SpinnerAdapter adapter) {
            
        	if (adapter == null) {
        		throw new IllegalArgumentException("The adapter parameter cannot be null");
        	}
        	
        	this.adapter = adapter;   
        	if (adapter instanceof ListAdapter) {
        		listAdapter = (ListAdapter)adapter;
        	}
        }

        public int getCount() {
            return adapter.getCount();
        }

        public Object getItem(int position) {
        	
            return adapter.getItem(position);
        }

        public long getItemId(int position) {
            return adapter.getItemId(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            return adapter.getDropDownView(position, convertView, parent);
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return adapter.getDropDownView(position, convertView, parent);
        }

        public boolean hasStableIds() {
            return adapter.hasStableIds();
        }

        public void registerDataSetObserver(DataSetObserver observer) {
            adapter.registerDataSetObserver(observer);
        }

        public void unregisterDataSetObserver(DataSetObserver observer) {
        	adapter.unregisterDataSetObserver(observer);
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true. 
         */
        public boolean areAllItemsEnabled() {
            if (listAdapter != null) {
                return listAdapter.areAllItemsEnabled();
            } else {
                return true;
            }
        }

        /**
         * If the wrapped SpinnerAdapter is also a ListAdapter, delegate this call.
         * Otherwise, return true.
         */
        public boolean isEnabled(int position) {
            if (listAdapter != null) {
                return listAdapter.isEnabled(position);
            } else {
                return true;
            }
        }

        public int getItemViewType(int position) {
            return adapter.getItemViewType(position);
        }

        public int getViewTypeCount() {
            return adapter.getViewTypeCount();
        }
        
        public boolean isEmpty() {
            return adapter.isEmpty();
        }
	}
	
	/**
	 * Wrapper class for this Spinner's SpinnerAdapter that adds an extra 
	 * empty item to the list.
	 */
	class BlankItemAdapter extends DelegatingAdatper {
		public BlankItemAdapter(SpinnerAdapter adapter) {
			super(adapter);
		}
		
		public int getCount() {
            return adapter.getCount()+1;
        }

        public Object getItem(int position) {
        	
            return position == 0 ? null : adapter.getItem(position-1);
        }

        public long getItemId(int position) {
            return position == 0 ? -1 : adapter.getItemId(position-1);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	if (position == 0) {
        		return createBlankView(convertView, parent);
        	}
        	
            return adapter.getView(position-1, convertView, parent);
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
            	return createBlankView(convertView, parent);
            }
        	return adapter.getDropDownView(position-1, convertView, parent);
        }
        
        private View createBlankView(View convertView, ViewGroup parent) {
        	if (convertView == null) {
	        	LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        	convertView = (TextView)inflater.inflate(R.layout.multiline_spinner_item, parent, false);
	        	((TextView)convertView).setHint(hint);
        	}
	        return convertView;
        }
        
        public int getItemViewType(int position) {
            return position == 0 ? 0 : adapter.getItemViewType(position-1)+1;
        }

        public int getViewTypeCount() {
            return adapter.getViewTypeCount()+1;
        }
        
        public boolean isEmpty() {
            return adapter.getCount() == 0;
        }

	}
}
