package com.sudiyi.apps.ble;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.sudiyi.apps.ble.utils.LogUtils;

public class MainActivity extends Activity implements OnClickListener {

    Button btnServer, btnClient;
    private static final String TAG = "BLE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnServer = (Button) findViewById(R.id.buttonServer);
        btnClient = (Button) findViewById(R.id.buttonClient);

        btnClient.setOnClickListener(this);
        btnServer.setOnClickListener(this);
        initPermission();//针对6.0以上版本做权限适配

        enableBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //检查权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    /**
     * 权限的结果回调函数
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean b = grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED;
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonServer:

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Toast.makeText(this, "Go get The Lollipop", Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, ServerActivity.class);
                    startActivity(intent);
                }

                break;

            case R.id.buttonClient:

                Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                startActivity(intent);

                break;

            default:
                break;
        }
    }

    //Check if bluetooth is enabled, if not, then request enable
    private void enableBluetooth() {
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            LogUtils.d("Bluetooth NOT supported");
        } else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }
}
