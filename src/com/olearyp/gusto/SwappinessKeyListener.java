package com.olearyp.gusto;

import android.text.Spanned;
import android.text.method.DigitsKeyListener;

public class SwappinessKeyListener extends DigitsKeyListener {

	@Override
	public CharSequence filter(CharSequence source, int start, int end,
			Spanned dest, int dstart, int dend) {
		// If new character not a digit or blank, kill it
		if (!source.toString().matches("[0-9]*"))
			return "";
		// Seed the new string with "0" in case it is blank
		StringBuilder result = new StringBuilder("0");
		result.append(dest.subSequence(0, dstart));
		result.append(source.subSequence(start, end));
		result.append(dest.subSequence(dend, dest.length()));
		// Bounds-check the resulting value
		Integer num = Double.valueOf(result.toString()).intValue();
		if (num < 0)
			return "";
		if (num > 100)
			return "";
		return null;
	}

}
