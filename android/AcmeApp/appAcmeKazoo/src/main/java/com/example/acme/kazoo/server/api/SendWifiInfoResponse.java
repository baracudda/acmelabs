package com.example.acme.kazoo.server.api;

import com.google.bits_gson.annotations.SerializedName;

/**
 * Handles the response to a request to upload wifi info.
 */
public class SendWifiInfoResponse
extends StandardResponse
{
    public class Data
    {
        @SerializedName( "frequency" )
        public String frequency ;

        @SerializedName( "link_speed" )
        public String link_speed ;

        @Override
        public String toString()
        {
            return "Data { " +
                    "frequency='" + frequency + '\'' +
                    ", link_speed='" + link_speed + '\'' +
                    " }";
        }
    }

    @SerializedName("data")
    public Data data ;

    @Override
    public String toString() {
        return "SendWifiInfoResponse { " +
                "data=" + data.toString() +
                " }" ;
    }
}
