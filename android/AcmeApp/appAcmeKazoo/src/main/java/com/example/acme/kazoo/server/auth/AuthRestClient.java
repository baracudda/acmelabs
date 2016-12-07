package com.example.acme.kazoo.server.auth;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import org.jetbrains.annotations.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.blackmoonit.androidbits.auth.AuthPrefsChangedListener;
import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.auth.FactoriesForBroadwayAuth;
import com.blackmoonit.androidbits.auth.IBroadwayAuthDeviceInfo;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.server.retrofit.BasicRetrofitClient;

/**
 * Provides an API for auth exchange with the remote server.
 */
@SuppressWarnings("unused")
public class AuthRestClient extends BasicRetrofitClient<AuthRestAPI>
	implements IBroadwayAuthDeviceInfo, AuthPrefsChangedListener.TaskToValidateURL
{
	static private final String TAG = AuthRestClient.class.getSimpleName();

    /** A container of Android device information. */
	protected IBroadwayAuthDeviceInfo m_devinfo = null ;

	public AuthRestClient(Context aContext, IBroadwayAuthDeviceInfo aDeviceInfo)
    {
    	super(AuthRestAPI.class, aContext);
    	m_devinfo = aDeviceInfo;
    }

	public AuthRestClient(Context aContext)
	{
		this( aContext, FactoriesForBroadwayAuth.obtainDeviceInfo( aContext ) );
	}

    @Override
    protected void setup( Context ctx ) {
    	super.setup(ctx);
		this.regenerate();
    }

    /**
     * Standard device information sent to the server to determine auth status.
     * Non-volatile information that should not change between API calls.
     * @return Returns the various data collected to present to the server.
     */
    @Override
    public String[] getDeviceFingerprints()
    {
        if( m_devinfo != null )
            return m_devinfo.getDeviceFingerprints() ;
        else return null ;
    }

    /**
     * Volatile device information sent to the server to determine auth status.
     * This covers information such as GPS location and timestamp.
     * @return Returns the various data collected to present to the server.
     */
    @Override
    public String[] getDeviceCircumstances()
    {
        if( m_devinfo != null )
            return m_devinfo.getDeviceCircumstances() ;
        else return null ;
    }

	@Override
	public StringBuilder composeBroadwayAuthData(StringBuilder aStrBldr)
	{
		if( m_devinfo != null )
			return m_devinfo.composeBroadwayAuthData( aStrBldr ) ;
		else return null ;
	}

	// ============================================
	//           API methods
	// ============================================

	public boolean validateUrl() {
		AuthRestAPI.MobileAuthValidateUrlResponse theMobileAuth = validateUrlResponse();
		return ( theMobileAuth != null && theMobileAuth.isValidated() );
	}

	@Nullable
	public AuthRestAPI.MobileAuthValidateUrlResponse validateUrlResponse() {
		try {
			return getImplementation().validateUrl(
					getContext().getString(R.string.account_auth_validation_ping)
			).execute().body();
		} catch (Exception | Error e) {
			Log.e(TAG, e.toString());
			return null;
		}
	}

	@Nullable
	public AuthRestAPI.MobileAuthRegisterResponse registerViaMobile(
			AuthRestAPI.MobileAuthRegisterRequest aRequest)
	{
		try {
			aRequest.auth_header_data = BroadwayAuthAccount.composeAuthorizationHeaderValue(
					composeBroadwayAuthData(null)
			);
			return getImplementation().registerViaMobile(aRequest).execute().body();
		} catch (Exception | Error e) {
			Log.e(TAG, "register fail", e);
			return null;
		}
	}

	@Nullable
	public AuthRestAPI.MobileAuthTokenResponse requestMobileAuthByLogin (
			AuthRestAPI.MobileAuthTokenRequestByLogin aRequest)
	{
		try {
			aRequest.auth_header_data = BroadwayAuthAccount.composeAuthorizationHeaderValue(
					composeBroadwayAuthData(null)
			);
			return getImplementation().requestMobileAuthByLogin(aRequest).execute().body();
		} catch (Exception | Error e) {
			Log.e(TAG, "reqMobileAuthByLogin", e);
		}
		return null;
	}

	@Nullable
	public AuthRestAPI.MobileAuthTokenResponse requestMobileAuthByAndroid (
			AuthRestAPI.MobileAuthTokenRequestByAndroid aRequest)
	{
		try {
			aRequest.auth_header_data = BroadwayAuthAccount.composeAuthorizationHeaderValue(
					composeBroadwayAuthData(null)
			);
			return getImplementation().requestMobileAuthByAndroid(aRequest).execute().body();
		} catch (Exception | Error e) {
			Log.e(TAG, "reqMobileAuthByAndroid", e);
			return null;
		}
	}

	@Nullable
	public AuthRestAPI.MobileAuthAccountResponse requestMobileAuthAccount()
	{
		try {
			AuthRestAPI.MobileAuthAccountRequest theRequest =
					new AuthRestAPI.MobileAuthAccountRequest();
			theRequest.auth_header_data = BroadwayAuthAccount.composeAuthorizationHeaderValue(
					composeBroadwayAuthData(null)
			);
			return getImplementation().requestMobileAuthAccount(theRequest).execute().body();
		} catch (Exception | Error e) {
			Log.e(TAG, "requestMobileAuthAccount", e);
			return null;
		}
	}

	/**
	 * Ask the server for an auth token.
	 */
	public String requestAuthToken(AccountManager aAcctMgr, BroadwayAuthAccount aAccount)
	{
		String theUserToken = aAccount.getAcctUserToken(aAcctMgr);
		if (!TextUtils.isEmpty(theUserToken)) {
			AuthRestAPI.MobileAuthTokenRequestByAndroid theRequest =
					new AuthRestAPI.MobileAuthTokenRequestByAndroid();
			theRequest.auth_id = aAccount.getAcctAuthId();
			theRequest.user_token = theUserToken;
			AuthRestAPI.MobileAuthTokenResponse theResponse = requestMobileAuthByAndroid(theRequest);
			if (theResponse!=null) {
				return theResponse.auth_token;
			}
		}
		return null;
	}

	@Override
	public boolean onURLChange(Context aContext, SharedPreferences aSettings) {
		if (aContext!=null && aSettings!=null) {
			setEndpoint(aSettings.getString(
					aContext.getString(R.string.pref_key_server_url), null));
			AuthRestAPI.MobileAuthValidateUrlResponse theMobileAuth = validateUrlResponse();
			if (theMobileAuth != null) {
				//record the API version website expects
				String thePrefKey = aContext.getString(R.string.pref_key_server_api_version);
				aSettings.edit().putString(thePrefKey, Integer.toString(
						theMobileAuth.api_version_seq)).apply();

				if (theMobileAuth.isValidated()) {
					Boolean bUrlValidated = theMobileAuth.isValidated();
					Log.i(TAG, aContext.getString((bUrlValidated)
							? R.string.msg_url_validated
							: R.string.msg_url_validate_failed
					));
					return bUrlValidated;
				}
			}
		}
		return false;
	}

}
