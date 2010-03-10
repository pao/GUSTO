package com.olearyp.gusto;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

public class LogMailer extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String logfiles[] = getEpLogs();
		Arrays.sort(logfiles);
		context.startActivity(Intent.createChooser(
				sendFile(logfiles[logfiles.length - 1]), "Send ep_log via...")
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	private String[] getEpLogs() {
		return Environment.getExternalStorageDirectory().list(
				new FilenameFilter() {
					@Override
					public boolean accept(File dir, String filename) {
						return filename.startsWith("ep_log_")
								&& filename.endsWith(".log");
					}
				});
	}

	/*
	 * Implementation of rot13 algorithm to hide email addresses from spambots
	 * looking through the sourcecode.
	 */
	private String decode_address(String string) {
		StringBuffer tempReturn = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			int abyte = string.charAt(i);
			int cap = abyte & 32;
			abyte &= ~cap;
			abyte = ((abyte >= 'A') && (abyte <= 'Z') ? ((abyte - 'A' + 13) % 26 + 'A')
					: abyte)
					| cap;
			tempReturn.append((char) abyte);
		}
		return tempReturn.toString();
	}

	/* Email an ep_log file as attachment to enomther */
	private Intent sendFile(String logfile) {
		// rabzgure is going to love me forever
		// TODO: Refactor the email address string
		return new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(
				Intent.EXTRA_EMAIL,
				new String[] { decode_address("rabzgure") + "@"
						+ decode_address("tznvy.pbz") }).putExtra(
				Intent.EXTRA_SUBJECT, "ep_log report from user").putExtra(
				Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/" + logfile));
	}

}
