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

package org.tint.firefoximporter.ui;

import org.tint.firefoximporter.Constants;
import org.tint.firefoximporter.R;
import org.tint.firefoximporter.model.BookmarksWrapper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class GeneralPreferencesFragment extends PreferenceFragment {
	
	private ProgressDialog mProgressDialog;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences_general_settings);
        
        Preference resetDataPref = findPreference(Constants.PREFERENCE_RESET_DATA);
        resetDataPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				resetData();
				return true;
			}
		});
	}
	
	private void resetData() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setCancelable(true);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		
		builder.setTitle(R.string.ResetDataDialogTitle);
		builder.setMessage(R.string.ResetDataDialogMessage);
		
		builder.setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int which) {
    			dialog.dismiss();
    			
    			mProgressDialog = ProgressDialog.show(
    					getActivity(),
    					getString(R.string.ResetDataProgressTitle),
    					getString(R.string.ResetDataProgressMessage));
    			
    			new Thread(new DataReseter()).start();
    		}
    	});
		
    	builder.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
    		@Override
    		public void onClick(DialogInterface dialog, int which) {
    			dialog.dismiss();
    		}
    	});
    	
    	AlertDialog alert = builder.create();
    	alert.show();
	}
	
	private class DataReseter implements Runnable {

		private Handler mHandler = new Handler() {
			public void handleMessage(Message msg) {
				mProgressDialog.dismiss();
			}
		};
		
		@Override
		public void run() {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
			
			Editor editor = prefs.edit();
			editor.putLong(Constants.TECHNICAL_PREFERENCE_LAST_SYNC_DATE, -1);
			editor.commit();
			
			BookmarksWrapper.deleteFirefoxFolder(
					getActivity().getContentResolver(),
					prefs.getString(Constants.PREFERENCE_FOLDER_NAME, Constants.DEFAULT_FOLDER_NAME));
			
			mHandler.sendEmptyMessage(0);
		}		
	}

}
