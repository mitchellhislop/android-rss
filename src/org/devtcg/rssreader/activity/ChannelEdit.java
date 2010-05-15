/*
 * $Id: ChannelEdit.java 112 2008-02-14 00:34:50Z jasta00 $
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

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class ChannelEdit extends Activity
{
	private TextView mURLText;
	private TextView mTitleText;
	
	private Uri mURI;
	
	private Cursor mCursor;
	
	private static final String[] PROJECTION = {
	  RSSReader.Channels._ID,
	  RSSReader.Channels.URL, RSSReader.Channels.TITLE,
	  RSSReader.Channels.ICON };
	
	private static final int URL_INDEX = 1;
	private static final int TITLE_INDEX = 2;
	private static final int ICON_INDEX = 3;
	
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
				
		mURI = getIntent().getData();
		mCursor = managedQuery(mURI, PROJECTION, null, null);

		setContentView(R.layout.channel_edit);
		
		mURLText = (TextView)findViewById(R.id.channelEditURL);
		mTitleText = (TextView)findViewById(R.id.channelEditName);
		
		Button save = (Button)findViewById(R.id.channelEditSave);
		save.setOnClickListener(mSaveListener);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (mCursor == null)
			return;

		mCursor.first();
		
		mURLText.setText(mCursor, URL_INDEX);
		mTitleText.setText(mCursor, TITLE_INDEX);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();

		if (mCursor == null)
			return;

		updateProvider();
		managedCommitUpdates(mCursor);
	}

	private void updateProvider()
	{
		if (mCursor == null)
			return;

		mCursor.updateString(URL_INDEX, mURLText.getText().toString());
		mCursor.updateString(TITLE_INDEX, mTitleText.getText().toString());
	}

	private OnClickListener mSaveListener = new OnClickListener()
	{
		public void onClick(View v)
		{
			updateProvider();
			mCursor.commitUpdates();
			
			setResult(RESULT_OK, mURI.toString());
			finish();
		}
	};
}
