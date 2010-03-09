package com.olearyp.gusto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class SuServer extends IntentService {

	public SuServer() {
		super("SuServer");
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		String cmdString = Uri.parse(intent.toUri(0)).getSchemeSpecificPart();
		Log.v("GUSTO", "SuServer has received request for command '" + cmdString + "'.");
		//Toast.makeText(this, "Received command '" + cmdString + "'.", Toast.LENGTH_SHORT).show();
		super.onStart(intent, startId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		String cmdString = Uri.parse(intent.toUri(0)).getSchemeSpecificPart();
		Log.v("GUSTO", "SuServer is handling command '" + cmdString + "'.");
		Toast.makeText(this, "Handling command '" + cmdString + "'.", Toast.LENGTH_SHORT).show();
		final Process p;
		try {
			// Based on ideas from enomther et al.
			p = Runtime.getRuntime().exec("su -c sh");
			BufferedReader stdInput = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(
					new InputStreamReader(p.getErrorStream()));
			BufferedWriter stdOutput = new BufferedWriter(
					new OutputStreamWriter(p.getOutputStream()));

			stdOutput
					.write(". /system/bin/exp_script.sh.lib && read_in_ep_config && "
							+ cmdString + "; exit\n");
			stdOutput.flush();
			/*
			 * We need to asynchronously find out when this process is done
			 * so that we are able to see its interim output...so thread it
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
					//publishProgress(status);
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
	}
}
