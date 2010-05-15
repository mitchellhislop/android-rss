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

package org.devtcg.rssreader.service;


import android.content.Context;
import android.content.Intent;
import android.content.IntentReceiver;

public class ReaderService_Alarm extends IntentReceiver
{
	@Override
	public void onReceiveIntent(Context context, Intent intent)
	{
		context.startService(new Intent(context, ReaderService.class), null);
	}
}
