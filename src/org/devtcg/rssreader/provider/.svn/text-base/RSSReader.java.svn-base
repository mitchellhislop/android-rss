/*
 * $Id$
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

import android.net.Uri;
import android.provider.BaseColumns;

public final class RSSReader
{
	public static final String AUTHORITY = "org.devtcg.rssreader.provider.RSSReader";
	
	public interface Channels extends BaseColumns
	{
		/* URI for accessing a specific channel.  See Posts.CONTENT_URI_LIST
		 * for how to "view" the channel posts. */
		public static final Uri CONTENT_URI = 
		  Uri.parse("content://" + AUTHORITY + "/channels");
		
		public static final String DEFAULT_SORT_ORDER = "title ASC";
		
		/* User-controllable RSS channel name. */
		public static final String TITLE = "title";
		
		/* RSS Feed URL. */
		public static final String URL = "url";
		
		/* Site's favicon; usually a guess. */
		public static final String ICON = "icon";
		public static final String ICON_URL = "icon_url";
		
		/* Site's formal logo; derived from the XML feed. */
		public static final String LOGO = "logo";
	}
	
	public interface Posts extends BaseColumns
	{
		/* URI for accessing a specific post. */
		public static final Uri CONTENT_URI = 
		  Uri.parse("content://" + AUTHORITY + "/posts");
		
		/* URI for accessing a list of posts on a particular channel. */
		public static final Uri CONTENT_URI_LIST =
		  Uri.parse("content://" + AUTHORITY + "/postlist");
		
		public static final String DEFAULT_SORT_ORDER = "posted_on DESC";
		
		/* Reference to the channel _ID to which this post belongs. */
		public static final String CHANNEL_ID = "channel_id";
		
		/* Boolean read value. */
		public static final String READ = "read"; 
		
		/* Post subject. */
		public static final String TITLE = "title";
		
		/* Post author. */
		public static final String AUTHOR = "author";
		
		/* "Read more..." URL. */
		public static final String URL = "url";
		
		/* Post text. */
		public static final String BODY = "body";
	
		/* Date of the post. */
		public static final String DATE = "posted_on";
	}
}
