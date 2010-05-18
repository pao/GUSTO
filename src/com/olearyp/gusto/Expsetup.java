/*
 * Copyright 2010 Patrick O'Leary. All rights reserved. The contents of this file are subject to the
 * terms of the Common Development and Distribution License, Version 1.0 only. See the file CDDL.txt
 * in this distribution or http://opensource.org/licenses/cddl1.php for details.
 */
package com.olearyp.gusto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.ViewSwitcher;

// GUSTO: GUI Used to Setup TheOfficial
public class Expsetup extends PreferenceActivity {
	protected static final int REBOOT_NOTIFICATION = 0x0043B007;
	private static final String ASPIN_URL = "http://files.androidspin.com/downloads.php?dir=enomther/";
	private static final int STD_BUF_SIZE = 4096;
	private SharedPreferences settings = null;
	private NotificationManager nm = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		settings = getSharedPreferences("serverState", MODE_PRIVATE);
		addPreferencesFromResource(R.xml.preferences);
		final Map<String, String> config = getCurrentConfig();
		String ramhack_file;
		// ROM-specific settings
		if (config.get("GLB_EP_VERSION_EPDATA").contains("-TMO")) {
			findPreference("launcher").setEnabled(false);
			findPreference("phone").setEnabled(false);
			findPreference("contacts").setEnabled(false);
			findPreference("teeter").setEnabled(false);
			findPreference("quickoffice").setEnabled(false);
			ramhack_file = "10mb_kernel_patch_tmo262.zip";
		} else {
			ramhack_file = "10mb_kernel_patch_adp262.zip";
		}
		// QuickCommands menu
		{
			findPreference("reboot").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.reboot_alert_title, R.string.reboot_msg,
					R.string.reboot));
			findPreference("reboot_recovery").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.reboot_alert_title,
					R.string.reboot_recovery_msg, R.string.reboot_recovery));
			findPreference("reboot_bootloader").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.reboot_alert_title,
					R.string.reboot_bootloader_msg, R.string.reboot_bootload));
			findPreference("reboot_poweroff").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.shutdown_alert_title, R.string.shutdown_msg,
					R.string.shutdown));
			findPreference("rwsystem").setOnPreferenceClickListener(
				new ExpPreferenceListener(R.string.rwsystem, "setting /system read-write"));
			findPreference("rosystem").setOnPreferenceClickListener(
				new ExpPreferenceListener(R.string.rosystem, "setting /system read-only"));
		}
		// CPU options
		{
			findPreference("freq_sample")
				.setOnPreferenceChangeListener(
					new ExpPreferenceListener("yes | set_ep_cyan_ond_mod",
						"setting frequency scaling"));
			((CheckBoxPreference) findPreference("freq_sample")).setChecked(isTrueish(config,
				"GLB_EP_ENABLE_CYAN_OND_MOD"));
			final List<String> freqs = Arrays.asList(getResources().getStringArray(
				R.array.cpu_freqs_str));
			final ListPreference minFreqPref = (ListPreference) findPreference("cpu_freq_min");
			final ListPreference maxFreqPref = (ListPreference) findPreference("cpu_freq_max");
			minFreqPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object newValue) {
					sendCommand(
						"GLB_EP_MIN_CPU="
							+ newValue.toString()
							+ " && "
							+ "write_out_ep_config && "
							+ "echo \"$GLB_EP_MIN_CPU\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq",
						"setting minimum CPU frequency", "none");
					final String[] legalfreqs = freqs
						.subList(freqs.indexOf(newValue), freqs.size()).toArray(new String[0]);
					maxFreqPref.setEntries(legalfreqs);
					maxFreqPref.setEntryValues(legalfreqs);
					return true;
				}
			});
			maxFreqPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object newValue) {
					sendCommand(
						"GLB_EP_MAX_CPU="
							+ newValue.toString()
							+ " && "
							+ "write_out_ep_config && "
							+ "echo \"$GLB_EP_MAX_CPU\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq",
						"setting maximum CPU frequency", "none");
					final String[] legalfreqs = freqs.subList(0, freqs.indexOf(newValue) + 1)
						.toArray(new String[0]);
					minFreqPref.setEntries(legalfreqs);
					minFreqPref.setEntryValues(legalfreqs);
					return true;
				}
			});
			final String[] maxfreqs = freqs.subList(freqs.indexOf(config.get("GLB_EP_MIN_CPU")),
				freqs.size()).toArray(new String[0]);
			maxFreqPref.setEntries(maxfreqs);
			maxFreqPref.setEntryValues(maxfreqs);
			final String[] minfreqs = freqs.subList(0,
				freqs.indexOf(config.get("GLB_EP_MAX_CPU")) + 1).toArray(new String[0]);
			minFreqPref.setEntries(minfreqs);
			minFreqPref.setEntryValues(minfreqs);
			maxFreqPref.setValue(config.get("GLB_EP_MAX_CPU"));
			minFreqPref.setValue(config.get("GLB_EP_MIN_CPU"));
		}
		// Downloadables
		{
			DownloadPreference p;
			p = ((DownloadPreference) findPreference("ramhack_kernel"));
			p.setParams(ASPIN_URL + "RESOURCE/" + ramhack_file, "/sdcard/" + ramhack_file);
			p.setOnPreferenceClickListener(new RamhackPreferenceListener(testRamhack(config), p));
			p = ((DownloadPreference) findPreference("kernel_mods"));
			p.setParams(ASPIN_URL + "ROM/kmods_v211_vsapp.zip",
				"/sdcard/epvsapps/available/kmods_v211_vsapp.zip");
			p.setOnPreferenceClickListener(new VsappPreferenceListener(new File(
				"/sdcard/epvsapps/available/kmods_v211_vsapp.zip").exists(), p));
			p = ((DownloadPreference) findPreference("teeter"));
			p.setParams(ASPIN_URL + "APPS/teeter_vsapp.zip",
				"/sdcard/epvsapps/available/teeter_vsapp.zip");
			p.setOnPreferenceClickListener(new VsappPreferenceListener(new File(
				"/sdcard/epvsapps/available/teeter_vsapp.zip").exists(), p));
			p = ((DownloadPreference) findPreference("quickoffice"));
			p.setParams(ASPIN_URL + "APPS/quickoffice_vsapp.zip",
				"/sdcard/epvsapps/available/quickoffice_vsapp.zip");
			p.setOnPreferenceClickListener(new VsappPreferenceListener(new File(
				"/sdcard/epvsapps/available/quickoffice_vsapp.zip").exists(), p));
			p = ((DownloadPreference) findPreference("ext_widgets"));
			p.setParams(ASPIN_URL + "APPS/widgetpack_v2_vsapp.zip",
				"/sdcard/epvsapps/available/widgetpack_v2_vsapp.zip");
			p.setOnPreferenceClickListener(new VsappPreferenceListener(new File(
				"/sdcard/epvsapps/available/widgetpack_v2_vsapp.zip").exists(), p));
			p = ((DownloadPreference) findPreference("xdan_java"));
			p.setParams(ASPIN_URL + "APPS/jbed_vsapp.zip",
				"/sdcard/epvsapps/available/jbed_vsapp.zip");
			p.setOnPreferenceClickListener(new VsappPreferenceListener(new File(
				"/sdcard/epvsapps/available/jbed_vsapp.zip").exists(), p));
			p = ((DownloadPreference) findPreference("iwnn_ime_jp"));
			p.setParams(ASPIN_URL + "APPS/iwnnime_vsapp.zip",
				"/sdcard/epvsapps/available/iwnnime_vsapp.zip");
			p.setOnPreferenceClickListener(new VsappPreferenceListener(new File(
				"/sdcard/epvsapps/available/iwnnime_vsapp.zip").exists(), p));
		}
		// Theme profile settings
		{
			findPreference("launcher").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Launcher.apk", "changing Launcher"));
			((CheckBoxPreference) findPreference("launcher")).setChecked(isTrueish(config,
				"Launcher.apk"));
			findPreference("phone").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Phone.apk", "changing Phone"));
			((CheckBoxPreference) findPreference("phone"))
				.setChecked(isTrueish(config, "Phone.apk"));
			findPreference("contacts").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Contacts.apk", "changing Contacts"));
			((CheckBoxPreference) findPreference("contacts")).setChecked(isTrueish(config,
				"Contacts.apk"));
			findPreference("browser").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Browser.apk", "changing the Browser"));
			((CheckBoxPreference) findPreference("browser")).setChecked(isTrueish(config,
				"Browser.apk"));
			findPreference("mms").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Mms.apk", "changing MMS"));
			((CheckBoxPreference) findPreference("mms")).setChecked(isTrueish(config, "Mms.apk"));
		}
		// Advanced Options
		{
			// Swappiness
			findPreference("swappiness").setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(final Preference preference,
						final Object newValue) {
						sendCommand("yes '" + newValue.toString() + "' | set_ep_swappiness",
							"setting swappiness", "none");
						return true;
					}
				});
			((EditTextPreference) findPreference("swappiness")).setText(config
				.get("GLB_EP_SWAPPINESS"));
			((EditTextPreference) findPreference("swappiness")).getEditText().setKeyListener(
				new SwappinessKeyListener());
			// Compcache
			findPreference("compcache").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_compcache", "setting compcache", true));
			((CheckBoxPreference) findPreference("compcache")).setChecked(isTrueish(config,
				"GLB_EP_ENABLE_COMPCACHE"));
			// Linux swap
			findPreference("linux_swap").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_linuxswap", "setting Linux-swap"));
			((CheckBoxPreference) findPreference("linux_swap")).setChecked(isTrueish(config,
				"GLB_EP_ENABLE_LINUXSWAP"));
			// userinit.sh
			findPreference("userinit").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_userinit", "setting userinit"));
			((CheckBoxPreference) findPreference("userinit")).setChecked(isTrueish(config,
				"GLB_EP_RUN_USERINIT"));
			// Remove odex on boot
			findPreference("odex").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_odex_boot_removal",
					"setting ODEX removal"));
			((CheckBoxPreference) findPreference("odex")).setChecked(isTrueish(config,
				"GLB_EP_ODEX_BOOT_REMOVAL"));
			// Odex now
			findPreference("reodex").setOnPreferenceClickListener(
				new ExpPreferenceListener("yes | odex_ep_data_apps", "re-ODEXing apps"));
			// Set pid priorities
			findPreference("pid_prioritize").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_pid_prioritizer",
					"setting PID prioritizer"));
			((CheckBoxPreference) findPreference("pid_prioritize")).setChecked(isTrueish(config,
				"GLB_EP_PID_PRIORITIZE"));
		}
		// Generate and send ep_log
		findPreference("ep_log").setOnPreferenceClickListener(new EpLogListener());
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			final String reboot_type = getSharedPreferences("serverState", Context.MODE_PRIVATE)
				.getString("serverState", "none");
			if (reboot_type.equals(getString(R.string.reboot_manual_flash_required))) {
				rebootDialog(R.string.reboot_alert_title, R.string.reboot_reflash_msg,
					R.string.reboot_recovery, true).show();
				return true;
			} else if (reboot_type.equals(getString(R.string.reboot_recovery_required))) {
				rebootDialog(R.string.reboot_alert_title, R.string.reboot_autoflash_msg,
					R.string.reboot_recovery, true).show();
				return true;
			} else if (reboot_type.equals(getString(R.string.reboot_required))) {
				rebootDialog(R.string.reboot_alert_title, R.string.reboot_request_msg,
					R.string.reboot, true).show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private Builder rebootDialog(final int title, final int message, final int reboot_cmd,
		final boolean close_if_no_reboot) {
		return new AlertDialog.Builder(Expsetup.this).setTitle(title)
			.setMessage(getString(message)).setPositiveButton(R.string.yes, new OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					sendCommand(getString(reboot_cmd), "rebooting", "none");
				}
			}).setNegativeButton(R.string.no, close_if_no_reboot ? new OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					Expsetup.this.finish();
				}
			} : null);
	}

	private boolean isTrueish(final Map<String, String> config, final String key) {
		return (config.get(key) != null && config.get(key).equals("YES"));
	}

	private Map<String, String> getCurrentConfig() {
		final HashMap<String, String> config = new HashMap<String, String>();
		// Read in the config file & parse it
		BufferedReader rd;
		try {
			rd = new BufferedReader(new FileReader("/system/bin/exp.config"));
			String line = rd.readLine();
			while (line != null) {
				final String[] parts = line.split("=");
				if (parts.length == 2) {
					config.put(parts[0], parts[1].substring(1, parts[1].length() - 1));
				}
				line = rd.readLine();
			}
			rd.close();
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Then check the theme profile settings
		final String[] app_profiles = new File(getString(R.string.theme_profile_folder)).list();
		for (final String app_profile : app_profiles) {
			try {
				rd = new BufferedReader(new FileReader(getString(R.string.theme_profile_folder)
					+ app_profile));
				config.put(app_profile, rd.readLine().trim());
				rd.close();
			} catch (final FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Find out available system memory
		final Process p;
		try {
			p = Runtime.getRuntime().exec("busybox free");
			final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()), STD_BUF_SIZE);
			p.waitFor();
			String status = stdInput.readLine();
			status = stdInput.readLine();
			final String[] memories = status.trim().split(" +");
			final String mem = memories[1].trim();
			config.put("system_memory", mem);
			stdInput.close();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return config;
	}

	/*
	 * This is a fairly dumb way to determine whether a board is running a ramhack kernel. Notably,
	 * any 32A board will return true; we'd need to come up with some absurd special cases to
	 * prevent this though.
	 */
	private boolean testRamhack(final Map<String, String> config) {
		final int sysmem = Integer.valueOf(config.get("system_memory"));
		Log.v("GUSTO", "Total system memory: " + Integer.toString(sysmem));
		if (sysmem < 98000) {
			return false;
		}
		return true;
	}

	protected void sendCommand(final String command, final String description, final String state) {
		final Intent runCmd = new Intent("com.olearyp.gusto.SUEXEC");
		runCmd.setData(
			Uri.fromParts("command", ". /system/bin/exp_script.sh.lib && read_in_ep_config && "
				+ command, "")).putExtra("com.olearyp.gusto.STATE", state);
		final Notification note = new Notification(R.drawable.icon, description.substring(0, 1)
			.toUpperCase()
			+ description.substring(1) + "...", System.currentTimeMillis());
		note.setLatestEventInfo(Expsetup.this, getString(R.string.app_name),
			getString(R.string.app_name) + " is " + description + "...", PendingIntent
				.getBroadcast(Expsetup.this, 0, null, 0));
		runCmd.putExtra("com.olearyp.gusto.RUN_NOTIFICATION", note);
		startService(runCmd);
		setServerState(state);
		if (getServerState().equals(getString(R.string.reboot_manual_flash_required))) {
			final Intent intent = new Intent("com.olearyp.gusto.SUEXEC").setData(Uri.fromParts(
				"commandid", Integer.toString(R.string.reboot_recovery), ""));
			final PendingIntent contentIntent = PendingIntent.getService(this, 0, intent, 0);
			final Notification rebootNote = new Notification(R.drawable.status_reboot,
				getString(R.string.reboot_recovery_required_msg), System.currentTimeMillis());
			rebootNote.setLatestEventInfo(this, "GUSTO reboot request",
				getString(R.string.reboot_recovery_doit_msg), contentIntent);
			rebootNote.deleteIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				"com.olearyp.gusto.RESET_SERVER_STATE"), 0);
			rebootNote.flags |= Notification.FLAG_SHOW_LIGHTS;
			rebootNote.ledOnMS = 200;
			rebootNote.ledOffMS = 400;
			rebootNote.ledARGB = Color.argb(255, 255, 0, 0);
			nm.notify(REBOOT_NOTIFICATION, rebootNote);
		} else if (getServerState().equals(getString(R.string.reboot_recovery_required))) {
			final Intent intent = new Intent("com.olearyp.gusto.SUEXEC").setData(Uri.fromParts(
				"commandid", Integer.toString(R.string.reboot), ""));
			final PendingIntent contentIntent = PendingIntent.getService(this, 0, intent, 0);
			final Notification rebootNote = new Notification(R.drawable.status_reboot,
				getString(R.string.reboot_autoflash_required_msg), System.currentTimeMillis());
			rebootNote.setLatestEventInfo(this, "GUSTO reboot request",
				getString(R.string.reboot_autoflash_doit_msg), contentIntent);
			rebootNote.deleteIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				"com.olearyp.gusto.RESET_SERVER_STATE"), 0);
			rebootNote.flags |= Notification.FLAG_SHOW_LIGHTS;
			rebootNote.ledOnMS = 200;
			rebootNote.ledOffMS = 600;
			rebootNote.ledARGB = Color.argb(255, 255, 255, 0);
			nm.notify(REBOOT_NOTIFICATION, rebootNote);
		} else if (getServerState().equals(getString(R.string.reboot_required))) {
			final Intent intent = new Intent("com.olearyp.gusto.SUEXEC").setData(Uri.fromParts(
				"commandid", Integer.toString(R.string.reboot), ""));
			final PendingIntent contentIntent = PendingIntent.getService(this, 0, intent, 0);
			final Notification rebootNote = new Notification(R.drawable.status_reboot,
				getString(R.string.reboot_required_msg), System.currentTimeMillis());
			rebootNote.setLatestEventInfo(this, "GUSTO reboot request",
				getString(R.string.reboot_doit_msg), contentIntent);
			rebootNote.deleteIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				"com.olearyp.gusto.RESET_SERVER_STATE"), 0);
			rebootNote.flags |= Notification.FLAG_SHOW_LIGHTS;
			rebootNote.ledOnMS = 200;
			rebootNote.ledOffMS = 600;
			rebootNote.ledARGB = Color.argb(255, 255, 255, 0);
			nm.notify(REBOOT_NOTIFICATION, rebootNote);
		}
	}

	/*
	 * Listener for 'generate ep_log' button
	 */
	private final class EpLogListener implements OnPreferenceClickListener {
		/*
		 * Listener for Positive button of ep_log dialog
		 */
		private final class EpLogDialogListener implements OnClickListener {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				final Intent runCmd = new Intent("com.olearyp.gusto.SUEXEC");
				runCmd.setData(Uri.fromParts("command", "gen_ep_logfile", ""));
				final PendingIntent post_ex_intent = PendingIntent.getBroadcast(Expsetup.this, 0,
					new Intent("com.olearyp.gusto.MAIL_LOG"), 0);
				runCmd.putExtra("com.olearyp.gusto.POST_EX_INTENT", post_ex_intent);
				final Notification note = new Notification(R.drawable.icon, "Generating log...",
					System.currentTimeMillis());
				note.setLatestEventInfo(Expsetup.this, getString(R.string.app_name),
					getString(R.string.app_name) + " is generating an ep_log...", PendingIntent
						.getBroadcast(Expsetup.this, 0, null, 0));
				runCmd.putExtra("com.olearyp.gusto.RUN_NOTIFICATION", note);
				startService(runCmd);
			}
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			new AlertDialog.Builder(Expsetup.this).setTitle(R.string.ep_log_alert_title)
				.setMessage(R.string.ep_log_confirm_msg).setPositiveButton(R.string.create_log,
					new EpLogDialogListener()).setNegativeButton(R.string.return_to_menu, null)
				.show();
			return true;
		}
	}

	/* Generic listener which executes a command as root */
	private final class ExpPreferenceListener implements OnPreferenceChangeListener,
		OnPreferenceClickListener {
		private String command = "";
		private String description = "";
		private boolean requires_reboot = false;
		private int commandid = 0;

		public ExpPreferenceListener(
			final int commandid, final String description) {
			super();
			this.commandid = commandid;
			this.description = description;
		}

		public ExpPreferenceListener(
			final String command, final String description) {
			this(command, description, false);
		}

		public ExpPreferenceListener(
			final String command, final String description, final boolean requires_reboot) {
			super();
			this.command = command;
			this.description = description;
			this.requires_reboot = requires_reboot;
		}

		@Override
		public boolean onPreferenceChange(final Preference preference, final Object newValue) {
			return runCommand();
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			return runCommand();
		}

		private boolean runCommand() {
			// new SuProcess(Expsetup.this).execute(command);
			// if (requires_reboot) {
			// system_needs_reboot = true;
			// }
			if (commandid > 0) {
				sendCommand(getString(commandid), description, requires_reboot ? "reboot-required"
					: "none");
			} else {
				sendCommand(command, description, requires_reboot ? "reboot-required" : "none");
			}
			return true;
		}
	}

	private class RebootPreferenceListener implements OnPreferenceClickListener {
		int message;
		int command;
		private final int title;

		public RebootPreferenceListener(
			final int title, final int message, final int command) {
			this.title = title;
			this.message = message;
			this.command = command;
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			rebootDialog(title, message, command, false).show();
			return false;
		}
	}

	/*
	 * Listener for theme profile configuration, which is more complex than the other settings
	 * (since there's no function in the library which does this for me.)
	 */
	public class ExpThemeProfileChangeListener implements OnPreferenceChangeListener {
		private String filename = "";
		private String description = "";

		public ExpThemeProfileChangeListener(
			final String filename, final String description) {
			super();
			this.filename = filename;
			this.description = description;
		}

		@Override
		public boolean onPreferenceChange(final Preference preference, final Object newValue) {
			if ((Boolean) newValue) {
				sendCommand("echo YES > /data/.epdata/theme_profile/" + filename, description,
					getString(R.string.reboot_manual_flash_required));
			} else {
				sendCommand("busybox rm -rf /data/.epdata/theme_profile/" + filename, description,
					getString(R.string.reboot_manual_flash_required));
			}
			return true;
		}
	}

	protected String getServerState() {
		return settings.getString("serverState", "none");
	}

	protected void setServerState(final String state) {
		if (getIndex(state) > getIndex(getServerState())) {
			settings.edit().putString("serverState", state).commit();
		}
	}

	private final int getIndex(final String specificValue) {
		final String[] stateIndex = getResources().getStringArray(R.array.server_states);
		for (int i = 0; i < stateIndex.length; i++) {
			if (stateIndex[i].equals(specificValue)) {
				return i;
			}
		}
		return -1;
	}

	protected class DownloadPreferenceListener implements OnPreferenceClickListener {
		protected boolean isDownloaded = false;
		protected DownloadPreference dp = null;

		public DownloadPreferenceListener(
			final boolean isDownloaded, final DownloadPreference dp) {
			super();
			this.isDownloaded = isDownloaded;
			this.dp = dp;
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (!isDownloaded) {
				((ViewSwitcher) dp.getView().findViewById(R.id.ViewSwitcher)).showNext();
				new Downloader((ProgressBar) dp.getView().findViewById(R.id.ProgressBar))
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
				((ViewSwitcher) dp.getView().findViewById(R.id.ViewSwitcher)).showPrevious();
				((CheckBox) dp.getView().findViewById(R.id.CheckBox)).setChecked(true);
				pb.setIndeterminate(true);
				super.onPostExecute(result);
			}

			@Override
			protected Void doInBackground(final Void... params) {
				final HttpClient client = new DefaultHttpClient();
				try {
					final HttpGet method = new HttpGet(dp.getUrl());
					final ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {
						@Override
						public Void handleResponse(final HttpResponse response)
							throws ClientProtocolException, IOException {
							final HttpEntity entity = response.getEntity();
							final Long fileLen = entity.getContentLength();
							if (entity != null) {
								final InputStream filecont = entity.getContent();
								final ReadableByteChannel dl_chan = Channels.newChannel(filecont);
								final File dst = new File(dp.getDestination());
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

	private class RamhackPreferenceListener extends DownloadPreferenceListener {
		public RamhackPreferenceListener(
			final boolean isDownloaded, final DownloadPreference dp) {
			super(isDownloaded, dp);
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (!isDownloaded) {
				new AlertDialog.Builder(Expsetup.this).setTitle("Install ramhack").setMessage(
					"The \"ramhack\" kernel will allocate ~10 MB of RAM currently assigned to "
						+ "video memory to the main memory pool. This may improve performance "
						+ "under normal circumstances at the cost of 3D performance.\n\n"
						+ "This operation cannot be undone except by reflashing the "
						+ "expansion pack.").setPositiveButton("Install ramhack",
					new OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							((ViewSwitcher) dp.getView().findViewById(R.id.ViewSwitcher))
								.showNext();
							new Downloader((ProgressBar) dp.getView()
								.findViewById(R.id.ProgressBar)) {
								@Override
								protected void onPostExecute(final Void result) {
									sendCommand(
										"echo 'boot-recovery' > /cache/recovery/command && "
											+ "echo '--update_package=SDCARD:"
											+ new File(dp.getDestination()).getName()
											+ "' >> /cache/recovery/command", "preparing kernel",
										getString(R.string.reboot_recovery_required));
									super.onPostExecute(result);
								}
							}.execute((Void) null);
							isDownloaded = true;
						}
					}).setNegativeButton("Do not install", null).show();
				return true;
			} else {
				new AlertDialog.Builder(Expsetup.this).setTitle("Ramhack removal").setMessage(
					"To remove the ramhack kernel, you must manually "
						+ "reflash the expansion pack from recovery, "
						+ "reboot to Android, then flash themes, "
						+ "etc. as desired.  Reboot to recovery now?").setPositiveButton(
					"Reboot to recovery", new OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog, final int which) {
							sendCommand(getString(R.string.reboot_recovery), "rebooting", "none");
						}
					}).setNegativeButton("Return", null).show();
			}
			return true;
		}
	}

	enum InstallChoice {
		INSTALL, UNINSTALL
	}

	private class VsappPreferenceListener extends DownloadPreferenceListener {

		public VsappPreferenceListener(
			final boolean isDownloaded, final DownloadPreference dp) {
			super(isDownloaded, dp);
		}

		@Override
		public boolean onPreferenceClick(final Preference preference) {
			if (!isDownloaded) {
				((ViewSwitcher) dp.getView().findViewById(R.id.ViewSwitcher)).showNext();
				new Downloader((ProgressBar) dp.getView().findViewById(R.id.ProgressBar)) {
					@Override
					protected void onPostExecute(final Void result) {
						manageVsapp(InstallChoice.INSTALL);
						super.onPostExecute(result);
					}
				}.execute((Void) null);
				isDownloaded = true;
				return true;
			} else {
				new AlertDialog.Builder(Expsetup.this).setTitle("VSAPP maintenance")
					.setPositiveButton("Reinstall", new OnClickListener() {
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
							((CheckBox) dp.getView().findViewById(R.id.CheckBox)).setChecked(false);
							new File(dp.getDestination()).delete();
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
			final File file = new File(dp.getDestination());
			final String epvspath = file.getParentFile().getParent() + "/" + file.getName();
			// File.renameTo() was found to be unreliable here
			final String mvincmd = "mv " + dp.getDestination() + " " + epvspath;
			final String mvoutcmd = "mv " + epvspath + " " + dp.getDestination();
			if (install_uninstall == InstallChoice.INSTALL) {
				sendCommand(mvincmd + " && install_vsapps ; " + mvoutcmd, "installing VSAPP",
					"none");
			} else {
				sendCommand(mvincmd + " && uninstall_vsapps ; " + mvoutcmd, "uninstalling VSAPP",
					"none");
			}
		}
	}
}
