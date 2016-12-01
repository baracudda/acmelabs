package com.example.acme.kazoo.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.blackmoonit.androidbits.app.ReportAnExceptionHandler;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.account.manager.AuthPrefs;
import com.example.acme.kazoo.server.auth.AuthRestClient;

/**
 * Activity used to register an account with cloud services.
 */
public class ActivityAccountAuthRegister extends Activity
	implements FragmentAccountAuthRegister.OnFragmentInteractionListener
{
	static private final String TAG = ActivityAccountAuthRegister.class.getSimpleName();
	protected ReportAnExceptionHandler mDamageReport;

	private AuthRestClient mAuthRestClient = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mDamageReport = new ReportAnExceptionHandler(this).setup();

		setContentView(R.layout.activity_account_auth_register);

		setup(this);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new FragmentAccountAuthRegister())
					.commit();
		}
	}

	@Override
	protected void onDestroy() {
		mDamageReport.cleanup();
		super.onDestroy();
	}

	protected void setup(Context aContext) {
		//maintain an object to handle the Auth API
		mAuthRestClient = new AuthRestClient(aContext);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu aMenu) {
		getMenuInflater().inflate(R.menu.menu_account_auth_register, aMenu);
		Intent prefsActivity = new Intent( this, AuthPrefs.class );
		prefsActivity.addCategory(Intent.CATEGORY_PREFERENCE);
		prefsActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	aMenu.findItem(R.id.menu_item_auth_settings).setIntent(prefsActivity);
		return true;
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

	@Override
	public AuthRestClient getAuthRestClient() {
		return mAuthRestClient;
	}

	@Override
	public void onNewUserRegistered(Bundle aNewUser) {
		setResult(Activity.RESULT_OK, getIntent().putExtras(aNewUser));
        finish();
	}
}
