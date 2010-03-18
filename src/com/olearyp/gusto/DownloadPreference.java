/*
 * Copyright 2010 Patrick O'Leary. All rights reserved. The contents of this file are subject to the
 * terms of the Common Development and Distribution License, Version 1.0 only. See the file CDDL.txt
 * in this distribution or http://opensource.org/licenses/cddl1.php for details.
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

	@Override
	protected View onCreateView(final ViewGroup parent) {
		v = super.onCreateView(parent);
		if (getOnPreferenceClickListener() != null
			&& ((DownloadPreferenceListener) getOnPreferenceClickListener()).isDownloaded()) {
			((CheckBox) v.findViewById(R.id.CheckBox)).setChecked(true);
		}
		return v;
	}

	public class DownloadPreferenceListener implements OnPreferenceClickListener {
		protected boolean isDownloaded = false;
		protected Expsetup ep = null;

		public DownloadPreferenceListener(
			final boolean isDownloaded, final Expsetup ep) {
			super();
			this.isDownloaded = isDownloaded;
			this.ep = ep;
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (!isDownloaded) {
				((ViewSwitcher) v.findViewById(R.id.ViewSwitcher)).showNext();
				new Downloader((ProgressBar) v.findViewById(R.id.ProgressBar)).execute((Void) null);
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
			private final ProgressBar pb;

			public Downloader(
				final ProgressBar pb) {
				super();
				this.pb = pb;
			}

			@Override
			protected void onProgressUpdate(final Integer... values) {
				pb.setIndeterminate(false);
				pb.setProgress(values[0]);
				super.onProgressUpdate(values);
			}

			@Override
			protected void onPostExecute(final Void result) {
				((ViewSwitcher) v.findViewById(R.id.ViewSwitcher)).showPrevious();
				((CheckBox) v.findViewById(R.id.CheckBox)).setChecked(true);
				pb.setIndeterminate(true);
				super.onPostExecute(result);
			}

			@Override
			protected Void doInBackground(final Void... params) {
				final HttpClient client = new DefaultHttpClient();
				try {
					final HttpGet method = new HttpGet(url);
					final ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {
						@Override
						public Void handleResponse(final HttpResponse response)
							throws ClientProtocolException, IOException {
							final HttpEntity entity = response.getEntity();
							final Long fileLen = entity.getContentLength();
							if (entity != null) {
								final InputStream filecont = entity.getContent();
								final ReadableByteChannel dl_chan = Channels.newChannel(filecont);
								final File dst = new File(destination);
								dst.getParentFile().mkdirs();
								dst.createNewFile();
								final FileOutputStream fout = new FileOutputStream(dst);
								final FileChannel fout_chan = fout.getChannel();
								int bytesRead = 0;
								while (bytesRead < fileLen) {
									bytesRead += fout_chan.transferFrom(dl_chan, bytesRead,
										update_block_size);
									publishProgress(Math.round(bytesRead / fileLen.floatValue()
										* 100));
								}
							}
							return null;
						}
					};
					client.execute(method, responseHandler);
				} catch (final ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				client.getConnectionManager().shutdown();
				return null;
			}
		}
	}

	public class RamhackPreferenceListener extends DownloadPreferenceListener {
		public RamhackPreferenceListener(
			final boolean isDownloaded, final Expsetup ep) {
			super(isDownloaded, ep);
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (!isDownloaded) {
				new AlertDialog.Builder(ep).setTitle("Install ramhack").setMessage(
					"The \"ramhack\" kernel will allocate ~10 MB of RAM currently assigned to "
						+ "video memory to the main memory pool. This may improve performance "
						+ "under normal circumstances at the cost of 3D performance.\n\n"
						+ "This operation cannot be undone except by reflashing the "
						+ "expansion pack.").setPositiveButton("Install ramhack",
					new OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							((ViewSwitcher) v.findViewById(R.id.ViewSwitcher)).showNext();
							new Downloader((ProgressBar) v.findViewById(R.id.ProgressBar)) {
								@Override
								protected void onPostExecute(final Void result) {
									ep.sendCommand(
										"echo 'boot-recovery' > /cache/recovery/command && "
											+ "echo '--update_package=SDCARD:"
											+ new File(destination).getName()
											+ "' >> /cache/recovery/command", "preparing kernel",
										ep.getString(R.string.reboot_recovery_required));
									super.onPostExecute(result);
								}
							}.execute((Void) null);
							isDownloaded = true;
						}
					}).setNegativeButton("Do not install", null).show();
				return true;
			} else {
				new AlertDialog.Builder(ep).setTitle("Ramhack removal").setMessage(
					"To remove the ramhack kernel, you must manually "
						+ "reflash the expansion pack from recovery, "
						+ "reboot to Android, then flash themes, "
						+ "etc. as desired.  Reboot to recovery now?").setPositiveButton(
					"Reboot to recovery", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							ep.sendCommand(ep.getString(R.string.reboot_recovery), "rebooting",
								"none");
						}
					}).setNegativeButton("Return", null).show();
			}
			return true;
		}
	}

	public class VsappPreferenceListener extends DownloadPreferenceListener {
		public VsappPreferenceListener(
			final boolean isDownloaded, final Expsetup ep) {
			super(isDownloaded, ep);
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (!isDownloaded) {
				((ViewSwitcher) v.findViewById(R.id.ViewSwitcher)).showNext();
				new Downloader((ProgressBar) v.findViewById(R.id.ProgressBar)) {
					@Override
					protected void onPostExecute(final Void result) {
						manageVsapp(InstallChoice.INSTALL);
						super.onPostExecute(result);
					}
				}.execute((Void) null);
				isDownloaded = true;
				return true;
			} else {
				new AlertDialog.Builder(ep).setTitle("VSAPP maintenance").setPositiveButton(
					"Reinstall", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							manageVsapp(InstallChoice.INSTALL);
						}
					}).setNeutralButton("Uninstall", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						manageVsapp(InstallChoice.UNINSTALL);
					}
				}).setNegativeButton("Purge", new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						manageVsapp(InstallChoice.UNINSTALL);
						((CheckBox) v.findViewById(R.id.CheckBox)).setChecked(false);
						new File(destination).delete();
						isDownloaded = false;
					}
				}).setMessage(
					"You may reinstall this VSAPP, uninstall it, "
						+ "uninstall it and delete its installation package (purge), "
						+ "or press the BACK button to cancel.").show();
			}
			return true;
		}

		private void manageVsapp(final InstallChoice install_uninstall) {
			final File file = new File(destination);
			final String epvspath = file.getParentFile().getParent() + "/" + file.getName();
			// File.renameTo() was found to be unreliable here
			final String mvincmd = "mv " + destination + " " + epvspath;
			final String mvoutcmd = "mv " + epvspath + " " + destination;
			if (install_uninstall == InstallChoice.INSTALL) {
				ep.sendCommand(mvincmd + " && install_vsapps ; " + mvoutcmd, "installing VSAPP",
					"none");
			} else {
				ep.sendCommand(mvincmd + " && uninstall_vsapps ; " + mvoutcmd,
					"uninstalling VSAPP", "none");
			}
		}
	}
}
