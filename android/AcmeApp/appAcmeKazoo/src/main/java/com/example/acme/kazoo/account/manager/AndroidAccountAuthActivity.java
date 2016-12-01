package com.example.acme.kazoo.account.manager;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.blackmoonit.androidbits.auth.AuthActivityHelper;

/**
 * Base class for implementing an Activity that is used to help implement an
 * AbstractAccountAuthenticator. If the AbstractAccountAuthenticator needs to use an activity
 * to handle the request then it can have the activity extend AccountAuthenticatorActivity.
 * The AbstractAccountAuthenticator passes in the response to the intent using the following:
 * <pre>
 * intent.putExtra({@link AccountManager#KEY_ACCOUNT_AUTHENTICATOR_RESPONSE}, response);
 * </pre>
 * The activity then sets the result that is to be handed to the response via
 * {@link #setAccountAuthenticatorResult(Bundle)}.
 * This result will be sent as the result of the request when the activity finishes. If this
 * is never set or if it is set to null then error {@link AccountManager#ERROR_CODE_CANCELED}
 * will be called on the response.
 */
public class AndroidAccountAuthActivity extends Activity {
	private AuthActivityHelper mAuthActivityHelper;

	/**
	 * Set the result that is to be sent as the result of the request that caused this
	 * Activity to be launched. If result is null or this method is never called then
	 * the request will be canceled.
	 * @param result this is returned as the result of the AbstractAccountAuthenticator request
	 */
	public final void setAccountAuthenticatorResult(Bundle result) {
		if (mAuthActivityHelper!=null)
			mAuthActivityHelper.setAccountAuthenticatorResult( result );
	}

	/**
	 * Ensure {@link #onNewIntent} gets called.
	 * @param icicle the save instance data of this Activity, may be null
	 */
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mAuthActivityHelper = new AuthActivityHelper(this);
		onNewIntent(getIntent());
	}

	@Override
	protected void onNewIntent(Intent aIntent) {
		super.onNewIntent(aIntent);
		if (mAuthActivityHelper!=null)
			mAuthActivityHelper.onNewIntent( aIntent );
	}

	@Override
	public void finish() {
		if (mAuthActivityHelper!=null)
			mAuthActivityHelper.onFinish();
		super.finish();
	}
}
