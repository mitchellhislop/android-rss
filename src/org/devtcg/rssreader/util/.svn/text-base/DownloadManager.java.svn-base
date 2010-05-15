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

package org.devtcg.rssreader.util;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Handler;
import android.util.Log;

/**
 * Generic network connection manager to gracefully throttle and control
 * unpredictable download threads.
 *
 * This class implements a strategy to gracefully satisfy the "refresh all"
 * semantics.  The default behaviour is to spawn a new connection {@link
 * Thread} every 8 seconds or whenever the previously queued {@link Thread}
 * finishes execution.  There is no specific awareness of what action is
 * being performed by the spawned {@link Thread}, nor is it even necessary that
 * the {@link Thread} utilize the network.
 *
 * @see Thread
 * @see android.os.Handler
 */
public class DownloadManager
{
	private static final String TAG = "DownloadManager";
	private static final int TIMEOUT = 8000;

	private Handler mHandler;

	/**
	 * Holds objects transferred from mQueue once they have been started.
	 * Entries will be removed when the thread finishes.
	 */
	private ConcurrentLinkedQueue<DownloadWrapper> mThreads =
	  new ConcurrentLinkedQueue<DownloadWrapper>();

	/**
	 * Holds the currently inactive Runnable objects waiting to run.
	 */
	private ConcurrentLinkedQueue<Runnable> mQueue =
	  new ConcurrentLinkedQueue<Runnable>();

	/**
	 * Associate a new DownloadManager instance with a thread's message
	 * queue.
	 *
	 * @param handler       {@link Handler} instance
	 */
	public DownloadManager(Handler handler)
	{
		mHandler = handler;
	}

	/**
	 * Schedule a new download worker.
	 *
	 * @param r 
	 *   Worker Runnable to be scheduled.
	 *
	 * @return
	 *   If true, <code>r</code> is spawned immediately.  Otherwise, the
	 *   scheduler is invoked, and <code>r</code> will run when appropriate.
	 */
	public boolean schedule(Runnable r)
	{
		mQueue.add(r);

		/* Nothing is going on yet, so start it all up. */
		if (mThreads.size() == 0)
		{
			mHandler.removeCallbacks(mWakeUp);
			mWakeUp.run();			
			return true;
		}

		return false;
	}

	private Runnable mWakeUp = new Runnable()
	{
		public void run()
		{
			Runnable r = mQueue.poll();

			if (r == null)
			{
				Log.d(TAG, "Hmm, woke up with nothing to do?  Sure.");
				return;
			}
			else
			{
				Log.d(TAG, "Yay, here I am, with work to do: thread#=" + mThreads.size() + ", queue#=" + mQueue.size());
			}

			/* Flag all currently processing workers as being too slow, so that
			 * we know not to wake up when they finish.  This would cause
			 * connections that take just slightly longer than TIMEOUT to add a
			 * new concurrent connection every TIMEOUT msec. */
			for (DownloadWrapper t: mThreads)
				t.tooSlow();

			DownloadWrapper t = new DownloadWrapper(mThreads, mHandler, r);
			mThreads.add(t);
			t.start();

			/* Schedule a "check-up" in TIMEOUT msec.  This will be
			 * bumped if `t` finishes within that window.  */
			mHandler.removeCallbacks(this);
			mHandler.postDelayed(this, TIMEOUT);
		}
	};

	/**
	 * Wraps the invocation of queued Runnables so that we can trap their exit.
	 * Normally, this would be done with a Handler, but our implementation is
	 * non-invasive to the Activity's Handler.
	 */
	private class DownloadWrapper extends Thread
	{
		Collection<DownloadWrapper> mThreads;
		Handler mHandler;
		Runnable mWrapped;

		private boolean tooSlow = false;

		public DownloadWrapper(Collection<DownloadWrapper> active, Handler handler, Runnable wrap)
		{
			mThreads = active;
			mHandler = handler;
			mWrapped = wrap;
		}

		public void tooSlow()
		{
			Log.d(TAG, "Too slow...");
			tooSlow = true;
		}

		public void run()
		{
			mWrapped.run();

			/* There may be another entry waiting in the queue for this one to
			 * finish.  So, bump the wait callback up to run now. */
			mThreads.remove(this);
			
			if (tooSlow == false)
			{
				mHandler.removeCallbacks(mWakeUp);
				mHandler.post(mWakeUp);
			}
		}
	}
}
