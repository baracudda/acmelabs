package com.example.acme.kazoo.account.manager;

/********************************************************************************************
 IMPLEMENTATION NOTES ON ACCOUNT AUTH PREFERENCE SCREEN LAUNCHED BY ANDROIDS AUTH SERVICE:
 SEE: http://stackoverflow.com/questions/5486228/how-do-we-control-an-android-sync-adapter-preference

the following 3 steps:
1. Set up Account Preferences XML
2. Create an activity to manage preferences
3. Extract the account information from the "preference editing" Intent

Setting up the account preferences XML. Consider the following snippet:

<PreferenceScreen
	android:key="account_settings"
	android:title="Account Preferences"
	android:summary="Misc account preferences">
	<intent
		android:action="some.unique.action.name.account.EDIT"
		android:targetPackage="com.example.preferences"
		android:targetClass="com.example.preferences.PreferencesActivity">
	</intent>
</PreferenceScreen>

Given this, here are some of the important points
1. The android:key for this PreferenceScreen must be "account_settings" or Android will not
   find & display your preferences.
2. By using an explicit Intent and specifying the targetPackage and targetClass, Android will
   start your Activity directly and you don't need to worry about an Intent filter.
3. Android stores the Account object for the currently selected account in this Intent's
   Extras -- which is very important on the receiving end so you can know which account you
   are managing.

Creating the preference managing Activity:  Create an Activity to correspond to the package
and class specified in the above XML. The choice of Activity is up to you -- it's most common
to subclass android.preference.PreferenceActivity but you can also subclass Activity directly.
Standard Activity development guidelines apply here.

Getting the Account from the "preference editing" Intent:
When your Activity starts up, you can extract the corresponding Account object from the Extras
Bundle (using this.getIntent().getExtras()) and the key "account". Recall that this Intent
will be the one that you specified in preferences XML file initially. Once you have the
Account, it should be straightforward to load/save preferences for that account using
SharedPreferences, your database, or whatever other method you prefer.
********************************************************************************************/

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MenuItem;

import com.blackmoonit.androidbits.app.AppPreferenceBase;
import com.blackmoonit.androidbits.app.ReportAnExceptionHandler;
import com.blackmoonit.androidbits.auth.AuthPrefsChangedListener;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.server.auth.AuthRestClient;

public class AuthPrefs extends AppPreferenceBase
	implements AuthPrefsChangedListener.TaskToValidateURL
{
	protected ReportAnExceptionHandler mDamageReport;
	protected SharedPreferences.OnSharedPreferenceChangeListener mPrefChangeListener;

	/**
	 * Allow decedents to define their own array, if desired.
	 * @return Returns the resource IDs for what preference screens to load and display.
	 */
	protected int[] getPreferenceScreenDefinitionsResourceIds() {
		return getResourceArray(getApplicationContext(), R.array.pref_screen_definitions_for_auth);
	}

	@Override
	protected void setup() {
		mDamageReport = new ReportAnExceptionHandler(this).setup();

		// Set up a listener whenever a key changes
		SharedPreferences theSettings = getPrefs(this);
		if (theSettings!=null) {
			mPrefChangeListener = new AuthPrefsChangedListener(this, this);
			theSettings.registerOnSharedPreferenceChangeListener(mPrefChangeListener);
		}
	}

	@Override
	protected void onDestroy() {
		// Unregister the listener
		SharedPreferences theSettings = getPrefs();
		if (theSettings!=null && mPrefChangeListener!=null) {
			theSettings.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
		}
		mDamageReport.cleanup();
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem aMenuItem) {
		final int theMenuChoice = aMenuItem.getItemId();
		switch (theMenuChoice) {
			case android.R.id.home:
				finish();
		}//switch
		return false;
	}

	/**
	 * Check the pref setting to see if the server url pref setting is valid.
 	 * @param aContext - the context to use.
	 * @return Returns TRUE if the server url has been validated.
	 */
	static public boolean isServerUrlValidated(Context aContext) {
		Context theContext = aContext.getApplicationContext();
		SharedPreferences theSettings = getPrefs(theContext);
		if (theSettings!=null) {
			String thePrefKey = theContext.getString(R.string.pref_key_server_url_validated);
			return "true".equals(theSettings.getString(thePrefKey, "false"));
		}
		return false;
	}

	@Override
	public boolean onURLChange(Context aContext, SharedPreferences aSettings) {
		AuthRestClient theAuthRestClient = new AuthRestClient( aContext );
		return theAuthRestClient.onURLChange( aContext, aSettings );
	}

}
