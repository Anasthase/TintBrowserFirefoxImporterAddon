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

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.emergent.android.weave.client.QueryParams;
import org.emergent.android.weave.client.QueryResult;
import org.emergent.android.weave.client.UserWeave;
import org.emergent.android.weave.client.WeaveAccountInfo;
import org.emergent.android.weave.client.WeaveBasicObject;
import org.emergent.android.weave.client.WeaveException;
import org.emergent.android.weave.client.WeaveFactory;

import org.json.JSONException;
import org.json.JSONObject;

import org.tint.firefoximporter.model.BookmarksWrapper;
import org.tint.firefoximporter.model.WeaveColumns;
import org.tint.firefoximporter.model.WeaveWrapper;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class SyncRunnable implements Runnable {
	
	private static final String WEAVE_PATH = "/storage/bookmarks";
	
	private static final String WEAVE_HEADER_TYPE = "type";
	
	private static final String WEAVE_VALUE_BOOKMARK = "bookmark";
	private static final String WEAVE_VALUE_FOLDER = "folder";
	private static final String WEAVE_VALUE_ITEM = "item";
	private static final String WEAVE_VALUE_ID = "id";
	private static final String WEAVE_VALUE_PARENT_ID = "parentid";
	private static final String WEAVE_VALUE_TITLE = "title";
	private static final String WEAVE_VALUE_URI = "bmkUri";
	private static final String WEAVE_VALUE_DELETED = "deleted";
	
	private static WeaveFactory mWeaveFactory = null;
	
	private Context mContext;
	private ContentResolver mContentResolver;
	private WeaveAccountInfo mAccountInfo;
	
	private ISyncListener mListener;
	
	private String mFolderName;
	private long mFolderId;
	
	private boolean mFullSync = false;
	
	private boolean mError;
	private String mErrorMessage;
	
	private ArrayList<ContentProviderOperation> mOperationsList;
	private Map<String, Integer> mFoldersMap;
	
	private static WeaveFactory getWeaveFactory() {
		if (mWeaveFactory == null) {
			mWeaveFactory = new WeaveFactory(true);
		}
		
		return mWeaveFactory;
	}
	
	public SyncRunnable(Context context, WeaveAccountInfo accountInfo, ISyncListener listener, String folderName) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mAccountInfo = accountInfo;
		mListener = listener;
		mFolderName = folderName;
	}

	@Override
	public void run() {
		
		mError = false;
		mErrorMessage = null;
		
		boolean syncDone = syncWeaveDb();
		if (syncDone) {
			writeToBookmarks();
		}
		
		onEnd(syncDone);
	}
	
	private void writeToBookmarks() {
		publishProgress(4, 0, 0);
		
		mFolderId = BookmarksWrapper.deleteFirefoxFolderContent(mContentResolver, mFolderName, false);
		if (mFolderId == -1) {
			mFolderId = BookmarksWrapper.createFirefoxFolder(mContentResolver, mFolderName);
		}
		
		mOperationsList = null;
		mFoldersMap = null;
		createFolderRecursive2("places", -1);
		
		Log.d("createFolderRecursive2", Integer.toString(mFoldersMap.size()));
		
		createBookmarks2();
		
		try {
			mContentResolver.applyBatch(BookmarksWrapper.AUTHORITY, mOperationsList);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		Map<String, Long> foldersMap = createFoldersRecursive("places", mFolderId, null);
//		
//		createBookmarks(foldersMap);
	}
	
	private void createBookmarks(Map<String, Long> foldersMap) {
		Cursor c = WeaveWrapper.getBookmarksOnly(mContentResolver);
		if (c != null) {
			if (c.moveToFirst()) {

				List<ContentValues> valuesList = new ArrayList<ContentValues>();
				
				int parentIdIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_PARENT_ID);
				int titleIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_TITLE);
				int urlIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_URL);

				do {
					
					String weaveParentId = c.getString(parentIdIndex);					
					
					if (foldersMap.containsKey(weaveParentId)) {
						long parentId = foldersMap.get(weaveParentId);
						
						String title = c.getString(titleIndex);
						String url = c.getString(urlIndex);

						ContentValues values = new ContentValues();
						values.put(BookmarksWrapper.Columns.TITLE, title);
						values.put(BookmarksWrapper.Columns.URL, url);
						values.put(BookmarksWrapper.Columns.BOOKMARK, 1);
						values.put(BookmarksWrapper.Columns.IS_FOLDER, 0);
						values.put(BookmarksWrapper.Columns.PARENT_FOLDER_ID, parentId);

						valuesList.add(values);					
					}

				} while (c.moveToNext());
				
				BookmarksWrapper.doBulkInsert(mContentResolver, valuesList.toArray(new ContentValues[valuesList.size()]));
			}
			
			c.close();
		}
	}
	
	private void createBookmarks2() {
		Cursor c = WeaveWrapper.getBookmarksOnly(mContentResolver);
		if (c != null) {
			if (c.moveToFirst()) {
				
				int parentIdIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_PARENT_ID);
				int titleIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_TITLE);
				int urlIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_URL);
				
				do {
					
					String weaveParentId = c.getString(parentIdIndex);
					
					if (mFoldersMap.containsKey(weaveParentId)) {
						int parentOperationIndex = mFoldersMap.get(weaveParentId);
						
						String title = c.getString(titleIndex);
						String url = c.getString(urlIndex);
						
						mOperationsList.add(ContentProviderOperation.newInsert(BookmarksWrapper.BOOKMARKS_URI)
								.withValue(BookmarksWrapper.Columns.TITLE, title)
								.withValue(BookmarksWrapper.Columns.URL, url)
								.withValue(BookmarksWrapper.Columns.BOOKMARK, 1)
								.withValue(BookmarksWrapper.Columns.IS_FOLDER, 0)
								.withValueBackReference(BookmarksWrapper.Columns.PARENT_FOLDER_ID, parentOperationIndex)
								.build());
					}
					
				} while (c.moveToNext());
			}
			
			c.close();
		}
	}
	
	private void createFolderRecursive2(String weaveParent, int parentOperationIndex) {
		
		if (mOperationsList == null) {
			mOperationsList = new ArrayList<ContentProviderOperation>();
		}
		
		if (mFoldersMap == null) {
			mFoldersMap = new HashMap<String, Integer>();
		}
		
		Log.d(weaveParent, Integer.toString(mFoldersMap.size()));
		
		Cursor c = WeaveWrapper.getFoldersByParent(mContentResolver, weaveParent);
		if (c != null) {
			if (c.moveToFirst()) {
				
				int weaveIdIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_ID);
				int titleIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_TITLE);
				
				do {
				
					String title = c.getString(titleIndex);
					
					if (parentOperationIndex == -1) { 
						mOperationsList.add(ContentProviderOperation.newInsert(BookmarksWrapper.BOOKMARKS_URI)
								.withValue(BookmarksWrapper.Columns.TITLE, title)
								.withValue(BookmarksWrapper.Columns.URL, null)
								.withValue(BookmarksWrapper.Columns.BOOKMARK, 0)
								.withValue(BookmarksWrapper.Columns.IS_FOLDER, 1)
								.withValue(BookmarksWrapper.Columns.PARENT_FOLDER_ID, mFolderId)
								.build());
					} else {
						mOperationsList.add(ContentProviderOperation.newInsert(BookmarksWrapper.BOOKMARKS_URI)
								.withValue(BookmarksWrapper.Columns.TITLE, title)
								.withValue(BookmarksWrapper.Columns.URL, null)
								.withValue(BookmarksWrapper.Columns.BOOKMARK, 0)
								.withValue(BookmarksWrapper.Columns.IS_FOLDER, 1)
								.withValueBackReference(BookmarksWrapper.Columns.PARENT_FOLDER_ID, parentOperationIndex)
								.build());
					}
					
					String weaveId = c.getString(weaveIdIndex);
					
					mFoldersMap.put(weaveId, mOperationsList.size() - 1);					
					
					createFolderRecursive2(weaveId, mOperationsList.size() - 1);
					
				} while (c.moveToNext());
			}
			
			c.close();
		}
	}
	
	private Map<String, Long> createFoldersRecursive(String weaveParent, long id, Map<String, Long> foldersMap) {
		
		if (foldersMap == null) {
			foldersMap = new HashMap<String, Long>();
		}
		
		Cursor c = WeaveWrapper.getFoldersByParent(mContentResolver, weaveParent);
		if (c != null) {
			if (c.moveToFirst()) {

				int weaveIdIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_ID);
				int titleIndex = c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_TITLE);

				do {

					String title = c.getString(titleIndex);

					ContentValues values = new ContentValues();
					values.put(BookmarksWrapper.Columns.TITLE, title);
					values.putNull(BookmarksWrapper.Columns.URL);
					values.put(BookmarksWrapper.Columns.BOOKMARK, 0);
					values.put(BookmarksWrapper.Columns.IS_FOLDER, 1);
					values.put(BookmarksWrapper.Columns.PARENT_FOLDER_ID, id);

					long insertedId = BookmarksWrapper.insert(mContentResolver, values);

					String weaveId = c.getString(weaveIdIndex);
					
					foldersMap.put(weaveId, insertedId);

					if (insertedId != -1) {						
						foldersMap = createFoldersRecursive(weaveId, insertedId, foldersMap);
					}

				} while (c.moveToNext());
			}			
			
			c.close();
		}
		
		return foldersMap;		
	}
	
	private boolean syncWeaveDb() {
		try {			
			publishProgress(0, 0, 0);
			
			UserWeave userWeave = getWeaveFactory().createUserWeave(mAccountInfo.getServer(), mAccountInfo.getUsername(), mAccountInfo.getPassword());						
			
			long lastModifiedDate = getLastModified(userWeave).getTime();			
			long lastSyncDate = PreferenceManager.getDefaultSharedPreferences(mContext).getLong(Constants.TECHNICAL_PREFERENCE_LAST_SYNC_DATE, -1);
			
			long folderId = BookmarksWrapper.getFolderId(mContentResolver, mFolderName);
			
//			Log.d("lastModifiedDate", Long.toString(lastModifiedDate));
//			Log.d("lastSyncDate", Long.toString(lastSyncDate));
			
			if ((lastModifiedDate > lastSyncDate) ||
					(folderId == -1)) {				
				
				mFullSync = (folderId == -1) || (lastSyncDate <= 0);
				
				QueryResult<List<WeaveBasicObject>> queryResult;
				
				QueryParams parms = null;
				if (!mFullSync) {
					parms = new QueryParams();
					parms.setFull(false);
					parms.setNewer(new Date(lastSyncDate));
				} else {
					WeaveWrapper.clear(mContentResolver);
				}
				
				queryResult = getCollection(userWeave, WEAVE_PATH, parms);
				List<WeaveBasicObject> wboList = queryResult.getValue();

				if (mFullSync) {
					doSync(mAccountInfo, userWeave, wboList);
				} else {
					doSyncByDelta(mAccountInfo, userWeave, wboList);
				}
				
				return true;
			} else {
				return false;
			}
			
		} catch (WeaveException e) {
			e.printStackTrace();
			mError = true;
			mErrorMessage = e.getMessage();
			return false;
		} catch (JSONException e) {
			e.printStackTrace();
			mError = true;
			mErrorMessage = e.getMessage();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			mError = true;
			mErrorMessage = e.getMessage();
			return false;
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
			mError = true;
			mErrorMessage = e.getMessage();
			return false;
		}
	}
	
	private void doSync(WeaveAccountInfo accountInfo, UserWeave userWeave, List<WeaveBasicObject> wboList)
			throws WeaveException, JSONException, IOException, GeneralSecurityException {

		int i = 0;
		int count = wboList.size();

		List<ContentValues> values = new ArrayList<ContentValues>();

		publishProgress(1, 0, 0);
		
		mContext.getContentResolver().delete(WeaveColumns.CONTENT_URI, null, null);

		for (WeaveBasicObject wbo : wboList) {
			JSONObject decryptedPayload = wbo.getEncryptedPayload(userWeave, accountInfo.getSecret());

			i++;

//			if (i > 50) break;
			//Log.d("Decrypted:", decryptedPayload.toString());

			if (decryptedPayload.has(WEAVE_HEADER_TYPE) &&
					((decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_BOOKMARK)) ||
							(decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_FOLDER)))) {

				if (decryptedPayload.has(WEAVE_VALUE_TITLE)) {					

					boolean isFolder = decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_FOLDER);

					String title = decryptedPayload.getString(WEAVE_VALUE_TITLE);    					
					String weaveId = decryptedPayload.has(WEAVE_VALUE_ID) ? decryptedPayload.getString(WEAVE_VALUE_ID) : null;
					String parentId = decryptedPayload.has(WEAVE_VALUE_PARENT_ID) ? decryptedPayload.getString(WEAVE_VALUE_PARENT_ID) : null;

					if ((title != null) && (title.length() > 0)) {

						ContentValues value = new ContentValues();
						value.put(WeaveColumns.WEAVE_BOOKMARKS_TITLE, title);
						value.put(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_ID, weaveId);
						value.put(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_PARENT_ID, parentId);

						if (isFolder) {
							value.put(WeaveColumns.WEAVE_BOOKMARKS_FOLDER, true);
						} else {
							String url = decryptedPayload.getString(WEAVE_VALUE_URI);

							value.put(WeaveColumns.WEAVE_BOOKMARKS_FOLDER, false);
							value.put(WeaveColumns.WEAVE_BOOKMARKS_URL, url);
						}

						values.add(value);    						
					}
				}
			}

			publishProgress(2, i, count);
		}

		int j = 0;
		ContentValues[] valuesArray = new ContentValues[values.size()];
		for (ContentValues value : values) {
			valuesArray[j++] = value;
		}

		publishProgress(3, 0, 0);
		mContext.getContentResolver().bulkInsert(WeaveColumns.CONTENT_URI, valuesArray);
	}
		
	private void doSyncByDelta(WeaveAccountInfo accountInfo, UserWeave userWeave, List<WeaveBasicObject> wboList)
			throws WeaveException, JSONException, IOException, GeneralSecurityException {

		int i = 0;
		int count = wboList.size();

		for (WeaveBasicObject wbo : wboList) {
			JSONObject decryptedPayload = wbo.getEncryptedPayload(userWeave, accountInfo.getSecret());

			i++;						

			if (decryptedPayload.has(WEAVE_HEADER_TYPE)) {

				if (decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_ITEM) &&
						decryptedPayload.has(WEAVE_VALUE_DELETED) &&
						decryptedPayload.getBoolean(WEAVE_VALUE_DELETED)) {

					String weaveId = decryptedPayload.has(WEAVE_VALUE_ID) ? decryptedPayload.getString(WEAVE_VALUE_ID) : null;
					if ((weaveId != null) &&
							(weaveId.length() > 0)) {

						WeaveWrapper.deleteByWeaveId(mContentResolver, weaveId);
					}
				} else if (decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_BOOKMARK) ||
						decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_FOLDER)) {

					String weaveId = decryptedPayload.has(WEAVE_VALUE_ID) ? decryptedPayload.getString(WEAVE_VALUE_ID) : null;
					if ((weaveId != null) &&
							(weaveId.length() > 0)) {

						boolean isFolder = decryptedPayload.getString(WEAVE_HEADER_TYPE).equals(WEAVE_VALUE_FOLDER);

						String title = decryptedPayload.getString(WEAVE_VALUE_TITLE);
						String parentId = decryptedPayload.has(WEAVE_VALUE_PARENT_ID) ? decryptedPayload.getString(WEAVE_VALUE_PARENT_ID) : null;

						ContentValues values = new ContentValues();
						values.put(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_ID, weaveId);
						values.put(WeaveColumns.WEAVE_BOOKMARKS_WEAVE_PARENT_ID, parentId);
						values.put(WeaveColumns.WEAVE_BOOKMARKS_TITLE, title);						

						if (isFolder) {
							values.put(WeaveColumns.WEAVE_BOOKMARKS_FOLDER, true);
						} else {
							String url = decryptedPayload.getString(WEAVE_VALUE_URI);

							values.put(WeaveColumns.WEAVE_BOOKMARKS_FOLDER, false);
							values.put(WeaveColumns.WEAVE_BOOKMARKS_URL, url);
						}

						long id = WeaveWrapper.getBookmarkIdByWeaveId(mContentResolver, weaveId);

						if (id == -1) {
							// Insert.
							WeaveWrapper.insertWeaveBookmark(mContentResolver, values);
						} else {
							// Update.
							WeaveWrapper.updateWeaveBookmark(mContentResolver, id, values);
						}						

					}
				}
			}

			//Log.d("Decrypted:", decryptedPayload.toString());

			publishProgress(2, i, count);
		}
	}
	
	private void publishProgress(Integer... values) {
		mListener.onSyncProgress(values[0], values[1], values[2]);
	}
	
	private void onEnd(boolean syncDone) {
		mListener.onSyncEnd(syncDone, mError, mErrorMessage);
	}
	
	private QueryResult<List<WeaveBasicObject>> getCollection(UserWeave weave, String name, QueryParams params) throws WeaveException {
		if (params == null)
			params = new QueryParams();
		URI uri = weave.buildSyncUriFromSubpath(name + params.toQueryString());
		return weave.getWboCollection(uri);
	}

	private Date getLastModified(UserWeave userWeave) throws WeaveException {
		try {
			JSONObject infoCol = userWeave.getNode(UserWeave.HashNode.INFO_COLLECTIONS).getValue();

			if (infoCol.has("bookmarks")) {
				long modLong = infoCol.getLong("bookmarks");
				return new Date(modLong * 1000);
			}

			return null;
		} catch (JSONException e) {
			throw new WeaveException(e);
		}
	}

}
