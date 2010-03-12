package com.olearyp.gusto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

public class DownloadPreference extends Preference {

	private URL uri;
	private String destination = "";
	private ProgressBar pb;
	public URL getUri() {
		return uri;
	}

	public void setUri(URL uri) {
		this.uri = uri;
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
		this.setOnPreferenceClickListener(new Downloader());
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
		final View v = super.onCreateView(parent);
		pb = (ProgressBar) v.findViewById(R.id.progress);
		return v;
	}


	private class Downloader implements OnPreferenceClickListener {

		@Override
		public boolean onPreferenceClick(Preference preference) {
			pb.setProgress(pb.getProgress() + 10);
//			try {
//				HttpURLConnection c;
//				c = (HttpURLConnection) uri.openConnection();
//				c.setRequestMethod("GET");
//				c.setDoOutput(true);
//				c.connect();
//				FileOutputStream f = new FileOutputStream(new File(destination));
//
//				InputStream in = c.getInputStream();
//
//				byte[] buffer = new byte[1024];
//				int len1 = 0;
//				while ((len1 = in.read(buffer)) > 0) {
//					f.write(buffer, 0, len1);
//				}
//				in.close();
//				f.close();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}

			return true;
		}
	}
}
