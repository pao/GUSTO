/* Copyright 2010 Patrick O'Leary.  All rights reserved.
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only.
 * See the file CDDL.txt in this distribution or
 * http://opensource.org/licenses/cddl1.php for details.
 */

package com.olearyp.gusto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;

// GUSTO: GUI Used to Setup TheOfficial 
public class Expsetup extends PreferenceActivity {
	private boolean system_needs_reboot = false;
	private boolean system_needs_reboot_recovery = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		Map<String, String> config = getCurrentConfig();

		findPreference("server_test").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						// TODO Auto-generated method stub
						Intent runCmd = new Intent("com.olearyp.gusto.SUEXEC");
						runCmd.setData(Uri.fromParts("command", "ls -l /", ""))
								.putExtra("com.olearyp.gusto.STATE", "none");
						Expsetup.this.startService(runCmd);
						return true;
					}
				});

		// QuickCommands menu
		findPreference("reboot").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.reboot_msg,
						R.string.reboot));
		findPreference("reboot_recovery").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.reboot_recovery_msg,
						R.string.reboot_recovery));
		findPreference("reboot_bootloader").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.reboot_bootloader_msg,
						R.string.reboot_bootload));
		findPreference("reboot_poweroff").setOnPreferenceClickListener(
				new RebootPreferenceListener(R.string.shutdown_msg,
						R.string.shutdown));
		findPreference("rwsystem").setOnPreferenceClickListener(
				new ExpPreferenceListener(R.string.rwsystem));
		findPreference("rosystem").setOnPreferenceClickListener(
				new ExpPreferenceListener(R.string.rosystem));

		// Generate and send ep_log
		findPreference("ep_log").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						new AlertDialog.Builder(Expsetup.this).setMessage(
								R.string.ep_log_confirm_msg).setPositiveButton(
								R.string.create_log, new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										new SuProcess(Expsetup.this) {
											@Override
											protected void onPostExecute(
													Void result) {
												String logfiles[] = getEpLogs();
												Arrays.sort(logfiles);
												sendFile(logfiles[logfiles.length - 1]);
												super.onPostExecute(result);
											}
										}.execute("gen_ep_logfile");
									}
								}).setNegativeButton(R.string.return_to_menu,
								null).show();
						return true;
					}
				});

		// CPU options
		findPreference("freq_sample").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | set_ep_cyan_ond_mod"));
		((CheckBoxPreference) findPreference("freq_sample"))
				.setChecked(isTrueish(config, "GLB_EP_ENABLE_CYAN_OND_MOD"));

		final List<String> freqs = Arrays.asList(getResources().getStringArray(
				R.array.cpu_freqs_str));
		final ListPreference minFreqPref = (ListPreference) findPreference("cpu_freq_min");
		final ListPreference maxFreqPref = (ListPreference) findPreference("cpu_freq_max");
		minFreqPref
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						new SuProcess(Expsetup.this)
								.execute("GLB_EP_MIN_CPU="
										+ newValue.toString()
										+ " && "
										+ "write_out_ep_config && "
										+ "echo \"$GLB_EP_MIN_CPU\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
						String[] legalfreqs = freqs.subList(
								freqs.indexOf(newValue), freqs.size()).toArray(
								new String[0]);
						maxFreqPref.setEntries(legalfreqs);
						maxFreqPref.setEntryValues(legalfreqs);
						return true;
					}
				});
		maxFreqPref
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						new SuProcess(Expsetup.this)
								.execute("GLB_EP_MAX_CPU="
										+ newValue.toString()
										+ " && "
										+ "write_out_ep_config && "
										+ "echo \"$GLB_EP_MAX_CPU\" > /sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq");
						String[] legalfreqs = freqs.subList(0,
								freqs.indexOf(newValue) + 1).toArray(
								new String[0]);
						minFreqPref.setEntries(legalfreqs);
						minFreqPref.setEntryValues(legalfreqs);
						return true;
					}

				});

		String[] maxfreqs = freqs.subList(
				freqs.indexOf(config.get("GLB_EP_MIN_CPU")), freqs.size())
				.toArray(new String[0]);
		maxFreqPref.setEntries(maxfreqs);
		maxFreqPref.setEntryValues(maxfreqs);

		String[] minfreqs = freqs.subList(0,
				freqs.indexOf(config.get("GLB_EP_MAX_CPU")) + 1).toArray(
				new String[0]);
		minFreqPref.setEntries(minfreqs);
		minFreqPref.setEntryValues(minfreqs);

		maxFreqPref.setValue(config.get("GLB_EP_MAX_CPU"));
		minFreqPref.setValue(config.get("GLB_EP_MIN_CPU"));

		// Swappiness
		findPreference("swappiness").setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						new SuProcess(Expsetup.this)
								.execute("yes '" + newValue.toString()
										+ "' | set_ep_swappiness");
						return true;
					}
				});
		((EditTextPreference) findPreference("swappiness")).setText(config
				.get("GLB_EP_SWAPPINESS"));
		((EditTextPreference) findPreference("swappiness")).getEditText()
				.setKeyListener(new SwappinessKeyListener());

		// Compcache
		findPreference("compcache").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_compcache", true));
		((CheckBoxPreference) findPreference("compcache"))
				.setChecked(isTrueish(config, "GLB_EP_ENABLE_COMPCACHE"));

		// Linux swap
		findPreference("linux_swap").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_linuxswap"));
		((CheckBoxPreference) findPreference("linux_swap"))
				.setChecked(isTrueish(config, "GLB_EP_ENABLE_LINUXSWAP"));

		// userinit.sh
		findPreference("userinit").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_userinit"));
		((CheckBoxPreference) findPreference("userinit")).setChecked(isTrueish(
				config, "GLB_EP_RUN_USERINIT"));

		// Remove odex on boot
		findPreference("odex").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_odex_boot_removal"));
		((CheckBoxPreference) findPreference("odex")).setChecked(isTrueish(
				config, "GLB_EP_ODEX_BOOT_REMOVAL"));

		// Odex now
		findPreference("reodex").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						new SuProcess(Expsetup.this)
								.execute("yes | odex_ep_data_apps");
						return true;
					}
				});

		// Set pid priorities
		findPreference("pid_prioritize").setOnPreferenceChangeListener(
				new ExpPreferenceListener("yes | toggle_ep_pid_prioritizer"));
		((CheckBoxPreference) findPreference("pid_prioritize"))
				.setChecked(isTrueish(config, "GLB_EP_PID_PRIORITIZE"));

		// Theme profile settings
		if (config.get("GLB_EP_VERSION_EPDATA").contains("-TMO")) {
			findPreference("launcher").setEnabled(false);
			findPreference("phone").setEnabled(false);
			findPreference("contacts").setEnabled(false);
		}

		findPreference("launcher").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Launcher.apk"));
		((CheckBoxPreference) findPreference("launcher")).setChecked(isTrueish(
				config, "Launcher.apk"));

		findPreference("phone").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Phone.apk"));
		((CheckBoxPreference) findPreference("phone")).setChecked(isTrueish(
				config, "Phone.apk"));

		findPreference("contacts").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Contacts.apk"));
		((CheckBoxPreference) findPreference("contacts")).setChecked(isTrueish(
				config, "Contacts.apk"));

		findPreference("browser").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Browser.apk"));
		((CheckBoxPreference) findPreference("browser")).setChecked(isTrueish(
				config, "Browser.apk"));

		findPreference("mms").setOnPreferenceChangeListener(
				new ExpThemeProfileChangeListener("Mms.apk"));
		((CheckBoxPreference) findPreference("mms")).setChecked(isTrueish(
				config, "Mms.apk"));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (system_needs_reboot_recovery) {
				rebootDialog(R.string.reboot_reflash_msg,
						R.string.reboot_recovery, true).show();
				return true;
			} else if (system_needs_reboot) {
				rebootDialog(R.string.reboot_required_msg, R.string.reboot,
						true).show();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private Builder rebootDialog(int message, final int reboot_cmd,
			boolean close_if_no_reboot) {
		return new AlertDialog.Builder(Expsetup.this).setMessage(
				getString(message)).setPositiveButton(R.string.yes,
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						new SuProcess(Expsetup.this).execute(reboot_cmd);
					}
				}).setNegativeButton(R.string.no,
				close_if_no_reboot ? new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Expsetup.this.finish();
					}
				} : null);
	}

	private boolean isTrueish(Map<String, String> config, String key) {
		return (config.get(key) != null && config.get(key).equals("YES"));
	}

	private Map<String, String> getCurrentConfig() {
		HashMap<String, String> config = new HashMap<String, String>();
		// Read in the config file & parse it
		BufferedReader rd;
		try {
			rd = new BufferedReader(new FileReader("/system/bin/exp.config"));
			String line = rd.readLine();
			while (line != null) {
				String[] parts = line.split("=");
				if (parts.length == 2) {
					config.put(parts[0], parts[1].substring(1, parts[1]
							.length() - 1));
				}
				line = rd.readLine();
			}
			rd.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Then check the theme profile settings
		String[] app_profiles = new File(
				getString(R.string.theme_profile_folder)).list();
		for (String app_profile : app_profiles) {
			try {
				rd = new BufferedReader(new FileReader(
						getString(R.string.theme_profile_folder) + app_profile));
				config.put(app_profile, rd.readLine().trim());
				rd.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return config;
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
	private void sendFile(String logfile) {
		// rabzgure is going to love me forever
		// TODO: Refactor the email address string
		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("text/plain");
		sendIntent.putExtra(Intent.EXTRA_EMAIL,
				new String[] { decode_address("rabzgure") + "@"
						+ decode_address("tznvy.pbz") });
		sendIntent.putExtra(Intent.EXTRA_SUBJECT, "ep_log report from user");
		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/"
				+ logfile));
		startActivity(Intent.createChooser(sendIntent, "Send ep_log via..."));
	}

	/* Generic listener which executes a command as root */
	private final class ExpPreferenceListener implements
			OnPreferenceChangeListener, OnPreferenceClickListener {

		private String command = "";
		private boolean requires_reboot = false;

		public ExpPreferenceListener(int command) {
			this(getString(command));
		}

		public ExpPreferenceListener(String command) {
			this(command, false);
		}

		public ExpPreferenceListener(String command, boolean requires_reboot) {
			super();
			this.command = command;
			this.requires_reboot = requires_reboot;
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			return runCommand();
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			return runCommand();
		}

		private boolean runCommand() {
			new SuProcess(Expsetup.this).execute(command);
			if (requires_reboot) {
				system_needs_reboot = true;
			}
			return true;
		}
	}

	private class RebootPreferenceListener implements OnPreferenceClickListener {
		int message;
		int command;

		public RebootPreferenceListener(int message, int command) {
			this.message = message;
			this.command = command;
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			rebootDialog(message, command, false).show();
			return false;
		}
	}

	/*
	 * Listener for theme profile configuration, which is more complex than the
	 * other settings (since there's no function in the library which does this
	 * for me.)
	 */
	public class ExpThemeProfileChangeListener implements
			OnPreferenceChangeListener {

		private String filename;

		public ExpThemeProfileChangeListener(String filename) {
			super();
			this.filename = filename;
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			SuProcess s = new SuProcess(Expsetup.this);
			if ((Boolean) newValue) {
				s.execute("echo YES > /data/.epdata/theme_profile/" + filename);
			} else {
				s.execute("busybox rm -rf /data/.epdata/theme_profile/"
						+ filename);
			}
			system_needs_reboot_recovery = true;
			return true;
		}

	}
}