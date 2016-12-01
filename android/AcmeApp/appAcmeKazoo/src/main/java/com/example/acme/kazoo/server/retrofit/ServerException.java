package com.example.acme.kazoo.server.retrofit;

/**
 * Wraps exceptions thrown by the Retrofit clients.
 */
public class ServerException extends RuntimeException
{
    public int code;

    public ServerException(int aCode, String aMessage)
    {
        super(aMessage) ;
        code = aCode ;
    }

}
