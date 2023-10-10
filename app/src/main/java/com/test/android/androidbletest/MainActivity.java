package com.test.android.androidbletest;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();

    ScanResult scanResult;
    BluetoothGatt gatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "蓝牙 checkPermission");
                if(BleTools.checkPermission(MainActivity.this, true)){
                    Log.i(TAG, "蓝牙 checkPermission成功..");
                    //查找设备
                    BleTools.scanDevice(MainActivity.this, "deviceName", 10000, new ScanCallback() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            scanResult = result;
                            Log.i(TAG, "蓝牙搜索结果: callbackType="+callbackType+" result"+result.getDevice().getName());
                        }

                        @Override
                        public void onScanFailed(int errorCode) {
                            Log.i(TAG, "蓝牙搜索失败: errorCode="+errorCode);
                        }
                    });
                }else{
                    Log.i(TAG, "蓝牙 checkPermission失败..");
                }
            }
        });

        findViewById(R.id.btn_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(scanResult != null){
                    gatt = BleTools.connectDevice(MainActivity.this, scanResult.getDevice());
                }
            }
        });

        findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if(gatt != null){
                    BleTools.disconnectDevice(gatt);
                }
            }
        });

        findViewById(R.id.btn_send_data).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if(gatt != null){
                    BleTools.writeData(gatt, Utils.hexStringToByteArray("abcdefg"));
                }
            }
        });
    }
}