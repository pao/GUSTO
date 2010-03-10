/* Copyright 2010 Patrick O'Leary.  All rights reserved.
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only.
 * See the file CDDL.txt in this distribution or
 * http://opensource.org/licenses/cddl1.php for details.
 */

package com.olearyp.gusto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/*
 * Resets the state of the SuServer to "none", so after rebooting, changing
 * a preference in GUSTO doesn't immediately trigger a reboot request.
 */
public class ResetServerState extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		context.getSharedPreferences("serverState", Context.MODE_PRIVATE)
				.edit().putString("serverState", "none").commit();
	}

}
