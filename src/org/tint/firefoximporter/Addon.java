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

package org.tint.firefoximporter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.tint.addons.framework.Action;
import org.tint.addons.framework.AskUserConfirmationAction;
import org.tint.addons.framework.Callbacks;
import org.tint.addons.framework.ShowDialogAction;
import org.tint.firefoximporter.R;
import org.tint.firefoximporter.ui.ErrorDetailsActivity;
import org.tint.firefoximporter.ui.PreferencesActivity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class Addon extends BaseAddon implements ISyncListener {
	
	private int mNotificationId;
	private Notification mNotification;
	private NotificationManager mNotificationManager;
	
	private int mLastProgress;
	
	private SyncRunnable mSyncRunnable = null;

	public Addon(Service service) {
		super(service);
	}

	@Override
	public int getCallbacks() throws RemoteException {
		return Callbacks.CONTRIBUTE_HISTORY_BOOKMARKS_MENU | Callbacks.HAS_SETTINGS_PAGE;
	}

	@Override
	public String getContributedBookmarkContextMenuItem(String currentTabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContributedHistoryBookmarksMenuItem(String currentTabId) throws RemoteException {
		return mService.getString(R.string.MenuTitle);
	}

	@Override
	public String getContributedHistoryContextMenuItem(String currentTabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContributedLinkContextMenuItem(String currentTabId, int hitTestResult, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContributedMainMenuItem(String currentTabId, String currentTitle, String currentUrl) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onBind() throws RemoteException {
		mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public List<Action> onContributedBookmarkContextMenuItemSelected(String currentTabId, String title, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onContributedHistoryBookmarksMenuItemSelected(String currentTabId) throws RemoteException {
		List<Action> result = null;
		
		if (!areCredentialsSet()) {
			result = new ArrayList<Action>();
			result.add(new AskUserConfirmationAction(
					mService.getString(R.string.NoCredentialsTitle),
					mService.getString(R.string.NoCredentialsMessage),
					mService.getString(R.string.Yes),
					mService.getString(R.string.No)));
		} else {
			if (mSyncRunnable == null) {
				startSync();
			} else {
				result = new ArrayList<Action>();
				result.add(new ShowDialogAction(
						mService.getString(R.string.ImportInProgressTitle),
						mService.getString(R.string.ImportInProgressMessage)));
			}
		}
		
		return result;
	}

	@Override
	public List<Action> onContributedHistoryContextMenuItemSelected(String currentTabId, String title, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onContributedLinkContextMenuItemSelected(String currentTabId, int hitTestResult, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onContributedMainMenuItemSelected(String currentTabId, String currentTitle, String currentUrl) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onPageFinished(String tabId, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onPageStarted(String tabId, String url) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onTabClosed(String tabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Action> onTabOpened(String tabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onTabSwitched(String tabId) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onUnbind() throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Action> onUserConfirm(String currentTabId, String questionId, boolean positiveAnswer) throws RemoteException {
		if (positiveAnswer) {
			showAddonSettingsActivity();
		}
		return null;
	}
	
	@Override
	public List<Action> onUserInput(String currentTabId, String questionId, boolean cancelled, String userInput) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Action> onUserChoice(String currentTabId, String questionId, boolean cancelled, int userChoice) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void showAddonSettingsActivity() throws RemoteException {
		Intent i = new Intent(mService, PreferencesActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		mService.startActivity(i);
	}
	
	private boolean areCredentialsSet() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mService);
		
		if (TextUtils.isEmpty(prefs.getString(Constants.PREFERENCE_USERNAME, null))) {
			return false;
		}
		
		if (TextUtils.isEmpty(prefs.getString(Constants.PREFERENCE_PASSWORD, null))) {
			return false;
		}
		
		if (TextUtils.isEmpty(prefs.getString(Constants.PREFERENCE_KEY, null))) {
			return false;
		}
		
		return true;
	}
	
	private String getWeaveAuthToken(Context context) {
		String server = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCE_SERVER, Constants.DEFAULT_SERVER);
    	String userName = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCE_USERNAME, null);
    	String password = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCE_PASSWORD, null);
    	String key = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PREFERENCE_KEY, null);
    
    	return String.format(Constants.AUTH_TOKEN_SCHEME, key, password, userName, server);
    }

	private void startSync() {
		String authToken = getWeaveAuthToken(mService);
		WeaveAccountInfo info = WeaveAccountInfo.createWeaveAccountInfo(authToken);
		
		mSyncRunnable = new SyncRunnable(
				mService,
				info,
				this,
				PreferenceManager.getDefaultSharedPreferences(mService).getString(Constants.PREFERENCE_FOLDER_NAME, Constants.DEFAULT_FOLDER_NAME)); 
		
		new Thread(mSyncRunnable).start();		
	}

	@Override
	public void onSyncProgress(int step, int done, int total) {
		switch (step) {
		case 0:
			mLastProgress = -1;
			
			Random r = new Random();			
			mNotificationId = r.nextInt();
			
			mNotification = new Notification(R.drawable.ic_stat_sync, mService.getString(R.string.NotificationImportStartTickerText), System.currentTimeMillis());
			mNotification.flags |= Notification.FLAG_NO_CLEAR;
			
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					mService.getString(R.string.NotificationImportMessageCheckLastDate),
					null);
			
			mNotificationManager.notify(mNotificationId, mNotification);
			
			break;
			
		case 1:
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					mService.getString(R.string.NotificationImportMessageRemovingOldData),
					null);
			
			mNotificationManager.notify(mNotificationId, mNotification);
			
			break;
			
		case 2:
			int progress = (int) (((float) done / total) * 100);
			
			if (progress > mLastProgress) {
				mNotification.setLatestEventInfo(
						mService,
						mService.getString(R.string.NotificationImportTitle),
						String.format(mService.getString(R.string.NotificationImportMessageRetrievingData), progress),
						null);
				
				mNotificationManager.notify(mNotificationId, mNotification);
				
				mLastProgress = progress;
			}
			
			break;
			
		case 3:
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					mService.getString(R.string.NotificationImportMessageWritingData),
					null);
			
			mNotificationManager.notify(mNotificationId, mNotification);
			
			break;
			
		case 4:
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					mService.getString(R.string.NotificationImportMessageSyncData),
					null);
			
			mNotificationManager.notify(mNotificationId, mNotification);
			
			break;
			
		default: break;
		}
	}

	@Override
	public void onSyncEnd(boolean syncDone, boolean error, String errorMessage) {
		mSyncRunnable = null;
		
		mNotificationManager.cancel(mNotificationId);
		
		Random r = new Random();
		mNotificationId = r.nextInt();
		
		mNotification = new Notification(R.drawable.ic_stat_sync, mService.getString(R.string.NotificationImportEndTickerText), System.currentTimeMillis());
		mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		
		if (error) {
			
			Intent errorIntent = new Intent(mService, ErrorDetailsActivity.class);
			errorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			errorIntent.putExtra(ErrorDetailsActivity.EXTRA_ERROR_DETAILS, errorMessage);
			PendingIntent contentIntent = PendingIntent.getActivity(mService, 0, errorIntent, 0);
			
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportEndTitleKo),
					mService.getString(R.string.NotificationImportEndMessageKo),
					contentIntent);
		} else {
			String message;
			
			if (syncDone) {
				
				Editor editor = PreferenceManager.getDefaultSharedPreferences(mService).edit();
				editor.putLong(Constants.TECHNICAL_PREFERENCE_LAST_SYNC_DATE, new Date().getTime());
				editor.commit();
				
				message = mService.getString(R.string.NotificationImportEndMessageOk);
			} else {
				message = mService.getString(R.string.NotificationImportEndMessageOkNoImport);
			}
			
			mNotification.setLatestEventInfo(
					mService,
					mService.getString(R.string.NotificationImportTitle),
					message,
					null);
		}
		
		mNotificationManager.notify(mNotificationId, mNotification);		
	}
}
