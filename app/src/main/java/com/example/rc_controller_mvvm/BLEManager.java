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
        connecting,
        disconnecting
    }
    public interface OnDeviceFoundListener{
        void onDeviceFound(BluetoothDevice device);
        void onScanFinish();
    }
    public interface OnConnectionChangedLister{
        void onConnectionStatusChanged(ConnectionStatus status);
    }

    public interface OnDataNotifiedLister{
        void onDataNotified(final byte[] dataArray);
    }

    private final Handler handler;
    private final Context context;
    private final BluetoothAdapter adapter;
    private final ArrayList<OnConnectionChangedLister> connectionChangedListers;
    private final ArrayList<OnDataNotifiedLister> dataNotifiedListers;
    private final ArrayList<OnDeviceFoundListener> onDeviceFoundListeners;


    private BluetoothGatt gatt;
    public final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            ConnectionStatus statEnum;
            switch (newState){
                case BluetoothGatt.STATE_CONNECTED:
                    gatt.discoverServices();
                    statEnum = ConnectionStatus.connected;
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    statEnum = ConnectionStatus.connecting;
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    statEnum = ConnectionStatus.disconnecting;
                    break;
                default:
                    statEnum = ConnectionStatus.disconnected;
            }
            for (OnConnectionChangedLister listener:connectionChangedListers){
                listener.onConnectionStatusChanged(statEnum);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] notified = characteristic.getValue();
            for(OnDataNotifiedLister listener:dataNotifiedListers){
                listener.onDataNotified(notified);
            }
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            for(OnDeviceFoundListener listener:onDeviceFoundListeners){
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
        connectionChangedListers = new ArrayList<>();
        onDeviceFoundListeners = new ArrayList<>();
        dataNotifiedListers = new ArrayList<>();
        handler = new Handler(Looper.myLooper());
        this.context = context;
    }

    public void setConnectionChangedLister (OnConnectionChangedLister listener){
        if(!connectionChangedListers.contains(listener)){
            connectionChangedListers.add(listener);
        }
    }
    public void setDataNotifiedLister (OnDataNotifiedLister listener){
        if(!dataNotifiedListers.contains(listener)){
            dataNotifiedListers.add(listener);
        }
    }
    public void setDeviceFoundListeners (OnDeviceFoundListener listener){
        if(!onDeviceFoundListeners.contains(listener)){
            onDeviceFoundListeners.add(listener);
        }
    }

    public void resetConnectionChangedLister (OnConnectionChangedLister listener){
        if(!connectionChangedListers.contains(listener)){
            connectionChangedListers.remove(listener);
        }
    }
    public void resetDataNotifiedLister (OnDataNotifiedLister listener){
        if(!dataNotifiedListers.contains(listener)){
            dataNotifiedListers.remove(listener);
        }
    }
    public void resetDeviceFoundListeners (OnDeviceFoundListener listener){
        if(!onDeviceFoundListeners.contains(listener)){
            onDeviceFoundListeners.remove(listener);
        }
    }


    private boolean isScanning = false;
    public void startScan(){
        if(!adapter.isDiscovering()){
            handler.postDelayed(this::stopScan,10000);
            adapter.getBluetoothLeScanner().startScan(scanCallback);
            isScanning = true;
        }
    }

    public void stopScan(){
        if(!adapter.isDiscovering()){
            if(isScanning){
                isScanning = false;
                adapter.getBluetoothLeScanner().stopScan(scanCallback);
                for(OnDeviceFoundListener listener:onDeviceFoundListeners){
                    listener.onScanFinish();
                }
            }
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
