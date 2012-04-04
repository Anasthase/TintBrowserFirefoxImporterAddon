/*
 * Firefox Bookmarks Importer for Tint Browser
 * 
 * Copyright (C) 2012 - to infinity and beyond J. Devauchelle and contributors.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package org.tint.firefoximporter.ui.preferences;

import org.tint.firefoximporter.Constants;
import org.tint.firefoximporter.R;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.AttributeSet;

public class ServerSpinnerPreference extends BaseSpinnerPreference {

	public ServerSpinnerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected int getTitleArray() {
		return R.array.ServerTitles;
	}

	@Override
	protected void setEditInputType() {
		mEditText.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
	}

	@Override
	protected void setSpinnerValueFromPreferences() {
		String currentServer = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(
				Constants.PREFERENCE_SERVER,
						Constants.DEFAULT_SERVER);
		
		if (Constants.DEFAULT_SERVER.equals(currentServer)) {
			mSpinner.setSelection(0);
			mEditText.setEnabled(false);
			mEditText.setText(Constants.DEFAULT_SERVER);
		} else {
			mSpinner.setSelection(1);
			mEditText.setEnabled(true);
			mEditText.setText(currentServer);					
		}
	}

	@Override
	protected void onSpinnerItemSelected(int position) {
		switch(position) {
		case 0:
			mEditText.setText(Constants.DEFAULT_SERVER);
			mEditText.setEnabled(false);
			break;
		case 1:
			mEditText.setEnabled(true);
			
			if (Constants.DEFAULT_SERVER.equals(mEditText.getText().toString())) {
				mEditText.setText(null);
			}
			
			mEditText.selectAll();
			showKeyboard();
			
			break;
		default:
			mEditText.setText(Constants.DEFAULT_SERVER);
			mEditText.setEnabled(false);
			break;
		}		
	}

}
