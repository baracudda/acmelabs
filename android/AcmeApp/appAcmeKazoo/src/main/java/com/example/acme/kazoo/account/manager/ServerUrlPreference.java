package com.example.acme.kazoo.account.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;

import com.blackmoonit.androidbits.widget.UriPreference;
import com.example.acme.kazoo.R;

import java.util.Arrays;

import static com.blackmoonit.androidbits.app.AppPreferenceBase.getPrefs;

/**
 * Preference that will accept string input and work with "validated" setting for summary.
 */
public class ServerUrlPreference extends UriPreference implements SharedPreferences.OnSharedPreferenceChangeListener {
	SharedPreferences mSettings;
	String[] mValidatedEntryValues;
	String[] mValidatedEntries;
	String mPrefKeyServerUrlValidated;
	String mValidatedEntry;

	@Override
	public CharSequence getSummary() {
		return mValidatedEntry;
	}

	public ServerUrlPreference(Context aContext, AttributeSet attrs) {
		super(aContext, attrs);
		mSettings = getPrefs(aContext);
		mSettings.registerOnSharedPreferenceChangeListener(this);
		mValidatedEntries = aContext.getResources().getStringArray(
				R.array.pref_entries_server_url_validated);
		mValidatedEntryValues = aContext.getResources().getStringArray(
				R.array.pref_entryvalues_server_url_validated);
		mPrefKeyServerUrlValidated = aContext.getString(R.string.pref_key_server_url_validated);
		onSharedPreferenceChanged(mSettings, mPrefKeyServerUrlValidated);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences aSharedPreferences, String aPrefKey) {
		if (mSettings!=null && mValidatedEntries!=null && mValidatedEntryValues!=null
				&& mPrefKeyServerUrlValidated!=null && mPrefKeyServerUrlValidated.equals(aPrefKey)) {
			String thePrefValue = mSettings.getString(mPrefKeyServerUrlValidated,"false");
			int theEntryValue = Arrays.asList(mValidatedEntryValues).indexOf(thePrefValue);
			mValidatedEntry = mValidatedEntries[theEntryValue];
			this.setSummary(mValidatedEntry); //need to trigger a repaint of the widgets
		}
	}
}
