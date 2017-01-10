package com.example.acme.kazoo.server;

import com.google.bits_gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Describes the web service API for Retrofit.
 */
public interface AcmeServerAPI
{
	@POST( "APIEndpoints/uploadCurrentWifiInformation" )
	Call<APIResponse> uploadCurrentWifiInformation( @Body WifiInformation request ) ;

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

	/**
	 * Example class for capturing an API response.
	 */
	class APIResponse
	{
		public static final String STATUS_SUCCESS = "SUCCESS" ;
		public static final String STATUS_FAILURE = "FAILURE" ;

		@SerializedName("status")
		public String status = STATUS_SUCCESS ;

		@SerializedName("error")
		public ErrorReport error = null ;

		public class Data
		{
			@SerializedName( "frequency" )
			public String frequency ;

			@SerializedName( "link_speed" )
			public String link_speed ;

			@Override
			public String toString()
			{
				return "Data{ " +
						"frequency='" + frequency + '\'' +
						", link_speed='" + link_speed + '\'' +
						" }";
			}
		}

		public static class ErrorReport
		{
			@SerializedName("cause")
			public String cause = null ;

			@SerializedName("message")
			public String message = null ;

			@Override
			public String toString()
			{
				return (new StringBuilder())
						.append( "[" ).append( this.cause ).append( "][" )
						.append( this.message ).append( "]" )
						.toString()
						;
			}
		}

		@Override
		public String toString()
		{
			return "APIResponse{ " +
					"status='" + status.toString() + '\'' +
					", error=" + error.toString() +
					" }" ;
		}
	}
}
