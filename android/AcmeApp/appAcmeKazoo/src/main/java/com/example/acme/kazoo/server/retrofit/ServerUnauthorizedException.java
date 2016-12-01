package com.example.acme.kazoo.server.retrofit;

/**
 * Used by Retrofit clients to signal that there is a network authorization problem.
 */
public class ServerUnauthorizedException extends RuntimeException
{
    public ServerUnauthorizedException(String aMessage, Throwable xCause) {
    	super( getErrorMessage(aMessage, xCause), xCause ) ;
    }

    public ServerUnauthorizedException(Throwable xCause) {
    	this(null, xCause) ;
    }

    public ServerUnauthorizedException() {
    	this(null);
	}

	static protected String getErrorMessage(String aMessage, Throwable xCause) {
		if (aMessage==null) {
			aMessage = "Unauthorized Access";
		}
		return aMessage;
	}

}
