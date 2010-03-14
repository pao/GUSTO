/* Copyright 2010 Patrick O'Leary.  All rights reserved.
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only.
 * See the file CDDL.txt in this distribution or
 * http://opensource.org/licenses/cddl1.php for details.
 */

package com.olearyp.gusto;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class DownloadPreference extends Preference {

	private URL url;
	private String destination = "";
	private View v;

	public URL getUri() {
		return url;
	}

	public void setUri(URL uri) {
		this.url = uri;
	}

	public DownloadPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public DownloadPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public DownloadPreference(Context context) {
		super(context);
		init();
	}

	private void init() {
		this.setOnPreferenceClickListener(new DownloadPreferenceListener());
		this.setWidgetLayoutResource(R.layout.download_progress);
	}

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		v = super.onCreateView(parent);
		return v;
	}

	private class DownloadPreferenceListener implements OnPreferenceClickListener {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			ProgressBar pb = (ProgressBar) v.findViewById(R.id.progress);
			pb.setProgress(pb.getProgress() + 10);
			// Create an instance of HttpClient.

			return true;
		}
	}
	
	private class Downloader extends AsyncTask<Object, Integer, Void> {

		@Override
		protected void onProgressUpdate(Integer... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}

		@Override
		protected Void doInBackground(Object... params) {
			HttpClient client = new DefaultHttpClient();

			try {
				HttpGet method = new HttpGet(url.toURI());
				ResponseHandler<ByteBuffer> responseHandler = new ResponseHandler<ByteBuffer>() {

					@Override
					public ByteBuffer handleResponse(HttpResponse response)
							throws ClientProtocolException, IOException {
						Header clh = response.getFirstHeader("content-length");
						if(clh == null) {
							return null;
						}
						HttpEntity entity = response.getEntity();
						if(entity != null) {
							
						}
						// TODO Auto-generated method stub
						return null;
					}

				};
				ByteBuffer responseBody = client.execute(method,
						responseHandler);
				
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			client.getConnectionManager().shutdown();
			return null;
		}
		
	}
}
