package com.example.acme.kazoo.server.auth;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.acme.kazoo.R;
import com.example.acme.kazoo.server.retrofit.BroadwayRetrofitClient;
import com.example.acme.kazoo.ui.ActivityAccountAuthLogin;
import com.example.acme.kazoo.server.api.AcmeServerAPI;
import com.example.acme.kazoo.service.KazooService;

/**
 * Provides a context and a callback handler for the task of authenticating a
 * login session on the web service.
 *
 * Under normal circumstances, the life cycle of this activity should be as
 * follows:
 *
 * <ol type="1">
 *     <li>{@link #onCreate}</li>
 *     <li>{@link #onStart} &mdash; Starts connection to {@link KazooService}</li>
 *     <li>{@link AuthenticationTask#run} &mdash; Acquires authentication.</li>
 *     <li>{@link #onActivityResult} &mdash; Completes the process.</li>
 *     <li>{@link #onStop} &mdash; Releases the service binding.</li>
 * </ol>
 *
 * The code within the class is intentionally arranged in the order listed
 * above, to reflect this model.
 */
public class AuthenticationActivity extends Activity
{
    public static final String TAG = AuthenticationActivity.class.getSimpleName() ;

    /**
     * Passed to {@link ActivityAccountAuthLogin} when initiated by the
     * {@link AuthenticationTask}.
     */
    public static final int LOGIN_REQUEST_CODE = 1 ;

    KazooService mService;
    boolean mBound = false;
    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            KazooService.LocalBinder binder = (KazooService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            // Now we can do stuff with the bound service
            (new AuthenticationTask()).run() ;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate( Bundle bndlState )
    {
        super.onCreate( bndlState ) ;
        this.setContentView( R.layout.popup_authenticating ) ;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to KazooService
        Intent intent = new Intent(this, KazooService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Asynchronous task for obtaining an authentication token while the
     * {@link AuthenticationActivity} is in the foreground.
     */
    protected class AuthenticationTask implements Runnable
    {
        final AuthenticationActivity m_act = AuthenticationActivity.this ;

        @Override
        public void run()
        {
            BroadwayRetrofitClient<AcmeServerAPI> http = (m_act.mService!=null && m_act.mBound)
                ? m_act.mService.getProcessor().getDataClient() : null;
            if( http == null )
            {
                Log.e(TAG,
                        "No HTTP client for relay service. Aborting login." ) ;
                Toast.makeText( m_act, R.string.toast_cannot_authenticate,
                        Toast.LENGTH_SHORT )
                    .show()
                    ;
                m_act.finish() ;
                return ;
            }
            http.setAccountManagerActivity(m_act) ;
            if( http.isAuthorized() )
            {
                Log.d(TAG, "Client is already authenticated." ) ;
                m_act.finish();
            }
            if( http.isEndpointDefined() )
            { // Try to get an authentication token from the server.
                http.acquireAuthToken( m_act, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        m_act.setResult( RESULT_OK, m_act.getIntent() ) ;
                        m_act.finish() ;
                    }
                });
            }
            else
            { // We don't have a server URI yet; ask for one.
                Log.i(TAG,
                        "No server URI provisioned; starting login activity." );
                m_act.startActivityForResult( new Intent(
                        m_act, ActivityAccountAuthLogin.class ), 1 )  ;
                // Don't finish the AuthenticationActivity until onActivityResult().
            }
        }
    }

    @Override
    protected void onActivityResult( int zRequest, int zResult, Intent sig )
    {
        if( zRequest == LOGIN_REQUEST_CODE )
        { // This is our login result.
            Log.i(TAG, "Authentication endpoint acquired." ) ;
            this.finish() ;
        }
        else super.onActivityResult( zRequest, zResult, sig ) ;
    }

    @Override
    protected void onStop() {
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onStop();
    }

}
