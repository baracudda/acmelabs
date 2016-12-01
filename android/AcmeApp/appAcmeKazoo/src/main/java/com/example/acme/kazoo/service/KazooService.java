package com.example.acme.kazoo.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Persistent service which drives the operation of the app.
 * @see <a href="https://developer.android.com/guide/components/bound-services.html">
 *   Android Bound Services</a>
 */
public class KazooService extends Service
{
    static private final String TAG = KazooService.class.getSimpleName() ;

    /**
     * Provides an API to the service.
     */
    public static class IntentFactory
    extends com.blackmoonit.androidbits.utils.IntentFactory
    {
        public static final String ACTION_PREFIX = "com.example.acme.kazoo.action." ;

        /**
         * Distinctive action for starting the service.
         */
        public static final String ACTION_KICKOFF = ACTION_PREFIX + "SERVICE_KICKOFF" ;
        /**
         * Distinctive action for stopping the service.
         */
        public static final String ACTION_STOP = ACTION_PREFIX + "SERVICE_STOP" ;

        /**
         * Returns an intent bound to {@link KazooService}.
         * @param ctx the context in which to create the intent
         * @return the bound intent
         */
        public static Intent getBoundIntent( Context ctx )
        { return IntentFactory.getBoundIntent( ctx, KazooService.class ) ; }

        /**
         * Creates a "kickoff" intent and uses it to start the service within
         * the specified context.
         * @param ctx the context in which the service should start
         */
        public static void kickoff( Context ctx )
        {
            ctx.startService( IntentFactory.getBoundIntent(ctx)
                .setAction( ACTION_KICKOFF ) ) ;
        }

        /**
         * Creates a "stop" intent and uses it to stop the service within the
         * specified context
         * @param ctx the context in which the service should stop
         */
        public static void stop( Context ctx )
        {
            ctx.startService( IntentFactory.getBoundIntent(ctx)
                .setAction( ACTION_STOP ) ) ;
        }
    }

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
		public KazooService getService() {
            // Return this instance of LocalService so clients can call public methods
			return KazooService.this;
		}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * A persistent reference to the processor that handles received signals.
     */
    protected ServiceActionProcessor m_proc = null ;

    @Override
    public void onCreate()
    {
        this.initProcessor() ;
        Log.i(TAG, "Started RelayService." ) ;
    }

    @Override
    public int onStartCommand( Intent sig, int zFlags, int nStartID )
    {
        super.onStartCommand( sig, zFlags, nStartID ) ;

        final String sAction = ServiceActionProcessor.discoverAction( sig ) ;

        if( sAction != null ) switch( sAction )
        {
            case IntentFactory.ACTION_KICKOFF:
                Log.i(TAG, "Received kickoff signal." ) ;
                this.initProcessor() ;
                break ;
            case IntentFactory.ACTION_STOP:
                Log.i(TAG, "Received stop signal." ) ;
                this.stopSelf() ;
                break ;
            default:
                m_proc.process( sig ) ;
        }

        return Service.START_STICKY ;
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "Destroying service. Releasing all references." ) ;
        m_proc.teardown() ;
        m_proc = null ;
        super.onDestroy() ;
    }

    /**
     * Accesses the relay processor bound to the service.
     * @return the service's relay processor
     */
    public ServiceActionProcessor getProcessor()
    { return m_proc ; }

    /**
     * Initializes the signal processor if an instance is not already created.
     * @return Returns <i>this</i> for chaining.
     */
    protected KazooService initProcessor()
    {
        if( m_proc == null )
            m_proc = new ServiceActionProcessor(this) ;
        return this ;
    }

}
