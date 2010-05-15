/* 
 * $Id: RSSReaderProvider.java 114 2008-02-14 05:09:23Z jasta00 $
 *
 * Copyright (C) 2007 Josh Guilfoyle <jasta@devtcg.org>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 */

package org.devtcg.rssreader.provider;


import org.devtcg.rssreader.provider.RSSReaderProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.devtcg.rssreader.R;

import android.content.ContentProvider;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteQueryBuilder;
import android.content.Resources;
import android.database.ArrayListCursor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

public class RSSReaderProvider extends ContentProvider
{
	private SQLiteDatabase mDB;
	
	private static final String TAG = "RSSReaderProvider";
	private static final String DATABASE_NAME = "rss_reader.db";
	private static final int DATABASE_VERSION = 9;

	private static HashMap<String, String> CHANNEL_LIST_PROJECTION_MAP;
	private static HashMap<String, String> POST_LIST_PROJECTION_MAP;

	private static final int CHANNELS = 1;
	private static final int CHANNEL_ID = 2;
	private static final int POSTS = 3;
	private static final int POST_ID = 4;
	private static final int CHANNEL_POSTS = 5;
	private static final int CHANNELICON_ID = 6;

	private static final UriMatcher URL_MATCHER;

	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		protected void onCreateChannels(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE rssreader_channel (_id INTEGER PRIMARY KEY," +
	           "	title TEXT UNIQUE, url TEXT UNIQUE, " +
	           "    icon TEXT, icon_url TEXT, logo TEXT);");
		}

		protected void onCreatePosts(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE rssreader_post (_id INTEGER PRIMARY KEY," +
			           "    channel_id INTEGER, title TEXT, url TEXT, " + 
			           "    posted_on DATETIME, body TEXT, author TEXT, read INTEGER(1) DEFAULT '0');");

			/* TODO: Should we narrow this more to just URL _or_ title? */
			db.execSQL("CREATE UNIQUE INDEX unq_post ON rssreader_post (title, url);");

			/* Create an index to efficiently access posts on a particular channel. */
			db.execSQL("CREATE INDEX idx_channel ON rssreader_post (channel_id);");
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			onCreateChannels(db);
			onCreatePosts(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			assert(newVersion == DATABASE_VERSION);
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + "...");

			switch(oldVersion)
			{
			case 7:
				db.execSQL("ALTER TABLE rssreader_channel ADD COLUMN icon_url TEXT;");
				break;

			default:
				Log.w(TAG, "Version too old, wiping out database contents...");
				db.execSQL("DROP TABLE IF EXISTS rssreader_channel;");
				db.execSQL("DROP TABLE IF EXISTS rssreader_post;");
				onCreate(db);
				break;
			}
		}
	}
	
	@Override
	public boolean onCreate()
	{
		DatabaseHelper dbHelper = new DatabaseHelper();
		mDB = dbHelper.openDatabase(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
		
		return (mDB == null) ? false : true;
	}
	
	@Override
	public Cursor query(Uri url, String[] projection, String selection,
	  String[] selectionArgs, String sort)
	{
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		String defaultSort = null;
		
		switch(URL_MATCHER.match(url))
		{
		case CHANNELS:
			qb.setTables("rssreader_channel");
			qb.setProjectionMap(CHANNEL_LIST_PROJECTION_MAP);
			defaultSort = RSSReader.Channels.DEFAULT_SORT_ORDER;
			break;

		case CHANNEL_ID:
			qb.setTables("rssreader_channel");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;

			/*
		case POSTS:
			qb.setTables("rssreader_post");
			qb.setProjectionMap(POST_LIST_PROJECTION_MAP);
			defaultSort = RSSReader.Posts.DEFAULT_SORT_ORDER;
			break;
			*/
		case CHANNEL_POSTS:
			qb.setTables("rssreader_post");
			qb.appendWhere("channel_id=" + url.getPathSegments().get(1));
			qb.setProjectionMap(POST_LIST_PROJECTION_MAP);
			defaultSort = RSSReader.Posts.DEFAULT_SORT_ORDER;
			break;
			
		case POST_ID:
			qb.setTables("rssreader_post");
			qb.appendWhere("_id=" + url.getPathSegments().get(1));
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		String orderBy;

		if (TextUtils.isEmpty(sort))
			orderBy = defaultSort;
		else
			orderBy = sort;

		Cursor c = qb.query(mDB, projection, selection, selectionArgs,
				null, null, orderBy);

		c.setNotificationUri(getContext().getContentResolver(), url);
		
		return c;
	}

	@Override
	public String getType(Uri url)
	{
		switch(URL_MATCHER.match(url))
		{
		case CHANNELS:
			return "vnd.android.cursor.dir/vnd.rssreader.channel";
		case CHANNEL_ID:
			return "vnd.android.cursor.item/vnd.rssreader.channel";
		case CHANNELICON_ID:
			return "image/x-icon";
		case POSTS:
		case CHANNEL_POSTS:
			return "vnd.android.cursor.dir/vnd.rssreader.post";
		case POST_ID:
			return "vnd.android.cursor.item/vnd.rssreader.post";
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
	}
	
	private String getIconFilename(long channelId)
	{
		return "channel" + channelId + ".ico";
	}

	private String getIconPath(long channelId)
	{
		return getContext().getFileStreamPath(getIconFilename(channelId)).getAbsolutePath();
	}
	
	private void copyDefaultIcon(String path)
	  throws FileNotFoundException, IOException
	{
		FileOutputStream out = new FileOutputStream(path);

		InputStream ico =
		  getContext().getResources().openRawResource(R.drawable.feedicon);

		byte[] buf = new byte[1024];
		int n;

		while ((n = ico.read(buf)) != -1)
			out.write(buf, 0, n);
		
		ico.close();
		
		out.close();
	}
	
	public ParcelFileDescriptor openFile(Uri uri, String mode)
	  throws FileNotFoundException
	{
		switch(URL_MATCHER.match(uri))
		{
		case CHANNELICON_ID:
			long id = Long.valueOf(uri.getPathSegments().get(1));
			String path = getIconPath(id);

			/* XXX: This appears to be an Android bug: files created with
			 * ParcelFileDescriptor.MODE_CREATE have mode 0, which is not
			 * readable. */
			if (mode.equals("rw") == true)
			{
				FileOutputStream foo = getContext().openFileOutput(getIconFilename(id), 0);
				
				try { foo.write(new byte[] { 't' }); foo.close(); }
				catch (Exception e) { }
			}

			File file = new File(path);
			int modeint;
			
			if (mode.equals("rw") == true)
			{
				modeint = ParcelFileDescriptor.MODE_READ_WRITE |
				  ParcelFileDescriptor.MODE_TRUNCATE;
			}
			else
			{
				modeint = ParcelFileDescriptor.MODE_READ;
				
				if (file.exists() == false)
				{
					try
					{
						/* TODO: Find a way around this.  We should be able to
						 * simply return an InputStream. */
						copyDefaultIcon(path);
					}
					catch(IOException e)
					{
						Log.d(TAG, "Unable to create default feed icon", e);
						return null;
					}					
				}
			}
			
			return ParcelFileDescriptor.open(file, modeint);
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}
	
	private long insertChannels(ContentValues values)
	{
		Resources r = Resources.getSystem();

		/* TODO: This validation sucks. */
		if (values.containsKey(RSSReader.Channels.TITLE) == false)
			values.put(RSSReader.Channels.TITLE, r.getString(android.R.string.untitled));

		long id = mDB.insert("rssreader_channel", "title", values);

		if (values.containsKey(RSSReader.Channels.ICON) == false)
		{
			Uri iconUri;

			iconUri = RSSReader.Channels.CONTENT_URI.buildUpon()
			  .appendPath(String.valueOf(id))
			  .appendPath("icon")
			  .build();

			/* LAME! */
			ContentValues update = new ContentValues();
			update.put(RSSReader.Channels.ICON, iconUri.toString());
			mDB.update("rssreader_channel", update, "_id=" + id, null);
		}

		return id;
	}

	private long insertPosts(ContentValues values)
	{
		/* TODO: Validation? */
		return mDB.insert("rssreader_post", "title", values);
	}

	@Override
	public Uri insert(Uri url, ContentValues initialValues)
	{
		long rowID;
		ContentValues values;

		if (initialValues != null)
			values = new ContentValues(initialValues);
		else
			values = new ContentValues();
		
		Uri uri;
		
		if (URL_MATCHER.match(url) == CHANNELS)
		{
			rowID = insertChannels(values);
			uri = ContentUris.withAppendedId(RSSReader.Channels.CONTENT_URI, rowID);
		}
		else if (URL_MATCHER.match(url) == POSTS)
		{
			rowID = insertPosts(values);
			uri = ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI, rowID);
		}
		else
		{
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		if (rowID > 0)
		{
			assert(uri != null);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		
		throw new SQLException("Failed to insert row into " + url);
	}
	
	@Override
	public int delete(Uri url, String where, String[] whereArgs)
	{
		int count;
		String myWhere;
		
		switch (URL_MATCHER.match(url))
		{
		case CHANNELS:
			count = mDB.delete("rssreader_channel", where, whereArgs);
			break;
			
		case CHANNEL_ID:
			myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.delete("rssreader_channel", myWhere, whereArgs);
			break;
			
		case POSTS:
			count = mDB.delete("rssreader_post", where, whereArgs);
			break;
			
		case POST_ID:
			myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.delete("rssreader_post", myWhere, whereArgs);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}
	
	@Override
	public int update(Uri url, ContentValues values, String where, String[] whereArgs)
	{
		int count;
		String myWhere;
		
		switch (URL_MATCHER.match(url))
		{
		case CHANNELS:
			count = mDB.update("rssreader_channel", values, where, whereArgs);
			break;
			
		case CHANNEL_ID:
			myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.update("rssreader_channel", values, myWhere, whereArgs);
			break;
			
		case POSTS:
			count = mDB.update("rssreader_post", values, where, whereArgs);
			break;
			
		case POST_ID:
			myWhere = "_id=" + url.getPathSegments().get(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");			
			count = mDB.update("rssreader_post", values, myWhere, whereArgs);
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URL " + url);
		}
		
		getContext().getContentResolver().notifyChange(url, null);
		return count;
	}

	static
	{
		URL_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URL_MATCHER.addURI(RSSReader.AUTHORITY, "channels", CHANNELS);
		URL_MATCHER.addURI(RSSReader.AUTHORITY, "channels/#", CHANNEL_ID);
		URL_MATCHER.addURI(RSSReader.AUTHORITY, "channels/#/icon", CHANNELICON_ID);
		URL_MATCHER.addURI(RSSReader.AUTHORITY, "posts", POSTS);
		URL_MATCHER.addURI(RSSReader.AUTHORITY, "posts/#", POST_ID);
		
		/* TODO: use channels/#/posts */
		URL_MATCHER.addURI(RSSReader.AUTHORITY, "postlist/#", CHANNEL_POSTS);

		CHANNEL_LIST_PROJECTION_MAP = new HashMap<String, String>();
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels._ID, "_id");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.TITLE, "title");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.URL, "url");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.ICON, "icon");
		CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.LOGO, "logo");
		
		POST_LIST_PROJECTION_MAP = new HashMap<String, String>();
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts._ID, "_id");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.CHANNEL_ID, "channel_id");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.READ, "read");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.TITLE, "title");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.URL, "url");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.AUTHOR, "author");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.DATE, "posted_on");
		POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.BODY, "body");
	}
}
