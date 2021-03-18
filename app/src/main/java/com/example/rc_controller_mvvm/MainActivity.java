package com.example.rc_controller_mvvm;

import android.Manifest;
import android.app.Activity;
import android.app.Presentation;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rc_controller_mvvm.ui.main.ControllerViewModel;
import com.example.rc_controller_mvvm.ui.main.MainFragment;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static String KEY_SELECTED_DEVICE_NAME = "Last Selected Device Name";
    private final static String KEY_SELECTED_DEVICE_ADDRESS = "Last Selected Device ADDRESS";


    public static class BLEDevice{
        private final String name;
        private final String address;
        public BLEDevice(String name,String address){
            this.address = address;
            this.name = name;
        };

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }
    }


    public static class BLEDeviceListAdapter extends BaseAdapter {
        private static class ViewHolder{
            TextView deviceName;
            TextView deviceAddress;

        }

        ArrayList<BLEDevice> devices;
        LayoutInflater inflater;

        public BLEDeviceListAdapter(LayoutInflater inflater){
            super();
            devices = new ArrayList<>();
            this.inflater = inflater;
        }
        public void addDevice(BLEDevice newDevice){
            for(BLEDevice contained:devices){
                if(contained.address.equals(newDevice.address)){
                    return;
                }
            }
            devices.add(newDevice);
        }

        public void clearList(){
            devices.clear();
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = inflater.inflate(R.layout.device_list,viewGroup,false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.textView_deviceAddress);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.textView_deviceName);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BLEDevice device = devices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("ななしのデバイス");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    private SensorManager sensorManager;
    private static final int ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private static final int ACCELEROMETER_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    private BLEManager bleManager;
    private ControllerViewModel controllerViewModel;

    BLEDeviceListAdapter listAdapter;
    Button scanButton;

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


        listAdapter = new BLEDeviceListAdapter(getLayoutInflater());
        scanButton = findViewById(R.id.button_scanStart);




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
        bleManager = new BLEManager(adapter,this);
        controllerViewModel.setupBLE(bleManager);
        ListView deviceList = findViewById(R.id.device_listView);
        this.listAdapter = new BLEDeviceListAdapter(getLayoutInflater());
        final TextView selectedNameView = findViewById(R.id.selected_Name);
        final TextView selectedAddressView = findViewById(R.id.selected_Address);

        scanButton.setText("検索");
        scanButton.setOnClickListener(scanning);
        bleManager.setDeviceFoundListeners(new BLEManager.OnDeviceFoundListener() {
            @Override
            public void onDeviceFound(BluetoothDevice device) {
                BLEDevice bleDevice = new BLEDevice(device.getName(),device.getAddress());
                listAdapter.addDevice(bleDevice);
                listAdapter.notifyDataSetChanged();

            }

            @Override
            public void onScanFinish() {
                scanButton.setText("検索");
                isScanning = false;
            }
        });
        final Activity activity = this;
        final DrawerLayout drawerLayout = findViewById(R.id.root_drawer);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        deviceList.setAdapter(listAdapter);
        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selectedName = ((BLEDevice)listAdapter.getItem(i)).name;
                String selectedAddress = ((BLEDevice)listAdapter.getItem(i)).address;
                selectedNameView.setText(selectedName);
                selectedAddressView.setText(selectedAddress);
                controllerViewModel.setDeviceName(selectedName);
                controllerViewModel.setDeviceAddress(selectedAddress);
                SharedPreferences.Editor preferences = PreferenceManager.getDefaultSharedPreferences(activity).edit();
                preferences.putString(KEY_SELECTED_DEVICE_ADDRESS,selectedAddress);
                preferences.putString(KEY_SELECTED_DEVICE_NAME,selectedName);
                preferences.apply();
                listAdapter.clearList();
                bleManager.disconnect();
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            }
        });



        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String lastAddress = preferences.getString(KEY_SELECTED_DEVICE_ADDRESS,"");
        String lastName = preferences.getString(KEY_SELECTED_DEVICE_NAME, "履歴なし");
        controllerViewModel.setDeviceName(lastName);
        controllerViewModel.setDeviceAddress(lastAddress);
        controllerViewModel.setChangeDeviceButtonAction(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleManager.disconnect();
                scanButton.performClick();
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        });


    }

    private boolean isScanning = false;
    private final View.OnClickListener scanning = (view -> {
        Button button = (Button)view;
        if(isScanning){
            button.setText("検索");
            bleManager.stopScan();
        }else {
            listAdapter.clearList();
            button.setText("中断");
            bleManager.startScan();
        }
        isScanning = !isScanning;
    });


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