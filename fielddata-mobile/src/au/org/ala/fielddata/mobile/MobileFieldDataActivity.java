/*******************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *  
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *  
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ******************************************************************************/
package au.org.ala.fielddata.mobile;

import android.app.AlertDialog;
import au.org.ala.fielddata.mobile.nrmplus.R;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Base class for the activities that make up the FieldData mobile client.
 * It provides "settings" and "about" menu items to the activity.
 */
public abstract class MobileFieldDataActivity extends SherlockActivity {

	public MobileFieldDataActivity() {
		super();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuInflater menuInflater = new MenuInflater(this);
		menuInflater.inflate(getMenuId(), menu);
		menuInflater.inflate(R.menu.common_menu_items, menu);
			
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:
			
			new AlertDialog.Builder(this).setMessage(R.string.aboutMessage).setPositiveButton(R.string.ok, null).create().show();
			return true;
		
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * @return the resource id of the menu for this activity.  The menu
	 * must contain an item with id R.id.about.
	 */
	protected abstract int getMenuId();
}
