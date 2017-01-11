package com.example.acme.kazoo.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.blackmoonit.androidbits.auth.FactoriesForBroadwayAuth;
import com.blackmoonit.androidbits.utils.BitsStringUtils;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.server.api.SendWifiInfoResponse;
import com.example.acme.kazoo.server.auth.AuthenticationActivity;
import com.example.acme.kazoo.server.api.AcmeServerAPI;
import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.auth.IBroadwayAuthDeviceInfo;
import com.example.acme.kazoo.server.retrofit.BroadwayRetrofitClient;
import com.example.acme.kazoo.server.retrofit.ServerUnauthorizedException;

import java.io.IOException;
import java.util.Vector;

/**
 * Used by the {@link KazooService} to process stuff.
 */
public class ServiceActionProcessor
implements
    SharedPreferences.OnSharedPreferenceChangeListener,
    BroadwayRetrofitClient.BroadwayAuthListener,
    BroadwayRetrofitClient.RestEndpointListener
{
    static private final String TAG = ServiceActionProcessor.class.getSimpleName() ;

//// Inner Classes /////////////////////////////////////////////////////////////

    /**
     * Activities in the app which display any information about our server
     * connection should implement this interface so that the views may be
     * updated in real time.
     */
    public interface LoginListener
    {
        /**
         * Receives a notification that the web service URI has changed.
         * @param sURI the new URI
         */
        void onServerURIChanged( String sURI ) ;

        /**
         * Receives a notification that our account information on the web
         * service has changed.
         * @param acct the account that is now logged in; this may be null if we
         *  are not, in fact, connected
         */
        void onServerAuthChanged( BroadwayAuthAccount acct ) ;
    }

    protected class SendWifiInfoTask
    implements Runnable
    {
        protected final ServiceActionProcessor m_proc =
                ServiceActionProcessor.this ;

        @Override
        public void run()
        {
            // Verify access before use.
            if( m_http != null )
            {
                // Create our API request data object.
                AcmeServerAPI.WifiInformation wifiInformation = new AcmeServerAPI.WifiInformation() ;

                // Determine our currently connected Wi-Fi frequency and link speed, if any.
                String frequency = "" ;
                String linkSpeed = "" ;
                WifiManager wifiMan = (WifiManager ) m_proc.getContext().getSystemService( Context.WIFI_SERVICE ) ;
                if (wifiMan != null)
                {
                    WifiInfo wifiInfo = wifiMan.getConnectionInfo();
                    if ( wifiInfo != null )
                    {
                        StringBuilder frequencyStringBuilder = new StringBuilder( String.valueOf( wifiInfo.getFrequency() ) ) ;
                        frequencyStringBuilder.append( " " ) ;
                        frequencyStringBuilder.append( wifiInfo.FREQUENCY_UNITS ) ;
                        frequency = frequencyStringBuilder.toString() ;

                        StringBuilder linkSpeedStringBuilder = new StringBuilder( String.valueOf( wifiInfo.getLinkSpeed() ) ) ;
                        linkSpeedStringBuilder.append( " " ) ;
                        linkSpeedStringBuilder.append( wifiInfo.LINK_SPEED_UNITS ) ;
                        linkSpeed = linkSpeedStringBuilder.toString() ;
                    }
                }
                wifiInformation.frequency = frequency ;
                wifiInformation.link_speed = linkSpeed ;

                Log.d( TAG, frequency + " " + linkSpeed ) ;

                AcmeServerAPI serverAPI = m_proc.m_http.getImplementation() ;

                if( serverAPI != null )
                {
                    try
                    { // Send our wifi information to server via API endpoint.
                        SendWifiInfoResponse apiResponse = serverAPI
                                .uploadCurrentWifiInformation( wifiInformation )
                                .execute().body()
                                ;
                        Log.d( TAG, apiResponse.toString() ) ;
                    }
                    catch( ServerUnauthorizedException sux )
                    {
                        Log.e( TAG, "Server connection not authorized.", sux ) ;
                        m_proc.logout().authenticate() ;
                    }
                    catch( IOException iox )
                    {
                        Log.e( TAG, "Could not parse response body.", iox ) ;
                    }
                }
                else
                {
                    Log.i( TAG, "Server API implementation has not been generated." ) ;
                }
            }
        }
    }

//// Statics ///////////////////////////////////////////////////////////////////

    /** Tag to identify logging events from this class. */
    public static final String LOG_TAG = ServiceActionProcessor.class.getSimpleName() ;

    /** Tag to identify wake locks acquired on behalf of this app. */
    public static final String WAKELOCK_TAG =
        ServiceActionProcessor.class.getCanonicalName() ;

    /**
     * Indicates that the processor should never relay signals autonomously.
     */
    public static final int RELAY_FREQ_NEVER = -1 ;
    /**
     * Indicates that the processor should relay signals in real time, rather
     * than batching them up and delivering them on a heartbeat.
     */
    public static final int RELAY_FREQ_REAL_TIME = 0 ;

    /**
     * Given an intent, the method discovers, logs, and returns the action
     * token discovered therein, if any.
     * @param sig the intent to be processed
     * @return the intent's action token, if any
     */
    public static String discoverAction( Intent sig )
    {
        String sAction = null ;

        if( sig == null )
            Log.d( LOG_TAG, "Received a null intent." ) ;
        else
        {
            sAction = sig.getAction() ;
            Log.d( LOG_TAG, (new StringBuilder())
                    .append( "Discovered action [" )
                    .append(( sAction == null ? "(null)" : sAction ))
                    .append( "]." )
                    .toString()
                );
        }

        return sAction ;
    }

//// Members ///////////////////////////////////////////////////////////////////

    /**
     * The context in which the processor was instantiated. This will be used to
     * fetch string resources, application preferences, etc.
     */
    protected Context m_ctx = null ;

    /**
     * A persistent reference to the app's preferences.
     */
    protected SharedPreferences m_prefs = null ;

    /**
     * A persistent copy of the device's authentication fingerprints.
     */
    protected IBroadwayAuthDeviceInfo m_authinfo = null ;

    /**
     * A persistent reference to a Retrofit client that can reach the web
     * service.
     */
    protected BroadwayRetrofitClient<AcmeServerAPI> m_http = null ;

    /**
     * Controls the processor's behavior; it will process commands only if it
     * considers itself enabled.
     */
    protected boolean m_bEnabled = false ;

    /**
     * A persistent reference to an Android wake lock.
     * The processor will attempt to maintain a wake lock as long as it remains
     * enabled and the relay frequency is set to any value other than "never".
     */
    protected PowerManager.WakeLock m_lock = null ;

    /**
     * A collection of listeners to our login status. This is intentionally a
     * {@link Vector} because it should be synchronized, as these listeners
     * might be added or removed by any thread.
     */
    protected Vector<LoginListener> m_vListeners = null ;

    /**
     * Controls the processor's behavior; specifies the frequency at which the
     * processor should relay intercepted signals to the web service.
     */
    protected int m_zRelayFreq = RELAY_FREQ_REAL_TIME ;

//// Life Cycle ////////////////////////////////////////////////////////////////

    /**
     * Binds the processor to a context, and registers it as a preference change
     * listener.
     * @param ctx the context to which the processor will bind
     */
    public ServiceActionProcessor(Context ctx )
    {
        m_ctx = ctx ;
        m_prefs = PreferenceManager.getDefaultSharedPreferences(m_ctx) ;
        m_prefs.registerOnSharedPreferenceChangeListener(this) ;
        m_authinfo = FactoriesForBroadwayAuth.obtainDeviceInfo(m_ctx) ;
        this.initLoginListeners()
            .checkIsEnabledPref()
            .setRelayFrequency()
            .initDataClient( true )
            .manageWakeLock()
            ;
    }

    /**
     * Unregisters the processor as a preference change listener, and unbinds it
     * from its context. This should be the reciprocal of the constructor.
     */
    public void teardown()
    {
        this.releaseWakeLock() ;
        if( m_vListeners != null )
        {
            m_vListeners.clear() ;
            m_vListeners = null ;
        }
        m_authinfo = null ;
        this.logout()
            .setEnabled( false )
            ;
        m_prefs.unregisterOnSharedPreferenceChangeListener(this) ;
        m_prefs = null ;
        m_ctx = null ;
    }

//// BroadwayRetrofitClient.BroadwayAuthListener ///////////////////////////////

    @Override
    public void onBroadwayAuthChanged( BroadwayAuthAccount acct )
    {
        if( acct != null )
        {
            Log.d( LOG_TAG, (new StringBuilder())
                    .append( "Authenticated for [" )
                    .append( acct.getAcctName() )
                    .append( "]." )
                    .toString()
                );

            sendWifiInformation() ;
        }
        else
            Log.d( LOG_TAG, "Account came back null." ) ;

        this.notifyServerAuthChanged( acct ) ;
    }

    public ServiceActionProcessor sendWifiInformation()
    {
        // Conduct our network transaction on a background thread.
        // This avoids the NetworkOnMainThreadException.
        ( new Thread( new SendWifiInfoTask() ) ).start() ;
        return this ;
    }

//// BroadwayRetrofitClient.RestEndpointListener ///////////////////////////////

    @Override
    public void onRestEndpointChanged( String sURI )
    {
        Log.d( LOG_TAG, "Caught URI change event as endpoint change signal from client." ) ;
        this.notifyServerURIChanged( sURI ) ;
    }

//// SharedPreferences.OnSharedPreferenceChangeListener ////////////////////////

    @Override
    public void onSharedPreferenceChanged( SharedPreferences prefs, String sKey )
    {
        if( sKey == null ) return ; // trivially

        if( sKey.equals( m_ctx.getString( R.string.pref_service_enabled_key ) ) )
            this.checkIsEnabledPref() ;
        else if( sKey.equals( m_ctx.getString( R.string.pref_relay_freq_key ) ) )
            this.setRelayFrequency() ;
        else if( sKey.equals( m_ctx.getString( R.string.pref_key_server_url ) ) )
        { // Discard our old authentication.
            Log.d( LOG_TAG, "Caught URI change event as preference change." ) ;
            this.logout() ;
        }
        else if( sKey.equals( m_ctx.getString( R.string.pref_key_server_url_validated ) ) )
        { // If true, we're validated; if false, we're not.
            if( "true".equals( prefs.getString( sKey, null ) ) )
            { // Our endpoint is valid; try to re-authenticate.
                if( m_http != null )
                {
                    m_http.setEndpoint( prefs.getString(
                        m_ctx.getString( R.string.pref_key_server_url ),
                        null ) ) ;
                    this.authenticate() ;
                }
            }
            else
            {
                Log.i( LOG_TAG, "URI no longer valid." ) ;
                this.logout() ;
            }
        }
    }

//// HTTP Client Management ////////////////////////////////////////////////////

    /**
     * Attempts to acquire or renew the app's authentication from the web
     * service.
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor authenticate()
    {
        Log.i( LOG_TAG, "Renewing client authentication..." ) ;
        Intent sig = new Intent( m_ctx, AuthenticationActivity.class ) ;
        sig.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        );
        m_ctx.startActivity(sig) ;
        return this ;
    }

    /**
     * Initializes the client for the data server.
     * @param bReplace Replace any existing data client we might already have.
     * @return Returns <i>this</i> for chaining.
     */
    protected ServiceActionProcessor initDataClient(boolean bReplace )
    {
        if( m_http != null && !bReplace ) return this ;

        BroadwayRetrofitClient<AcmeServerAPI> httpData =
            new BroadwayRetrofitClient<>(
                AcmeServerAPI.class, m_ctx, m_authinfo ) ;
        if( m_http != null )
        {
            httpData.copyAuthListenersFrom( m_http )
                .copyEndpointListenersFrom( m_http )
            ;
        }
        if( httpData.canRegenerate() )
            httpData.regenerate() ;
        else
            this.authenticate() ;
        httpData.registerAuthListener( this )
            .registerEndpointListener( this )
        ;
        m_http = httpData ;
        return this ;
    }

    /**
     * Accesses the Retrofit client for the data server.
     * @return the service's HTTP client for data transfer, if any
     */
    public BroadwayRetrofitClient<AcmeServerAPI> getDataClient()
    { return m_http ; }

    /**
     * Returns the URI to which our web service client is currently connected,
     * if any.
     * @return the URI of the web service, if connected; null otherwise
     */
    public String getServerURI()
    {
        if( m_http == null ) return null ;
        String sClientURI = m_http.getEndpointURL() ;
        if( ! BitsStringUtils.isEmpty( sClientURI ) )
            return sClientURI ;
        else return null ;
    }

    /**
     * Accesses the web service account that is authenticated, if any.
     * @return the authenticated account, if any
     */
    public BroadwayAuthAccount getAuthenticatedAccount()
    {
        if( m_http != null )
            return m_http.getAccountInUse() ;
        else
            return null ;
    }

    /**
     * Releases the app's authentication/login session with the web service.
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor logout()
    {
        if( m_http != null && m_http.isAuthorized() )
        {
            m_http.logout() ;
            Log.i( LOG_TAG, "Released client authentication." ) ;
        }
        return this ;
    }

//// Wake Lock Management //////////////////////////////////////////////////////

    /**
     * Acquires or releases a wake lock based on the current status of the
     * processor and the app's preferences.
     * @return Returns <i>this</i> for chaining.
     */
    public synchronized ServiceActionProcessor manageWakeLock()
    {
        if( ! m_bEnabled || m_zRelayFreq == RELAY_FREQ_NEVER )
            return this.releaseWakeLock() ;
        else return this.acquireWakeLock() ;
    }

    /**
     * Acquires a wake lock, if not already held.
     * @return Returns <i>this</i> for chaining.
     * @see #manageWakeLock()
     */
    protected synchronized ServiceActionProcessor acquireWakeLock()
    {
        if( m_lock == null )
        {
            PowerManager mgrPower = ((PowerManager)
                    ( m_ctx.getSystemService( Context.POWER_SERVICE ) )) ;
            m_lock = mgrPower.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG ) ;
        }
        if( ! m_lock.isHeld() )
        {
            m_lock.acquire() ;
            Log.i( LOG_TAG, "Wake lock ACQUIRED." ) ;
        }
        return this ;
    }

    /**
     * Releases the wake lock held by the processor, if any.
     * @return Returns <i>this</i> for chaining.
     * @see #manageWakeLock()
     */
    protected synchronized ServiceActionProcessor releaseWakeLock()
    {
        if( m_lock != null && m_lock.isHeld() )
        {
            m_lock.release() ;
            Log.i( LOG_TAG, "Wake lock RELEASED." ) ;
        }
        return this ;
    }

//// Login Listener Management /////////////////////////////////////////////////

    /**
     * Initializes our vector of login listeners.
     * @return Returns <i>this</i> for chaining.
     */
    protected ServiceActionProcessor initLoginListeners()
    {
        if( m_vListeners == null )
            m_vListeners = new Vector<>() ;
        return this ;
    }

    /**
     * Accessor for the vector of login listeners.
     * Creates the vector if it is still null.
     * @return Returns <i>this</i> for chaining.
     */
    public Vector<LoginListener> getListeners()
    {
        if( m_vListeners == null ) this.initLoginListeners() ;
        return m_vListeners ;
    }

    /**
     * Registers a new listener, if it has not already been registered.
     * The listener also receives notifications of both server events, in order
     * to set its initial state based on real data.
     * @param l the listener to be registered
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor addListener(LoginListener l )
    {
        if( ! this.getListeners().contains(l) )
            m_vListeners.add(l) ;
        this.notifyServerURIChanged( l, this.getServerURI() )
            .notifyServerAuthChanged( l, this.getAuthenticatedAccount() )
            ;
        return this ;
    }

    /**
     * Unregisters a listener, if it has already been registered.
     * @param l the listener to be unregistered
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor removeListener(LoginListener l )
    {
        if( this.getListeners().contains(l) )
            m_vListeners.remove(l) ;
        return this ;
    }

    protected ServiceActionProcessor notifyServerURIChanged(String sURI )
    {
        for( LoginListener l : this.getListeners() )
            this.notifyServerURIChanged( l, sURI ) ;
        return this ;
    }

    protected ServiceActionProcessor notifyServerURIChanged(LoginListener l, String sURI )
    { l.onServerURIChanged( sURI ) ; return this ; }

    protected ServiceActionProcessor notifyServerAuthChanged(BroadwayAuthAccount acct )
    {
        for( LoginListener l : this.getListeners() )
            this.notifyServerAuthChanged( l, acct ) ;
        return this ;
    }

    protected ServiceActionProcessor notifyServerAuthChanged(LoginListener l, BroadwayAuthAccount acct )
    { l.onServerAuthChanged( acct ) ; return this ; }

//// Other Accessors / Mutators ////////////////////////////////////////////////

    /**
     * Accesses the context in which the processor is operating (usually, an
     * instance of {@link KazooService}.
     * @return the operational context of the processor
     */
    public Context getContext()
    { return m_ctx ; }

    /**
     * Indicates whether the processor considers itself enabled.
     * @return {@code true} iff the processor considers itself enabled
     */
    public boolean isEnabled()
    { return m_bEnabled ; }

    /**
     * Specifies whether the processor should consider itself enabled.
     * @param bEnabled the new flag value
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor setEnabled(boolean bEnabled )
    {
        m_bEnabled = bEnabled ;
        Log.i( LOG_TAG, (new StringBuilder())
                .append( "Relay processor is now [" )
                .append(( m_bEnabled ? "enabled]." : "disabled]." ))
                .toString()
            );
        this.manageWakeLock() ;
        return this ;
    }

    /**
     * Accessor for the processor's persistent reference to the app's
     * preferences.
     * @return the app's preferences
     */
    public SharedPreferences getPrefs()
    { return m_prefs ; }

    /**
     * Sets the processor's signal relay frequency to the value that is defined
     * in the app's preferences.
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor setRelayFrequency()
    {
        int zFreq = RELAY_FREQ_REAL_TIME ;

        if( m_prefs != null )
        {
            final String sFreq = m_prefs.getString(
                m_ctx.getString( R.string.pref_relay_freq_key ),
                Integer.toString( RELAY_FREQ_REAL_TIME ) ) ;
            try { zFreq = Integer.parseInt( sFreq ) ; }
            catch( NumberFormatException xFormat )
            {
                Log.w( LOG_TAG, (new StringBuilder())
                        .append( "The frequency preference value [" )
                        .append( sFreq )
                        .append( "] cannot be parsed as an integer." )
                        .toString()
                    );
            }
            this.setRelayFrequency( zFreq ) ;
        }

        return this.setRelayFrequency( zFreq ) ;
    }

    /**
     * Sets the processor's signal relay frequency to the specified value.
     * @param zFreq the frequency at which signals should be relayed, in seconds
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor setRelayFrequency(int zFreq )
    {
        Log.i( LOG_TAG, (new StringBuilder())
                .append( "Setting relay frequency to [" )
                .append( zFreq )
                .append( "]." )
                .toString()
            );
        m_zRelayFreq = zFreq ;
        return this ;
    }

//// Other instance methods ////////////////////////////////////////////////////

    /**
     * Sets the processor's "enabled" flag based on the value of the
     * {@code service.enabled} preference.
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor checkIsEnabledPref()
    {
        if( m_prefs == null )
        {
            Log.w( LOG_TAG, "Processor tried to check enabled status, but can't get a reference to app preferences." ) ;
            return this ;
        }

        return this.setEnabled( m_prefs.getBoolean(
            m_ctx.getString( R.string.pref_service_enabled_key ), false ) ) ;
    }

    /**
     * Dumps a bunch of debug info to the logs.
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor dump()
    {
        Log.d( LOG_TAG, (new StringBuilder())
                .append( "DEBUG - Processor is [" )
                .append(( m_bEnabled ? "enabled]." : "disabled]." ))
                .toString()
            );
        Log.d( LOG_TAG, (new StringBuilder())
                .append( "DEBUG - Relay frequency [" )
                .append( m_zRelayFreq )
                .append( "]" )
                .toString()
            );
        return this ;
    }

    /**
     * Processes a received intent.
     * @param sig the received intent
     * @return Returns <i>this</i> for chaining.
     */
    public ServiceActionProcessor process(Intent sig )
    {
        final String sAction = discoverAction( sig ) ;

        if( sAction == null )
        {
            Log.i( LOG_TAG, "Ignoring intent without an action." ) ;
            return this ;
        }

        Log.i( LOG_TAG, (new StringBuilder())
                .append( "Caught signal [" )
                .append( sAction )
                .append( "] for processing." )
                .toString()
            );

        return this ;
    }

}
