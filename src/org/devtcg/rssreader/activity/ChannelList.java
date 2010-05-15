/*
 * $Id: ChannelList.java 114 2008-02-14 05:09:23Z jasta00 $
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

package org.devtcg.rssreader.activity;

import java.util.HashMap;

import org.devtcg.rssreader.R;
import org.devtcg.rssreader.parser.ChannelRefresh;
import org.devtcg.rssreader.provider.RSSReader;
import org.devtcg.rssreader.util.DownloadManager;
import org.devtcg.rssreader.view.ChannelListRow;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentUris;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu.Item;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ChannelList extends ListActivity
{
	public static final String TAG = "RSSChannelList";
	public static final String TAG_PREFS = "RSSReader";

	public static final int DELETE_ID = Menu.FIRST;
	public static final int INSERT_ID = Menu.FIRST + 1;
	public static final int REFRESH_ID = Menu.FIRST + 2;
	public static final int REFRESH_ALL_ID = Menu.FIRST + 3;
	public static final int EDIT_ID = Menu.FIRST + 4;

	private Cursor mCursor;

//	private boolean mFirstTime;

//	private IRSSReaderService mService;

	private DownloadManager mDownloadManager;
	private Handler mRefreshHandler;
//	private HashMap<Long, Thread> mRefreshThreads;

    private static final String[] PROJECTION = new String[] {
      RSSReader.Channels._ID, RSSReader.Channels.ICON,
      RSSReader.Channels.TITLE, RSSReader.Channels.URL };

    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        setContentView(R.layout.channel_list);

        Intent intent = getIntent();
        if (intent.getData() == null)
            intent.setData(RSSReader.Channels.CONTENT_URI);

        if (intent.getAction() == null)
        	intent.setAction(Intent.VIEW_ACTION);

        mCursor = managedQuery(getIntent().getData(), PROJECTION, null, null);

//        bindService(new Intent(this, RSSReaderService.class),
//          null, mServiceConn, Context.BIND_AUTO_CREATE);

//        SharedPreferences settings = getSharedPreferences(TAG_PREFS, 0);
//        mFirstTime = settings.getBoolean("firstTime", true);

        /*
         * If this is the first time the user has opened our app ever,
         * install the RSSReaderService_Alarm as has been scheduled for
         * next device reboot.  After `firstTime`, RSSReaderService_Setup
         * will handle our setup at BOOT_COMPLETED.
         *
         * TODO: There must be a more appropriate way to handle this?
         */
//        if (mFirstTime == true)
//        	RSSReaderService_Setup.setupAlarm(this);

        ListAdapter adapter = new ChannelListAdapter(mCursor, this);
        setListAdapter(adapter);
    }

    @Override
    protected void onStop()
    {
    	super.onStop();

//    	if (mFirstTime == true)
//    	{
//    		SharedPreferences settings = getSharedPreferences(TAG_PREFS, 0);
//    		SharedPreferences.Editor editor = settings.edit();
//    		editor.putBoolean("firstTime", false);
//    		editor.commit();
//    	}
    }

//    private ServiceConnection mServiceConn = new ServiceConnection()
//    {
//    	public void onServiceConnected(ComponentName className, IBinder service)
//    	{
//    		Log.d(TAG, "onServiceConnected");
//    		mService = IRSSReaderService.Stub.asInterface((IBinder)service);
//    	}
//
//    	public void onServiceDisconnected(ComponentName className)
//    	{
//    		Log.d(TAG, "onServiceDisconnected");
//    		mService = null;
//    	}
//    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	super.onCreateOptionsMenu(menu);

    	menu.add(0, INSERT_ID, "New Channel").
    	  setShortcut('3', 'a');

    	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	super.onPrepareOptionsMenu(menu);
    	final boolean haveItems = mCursor.count() > 0;

		menu.removeGroup(Menu.SELECTED_ALTERNATIVE);

    	/* If there are items in the list, add the extra context menu entries
    	 * available on each channel listed. */
    	if (haveItems)
    	{
//          ContentURI uri = getIntent().getData().addId(getSelectionRowID());
//
//    		menu.addSeparator(Menu.SELECTED_ALTERNATIVE, 0);

    		menu.add(Menu.SELECTED_ALTERNATIVE, REFRESH_ALL_ID, "Refresh All");

//    		menu.add(Menu.SELECTED_ALTERNATIVE, REFRESH_ID, "Refresh Channel").
//		  	  setShortcut(0, 0, KeyEvent.KEYCODE_R);

//    		menu.addSeparator(Menu.SELECTED_ALTERNATIVE, 0);

//          Item edit = menu.add(Menu.SELECTED_ALTERNATIVE, EDIT_ID, "Edit Channel");
//          edit.setIntent(new Intent(Intent.EDIT_ACTION, uri));
//          edit.setShortcut(KeyEvent.KEYCODE_1, 0, KeyEvent.KEYCODE_E);

//			menu.add(Menu.SELECTED_ALTERNATIVE, DELETE_ID, "Delete Channel").
//			setShortcut(KeyEvent.KEYCODE_2, 0, KeyEvent.KEYCODE_D);
//
//    		menu.addSeparator(Menu.SELECTED_ALTERNATIVE, 0);
//
    		menu.setDefaultItem(INSERT_ID);
    	}

    	return true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
    	String action = getIntent().getAction();

    	if (action.equals(Intent.PICK_ACTION) ||
    	    action.equals(Intent.GET_CONTENT_ACTION))
    	{
    		Uri uri =
    		  ContentUris.withAppendedId(getIntent().getData(), id);
    		
    		setResult(RESULT_OK, uri.toString());
    	}
    	else
    	{
    		Uri uri = 
    		  ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI_LIST, id);
    		
    		startActivity(new Intent(Intent.VIEW_ACTION, uri));
    	}
    }

    @Override
    public boolean onOptionsItemSelected(Menu.Item item)
    {
    	switch(item.getId())
    	{
    	case INSERT_ID:
    		startActivity(new Intent(Intent.INSERT_ACTION, getIntent().getData()));
    		return true;

//    	case DELETE_ID:
//    		deleteChannel();
//    		return true;
//
//    	case REFRESH_ID:
//        	mCursor.moveTo(getSelection());
//    		refreshChannel();
//    		return true;

    	case REFRESH_ALL_ID:
    		refreshAllChannels();
    		return true;
    	}

    	return super.onOptionsItemSelected(item);
    }

//    private final void deleteChannel()
//    {
//    	long channelId = getSelectionRowID();
//
////    	Thread refresh;
////
////    	if (mRefreshThreads != null &&
////    	    (refresh = mRefreshThreads.remove(channelId)) != null)
////    	{
////    		/* TODO: Stop the thread. */
////    	}
//
//		/* Delete related posts. */
//		getContentResolver().delete(RSSReader.Posts.CONTENT_URI,
//    	  "channel_id=?", new String[] { String.valueOf(channelId) });
//
//		mCursor.deleteRow();
//    }

    private final void refreshAllChannels()
    {
    	if (mCursor.first() == false)
    		return;
    	
    	do 
    	{
    		refreshChannel();
    	} while (mCursor.next() == true);
    }

    /* This method assumes that `mCursor` has been positioned on the record
     * we want to refresh. */
    private final void refreshChannel()
    {
    	/* We don't initialize these unless the user requests a refresh.
    	 * The common case is that the background service will be responsible
    	 * for this task, so the behaviour in this activity will rarely be
    	 * invoked.
    	 */
    	if (mDownloadManager == null)
    	{
    		mRefreshHandler = new Handler();
    		mDownloadManager = new DownloadManager(mRefreshHandler);
    	}

//    	if (mRefreshThreads == null)
//    		mRefreshThreads = new HashMap<Long, Thread>();

    	long channelId =
      	  mCursor.getInt(mCursor.getColumnIndex(RSSReader.Channels._ID));

//    	/* Don't refresh the same channel more than once. */
//    	if (mRefreshThreads.containsKey(channelId) == true)
//    		return;

    	String rssurl = mCursor.getString(mCursor.getColumnIndex(RSSReader.Channels.URL));

    	/* TODO: Is there a generalization of getListView().getSelectedView() we can use here?
    	 * http://groups.google.com/group/android-developers/browse_thread/thread/4070126fd996001c */
    	ChannelListRow row =
    	  ((ChannelListAdapter)getListAdapter()).getViewByRowID(channelId);

    	assert(row != null);

		Runnable refresh = new RefreshRunnable(mRefreshHandler, row, channelId, rssurl);

		mDownloadManager.schedule(refresh);

//    	Thread t = new Thread(refresh);
//
//    	/* Manage active threads so we a) don't refresh an already refreshing
//    	 * channel, and b) we can stop the thread if the user deletes the
//    	 * channel. */
//    	mRefreshThreads.put(channelId, t);
//
//    	t.start();
    }

    private static class ChannelListAdapter extends CursorAdapter implements Filterable
    {
    	/* TODO: Android should provide a way to look up a View by row, but
    	 * it does not currently.  Hopefully this will be fixed in future
    	 * releases. */
    	private HashMap<Long, ChannelListRow> rowMap;

		public ChannelListAdapter(Cursor c, Context context)
		{
			super(c, context);
			rowMap = new HashMap<Long, ChannelListRow>();
		}

		protected void updateRowMap(Cursor cursor, ChannelListRow row)
		{
			long channelId =
			  cursor.getLong(cursor.getColumnIndex(RSSReader.Channels._ID));

			rowMap.put(new Long(channelId), row);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			ChannelListRow row = (ChannelListRow)view;
			row.bindView(cursor);
			updateRowMap(cursor, row);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			ChannelListRow row = new ChannelListRow(context);
			row.bindView(cursor);
			updateRowMap(cursor, row);
			return row;
		}

		public ChannelListRow getViewByRowID(long id)
		{
			return rowMap.get(new Long(id));
		}
    }

    private class RefreshRunnable implements Runnable
    {
    	private Handler mHandler;
    	private ChannelListRow mRow;
    	private long mChannelID;
    	private String mRSSURL;

    	public RefreshRunnable(Handler handler, ChannelListRow row, long channelId, String rssurl)
    	{
    		mHandler = handler;
    		mRow = row;
    		mChannelID = channelId;
    		mRSSURL = rssurl;
    	}

    	public void run()
    	{
			Log.e("RSSChannelList", "Here we go: " + mRSSURL + "...");

			mHandler.post(new Runnable() {
				public void run()
				{
					mRow.startRefresh();
				}
			});

			try
			{
				new ChannelRefresh(getContentResolver()).
				  syncDB(mHandler, mChannelID, mRSSURL);
			}
			catch (Exception e)
			{
				/* TODO: Handle me somehow... */
				Log.d("RSSChannelList", Log.getStackTraceString(e));
			}

	    	mHandler.post(new Runnable() {
	    		public void run()
	    		{
	    			mRow.finishRefresh(mChannelID);
//	    			mRefreshThreads.remove(mChannelID);
	    		}
	    	});
    	}
    }
}
