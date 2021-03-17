package com.example.rc_controller_mvvm;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.rc_controller_mvvm.ui.main.ControllerViewModel;
import com.example.rc_controller_mvvm.ui.main.MainFragment;

public class MainActivity extends AppCompatActivity {

    private SensorManager sensorManager;
    private static final int ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private static final int ACCELEROMETER_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    private ControllerViewModel controllerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }
        if(
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        ){
            initBLE();
        }else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Bluetoothを使用するには現在位置へのアクセスが必要です", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 1);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1){
            if(grantResults.length > 0){
                initBLE();
            }else {
                finish();
            }
        }
    }

    private void initBLE(){
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if(adapter == null){
            Toast.makeText(this,"Bluetoothに対応していません",Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this,"Bluetooth LowEnergyに対応していません",Toast.LENGTH_SHORT).show();
            finish();
            return;

        }
        sensorManager =(SensorManager) getSystemService(Context.SENSOR_SERVICE);

        controllerViewModel = new ViewModelProvider(this).get(ControllerViewModel.class);
        final BLEManager bleManager = new BLEManager(adapter,this);
        controllerViewModel.setupBLE(bleManager);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeSensor();
    }

    private void startSensor(){
        if(controllerViewModel != null && sensorManager != null){
            Sensor sensor = sensorManager.getDefaultSensor(ACCELEROMETER);
            sensorManager.registerListener(controllerViewModel,sensor,ACCELEROMETER_DELAY);
        }
    }

    private void closeSensor(){
        if(controllerViewModel != null && sensorManager != null){
            Sensor sensor = sensorManager.getDefaultSensor(ACCELEROMETER);
            sensorManager.unregisterListener(controllerViewModel,sensor);
        }
    }
}