/*
 * $Id: PostListRow.java 112 2008-02-14 00:34:50Z jasta00 $
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.devtcg.rssreader.provider.RSSReader;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class PostListRow extends ViewGroup
{
	private static final String TAG = "PostListRow";
	
	private static final int SUBJECT_ID = 1;
	private static final int DATE_ID = 2;

	private TextView mSubject;
	private TextView mDate;

	private Rect mRect;
	private Paint mGray;

	private static final SimpleDateFormat mDateFmtDB;
	private static final SimpleDateFormat mDateFmtToday;
	private static final SimpleDateFormat mDateFmt;

	static
	{
		mDateFmtDB = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		mDateFmtToday = new SimpleDateFormat("h:mma");

		/* TODO: Format date according to the current locale preference. */
		mDateFmt = new SimpleDateFormat("MM/dd/yyyy h:mma");
	}

	public PostListRow(Context context)
	{
		super(context);

		mRect = new Rect();
		mGray = new Paint();
		mGray.setStyle(Paint.Style.STROKE);
		mGray.setColor(0xff9c9e9c);

		mSubject = new TextView(context);
		mSubject.setId(SUBJECT_ID);

		LayoutParams subjectRules =
		  new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		addView(mSubject, subjectRules);

		mDate = new TextView(context);
		mDate.setId(DATE_ID);
		mDate.setTextColor(0xffaaaaaa);

		LayoutParams dateRules =
		  new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		addView(mDate, dateRules);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		int subjw = mSubject.getMeasuredWidth();
		int subjh = mSubject.getMeasuredHeight();
		int datew = mDate.getMeasuredWidth();
		int dateh = mDate.getMeasuredHeight();
		int selfw = getMeasuredWidth();
		int selfh = getMeasuredHeight();

		mSubject.layout(0, 0, subjw, subjh);
		mDate.layout(selfw - datew, selfh - (dateh + 4), selfw, selfh - 4);
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec)
	{
		int w = View.MeasureSpec.getSize(widthSpec);

		/* TODO: Honor mSubject LayoutParams()? */
		mSubject.measure(widthSpec, heightSpec);
		mDate.measure
		 (getChildMeasureSpec(widthSpec, 0, mDate.getLayoutParams().width),
		  getChildMeasureSpec(heightSpec, 0, mDate.getLayoutParams().height));

		int h;
		int lines = mSubject.getLineCount();

		if (lines <= 1)
			h = mSubject.getMeasuredHeight() + mDate.getMeasuredHeight();
		else
		{
			h = mSubject.getMeasuredHeight();

			/* Attempt to figure out if the last line "bleeds" into the date.
			 * If it does, we need to arbitrarily force our layout one line
			 * longer. */
			float linew = mSubject.getLayout().getLineRight(lines - 1);	

			if ((linew + 10) > (w - mDate.getMeasuredWidth()))
				h += mDate.getMeasuredHeight();
		}

		/* Add a bottom 4px padding. */
		setMeasuredDimension(w, h + 4);
	}

	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		Rect r = mRect;

		getDrawingRect(r);
		canvas.drawLine(r.left, r.bottom - 1, r.right, r.bottom - 1, mGray);

		super.dispatchDraw(canvas);
	}

	public void bindView(Cursor cursor)
	{
		if (cursor.getInt(cursor.getColumnIndex(RSSReader.Posts.READ)) != 0)
			mSubject.setTypeface(Typeface.DEFAULT);
		else
			mSubject.setTypeface(Typeface.DEFAULT_BOLD);

		mSubject.setText(cursor, cursor.getColumnIndex(RSSReader.Posts.TITLE));

		String datestr = cursor.getString(cursor.getColumnIndex(RSSReader.Posts.DATE));

		try
		{
			Date date = mDateFmtDB.parse(datestr);

			Calendar then = new GregorianCalendar();
			then.setTime(date);

			Calendar now = new GregorianCalendar();

			SimpleDateFormat fmt;

			if (now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR))
				fmt = mDateFmtToday;
			else
				fmt = mDateFmt;

			mDate.setText(fmt.format(date));
		}
		catch (ParseException e)
		{
			Log.d(TAG, Log.getStackTraceString(e));
		}		
	}
}
