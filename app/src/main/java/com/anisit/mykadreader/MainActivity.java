package com.anisit.mykadreader;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

/*
 * Copyright (C) 2011-2013 Advanced Card Systems Ltd. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Advanced
 * Card Systems Ltd. ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ACS.
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.acs.smartcard.Features;
import com.acs.smartcard.PinModify;
import com.acs.smartcard.PinProperties;
import com.acs.smartcard.PinVerify;
import com.acs.smartcard.ReadKeyOption;
import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;
import com.acs.smartcard.ReaderException;
import com.acs.smartcard.TlvProperties;

import org.json.JSONObject;

/**
 * Test program for ACS smart card readers.
 *
 * @author Godfrey Chung
 * @version 1.1.1, 16 Apr 2013
 */
public class MainActivity extends AppCompatActivity {

    public static int _deviceSlotNum;
    public String TAG = "MyKad";
    public Helper myHelper;
    public MyKad myKad;
    public MyKad_JPN myKad_jpn;
    public MyKad_Data myKad_data;

    public CardReader myCardReader;

    public JSONObject obj;
    private static final String MESSAGE = "Message";
    private static final String NAME = "Name";
    private static final String NRIC = "Nric";
    private static final String CITIZENSHIP = "Citizenship";
    private static final String CITY = "City";
    private static final String ADDRESS1 = "Address1";
    private static final String ADDRESS2 = "Address2";
    private static final String ADDRESS3 = "Address3";
    private static final String DOB = "DOB";
    private static final String GENDER = "Gender";
    private static final String RACE = "Race";
    private static final String POSTCODE = "Postcode";
    private static final String STATE = "State";

    public String message = "";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final String[] powerActionStrings = { "Power Down",
            "Cold Reset", "Warm Reset" };

    private static final String[] stateStrings = { "Unknown", "Absent",
            "Present", "Swallowed", "Powered", "Negotiable", "Specific" };

    private static final String[] featureStrings = { "FEATURE_UNKNOWN",
            "FEATURE_VERIFY_PIN_START", "FEATURE_VERIFY_PIN_FINISH",
            "FEATURE_MODIFY_PIN_START", "FEATURE_MODIFY_PIN_FINISH",
            "FEATURE_GET_KEY_PRESSED", "FEATURE_VERIFY_PIN_DIRECT",
            "FEATURE_MODIFY_PIN_DIRECT", "FEATURE_MCT_READER_DIRECT",
            "FEATURE_MCT_UNIVERSAL", "FEATURE_IFD_PIN_PROPERTIES",
            "FEATURE_ABORT", "FEATURE_SET_SPE_MESSAGE",
            "FEATURE_VERIFY_PIN_DIRECT_APP_ID",
            "FEATURE_MODIFY_PIN_DIRECT_APP_ID", "FEATURE_WRITE_DISPLAY",
            "FEATURE_GET_KEY", "FEATURE_IFD_DISPLAY_PROPERTIES",
            "FEATURE_GET_TLV_PROPERTIES", "FEATURE_CCID_ESC_COMMAND" };

    private static final String[] propertyStrings = { "Unknown", "wLcdLayout",
            "bEntryValidationCondition", "bTimeOut2", "wLcdMaxCharacters",
            "wLcdMaxLines", "bMinPINSize", "bMaxPINSize", "sFirmwareID",
            "bPPDUSupport", "dwMaxAPDUDataSize", "wIdVendor", "wIdProduct" };

    private static final int DIALOG_VERIFY_PIN_ID = 0;
    private static final int DIALOG_MODIFY_PIN_ID = 1;
    private static final int DIALOG_READ_KEY_ID = 2;
    private static final int DIALOG_DISPLAY_LCD_MESSAGE_ID = 3;

    private UsbManager mManager;
    public static Reader mReader;
    private PendingIntent mPermissionIntent;
    private static Context _context;

    private Helper helper;


    private static final int MAX_LINES = 25;
    private TextView mResponseTextView;
    private ArrayAdapter<String> mReaderAdapter;
    private ArrayAdapter<String> mSlotAdapter;
    private Button mReadButton;

    private Features mFeatures = new Features();
    private PinVerify mPinVerify = new PinVerify();
    private PinModify mPinModify = new PinModify();
    private ReadKeyOption mReadKeyOption = new ReadKeyOption();
    private String mLcdMessage;
    private int mThemeId;
    private Handler pollhandler;
    private Handler detecthandler;
    private Handler logsHandler;

    public int protocol;
    private DateFormat dateFormat;
//    private BroadcastReceiver mReceiver;

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();


                if (ACTION_USB_PERMISSION.equals(action)) {

                    synchronized (this) {

                        UsbDevice device = (UsbDevice) intent
                                .getParcelableExtra(UsbManager.EXTRA_DEVICE);

//                        logMsg(device.getDeviceName());

                        if (intent.getBooleanExtra(
                                UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                            if (device != null) {

                                // Open reader
                                logMsg("Opening reader: " + device.getDeviceName()
                                        + "...");
                                new OpenTask().execute(device);
                            }

                        } else {

                            if(device != null) {

                                logMsg("Permission denied for device "
                                        + device.getDeviceName());
                            }else{
                                logMsg("Permission denied for device null");
                            }
//                            // Enable open button
//                            mOpenButton.setEnabled(true);
                        }
                    }

                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                    synchronized (this) {

                        // Update reader list
                        mReaderAdapter.clear();
                        for (UsbDevice device : mManager.getDeviceList().values()) {
                            if (mReader.isSupported(device)) {
                                mReaderAdapter.add(device.getDeviceName());
                            }
                        }

                        UsbDevice device = (UsbDevice) intent
                                .getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        if (device != null && device.equals(mReader.getDevice())) {

//                            // Disable buttons
//                            mCloseButton.setEnabled(false);
//                            // Clear slot items
//                            mSlotAdapter.clear();

                            // Close reader
                            logMsg("Closing reader...");
                            new CloseTask().execute();
                        }
                    }
                }
            }
        };


    private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {

        @Override
        protected Exception doInBackground(UsbDevice... params) {

            Exception result = null;

            try {

                mReader.open(params[0]);

            } catch (Exception e) {

                result = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(Exception result) {

            if (result != null) {

                logMsg(result.toString());

            } else {

                logMsg("Reader name: " + mReader.getReaderName());

                int numSlots = mReader.getNumSlots();
                logMsg("Number of slots: " + numSlots);

//                // Add slot items
//                mSlotAdapter.clear();
//                for (int i = 0; i < numSlots; i++) {
//                    mSlotAdapter.add(Integer.toString(i));
//                }

                // Remove all control codes
                mFeatures.clear();
            }
        }
    }
    public class ConnectProgress {
        public byte[] atr;
        public int atrLength;
        public Exception e;
    }
    class LogHandler implements Runnable {
        private final /* synthetic */ TransmitProgress val$progress;
        private String _deviceSlotNum;

        LogHandler(TransmitProgress transmitProgress) {
            this.val$progress = transmitProgress;
        }

        public void run() {
            logMsg("Slot " + this._deviceSlotNum + ": Transmitting Control...");
            logMsg("Command:");
            logBuffer(this.val$progress.command, this.val$progress.commandLength);
            logMsg("Response:");
            logBuffer(this.val$progress.response, this.val$progress.responseLength);
        }
    }
    public TransmitProgress sendApdu(byte[] bArr) {
        logMsg("tesss");
        if (this._deviceSlotNum == -1) {
            return null;
        }
        byte[] bArr2 = new byte[300];
        TransmitProgress transmitProgress = new TransmitProgress();
        try {
            int transmit = this.mReader.transmit(this._deviceSlotNum , bArr, bArr.length, bArr2, bArr2.length);
            transmitProgress.command = bArr;
            transmitProgress.commandLength = bArr.length;
            transmitProgress.response = bArr2;
            transmitProgress.responseLength = transmit;
            transmitProgress.e = null;
            this.logsHandler.post(new LogHandler(transmitProgress));
            logMsg(transmitProgress.toString());
            return transmitProgress;
        } catch (Exception e) {
            transmitProgress.command = null;
            transmitProgress.commandLength = 0;
            transmitProgress.response = null;
            transmitProgress.responseLength = 0;
            transmitProgress.e = e;
            logMsg(transmitProgress.toString());
            return transmitProgress;
        }
    }

    public TransmitProgress sendControl(byte[] bArr, int i) {
        if (this._deviceSlotNum  == -1) {
            return null;
        }
        byte[] bArr2 = new byte[300];
        TransmitProgress transmitProgress = new TransmitProgress();
        try {
            int control = this.mReader.control(this._deviceSlotNum , i, bArr, bArr.length, bArr2, bArr2.length);
            transmitProgress.command = bArr;
            transmitProgress.commandLength = bArr.length;
            transmitProgress.response = bArr2;
            transmitProgress.responseLength = control;
            transmitProgress.e = null;
            this.logsHandler.post(new LogHandler(transmitProgress));
            return transmitProgress;
        } catch (Exception e) {
            transmitProgress.command = null;
            transmitProgress.commandLength = 0;
            transmitProgress.response = null;
            transmitProgress.responseLength = 0;
            transmitProgress.e = e;
            return transmitProgress;
        }
    }

    private class CloseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            mReader.close();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
//            mOpenButton.setEnabled(true);
        }

    }


    private class PowerParams {

        public int slotNum;
        public int action;
    }

    private class PowerResult {

        public byte[] atr;
        public Exception e;
    }

    private class PowerTask extends AsyncTask<PowerParams, Void, PowerResult> {

        @Override
        protected PowerResult doInBackground(PowerParams... params) {

            PowerResult result = new PowerResult();

            try {

                result.atr = mReader.power(params[0].slotNum, params[0].action);

            } catch (Exception e) {

                result.e = e;
            }

            return result;
        }

        @Override
        protected void onPostExecute(PowerResult result) {

            if (result.e != null) {

                logMsg(result.e.toString());

            } else {

                // Show ATR
                if (result.atr != null) {

                    logMsg("ATR:");
                    logBuffer(result.atr, result.atr.length);

                } else {

                    logMsg("ATR: None");
                }
            }
        }
    }

    public void closeMykad() {
        //anis create utk panggil close after use

//                // Clear slot items
//                mSlotAdapter.clear();

        // Close reader
        logMsg("Closing reader...");
        new CloseTask().execute();
    }

    private void setPower() {

        // Get slot number
//        int slotNum = mSlotSpinner.getSelectedItemPosition();
        int slotNum = 0;

        // Get action number
//        int actionNum = mPowerSpinner.getSelectedItemPosition();
        int actionNum = 2;

        // If slot and action are selected
        if (slotNum != Spinner.INVALID_POSITION
                && actionNum != Spinner.INVALID_POSITION) {

            if (actionNum < Reader.CARD_POWER_DOWN
                    || actionNum > Reader.CARD_WARM_RESET) {
                actionNum = Reader.CARD_WARM_RESET;
            }

            // Set parameters
            PowerParams params = new PowerParams();
            params.slotNum = slotNum;
            params.action = actionNum;

            // Perform power action
            logMsg("Slot " + slotNum + ": "
                    + powerActionStrings[actionNum] + "...");
            new PowerTask().execute(params);
        }
    }

    private void setProtocol() {

        // Get slot number
//        int slotNum = mSlotSpinner.getSelectedItemPosition();
        int slotNum = 0;

        // If slot is selected
        if (slotNum != Spinner.INVALID_POSITION) {

            int preferredProtocols = Reader.PROTOCOL_UNDEFINED;
            String preferredProtocolsString = "";

//            if (mT0CheckBox.isChecked()) {

                preferredProtocols = Reader.PROTOCOL_T0;
                preferredProtocolsString = "T=0";
//            }

//            if (mT1CheckBox.isChecked()) {
//
//                preferredProtocols = Reader.PROTOCOL_T1;
//                if (preferredProtocolsString != "") {
//                    preferredProtocolsString += "/";
//                }
//
//                preferredProtocolsString += "T=1";
//            }

            if (preferredProtocolsString == "") {
                preferredProtocolsString = "None";
            }

            // Set Parameters
            SetProtocolParams params = new SetProtocolParams();
            params.slotNum = slotNum;
            params.preferredProtocols = preferredProtocols;

            // Set protocol
            logMsg("Slot " + slotNum + ": Setting protocol to "
                    + preferredProtocolsString + "...");
            new SetProtocolTask().execute(params);
        }
    }

    private class SetProtocolParams {

        public int slotNum;
        public int preferredProtocols;
    }

    private class SetProtocolResult {

        public int activeProtocol;
        public Exception e;
    }

    private class SetProtocolTask extends
            AsyncTask<SetProtocolParams, Void, SetProtocolResult> {

        @Override
        protected SetProtocolResult doInBackground(SetProtocolParams... params) {

            SetProtocolResult result = new SetProtocolResult();

            try {


                if(mReader.isOpened()){
                    logMsg("reader buka");
                    result.activeProtocol = mReader.setProtocol(params[0].slotNum,
                            params[0].preferredProtocols);

                    logMsg("Set: " + result.activeProtocol);
                    Log.d(TAG,"Set: " + result.activeProtocol);
                }else{
                    logMsg("cuba buka reader");
                    openReader();
                }


            } catch (ReaderException e) {

                result.e = e;
                Log.d(TAG,"Failed Set: " +  result.e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(SetProtocolResult result) {

            if (result.e != null) {

                logMsg(result.e.toString());

            } else {

                String activeProtocolString = "Active Protocol: ";

                switch (result.activeProtocol) {

                    case Reader.PROTOCOL_T0:
                        activeProtocolString += "T=0";
                        break;

                    case Reader.PROTOCOL_T1:
                        activeProtocolString += "T=1";
                        break;

                    default:
                        activeProtocolString += "Unknown";
                        break;
                }

                // Show active protocol
                logMsg(activeProtocolString);
            }
        }
    }

    private class TransmitParams {

        public int slotNum;
        public int controlCode;
        public String commandString;
    }

    class TransmitProgress {

        public int controlCode;
        public byte[] command;
        public int commandLength;
        public byte[] response;
        public int responseLength;
        public Exception e;
    }

    private class TransmitTask extends
            AsyncTask<TransmitParams, TransmitProgress, Void> {

        @Override
        protected Void doInBackground(TransmitParams... params) {

            TransmitProgress progress = null;

            byte[] command = null;
            byte[] response = null;
            int responseLength = 0;
            int foundIndex = 0;
            int startIndex = 0;

            do {

                // Find carriage return
                foundIndex = params[0].commandString.indexOf('\n', startIndex);
                if (foundIndex >= 0) {
                    command = toByteArray(params[0].commandString.substring(
                            startIndex, foundIndex));
                } else {
                    command = toByteArray(params[0].commandString
                            .substring(startIndex));
                }

                // Set next start index
                startIndex = foundIndex + 1;

                response = new byte[65538];
                progress = new TransmitProgress();
                progress.controlCode = params[0].controlCode;
                try {

                    if (params[0].controlCode < 0) {

                        // Transmit APDU
                        responseLength = mReader.transmit(params[0].slotNum,
                                command, command.length, response,
                                response.length);

                    } else {

                        // Transmit control command
                        responseLength = mReader.control(params[0].slotNum,
                                params[0].controlCode, command, command.length,
                                response, response.length);
                    }

                    progress.command = command;
                    progress.commandLength = command.length;
                    progress.response = response;
                    progress.responseLength = responseLength;
                    progress.e = null;

                } catch (Exception e) {

                    progress.command = null;
                    progress.commandLength = 0;
                    progress.response = null;
                    progress.responseLength = 0;
                    progress.e = e;
                }

                publishProgress(progress);

            } while (foundIndex >= 0);

            return null;
        }

        @Override
        protected void onProgressUpdate(TransmitProgress... progress) {

            if (progress[0].e != null) {

                logMsg(progress[0].e.toString());

            } else {

                logMsg("Command:");
                logBuffer(progress[0].command, progress[0].commandLength);

                logMsg("Response:");
                logBuffer(progress[0].response, progress[0].responseLength);

                if (progress[0].response != null
                        && progress[0].responseLength > 0) {

                    int controlCode;
                    int i;

                    // Show control codes for IOCTL_GET_FEATURE_REQUEST
                    if (progress[0].controlCode == Reader.IOCTL_GET_FEATURE_REQUEST) {

                        mFeatures.fromByteArray(progress[0].response,
                                progress[0].responseLength);

                        logMsg("Features:");
                        for (i = Features.FEATURE_VERIFY_PIN_START; i <= Features.FEATURE_CCID_ESC_COMMAND; i++) {

                            controlCode = mFeatures.getControlCode(i);
                            if (controlCode >= 0) {
                                logMsg("Control Code: " + controlCode + " ("
                                        + featureStrings[i] + ")");
                            }
                        }

                    }

                    controlCode = mFeatures
                            .getControlCode(Features.FEATURE_IFD_PIN_PROPERTIES);
                    if (controlCode >= 0
                            && progress[0].controlCode == controlCode) {

                        PinProperties pinProperties = new PinProperties(
                                progress[0].response,
                                progress[0].responseLength);

                        logMsg("PIN Properties:");
                        logMsg("LCD Layout: "
                                + toHexString(pinProperties.getLcdLayout()));
                        logMsg("Entry Validation Condition: "
                                + toHexString(pinProperties
                                .getEntryValidationCondition()));
                        logMsg("Timeout 2: "
                                + toHexString(pinProperties.getTimeOut2()));
                    }

                    controlCode = mFeatures
                            .getControlCode(Features.FEATURE_GET_TLV_PROPERTIES);
                    if (controlCode >= 0
                            && progress[0].controlCode == controlCode) {

                        TlvProperties readerProperties = new TlvProperties(
                                progress[0].response,
                                progress[0].responseLength);

                        Object property;
                        logMsg("TLV Properties:");
                        for (i = TlvProperties.PROPERTY_wLcdLayout; i <= TlvProperties.PROPERTY_wIdProduct; i++) {

                            property = readerProperties.getProperty(i);
                            if (property instanceof Integer) {
                                logMsg(propertyStrings[i] + ": "
                                        + toHexString((Integer) property));
                            } else if (property instanceof String) {
                                logMsg(propertyStrings[i] + ": " + property);
                            }
                        }
                    }
                }
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get USB manager
        mManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Initialize reader
        mReader = new Reader(mManager);
        mReader.setOnStateChangeListener(new OnStateChangeListener() {

            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {

                if (prevState < Reader.CARD_UNKNOWN
                        || prevState > Reader.CARD_SPECIFIC) {
                    prevState = Reader.CARD_UNKNOWN;
                }

                if (currState < Reader.CARD_UNKNOWN
                        || currState > Reader.CARD_SPECIFIC) {
                    currState = Reader.CARD_UNKNOWN;
                }

                // Create output string
                final String outputString = "Slot " + slotNum + ": "
                        + stateStrings[prevState] + " -> "
                        + stateStrings[currState];

                final TextView textWujud = (TextView)findViewById(R.id.wujud);


                if(stateStrings[currState] == "Absent"){
                    textWujud.setText("Tiada Kad Dikesan");
                    Button buttonRead = (Button) findViewById(R.id.main_button_read);
                    buttonRead.setClickable(false);
                    buttonRead.setBackgroundColor(Color.GRAY);
                }else if(stateStrings[currState] == "Present"){
                    textWujud.setText("Kad Dikesan");
                    Button buttonRead = (Button) findViewById(R.id.main_button_read);
                    buttonRead.setClickable(true);
                    buttonRead.setBackgroundColor(Color.BLUE);
                }

                // Show output
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        logMsg(outputString);
                    }
                });
            }
        });

        // Register receiver for USB permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                    ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        }else {
            mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
                    ACTION_USB_PERMISSION),  PendingIntent.FLAG_UPDATE_CURRENT);
//        mPermissionIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(
//                ACTION_USB_PERMISSION),PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(this.mReceiver, filter);

        // Initialize response text view
        mResponseTextView = (TextView) findViewById(R.id.main_text_view_response);
        mResponseTextView.setMovementMethod(new ScrollingMovementMethod());
        mResponseTextView.setMaxLines(MAX_LINES);
        mResponseTextView.setText("");

        Button buttonRead = (Button) findViewById(R.id.main_button_read);
        buttonRead.setClickable(false);
        buttonRead.setBackgroundColor(Color.GRAY);

        openReader();



        // PIN verification command (ACOS3)
        byte[] pinVerifyData = { (byte) 0x80, 0x20, 0x06, 0x00, 0x08,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };


        // PIN modification command (ACOS3)
        byte[] pinModifyData = { (byte) 0x80, 0x24, 0x00, 0x00, 0x08,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

        // Hide input window
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onResume() {
        super.onResume();

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            // Your code here!
        }
    }

    private void openReader(){
        String deviceName = null;
        // Initialize reader spinner
        mReaderAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item);
        for (UsbDevice device : mManager.getDeviceList().values()) {
            if (mReader.isSupported(device)) {
                mReaderAdapter.add(device.getDeviceName());
                deviceName = (String) device.getDeviceName();
            }
        }


        if (deviceName != null) {

            // For each device
            for (UsbDevice device : mManager.getDeviceList().values()) {

                // If device name is found
                if (deviceName.equals(device.getDeviceName())) {

                    // Request permission
                    mManager.requestPermission(device,
                            mPermissionIntent);
                    break;
                }
            }
        }
    }
    public void onClickRead(View view) {
        setPower();
        setProtocol();

        myCardReader = new CardReader(_context, -1);
//        Log.d(TAG,"cardreader okiee !");
        logMsg("masuk read");
        myKad = new MyKad(myCardReader);

        logMsg("lepas mykad");
        Log.d(TAG, "berjaya myKad ! tttt");
        myKad_data = new MyKad_Data();
        Log.d(TAG, "berjaya myKad data! tttt");
//

        if (myKad != null) {
            try {

                if (myKad.selectApplicationJPN() == true) {
                    myKad_data = myKad.GetMyKadDetail();

                    message += "Result is "
                            + myKad_data.GetName() + "\n"
                            + myKad_data.GetNric() + "\n"
                            + myKad_data.GetCitizenship() + "\n"
                            + myKad_data.GetCity() + "\n"
                            + myKad_data.GetAddress1() + "\n"
                            + myKad_data.GetAddress2() + "\n"
                            + myKad_data.GetAddress3() + "\n"
                            + myKad_data.GetDateOfBirth() + "\n"
                            + myKad_data.GetGender() + "\n"
                            + myKad_data.GetRace() + "\n"
                            + myKad_data.GetPostcode() + "\n"
                            + myKad_data.GetState() + "\n"
                    ;


                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result1", "VerifyActivity:onClickVerify => " );
                    returnIntent.putExtra("name", myKad_data.GetName());
                    returnIntent.putExtra("nric", myKad_data.GetNric());
                    returnIntent.putExtra("citizenship", myKad_data.GetCitizenship());
                    returnIntent.putExtra("city", myKad_data.GetCity() );
                    returnIntent.putExtra("address", myKad_data.GetAddress1());
                    returnIntent.putExtra("dob", myKad_data.GetDateOfBirth());
                    returnIntent.putExtra("gender", myKad_data.GetGender());
                    setResult(Activity.RESULT_OK, returnIntent);
                } else {
                    message += "\n Using JPN Application is not supported";
                }


            } catch (Exception e) {
                message += "Trying to read card failed "+ e;
            }
            logMsg(message);
        } else {
            logMsg("mykad null");
        }
    }

    @Override
    protected void onDestroy() {

        // Close reader
        mReader.close();

        // Unregister receiver
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    /**
     * Logs the message.
     *
     * @param msg
     *            the message.
     */
    public void logMsg(String msg) {

        DateFormat dateFormat = new SimpleDateFormat("[dd-MM-yyyy HH:mm:ss]: ");
        Date date = new Date();
        String oldMsg = mResponseTextView.getText().toString();

        mResponseTextView
                .setText(oldMsg + "\n" + dateFormat.format(date) + msg);

        if (mResponseTextView.getLineCount() > MAX_LINES) {
            mResponseTextView.scrollTo(0,
                    (mResponseTextView.getLineCount() - MAX_LINES)
                            * mResponseTextView.getLineHeight());
        }
    }

    /**
     * Logs the contents of buffer.
     *
     * @param buffer
     *            the buffer.
     * @param bufferLength
     *            the buffer length.
     */
    private void logBuffer(byte[] buffer, int bufferLength) {

        String bufferString = "";

        for (int i = 0; i < bufferLength; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            if (i % 16 == 0) {

                if (bufferString != "") {

                    logMsg(bufferString);
                    bufferString = "";
                }
            }

            bufferString += hexChar.toUpperCase() + " ";
        }

        if (bufferString != "") {
            logMsg(bufferString);
        }
    }

    /**
     * Converts the HEX string to byte array.
     *
     * @param hexString
     *            the HEX string.
     * @return the byte array.
     */
    private byte[] toByteArray(String hexString) {

        int hexStringLength = hexString.length();
        byte[] byteArray = null;
        int count = 0;
        char c;
        int i;

        // Count number of hex characters
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];
        boolean first = true;
        int len = 0;
        int value;
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {

                if (first) {

                    byteArray[len] = (byte) (value << 4);

                } else {

                    byteArray[len] |= value;
                    len++;
                }

                first = !first;
            }
        }

        return byteArray;
    }

    /**
     * Converts the integer to HEX string.
     *
     * @param i
     *            the integer.
     * @return the HEX string.
     */
    private String toHexString(int i) {

        String hexString = Integer.toHexString(i);
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString;
        }

        return hexString.toUpperCase();
    }

    /**
     * Converts the byte array to HEX string.
     *
     * @param buffer
     *            the buffer.
     * @return the HEX string.
     */
    private String toHexString(byte[] buffer) {

        String bufferString = "";

        for (int i = 0; i < buffer.length; i++) {

            String hexChar = Integer.toHexString(buffer[i] & 0xFF);
            if (hexChar.length() == 1) {
                hexChar = "0" + hexChar;
            }

            bufferString += hexChar.toUpperCase() + " ";
        }

        return bufferString;
    }
}
