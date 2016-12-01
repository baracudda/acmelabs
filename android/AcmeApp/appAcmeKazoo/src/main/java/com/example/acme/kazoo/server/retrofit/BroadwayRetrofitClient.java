package com.example.acme.kazoo.server.retrofit;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.auth.IBroadwayAuthAccountInfo;
import com.blackmoonit.androidbits.auth.IBroadwayAuthDeviceInfo;
import com.blackmoonit.androidbits.concurrent.ThreadTask;
import com.example.acme.kazoo.account.manager.AccountAuthenticator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Retrofit REST client that support BroadwayAuth website authentication.
 */
public class BroadwayRetrofitClient<I>
extends DeviceInfoRetrofitClient<I>
implements IBroadwayAuthAccountInfo
{
	private static final String TAG =
		BroadwayRetrofitClient.class.getSimpleName() ;

	// Inner classes and interfaces...

	/**
	 * An implementation would take action whenever the client's authorization
	 * credentials have changed.
	 */
	public interface BroadwayAuthListener
	{
		/**
		 * This method will be notified whenever the Broadway client's
		 * authorization credentials have changed.
		 * @param aAcctInfo the account currently in use by the client; this might be
		 *  null, so the implementation should handle that case gracefully
		 */
		void onBroadwayAuthChanged(BroadwayAuthAccount aAcctInfo) ;
	}

	// Instance fields...

	/** List of listeners to the client's authorization token. */
	protected ArrayList<BroadwayAuthListener> m_aAuthListeners = new ArrayList<>() ;
	/** The account that is currently in use for authorization. */
	protected BroadwayAuthAccount m_acctInUse = null ;
	/** The account chosen from the list of available accounts in the manager.*/
	protected BroadwayAuthAccount m_acctChosen = null ;
	/** Indicates whether an authorization request is already in progress. */
	protected boolean m_bAuthInProgress = false ;

	/** Activity which provides an account manager and app contexts. */
	protected WeakReference<Activity> mw_actAcctMgr = null ;

	// Constructors...

	public BroadwayRetrofitClient( Class<I> clsAPI, Context ctx,
									  IBroadwayAuthDeviceInfo devinfo )
	{
		super( clsAPI, ctx, devinfo) ;
		if( ctx instanceof Activity )
			this.setAccountManagerActivity( (Activity)ctx ) ;
	}

	// Overrides of BasicRetrofitClient<I>...

	@Override
	protected void setup( Context ctx ) {
		super.setup(ctx);
		if( ctx instanceof Activity )
			this.setAccountManagerActivity((Activity) ctx) ;
	}

	@Override
	protected void cleanup( Context ctx ) {
		//nothing much to cleanup
		super.cleanup(ctx);
	}

	@Override // trivial override for precise chaining
	public synchronized BroadwayRetrofitClient<I> registerEndpointListener(
		RestEndpointListener o )
	{ super.registerEndpointListener( o ) ; return this ; }

	@Override // trivial override for precise chaining
	public synchronized BroadwayRetrofitClient<I> unregisterEndpointListener(
		RestEndpointListener o )
	{ super.unregisterEndpointListener( o ) ; return this ; }

	@Override // trivial override for precise chaining
	public synchronized BroadwayRetrofitClient<I> setEndpoint( String sURL)
	{ super.setEndpoint( sURL ) ; return this ; }

	/** Notifies all auth token listeners. */
	@Override
	protected synchronized void onEndpointChanged()
	{
		logout(); //forget credentials of last website if we change the website URL
	}

	/**
	 * Override this method if you want something special to happen when the
	 * {@link OkHttpClient} is being created by {@link #getRestClient}.
	 * @param aBuilder - the HTTP client builder.
	 * @return Returns the builder after adding on to it.
	 */
	@Override
	protected synchronized OkHttpClient.Builder onHttpClientCreate(OkHttpClient.Builder aBuilder)
	{
		return super.onHttpClientCreate(aBuilder)
			.addInterceptor( new RequestInterceptorBroadwayAuth( this, m_devinfo ) )
			;
	}

	@Override // trivial override for precise chaining
	public BroadwayRetrofitClient<I> setAdapterBuilder( Retrofit.Builder b )
	{ super.setAdapterBuilder(b) ; return this ; }

	@Override // trivial override for precise chaining
	public BroadwayRetrofitClient<I> regenerate()
	{ super.regenerate() ; return this ; }

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean canRegenerate()
	{
		if( ! super.canRegenerate() ) return false ;
		// Now check for anything that's specific to this client type.
		// Namely, can we build our default request interceptor?
		if( m_acctInUse == null ) return false ;
		return true ;
	}

	/**
	 * During {@link #onSharedPreferenceChanged}, if the config endpoint has
	 * changed, then log out of the previous account.
	 */
	@Override
	protected void onConfigEndpointChanged()
	{ this.logout() ; }

	// Extensions...

	/**
	 * Accesses the list of objects listening to the client's auth credentials.
	 * @return a list of objects listening to the client's auth credentials
	 */
	protected ArrayList<BroadwayAuthListener> getAuthListeners()
	{
		return m_aAuthListeners ;
	}

	/**
	 * Registers an object as an auth token listener.
	 * @param l an auth token listener
	 * @return the client, for chained invocations
	 */
	public synchronized BroadwayRetrofitClient<I> registerAuthListener(
		BroadwayAuthListener l )
	{
		if( ! m_aAuthListeners.contains(l) )
			m_aAuthListeners.add(l) ;
		this.notifyAuthListener(l) ;
		return this ;
	}

	/**
	 * Copies all auth listeners from some other client into this client. This
	 * method could be useful to consumers of the client who want to replace it
	 * with a new client.
	 * @param that the other client, from whom listeners should be copied
	 * @return this client, for chained invocations
	 */
	public synchronized BroadwayRetrofitClient<I> copyAuthListenersFrom(
		BroadwayRetrofitClient<?> that )
	{
		for( BroadwayAuthListener l : that.getAuthListeners() )
			this.registerAuthListener(l) ;
		return this ;
	}

	/**
	 * Removes an object as an auth token listener.
	 * @param o an auth token listener
	 * @return the client, for chained invocations
	 */
	public synchronized BroadwayRetrofitClient<I> unregisterAuthListener(
		BroadwayAuthListener o )
	{
		if( m_aAuthListeners.contains(o) )
			m_aAuthListeners.remove(o) ;
		return this ;
	}

	/** Notify all listeners of an authorization change. */
	protected synchronized void notifyAuthListeners()
	{
		for( BroadwayAuthListener l : m_aAuthListeners )
			this.notifyAuthListener(l) ;
	}

	/**
	 * Notifies a single listener of an authorization change. This is a
	 * separate method only so that {@link #notifyAuthListeners} can use it in
	 * a loop but {@link #registerAuthListener} can use it individually. If the
	 * logic for what happens during a notification ever gets more complicated,
	 * this method can be a single point of modification.
	 * @param l the listener to be notified
	 */
	protected synchronized void notifyAuthListener( BroadwayAuthListener l )
	{ l.onBroadwayAuthChanged( m_acctInUse ) ; }

	/**
	 * Sets the user account for authorization.
	 * @param acct a user account
	 */
	public synchronized BroadwayRetrofitClient<I> setAccountInUse(
		BroadwayAuthAccount acct )
	{
		m_acctInUse = acct ;
		Log.d( TAG, (new StringBuffer())
			.append( "Setting account in use [" )
			.append(( acct != null ? acct.getAcctName() : "(null)" ))
			.append( "]." )
			.toString()
			);
		m_bAuthInProgress = false ;
		m_bIsImplReady = false ;
		this.notifyAuthListeners() ;
		return this ;
	}

	@Override
	public synchronized BroadwayAuthAccount getAccountInUse()
	{ return m_acctInUse ; }

	public synchronized BroadwayRetrofitClient<I> setAccountChosen(
		BroadwayAuthAccount acct )
	{
		m_acctChosen = acct ;
		Log.d( TAG, (new StringBuffer())
			.append( "Setting chosen account [" )
			.append(( acct != null ? acct.getAcctName() : "(null)" ))
			.append( "]." )
			.toString()
			);
		return this ;
	}

	public synchronized BroadwayAuthAccount getAccountChosen()
	{ return m_acctChosen ; }

	/**
	 * In order to work with the Android {@link AccountManager}, we need to
	 * provide it with a Context so that it can call
	 * {@link Activity#startActivity} with it using our classes.
	 * @param act the activity to use for context
	 * @return the client, for chained invocations
	 */
	public synchronized BroadwayRetrofitClient<I> setAccountManagerActivity(
		Activity act )
	{
//		if( (mw_actAcctMgr==null || mw_actAcctMgr.get()==null) && act != null )
		mw_actAcctMgr = new WeakReference<>( act ) ;
		m_bIsImplReady = false ;
		return this ;
	}

	/** Accessor for the account manager activity. */
	public synchronized Activity getAccountManagerActivity()
	{
		return ( mw_actAcctMgr != null ) ? mw_actAcctMgr.get() : null ;
	}

	/**
	 * Gets an {@link AccountManager} instance linked to the client's activity.
	 * @return the {@link AccountManager} for the linked activity
	 */
	public AccountManager getAndroidAcctMgr()
	{
		Activity theAct = this.getAccountManagerActivity() ;
		return ( theAct != null ? AccountManager.get(theAct) : null ) ;
	}

	/**
	 * DataService uses this method to fire off whole auth token mechanism.
	 * DataServiceAuth avoids this method to fend off infinite loops.
	 */
	public synchronized void checkAuthToken()
	{
		if( getAccountManagerActivity() == null )
			return ;

		if( m_acctInUse == null || m_acctInUse.getAcctAuthToken() == null )
			this.requestAuthToken() ;
		else if( ! m_bAuthInProgress )
			this.notifyAuthListeners() ;
	}

	/**
	 * Standardizes the way that the instance uses {@link #acquireAuthToken}
	 * internally.
	 */
	protected synchronized void requestAuthToken()
	{
		Activity theAct = getAccountManagerActivity();
		if (theAct!=null) {
			this.acquireAuthToken( this.getAccountManagerActivity(), null );
			m_bAuthInProgress = true;
			m_bIsImplReady = false;
		}
	}

	/**
	 * Call this method if you have a valid auth token, but still receive a 401 server error.
	 */
	@SuppressWarnings("MissingPermission")
	public synchronized void clearStaleAuthToken() {
		BroadwayAuthAccount theAcct = getAccountInUse();
		Activity theAct = getAccountManagerActivity();
		if (theAct!=null) {
			AccountManager theAndroidAcctMgr = AccountManager.get(theAct);
			theAndroidAcctMgr.invalidateAuthToken(theAcct.getAcctType(), theAcct.getAcctAuthToken());
		}
		theAcct.setAcctAuthToken(null);
	}

	/**
	 * Get an authorization token for the account, creating the account first if
	 * necessary. If it doesn't yet exist, add it and return its token; if it
	 * does exist, just return the token. If there is more than one account,
	 * display a chooser.
	 * @param actAcctMgr - the context to use for the AccountManager.
	 * @param taskHandleToken - (can be NULL) callback when auth token is received.
	 */
	public void acquireAuthToken( final Activity actAcctMgr,
								  final Runnable taskHandleToken )
	{
		if( m_bAuthInProgress ) return ;  // Try to ensure only one concurrence.
		final AccountManager mgr = AccountManager.get(actAcctMgr) ;

		//if we are trying to get an auth token, but we think we are already authorized, then
		//clear out the obviously stale auth token and let the AccountManager get us a new one.
		if (isAuthorized()) {
			clearStaleAuthToken();
		}

		final String[] theFeatureList = null;
		final Bundle theAccountOptions = null;
		final Bundle theAuthTokenOptions = null;
		/*
		Log.d("AuthToken", "theCallback=" + ((theCallback == null) ? "null" : "not null"));
		if( theCallback == null )
			Log.e("AuthToken", ReportAnExceptionHandler.Utils.getDebugInstructionTrace(this.getContext(), new Exception()));
		*/
		final AccountManagerCallback<Bundle> mgrcb =
			new AccountManagerCallback<Bundle>()
		{
			private final String am_sTag = "AuthToken" ;

			@Override
			public void run( AccountManagerFuture<Bundle> result )
			{
				Bundle bndl = null ;
				try
				{
					bndl = result.getResult() ;
					//Log.d(am_sTag, "got the AcctMgrCallback future result");
					BroadwayRetrofitClient.this.setAccountChosen(
						BroadwayAuthAccount.fromBundle( mgr, bndl ) ) ;
				}
				catch( OperationCanceledException ocx )
				{
					Log.i( am_sTag, "Login was cancelled." ) ;
					if( getAccountChosen() != null )
						getAccountChosen().logout(mgr) ;
					setAccountChosen(null) ;
				}
				catch( Exception x )
				{
					Log.e( am_sTag,
						"Exception while processing account info bundle", x ) ;
				}

				setAccountInUse( getAccountChosen() ) ;

				if( getAccountChosen() == null )
				{
					Log.e( am_sTag, (new StringBuffer())
						.append( "Auth failed, no account chosen. Bundle: " )
						.append(( bndl != null ? bndl.toString() : "(null)" ))
						.toString()
						);
				}

				//Log.d(am_sTag, "is there a callback?");
				if( taskHandleToken != null )
				{
					//Log.d(am_sTag, "execute the callback on the UI thread");
					actAcctMgr.runOnUiThread(taskHandleToken);
				}
			}
		} ;

		new ThreadTask( new Runnable()
		{
			private final String am_sTag = "AuthToken" ;
			private final String am_sAcctType = actAcctMgr.getString(
				AccountAuthenticator.RES_ACCOUNT_TYPE ) ;
			private final String am_sTokenType =
				AccountAuthenticator.AUTHTOKEN_KIND_FULL_ACCESS ;

			@SuppressWarnings("MissingPermission")
			@Override
			public void run()
			{
				if( getAccountChosen() != null )
				{
					Log.d(am_sTag, "calling getAuthToken()");
					mgr.getAuthToken( getAccountChosen(), am_sAcctType,
						theAuthTokenOptions, actAcctMgr, mgrcb, null ) ;
				}
				else
				{
					Log.d(am_sTag, "calling getAuthTokenByFeatures()");
					mgr.getAuthTokenByFeatures( am_sAcctType, am_sTokenType,
							theFeatureList, actAcctMgr, theAccountOptions,
							theAuthTokenOptions, mgrcb, null ) ;
				}

			}
		}).execute() ;
	}

	/**
	 * Invalidates the auth token for the account. This method should be called
	 * to force a new login whenever a HTTP 401 response is returned from the
	 * data server.
	 */
	public void invalidateAuthToken()
	{
		// Hold a reference here because we're about to break it in the instance
		final BroadwayAuthAccount acctClosing = this.getAccountInUse() ;
		if( acctClosing != null )
		{ // Clear out all cached authorization tokens and data.
			this.setAccountInUse( null ) ;
			final AccountManager mgr = this.getAndroidAcctMgr() ;
			if( mgr != null )
			{
				acctClosing.logout( mgr ) ;
			}
			this.notifyAuthListeners() ;
		}
	}

	/**
	 * Remove tokens such that auto-login will fail and force the user to
	 * re-enter a password next time.
	 */
	public void logout()
	{
		BroadwayAuthAccount acct = this.getAccountInUse() ;
		if( this.getAccountInUse() != null )
		{
			Log.i( TAG, (new StringBuffer())
				.append( "Logging out of account [" )
				.append( acct.getAcctName() )
				.append( "]" )
				.toString()
				);
			acct.logout( this.getAndroidAcctMgr() ) ;
			this.invalidateAuthToken() ;
		}
		this.setAccountChosen( null ) ;
		m_bIsImplReady = false ;
	}

	public boolean isAuthorized()
	{ return ( getAccountInUse() != null && getAccountInUse().isAuthorized() ) ; }
}
