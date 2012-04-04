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

package org.tint.firefoximporter.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

public class WeaveWrapper {
	
	public static void clear(ContentResolver contentResolver) {
		contentResolver.delete(WeaveColumns.CONTENT_URI, null, null);
	}
	
	public static void deleteByWeaveId(ContentResolver contentResolver, String weaveId) {
		String whereClause = WeaveColumns.WEAVE_BOOKMARKS_WEAVE_ID + " = \"" + weaveId + "\"";
		contentResolver.delete(WeaveColumns.CONTENT_URI, whereClause, null);
	}
	
	public static long getBookmarkIdByWeaveId(ContentResolver contentResolver, String weaveId) {
		long result = -1;
		String whereClause = WeaveColumns.WEAVE_BOOKMARKS_WEAVE_ID + " = \"" + weaveId + "\"";
		
		Cursor c = contentResolver.query(WeaveColumns.CONTENT_URI, null, whereClause, null, null);
		if (c != null) {
			if (c.moveToFirst()) {
				result = c.getLong(c.getColumnIndex(WeaveColumns.WEAVE_BOOKMARKS_ID));
			}
			
			c.close();
		}
		
		return result;
	}
	
	public static void insertWeaveBookmark(ContentResolver contentResolver, ContentValues values) {
		contentResolver.insert(WeaveColumns.CONTENT_URI, values);
	}
	
	public static void updateWeaveBookmark(ContentResolver contentResolver, long id, ContentValues values) {
		String whereClause = WeaveColumns.WEAVE_BOOKMARKS_ID + " = " + id;
		contentResolver.update(WeaveColumns.CONTENT_URI, values, whereClause, null);		
	}
	
	public static Cursor getFoldersByParent(ContentResolver contentResolver, String parentId) {
		String whereClause = WeaveColumns.WEAVE_BOOKMARKS_FOLDER + " = 1 AND " + WeaveColumns.WEAVE_BOOKMARKS_WEAVE_PARENT_ID + " = \"" + parentId + "\"";
		return contentResolver.query(WeaveColumns.CONTENT_URI, WeaveColumns.WEAVE_BOOKMARKS_PROJECTION, whereClause, null, null);
	}
	
	public static Cursor getContentByParent(ContentResolver contentResolver, String parentId) {
		String whereClause = WeaveColumns.WEAVE_BOOKMARKS_FOLDER + " = 0 AND " + WeaveColumns.WEAVE_BOOKMARKS_WEAVE_PARENT_ID + " = \"" + parentId + "\"";
		return contentResolver.query(WeaveColumns.CONTENT_URI, WeaveColumns.WEAVE_BOOKMARKS_PROJECTION, whereClause, null, null);
	}
	
	public static Cursor getBookmarksOnly(ContentResolver contentResolver) {
		String whereClause = WeaveColumns.WEAVE_BOOKMARKS_FOLDER + " = 0";
		return contentResolver.query(WeaveColumns.CONTENT_URI, WeaveColumns.WEAVE_BOOKMARKS_PROJECTION, whereClause, null, null);
	}

}
