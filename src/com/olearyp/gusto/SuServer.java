/* Copyright 2010 Patrick O'Leary.  All rights reserved.
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only.
 * See the file CDDL.txt in this distribution or
 * http://opensource.org/licenses/cddl1.php for details.
 */

package com.olearyp.gusto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class SuServer extends IntentService {

	public class UnsupportedCommandSchemeException extends Exception {
		private static final long serialVersionUID = 7351188199664441783L;

		private String scheme = "?";
		
		public UnsupportedCommandSchemeException(String scheme) {
			this.scheme = scheme;
		}

		@Override
		public String getMessage() {
			return "Command scheme '" + scheme + "' is not supported by " + SuServer.class.getName() + ".";
		}

	}


	private static final int STATUS_NOTIFICATION = 0x0057A705;

	private NotificationManager nm = null;


	public SuServer() {
		super("SuServer");
	}

	@Override
	public void onCreate() {
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// We're done, kill the "running" notification
		nm.cancel(STATUS_NOTIFICATION);
		// Create or modify reboot notification, if needed
		super.onDestroy();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		String cmdString = Uri.parse(intent.toUri(0)).getSchemeSpecificPart();
		if (Uri.parse(intent.toUri(0)).getScheme().equals("commandid")) {
			cmdString = getString(Integer.parseInt(cmdString));
		}
		Log.v(getString(R.string.app_name), "SuServer has received request for command '"
				+ cmdString + "'.");
		super.onStart(intent, startId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String cmdString = Uri.parse(intent.toUri(0)).getSchemeSpecificPart();
		final String scheme = Uri.parse(intent.toUri(0)).getScheme();
		if (scheme.equals("commandid")) {
			cmdString = getString(Integer.parseInt(cmdString));
		} else if (!scheme.equals("command")) {
			try {
				throw new UnsupportedCommandSchemeException(scheme);
			} catch (UnsupportedCommandSchemeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		final PendingIntent postExecuteIntent = (PendingIntent) intent
				.getParcelableExtra("com.olearyp.gusto.POST_EX_INTENT");
		Notification note = (Notification) intent
				.getParcelableExtra("com.olearyp.gusto.RUN_NOTIFICATION");

		Log.v(getString(R.string.app_name), "SuServer is handling command '" + cmdString + "'.");

		if (note != null) {
			nm.notify(STATUS_NOTIFICATION, note);
		}

		final Process p;
		try {
			// Based on ideas from enomther et al.
			p = Runtime.getRuntime().exec("su -c sh");
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));
			BufferedWriter stdOutput = new BufferedWriter(
					new OutputStreamWriter(p.getOutputStream()));

			stdOutput
					.write(cmdString + "; exit\n");
			stdOutput.flush();
			/*
			 * We need to asynchronously find out when this process is done so
			 * that we are able to see its interim output...so thread it
			 */
			Thread t = new Thread() {
				public void run() {
					try {
						p.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			t.start();

			// Poor man's select()
			while (t.isAlive()) {
				String status = stdInput.readLine();
				if (status != null) {
					// publishProgress(status);
				}
				Thread.sleep(20);
			}

			stdInput.close();
			stdError.close();
			stdOutput.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (postExecuteIntent != null) {
			try {
				postExecuteIntent.send();
			} catch (CanceledException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
