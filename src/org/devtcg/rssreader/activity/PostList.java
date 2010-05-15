/*
 * $Id: PostList.java 112 2008-02-14 00:34:50Z jasta00 $
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

import org.devtcg.rssreader.R;
import org.devtcg.rssreader.provider.RSSReader;
import org.devtcg.rssreader.util.KeyUtils;
import org.devtcg.rssreader.view.ChannelHead;
import org.devtcg.rssreader.view.PostListRow;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.ListView;

public class PostList extends ListActivity
{
	private static final int PREV_ID = Menu.FIRST;
	private static final int NEXT_ID = Menu.FIRST + 1;
	
	private static final String[] PROJECTION = new String[] {
	  RSSReader.Posts._ID, RSSReader.Posts.CHANNEL_ID,
	  RSSReader.Posts.TITLE, RSSReader.Posts.READ,
	  RSSReader.Posts.DATE };
	
	private Cursor mCursor;
	private long mID = -1;
	
	private long mPrevID = -1;
	private long mNextID = -1;

	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		
		setContentView(R.layout.post_list);

		Uri uri = getIntent().getData();
		mCursor = managedQuery(uri, PROJECTION, null, null);
		mID = Long.parseLong(uri.getPathSegments().get(1));

		ListAdapter adapter = new PostListAdapter(mCursor, this);
        setListAdapter(adapter);
        
        initWithData();
	}

	private void initWithData()
	{
		long channelId = Long.parseLong(getIntent().getData().getPathSegments().get(1));

		ContentResolver cr = getContentResolver();		
		Cursor cChannel = cr.query(ContentUris.withAppendedId(RSSReader.Channels.CONTENT_URI, channelId),
		  new String[] { RSSReader.Channels.LOGO, RSSReader.Channels.ICON, RSSReader.Channels.TITLE }, null, null, null);

		assert(cChannel.count() == 1);
		cChannel.first();

		/* TODO: Check if RSSReader.Channels.LOGO exists and use it. */
		ChannelHead head = (ChannelHead)findViewById(R.id.postListHead);
		head.setLogo(cChannel);
		
		cChannel.close();
	}

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
		Uri uri = 
		  ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI, id);
    	String action = getIntent().getAction();
    	
    	if (action.equals(Intent.PICK_ACTION) ||
    	    action.equals(Intent.GET_CONTENT_ACTION))
    	{
    		setResult(RESULT_OK, uri.toString());
    	}
    	else
    	{
    		startActivity(new Intent(Intent.VIEW_ACTION, uri));
    	}
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	menu.removeGroup(0);

    	getSiblings();
    	
		if (mPrevID >= 0)
		{
			menu.add(0, PREV_ID, "Previous Channel").
  	  	  	  setShortcut('1', '[');
		}

		if (mNextID >= 0)
		{
			menu.add(0, NEXT_ID, "Next Channel").
			  setShortcut('3', ']');

			menu.setDefaultItem(PREV_ID);
		}
		
		return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(Menu.Item item)
    {
    	switch(item.getId())
    	{
    	case PREV_ID:
    		return prevChannel();
    		
    	case NEXT_ID:
    		return nextChannel();
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    private void getSiblings()
    {
    	if (mNextID >= 0 && mPrevID >= 0)
    		return;
    	
    	Cursor cChannelList = getContentResolver().query
    	  (RSSReader.Channels.CONTENT_URI,
    	    new String[] { RSSReader.Channels._ID }, null, null, null);

    	/* TODO: This is super lame; we need to use SQLite queries to
    	 * determine posts either newer or older than the current one
    	 * without. */
    	cChannelList.first();

    	long lastId = -1;

    	for (cChannelList.first(); cChannelList.isLast() == false; cChannelList.next())
    	{
    		long thisId = cChannelList.getLong(0);

    		if (thisId == mID)
    			break;

    		lastId = thisId;
    	}

    	if (mPrevID < 0)
    		mPrevID = lastId;

    	if (mNextID < 0)
    	{
    		if (cChannelList.isLast() == false)
    		{
    			cChannelList.next();
    			mNextID = cChannelList.getLong(0);
    		}
    	}

		cChannelList.close();
    }
    
    private void moveTo(long id)
    {
    	Uri uri = 
    	  ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI_LIST, id);
		Intent intent = new Intent(Intent.VIEW_ACTION, uri);
		startActivity(intent);
		
		/* Assume that user would do not want to keep the [now read]
		 * current post in the history stack. */
		finish();    	
    }
    
    private boolean prevChannel()
    {
    	if (mPrevID < 0)
    		return false;
    	
    	moveTo(mPrevID);
    	return true;
    }
    
    private boolean nextChannel()
    {
    	if (mNextID < 0)
    		return false;
    	
    	moveTo(mNextID);
    	return true;
    }
    
    @Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
    {
    	switch (KeyUtils.interpretDirection(keyCode))
    	{
    	case KeyEvent.KEYCODE_DPAD_LEFT:
    		getSiblings();
    		return prevChannel();
    		
    	case KeyEvent.KEYCODE_DPAD_RIGHT:
    		getSiblings();
    		return nextChannel();
    	}
    	
    	return false;
    }

    private static class PostListAdapter extends CursorAdapter implements Filterable
    {
		public PostListAdapter(Cursor c, Context context)
		{
			super(c, context);
		}
		
		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			((PostListRow)view).bindView(cursor);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			PostListRow post = new PostListRow(context);
			post.bindView(cursor);
			return post;
		}
    }
}
