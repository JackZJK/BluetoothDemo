package com.sudiyi.apps.ble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sudiyi.apps.ble.utils.LogUtils;
import com.sudiyi.apps.ble.utils.Md5DataUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ServerActivity extends Activity {
    private Button btnStopAdv;
    private Button btnAdv;
    private Button btnSendData;
    private EditText etInput;
    private TextView tvServer;
    private TextView mAddress;

    private BluetoothGattServer mGattServer;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothDevice mConnectedDevice;

    private boolean isAdvertising;
    private boolean isDeviceSet = false;

    private ArrayList<BluetoothGattService> mAdvertisingServices;
    private List<ParcelUuid> mServiceUuids;
    private static final String DEVICE_NAME = "SUDIYI-XHT";//设备name
    private ControlCommand mControlCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        isAdvertising = false;
        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            LogUtils.e("can not support server");
            Toast.makeText(ServerActivity.this, "该设备不支持蓝牙低功耗通讯", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        mBluetoothAdapter.setName(DEVICE_NAME);
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            LogUtils.e("can not support server");
            Toast.makeText(ServerActivity.this, "该设备不支持蓝牙低功耗从设备通讯", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAdvertisingServices = new ArrayList<>();
        mServiceUuids = new ArrayList<>();
        btnAdv = (Button) findViewById(R.id.buttonAdvStart);
        btnStopAdv = (Button) findViewById(R.id.buttonAdvStop);
        btnSendData = (Button) findViewById(R.id.buttonSendServer);
        tvServer = (TextView) findViewById(R.id.textViewServer);
        mAddress = (TextView) findViewById(R.id.bluetooth_state_text);
        etInput = (EditText) findViewById(R.id.editTextInputServer);
        etInput.setText("Server");
        String address = mBluetoothAdapter.getAddress();
        mAddress.setText(address);

        //adding service and characteristics
        BluetoothGattService firstService = new BluetoothGattService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1), BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic firstServiceChar = new BluetoothGattCharacteristic(
                UUID.fromString(BluetoothUtility.CHAR_UUID_1),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        firstService.addCharacteristic(firstServiceChar);

        mAdvertisingServices.add(firstService);
        mServiceUuids.add(new ParcelUuid(firstService.getUuid()));

        initCommandList();

    }

    private Map mCommandList = new HashMap();
    public static final String TEST_DEVICE_ID = "111111111";

    /**
     * 初始化 指令加密集合
     */
    private void initCommandList() {
        String open0 = Md5DataUtil.getMd5Data(TEST_DEVICE_ID, ControlCommand.Open0.getValue());
        String open1 = Md5DataUtil.getMd5Data(TEST_DEVICE_ID, ControlCommand.Open1.getValue());
        String open2 = Md5DataUtil.getMd5Data(TEST_DEVICE_ID, ControlCommand.Open2.getValue());
        String open3 = Md5DataUtil.getMd5Data(TEST_DEVICE_ID, ControlCommand.Open3.getValue());
        mCommandList.put(open0, ControlCommand.Open0.getValue());
        mCommandList.put(open1, ControlCommand.Open1.getValue());
        mCommandList.put(open2, ControlCommand.Open2.getValue());
        mCommandList.put(open3, ControlCommand.Open3.getValue());
    }


    public void handleStartClick(View view) {
        startAdvertise();
        btnAdv.setEnabled(false);
        btnStopAdv.setEnabled(true);
    }

    public void handleStopClick(View view) {
        stopAdvertise();
        btnAdv.setEnabled(true);
        btnStopAdv.setEnabled(false);
    }

    public void handleSendClick(View view) {
        String inputString = etInput.getText().toString();
        sendData(inputString);
    }

    /**
     * 发送数据
     *
     * @param data
     */
    private void sendData(String data) {
        if (TextUtils.isEmpty(data)) {
            Toast.makeText(this, "录入信息不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        if (isDeviceSet && writeCharacteristicToGatt(data)) {
            Toast.makeText(ServerActivity.this, "Data written", Toast.LENGTH_SHORT).show();
            LogUtils.d("Data written from server");
        } else {
            Toast.makeText(ServerActivity.this, "Data not written", Toast.LENGTH_SHORT).show();
            LogUtils.d("Data not written");
        }
    }

    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        if (mBluetoothAdapter == null) {
            LogUtils.d("Bluetooth NOT supported");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
            return;
        }
    }

    private void startGattServer() {
        mGattServer = mBluetoothManager.openGattServer(getApplicationContext(), gattServerCallback);
        for (int i = 0; i < mAdvertisingServices.size(); i++) {
            mGattServer.addService(mAdvertisingServices.get(i));
            LogUtils.d("uuid" + mAdvertisingServices.get(i).getUuid());
        }
    }

    //Public method to begin advertising services
    public void startAdvertise() {
        if (isAdvertising) return;
        enableBluetooth();
        startGattServer();

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
        dataBuilder.setIncludeDeviceName(true);
        for (ParcelUuid serviceUuid : mServiceUuids)
            dataBuilder.addServiceUuid(serviceUuid);

        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
        isAdvertising = true;
    }

    //Stop ble advertising and clean up
    public void stopAdvertise() {
        if (!isAdvertising) return;
        mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        mGattServer.clearServices();
        mGattServer.close();
        mAdvertisingServices.clear();
        isAdvertising = false;
    }

    public boolean writeCharacteristicToGatt(String data) {
        final BluetoothGattService service = mGattServer.getService(UUID.fromString(BluetoothUtility.SERVICE_UUID_1));
        if (service == null) {
            return false;
        }
        final BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(BluetoothUtility.CHAR_UUID_1));

        if (mConnectedDevice != null && characteristic.setValue(data)) {
            mGattServer.notifyCharacteristicChanged(mConnectedDevice, characteristic, true);
            return true;
        } else
            return false;
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            String successMsg = "Advertisement command attempt successful";
            LogUtils.d(successMsg);
        }

        @Override
        public void onStartFailure(int i) {
            String failMsg = "Advertisement command attempt failed: " + i;
            LogUtils.e(failMsg);
        }
    };

    public BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            LogUtils.d("onConnectionStateChange status=" + status + "->" + newState);
            mConnectedDevice = device;
            isDeviceSet = true;
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            LogUtils.d("service added: " + status);
        }

        @Override
        public void onCharacteristicReadRequest(
                BluetoothDevice device,
                int requestId,
                int offset,
                BluetoothGattCharacteristic characteristic
        ) {
            LogUtils.d("onCharacteristicReadRequest requestId=" + requestId + " offset=" + offset);

            if (characteristic.getUuid().equals(UUID.fromString(BluetoothUtility.CHAR_UUID_1))) {
                characteristic.setValue("test");
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicWriteRequest(
                BluetoothDevice device,
                int requestId,
                BluetoothGattCharacteristic characteristic,
                boolean preparedWrite,
                boolean responseNeeded,
                int offset,
                byte[] value) {
            if (value != null) {
                LogUtils.d("Data written: " + BluetoothUtility.byteArraytoString(value));
                final String tmp = BluetoothUtility.byteArraytoString(value);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String receiveResult = (String) mCommandList.get(tmp);
                        if (receiveResult == null) {
                            receiveResult = "null";
                        }
                        mControlCommand = ControlCommand.vOf(receiveResult);
                        String result = "";
                        switch (mControlCommand) {
                            case Open0:
                                result = "open 0 ok";
                                break;
                            case Open1:
                                result = "open 1 ok";
                                break;
                            case Open2:
                                result = "open 2 ok";
                                break;
                            case Open3:
                                result = "open 3 ok";
                                break;
                            case Unknown:
                                result = "open fail err id";
                                break;
                        }
                        sendData(result);
                        tvServer.setText(result);
                        LogUtils.d("open result:" + result);
                    }
                });
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            } else
                LogUtils.d("value is null");
        }
    };

    @Override
    protected void onDestroy() {
        stopAdvertise();
        if (mServiceUuids != null) {
            mServiceUuids.clear();
            mServiceUuids = null;
        }
        if (mAdvertisingServices != null) {
            mAdvertisingServices.clear();
            mAdvertisingServices = null;
        }
        if (mCommandList != null) {
            mCommandList.clear();
            mCommandList = null;
        }
        if (mControlCommand != null) {
            mControlCommand = null;
        }
        super.onDestroy();
    }
}
