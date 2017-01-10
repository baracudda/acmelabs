package com.example.acme.kazoo.server.api;

import com.google.bits_gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Describes the web service API for Retrofit.
 */
public interface AcmeServerAPI
{
	/**
	 * Example class for storing device information for an API request.
	 */
	class WifiInformation
	{
		@SerializedName( "frequency" )
		public String frequency ;

		@SerializedName( "link_speed" )
		public String link_speed ;
	}

	@POST( "APIEndpoints/uploadWifiInformation" )
	Call<SendWifiInfoResponse> uploadCurrentWifiInformation( @Body WifiInformation request ) ;
}
