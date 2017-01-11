package com.example.acme.kazoo.server.retrofit;

import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.auth.IBroadwayAuthDeviceInfo;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Provides Device info for RetroFit-driven calls to the server.
 */
public class RequestInterceptorDeviceInfo
implements Interceptor
{
	final protected IBroadwayAuthDeviceInfo mDeviceInfo;

	public RequestInterceptorDeviceInfo(IBroadwayAuthDeviceInfo aDeviceInfo) {
		super();
		mDeviceInfo = aDeviceInfo;
		if (mDeviceInfo==null) {
			throw new IllegalArgumentException("BroadwayAuthInfo cannot be null");
		}
	}

	/**
	 * Composes the authorization header string. Override this method to catch
	 * the output of this implementation and append additional fields to the
	 * buffer before it is composed in {@link #intercept}.
	 * @return a string builder with the initial fields of an auth header
     */
	protected StringBuilder composeAuthorizationHeader()
	{
		return mDeviceInfo.composeBroadwayAuthData(null);
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		Request theRequest = chain.request();
		// Customize the request
		return chain.proceed(theRequest.newBuilder()
			.header( "Authorization", BroadwayAuthAccount.composeAuthorizationHeaderValue(
				this.composeAuthorizationHeader() ) )
			.build()
		);
	}

}
