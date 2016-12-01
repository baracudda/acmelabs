package com.example.acme.kazoo.server.retrofit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.acme.kazoo.R;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static com.blackmoonit.androidbits.app.AppPreferenceBase.getPrefs;
import static com.blackmoonit.androidbits.app.AppPrefsDebug.isDebugMode;

/**
 * Bare minimum necessary to generate and access a Retrofit API implementation
 */
public class BasicRetrofitClient<I>
implements SharedPreferences.OnSharedPreferenceChangeListener
{
	/** Logging tag. */
	private static final String TAG = BasicRetrofitClient.class.getSimpleName();

	/**
	 * An implementation would take action whenever the client's endpoint URL
	 * has changed.
	 */
	public interface RestEndpointListener
	{
		void onRestEndpointChanged(String url) ;
	}

	/** Persistent Retrofit OkHttpClient */
	protected OkHttpClient m_okhttp = null ;
	/** Logging is handled with an interceptor */
	protected HttpLoggingInterceptor m_okhttpLogger = null;

	/** Registered listeners to the client's endpoint URL */
	protected ArrayList<RestEndpointListener> m_aEndpointListeners = null ;
	/** The endpoint URL to which requests are sent. */
	protected String m_sEndpointURL = null ;
	/** The URL gleaned from app Settings. */
	protected String m_sConfigURLKey = null ;
	/** A validation indicator gleaned from app Settings. */
	protected String m_sConfigURLValidated = null ;

	/**
	 * The interface that defines the REST API for Retrofit. Because Java
	 * doesn't let you discover the value of your own instance's template
	 * parameter, we have to store the hint here.
	 */
	protected Class<I> m_clsAPI = null ;
	/**
	 * The RestAdapter.Builder class to be used when regenerating the API
	 * implementation. If left as {@code null}, then a default will be used.
	 */
	protected Retrofit.Builder m_builder = null ;
	/** cache of the rest adapter the m_impl is constructed from. */
	protected Retrofit m_adapter = null;
	/** The implementation of the API, as created by Retrofit. */
	protected I m_impl = null ;
	/**
	 * Indicates whether the implementation instance is current. This will be
	 * set to false any time the endpoint or client changes. The flag is set to
	 * true only by {@link #regenerate}.
	 */
	protected boolean m_bIsImplReady = false ;
	/** Context to use for various purposes like getting strings. */
	protected Context m_appctx;

	/** Disallow the default constructor. */
	protected BasicRetrofitClient() {}

	/**
	 * Because Java won't let you discover the class's template parameter later,
	 * a hint must be provided in the constructor.
	 * @param clsAPI the interface that defines the REST API for Retrofit
	 */
	public BasicRetrofitClient( Class<I> clsAPI, Context aContext )
	{
		m_clsAPI = clsAPI ;
		m_appctx = aContext.getApplicationContext();
		setup( aContext );
	}

	public Context getContext() {
		return m_appctx;
	}

	protected void setup( Context ctx ) {
		m_sConfigURLKey = ctx.getString( R.string.pref_key_server_url );
		m_sConfigURLValidated = ctx.getString( R.string.pref_key_server_url_validated );
		SharedPreferences theSettings = getPrefs(ctx) ;
		if( theSettings != null )
		{
			theSettings.registerOnSharedPreferenceChangeListener(this) ;
			this.onSharedPreferenceChanged( theSettings, null ) ;
		}
	}

	@Override
	protected void finalize() throws Throwable
	{
		cleanup( getContext() );
		super.finalize() ;
	}

	protected void cleanup( Context ctx ) {
		SharedPreferences theSettings = getPrefs(ctx) ;
		if( theSettings != null ) {
			theSettings.unregisterOnSharedPreferenceChangeListener(this);
		}
	}

	/**
	 * Accesses the list of objects listening to the client's endpoint.
	 * @return a list of objects listening to the client's endpoint
	 */
	protected ArrayList<RestEndpointListener> getEndpointListeners()
	{
		if( m_aEndpointListeners == null )
			m_aEndpointListeners = new ArrayList<>() ;
		return m_aEndpointListeners ;
	}

	/**
	 * Registers an object as an endpoint listener.
	 * @param l an endpoint listener
	 * @return the client, for chained invocations
	 */
	public synchronized BasicRetrofitClient<I> registerEndpointListener(
		RestEndpointListener l )
	{
		if( ! this.getEndpointListeners().contains(l) )
			m_aEndpointListeners.add(l) ;
		this.notifyEndpointListener(l) ;
		return this ;
	}

	/**
	 * Copies all endpoint listeners from some other client into this client.
	 * This method could be useful to consumers of the client who want to
	 * replace it with a new client.
	 * @param that the other client, from which listeners should be copied
	 * @return this client, for chained invocations
	 */
	public synchronized BasicRetrofitClient<I> copyEndpointListenersFrom(
		BasicRetrofitClient<?> that )
	{
		for( RestEndpointListener l : that.getEndpointListeners() )
			this.registerEndpointListener(l) ;
		return this ;
	}

	/**
	 * Removes an object as an endpoint listener. Any class that can register
	 * itself should also remove itself when its lifecycle ends.
	 * @param o an endpoint listener
	 * @return the client, for chained invocations
	 */
	public synchronized BasicRetrofitClient<I> unregisterEndpointListener(
		RestEndpointListener o )
	{
		if( this.getEndpointListeners().contains(o) )
			m_aEndpointListeners.remove(o) ;
		return this ;
	}

	/**
	 * Sets the client's endpoint URL and triggers a notification to all
	 * registered listeners.
	 * @param sURL the new endpoint URL
	 * @return the client, for chained invocations
	 */
	public synchronized BasicRetrofitClient<I> setEndpoint( String sURL )
	{
		if (sURL!=null && !sURL.endsWith("/"))
			sURL += "/";
		if( sURL != null && ! sURL.equals(m_sEndpointURL) )
		{
			Log.i( TAG, (new StringBuffer())
				.append("set endpoint [")
				.append(sURL)
				.append("] for API ")
				.append(m_clsAPI.getCanonicalName())
				.toString()
			) ;
			m_sEndpointURL = sURL ;
			m_bIsImplReady = false ;
			this.onEndpointChanged() ;
			this.notifyEndpointListeners() ;
		}
		return this ;
	}

	/**
	 * Override this method if you want something special to happen when the
	 * endpoint is changed by {@link #setEndpoint}.
	 */
	protected synchronized void onEndpointChanged()
	{}

	/**
	 * Indicates whether the client's endpoint URL has been set.
	 * @return true if the URL is non-null and non-empty
	 */
	public boolean hasEndpointURL()
	{ return( m_sEndpointURL != null && ! m_sEndpointURL.equals("") ) ; }

	/**
	 * Alias for hasEndpointURL().
	 * @return Returns TRUE if the URL is not null.
	 * @see BroadwayRetrofitClient#hasEndpointURL()
	 */
	public boolean isEndpointDefined() {
		return hasEndpointURL();
	}

	/**
	 * Accesses the client's endpoint URL.
	 * @return the client's endpoint URL
	 */
	@SuppressWarnings("unused")
	public String getEndpointURL()
	{ return m_sEndpointURL ; }

	/**
	 * Notifies all registered endpoint listeners that a change has occurred.
	 */
	protected synchronized void notifyEndpointListeners()
	{
		if( m_aEndpointListeners == null ) return ;
		for( RestEndpointListener l : m_aEndpointListeners )
			this.notifyEndpointListener(l) ;
	}

	/**
	 * Notifies a single endpoint listener of a change.
	 * @param l the listener to be notified
	 */
	protected synchronized void notifyEndpointListener( RestEndpointListener l )
	{ l.onRestEndpointChanged( m_sEndpointURL ) ; }

	@Override
	public void onSharedPreferenceChanged( SharedPreferences cfg, String sKey )
	{
		if( cfg != null )
		{
			if( sKey == null || sKey.equals( m_sConfigURLValidated ) )
			{
				if("true".equals( cfg.getString(m_sConfigURLValidated,"false")) ) {
					this.setEndpoint( cfg.getString(m_sConfigURLKey, null) );
					regenerate();
				} else {
					Log.w(TAG, (new StringBuffer())
							.append("endpoint is not validated for API ")
							.append(m_clsAPI.getCanonicalName())
							.toString()
					);
				}
			}

			if ( sKey != null && sKey.equals(m_sConfigURLKey) )
			{
				this.onConfigEndpointChanged() ;
			}
		}
	}

	/**
	 * Generates the HTTP client for use by Retrofit.
	 * @return an instance of {@link OkHttpClient}
	 */
	public synchronized OkHttpClient getRestClient()
	{
		if( m_okhttp == null )
		{
			OkHttpClient.Builder theHttpClientBuilder = new OkHttpClient.Builder();
			m_bIsImplReady = false ;
			//standard error handler
			theHttpClientBuilder = this.onHttpClientErrorHandlerCreate(theHttpClientBuilder) ;
			//descendants add stuff in the middle
			theHttpClientBuilder = this.onHttpClientCreate(theHttpClientBuilder) ;
			//logging must be the last interceptor, if at all
			theHttpClientBuilder = this.onHttpClientLoggerCreate(theHttpClientBuilder) ;
			m_okhttp = theHttpClientBuilder.build() ;
		}
		return m_okhttp ;
	}

	/**
	 * Override this method if you want something special to happen when the
	 * {@link OkHttpClient} is being created by {@link #getRestClient}.
	 * The method sets a default timeout for reads and connects to 45 seconds.
	 * This may be overridden in a descendant class.
	 * @param aBuilder - the HTTP client builder.
	 * @return Returns the builder after adding on to it.
	 */
	protected synchronized OkHttpClient.Builder onHttpClientCreate(OkHttpClient.Builder aBuilder)
	{
		return aBuilder.readTimeout( 45L, TimeUnit.SECONDS )
			.connectTimeout( 45L, TimeUnit.SECONDS )
			;
	}

	/**
	 * Override this method if you want special logging to happen when the
	 * {@link OkHttpClient} is being created by {@link #getRestClient}.
	 */
	protected synchronized OkHttpClient.Builder onHttpClientLoggerCreate(OkHttpClient.Builder aBuilder)
	{
		m_okhttpLogger = new HttpLoggingInterceptor();
		//NONE, BASIC, HEADERS, BODY
		HttpLoggingInterceptor.Level theLogLevel = HttpLoggingInterceptor.Level.NONE;
		if (isDebugMode())
			theLogLevel = HttpLoggingInterceptor.Level.BODY;
		m_okhttpLogger.setLevel(theLogLevel);
		aBuilder.addInterceptor(m_okhttpLogger);
		return aBuilder;
	}

	protected synchronized OkHttpClient.Builder onHttpClientErrorHandlerCreate(OkHttpClient.Builder aBuilder)
	{
		Interceptor theErrorHandler = new Interceptor() {

			@Override
			public Response intercept(Chain chain) throws IOException {
				Response theResponse = chain.proceed(chain.request());
				if (! theResponse.isSuccessful())
				{
					Log.e(TAG, (new StringBuffer())
							.append("HTTP Request failed with status: [")
							.append(theResponse.code())
							.append("] and msg: [")
							.append(theResponse.message())
							.append("]")
							.toString()
					);
					if (theResponse.code()==401 || theResponse.code()==403)
						throw new ServerUnauthorizedException();
					else
						throw new ServerException(theResponse.code(),
								theResponse.message()
						);
				}
				return theResponse;
			}

		};
		aBuilder.addInterceptor(theErrorHandler);
		return aBuilder;
	}

	/**
	 * Returns the default RetroFit adapter builder, using the data that is
	 * currently stored in the client instance. Call this method when you want
	 * to regenerate the client's API implementation using your own builder
	 * settings.
	 * @return a builder for the client's REST API implementation
	 */
	public Retrofit.Builder getAdapterBuilder()
	{
		if( m_builder != null )
			return m_builder ;
		else
			return this.getDefaultAdapterBuilder() ;
	}

	/**
	 * Creates a default RestAdapter.Builder instance, which simply sets an
	 * endpoint and client instance. Call this method to get a baseline which
	 * you can then customize and feed back to {@link #setAdapterBuilder}.
	 * @return a default/canonical RestAdapter.Builder instance
	 */
	public Retrofit.Builder getDefaultAdapterBuilder()
	{
		return new Retrofit.Builder()
			//.setLogLevel( Retrofit.LogLevel.FULL )
			.baseUrl( m_sEndpointURL )
			.client( this.getRestClient() )
			.addConverterFactory( JacksonConverterFactory.create(
				new ObjectMapper()
					//do not care if data from server does not exactly match response class
					.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false)
					//obey public/protected/private
					.configure(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS, false)
					//only map fields, not get* methods
					.configure(MapperFeature.AUTO_DETECT_GETTERS, false)
					//only map fields, not is* methods as getters
					.configure(MapperFeature.AUTO_DETECT_IS_GETTERS, false)
					//only map fields, not set* methods
					.configure(MapperFeature.AUTO_DETECT_SETTERS, false)
					//only visible and explicitly annotated accessors are ever used
					.configure(MapperFeature.INFER_PROPERTY_MUTATORS, false)
			))
			;
	}

	public BasicRetrofitClient<I> setAdapterBuilder(Retrofit.Builder builder)
	{ m_builder = builder ; m_bIsImplReady = false ; return this ; }

	/**
	 * Regenerates the implementation instance for the REST API spec bound to
	 * this client.
	 * @return the client itself, for chained invocations
	 */
	public BasicRetrofitClient<I> regenerate()
	{
		if( this.canRegenerate() )
		{
			m_adapter = this.getAdapterBuilder().build() ;
			m_impl = m_adapter.create( m_clsAPI ) ;
			m_bIsImplReady = true ;
		}
		return this ;
	}

	/**
	 * Indicates whether all the preconditions have been met so that we can
	 * actually regenerate the RetroFit API implementation instance.
	 * @return true if {@link #regenerate} can be called successfully.
	 */
	protected boolean canRegenerate()
	{
		return ( m_clsAPI != null && this.hasEndpointURL() );
	}

	/** Accessor for the interface that defines the REST API. */
	public Class<I> getAPIClass()
	{ return m_clsAPI ; }

	/**
	 * Returns an implementation adapter for the client's REST API. If the
	 * instance has not yet been created, then this method will try to create it
	 * with {@link #regenerate} if able. If no implementation instance can be
	 * created, then {@code null} is returned.
	 * @return an implementation adapter for the client's REST API
	 */
	public Retrofit getImplementationAdapter()
	{
		if( this.hasCurrentImplementation() )
			return m_adapter ;
		else if( this.canRegenerate() )
			return this.regenerate().m_adapter ;
		else return null ;
	}

	/**
	 * Returns an implementation instance for the client's REST API. If the
	 * instance has not yet been created, then this method will try to create it
	 * with {@link #regenerate} if able. If no implementation instance can be
	 * created, then {@code null} is returned.
	 * @return an implementation instance for the client's REST API
	 */
	public I getImplementation()
	{
		if( this.hasCurrentImplementation() )
			return m_impl ;
		else if( this.canRegenerate() )
			return this.regenerate().m_impl ;
		else return null ;
	}

	public boolean hasCurrentImplementation()
	{ return( m_bIsImplReady && m_impl != null ) ; }

	/**
	 * Returns the last REST API implementation that was generated by this
	 * instance. This might or might not be in sync with the current
	 * configuration of the instance if the endpoint or any other
	 * characteristics have changed.
	 * @return the last implementation instance generated by the client
	 */
	@SuppressWarnings("unused")
	public I getLastImplementation()
	{ return m_impl ; }

	/**
	 * Indicates whether the instance has any API implementation bound to it.
	 * Makes no promises as to whether it is in sync with current parameters.
	 */
	@SuppressWarnings("unused")
	public boolean hasAnyImplementation()
	{ return( m_impl != null ) ; }

	/**
	 * Override this method if you want something special to happen when the
	 * URL from shared preferences is modified.
	 */
	protected void onConfigEndpointChanged()
	{
	}

	public void setLoggerLevel(HttpLoggingInterceptor.Level aLevel) {
		if (m_okhttpLogger!=null)
			m_okhttpLogger.setLevel(aLevel);
	}

	public HttpLoggingInterceptor.Level getLoggerLevel() {
		if (m_okhttpLogger!=null)
			return m_okhttpLogger.getLevel();
		else
			return HttpLoggingInterceptor.Level.NONE;
	}


}
