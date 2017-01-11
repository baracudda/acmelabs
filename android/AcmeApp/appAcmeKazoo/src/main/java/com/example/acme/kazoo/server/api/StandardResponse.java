package com.example.acme.kazoo.server.api;

import com.google.bits_gson.annotations.SerializedName;

/**
 * Encapsulates a standard response from the API.
 *
 * Each standardized response will also have a {@code data} member, but since
 * the format of this object will be different for every API function, it is up
 * to the descendant class to define that inner class as the type of its
 * {@code data} member.
 */
@SuppressWarnings("unused")
public abstract class StandardResponse
{
    /**
     * The error token usually returned with an HTTP 401 Not Authorized (sic)
     * response.
     */
    public static final String ERR_NOT_AUTHENTICATED = "NOT_AUTHENTICATED" ;

    /** The error token usually returned with an HTTP 403 Forbidden response. */
    public static final String ERR_FORBIDDEN = "FORBIDDEN" ;

    /**
     * Encapsulates an error report within a {@link StandardResponse}.
     */
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

    public static final String STATUS_SUCCESS = "SUCCESS" ;

    public static final String STATUS_FAILURE = "FAILURE" ;

    @SerializedName("status")
    public String status = STATUS_SUCCESS ;

    @SerializedName("error")
    public ErrorReport error = null ;

    /**
     * If there is an error included in the response, describe it.
     * Generally used only for logging purposes.
     * @return a description of the error
     */
    public String describeError()
    { return ( this.error == null ? "" : this.error.toString() ) ; }

    /**
     * Returns the cause of an error, if any.
     * @return an error cause, or null if there is no error or no cause
     */
    public String getErrorCause()
    { return ( this.error == null ? null : this.error.cause ) ; }
}
