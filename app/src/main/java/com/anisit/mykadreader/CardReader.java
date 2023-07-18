package com.anisit.mykadreader;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Spinner;

import com.acs.smartcard.Reader;
import com.acs.smartcard.Reader.OnStateChangeListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CardReader{

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static int DeviceHasPermission;
    private static String TAG;
    private static Context _context;
    public static boolean acrDeviceSupported;
    private static boolean autoDetect;
    private static boolean breakAutodetect;
    public static boolean isConnectedToReader;
    private static final String[] powerActionStrings;
    private static final String[] stateStrings;
    private int _deviceSlotNum;
    private Spinner mSlotSpinner;
    private OnStateChangeListener cardPollListener;
    private DateFormat dateFormat;
    private Handler detecthandler;
    private Handler logsHandler;
    private UsbManager mManager;
    private PendingIntent mPermissionIntent;
//    private Reader mReader;
    private BroadcastReceiver mReceiver;
    private int mThemeId;
    private Handler pollhandler;
    public int protocol;
    public String[] readerDeviceList;
    public MyKad myKad;
    public MyKad_Data myKad_data;
    private MyKad_JPN myKad_jpn;

    //for JSON Object as a return obj from read
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

    public CardReader(Context context, int i) {
        Log.d(TAG,"Card Reader object is instanticated ..");
        this._deviceSlotNum = 0;
        this.mThemeId = -1;
        this.protocol = -1;
        this.dateFormat = new SimpleDateFormat("[dd-MM-yy HH:mm:ss]: ");
//        Log.d(TAG,this.mReceiver.toString() + " tttt");
        Log.d(TAG,"cubaa tttt");
        _context = context;
        this.mThemeId = i;
        this.pollhandler = new Handler();
        this.detecthandler = new Handler();
        this.logsHandler = new Handler();
    }

    class LogHandler implements Runnable {
        private final /* synthetic */ TransmitProgress val$progress;

        LogHandler(TransmitProgress transmitProgress) {
            this.val$progress = transmitProgress;
        }

        public void run() {
            logMsg("Slot " + CardReader.this._deviceSlotNum + ": Transmitting Control...");
            logMsg("Command:");
            logBuffer(this.val$progress.command, this.val$progress.commandLength);
            logMsg("Response:");
            logBuffer(this.val$progress.response, this.val$progress.responseLength);
        }
    }

    private class AutoDetectTask extends AsyncTask<Void, Void, Void> {
        private AutoDetectTask() {
        }

        protected Void doInBackground(Void... voidArr) {
            while (!CardReader.isConnectedToReader) {
                Log.d(TAG,"Looping Auto detection -----------------??");
                CardReader.breakAutodetect = false;
                CardReader.this.detectUsbDevice(false);
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CardReader.autoDetect = true;
                Log.d(TAG, "Tracing break auto detect ----> " + !CardReader.breakAutodetect);
                //if (!CardReader.isConnectedToReader || !CardReader.breakAutodetect) {
                //    //check if the reader was open
                //   CardReader.this.closeReader();
                //   logMsg("Card reader is closed");
                //}
            }
            return null;
        }
    }

    private class CloseTask extends AsyncTask<Void, Void, Void> {
        private CloseTask() {
        }

        protected Void doInBackground(Void... voidArr) {
            CardReader.this.closeReader();
            return null;
        }

        protected void onPostExecute(Void voidR) {

        }
    }

    public class ConnectProgress {
        public byte[] atr;
        public int atrLength;
        public Exception e;
    }

    private class OpenTask extends AsyncTask<UsbDevice, Void, Exception> {
        private OpenTask() {
        }

        @Override
        protected Exception doInBackground(UsbDevice... params) {

            Exception result = null;

            try {

                MainActivity.mReader.open(params[0]);

            } catch (Exception e) {

                result = e;
            }

            return result;
        }

        protected void onPostExecute(Exception exception) {
            if (exception != null) {
                Log.e(TAG,"Error open usb device "+ exception.toString());
                CardReader.breakAutodetect = true;
                CardReader.autoDetect = false;
                CardReader.DeviceHasPermission = -1;

                return;
            }
            logMsg(" Post Open Task ....");
            String readerName = MainActivity.mReader.getReaderName();
            logMsg("reader name is " + readerName);
            if (readerName == null) {
                return;
            }
            if (readerName.contains("ACS CCID USB Reader")) {

                CardReader.isConnectedToReader = true;
                //CardReader.this.readCard();

                return;
            }

            MainActivity.mReader.close();
            CardReader.breakAutodetect = true;
            CardReader.acrDeviceSupported = false;
            logMsg("Unsupported device.");
        }
    }

    public class TransmitProgress {
        public byte[] command;
        public int commandLength;
        public Exception e;
        public byte[] response;
        public int responseLength;
    }

    static {
        TAG = "ACS_Reader_Logs";
        powerActionStrings = new String[]{"Power Down", "Cold Reset", "Warm Reset"};
        stateStrings = new String[]{"Unknown", "Absent", "Present", "Swallowed", "Powered", "Negotiable", "Specific"};
        isConnectedToReader = false;
        DeviceHasPermission = 0;
        acrDeviceSupported = true;
        breakAutodetect = false;
        autoDetect = false;
    }


    //change for a while to public
    public void logBuffer(byte[] bArr, int i) {
        //logMsg("original byte is ---> " + bArr);
        String obj = "";
        for (int i2 = 0; i2 < i; i2++) {
            String toHexString = Integer.toHexString(bArr[i2] & 255);
            if (toHexString.length() == 1) {
                toHexString = "0" + toHexString;
            }
            if (i2 % 32 == 0 && obj != "") {
                logMsg(obj);
                obj = "";
            }
            obj = new StringBuilder(String.valueOf(obj)).append(toHexString.toUpperCase()).append(" ").toString();
        }
        if (obj != "") {
            logMsg(obj);
        }
    }

    private void logMsg(String str) {
        String stringBuilder = new StringBuilder(String.valueOf(this.dateFormat.format(new Date()))).append(str).toString();
        Log.d(TAG, str);
    }

    public void autoDetectUsbDevice() {
        new AutoDetectTask().execute(new Void[0]);
    }

    public void closeReader() {
        breakAutodetect = true;
        try {
            if (MainActivity.mReader != null) {
                MainActivity.mReader.setOnStateChangeListener(null);
                MainActivity.mReader.close();
                Log.e(TAG,"closed reader !!!" );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in closing reader");
        }
    }

    public ConnectProgress connectToCard() {
        if (this._deviceSlotNum == -1) {
            return null;
        }
        logMsg("Connecting Card");
        logMsg("Slot " + this._deviceSlotNum + ": " + powerActionStrings[2] + "...");
        ConnectProgress connectProgress = new ConnectProgress();
        try {
            byte[] power = MainActivity.mReader.power(this._deviceSlotNum, 2);
//            logMsg("ATR:");
            logBuffer(power, power.length);
            connectProgress.atr = power;
            connectProgress.atrLength = power.length;
            connectProgress.e = null;
            logMsg("atr " + connectProgress.atr + "length " + connectProgress.atrLength);
            Log.d(TAG,"atr " + connectProgress.atr + "length " + connectProgress.atrLength + " tttt");
            return connectProgress;
        } catch (Exception e) {
            logMsg(e.toString());
//            connectProgress.atr = null;
//            connectProgress.atrLength = 0;
//            connectProgress.e = e;
            return connectProgress;
        }
    }

    public void detectUsbDevice(boolean z) {
        Log.d(TAG,"siniiii");
        if(MainActivity.mReader == null){
            this.mManager = (UsbManager) _context.getSystemService(_context.USB_SERVICE);
            MainActivity.mReader = new Reader(this.mManager);
            // Initialize reader
            MainActivity.mReader = new Reader(mManager);
            MainActivity.mReader.setOnStateChangeListener(new OnStateChangeListener() {

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

                    // Show output
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            logMsg(outputString);
                        }
                    });
                }
            });
        }

        isConnectedToReader = false;
        registerUsbReceivers();
        listReaders();
        openReader();
        autoDetect = false;
        if (z) {
            this.detecthandler.postDelayed(new Runnable() {
                public void run() {
                    CardReader.autoDetect = true;
                    if (!CardReader.isConnectedToReader && !CardReader.breakAutodetect) {
                        CardReader.this.closeReader();
                        CardReader.this.autoDetectUsbDevice();
                    }
                }
            }, 3000);
        }
    }

    private void runOnUiThread(Runnable runnable) {
    }

    public int getProtocol() {
        try {
            String str;
            int protocol = MainActivity.mReader.getProtocol(this._deviceSlotNum);
            switch (protocol) {
                case Reader.PROTOCOL_T0 /*1*/:
                    str = "Active Protocol: " + "T=0";
                    break;
                case Reader.PROTOCOL_T1 /*2*/:
                    str = "Active Protocol: " + "T=1";
                    break;
                default:
                    str = "Active Protocol: " + "Unknown";
                    break;
            }
            logMsg(str);
            return protocol;
        } catch (IllegalArgumentException e) {
            IllegalArgumentException illegalArgumentException = e;
            int i = -1;
//            logMsg(illegalArgumentException.toString());
            return i;
        }
    }

    public String getReaderName() {
        return !isConnectedToReader ? "" : MainActivity.mReader.getReaderName();
    }

    public int getState() {
        // Get slot number
        this._deviceSlotNum = 0;

        // If slot is selected
//        if (slotNum != Spinner.INVALID_POSITION) {

            try {

        // Get state
        logMsg("Slot " + this._deviceSlotNum + ": Getting state...");
        if(MainActivity.mReader != null){
            int state = MainActivity.mReader.getState(this._deviceSlotNum);

            Log.e(TAG,"Setate: " + state);

            if (state < Reader.CARD_UNKNOWN
                    || state > Reader.CARD_SPECIFIC) {
                state = Reader.CARD_UNKNOWN;
            }

            logMsg("State: " + stateStrings[state]);
            Log.e(TAG,"Setate: " + stateStrings[state]);
            return state;
        }
        return -2;


            } catch (IllegalArgumentException e) {

//                logMsg(e.toString());
                return -1;
            }
//        }else{
//            return -2;
//        }
    }

    public void listReaders() {
        int i = 0;
        int i2 = 0;
        for (UsbDevice usbDevice : this.mManager.getDeviceList().values()) {
            if (MainActivity.mReader.isSupported(usbDevice)) {
                logMsg(usbDevice.getDeviceName() + " Slot No= " + i2);
                i2++;
            }
        }
        if (i2 != 0) {
            this.readerDeviceList = new String[i2];
            for (UsbDevice usbDevice2 : this.mManager.getDeviceList().values()) {
                if (MainActivity.mReader.isSupported(usbDevice2)) {
                    this.readerDeviceList[i] = usbDevice2.getDeviceName();
                    i++;

                }
            }
        }
    }

    public boolean openReader() {
        if (this.readerDeviceList == null) {
            return false;
        }

        if (this.readerDeviceList != null) {
            String str = this.readerDeviceList[0];
            Log.e(TAG, "Trying to open reader " + str);
            for (UsbDevice usbDevice : this.mManager.getDeviceList().values()) {
                if (str.equals(usbDevice.getDeviceName())) {
                    this.mManager.requestPermission(usbDevice, this.mPermissionIntent);
                    Log.e(TAG, "open reader successfully --> " + str);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean openReader(String str) {
        if (this.readerDeviceList == null) {
            return false;
        }

        if (str != null) {
            for (UsbDevice usbDevice : this.mManager.getDeviceList().values()) {
                if (str.equals(usbDevice.getDeviceName())) {
                    this.mManager.requestPermission(usbDevice, this.mPermissionIntent);
                    return true;
                }
            }
        }
        return false;
    }

    public void registerUsbReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.mPermissionIntent = PendingIntent.getBroadcast(_context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        }else {
            this.mPermissionIntent = PendingIntent.getBroadcast(_context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
//        this.mPermissionIntent = PendingIntent.getActivity(_context, 0, new Intent(
//                ACTION_USB_PERMISSION),PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_USB_PERMISSION);
        intentFilter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        if (_context != null) {
            _context.registerReceiver(this.mReceiver, intentFilter);
            logMsg("Register new USB receivers.");
        }
    }

    public TransmitProgress sendApdu(byte[] bArr) {
        if (this._deviceSlotNum == -1) {
            return null;
        }
        byte[] bArr2 = new byte[300];
        TransmitProgress transmitProgress = new TransmitProgress();
        try {
            int transmit = MainActivity.mReader.transmit(MainActivity._deviceSlotNum, bArr, bArr.length, bArr2, bArr2.length);
            transmitProgress.command = bArr;
            transmitProgress.commandLength = bArr.length;
            transmitProgress.response = bArr2;
            transmitProgress.responseLength = transmit;
            transmitProgress.e = null;
            this.logsHandler.post(new LogHandler(transmitProgress));
            Log.d(TAG, "1 - transmitProgress " + transmitProgress);
            return transmitProgress;
        } catch (Exception e) {
            transmitProgress.command = null;
            transmitProgress.commandLength = 0;
            transmitProgress.response = null;
            transmitProgress.responseLength = 0;
            transmitProgress.e = e;

            Log.d(TAG, "1 - transmitProgress " + transmitProgress);
            return transmitProgress;
        }
    }

    public TransmitProgress sendControl(byte[] bArr, int i) {
        if (this._deviceSlotNum == -1) {
            return null;
        }
        byte[] bArr2 = new byte[300];
        TransmitProgress transmitProgress = new TransmitProgress();
        try {
            int control = MainActivity.mReader.control(this._deviceSlotNum, i, bArr, bArr.length, bArr2, bArr2.length);
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

    public void setProtocol() {
        if (this._deviceSlotNum != -1) {
            Object obj = "T=0";
            if ("T=0" != "") {
                obj = "T=0" + "/";
            }
            String stringBuilder = new StringBuilder(String.valueOf(obj)).append("T=1").toString();
            try {
                MainActivity.mReader.setProtocol(this._deviceSlotNum, 3);
            } catch (Exception e) {
                logMsg(e.toString());
            }
            logMsg("Slot " + this._deviceSlotNum + ": Setting protocol to " + stringBuilder + "...");
        }
    }

    public void setProtocol(int i) {
        if (this._deviceSlotNum != -1) {
            try {
                MainActivity.mReader.setProtocol(this._deviceSlotNum, i);
            } catch (Exception e) {
                logMsg(e.toString());
            }
            logMsg("Slot " + this._deviceSlotNum + ": Setting protocol to " + i + "...");
        }
    }

    public void unpowerCard() {
        if (this._deviceSlotNum != -1 && this._deviceSlotNum != -1) {
            logMsg("Disconnecting Card");
            logMsg("Slot " + this._deviceSlotNum + ": " + powerActionStrings[0] + "...");
            try {
                byte[] power = MainActivity.mReader.power(this._deviceSlotNum, 0);
                logMsg("ATR:");
                logBuffer(power, power.length);
            } catch (Exception e) {
                logMsg(e.toString());
            }
        }
    }

    private boolean checkCard(CardReader.ConnectProgress connectProgress) {
        try {
            if (CardReader.isConnectedToReader) {
                if (myKad.checkCard(connectProgress)) {
                    //message += "check card true";
                    return true;
                }
                //message += "check card false due to " + connectProgress.atr;
                return false;
            }
            throw new Exception("Please connect reader");
        } catch (Exception e) {
            return false;
        }
    }

}
