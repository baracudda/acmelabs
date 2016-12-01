package com.example.acme.kazoo.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.blackmoonit.androidbits.auth.BroadwayAuthAccount;
import com.blackmoonit.androidbits.concurrent.OnClickTask;
import com.example.acme.kazoo.R;
import com.example.acme.kazoo.account.manager.AccountAuthenticator;
import com.example.acme.kazoo.server.auth.AuthRestAPI;
import com.example.acme.kazoo.server.auth.AuthRestClient;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentAccountAuthRegister.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentAccountAuthRegister#newInstance} factory method to
 * create an instance of this fragment.
 */
@SuppressWarnings("MissingPermission")
public final class FragmentAccountAuthRegister extends Fragment {
	static private final String TAG = FragmentAccountAuthRegister.class.getSimpleName();

	static private final String ARG_USERNAME = AccountAuthenticator.EXTRA_ACCOUNT_NAME;
	static private final String ARG_EMAIL = AccountAuthenticator.EXTRA_EMAIL;
	static private final String ARG_PW = AccountAuthenticator.EXTRA_PW_INPUT;
	static private final String ARG_REG_CODE = "reg_code";

	//views we handle often
	private EditText mEditTextUsername;
	private EditText mEditTextEmail;
	private EditText mEditTextPw;
	private EditText mEditTextPw_check;
	private EditText mEditTextReg_code;
	/**
	 * Using the OnClickThreadTask avoids bogging down the UI and also prevents multi-taps
	 * firing the event multiple times, especially useful for functions like Delete or Register.
	 */
	private OnClickTask mSubmitHandler;

	//request object
	private AuthRestAPI.MobileAuthRegisterRequest mAuthRegisterRequest;

	private OnFragmentInteractionListener mListener;
	protected AuthRestClient mAuthRestClient;

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param aUsername - the user's login username.
	 * @param aEmail - the user's email account associated with the username.
	 * @param aPassword - the user's password input.
	 * @param aRegCode - the registration code to use.
	 * @return A new instance of fragment FragmentAccountAuthRegister.
	 */
	public static FragmentAccountAuthRegister newInstance(String aUsername, String aEmail,
			String aPassword, String aRegCode) {
		FragmentAccountAuthRegister theFragment = new FragmentAccountAuthRegister();
		Bundle args = new Bundle();
		args.putString(ARG_USERNAME, aUsername);
		args.putString(ARG_EMAIL, aEmail);
		args.putString(ARG_PW, aPassword);
		args.putString(ARG_REG_CODE, aRegCode);
		theFragment.setArguments(args);
		return theFragment;
	}

	public FragmentAccountAuthRegister() {
		// Required empty public constructor
	}

	@Override
	public void onAttach(Activity anAct) {
		super.onAttach(anAct);
		try {
			mListener = (OnFragmentInteractionListener) anAct;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("%1$s must implement %3$s.%2$s",
					anAct.toString(), OnFragmentInteractionListener.class.getSimpleName(),
					FragmentAccountAuthRegister.class.getSimpleName()));
		}
		mAuthRegisterRequest = new AuthRestAPI.MobileAuthRegisterRequest();

		mAuthRestClient = mListener.getAuthRestClient();

		mSubmitHandler = new OnClickTask(OnClickSubmitTask.class, this);

		mAuthRegisterRequest.kind = AccountAuthenticator.AUTHTOKEN_KIND_FULL_ACCESS;
		Intent theIntent = anAct.getIntent();
		if (theIntent.hasExtra(AccountAuthenticator.EXTRA_AUTHTOKEN_KIND)) {
			mAuthRegisterRequest.kind = theIntent.getStringExtra(AccountAuthenticator.EXTRA_AUTHTOKEN_KIND);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mAuthRegisterRequest.name = getArguments().getString(ARG_USERNAME);
			mAuthRegisterRequest.email = getArguments().getString(ARG_EMAIL);
			mAuthRegisterRequest.salt = getArguments().getString(ARG_PW);
			mAuthRegisterRequest.code = getArguments().getString(ARG_REG_CODE);
		}
		if (TextUtils.isEmpty(mAuthRegisterRequest.email)) {
			Account[] theAccounts = AccountManager.get(getActivity()).getAccountsByType("com.google");
			if (theAccounts.length>0) {
				mAuthRegisterRequest.email = theAccounts[0].name;
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup aContainer,
			Bundle aSavedInstanceState) {
		View theResult = inflater.inflate(R.layout.fragment_account_auth_register, aContainer, false);
		View v;

		mEditTextUsername = (EditText)theResult.findViewById(R.id.account_auth_register_edit_username);
		mEditTextUsername.setText(mAuthRegisterRequest.name);
		mEditTextEmail = (EditText)theResult.findViewById(R.id.account_auth_register_edit_email);
		mEditTextEmail.setText(mAuthRegisterRequest.email);
		mEditTextPw = (EditText)theResult.findViewById(R.id.account_auth_register_edit_pw);
		mEditTextPw.setText(mAuthRegisterRequest.salt);
		mEditTextPw_check = (EditText)theResult.findViewById(R.id.account_auth_register_edit_pw_check);
		mEditTextPw_check.setText("");
		mEditTextReg_code = (EditText)theResult.findViewById(R.id.account_auth_register_edit_reg_code);
		mEditTextReg_code.setText(mAuthRegisterRequest.code);

		v = theResult.findViewById(R.id.account_auth_register_btn_submit);
		v.setOnClickListener(mSubmitHandler);

		return theResult;
	}

	@Override
	public void onDetach() {
		mAuthRestClient = null;
		mListener = null;
		super.onDetach();
	}

	/**
	 * Used to notify other fragments that a new user was registered (mainly to grab the username).
	 * @param aNewUser - a new user account was created, here's the non-sensitive info.
	 */
	public void onNewUserRegistered(Bundle aNewUser) {
		if (mListener != null) {
			mListener.onNewUserRegistered(aNewUser);
		}
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
		AuthRestClient getAuthRestClient();

		void onNewUserRegistered(Bundle aNewUser);
	}

	/**
	 * Submit button's onClick() task to perform.
	 */
	static public class OnClickSubmitTask extends OnClickTask.TaskDef<Boolean, Object,
			AuthRestAPI.MobileAuthRegisterResponse> {
		protected FragmentAccountAuthRegister myFrag;

        public OnClickSubmitTask() {
            // Required empty public constructor
        }

		@Override
		protected void setup(Object... aSetupParams) {
			myFrag = (FragmentAccountAuthRegister)aSetupParams[0];
		}

		@Override
		public Boolean beforeTask(View v) {
			View theViewFail = null;
			if (TextUtils.isEmpty(myFrag.mEditTextUsername.getText().toString()))
				theViewFail = myFrag.mEditTextUsername;
			if (TextUtils.isEmpty(myFrag.mEditTextEmail.getText().toString()))
				theViewFail =  myFrag.mEditTextEmail;
			if (TextUtils.isEmpty(myFrag.mEditTextPw.getText().toString()))
				theViewFail =  myFrag.mEditTextPw;
			if (TextUtils.isEmpty(myFrag.mEditTextReg_code.getText().toString()))
				theViewFail =  myFrag.mEditTextReg_code;
			if (myFrag.mEditTextPw_check.isShown()) {
				if (!TextUtils.equals(myFrag.mEditTextPw.getText().toString(),
						myFrag.mEditTextPw_check.getText().toString()))
					theViewFail =  myFrag.mEditTextPw_check;
			}

			if (theViewFail!=null) {
				//errors in user input, say msg and set focus
				if (theViewFail!=myFrag.mEditTextPw_check) {
					Toast.makeText(myFrag.getActivity(), R.string.account_auth_register_msg_pw_check_fail, Toast.LENGTH_SHORT).show();
					theViewFail.requestFocus();
				} else {
					Toast.makeText(myFrag.getActivity(), R.string.msg_input_required, Toast.LENGTH_SHORT).show();
					theViewFail.requestFocus();
				}
			}

			return (theViewFail==null); //if no problems, return TRUE
		}

		@Override
		public AuthRestAPI.MobileAuthRegisterResponse doTask(View v, Boolean aBeforeTaskResult) {
			if (aBeforeTaskResult && myFrag.mAuthRestClient!=null) {
				myFrag.mAuthRegisterRequest.name = myFrag.mEditTextUsername.getText().toString();
				myFrag.mAuthRegisterRequest.email = myFrag.mEditTextEmail.getText().toString();
				myFrag.mAuthRegisterRequest.salt = myFrag.mEditTextPw.getText().toString();
				myFrag.mAuthRegisterRequest.code = myFrag.mEditTextReg_code.getText().toString();
				myFrag.mAuthRegisterRequest.auth_header_data =
						BroadwayAuthAccount.composeAuthorizationHeaderValue(
								myFrag.mAuthRestClient.composeBroadwayAuthData(null)
						);
				return myFrag.mAuthRestClient.registerViaMobile(myFrag.mAuthRegisterRequest);
			} else {
				return null;
			}
		}

		@Override
		public void onTaskProgressUpdate(Object o) {
			//not needed
		}

		@Override
		public void afterTask(View v, AuthRestAPI.MobileAuthRegisterResponse aTaskResult) {
			if (aTaskResult!=null && aTaskResult.code!=null) {
				if (aTaskResult.code == AuthRestAPI.MobileAuthRegisterResponse.REGISTRATION_SUCCESS) {
					Bundle theNewUser = new Bundle();
					theNewUser.putString(AccountAuthenticator.EXTRA_ACCOUNT_NAME,
							myFrag.mAuthRegisterRequest.name);
					theNewUser.putString(AccountAuthenticator.EXTRA_USER_TOKEN,
							aTaskResult.user_token);
					theNewUser.putString(AccountAuthenticator.EXTRA_EMAIL,
							myFrag.mAuthRegisterRequest.email);
					theNewUser.putString(AccountAuthenticator.EXTRA_AUTH_ID,
							aTaskResult.auth_id);
					Log.i(TAG, "registered! "+aTaskResult.toString());
					myFrag.onNewUserRegistered(theNewUser);
				} else {
					Log.i(TAG, "!registered "+aTaskResult.toString());
					switch (aTaskResult.code) {
						case AuthRestAPI.MobileAuthRegisterResponse.REGISTRATION_NAME_TAKEN:
							Toast.makeText(myFrag.getActivity(), R.string.account_auth_register_msg_registration_name_fail, Toast.LENGTH_LONG).show();
							myFrag.mEditTextUsername.requestFocus();
							break;
						case AuthRestAPI.MobileAuthRegisterResponse.REGISTRATION_EMAIL_TAKEN:
							Toast.makeText(myFrag.getActivity(), R.string.account_auth_register_msg_registration_email_fail, Toast.LENGTH_LONG).show();
							myFrag.mEditTextEmail.requestFocus();
							break;
						case AuthRestAPI.MobileAuthRegisterResponse.REGISTRATION_REG_CODE_FAIL:
							Toast.makeText(myFrag.getActivity(), R.string.account_auth_register_msg_registration_code_fail, Toast.LENGTH_LONG).show();
							myFrag.mEditTextReg_code.requestFocus();
							break;
						case AuthRestAPI.MobileAuthRegisterResponse.REGISTRATION_UNKNOWN_ERROR:
							Toast.makeText(myFrag.getActivity(), R.string.msg_action_failed, Toast.LENGTH_LONG).show();
							myFrag.mEditTextReg_code.requestFocus();
							break;
					}//switch
				}
			} else {
				Log.i(TAG, "register returned NULL");
			}
		}
	}

}
