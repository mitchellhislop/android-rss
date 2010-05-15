/*
 * $Id: ChannelListRow.java 114 2008-02-14 05:09:23Z jasta00 $
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

package org.devtcg.rssreader.view;

import java.net.URISyntaxException;

import org.devtcg.rssreader.R;
import org.devtcg.rssreader.provider.RSSReader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewInflate;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ChannelListRow extends RelativeLayout
{
	private ImageView mIcon;
	private TextView mName;
	private TextView mCount;
	private ProgressBar mRefresh;

//	private static final int CHANNEL_NAME = 1;
//	private static final int CHANNEL_RIGHT = 2;
//	private static final int CHANNEL_ICON = 3;

	public ChannelListRow(Context context)
	{
		super(context);

		setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		View view = ViewInflate.from(getContext()).inflate(R.layout.channel_list_item, null, false, null);

		mIcon = (ImageView)view.findViewById(R.id.channel_icon);
		mName = (TextView)view.findViewById(R.id.channel_name);
		mCount = (TextView)view.findViewById(R.id.channel_post_count);
		mRefresh = (ProgressBar)view.findViewById(R.id.channel_refresh);
		mRefresh.setVisibility(View.GONE);
		
		RelativeLayout.LayoutParams rules =
		  new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		addView(view, rules);


//		mIcon = new ImageView(context);
//		mIcon.setPadding(0, 2, 3, 2);
//		mIcon.setId(CHANNEL_ICON);
//
//		RelativeLayout.LayoutParams iconRules =
//		  new RelativeLayout.LayoutParams(16 + 3, 16 + 4);
//		
//		iconRules.addRule(ALIGN_WITH_PARENT_LEFT);
//		addView(mIcon, iconRules);
//		
//		LinearLayout rightSide = new LinearLayout(context);
//		rightSide.setId(CHANNEL_RIGHT);
//		
//		/* <right> */
//		mRefresh = new ProgressBar(context);
//		mRefresh.setIndeterminate(true);
//		mRefresh.setVisibility(GONE);
//		
//		LinearLayout.LayoutParams refreshRules = new LinearLayout.LayoutParams(18, 18);
//		refreshRules.gravity = Gravity.CENTER_VERTICAL;
//		rightSide.addView(mRefresh, refreshRules);
//
//		mCount = new TextView(context);
//		mCount.setGravity(Gravity.CENTER_VERTICAL);
//		rightSide.addView(mCount, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//		/* </right> */
//		
//		RelativeLayout.LayoutParams rightRules =
//		  new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
//		
//		rightRules.addRule(ALIGN_WITH_PARENT_RIGHT);
//		
//		addView(rightSide, rightRules);
//		
//		mName = new TextView(context);
//		mName.setPadding(3, 0, 0, 0);
//		mName.setId(CHANNEL_NAME);
//
//		RelativeLayout.LayoutParams nameRules =
//		  new RelativeLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT);
//
//		nameRules.addRule(POSITION_TO_LEFT, CHANNEL_RIGHT);
//		nameRules.addRule(POSITION_TO_RIGHT, CHANNEL_ICON);
//		addView(mName, nameRules);    		
	}

	public void bindView(Cursor cursor)
	{
		ContentResolver content = getContext().getContentResolver();
		
		long channelId = 
		  cursor.getLong(cursor.getColumnIndex(RSSReader.Channels._ID));

		/* Determine number of unread posts. */
		Cursor unread = 
		  content.query(ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI_LIST, channelId), 
		    new String[] { RSSReader.Posts._ID }, "read=0", null, null);

		Typeface tf;

		int unreadCount = unread.count();
		unread.close();

		if (unreadCount > 0)
			tf = Typeface.DEFAULT_BOLD;
		else
			tf = Typeface.DEFAULT;

		String icon = 
		  cursor.getString(cursor.getColumnIndex(RSSReader.Channels.ICON));
		
		mIcon.setImageURI(Uri.parse(icon));

		mName.setTypeface(Typeface.DEFAULT);
		mName.setText(cursor, cursor.getColumnIndex(RSSReader.Channels.TITLE));

		mCount.setTypeface(tf);
		mCount.setText(new Integer(unreadCount).toString());
	}
	
	public void startRefresh()
	{
		Log.d("RSSChannelListRow", "Start refresh...");
		mCount.setVisibility(GONE);
		mRefresh.setVisibility(VISIBLE);
	}
	
	public void updateRefresh(int progress)
	{
		Log.d("RSSChannelListRow", "Switch to value-based, update to: " + progress);
	}
	
	public void finishRefresh(Cursor cursor)
	{
		Log.d("RSSChannelListRow", "Finished refresh, reset original view...");
		
		mRefresh.setVisibility(GONE);
		bindView(cursor);		
		mCount.setVisibility(VISIBLE);
	}
	
	public void finishRefresh(long channelId)
	{
		Cursor cursor = getContext().getContentResolver().query
		 (ContentUris.withAppendedId(RSSReader.Channels.CONTENT_URI, channelId),
		  new String[] { RSSReader.Channels._ID, RSSReader.Channels.TITLE, RSSReader.Channels.ICON },
		  null, null, null);
		
		/* Hmm, they must have deleted this channel while we were
		 * refreshing?  OK, we can deal... */
		if (cursor.count() < 1)
			return;
		
		cursor.first();
		finishRefresh(cursor);
		cursor.close();
	}
}
