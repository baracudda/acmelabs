package com.example.acme.kazoo.server.retrofit;

import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.auth.IBroadwayAuthAccountInfo;
import com.blackmoonit.androidbits.auth.IBroadwayAuthDeviceInfo;

/**
 * Provides BroadwayAuth support for RetroFit-driven calls to the server.
 */
public class RequestInterceptorBroadwayAuth
extends RequestInterceptorDeviceInfo
{
	final protected IBroadwayAuthAccountInfo mAuthAccountInfo;

	public RequestInterceptorBroadwayAuth(IBroadwayAuthAccountInfo aAuthAccountInfo,
			IBroadwayAuthDeviceInfo aDeviceInfo) {
		super(aDeviceInfo);
		mAuthAccountInfo = aAuthAccountInfo;
	}

	@Override
	protected StringBuilder composeAuthorizationHeader()
	{
		StringBuilder bldr = super.composeAuthorizationHeader() ;

		BroadwayAuthAccount theAuthAccount = null ;
		if( mAuthAccountInfo != null )
			theAuthAccount = mAuthAccountInfo.getAccountInUse() ;

		if( theAuthAccount != null )
		{
			bldr.append( ",auth_id=\"" )
				.append( theAuthAccount.getAcctAuthId() )
				.append( "\"" )
				.append( ",auth_token=\"" )
				.append( theAuthAccount.getAcctAuthToken() )
				.append( "\"" )
				;
		}

		return bldr ;
	}
}
