package net.axley.smartlocktest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;

import java.io.IOException;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, AsyncResponse {

        private static final int RC_SIGN_IN = 9001;
        public static final String LOGTAG = "SmartLockTest";
        GoogleApiClient mGoogleApiClient;
        TextView textView;
        Button signOn;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                setContentView(R.layout.activity_main);

                textView = (TextView) findViewById(R.id.textView);
                signOn = (Button) findViewById(R.id.signOn);
                findViewById(R.id.signOn).setOnClickListener(this);


                // We want to obtain an OAuth token for this user that we can possibly use to issue other requests on their behalf

                // Configure sign-in to request the user's ID, email address, and basic profile. ID and
                // basic profile are included in DEFAULT_SIGN_IN.
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build();

                // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                        .enableAutoManage(this, this)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .addOnConnectionFailedListener(this)
                        .build();
        }

        @Override
        protected void onStart() {
                super.onStart();

                OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

                if (opr.isDone()) {
                        // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
                        // and the GoogleSignInResult will be available instantly.
                        Log.d(LOGTAG, "Got cached sign-in");
                        GoogleSignInResult result = opr.get();
                        handleSignInResult(result);
                } else {
                        // If the user has not previously signed in on this device or the sign-in has expired,
                        // this asynchronous branch will attempt to sign in the user silently.  Cross-device
                        // single sign-on will occur in this branch.
                        opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                                @Override
                                public void onResult(GoogleSignInResult googleSignInResult) {
                                        handleSignInResult(googleSignInResult);
                                }
                        });
                }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);

                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
                if (requestCode == RC_SIGN_IN) {
                        GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                        handleSignInResult(result);
                }
        }

        private void signIn() {
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
        }

        private void handleSignInResult(GoogleSignInResult result) {
                Log.d(LOGTAG, "handleSignInResult:" + result.isSuccess());
                if (result.isSuccess()) {
                        // Signed in successfully, show authenticated UI.
                        GoogleSignInAccount acct = result.getSignInAccount();
                        textView.setText(getString(R.string.signed_in_fmt, acct.getDisplayName()));
                    //getOauthToken(acct.getEmail());
                    getOauthTokenFromAccountManager();
                } else {
                        // Signed out, show unauthenticated UI.
                        textView.setText(R.string.guest);
                }
        }

        private void getOauthTokenFromAccountManager() {
            AccountManager accountManager = AccountManager.get(getApplicationContext());
            Account[] accts = accountManager.getAccountsByType("com.google");
            // Cheating for this example and using the first Google account found
            Account acct = accts[0];
            accountManager.getAuthToken(acct, "oauth2:https://www.googleapis.com/auth/login_manager", false, new GetAuthTokenCallback(), null);
        }

        private void getOauthToken(String email) {
            // Retrieve the oAuth 2.0 access token.
            final Context context = this.getApplicationContext();
            // CANNOT run this on the main thread as it is synchronous and will block
            String regularScope = "oauth2:" + Scopes.PROFILE + " " + Scopes.EMAIL + " openid";
            // This doesn't work using the normal API.  This is a special scope sent for android auth to an android endpoint
            //String smartLockScope = "oauth2:https://www.googleapis.com/auth/login_manager";
            String godScope = "oauth2:mail reader cl talk groups2 notebook analytics alerts ig toolbar youtube lh2 wise writely pages u2u local sprose news grandcentral ah android wave lso goanna_mobile sierra jotspot hist chromiumsync multilogin androidmarket oz webupdates doritos code omaha friendview";
            String androidScope = "oauth2:android";
            GetOAuthTokenTask task = new GetOAuthTokenTask(context, email, regularScope, this);
            task.execute();
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.d(LOGTAG, "Google Connection failed! " + connectionResult);
        }

        @Override
        public void onClick(View view) {
                switch (view.getId()) {
                        case R.id.signOn:
                                signIn();
                }
        }

    @Override
    public void processFinish(String result) {
        // we got the async result!
        Log.d(LOGTAG, "Got token: " + result);

    }

    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
            try {
                Bundle authTokenBundle = accountManagerFuture.getResult();
                String authToken = authTokenBundle.get(AccountManager.KEY_AUTHTOKEN).toString();
                Log.d(LOGTAG, "Got token: " + authToken);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            }
        }
    }

    private class GetOAuthTokenTask extends AsyncTask<Void, Void, String> {

        Context mContext;
        String mEmail;
        String mScope;

        AsyncResponse delegate = null;

        @Override
        protected void onPostExecute(String result) {
            delegate.processFinish(result);
        }


        GetOAuthTokenTask(Context context, String name, String scope, AsyncResponse delegate) {
            this.mContext = context;
            this.mEmail = name;
            this.mScope = scope;
            this.delegate = delegate;
        }

        protected String doInBackground(Void... params) {
            try {
                // We can retrieve the token to check via
                // tokeninfo or to pass to a service-side
                // application.
                String token = GoogleAuthUtil.getToken(mContext,
                        mEmail, mScope);
                delegate.processFinish(token);
            } catch (UserRecoverableAuthException e) {
                // This error is recoverable, so we could fix this
                // by displaying the intent to the user.
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GoogleAuthException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

interface AsyncResponse {
    void processFinish(String result);
}
