/*
 * Copyright 2010 Patrick O'Leary. All rights reserved. The contents of this file are subject to the
 * terms of the Common Development and Distribution License, Version 1.0 only. See the file CDDL.txt
 * in this distribution or http://opensource.org/licenses/cddl1.php for details.
 */
package com.olearyp.gusto;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.olearyp.gusto.Expsetup.DownloadPreferenceListener;

public class DownloadPreference extends Preference {
	private String url;
	private String destination = "";
	private View v;

	public DownloadPreference(
		final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public DownloadPreference(
		final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public DownloadPreference(
		final Context context) {
		super(context);
		init();
	}

	private void init() {
		setWidgetLayoutResource(R.layout.download_progress);
	}

	public void setParams(final String url, final String destination) {
		this.url = url;
		this.destination = destination;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(final String destination) {
		this.destination = destination;
	}

	public View getView() {
		return v;
	}

	@Override
	protected View onCreateView(final ViewGroup parent) {
		v = super.onCreateView(parent);
		if (getOnPreferenceClickListener() != null
			&& ((DownloadPreferenceListener) getOnPreferenceClickListener()).isDownloaded()) {
			((CheckBox) getView().findViewById(R.id.CheckBox)).setChecked(true);
		}
		return getView();
	}
}
