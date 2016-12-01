package com.example.acme.kazoo.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.utils.BitsAppUtils;
import com.blackmoonit.androidbits.utils.BitsStringUtils;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.service.KazooService;
import com.example.acme.kazoo.service.ServiceActionProcessor;

/**
 * Main activity for the app.
 */
public class MainActivity extends Activity
implements ServiceActionProcessor.LoginListener
{
    static private final String TAG = MainActivity.class.getSimpleName() ;

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
            mService.getProcessor().addListener( MainActivity.this ) ;
            MainActivity.this.updateServerURI( mService.getProcessor().getServerURI() ) ;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    /**
     * The layout that contains the title card and button panel. This is a
     * linear layout whose orientation changes with that of the device. It is
     * <i>not</i> the container for the entire screen; there are other
     * components whose position on the overall screen is not
     * orientation-dependent.
     */
    protected LinearLayout m_layCardAndButtons = null ;

    /**
     * A persistent reference to the app title card.
     */
    protected TextView m_twTitleCard = null ;

    /**
     * A persistent reference to the view that displays our server URI.
     */
    protected TextView m_twServerURI = null ;

    /**
     * A persistent reference to the login/logout button.
     * This will have its text altered dynamically in reaction to the app's
     * authentication state.
     */
    protected Button m_btnLoginLogout = null ;

    /**
     * A persistent reference to the login/logout menu option.
     * This will have its text altered dynamically in reaction to the app's
     * authentication state.
     */
    protected MenuItem m_mniLoginLogout = null ;

    /**
     * Stores the last account name we received as a notification from the
     * service. If null, then we're probably not authenticated. If non-null,
     * then this is the name of the account that was probably last
     * authenticated.
     */
    protected String m_sLastLoginName = null ;

//// Android Activity Life Cycle ///////////////////////////////////////////////

    @Override
    protected void onCreate( Bundle bndlState )
    {
        super.onCreate( bndlState ) ;
        Log.d(TAG, "Creating activity." ) ;
        this.setContentView( R.layout.activity_main ) ;
        m_layCardAndButtons = ((LinearLayout)
                ( this.findViewById( R.id.layKazooMainCardAndButtons ) )) ;
        m_twTitleCard = ((TextView)
                ( this.findViewById( R.id.twKazooTitleCard ) )) ;
        this.initTitleCard() ;
        m_twServerURI = ((TextView)
                ( this.findViewById( R.id.twKazooMainServerURIDisplay ) ));
        m_btnLoginLogout = ((Button)
                ( this.findViewById( R.id.btnKazooMainLogin ) )) ;
        KazooService.IntentFactory.kickoff(this) ;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to KazooService
        Intent intent = new Intent(this, KazooService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

    @Override
    protected void onResume()
    {
        super.onResume() ;
        this.setOrientation();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        this.getMenuInflater().inflate( R.menu.menu_main, menu ) ;
        return true ;
    }

    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        m_mniLoginLogout = menu.findItem( R.id.menuitem_kazoo_login_logout ) ;
        this.updateLoginControls() ;
        return super.onPrepareOptionsMenu(menu) ;
    }

//// ServiceActionProcessor.LoginListener //////////////////////////////////////////////

    public void onServerURIChanged( String sURI )
    { this.updateServerURI( sURI ) ; }

    public void onServerAuthChanged( BroadwayAuthAccount acct )
    {
        if( acct != null && ! BitsStringUtils.isEmpty( acct.getAcctName() ) )
            m_sLastLoginName = acct.getAcctName() ;
        else
            m_sLastLoginName = null ;
        this.updateLoginControls() ;
    }

//// Activity //////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onOptionsItemSelected( MenuItem item )
    {
        final int nItem = item.getItemId() ;

        switch( nItem )
        {
            case R.id.menuitem_kazoo_login_logout:
                this.handleLoginLogoutCommand(null) ; return true ;
            case R.id.menuitem_kazoo_settings:
                this.navToSettingsScreen(null) ; return true ;
        }

        return super.onOptionsItemSelected(item) ;
    }

//// Other Instance Methods ////////////////////////////////////////////////////

    /**
     * Initializes the text of the main screen's title card.
     * @return Returns <i>this</i> for chaining.
     */
    protected MainActivity initTitleCard()
    {
        final String sTitleCardText =
            this.getString( R.string.label_main_title_card,
                BitsAppUtils.getAppPackageInfo( this, 0 ).versionName ) ;

        this.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                MainActivity.this.m_twTitleCard.setText( sTitleCardText ) ;
            }
        });

        return this ;
    }

    /**
     * Navigates to the login screen.
     * @param w the view that triggered the navigation event (ignored)
     */
    public void handleLoginLogoutCommand( View w )
    {
        final ServiceActionProcessor proc = ( mConnection != null && mBound ?
            mService.getProcessor() : null ) ;
        if( m_sLastLoginName != null )
        { // Log out.
            new AlertDialog.Builder( this )
                .setTitle( R.string.dialog_logout_title )
                .setMessage( this.getString(
                        R.string.dialog_logout_text, m_sLastLoginName ) )
                .setNegativeButton( android.R.string.cancel, null )
                .setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick( DialogInterface dia, int w )
                        {
                            if( proc != null ) proc.logout() ;
                            m_sLastLoginName = null ;
                        }
                    })
                .create()
                .show()
                ;
        }
        else
        { // Log in.
            if( proc != null ) proc.authenticate() ;
            // We'll reset m_sLastLoginName in reaction to the login event.
        }
    }

    /**
     * Navigates to the preference screen.
     * @param w the view that triggered the navigation event (ignored)
     */
    public void navToSettingsScreen( View w )
    {
        Intent sig = new Intent( this, SettingsActivity.class ) ;
        this.startActivity( sig ) ;
    }

    /**
     * Sets the orientation of the overall screen layout in reaction to the
     * device's orientation.
     * @return Returns <i>this</i> for chaining.
     */
    @SuppressLint( "SwitchIntDef" ) // Android Studio bug; all cases are handled.
    protected MainActivity setOrientation()
    {
        final int nRotation =
            this.getWindowManager().getDefaultDisplay().getRotation() ;
        switch( nRotation )
        {
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                m_layCardAndButtons.setOrientation( LinearLayout.HORIZONTAL ) ;
                break;
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
            default:
                m_layCardAndButtons.setOrientation( LinearLayout.VERTICAL ) ;
                break;
        }
        return this ;
    }

    /**
     * Updates the text view that displays our web service URI.
     * @param sURI the URI to display
     * @return Returns <i>this</i> for chaining.
     */
    protected MainActivity updateServerURI(final String sURI )
    {
        if( sURI == null ) return this.acquireServerURI() ;          // recurses
        this.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            { MainActivity.this.m_twServerURI.setText(sURI) ; }
        });
        return this ;
    }

    /**
     * Tries to determine the server URI based on the {@link ServiceActionProcessor}'s
     * client URI, or the URI configured in the app's preferences, then recurses
     * back to {@link #updateServerURI}.
     * @return Returns <i>this</i> for chaining.
     */
    protected MainActivity acquireServerURI()
    {
        String sURI = null ;
        if( mConnection != null && mBound )
        { // Try to get a URI from the processor.
            final KazooService srv = mService ;
            if( srv != null )
            {
                final ServiceActionProcessor proc = srv.getProcessor() ;
                sURI = proc.getServerURI() ;
                if( sURI == null )
                { // Fall back to app prefs, if any.
                    final String sPrefsURI = proc.getPrefs().getString(
                            this.getString( R.string.pref_key_server_url ),
                            null
                        );
                    if( ! BitsStringUtils.isEmpty( sPrefsURI ) )
                    { // Use the preference URI, but label it specially.
                        sURI = this.getString(
                                R.string.label_server_uri_not_bound,
                                sPrefsURI
                            );
                    }
                }
            }
        }

        if( sURI == null )
            sURI = this.getString( R.string.label_server_uri_unknown ) ;
        return this.updateServerURI( sURI ) ;
    }

    /**
     * Updates the label on the login/logout button and menu item.
     * @return Returns <i>this</i> for chaining.
     */
    protected MainActivity updateLoginControls()
    {
        final String sButtonText = ( m_sLastLoginName == null ?
                this.getString( R.string.action_login ) :
                this.getString( R.string.label_button_logout_of_account,
                    m_sLastLoginName )
            );
        final String sMenuItemText = ( m_sLastLoginName == null ?
                this.getString( R.string.action_login ) :
                this.getString( R.string.action_logout )
            );

        final MainActivity act = this ;    // shorthand for anon class

        this.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                act.m_btnLoginLogout.setText(sButtonText) ;
                if( act.m_mniLoginLogout != null )
                    act.m_mniLoginLogout.setTitle(sMenuItemText) ;
            }
        });

        return this ;
    }
}
