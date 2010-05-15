/*
 * $Id: ChannelRefresh.java 112 2008-02-14 00:34:50Z jasta00 $
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
 *
 * TODO: This class needs to be generalized much better, with specialized
 * parsers for Atom 1.0, Atom 0.3, RSS 0.91, RSS 1.0 and RSS 2.0.  Hell,
 * this whole thing needs to be chucked and redone.
 */

package org.devtcg.rssreader.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.devtcg.rssreader.provider.RSSReader;
import org.devtcg.rssreader.util.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class ChannelRefresh extends DefaultHandler
{
	private static final String TAG = "RSSChannelRefresh";

	private Handler mHandler;
	private long mID;
	private String mRSSURL;

	private ContentResolver mContent;

	/* Buffer post information as we learn it in STATE_IN_ITEM. */
	private ChannelPost mPostBuf;

	/* Efficiency is the name of the game here... */
	private int mState;
	private static final int STATE_IN_ITEM = (1 << 2);
	private static final int STATE_IN_ITEM_TITLE = (1 << 3);
	private static final int STATE_IN_ITEM_LINK = (1 << 4);
	private static final int STATE_IN_ITEM_DESC = (1 << 5);
	private static final int STATE_IN_ITEM_DATE = (1 << 6);
	private static final int STATE_IN_ITEM_AUTHOR = (1 << 7);
	private static final int STATE_IN_TITLE = (1 << 8);

	private static HashMap<String, Integer> mStateMap;

	static
	{
		mStateMap = new HashMap<String, Integer>();
		mStateMap.put("item", new Integer(STATE_IN_ITEM));
		mStateMap.put("entry", new Integer(STATE_IN_ITEM));
		mStateMap.put("title", new Integer(STATE_IN_ITEM_TITLE));
		mStateMap.put("link", new Integer(STATE_IN_ITEM_LINK));
		mStateMap.put("description", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("content", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("content:encoded", new Integer(STATE_IN_ITEM_DESC));
		mStateMap.put("dc:date", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("updated", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("pubDate", new Integer(STATE_IN_ITEM_DATE));
		mStateMap.put("dc:author", new Integer(STATE_IN_ITEM_AUTHOR));
		mStateMap.put("author", new Integer(STATE_IN_ITEM_AUTHOR));
	}

	public ChannelRefresh(ContentResolver resolver)
	{
		super();
		mContent = resolver;
	}

	/*
	 * Note that if syncDB is called with id == -1, this class will interpret
	 * that to mean that a new channel is being added (and also tested) so
	 * the first meaningful piece of data encountered will trigger an insert
	 * into the database.
	 *
	 * This logic is all just terrible, but this entire class needs to be
	 * scrapped and redone to make room for improved cooperation with the rest
	 * of the application.
	 */
	public long syncDB(Handler h, long id, String rssurl)
	  throws Exception
	{
		mHandler = h;
		mID = id;
		mRSSURL = rssurl;

		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();

		xr.setContentHandler(this);
		xr.setErrorHandler(this);

		URL url = new URL(mRSSURL);
		
		URLConnection c = url.openConnection();
		c.setRequestProperty("User-Agent", "Android/m3-rc37a");
		xr.parse(new InputSource(c.getInputStream()));

		return mID;
	}

	public boolean updateFavicon(long id, String iconUrl)
	  throws MalformedURLException
	{
		return updateFavicon(id, new URL(iconUrl));
	}

	public boolean updateFavicon(long id, URL iconUrl)
	{
		InputStream stream = null;
		OutputStream ico = null;

		boolean r = false;
		
		Uri iconUri = RSSReader.Channels.CONTENT_URI
			.buildUpon()
			.appendPath(String.valueOf(id))
			.appendPath("icon")
			.build();

		try
		{
			stream = iconUrl.openStream();

			ico = mContent.openOutputStream(iconUri);

			byte[] b = new byte[1024];

			int n;
			while ((n = stream.read(b)) != -1)
				ico.write(b, 0, n);

			r = true;
		}
		catch (Exception e)
		{
			Log.d(TAG, Log.getStackTraceString(e));
		}
		finally
		{
			try
			{
				if (stream != null)
					stream.close();

				if (ico != null)
					ico.close();
			}
			catch (IOException e) { }
		}

		return r;
	}

	public void startElement(String uri, String name, String qName,
			Attributes attrs)
	{
		/* HACK: when we see <title> outside of an <item>, assume it's the
		 * feed title.  Only do this when we are inserting a new feed. */
		if (mID == -1 &&
		    qName.equals("title") && (mState & STATE_IN_ITEM) == 0)
		{
			mState |= STATE_IN_TITLE;
			return;
		}

		Integer state = mStateMap.get(qName);

		if (state != null)
		{
			mState |= state.intValue();

			if (state.intValue() == STATE_IN_ITEM)
				mPostBuf = new ChannelPost();
			else if ((mState & STATE_IN_ITEM) != 0 && state.intValue() == STATE_IN_ITEM_LINK)
			{
				String href = attrs.getValue("href");

				if (href != null)
					mPostBuf.link = href;
			}
		}
	}

	public void endElement(String uri, String name, String qName)
	{
		Integer state = mStateMap.get(qName);

		if (state != null)
		{
			mState &= ~(state.intValue());

			if (state.intValue() == STATE_IN_ITEM)
			{
				if (mID == -1)
				{
					Log.d(TAG, "Oops, </item> found before feed title and our parser sucks too much to deal.");
					return;
				}

				String[] dupProj =
				  new String[] { RSSReader.Posts._ID };

				Uri listURI =
				  ContentUris.withAppendedId(RSSReader.Posts.CONTENT_URI_LIST, mID);

				Cursor dup = mContent.query(listURI,
					dupProj, "title = ? AND url = ?",
					new String[] { mPostBuf.title, mPostBuf.link}, null);

				Log.d(TAG, "Post: " + mPostBuf.title);

				if (dup.count() == 0)
				{
					ContentValues values = new ContentValues();

					values.put(RSSReader.Posts.CHANNEL_ID, mID);
					values.put(RSSReader.Posts.TITLE, mPostBuf.title);
					values.put(RSSReader.Posts.URL, mPostBuf.link);
					values.put(RSSReader.Posts.AUTHOR, mPostBuf.author);
					values.put(RSSReader.Posts.DATE, mPostBuf.getDate());
					values.put(RSSReader.Posts.BODY, mPostBuf.desc);

					mContent.insert(RSSReader.Posts.CONTENT_URI, values);
				}

				dup.close();
			}
		}
	}

	public void characters(char ch[], int start, int length)
	{
		/* HACK: This is the other side of the above hack in startElement. */
		if (mID == -1 && (mState & STATE_IN_TITLE) != 0)
		{
			ContentValues values = new ContentValues();

			values.put(RSSReader.Channels.TITLE, new String(ch, start, length));
			values.put(RSSReader.Channels.URL, mRSSURL);

			Uri added =
			  mContent.insert(RSSReader.Channels.CONTENT_URI, values);

			mID = Long.parseLong(added.getPathSegments().get(1));

			/* There's no reason we need to do this ever, but we'll just be
			 * good about removing this awful hack from runtime data. */
			mState &= ~STATE_IN_TITLE;

			return;
		}

		if ((mState & STATE_IN_ITEM) == 0)
			return;

		/*
		 * We sort of pretended that mState was inclusive, but really only
		 * STATE_IN_ITEM is inclusive here.  This is a goofy design, but it is
		 * done to make this code a bit simpler and more efficient.
		 */
		switch (mState)
		{
		case STATE_IN_ITEM | STATE_IN_ITEM_TITLE:
			mPostBuf.title = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_DESC:
			mPostBuf.desc = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_LINK:
			mPostBuf.link = new String(ch, start, length);
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_DATE:
			mPostBuf.setDate(new String(ch, start, length));
			break;
		case STATE_IN_ITEM | STATE_IN_ITEM_AUTHOR:
			mPostBuf.author = new String(ch, start, length);
			break;
		default:
			/* Don't care... */
		}
	}

	/* TODO: Create org.devtcg.provider.dao.* classes for this. */
	private class ChannelPost
	{
		public String title;
		public Date date;
		public String desc;
		public String link;
		public String author;

		public ChannelPost()
		{
			/* Empty. */
		}

		public void setDate(String str)
		{
			date = DateUtils.parseDate(str);

			if (date == null)
				date = new Date();
		}

		public String getDate()
		{
			return DateUtils.formatDate(mPostBuf.date);
		}
	}
}
