package com.example.acme.kazoo.account.manager;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.blackmoonit.androidbits.auth.ABroadwayAuthenticator;
import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.example.acme.kazoo.server.auth.AuthRestClient;

/**
 * Android Account Manager descendant for working with Broadway Auth REST server.
 */
public class AccountAuthenticator extends ABroadwayAuthenticator
{
	/**
	 * Android Authenticator requires the Authenticator class to be a Locally Bound service.
	 */
	static public class ServiceAuthenticator extends Service {
		@Override
		public IBinder onBind(Intent intent) {
			return (new AccountAuthenticator( getApplicationContext() )).getIBinder();
		}
	}

	public AccountAuthenticator(Context aContext) {
		super(aContext);
	}

	@Override
	protected String requestServerForAuthToken(AccountManager aAcctMgr, BroadwayAuthAccount aAccount)
	{
		return (new AuthRestClient( getContext() )).requestAuthToken( aAcctMgr, aAccount );
	}

}
