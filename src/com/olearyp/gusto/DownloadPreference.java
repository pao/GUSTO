/* Copyright 2010 Patrick O'Leary.  All rights reserved.
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only.
 * See the file CDDL.txt in this distribution or
 * http://opensource.org/licenses/cddl1.php for details.
 */

package com.olearyp.gusto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

public class DownloadPreference extends Preference {

	private String url;
	private String destination = "";
	private View v;

	enum InstallChoice {
		INSTALL, UNINSTALL
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
		this.setWidgetLayoutResource(R.layout.download_progress);
	}

	public void setParams(String url, String destination) {
		this.url = url;
		this.destination = destination;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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
		if (this.getOnPreferenceClickListener() != null
				&& ((DownloadPreferenceListener) this
						.getOnPreferenceClickListener()).isDownloaded()) {
			((CheckBox) v.findViewById(R.id.CheckBox)).setChecked(true);
		}
		return v;
	}

	public class DownloadPreferenceListener implements
			OnPreferenceClickListener {
		protected boolean isDownloaded = false;
		protected Expsetup ep = null;

		public DownloadPreferenceListener(boolean isDownloaded, Expsetup ep) {
			super();
			this.isDownloaded = isDownloaded;
			this.ep = ep;
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (!isDownloaded) {
				((ViewSwitcher) v.findViewById(R.id.ViewSwitcher)).showNext();
				new Downloader((ProgressBar) v.findViewById(R.id.ProgressBar))
						.execute((Void) null);
				isDownloaded = true;
				return true;
			}
			return false;
		}

		public boolean isDownloaded() {
			return isDownloaded;
		}

		protected class Downloader extends AsyncTask<Void, Integer, Void> {
			protected static final long update_block_size = 4096;
			private ProgressBar pb;

			public Downloader(ProgressBar pb) {
				super();
				this.pb = pb;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				pb.setIndeterminate(false);
				pb.setProgress(values[0]);
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(Void result) {
				((ViewSwitcher) v.findViewById(R.id.ViewSwitcher))
						.showPrevious();
				((CheckBox) v.findViewById(R.id.CheckBox)).setChecked(true);
				pb.setIndeterminate(true);
				super.onPostExecute(result);
			}

			@Override
			protected Void doInBackground(Void... params) {
				HttpClient client = new DefaultHttpClient();
				try {
					HttpGet method = new HttpGet(url);
					ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {
						@Override
						public Void handleResponse(HttpResponse response)
								throws ClientProtocolException, IOException {
							HttpEntity entity = response.getEntity();
							Long fileLen = entity.getContentLength();
							if (entity != null) {
								InputStream filecont = entity.getContent();
								ReadableByteChannel dl_chan = Channels
										.newChannel(filecont);
								File dst = new File(destination);
								dst.getParentFile().mkdirs();
								dst.createNewFile();
								FileOutputStream fout = new FileOutputStream(
										dst);
								FileChannel fout_chan = fout.getChannel();

								int bytesRead = 0;
								while (bytesRead < fileLen) {
									bytesRead += fout_chan.transferFrom(
											dl_chan, bytesRead,
											update_block_size);
									publishProgress(Math.round(bytesRead
											/ fileLen.floatValue() * 100));
								}
							}
							return null;
						}
					};
					client.execute(method, responseHandler);
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				client.getConnectionManager().shutdown();
				return null;
			}
		}
	}

	public class VsappPreferenceListener extends DownloadPreferenceListener {

		public VsappPreferenceListener(boolean isDownloaded, Expsetup ep) {
			super(isDownloaded, ep);
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			if (!isDownloaded) {
				((ViewSwitcher) v.findViewById(R.id.ViewSwitcher)).showNext();
				new Downloader((ProgressBar) v.findViewById(R.id.ProgressBar)) {
					@Override
					protected void onPostExecute(Void result) {
						manageVsapp(InstallChoice.INSTALL);
						super.onPostExecute(result);
					}
				}.execute((Void) null);
				isDownloaded = true;
				return true;
			} else {
				new AlertDialog.Builder(ep)
						.setTitle("VSAPP maintenance")
						.setPositiveButton("Reinstall", new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								manageVsapp(InstallChoice.INSTALL);
							}
						})
						.setNeutralButton("Uninstall", new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								manageVsapp(InstallChoice.UNINSTALL);
							}
						})
						.setNegativeButton("Purge", new OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								manageVsapp(InstallChoice.UNINSTALL);
								((CheckBox) v.findViewById(R.id.CheckBox))
										.setChecked(false);
								new File(destination).delete();
								isDownloaded = false;
							}
						})
						.setMessage(
								"You may reinstall this VSAPP, uninstall it, "
										+ "uninstall it and delete its installation package (purge), "
										+ "or press the BACK button to cancel.")
						.show();
			}
			return true;
		}

		private void manageVsapp(InstallChoice install_uninstall) {
			final File file = new File(destination);
			final String epvspath = file.getParentFile().getParent()
					+ "/" + file.getName();
			// File.renameTo() was found to be unreliable here
			final String mvincmd = "mv " + destination + " " + epvspath;
			final String mvoutcmd = "mv " + epvspath + " " + destination;
			if (install_uninstall == InstallChoice.INSTALL) {
				ep.sendCommand(mvincmd + " && install_vsapps && " + mvoutcmd,
						"installing VSAPP", "none");
			} else {
				ep.sendCommand(mvincmd + " && uninstall_vsapps && " + mvoutcmd,
						"uninstalling VSAPP", "none");
			}
		}
	}
}
