package com.example.rc_controller_mvvm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

public class BLEManager {
    public enum ConnectionStatus{
        connected,
        disconnected,
        connecting
    }
    public interface OnBLEEventListener{
        void onDeviceFound(BluetoothDevice device);
        void onConnectionStatusChanged(ConnectionStatus status);
        void onDataNotified(final byte[] dataArray);
    }

    private final Handler handler;
    private final Context context;
    private final BluetoothAdapter adapter;
    private final ArrayList<OnBLEEventListener> listeners;

    private BluetoothGatt gatt;
    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState){
                case BluetoothGatt.STATE_CONNECTED:
                    gatt.discoverServices();
                    for (OnBLEEventListener listener:listeners){
                        listener.onConnectionStatusChanged(ConnectionStatus.connected);
                    }
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                case BluetoothGatt.STATE_DISCONNECTING:
                    for (OnBLEEventListener listener:listeners){
                        listener.onConnectionStatusChanged(ConnectionStatus.connecting);
                    }
                    break;
                default:
                    for (OnBLEEventListener listener:listeners){
                        listener.onConnectionStatusChanged(ConnectionStatus.disconnected);
                    }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] notified = characteristic.getValue();
            for(OnBLEEventListener listener:listeners){
                listener.onDataNotified(notified);
            }
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            for(OnBLEEventListener listener:listeners){
                listener.onDeviceFound(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };


    public BLEManager(@NonNull BluetoothAdapter adapter, Context context){
        this.adapter = adapter;
        listeners = new ArrayList<>();
        handler = new Handler(Looper.myLooper());
        this.context = context;
    }

    public void addEventListener(OnBLEEventListener listener){
        if(!listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    public void removeEventListener(OnBLEEventListener listener){
        if(!listeners.contains(listener)){
            listeners.remove(listener);
        }
    }

    public void startScan(){
        if(!adapter.isDiscovering()){
            handler.postDelayed(this::stopScan,10000);

            adapter.getBluetoothLeScanner().startScan(scanCallback);
        }
    }

    public void stopScan(){
        if(!adapter.isDiscovering()){
            adapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    public void connect(String macAddress){
        gatt = adapter.getRemoteDevice(macAddress).connectGatt(context,false,gattCallback);
        if(gatt == null){
            Log.w("BLEManager","connection failed");
        }

    }

    public void disconnect(){
        if(gatt != null){
            gatt.disconnect();
            gatt = null;
        }else {
            Log.i("BLEManager","request disconnect without connection");
        }
    }

    public void writeCharacteristic(UUID service,UUID characteristic,byte[] data){
        if(gatt == null){
            Log.w("BLEManager","request write without connection");
            return;
        }
        BluetoothGattCharacteristic remoteChara = gatt.getService(service).getCharacteristic(characteristic);
        remoteChara.setValue(data);
        gatt.writeCharacteristic(remoteChara);
    }

    public void changeNotifyEnable(UUID service,UUID characteristic,boolean enable){
        if(gatt == null){
            Log.w("BLEManager","request write without connection");
            return;
        }

        BluetoothGattCharacteristic remoteChara = gatt.getService(service).getCharacteristic(characteristic);
        BluetoothGattDescriptor remoteDesc = remoteChara.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if(enable){
            remoteDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        }else {
            remoteDesc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        gatt.writeDescriptor(remoteDesc);
        gatt.writeCharacteristic(remoteChara);
    }
}
