package com.example.rc_controller_mvvm.ui.main;

import android.bluetooth.BluetoothDevice;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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

import java.util.Locale;


public class ControllerViewModel extends ViewModel implements SensorEventListener, Observable {
    private final PropertyChangeRegistry callbacks = new PropertyChangeRegistry();


    // TODO: Implement the ViewModel
    private final Controller controller;
    private final ObservableBoolean connected;
    private byte speed;
    private final ObservableField<String> speedText;
    public final View.OnLongClickListener speedUp = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if(controller != null){
                controller.setSpeed(controller.getSpeed()+1);
                speed=controller.getSpeed();
                setSpeedText();
                return true;
            }

            return false;
        }
    };
    public final View.OnLongClickListener speedDown = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if(controller != null){
                controller.setSpeed(controller.getSpeed()-1);
                speed=controller.getSpeed();
                setSpeedText();
                return true;
            }

            return false;
        }
    };

    public final CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){

            }

        }
    };

    private final BLEManager.OnBLEEventListener bleEventListener = new BLEManager.OnBLEEventListener() {
        @Override
        public void onDeviceFound(BluetoothDevice device) {

        }

        @Override
        public void onConnectionStatusChanged(BLEManager.ConnectionStatus status) {

        }


        @Override
        public void onDataNotified(byte[] dataArray) {

        }
    };

    @BindingAdapter("android:onLongClick")
    public static void setOnLongClick(RepeatButton button, View.OnLongClickListener listener){
        button.setOnLongClickListener(listener);
    }


    private final ObservableInt roll;


    public ControllerViewModel() {
        super();
        connected = new ObservableBoolean();
        connected.set(false);
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

    public ObservableInt getRoll() {
        return roll;
    }


    public ObservableField<String> getSpeedText(){
        return speedText;
    }

    private void setSpeedText(){
        speedText.set(String.format(Locale.JAPAN,"出力: %3d%%",speed));
    }

    public void changeSpeed(int diff){

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        controller.setRoll(x, y);
        roll.set(controller.getRoll());

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.add(callback);

    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }
    void notifyChange() {
        callbacks.notifyCallbacks(this, 0, null);
    }

    void notifyPropertyChanged(int fieldId) {
        callbacks.notifyCallbacks(this, fieldId, null);
    }


}

