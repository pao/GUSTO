package com.olearyp.gusto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class SuServer extends IntentService {

	final Handler mHandler = new Handler();

	private String serverState;

	private NotificationManager nm = null;
	private static String[] stateIndex = { "none", "reboot-required",
			"reboot-recovery-required" };

	public SuServer() {
		super("SuServer");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		String cmdString = Uri.parse(intent.toUri(0)).getSchemeSpecificPart();
		if (Uri.parse(intent.toUri(0)).getScheme().equals("commandid")) {
			cmdString = getString(Integer.parseInt(cmdString));
		}
		Log.v("GUSTO", "SuServer has received request for command '"
				+ cmdString + "'.");
		// Toast.makeText(this, "Received command '" + cmdString + "'.",
		// Toast.LENGTH_SHORT).show();
		super.onStart(intent, startId);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		// Create or modify reboot notification, if needed
		//nm.
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		String cmdString = Uri.parse(intent.toUri(0)).getSchemeSpecificPart();
		if (Uri.parse(intent.toUri(0)).getScheme().equals("commandid")) {
			cmdString = getString(Integer.parseInt(cmdString));
		}
		final String state = intent.getStringExtra("com.olearyp.gusto.STATE");
		final String preExecuteIntent = intent
				.getStringExtra("com.olearyp.gusto.PRE_EX_INTENT");
		final String preExecuteUri = intent
				.getStringExtra("com.olearyp.gusto.PRE_EX_URI");
		final String postExecuteIntent = intent
				.getStringExtra("com.olearyp.gusto.POST_EX_INTENT");
		final String postExecuteUri = intent
				.getStringExtra("com.olearyp.gusto.POST_EX_URI");

		if (preExecuteIntent != null) {
			Intent intentX = new Intent(preExecuteIntent);
			intentX.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intentX.setData(Uri.parse(preExecuteUri));
			startActivity(intentX);
		}

		Log.v("GUSTO", "SuServer is handling command '" + cmdString + "'.");
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
					.write(". /system/bin/exp_script.sh.lib && read_in_ep_config && "
							+ cmdString + "; exit\n");
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
			Intent intentX = new Intent(postExecuteIntent);
			intentX.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intentX.setData(Uri.parse(postExecuteUri));
			startActivity(intentX);
		}

		if (state != null) {
			setServerState(state);
		}

		final String cmdCopy = cmdString;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(
						SuServer.this,
						"State is now '" + serverState + "'. Command was '"
								+ cmdCopy + "'.", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private static final int getIndex(String specificValue) {
		for (int i = 0; i < stateIndex.length; i++) {
			if (stateIndex[i].equals(specificValue)) {
				return i;
			}
		}
		return -1;
	}

	private void setServerState(String state) {
		if (getIndex(state) > getIndex(serverState)) {
			this.serverState = state;
		}
	}
}
