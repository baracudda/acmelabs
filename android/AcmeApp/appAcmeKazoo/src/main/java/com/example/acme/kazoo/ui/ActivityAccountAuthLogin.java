package com.example.acme.kazoo.ui;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.blackmoonit.androidbits.app.ActivityResult;
import com.blackmoonit.androidbits.app.ReportAnExceptionHandler;
import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.concurrent.ThreadTask;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.account.manager.AccountAuthenticator;
import com.example.acme.kazoo.account.manager.AndroidAccountAuthActivity;
import com.example.acme.kazoo.account.manager.AuthPrefs;
import com.example.acme.kazoo.server.auth.AuthRestAPI;
import com.example.acme.kazoo.server.auth.AuthRestClient;

/**
 * Activity class that will be called by Android's Authenticator mechanism
 * and is in charge with identifying the user.
 * setResult() returns our results to the Android Authenticator.
 */
@SuppressWarnings("MissingPermission")
public final class ActivityAccountAuthLogin extends AndroidAccountAuthActivity
	implements FragmentAccountAuthLogin.OnFragmentInteractionListener
{
	static private final String TAG = ActivityAccountAuthLogin.class.getSimpleName();
	private ReportAnExceptionHandler mDamageReport;

	private ActivityResult.Manager mActResultMgr;
	private AuthRestClient mAuthRestClient = null;
	private AccountManager mAccountManager = null;
	private BroadwayAuthAccount myAccount = null;

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mDamageReport = new ReportAnExceptionHandler(this).setup();
		AuthPrefs.setDefaultPrefs(this);

		setContentView(R.layout.activity_account_auth_login);

		mAccountManager = AccountManager.get(this);
		myAccount = (getIntent()!=null)
			? BroadwayAuthAccount.fromBundle(mAccountManager, getIntent().getExtras())
			: null
		;
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, FragmentAccountAuthLogin.newInstance(
							((myAccount != null) ? myAccount.getAcctName() : ""), ""))
					.commit();
		}

	}

	@Override
	protected void onDestroy() {
		mDamageReport.cleanup();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu aMenu) {
		getMenuInflater().inflate(R.menu.menu_account_auth_login,aMenu);

		Intent prefsActivity = new Intent(this, AuthPrefs.class);
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
	protected void onActivityResult(int aRequestCode, int aResultCode, Intent data) {
		getActivityResultManager().onActivityResult(aRequestCode, aResultCode, data);
	}

	public ActivityResult.Manager getActivityResultManager() {
		if (mActResultMgr==null)
			mActResultMgr = new ActivityResult.Manager(this);
		return mActResultMgr;
	}

	public AuthRestClient getAuthRestClient() {
		return mAuthRestClient;
	}

	public AccountManager getAccountManager() {
		return mAccountManager;
	}

	public void afterSetAccountAuthenticatorResult(Bundle aResult) {
		Intent theIntent = (getIntent()!=null) ? getIntent() : new Intent();
		setResult(RESULT_OK, theIntent.putExtras(aResult));
        finish();
	}

	public Context getAlertDialogContext() {
		return (getActionBar()!=null)
			? getActionBar().getThemedContext()
			: getBaseContext();
	}

	protected Runnable mTaskMobileAutoAuth = new Runnable() {
		@Override
		public void run() {
			Log.i(TAG, "attempting to retrieve an auth token automatically");
			AuthRestAPI.MobileAuthTokenRequestByAndroid theRequest =
					new AuthRestAPI.MobileAuthTokenRequestByAndroid();
			theRequest.user_token = myAccount.getAcctUserToken(mAccountManager);
			theRequest.auth_id = myAccount.getAcctAuthId();
			AuthRestAPI.MobileAuthTokenResponse theResponse =
					mAuthRestClient.requestMobileAuthByAndroid(theRequest);
			if (theResponse!=null
					&& !TextUtils.isEmpty(theResponse.account_name)
					&& !TextUtils.isEmpty(theResponse.auth_id)
					&& !TextUtils.isEmpty(theResponse.user_token)
					&& !TextUtils.isEmpty(theResponse.auth_token)
					) {
				myAccount.setAcctUserToken(mAccountManager, theResponse.user_token);
				mAccountManager.setAuthToken(myAccount,
						AccountAuthenticator.AUTHTOKEN_KIND_FULL_ACCESS,
						theResponse.auth_token);
				myAccount.setAcctAuthToken(theResponse.auth_token);
				//set the result
				final Bundle theResult = myAccount.toBundle();
				setAccountAuthenticatorResult(theResult);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						afterSetAccountAuthenticatorResult(theResult);
					}
				});
			}
		}
	};

	protected Runnable mTaskCheckForMobileAccount = new Runnable() {
		@Override
		public void run() {
			Log.i(TAG, "attempting to ask server for an auth account");
			AuthRestAPI.MobileAuthAccountResponse theResponse =
					mAuthRestClient.requestMobileAuthAccount();
			if (theResponse!=null && theResponse.isValid())
			{
				BroadwayAuthAccount theAuthAccount = BroadwayAuthAccount.explicitlyCreateNewAccount(
						mAccountManager, theResponse.account_name,
						getString(R.string.account_auth_type), theResponse.auth_id,
						theResponse.user_token, theResponse.auth_token
				);
				//set the result
				final Bundle theResult = theAuthAccount.toBundle();
				setAccountAuthenticatorResult(theResult);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						afterSetAccountAuthenticatorResult(theResult);
					}
				});
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		Log.i("AuthToken", "Login screen shown");
		//maintain an object to handle the Auth API
		mAuthRestClient = new AuthRestClient( this );
		if( mAuthRestClient.hasCurrentImplementation() )
        {
			//if this activity has been launched and we have an account defined, try to auto-auth
			if (myAccount!=null) {
				ThreadTask.runThisTask(mTaskMobileAutoAuth, "auto-auth token");
			}
			else {
				//see if we have a pre-provisioned account from the server
				ThreadTask.runThisTask(mTaskCheckForMobileAccount, "requestMobileAuthAccount");
			}
		} else {
			//if we cannot connect to the api client, popup server settings, no valid URL yet.
			startActivity( new Intent(this, AuthPrefs.class) );
		}
	}

}
