package com.example.acme.kazoo.server.auth;

import android.text.TextUtils;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Provides an authentication API.
 */
public interface AuthRestAPI
{
	class MobileAuthValidateUrlResponse
	{
		public String challenge;
		public String response;
		public Integer api_version_seq;

		public boolean isValidated() {
			return "ping".equals(challenge) && "pong".equals(response);
		}
	}
    /**
     * Checks to see if the entered URL is a BroadwayAuth-capable URL.
     * @return the HTTP response
     */
    @GET("account/requestMobileAuth/{ping}")
	Call<MobileAuthValidateUrlResponse> validateUrl(@Path("ping") String aPingStr);

	//-----------------------------------------

	class MobileAuthRegisterRequest
	{
		public String name;
		public String salt;
		public String email;
		public String code;
		public String kind;
		public String auth_header_data;
	}
	class MobileAuthRegisterResponse
	{
		//code values
		static public final int REGISTRATION_SUCCESS = 0;
		static public final int REGISTRATION_NAME_TAKEN = 1;
		static public final int REGISTRATION_EMAIL_TAKEN = 2;
		static public final int REGISTRATION_REG_CODE_FAIL = 3;
		static public final int REGISTRATION_UNKNOWN_ERROR = 4;

		public Integer code;
		public String auth_id;
		public String user_token;

		@Override
		public String toString() {
			return "code="+code+" auth_id="+auth_id+" user_token="+user_token;
		}
	}
	/**
     * Register a user via mobile app rather than on web page.
     * @param aRequest the POST data object
     * @return the server reply object
	 */
    @POST("account/registerViaMobile")
    Call<MobileAuthRegisterResponse> registerViaMobile(@Body MobileAuthRegisterRequest aRequest);

	//-----------------------------------------

	class MobileAuthTokenRequestByLogin
	{
		public String ticketholder;
		public String pwinput;
		public String auth_header_data;
	}
	class MobileAuthTokenResponse
	{
		public String account_name;
		public String auth_id;
		public String user_token;
		public String auth_token;
	}

	/**
     * Login a user via mobile app rather than on web page using human entered user/pw.
     * @param aRequest the POST data object
     * @return the server reply object
	 */
    @POST("account/requestMobileAuth")
    Call<MobileAuthTokenResponse> requestMobileAuthByLogin(@Body MobileAuthTokenRequestByLogin aRequest);

	//-----------------------------------------

	class MobileAuthTokenRequestByAndroid
	{
		public String auth_id;
		public String user_token;
		public String auth_header_data;
	}
	/**
     * Login a user via mobile app rather than on web page using automated tokens.
     * @param aRequest the POST data object
     * @return the server reply object
	 */
    @POST("account/requestMobileAuth")
    Call<MobileAuthTokenResponse> requestMobileAuthByAndroid(@Body MobileAuthTokenRequestByAndroid aRequest);

	//-----------------------------------------

	class MobileAuthAccountRequest
	{
		public String auth_header_data;
	}
    class MobileAuthAccountResponse
    {
		public String account_name;
		public String auth_id;
		public String user_token;
		public String auth_token;

		public boolean isValid()
		{
			return !TextUtils.isEmpty(account_name)
					&& !TextUtils.isEmpty(auth_id)
					&& !TextUtils.isEmpty(user_token)
					&& !TextUtils.isEmpty(auth_token)
					;
		}
    }
    /**
     * Mobile devices may be pre-provisioned with an account. Check for an account.
     */
    @POST("account/requestMobileAuthAccount")
    Call<MobileAuthAccountResponse> requestMobileAuthAccount(@Body MobileAuthAccountRequest aRequest) ;

}
