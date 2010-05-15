/*
 * $Id: ReaderService_Setup.java 76 2007-12-06 00:58:36Z jasta00 $
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


import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentReceiver;
import android.os.SystemClock;
import android.util.Log;

public class ReaderService_Setup extends IntentReceiver
{
	public static final String TAG = "RSSReaderService_Setup";

	@Override
	public void onReceiveIntent(Context context, Intent intent)
	{
		Log.d(TAG, "onReceiveIntent");		
		setupAlarm(context);
	}
	
	public static void setupAlarm(Context context)
	{
		Log.d(TAG, "setupAlarm");
		
		/* Start our service via the IntentFilter dummy class. */
		Intent start = new Intent(context, ReaderService_Alarm.class);
		
		/* Every hour. */
		long interval = 50 * 1000;

		AlarmManager amStart = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		amStart.setRepeating(AlarmManager.ELAPSED_REALTIME,
		  SystemClock.elapsedRealtime() + interval, interval, start);
	}
}
