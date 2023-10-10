package com.test.android.androidbletest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BleTools {

    static final String ClientCharacteristicConfig = "00002901-0000-1000-8000-xxxxxxxxxxxx";
    static final String ServiceId = "0000fff0-0000-1000-8000-xxxxxxxxxxxx";
    static final String NotifyCharacteristicId = "0000fff6-0000-1000-8000-xxxxxxxxxxxx";
    static final String WriteCharacteristicId = "0000fff6-0000-1000-8000-xxxxxxxxxxxx";

    /**
     * 查找蓝牙设备
     * @param context
     * @param name
     * @param timeoutMillis 超时时间
     * @param callback
     */
    @SuppressLint("MissingPermission")
    public static void scanDevice(Activity context, String name, long timeoutMillis, ScanCallback callback){
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        Log.i("TAG", "蓝牙 bluetoothManager=:"+bluetoothManager);
        if (bluetoothManager == null) {
            callback.onScanFailed(ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED);
            return;
        }
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setDeviceName(name)
                .build();
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //高功耗模式，扫描速度快
                .build();

        BluetoothLeScanner scanner = bluetoothManager.getAdapter().getBluetoothLeScanner();

        Timer timer = new Timer();

        ScanCallback innerCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if(result != null){
                    timer.cancel();
                    scanner.stopScan(this);
                    callback.onScanResult(callbackType, result);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                timer.cancel();
                scanner.stopScan(this);
                callback.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            }
        };

        scanner.startScan(Collections.singletonList(scanFilter), scanSettings, innerCallback);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                scanner.stopScan(innerCallback);
                callback.onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR);
            }
        }, timeoutMillis);
    }

    @SuppressLint("MissingPermission")
    public static void disconnectDevice(BluetoothGatt gatt){
        gatt.close();
    }

    @SuppressLint("MissingPermission")
    public static void writeData(BluetoothGatt gatt, byte[] data){
        BluetoothGattService service = gatt.getService(UUID.fromString(ServiceId));
        BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(UUID.fromString(WriteCharacteristicId));
        writeCharacteristic.setValue(data);
        gatt.writeCharacteristic(writeCharacteristic);
    }

    /**
     * 链接蓝牙设备
     */
    public static BluetoothGatt connectDevice(Context context, BluetoothDevice device){
        @SuppressLint("MissingPermission") BluetoothGatt gatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            }

            @Override
            public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                super.onPhyRead(gatt, txPhy, rxPhy, status);
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onConnectionStateChange() 蓝牙链接状态改变");
                if (status == BluetoothGatt.GATT_SUCCESS){
                    Log.i("TAG", "蓝牙链接状态改变 status=BluetoothGatt.GATT_SUCCESS");
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i("TAG", "蓝牙链接状态改变 newState=BluetoothProfile.STATE_CONNECTED");
                        //开始搜索服务，搜索到服务并且使能特征值，才算是链接成功
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        //结束链接，否则会收到多条消息(蓝牙会自动重连)
                        disconnectDevice(gatt);
                        Log.i("TAG", "蓝牙链接状态改变 newState=BluetoothProfile.STATE_DISCONNECTED");
                    }
                }else if(status == BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION){
                    Log.i("TAG", "蓝牙链接状态改变 GATT 授权不足");
                    disconnectDevice(gatt);
                }else if(status == BluetoothGatt.GATT_READ_NOT_PERMITTED){
                    Log.i("TAG", "蓝牙链接状态改变 GAT读未授权");
                    disconnectDevice(gatt);
                }else if(status == BluetoothGatt.GATT_FAILURE){
                    Log.i("TAG", "蓝牙链接状态改变 连接失败");
                    disconnectDevice(gatt);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onServicesDiscovered()");
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    BluetoothGattService service = null;

                    BluetoothGattCharacteristic notifyCharacteristic = null;
                    BluetoothGattCharacteristic writeCharacteristic = null;

                    service = gatt.getService(UUID.fromString(ServiceId));
                    if (service!=null) {
                        Log.i("TAG", "找到了蓝牙Service:"+ ServiceId);
                        //找到服务，继续查找特征值
                        notifyCharacteristic = service.getCharacteristic(UUID.fromString(NotifyCharacteristicId));
                        writeCharacteristic = service.getCharacteristic(UUID.fromString(WriteCharacteristicId));
                    }

                    if (notifyCharacteristic != null) {
                        Log.i("TAG", "蓝牙notifyCharacteristic找到"+notifyCharacteristic.getUuid());
                        //使能Notify
                        gatt.setCharacteristicNotification(notifyCharacteristic, true);
                        // This is specific to BLE SPP Notify.
                        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                            BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(
                                    UUID.fromString(ClientCharacteristicConfig));
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }

                    if(writeCharacteristic != null) {
                        Log.i("TAG", "蓝牙writeCharacteristic找到"+writeCharacteristic.getUuid());
                    }else{
                        Log.i("TAG", "蓝牙writeCharacteristic未找到 通知并关闭设备");
                    }
                    if(notifyCharacteristic == null){
                        Log.i("TAG", "蓝牙notifyCharacteristic未找到 通知并关闭设备");
                    }

                    if(service == null || notifyCharacteristic == null || writeCharacteristic == null){
                        Log.i("TAG", "蓝牙Service不存在 连接失败 通知并关闭设备");
                        gatt.close();
                    }
                } else {
                    Log.i("TAG", "蓝牙Service发现未成功:"+status);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onCharacteristicRead()");
            }

            @Override
            public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
                super.onCharacteristicRead(gatt, characteristic, value, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onCharacteristicRead()");
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onCharacteristicWrite()");
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onCharacteristicChanged()");
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0)
                {
                    Log.i("TAG", "原始蓝牙消息:"+gatt.getDevice().getName()+" ->"+ Utils.bytesToHexString(data));
                }
            }

            @Override
            public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
                super.onCharacteristicChanged(gatt, characteristic, value);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onCharacteristicChanged()");
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onDescriptorRead()");
            }

            @Override
            public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
                super.onDescriptorRead(gatt, descriptor, status, value);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onDescriptorRead()");
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onDescriptorWrite()");
            }

            @Override
            public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                super.onReliableWriteCompleted(gatt, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onReliableWriteCompleted()");
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onReadRemoteRssi()");
            }

            @Override
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onMtuChanged()");
            }

            @Override
            public void onServiceChanged(@NonNull BluetoothGatt gatt) {
                super.onServiceChanged(gatt);
                Log.i("TAG", "蓝牙 BluetoothGattCallback.onServiceChanged()");
            }
        });
        return gatt;
    }

    /**
     * 获取蓝牙需要的权限列表
     * @return
     */
    public static List<String> getPermissions(){
        List<String> permissions = new ArrayList<>();

        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.R){
            permissions.add(android.Manifest.permission.BLUETOOTH);
            permissions.add(android.Manifest.permission.BLUETOOTH_ADMIN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);

        return permissions;
    }

    /**
     * 检查蓝牙权限
     * @param activity
     * @param requestPermissions
     * @return
     */
    public static boolean checkPermission(Activity activity, boolean requestPermissions){
        boolean hasBlePermission = true;

        List<String> permissions = getPermissions();
        Log.i("TAG", "蓝牙权限数量:"+permissions.size());

        for(String p : permissions){
            int res = ActivityCompat.checkSelfPermission(activity, p);
            if (res != PackageManager.PERMISSION_GRANTED) {
                hasBlePermission = false;
            }
        }
        Log.i("TAG", "是否满足蓝牙权限:"+hasBlePermission);
        if(!hasBlePermission && requestPermissions){
            String[] permissionsArray = permissions.toArray(new String[permissions.size()]);
            activity.requestPermissions(permissionsArray, 500);
        }
        return hasBlePermission;
    }
}
