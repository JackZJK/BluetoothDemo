package com.sudiyi.apps.ble;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sudiyi.apps.ble.utils.LogUtils;
import com.sudiyi.apps.ble.utils.Md5DataUtil;
import com.sudiyi.apps.ble.utils.RssiUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ClientActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "BLE";
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    public final static int SCAN_PERIOD = 10000;

    private boolean isScanning;

    private Button btnScan;
    private Button btnStop;
    private Button btnConnect;
    private Button btnSend;
    private EditText etInput;
    private TextView tvClient;
    private Spinner spnCentralList;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private List<BluetoothDevice> mDeviceList;
    private ArrayList<String> mDeviceNameList;
    private static ArrayAdapter<String> centralAdapter;

    private Handler mHandler;

    private BluetoothGatt mBtGatt = null;
    private BluetoothGattService mGattService = null;
    BluetoothGattCharacteristic mGattCharacteristic = null;

    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private int mRssi;
    private ScheduledExecutorService mScheduler;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        initView();

        isScanning = false;
        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mDeviceNameList = new ArrayList<>();
        centralAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mDeviceNameList);
        centralAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCentralList.setAdapter(centralAdapter);
        mDeviceList = new ArrayList<>();

        mHandler = new Handler();
        mScheduler = Executors.newSingleThreadScheduledExecutor();

    }

    private void initView() {
        btnScan = (Button) findViewById(R.id.buttonScan);
        btnStop = (Button) findViewById(R.id.buttonStopScan);
        //btnSend = (Button) findViewById(R.id.buttonSendClient);
        btnConnect = (Button) findViewById(R.id.buttonConnect);
        spnCentralList = (Spinner) findViewById(R.id.spinnerCentralList);
        etInput = (EditText) findViewById(R.id.editTextInputClient);
        tvClient = (TextView) findViewById(R.id.textViewClient);

        findViewById(R.id.btn_open0).setOnClickListener(this);
        findViewById(R.id.btn_open1).setOnClickListener(this);
        findViewById(R.id.btn_open2).setOnClickListener(this);
        findViewById(R.id.btn_open3).setOnClickListener(this);
        btnConnect.setEnabled(false);
        etInput.setText(ServerActivity.TEST_DEVICE_ID);
    }

    private void showToast(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ClientActivity.this, string, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void getDistance() {
        mScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (canGetRssi()) {
                    if (mConnectionState == BluetoothProfile.STATE_CONNECTED) {
                        showToast("当前信号强度：" + mRssi);
                    }
                }
            }
        }, 3000, 300, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onStop() {
        if (mDeviceNameList != null) {
            mDeviceNameList.clear();
            mDeviceNameList = null;
        }
        if (mBtGatt != null)
            mBtGatt.close();
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);

        if (mDeviceNameList != null) {
            mDeviceNameList.clear();
            mDeviceNameList = null;
        }
        if (mBtGatt != null)
            mBtGatt.close();
        super.onDestroy();
    }

    public void handleScanStart(View view) {
        mDeviceNameList.clear();
        mDeviceList.clear();
        btnConnect.setEnabled(false);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                centralAdapter.clear();
                centralAdapter.notifyDataSetChanged();
            }
        });

        startBleScan();
    }

    public void handleScanStop(View view) {
        stopBleScan();
    }

    public void handleConnect(View view) {
        int choice = spnCentralList.getSelectedItemPosition();

        LogUtils.d("choosen: " + choice);
        LogUtils.d("devices size: " + mDeviceList.size());

        if (mDeviceList.size() > 0) {
            if (mBtGatt != null)
                mBtGatt.close();
            mBtGatt = mDeviceList.get(choice).connectGatt(getApplicationContext(), false, mGattCallback);
        } else
            LogUtils.d("No device to connect");
    }

    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        if (mBluetoothAdapter == null) {
            LogUtils.d("Bluetooth NOT supported");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    /**
     * BLE Scanning
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void startBleScan() {
        if (isScanning) return;
        enableBluetooth();
        isScanning = true;

        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @SuppressLint("NewApi")
            @Override
            public void run() {
                isScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                btnScan.setEnabled(true);
                btnStop.setEnabled(false);
            }
        }, SCAN_PERIOD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);
        } else {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }

        btnScan.setEnabled(false);
        btnStop.setEnabled(true);
        LogUtils.d("Bluetooth is currently scanning...");
    }

    public void stopBleScan() {
        if (!isScanning) return;
        isScanning = false;

        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
        btnScan.setEnabled(true);
        btnStop.setEnabled(false);
        LogUtils.d("Scanning has been stopped");
    }

    // Device scan callback for previous sdk version
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            LogUtils.d("scanRecord: " + scanRecord);

            double distance = RssiUtil.getValue(scanRecord, rssi);
            String deviceInfo = device.getName() + "-" + distance + "米";
            if (mDeviceNameList.contains(device.getAddress())) {
                return;
            }
            mDeviceNameList.add(deviceInfo);
            mDeviceList.add(device);

            LogUtils.d("Device: " + deviceInfo + " Scanned!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    centralAdapter.notifyDataSetChanged();
                }
            });
            btnConnect.setEnabled(true);
        }
    };
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();
            int txPowerLevel = result.getScanRecord().getTxPowerLevel();

            //double distance = RssiUtil.calculateAccuracy(txPowerLevel, result.getRssi());

            int len = result.getScanRecord().getBytes().length;
            String scanHex = RssiUtil.bytesToHex(result.getScanRecord().getBytes());

            LogUtils.d("DEBUG", "len: " + len + " data:" + scanHex + ";txPowerLevel:" + txPowerLevel);
            double distance = RssiUtil.calculateAccuracy(-58, result.getRssi());
            LogUtils.d("scanRecord: " + result.getScanRecord());
            String deviceInfo = device.getName() + "-" + distance + "米";
            if (mDeviceList == null) {
                return;
            }

            if (mDeviceList.contains(device)) {
                return;
            }

            mDeviceNameList.add(deviceInfo);
            mDeviceList.add(device);
            LogUtils.d("Device: " + deviceInfo + " Scanned!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    centralAdapter.notifyDataSetChanged();
                }
            });
            btnConnect.setEnabled(true);
        }
    };

    public boolean canGetRssi() {
        if (mBluetoothAdapter == null || mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
            return false;
        return mBtGatt.readRemoteRssi();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                getDistance();
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                LogUtils.i("Connected to GATT server.");
                showToast("Connected to GATT server");
                if (gatt != null) {
                    mBtGatt = gatt;
                    if (gatt.discoverServices()) {
                        LogUtils.d("Attempt to discover Service");
                    } else {
                        LogUtils.d("Failed to discover Service");
                    }
                } else {
                    LogUtils.d("btGatt == null");
                }

            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnectionState = BluetoothProfile.STATE_CONNECTING;
                showToast("Connecting GATT server");
                LogUtils.i("Connecting GATT server.");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                LogUtils.i("Disconnected from GATT server.");
                mBtGatt.close();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
                LogUtils.i("Disconnecting from GATT server.");
            }
        }


        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            showToast("当前信号强度：" + mRssi);
            mRssi = rssi;
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogUtils.d("Services Discovered successfully : " + status);

                List<BluetoothGattService> gattServices = gatt.getServices();
                mBtGatt = gatt;
                if (gattServices.size() > 0) {
                    LogUtils.d("Found : " + gattServices.size() + " services");
                    for (BluetoothGattService bluetoothGattService : gattServices)
                        LogUtils.d("UUID = " + bluetoothGattService.getUuid().toString());

                    BluetoothGattService gattServ = gatt.getService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1));
                    if (gattServ != null) {
                        mGattService = gattServ;
                        BluetoothGattCharacteristic gattChar = gattServ.getCharacteristic(UUID.fromString(BluetoothUtility.CHAR_UUID_1));
                        mGattCharacteristic = gattChar;
                        gatt.readCharacteristic(gattChar);
                    } else
                        LogUtils.d("gattServ == null");
                } else
                    LogUtils.d("gattServices.size() == 0");
            } else {
                LogUtils.e("onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                LogUtils.d("Characteristic is read");
                gatt.setCharacteristicNotification(characteristic, true);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data != null) {
                final String tmp = BluetoothUtility.byteArraytoString(data);
                LogUtils.d("Changed data : " + tmp);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvClient.setText(tmp);
                    }
                });
            } else
                LogUtils.d("Changed Data is null");
        }
    };

    @Override
    public void onClick(View v) {
        String id;
        String openWay = "";
        if (TextUtils.isEmpty(etInput.getText().toString())) {
            showToast("请输入id");
            return;
        }
        switch (v.getId()) {
            case R.id.btn_open0:
                openWay = ControlCommand.Open0.getValue();
                break;
            case R.id.btn_open1:
                openWay = ControlCommand.Open1.getValue();

                break;
            case R.id.btn_open2:
                openWay = ControlCommand.Open2.getValue();

                break;
            case R.id.btn_open3:
                openWay = ControlCommand.Open3.getValue();
                break;

        }

        id = etInput.getText().toString();
        String md5Data = Md5DataUtil.getMd5Data(id, openWay);
        if (mBtGatt != null) {
            if (mGattCharacteristic == null) {
                return;
            }
            mGattCharacteristic.setValue(BluetoothUtility.stringToByte(md5Data));
            if (mBtGatt.writeCharacteristic(mGattCharacteristic)) {
                showToast("Data sent");
                LogUtils.d("Data sent");
            } else {
                showToast("Data not sent");
                LogUtils.d("Data not sent");
            }
        }
    }
}
