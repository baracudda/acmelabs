package com.example.acme.kazoo.server.retrofit;

import android.content.Context;

import com.blackmoonit.androidbits.auth.IBroadwayAuthDeviceInfo;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Retrofit REST client that supports sending IBroadwayAuthDeviceInfo.
 */
public class DeviceInfoRetrofitClient<I>
extends BasicRetrofitClient<I>
implements IBroadwayAuthDeviceInfo
{
	/** A container of Android device information. */
	protected IBroadwayAuthDeviceInfo m_devinfo = null ;

	// Constructors...

	public DeviceInfoRetrofitClient( Class<I> clsAPI, Context ctx,
			IBroadwayAuthDeviceInfo devinfo )
	{
		super( clsAPI, ctx ) ;
		this.setDeviceInfo(devinfo) ;
	}

	// Overrides of BasicRetrofitClient<I>...

	@Override // trivial override for precise chaining
	public synchronized DeviceInfoRetrofitClient<I> registerEndpointListener(
		RestEndpointListener o )
	{ super.registerEndpointListener( o ) ; return this ; }

	@Override // trivial override for precise chaining
	public synchronized DeviceInfoRetrofitClient<I> unregisterEndpointListener(
		RestEndpointListener o )
	{ super.unregisterEndpointListener( o ) ; return this ; }

	@Override // trivial override for precise chaining
	public synchronized DeviceInfoRetrofitClient<I> setEndpoint( String sURL)
	{ super.setEndpoint( sURL ) ; return this ; }

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
			.addInterceptor( new RequestInterceptorDeviceInfo( m_devinfo ) )
			;
	}

	@Override // trivial override for precise chaining
	public DeviceInfoRetrofitClient<I> setAdapterBuilder( Retrofit.Builder b )
	{ super.setAdapterBuilder(b) ; return this ; }

	@Override // trivial override for precise chaining
	public DeviceInfoRetrofitClient<I> regenerate()
	{ super.regenerate() ; return this ; }

	@SuppressWarnings("RedundantIfStatement")
	@Override
	public boolean canRegenerate()
	{
		if( ! super.canRegenerate() ) return false ;
		// Now check for anything that's specific to this client type.
		// Namely, can we build our default request interceptor?
		if( m_devinfo == null ) return false ;
		return true ;
	}

	// Implementations for IBroadwayAuthDeviceInfo...

	@Override
	public String[] getDeviceFingerprints()
	{
		if( m_devinfo != null )
			return m_devinfo.getDeviceFingerprints() ;
		else return null ;
	}

	@Override
	public String[] getDeviceCircumstances()
	{
		if( m_devinfo != null )
			return m_devinfo.getDeviceCircumstances() ;
		else return null ;
	}

	@Override
	public StringBuilder composeBroadwayAuthData(StringBuilder aStrBldr)
	{
		if( m_devinfo != null )
			return m_devinfo.composeBroadwayAuthData( aStrBldr ) ;
		else return null ;
	}

	// Extensions...

	/** Accesses the client's device info. */
	@SuppressWarnings("unused")
	public IBroadwayAuthDeviceInfo getDeviceInfo()
	{ return m_devinfo ; }

	/**
	 * Modifies the client's device info.
	 * @param devinfo device information
	 * @return the client, for chained invocations
	 */
	public DeviceInfoRetrofitClient<I> setDeviceInfo( IBroadwayAuthDeviceInfo devinfo )
	{
		m_devinfo = devinfo ;
		m_bIsImplReady = false ;
		return this ;
	}

}
