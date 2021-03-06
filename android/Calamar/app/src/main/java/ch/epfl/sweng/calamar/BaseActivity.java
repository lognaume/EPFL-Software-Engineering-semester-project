package ch.epfl.sweng.calamar;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import ch.epfl.sweng.calamar.item.CreateItemActivity;
import ch.epfl.sweng.calamar.map.GPSProvider;
import ch.epfl.sweng.calamar.push.RegistrationIntentService;

public abstract class BaseActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // LogCat tag
    private static final String TAG = BaseActivity.class.getSimpleName();

    // activity request codes
    protected static final int ACCOUNT_CHOOSEN = 3001;
    private static final int ERROR_RESOLUTION_REQUEST = 1001;

    private CalamarApplication app;

    // google api related stuff
    private boolean resolvingError;

    // *********************************************************************************************
    // ACTIVITY LIFECYCLE CALLBACKS
    // https://developer.android.com/training/basics/activity-lifecycle/starting.html
    // even better :
    // https://stackoverflow.com/questions/12203651/why-is-onresume-called-when-an-activity-starts
    //
    // every time screen is rotated, activity is destroyed/recreated :
    // https://stackoverflow.com/questions/7618703/activity-lifecycle-oncreate-called-on-every-re-orientation
    // maybe prevent this ...
    //
    //TODO check activity lifecycle and pertinent action to make when entering new states
    // regarding connection / disconnection of googleapiclient, start stop GPSProvider updates
    // etc...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = CalamarApplication.getInstance();
        if (!app.isGoogleApiClientCreated()) {
            buildGoogleApiClient();
        }  // will connect in onResume(), errors are handled in onConnectionFailed()
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.onActivityResumed(this);

        GoogleApiClient googleApiClient = app.getGoogleApiClient();
        if (!resolvingError &&
                !googleApiClient.isConnected() &&
                !googleApiClient.isConnecting()) {
            googleApiClient.connect();
            // if errors, such as no google play apk, onConnectionFailed will handle the errors
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        app.onActivityPaused(this);
    }

    // *********************************************************************************************

    // *********************************************************************************************
    // GOOGLE API CLIENT CALLBACKS METHODS
    @Override
    public void onConnected(Bundle arg0) {
        Log.i(TAG, getString(R.string.google_api_client_connected));
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        app.getGoogleApiClient().connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO potential corner case double dialog if google shows dialog when acount selection
        // maybe let google do the work, now that we have account selection
        if (resolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            resolvingError = true;
            Log.e(TAG, getString(R.string.google_api_client_failed_auto_resolution, connectionResult.getErrorCode()));

            try {
                connectionResult.startResolutionForResult(this, ERROR_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                app.getGoogleApiClient().connect();
            }
        } else {
            resolvingError = true;
            Log.e(TAG, getString(R.string.google_api_client_failed_no_resolution, connectionResult.getErrorCode()));

            // show error dialog
            showGoogleApiErrorDialog(connectionResult.getErrorCode());
        }
    }

    // *********************************************************************************************

    // TODO test all use don't crash

    /**
     * Displays an error message in a dialog
     *
     * @param message the message to be displayed
     */
    public void displayErrorMessage(String message, final boolean criticalError) {
        Log.e(BaseActivity.TAG, message);
        if (!this.isFinishing()) {//&& !isPaused()) {
            final AlertDialog.Builder errorDialog = new AlertDialog.Builder(this);
            errorDialog.setTitle(message);
            errorDialog.setPositiveButton(R.string.alert_dialog_default_positive_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //OK
                    if (criticalError) {
                        BaseActivity.this.finish();
                    }
                }
            });
//            if (criticalError) {
//                errorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                    @Override
//                    public void onDismiss(DialogInterface dialog) {
//                        BaseActivity.this.finish();
//                    }
//                });
//            }
            errorDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GPSProvider.CHECK_SETTINGS_REQUEST:
                GPSProvider gpsProvider = GPSProvider.getInstance();
                //Log.e(TAG, resultCode+"");
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, getString(R.string.location_settings_fixed));
                        // start only the updates, settings should have been fixed now
                        gpsProvider.startLocationUpdates();
                        break;
                    default: {// WARNING, on api level 16, if multiple action need to be done,
                        // multiple dialog displayed BUT (WTF ?) on 'ok' of 1st dialog
                        // resultCode = 0 instead of -1(RESULTOK) and all the problem solved...but error msg...
                        // don't see why...
                        gpsProvider.displayErrorMessage(this);
                    }
                }
                break;
            case ERROR_RESOLUTION_REQUEST:
                resolvingError = false;
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Make sure the app is not already connected or attempting to connect
                        GoogleApiClient googleApiClient = app.getGoogleApiClient();
                        if (!googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
                            googleApiClient.connect();
                        }
                        break;
                    default:
                        displayErrorMessage(getString(R.string.google_api_client_cant_connect), true);
                }
                break;
            case ACCOUNT_CHOOSEN:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // get account name
                        String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                        CalamarApplication.getInstance().setCurrentUserName(accountName);
                        afterAccountAuthentication();
                        break;
                    default:
                        displayErrorMessage(getString(R.string.account_not_chosen_message), true);
                }
                break;
            default:
                throw new IllegalStateException(getString(R.string.unknown_onActivityResult_request));
        }
    }


    /**
     * This shows a Dialog provided by Google Play services that's appropriate for the given error.
     * The dialog may simply provide a message explaining the error, but it may also provide an action
     * to launch an activity that can resolve the error
     * (such as when the user needs to install a newer version of Google Play services).
     *
     * @param errorCode , the error code returned by onConnectionFailed
     */
    private void showGoogleApiErrorDialog(int errorCode) {
        // retrieve dialog for errorCode, if user cancel finish activity,
        // we cannot do much more...google play apk must be present
        Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, errorCode,
                ERROR_RESOLUTION_REQUEST, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Log.e(TAG, getString(R.string.error_dialog_cancelled));
                        //works even if dialog cancelled without clicking any button
                        finish();//TODO maybe refine..and create a method to handle this kind of actions
                    }
                });
        // reset resolvingError to false when dialog dismissed
        errorDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                resolvingError = false;
            }
        });
        // show dialog
        errorDialog.show();
    }

    /**
     * Creates google api client object
     *
     * @see <a href="https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi.html">
     * FusedLocationProviderAPI</a>
     */
    private synchronized void buildGoogleApiClient() {
        app.setGoogleApiClient(
                new GoogleApiClient.Builder(app)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build());
    }

    /**
     * Called after the account was authenticated
     */
    private void afterAccountAuthentication() {
        // The user need to be authenticated before registration
        Intent intent = new Intent(this, RegistrationIntentService.class);
        startService(intent);
    }


    /**
     * Launches an activity allowing the user to create an item
     *
     * @param v The + button
     */
    public void createItem(View v) {
        Intent intent = new Intent(this, CreateItemActivity.class);
        startActivity(intent);
    }
}
