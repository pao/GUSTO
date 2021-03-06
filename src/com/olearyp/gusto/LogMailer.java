package com.olearyp.gusto;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

public class LogMailer extends BroadcastReceiver {

	private static final int READY_NOTIFICATION = 0x0004EADE;

	@Override
	public void onReceive(Context context, Intent intent) {
		final String logfiles[] = getEpLogs();
		Arrays.sort(logfiles);
		final PendingIntent mailLog = PendingIntent.getActivity(context, 0, Intent.createChooser(
				sendFile(logfiles[logfiles.length - 1]), "Send ep_log via...")
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification note = new Notification(R.drawable.icon, "ep_log ready to send!", System.currentTimeMillis());
		note.setLatestEventInfo(context, "ep_log is ready!", "Select to send log to enomther", mailLog);
		note.flags |= Notification.FLAG_AUTO_CANCEL;
		nm.notify(READY_NOTIFICATION, note);
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
