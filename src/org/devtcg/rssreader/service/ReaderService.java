/*
 * $Id: ReaderService.java 112 2008-02-14 00:34:50Z jasta00 $
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

import org.devtcg.rssreader.service.IReaderService;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;

public class ReaderService extends Service implements Runnable
{
	protected boolean mRunning;
	private static final String TAG = "RSSReaderService";
	
	@Override
	protected void onCreate()
	{
		Log.d(TAG, "onCreate");
	}
	
	@Override
	protected void onStart(int startId, Bundle args)
	{
		Log.d(TAG, "onStart(" + startId + ")");
		
		Thread t = new Thread(null, this, "RSSReaderService_Service");
		t.start();
		
		mRunning = true;
	}
	
	@Override
	protected void onDestroy()
	{
		/* TODO: Do something? */
		Log.d(TAG, "onDestroy");
	}
	
	public void run()
	{
		Log.d(TAG, "Doing some work, look at me!");

        // Normally we would do some work here...  for our sample, we will
        // just sleep for 4 seconds.
        long endTime = System.currentTimeMillis() + 4*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (mBinder) {
                try {
                    mBinder.wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }
        
        Log.d(TAG, "Finished, sigh...");
        
		/* Done synchronizing, stop our service.  We will be called up again
		 * at our next scheduled interval... */
		this.stopSelf();
		
		mRunning = false;
	}
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}
	
	private final IReaderService.Stub mBinder = new IReaderService.Stub()
	{
		public int getPid()
		{
			return Process.myPid();
		}
	};
}
