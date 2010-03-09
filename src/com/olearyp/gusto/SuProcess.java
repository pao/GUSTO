/**
 * 
 */
package com.olearyp.gusto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;

class SuProcess extends AsyncTask<String, String, Void> {

	private final Activity caller;

	final Handler mHandler = new Handler();
	public Handler getHandler() {
		return mHandler;
	}

	/**
	 * @param caller
	 */
	SuProcess(Activity caller) {
		this.caller = caller;
	}

	private ProgressDialog pd;

	@Override
	protected void onPreExecute() {
		pd = ProgressDialog.show(this.caller, "Working",
				"Starting process...", true, false);
	}

	public void execute(int command) {
		// TODO Auto-generated method stub
		this.execute(this.caller.getString(command));
	}

	@Override
	protected void onProgressUpdate(String... values) {
		// TODO: Implement advanced dialog
		// pd.setMessage(values[0]);
	}

	@Override
	protected void onPostExecute(Void result) {
		pd.dismiss();
	}

	@Override
	protected Void doInBackground(String... args) {
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
							+ args[0] + "; exit\n");
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
					publishProgress(status);
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
		return null;
	}

}