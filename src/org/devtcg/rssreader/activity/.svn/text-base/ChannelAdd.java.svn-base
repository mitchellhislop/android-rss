/*
 * $Id:ChannelAdd.java 87 2007-12-10 06:24:56Z jasta00 $
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

import java.net.MalformedURLException;
import java.net.URL;

import org.devtcg.rssreader.R;
import org.devtcg.rssreader.parser.ChannelRefresh;
import org.devtcg.rssreader.provider.RSSReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ChannelAdd extends Activity
{
	private static final String TAG = "ChannelAdd";

	public EditText mURLText;

	/* We need this to not block when accessing the RSS feed for validation
	 * and for name downloads. */
	protected ProgressDialog mBusy;	
	final Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		setContentView(R.layout.channel_add);

		mURLText = (EditText)findViewById(R.id.url);
//		mURLText.setKeyListener(new View.OnKeyListener() {
//			public boolean onKey(View v, int keyCode, KeyEvent event)
//			{
//				if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
//				    keyCode == KeyEvent.KEYCODE_NEWLINE)
//				{
//					addChannel();
//					return true;
//				}
//
//				return false;
//			}
//		});
		
		Button add = (Button)findViewById(R.id.add);
		add.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v)
			{
				addChannel();
			}
		});
	}

	@Override
	protected void onStart()
	{
		super.onStart();		
	}
	
	private static URL getDefaultFavicon(String rssurl)
	{
		try
		{
			URL orig = new URL(rssurl);

			URL iconUrl = new URL(orig.getProtocol(), orig.getHost(),
			  orig.getPort(), "/favicon.ico");
			
			return iconUrl;
		}
		catch (MalformedURLException e)
		{
			/* This shouldn't happen since we've already validated. */
			Log.d(TAG, Log.getStackTraceString(e));
			return null;
		}
	}

	private void addChannel()
	{
		final String rssurl = mURLText.getText().toString();

		mBusy = ProgressDialog.show(ChannelAdd.this,
		  "Downloading", "Accessing XML feed...", true, false);

		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					ChannelRefresh refresh = new ChannelRefresh(getContentResolver());

					final long id = refresh.syncDB(null, -1, rssurl);
					
					if (id >= 0)
					{
						URL iconurl = getDefaultFavicon(rssurl);
						refresh.updateFavicon(id, iconurl);
					}

			    	mHandler.post(new Runnable() {
			    		public void run()
			    		{
			    			mBusy.dismiss();
			    			
			    			Uri uri = ContentUris.withAppendedId(RSSReader.Channels.CONTENT_URI, id);
			    			setResult(RESULT_OK, uri.toString());
			    			finish();
			    		}
			    	});
				}
				catch(Exception e)
				{
					final String errmsg = e.getMessage();
					final String errmsgFull = e.toString();

		    		mHandler.post(new Runnable() {
		    			public void run()
		    			{
		    				mBusy.dismiss();

		    				String errstr = ((errmsgFull != null) ? errmsgFull : errmsg);

		    				AlertDialog.show(ChannelAdd.this,
		    				  "Feed error", R.drawable.star_big_on, 
		    				  "An error was encountered while accessing the feed: " + errstr,
		    				  "OK", true);
		    			}
		    		});
				}			    	
			}
		};

		t.start();		
	}
}
