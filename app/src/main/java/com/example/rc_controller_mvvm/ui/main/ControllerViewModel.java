package com.example.rc_controller_mvvm.ui.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;

import androidx.databinding.BindingAdapter;
import androidx.databinding.BindingMethod;
import androidx.databinding.BindingMethods;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableByte;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableInt;
import androidx.databinding.PropertyChangeRegistry;
import androidx.lifecycle.ViewModel;

import com.example.rc_controller_mvvm.BLEManager;
import com.example.rc_controller_mvvm.RepeatButton;

import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class ControllerViewModel extends ViewModel implements SensorEventListener {
    private final static String DEFAULT_DEVICE_ADDRESS = "68:27:19:21:B4:7F";

    public static class GattAttributes {
        private static final HashMap<String, String> attributes = new HashMap<>();
        public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
        public static String POWER = "00002B05-0000-1000-8000-00805f9b34fb";
        public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
        public static String UART_OVER_BLE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"; // UART service UUID 6E400001B5A3F393E0A9E50E24DCCA9E
        public static String CLIENT_CHARACTERISTIC_Rx = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"; //6e400002b5a3f393e0a9e50e24dcca9e
        public static String CLIENT_CHARACTERISTIC_Tx = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"; //6e400003b5a3f393e0a9e50e24dcca9e

        static {
            // Sample Services.
            attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
            attributes.put("0000180f-0000-1000-8000-00805f9b34fb", "Battery Service");
            attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
            // Sample Characteristics.
            attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
            attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
            attributes.put("00002B05-0000-1000-8000-00805f9b34fb", "Power");
            attributes.put(UART_OVER_BLE, "UART over BLE");
            attributes.put(CLIENT_CHARACTERISTIC_Rx, "Rx for client");
            attributes.put(CLIENT_CHARACTERISTIC_Tx, "Tx for client");
        }

        public static String lookup(String uuid, String defaultName) {
            String name = attributes.get(uuid);
            return name == null ? defaultName : name;
        }
    }



    // TODO: Implement the ViewModel

    private final Handler handler;
    private BLEManager bleManager;
    private final Controller controller;
    private final ObservableBoolean connected;
    private final ObservableBoolean connecting;
    private final ObservableField<String> connectionText;
    private byte speed;
    private final ObservableInt roll;
    private final ObservableField<String> speedText;


    private ScheduledExecutorService scheduledExecutorService;

    private void sendControlData(){
        if(bleManager != null){
            //Log.d("Controller","Task start.");
            byte[] transmit = new byte[2];
            transmit[1] = speed;
            //Log.d("Controller","Roll="+roll.get());
            transmit[0] = (byte) (roll.get()+90);
            bleManager.writeCharacteristic(UUID.fromString(GattAttributes.UART_OVER_BLE),UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_Tx),transmit);
            //Log.d("CVM","Transmit control data.");


        }
    }

    private boolean changeSpeed(int diff){
        if(controller != null){
            controller.setSpeed(controller.getSpeed()+diff);
            speed=controller.getSpeed();
            setSpeedText();
            return true;
        }

        return false;

    }

    private final BLEManager.OnBLEEventListener bleEventListener = new BLEManager.OnBLEEventListener() {
        @Override
        public void onDeviceFound(BluetoothDevice device) {

        }

        @Override
        public void onConnectionStatusChanged(BLEManager.ConnectionStatus status) {
            handler.post(() ->{
                switch (status){
                    case connected:
                        connected.set(true);
                        if(scheduledExecutorService != null){
                            scheduledExecutorService.shutdown();
                            scheduledExecutorService =null;
                        }
                        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
                        scheduledExecutorService.scheduleWithFixedDelay(ControllerViewModel.this::sendControlData,1000,100, TimeUnit.MILLISECONDS);
                        connecting.set(false);
                        break;
                    case disconnected:
                        connected.set(false);
                        if(scheduledExecutorService != null){
                            scheduledExecutorService.shutdown();
                            scheduledExecutorService = null;
                        }
                        speed = 0;
                        setSpeedText();
                        connecting.set(false);
                        break;
                    case connecting:
                        connecting.set(true);

                }
                connectionUpdate();

            });
        }


        @Override
        public void onDataNotified(byte[] dataArray) {

        }
    };


    @BindingAdapter("android:onLongClick")
    public static void setOnLongClick(RepeatButton button, View.OnLongClickListener listener){
        button.setOnLongClickListener(listener);
    }
    public final View.OnLongClickListener speedUp = v -> changeSpeed(1);
    public final View.OnLongClickListener speedDown = v -> changeSpeed(-1);

    @BindingAdapter("android:onCheckChange")
    public static void setOnCheckChange(CompoundButton compoundButton, CompoundButton.OnCheckedChangeListener listener){
        compoundButton.setOnCheckedChangeListener(listener);
    }

    public final CompoundButton.OnCheckedChangeListener onConnectionChanged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){
                bleManager.connect(DEFAULT_DEVICE_ADDRESS);
            }else {
                bleManager.disconnect();
            }
            connectionUpdate();

        }
    };

    public final CompoundButton.OnCheckedChangeListener notifyConfigured = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            if(bleManager != null){
                bleManager.changeNotifyEnable(UUID.fromString(GattAttributes.UART_OVER_BLE),UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_Rx),b);
            }
        }
    };


    public ControllerViewModel() {
        super();
        handler = new Handler(Looper.getMainLooper());

        connected = new ObservableBoolean();
        connected.set(false);
        connecting = new ObservableBoolean();
        connecting.set(false);
        connectionText = new ObservableField<>();
        connectionUpdate();

        controller = new Controller();
        roll = new ObservableInt();
        roll.set(90);
        speed = 0;
        speedText = new ObservableField<>();
        setSpeedText();

    }
    public ObservableBoolean getConnected(){
        return connected;
    }

    public ObservableBoolean getConnecting() {
        return connecting;
    }

    public ObservableField<String> getConnectionText(){
        return connectionText;
    }

    private void connectionUpdate(){
        String text="";
        if(connected.get()){
            if(connecting.get()){
                text = "接続中";
            }else {
                text = "切断";
            }
        }else {
            if(connecting.get()){
                text = "切断中";
            }else {
                text = "接続";
            }
        }

        connectionText.set(text);
    }

    public ObservableInt getRoll() {
        return roll;
    }


    public ObservableField<String> getSpeedText(){
        return speedText;
    }

    private void setSpeedText(){
        speedText.set(String.format(Locale.JAPAN,"出力: %3d%%\n角度%3d°",speed,roll.get()));
    }

    public void setupBLE(BLEManager bleManager){
        if(bleManager == null){
            return;
        }
        if(this.bleManager != null){
            this.bleManager.disconnect();
            this.bleManager.removeEventListener(bleEventListener);
        }
        this.bleManager = bleManager;
        this.bleManager.addEventListener(bleEventListener);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        controller.setRoll(x, y);
        roll.set(controller.getRoll());
        setSpeedText();
        //Log.d("SensorEvent","Angle update: "+roll.get());

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }



}

