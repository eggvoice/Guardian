package com.edward6chan.www.guardian;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.doomonafireball.betterpickers.hmspicker.HmsPickerBuilder;
import com.doomonafireball.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.*;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.LocationClient;

import java.util.List;
import java.util.UUID;

public class ManageGuardian extends FragmentActivity implements HmsPickerDialogFragment.HmsPickerDialogHandler, GooglePlayServicesClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private SharedPreferences mSharedPreferences;
    private final String TAG = "ManageGuardian";

    private TextView statusView;
    private TextView mToggleSwitch;

    public View alertView;
    public AlertDialog myDialog;
    private int seconds;


    String name, phoneNumber;

    ImageButton mEditAngel, mEditTimer;

    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 1;
    public static final int DETECTION_INTERVAL_MILLISECONDS = MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;

    public enum REQUEST_TYPE {START, STOP}

    private REQUEST_TYPE mRequestType;

    private BroadcastReceiver mActivityBroadcastReceiver;
    private IntentFilter filter;

    Boolean mFlagTimerStarted;
    String activityPerformed;
    int confidence;
    int secondsInt;

    MyCountdownTimer mImmobileTimer;
    MyCountdownTimer mtimerOk;
    Button toggle;
    private TextView mTimer_Set;
    private Context mContext;

    private static final int TEMP_KEY = 1;
    //private static final UUID MY_UUID = UUID.fromString("485579a8-8636-4cd7-9aba-abe7863adbe3");
    private static final UUID MY_UUID = UUID.fromString("3846bf8c-d031-4934-a796-49d55b355315");


    ManageGuardian thisManageGuardian;
    /*
     * Store the PendingIntent used to send activity recognition events
     * back to the app
     */
    private PendingIntent mActivityRecognitionPendingIntent;
    // Store the current activity recognition client
    private ActivityRecognitionClient mActivityRecognitionClient;

    private LocationClient mLocationClient;

    private Location mCurrentLocation;

    // Flag that indicates if a request is underway.
    private boolean mInProgress;
    AlertDialog.Builder builder;

    int mImmobile = 0;

    private static final int KEY_BUTTON_EVENT = 1;
    private static final int BUTTON_EVENT_SELECT = 2;

    private PebbleDataReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_guardian);
        mContext=this;
        thisManageGuardian = this;
        PebbleKit.startAppOnPebble(getApplicationContext(), MY_UUID);           // opens app on Pebble




//        //for alert window
//        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
//        alertView = inflater.inflate(R.layout.custom_alert_layout, null);
        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        //Now you can use the ID with a TextView
        TextView abTitle = (TextView) findViewById(titleId);
        abTitle.setTextColor(getResources().getColor(R.color.guardian_white));

        mActivityRecognitionClient = new ActivityRecognitionClient(mContext, this, this);

        /*
         * Create the PendingIntent that Location Services uses
         * to send activity recognition updates back to this app.
         */
        Intent intent = new Intent(mContext, ActivityRecognitionIntentService.class);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);

        /*
         * Return a PendingIntent that starts the IntentService.
         */
        mActivityRecognitionPendingIntent = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Start with the request flag set to false
        mInProgress = false;

        statusView = (TextView) findViewById(R.id.status);

        //getting shared preferences file
        mSharedPreferences = getSharedPreferences("GUARDIAN_PREFERENCES", MODE_PRIVATE);

        //getting angel contact name from shared preferences file
        name = mSharedPreferences.getString("ANGEL_NAME", null);

        //getting angel contact number from shared preferences file
        phoneNumber = mSharedPreferences.getString("guardian_phone_number", null);

        //getting saved ti`me from shared preferences file
        seconds = Integer.parseInt(mSharedPreferences.getString("TIMER", null));

        PebbleDictionary dict = new PebbleDictionary();
        dict.addString(0, name);
        PebbleKit.sendDataToPebble(getApplicationContext(), MY_UUID, dict);

        //Bundle extras = getIntent().getExtras();
        //if (extras != null) {

        //pulling name from shared preferences
        TextView tv = (TextView) findViewById(R.id.angel_name);
        tv.setText(name);

        //pulling number from shared preferences
        tv = (TextView) findViewById(R.id.angel_phone_number);
        tv.setText(phoneNumber);

        //creating timer and displaying timer to correct textview
        mTimer_Set = (TextView) findViewById(R.id.timer_set);
        secondsInt = seconds;
        secondsInt = secondsInt * 1000;

        mImmobileTimer = new MyCountdownTimer(secondsInt, 1000, mTimer_Set, this, mImmobile);
        mFlagTimerStarted = false;

        //}



        //sendGuardianToWatch(name);

        mLocationClient = new LocationClient(this, this, this);
        Log.i(TAG, "mLocationClient object created.");

    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume() hit.");
        super.onResume();
        // mSensorManager.registerListener(this, mStepSensor, SensorManager.SENSOR_DELAY_FASTEST);

        mReceiver = new PebbleDataReceiver(UUID.fromString("3846bf8c-d031-4934-a796-49d55b355315")) {

            Button toggle = (Button) findViewById(R.id.switch_status);

            @Override
            public void receiveData(Context context, int transactionId, PebbleDictionary data) {

                PebbleKit.sendAckToPebble(context, transactionId);


                if (data.getUnsignedInteger(KEY_BUTTON_EVENT) != null) {
                    int button = data.getUnsignedInteger(KEY_BUTTON_EVENT).intValue();

                    switch(button) {
                        case BUTTON_EVENT_SELECT:


                            toggle.performClick();
                    }
                }
            }

        };

        PebbleKit.registerReceivedDataHandler(this, mReceiver);

        //move to be toggled when active
        //startUpdates();
    }

    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this, mStepSensor);

        unregisterReceiver(mReceiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
        Log.i(TAG, "Connected to location client");
    }

    @Override
    public void onStop() {
        // Disconnect the client.
        mLocationClient.disconnect();
        super.onStop();
        Log.i(TAG, "Disconnected from location client");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.manage_guardian, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     * LOCATION SERVICES METHODS
     */

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    //timer start

    public void startImmobileTimer() {
        mImmobileTimer.start();
    }

    /**
     * Request activity recognition updates based on the current
     * detection interval.
     */
    public void startUpdates() {
        Log.i(TAG, "startUpdates() hit.");


        // Set the request type to START
        mRequestType = REQUEST_TYPE.START;
        /*
         * Test for Google Play services after setting the request type.
         * If Google Play services isn't present, the proper request type
         * can be restarted.
         */

        // Check for Google Play services
        if (!servicesConnected()) {
            return;
        }

        // If a request is not already underway
        if (!mInProgress) {
            Log.i(TAG, "Not in progress");
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
            //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
        //Register broadcast receiver here, to start listening
        mActivityBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //called every time it receives something
                //'intent' stores information that the intent that is sending the info stores for this receiver
                // in our case --- ActivityRecognitionIntentService
                // intent.getStringExtra("Activity") --- stores the activity name/code
                // intent.getExtras().getInt("Confidence") -- corresponding confidence (100% etc..)

                activityPerformed = intent.getStringExtra("Activity");
                confidence = intent.getExtras().getInt("Confidence");

                Log.i(TAG, "Activity: " + activityPerformed + ", " + "Confidence: " + confidence);

                if (activityPerformed.isEmpty()){
                    statusView.setText("Activating Guardian...");
                }
                else {
                    statusView.setText(activityPerformed + ", " + confidence);
                }
                //mImmobileTimer.onTick(long );

                String still = "Still";
                if (activityPerformed.equals(still) && !mFlagTimerStarted) {
                    //    mImmobileTimer.start();
                    startImmobileTimer();
                    mFlagTimerStarted = true;

                } else if (activityPerformed.equals(still)) {

                } else {
                    mImmobileTimer.cancel();
                    mImmobileTimer.timerReset(secondsInt, mImmobile);
                    mFlagTimerStarted = false;


                }
            }


        };


        filter = new IntentFilter();

        filter.addAction("com.edward6chan.www.guardian.ACTIVITY_RECOGNITION_DATA");

        registerReceiver(mActivityBroadcastReceiver, filter);
    }

    /**
     * Turn off activity recognition updates
     */
    public void stopUpdates() {
        Log.i(TAG, "stopUpdates() hit.");

        // Set the request type to STOP
        mRequestType = REQUEST_TYPE.STOP;
        /*
         * Test for Google Play services after setting the request type.
         * If Google Play services isn't present, the request can be
         * restarted.
         */
        if (!servicesConnected()) {
            return;
        }

        mInProgress = false;

        mActivityRecognitionClient.removeActivityUpdates(mActivityRecognitionPendingIntent);

        //krista added not android - will this help it stop getting updates?
        mActivityRecognitionClient.disconnect();
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        Log.i(TAG, "onConnected() hit.");

        if (mRequestType != null) {
            switch (mRequestType) {
                case START:
                    Log.i(TAG, "Case: START");

                /*
                 * Request activity recognition updates using the
                 * preset detection interval and PendingIntent.
                 * This call is synchronous.
                 */
                    mActivityRecognitionClient.requestActivityUpdates(DETECTION_INTERVAL_MILLISECONDS, mActivityRecognitionPendingIntent);

                    break;

                case STOP:
                    //CASE NOT REQUIRED -- CLEANUP
                    Log.i(TAG, "Case: STOP");

                    mActivityRecognitionClient.removeActivityUpdates(mActivityRecognitionPendingIntent);

                    //krista added not android - will this help it stop getting updates?
                    mActivityRecognitionClient.disconnect();

                    break;
                /*
                 * An enum was added to the definition of REQUEST_TYPE,
                 * but it doesn't match a known case. Throw an exception.
                 */
                default:
                    try {
                        throw new Exception("Unknown request type in onConnected().");
                    } catch (Exception e) {
                        Log.e(TAG, "Exception thrown: " + e.getMessage());
                    }
                    break;
            }
        }
    }


    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected() hit.");

        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        mActivityRecognitionClient = null;
    }

    // Implementation of OnConnectionFailedListener.onConnectionFailed
    //for google play services
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed() hit.");

        // Turn off the request flag
        mInProgress = false;
        /*
         * If the error has a resolution, start a Google Play services
         * activity to resolve it.
         */
        if (connectionResult.hasResolution()) {
            try {

                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
            // If no resolution is available, display an error dialog
        } else {
            // Get the error code
            int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);
            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(), "Activity Recognition");
            }
        }
    }

    public void handleTimePicker(View v) {
        HmsPickerBuilder hmsPickerBuilder = new HmsPickerBuilder()
                .setFragmentManager(getSupportFragmentManager())
                .setStyleResId(R.style.BetterPickersDialogFragment);
        hmsPickerBuilder.show();

    }


    @Override
    public void onDialogHmsSet(int i, int hour, int minute, int second) {

        seconds = hour * 60 * 60 + minute * 60 + second;
        int milliSeconds = seconds * 1000;
        mSharedPreferences.edit().putString("TIMER", seconds + "").commit();
        secondsInt = milliSeconds;
        mImmobileTimer = new MyCountdownTimer(secondsInt, 1000, mTimer_Set, this, mImmobile);
        mImmobileTimer.cancel();
        mImmobileTimer.timerReset(secondsInt, mImmobile);
        mFlagTimerStarted = false;

    }

    //when the immobile timer expires, Guardian asks the user if they are okay to determine
    //if they are truly unresponsive
    public void timerDoneAskOk() {
        //for alert window
        toggle.performClick();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        alertView = inflater.inflate(R.layout.custom_alert_layout, null);
        //add android.R.style.Theme_DeviceDefault_Light_Dialog if action bar is readded
        //android.R.style.Theme_Holo_Light_Dialog
        builder = new AlertDialog.Builder(this);
        builder.setView(alertView);

        TextView okTimer = (TextView) alertView.findViewById(R.id.ok_timer);
        int ok = 1;
        //should be 45000 but changed to 5000 for testing purposes
        mtimerOk = new MyCountdownTimer(15000, 1000, okTimer, thisManageGuardian, ok);
        builder.setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //mImmobileTimer.timerReset(secondsInt, mImmobile);
                toggle.performClick();
                mtimerOk.cancel();

            }
        })
                .setNeutralButton(R.string.setInactive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mtimerOk.cancel();
                    }
                });

        myDialog = builder.create();
        myDialog.show();


    }


    public void onSwitchClick(View v) {

        toggle = (Button) v;

        mEditAngel = (ImageButton) findViewById(R.id.edit_angel);
        mEditTimer = (ImageButton) findViewById(R.id.edit_timer);

        mToggleSwitch = (TextView) findViewById(R.id.toggle_active_inactive);
        String active_inactive = mToggleSwitch.getText().toString();

        if (active_inactive.equals("ACTIVE")) {
            mToggleSwitch.setText("INACTIVE");
            stopUpdates();
            // stop broadcast receiver
            unregisterReceiver(mActivityBroadcastReceiver);
            statusView.setText("");
            mImmobileTimer.cancel();
            mImmobileTimer.timerReset(secondsInt, mImmobile);
            mFlagTimerStarted = false;
            mEditAngel.setVisibility(View.VISIBLE);
            mEditTimer.setVisibility(View.VISIBLE);

        }
        if (active_inactive.equals("ANGEL CONTACTED")) {
            mToggleSwitch.setText("ACTIVE");
            // stop broadcast receiver
//            unregisterReceiver(mActivityBroadcastReceiver);
//            mImmobileTimer.cancel();
//            mImmobileTimer.timerReset(secondsInt, mImmobile);
//            mFlagTimerStarted = false;
//            mEditAngel.setVisibility(View.VISIBLE);
//            mEditTimer.setVisibility(View.VISIBLE);
            startUpdates();
            mEditAngel.setVisibility(View.GONE);
            mEditTimer.setVisibility(View.GONE);

        }
        if (active_inactive.equals("INACTIVE")) {
            mToggleSwitch.setText("ACTIVE");
            startUpdates();
            mEditAngel.setVisibility(View.GONE);
            mEditTimer.setVisibility(View.GONE);


        }
        PebbleDictionary dict = new PebbleDictionary();
        dict.addInt32(BUTTON_EVENT_SELECT, 0);
        PebbleKit.sendDataToPebble(getApplicationContext(), MY_UUID, dict);
    }

    final int PICK_CONTACT = 1;

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {

                    String[] projection = new String[]{
                            ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
                    Uri contacts = data.getData();
                    Cursor cursor = getContentResolver().query(contacts,
                            projection, // Which columns to return
                            null,       // Which rows to return (all rows)
                            // Selection arguments (with a given ID)
                            null,
                            // Put the results in ascending order by name
                            null);
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        Intent i = new Intent(this, ManageGuardian.class);

                        //Save to shared preferences
                        mSharedPreferences.edit().putString("ANGEL_NAME", name + "").commit();
                        mSharedPreferences.edit().putString("guardian_phone_number", phoneNumber + "").commit();
                        /*Bundle guardian_info = new Bundle();
                        guardian_info.putString("guardian_name", name);
                        guardian_info.putString("guardian_phone_number", phoneNumber);
                        i.putExtras(guardian_info);*/
                        //i.putExtra("guardian_name", name);

                        ManageGuardian.this.startActivity(i);
                    }
                }
                break;

            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
            /*
             * If the result code is Activity.RESULT_OK, try
             * to connect again
             */
                switch (resultCode) {
                    case Activity.RESULT_OK:
                    /*
                     * TODO: TRY REQUEST AGAIN
                     */
                        break;
                }
        }
    }

    //for google play services
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Activity Recognition", "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    resultCode,
                    this,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);

            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(getFragmentManager(), "Activity Recognition");
            }
            return false;
        }
    }

    public void onEditAngelClick(View v) {
        mEditAngel = (ImageButton) v;
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(intent, PICK_CONTACT);
    }


    //**
    //SEND SMS WITH LOCATION TO ANGEL
    //**
    public void contactAngel() {
        mToggleSwitch.setText("ANGEL CONTACTED");
        //stopUpdates();
        requestLocationForSms();

    }

    public void requestLocationForSms() {

        if (servicesConnected() == true) {
            mCurrentLocation = mLocationClient.getLastLocation();
            Log.i(TAG, "Current Location retrieved");
            if (mCurrentLocation != null) {
                Log.i(TAG, mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude());
                sendLocationSms(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            } else {
                // Build the alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                builder.setTitle("Location Services Not Active");
                builder.setMessage("Please enable Location Services and GPS");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Show location settings when the user acknowledges the alert dialog
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                });

                Dialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        }

//        if (mProviderName != null && mLocationPending == false) {
//            mLocationPending = true;
//
//            Location lastLocation = mLocationManager.getLastKnownLocation(mProviderName);
//            // if we have a location that's newer than 10 minutes, use it; otherwise get a new location
//            if (lastLocation != null && (System.currentTimeMillis() - lastLocation.getTime() > DateUtils.MINUTE_IN_MILLIS * 10)) {
//                mLocationManager.requestLocationUpdates(mProviderName,
//                        10000,
//                        10,
//                        mLocationListener);
//            } else {
//            }
//        }
    }

    public void sendLocationSms(double latitude, double longitude) {

        Log.i(TAG, "sendLocationSms() hit. Lat/Long: " + latitude + "," + longitude);
        // send SMS with GPS coordinates
        SmsManager smsManager = SmsManager.getDefault();
        String locationString = "Get me: " + latitude + ", " + longitude;
        Log.i(TAG, "Phone number:" + phoneNumber);
        smsManager.sendTextMessage(phoneNumber, null, locationString, null, null);

        // get address text if we can
        Geocoder geocoder = new Geocoder(ManageGuardian.this);

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                Address a = addresses.get(0);
                String addressText = "";
                for (int i = 0; i <= a.getMaxAddressLineIndex(); i++) {
                    addressText += a.getAddressLine(i) + " ";
                }
                smsManager.sendTextMessage(phoneNumber, null, addressText, null, null);
            }
        } catch (Exception e) {
            // unable to geocode
        }
    }

    public void receiveData(Context context, int transactionId, PebbleDictionary data) {

        PebbleKit.sendAckToPebble(context, transactionId);


        if (data.getUnsignedInteger(KEY_BUTTON_EVENT) != null) {
            int button = data.getUnsignedInteger(KEY_BUTTON_EVENT).intValue();

            switch(button) {
                case BUTTON_EVENT_SELECT:


                    mEditAngel = (ImageButton) findViewById(R.id.edit_angel);
                    mEditTimer = (ImageButton) findViewById(R.id.edit_timer);

                    mToggleSwitch = (TextView) findViewById(R.id.toggle_active_inactive);
                    String active_inactive = mToggleSwitch.getText().toString();

                    if (active_inactive.equals("ACTIVE")) {
                        mToggleSwitch.setText("INACTIVE");
                        stopUpdates();
                        // stop broadcast receiver
                        unregisterReceiver(mActivityBroadcastReceiver);
                        statusView.setText("");
                        mImmobileTimer.cancel();
                        mImmobileTimer.timerReset(secondsInt, mImmobile);
                        mFlagTimerStarted = false;
                        mEditAngel.setVisibility(View.VISIBLE);
                        mEditTimer.setVisibility(View.VISIBLE);

                    }
                    if (active_inactive.equals("ANGEL CONTACTED")) {
                        mToggleSwitch.setText("ACTIVE");
                        // stop broadcast receiver
//            unregisterReceiver(mActivityBroadcastReceiver);
//            mImmobileTimer.cancel();
//            mImmobileTimer.timerReset(secondsInt, mImmobile);
//            mFlagTimerStarted = false;
//            mEditAngel.setVisibility(View.VISIBLE);
//            mEditTimer.setVisibility(View.VISIBLE);
                        startUpdates();
                        mEditAngel.setVisibility(View.GONE);
                        mEditTimer.setVisibility(View.GONE);

                    }
                    if (active_inactive.equals("INACTIVE")) {
                        mToggleSwitch.setText("ACTIVE");
                        startUpdates();
                        mEditAngel.setVisibility(View.GONE);
                        mEditTimer.setVisibility(View.GONE);


                    }
            }
        }
    }




}