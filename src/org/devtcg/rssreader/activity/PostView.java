/*
 * $Id: PostView.java 112 2008-02-14 00:34:50Z jasta00 $
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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.TextView;

public class PostView extends Activity
{
	private static final String TAG = "PostView";
	
	private final static int NEXT_POST_ID = Menu.FIRST;
	private final static int PREV_POST_ID = Menu.FIRST + 1;
	
	private static final String[] PROJECTION = new String[] {
	  RSSReader.Posts._ID, RSSReader.Posts.CHANNEL_ID,
	  RSSReader.Posts.TITLE, RSSReader.Posts.BODY, RSSReader.Posts.READ,
	  RSSReader.Posts.URL };
	
	private long mChannelID = -1;
	private long mPostID = -1;
	
	private Cursor mCursor;
	
	private long mPrevPostID = -1;
	private long mNextPostID = -1;
	
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);		
		setContentView(R.layout.post_view);

//		RSSChannelHead head = (RSSChannelHead)findViewById(R.id.postViewHead);
//		PostScrollView scroll = (PostScrollView)findViewById(R.id.postViewScroll);
//		scroll.setChannelHead(head);
		
		Uri uri = getIntent().getData();

		mCursor = managedQuery(uri, PROJECTION, null, null, null);

		if (mCursor == null || mCursor.first() == false)
			finish();

		mChannelID = mCursor.getLong(mCursor.getColumnIndex(RSSReader.Posts.CHANNEL_ID));
		mPostID = Long.parseLong(uri.getPathSegments().get(1));

		/* TODO: Should this be in onStart() or onResume() or something?  */
		initWithData();
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		if (mCursor == null || mCursor.first() == false)
			return;

		/* Set the post to read. */
		mCursor.updateInt(mCursor.getColumnIndex(RSSReader.Posts.READ), 1);
		mCursor.commitUpdates();
	}

	private void initWithData()
	{	
		ContentResolver cr = getContentResolver();

		Cursor cChannel = cr.query(ContentUris.withAppendedId(RSSReader.Channels.CONTENT_URI, mChannelID),
		  new String[] { RSSReader.Channels.ICON, RSSReader.Channels.LOGO, RSSReader.Channels.TITLE }, null, null, null);

		assert(cChannel.count() == 1);
		cChannel.first();

		/* Make the view useful. */
		ChannelHead head = (ChannelHead)findViewById(R.id.postViewHead);
		head.setLogo(cChannel);
//		head.setPost(mCursor);

		cChannel.close();

		TextView postTitle = (TextView)findViewById(R.id.postTitle);
		postTitle.setText(mCursor, mCursor.getColumnIndex(RSSReader.Posts.TITLE));

		WebView postText = (WebView)findViewById(R.id.postText);

		/* TODO: I want the background transparent, but that doesn't seem 
		 * possible.  Black will do for now. */
		String html =
		  "<html><head><style type=\"text/css\">body { background-color: #201c19; color: white; } a { color: #ddf; }</style></head><body>" +
		  getBody() +
		  "</body></html>";

		postText.loadData(html, "text/html", "utf-8");
	}
	
	/* Apply some simple heuristics to the post text to determine what special
	 * features we want to show. */
	private String getBody()
	{
		String body =
		  mCursor.getString(mCursor.getColumnIndex(RSSReader.Posts.BODY));
		
		String url =
		  mCursor.getString(mCursor.getColumnIndex(RSSReader.Posts.URL));
	
		if (hasMoreLink(body, url) == false)
			body += "<p><a href=\"" + url + "\">Read more...</a></p>";
		
		/* TODO: We should add a check for "posted by", "written by",
		 * "posted on", etc, and optionally add our own tagline if
		 * the information is in the feed. */
		return body;
	}
	
	private boolean hasMoreLink(String body, String url)
	{
		int urlpos;

		/* Check if the body contains an anchor reference with the
		 * destination of the read more URL we got from the feed. */
		if ((urlpos = body.indexOf(url)) <= 0)
			return false;
		
		try
		{
			/* TODO: Improve this check with a full look-behind parse. */
			if (body.charAt(urlpos - 1) != '>')
				return false;

			if (body.charAt(urlpos + url.length() + 1) != '<')
				return false;
		}
		catch (IndexOutOfBoundsException e)
		{
			return false;
		}
			
		return true;
	}
	
	private void getSiblings()
	{
		if (mNextPostID < 0 || mPrevPostID < 0)
		{
	    	Cursor cPostList = getContentResolver().query
	    	 (ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI_LIST, mChannelID),
	    	  new String[] { RSSReader.Posts._ID }, null, null, null);

	    	/* TODO: This is super lame; we need to use SQLite queries to
	    	 * determine posts either newer or older than the current one
	    	 * without. */
	    	cPostList.first();
	    	
	    	int indexId = cPostList.getColumnIndex(RSSReader.Posts._ID);
	    	
	    	long lastId = -1;
	    	
	    	for (cPostList.first(); cPostList.isLast() == false; cPostList.next())
	    	{
	    		long thisId = cPostList.getLong(indexId);
	    		
	    		if (thisId == mPostID)
	    			break;
	    		
	    		lastId = thisId;
	    	}

	    	/* Remember, the order is descending by date. */
	    	if (mNextPostID < 0)
	    		mNextPostID = lastId;

	    	if (mPrevPostID < 0)
	    	{
	    		if (cPostList.isLast() == false)
	    		{
	    			cPostList.next();
	    			mPrevPostID = cPostList.getLong(indexId);
	    		}
	    	}
		}		
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		menu.removeGroup(0);
		
		getSiblings();

		if (mNextPostID >= 0)
		{
			menu.add(0, NEXT_POST_ID, "Newer Post").
  	  	  	  setShortcut('1', '[');
		}
    	
		if (mPrevPostID >= 0)
		{
			menu.add(0, PREV_POST_ID, "Older Post").
			  setShortcut('3', ']');
			
			menu.setDefaultItem(PREV_POST_ID);
		}
		
		return true;
	}
	
	private void moveTo(long id)
	{
		Intent intent = new Intent(Intent.VIEW_ACTION,
		  ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI, id));
		
		startActivity(intent);
		
		/* Assume that user would do not want to keep the [now read]
		 * current post in the history stack. */
		finish();
	}
	
	private boolean prevPost()
	{
		if (mPrevPostID < 0)
			return false;
		
		moveTo(mPrevPostID);
		return true;
	}
	
	private boolean nextPost()
	{
		if (mNextPostID < 0)
			return false;
		
		moveTo(mNextPostID);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(Menu.Item item)
    {
    	switch (item.getId())
    	{
    	case PREV_POST_ID:
    		return prevPost();
    		
    	case NEXT_POST_ID:
    		return nextPost();
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
    {
    	/*
    	 * If the user actually presses left or right, let's pass that key
    	 * press on to the WebView in case scrolling is necessary.  All 
    	 * other direction keypresses (keypad, bracket, etc) as defined
    	 * by KeyUtils.interpretDirection() will still be honored.
    	 */
    	if (keyCode != KeyEvent.KEYCODE_DPAD_LEFT &&
    	    keyCode != KeyEvent.KEYCODE_DPAD_RIGHT)
    	{
    		switch (KeyUtils.interpretDirection(keyCode))
    		{
    		case KeyEvent.KEYCODE_DPAD_LEFT:
    			getSiblings();
    			return nextPost();

    		case KeyEvent.KEYCODE_DPAD_RIGHT:
    			getSiblings();
    			return prevPost();
    		}
    	}
    	
    	return false;
    }

//    /* 
//     * Special ScrollView class that is used to determine if the post title
//     * is currently visible.  If not, it will change it to show the
//     * RSSChannelHead to show the title with a flashy animation to show
//     * off Android goodness.
//     */
//    public static class PostScrollView extends ScrollView
//    {
//    	private int mTitleTop = 0;
//    	private ChannelHead mHead;
//    	
//    	public PostScrollView(Context context)
//    	{
//    		super(context);
//    	}
//
//    	public PostScrollView(Context context, AttributeSet attrs, Map inflateParams)
//    	{
//    		super(context, attrs, inflateParams);
//    	}
//
//    	public PostScrollView(Context context, AttributeSet attrs, Map inflateParams, int defStyle)
//    	{
//    		super(context, attrs, inflateParams, defStyle);
//    	}
//    	
//    	public void setChannelHead(ChannelHead head)
//    	{
//    		mHead = head;
//    	}
//    	
//    	@Override
//        /* TODO: Overriding computeScroll() does not seem to be an efficient
//         * way to tackle this problem.  Any suggestions for improvement? */ 
//    	public void computeScroll()
//    	{
//    		super.computeScroll();
//
//    		if (mTitleTop < 0)
//    		{
//    			TextView title = (TextView)findViewById(R.id.postTitle);
//
//    			if (title != null)
//    			{
//    				mTitleTop = title.getLineHeight() / 2;
//    				assert(mTitleTop > 0);
//    			}
//    		}
//    		else
//    		{
//    			if (mScrollY > mTitleTop)
//    			{
//    				if (mHead.isPostTitleVisible() == false)
//    					mHead.showPostTitle();
//    			}
//    			else
//    			{
//    				if (mHead.isPostTitleVisible() == true)
//    					mHead.showChannelTitle();
//    			}
//    		}
//    	}
//    }
}
