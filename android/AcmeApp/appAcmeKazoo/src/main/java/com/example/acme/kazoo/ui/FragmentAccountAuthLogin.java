package com.example.acme.kazoo.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.blackmoonit.androidbits.app.ActivityResult;
import com.blackmoonit.androidbits.concurrent.OnClickTask;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.account.manager.AccountAuthenticator;
import com.example.acme.kazoo.server.auth.AuthRestAPI;
import com.example.acme.kazoo.server.auth.AuthRestClient;

import java.lang.ref.WeakReference;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentAccountAuthLogin.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentAccountAuthLogin#newInstance} factory method to
 * create an instance of this fragment.
 */
@SuppressWarnings("MissingPermission")
public final class FragmentAccountAuthLogin extends Fragment {
	static private final String TAG = FragmentAccountAuthLogin.class.getSimpleName();

	/*************************************************************
	 * Fragment related fields and constants
	 *************************************************************/
	static private final String ARG_USER_OR_EMAIL = AccountAuthenticator.EXTRA_USER_INPUT;
	static private final String ARG_PW = AccountAuthenticator.EXTRA_PW_INPUT;

	//views we handle often
	private EditText mEditTextUserInput;
	private EditText mEditTextPwInput;
	/**
	 * Using the OnClickThreadTask avoids bogging down the UI and also prevents multi-taps
	 * firing the event multiple times, especially useful for functions like Delete or Register.
	 */
	private OnClickTask mSubmitHandler;

	//request object
	private AuthRestAPI.MobileAuthTokenRequestByLogin mAuthTokenRequest;

	private OnFragmentInteractionListener mListener;

	/*************************************************************
	 * Class methods
	 *************************************************************/

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param aUserOrEmail - the user's login username or email address.
	 * @param aPassword - the user's password input.
	 * @return A new instance of fragment FragmentAccountAuthLogin.
	 */
	static public FragmentAccountAuthLogin newInstance(String aUserOrEmail,
			String aPassword) {
		FragmentAccountAuthLogin theFragment = new FragmentAccountAuthLogin();
		Bundle args = new Bundle();
		args.putString(ARG_USER_OR_EMAIL, aUserOrEmail);
		args.putString(ARG_PW, aPassword);
		theFragment.setArguments(args);
		return theFragment;
	}

	public FragmentAccountAuthLogin() {
		// Required empty public constructor
	}

	@Override
	public void onAttach(final Activity anAct) {
		super.onAttach(anAct);
		//onAttach happens before onCreate()
		try {
			mListener = (OnFragmentInteractionListener) anAct;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("%1$s must implement %3$s.%2$s",
					anAct.toString(), OnFragmentInteractionListener.class.getSimpleName(),
					FragmentAccountAuthLogin.class.getSimpleName() ));
		}
		mAuthTokenRequest = new AuthRestAPI.MobileAuthTokenRequestByLogin();
		mSubmitHandler = new OnClickTask(OnClickSubmitTask.class, this);

		final Intent theIntent = anAct.getIntent();
		if (theIntent.hasExtra(AccountManager.KEY_ACCOUNT_NAME)) {
			mAuthTokenRequest.ticketholder = theIntent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//onCreate happens after onAttach()
		if (getArguments() != null) {
			mAuthTokenRequest.ticketholder = getArguments().getString(ARG_USER_OR_EMAIL);
			mAuthTokenRequest.pwinput = getArguments().getString(ARG_PW);
		}
	}

	static protected class MyResultHandler extends ActivityResult.Handler {
		private final WeakReference<FragmentAccountAuthLogin> myFrag;

		public MyResultHandler( FragmentAccountAuthLogin aFrag ) {
			super();
			myFrag = new WeakReference<>(aFrag);
		}

		@Override
		public void handleResultOk(Intent aData) {
			if (myFrag.get()==null)
				return;
			if (aData.hasExtra(AccountAuthenticator.EXTRA_ACCOUNT_NAME))
				myFrag.get().mEditTextUserInput.setText(aData.getStringExtra(
						AccountAuthenticator.EXTRA_ACCOUNT_NAME));
			myFrag.get().followUpAuthRequest(aData);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup aContainer,
			Bundle aSavedInstanceState) {
		//onCreateView happens after onCreate()
		View theResult = inflater.inflate(R.layout.fragment_account_auth_login, aContainer, false);
		View v;

		mEditTextUserInput = (EditText)theResult.findViewById(R.id.account_auth_login_edit_username);
		mEditTextUserInput.setText(mAuthTokenRequest.ticketholder);
		mEditTextPwInput = (EditText)theResult.findViewById(R.id.account_auth_login_edit_password);
		mEditTextPwInput.setText(mAuthTokenRequest.pwinput);
		if (!TextUtils.isEmpty(mAuthTokenRequest.ticketholder))
			mEditTextPwInput.requestFocus();

		v = theResult.findViewById(R.id.account_auth_login_btn_submit);
		v.setOnClickListener(mSubmitHandler);

		v = theResult.findViewById(R.id.account_auth_login_btn_register);
		if (v!=null) {
			((Button)v).setText(getString(R.string.account_auth_login_label_register, getString(R.string.app_name)));

			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					//Log.i(TAG, "tapped auth reg");
					if (mListener!=null) {
						// Since there can only be one AuthenticatorActivity,
						// we call the register activity, get that activity's results,
						// and return them in setAccountAuthenticatorResult(). See finishLogin().
						Intent theRegisterIntent = new Intent(getActivity(), ActivityAccountAuthRegister.class);

                        if (getActivity().getIntent().getExtras() != null) {
                            theRegisterIntent.putExtras(getActivity().getIntent().getExtras());
                        }

						ActivityResult.Manager theActResultMgr = mListener.getActivityResultManager();
						int theRequestCode = theActResultMgr.registerResultHandler(
								new MyResultHandler(FragmentAccountAuthLogin.this)
						);
						theActResultMgr.startActivityForResult(theRegisterIntent, theRequestCode);
					}
				}
			});
		}

		return theResult;
	}

	@Override
	public void onDetach() {
		mListener = null;
		super.onDetach();
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {

		/**
		 * We have tasks in the fragment that require results passed back from the
		 * onActivityResults callback, force the Activity containing this fragment to
		 * use the ActivityResult.Manager class so our fragment can get its results.
		 * @return Return the ActivityResults.Manager class in use by the Activity.
		 */
		ActivityResult.Manager getActivityResultManager();

		AuthRestClient getAuthRestClient();

		AccountManager getAccountManager();

		/**
		 * The result of all our shenanigans needs to be bundled up and sent to the
		 * Authenticator via the Activity's special method.
		 * @param result - the Authentication results
		 */
		void setAccountAuthenticatorResult(Bundle result);

		void afterSetAccountAuthenticatorResult(Bundle aResult);

		Context getAlertDialogContext();

	}

	/**
	 * We perform the actual work to get the auth token in a background thread
	 */
	static public class OnClickSubmitTask extends OnClickTask.TaskDef<Boolean, Object,
			AuthRestAPI.MobileAuthTokenResponse> {
		protected FragmentAccountAuthLogin myFrag;
		protected AlertDialog mPopupDialog;

        public OnClickSubmitTask() {
            // Required empty public constructor
        }

        @Override
		protected void setup(Object... aSetupParams) {
			myFrag = (FragmentAccountAuthLogin)aSetupParams[0];
		}

		@Override
		public Boolean beforeTask(View v) {
			View theViewFail = null;
			if (TextUtils.isEmpty(myFrag.mEditTextUserInput.getText().toString()))
				theViewFail = myFrag.mEditTextUserInput;
			if (TextUtils.isEmpty(myFrag.mEditTextPwInput.getText().toString()))
				theViewFail =  myFrag.mEditTextPwInput;

			if (theViewFail!=null) {
				//errors in user input, say msg and set focus
				Toast.makeText(myFrag.getActivity(), R.string.msg_input_required, Toast.LENGTH_SHORT).show();
				theViewFail.requestFocus();
			} else {
				AlertDialog.Builder theDlgBuilder = new AlertDialog.Builder(
						myFrag.mListener.getAlertDialogContext());
				theDlgBuilder.setView(R.layout.popup_authenticating);
				theDlgBuilder.setTitle(R.string.app_name);
				mPopupDialog = theDlgBuilder.create();
				mPopupDialog.show();
			}

			return (theViewFail==null); //if no problems, return TRUE
		}

		@Override
		public AuthRestAPI.MobileAuthTokenResponse doTask(View v, Boolean aBeforeTaskResult) {
			if (aBeforeTaskResult && myFrag!=null && myFrag.mListener!=null &&
				myFrag.mListener.getAuthRestClient()!=null) {
				myFrag.mAuthTokenRequest.ticketholder = myFrag.mEditTextUserInput.getText().toString();
				myFrag.mAuthTokenRequest.pwinput = myFrag.mEditTextPwInput.getText().toString();
				return myFrag.mListener.getAuthRestClient().requestMobileAuthByLogin(myFrag.mAuthTokenRequest);
			} else {
				return null;
			}
		}

		@Override
		public void onTaskProgressUpdate(Object o) {
			//our progress dialog is indeterminate
		}

		@Override
		public void afterTask(View v, AuthRestAPI.MobileAuthTokenResponse aTaskResult) {
			if (mPopupDialog!=null && mPopupDialog.isShowing()) {
				mPopupDialog.dismiss();
			}
			if (aTaskResult!=null && !TextUtils.isEmpty(aTaskResult.account_name) &&
					!TextUtils.isEmpty(aTaskResult.user_token) &&
					!TextUtils.isEmpty(aTaskResult.auth_token)) {
				Bundle theAuthData = new Bundle();
				theAuthData.putString(AccountAuthenticator.EXTRA_ACCOUNT_NAME,
						aTaskResult.account_name);
				theAuthData.putString(AccountAuthenticator.EXTRA_AUTH_ID,
						aTaskResult.auth_id);
				theAuthData.putString(AccountAuthenticator.EXTRA_USER_TOKEN,
						aTaskResult.user_token);
				theAuthData.putString(AccountAuthenticator.EXTRA_AUTH_TOKEN,
						aTaskResult.auth_token);
				final Intent theResult = new Intent();
				theResult.putExtras(theAuthData);
				myFrag.followUpAuthRequest(theResult);
			} else {
				if (myFrag!=null && myFrag.getActivity()!=null) {
					Toast.makeText(myFrag.getActivity(), myFrag.getString(R.string.account_auth_login_fail),
							Toast.LENGTH_LONG).show();
				}
				if (myFrag!=null && myFrag.mEditTextPwInput!=null) {
					myFrag.mEditTextPwInput.requestFocus();
				}
			}
		}
	}

	/**
	 * Once login/register finish, follow up with this method call.
	 * @param aIntent - login/register results
	 */
	protected void followUpAuthRequest(Intent aIntent) {
		if (getActivity()==null) {
			Log.e(TAG, "followUpAuthRequest called after detached from Activity, abort set user account");
			return;
		}
		Log.d(TAG, "followUpAuthRequest");
		//basic account data
		String theAcctName = aIntent.getStringExtra(AccountAuthenticator.EXTRA_ACCOUNT_NAME);
		String theUserToken = aIntent.getStringExtra(AccountAuthenticator.EXTRA_USER_TOKEN);
		String theAcctType = getString(AccountAuthenticator.RES_ACCOUNT_TYPE);
		if (aIntent.hasExtra(AccountAuthenticator.EXTRA_ACCOUNT_TYPE))
			theAcctType = aIntent.getStringExtra(AccountAuthenticator.EXTRA_ACCOUNT_TYPE);
        else
            aIntent.putExtra(AccountAuthenticator.EXTRA_ACCOUNT_TYPE,theAcctType);
		//extra account data we need
		String theAuthId = aIntent.getStringExtra(AccountAuthenticator.EXTRA_AUTH_ID);
		Bundle theUserData = new Bundle();
		theUserData.putString(AccountAuthenticator.EXTRA_AUTH_ID, theAuthId);
		aIntent.putExtra(AccountManager.KEY_USERDATA, theUserData);

		AccountManager theAccountMgr = mListener.getAccountManager();
		Account[] theAccounts = theAccountMgr.getAccountsByType(theAcctType);
		Account theUserAccount = null;
		if( theAccounts != null )
        {
			for( Account a : theAccounts )
            {
				if( a.name.equals(theAcctName) )
                {
					theUserAccount = a ;
					break;
				}
			}
		}
		/*
		if (theUserAccount!=null &&
				aIntent.getBooleanExtra(AccountAuthenticatorForFresnel.EXTRA_IS_ADDING_NEW_ACCOUNT, false)) {
			AccountManagerFuture<Boolean> theRemoveFuture = theAccountMgr.removeAccount(theUserAccount,null,null);
			theUserAccount = null;
			try {
				theRemoveFuture.getResult();
			} catch (Exception e) {
				//don't care about result, only that it finished
			}
		}
		*/
		if (theUserAccount==null) {
			theUserAccount = new Account(theAcctName, theAcctType);
			if (!theAccountMgr.addAccountExplicitly(theUserAccount, theUserToken, theUserData)) {
				theUserAccount = null;
			}
		} else {
			theAccountMgr.setPassword(theUserAccount, theUserToken);
			theAccountMgr.setUserData(theUserAccount, AccountAuthenticator.EXTRA_AUTH_ID, theAuthId);
		}

		if (theUserAccount!=null) {
			if (aIntent.hasExtra(AccountAuthenticator.EXTRA_AUTH_TOKEN)) {
				theAccountMgr.setAuthToken(theUserAccount, AccountAuthenticator.AUTHTOKEN_KIND_FULL_ACCESS,
						aIntent.getStringExtra(AccountAuthenticator.EXTRA_AUTH_TOKEN));
			}
		}

		if( mListener != null )
        {
			mListener.setAccountAuthenticatorResult(aIntent.getExtras());
			mListener.afterSetAccountAuthenticatorResult(aIntent.getExtras());
		}
        else
        {
            Log.wtf( TAG, "Authentication fragment had no listeners!" ) ;
        }
	}

}
