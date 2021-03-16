package com.example.rc_controller_mvvm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;

public class BLEManager {
    public enum ConnectionStatus{
        connected,
        disconnected,
        connecting
    }
    public interface OnBLEEventListener{
        public void onDeviceFound(BluetoothDevice device);
        public void onConnectionStatusChanged(ConnectionStatus status);
        public void onDataNotified(byte[] dataArray);
    }

    private final Handler handler;
    private BluetoothAdapter adapter;
    private final ArrayList<OnBLEEventListener> listeners;

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED){
                gatt.discoverServices();
                for (OnBLEEventListener listener:listeners){
                    listener.onConnectionStatusChanged(ConnectionStatus.connected);
                }
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                for (OnBLEEventListener listener:listeners){
                    listener.onConnectionStatusChanged(ConnectionStatus.disconnected);
                }
            }else if(newState == BluetoothProfile.STATE_CONNECTING){
                gatt.discoverServices();
                for (OnBLEEventListener listener:listeners){
                    listener.onConnectionStatusChanged(ConnectionStatus.connecting);
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
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

    public BLEManager(){
        listeners = new ArrayList<>();
        handler = new Handler(Looper.myLooper());
    }
    private void checkAdapter(){
        if(adapter == null){
            Log.w("BLEManager","BluetoothAdapter is not attached. No Bluetooth events occur.");
            throw new NullPointerException("adapter is null");
        }

    }

    public void setAdapter(BluetoothAdapter adapter) {
        this.adapter = adapter;
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
        checkAdapter();
        if(!adapter.isDiscovering()){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            },10000);

            adapter.getBluetoothLeScanner().startScan(scanCallback);
        }
    }

    public void stopScan(){
        checkAdapter();
        if(!adapter.isDiscovering()){
            adapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }

    public void connect(String macAddress){

    }
}
