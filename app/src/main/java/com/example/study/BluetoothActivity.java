package com.example.study;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.study.Constants.CHARACTERISTIC_COMMAND_STRING;
import static com.example.study.Constants.CHARACTERISTIC_RESPONSE_STRING;
import static com.example.study.Constants.MAC_ADDRESS;
import static com.example.study.Constants.REQUEST_ENABLE_BT;
import static com.example.study.Constants.REQUEST_FINE_LOCATION;
import static com.example.study.Constants.SCAN_PERIOD;
import static com.example.study.Constants.TAG;

public class BluetoothActivity extends Activity {
    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning = false;
    private boolean connected = false;
    private Map<String, BluetoothDevice> scanResult;
    private ScanCallback scanCallback;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic gattCharacteristic;
    private BluetoothGattDescriptor desc;

    private TextView stateTextview;
    private TextView readTextview;
    private EditText inputEdit;
    private Button scan_btn;
    private Button send_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        stateTextview = findViewById(R.id.stateTextview);
        readTextview = findViewById(R.id.readTextview);
        inputEdit = findViewById(R.id.inputEdit);
        scan_btn = findViewById(R.id.scan_btn);
        send_btn = findViewById(R.id.send_btn);

        //BluetoothManager를 이용해 bluetoothAdapter설정 -> 스캔 하기 위한 기본 준비 완료
        BluetoothManager bluetoothManager;
        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan(view);
            }
        });

        send_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendData(view);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //블루투스 지원이 안되면 앱 종료
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, "BLE 지원이 안됩니다.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    //블루투스 스캔 시작
    private void startScan(View v){
        stateTextview.setText("Scanning...");

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent bleEnableIntent= new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
            startActivityForResult( bleEnableIntent, REQUEST_ENABLE_BT );
            stateTextview.setText("Scanning Failed: ble not enabled");
            return;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION );
            stateTextview.setText("Scanning Failed: no fine location permission");
            return;
        }

        //기존 연결 되어있던 GAPP 서버 연결 종료
        disconnectGattServer();

        //스캔할 장치 필터 - MAC주소의 장치만 스캔
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(MAC_ADDRESS).build();
        filters.add(scanFilter);

        //스캔 모드를 설정 - 저전력 모드로 스캔
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();

        //스캔 결과 처리 콜백함수
        scanResult = new HashMap<>();
        scanCallback = new BLEScanCallback(scanResult);

        if (bluetoothLeScanner == null){
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        //스캔 시작
        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        mScanning = true;

        //설정한 스캔 시간 지나면 스캔 정지
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_PERIOD);
    }

    private void sendData(View view) {
        if(!connected){
            Log.d(TAG, "Failed to sendData due to no connection");
            return;
        }

        gattCharacteristic = BluetoothUtils.findCommandCharacteristic(bluetoothGatt);
        if(gattCharacteristic == null){
            Log.d(TAG, "Unable to find cmd characteristic");
            disconnectGattServer();
            return;
        }

        String input = inputEdit.getText().toString();
        startStimulation(gattCharacteristic, input);
    }

    private class BLEScanCallback extends ScanCallback{
        private  Map<String, BluetoothDevice> scanResults;

        BLEScanCallback(Map<String, BluetoothDevice> mScanResults){
            scanResults = mScanResults;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult");
            addSanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult result : results){
                addSanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with code " + errorCode);
        }

        //장치의 MAC주소와 장치를 리스트에 추가
        private void addSanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            scanResults.put(deviceAddress, device);
            Log.d(TAG, "scan results device : " + device);
            stateTextview.setText("add scanned device : " + deviceAddress);
        }
    }

    private void stopScan(){
        if(mScanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null){
            bluetoothLeScanner.stopScan(scanCallback);
            scanComplete();
        }
        //scanCallback = null;
        mScanning = false;
        handler = null;
        stateTextview.setText("scanning stopped");

    }

    //찾아진 장치에 연결
    private void scanComplete() {
        if(scanResult.isEmpty()){
            stateTextview.setText("scan results is empty");
            Log.d(TAG, "scan results is empty");
            return;
        }

        //result값이 있으면 하나씩 추출해서 연결 시도
        for(String deviceAddress : scanResult.keySet()){
            Log.d(TAG, "Found device" + deviceAddress);
            //MAC주소를 이용해 장치 인스턴스 가져오기
            BluetoothDevice device = scanResult.get(deviceAddress);
            if(MAC_ADDRESS.equals(deviceAddress)){
                Log.d(TAG, "connecting device : " + deviceAddress);
                connectDevice(device);
            }
        }
    }

    //BLE 디바이스 연결
    private void connectDevice(BluetoothDevice device) {
        stateTextview.setText("Connectiong to" + device.getAddress());
        //GATT 서버 접속
        GattClientCallback gattClientCallback = new GattClientCallback();
        bluetoothGatt = device.connectGatt(this, false, gattClientCallback);
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            //연결 실패하면 GATT 서버 연결 종료
            if(status == BluetoothGatt.GATT_FAILURE){
                disconnectGattServer();
                return;
            } else if(status != BluetoothGatt.GATT_SUCCESS){
                disconnectGattServer();
                return;
            }

            if(newState == BluetoothProfile.STATE_CONNECTED){
                connected = true;
                stateTextview.setText("Connected");
                Log.d(TAG, "Connected to the GATT server");
                gatt.discoverServices();
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                disconnectGattServer();
            }
        }

        //데이터 읽기, 쓰기, 상태변화 처리
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if(status != BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, "Device service discovery failed, status"+status);
                return;
            }

            List<BluetoothGattCharacteristic> matchingCharacteristics = BluetoothUtils.findBLECharacteristics(gatt);
            if(matchingCharacteristics.isEmpty()){
                Log.d(TAG, "Unable to find characteristics");
                return;
            }

            Log.d(TAG, "Service discovery is successful");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG,"123123");
            super.onCharacteristicChanged(gatt, characteristic);

            gatt.setCharacteristicNotification(characteristic, true);

            Log.d(TAG, "characteristic changed : "+characteristic.getUuid().toString());
            readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            gatt.setCharacteristicNotification(characteristic, true);

            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, "Characteristic written successfully");
            } else {
                Log.d(TAG, "Characteristic write unsuccessful, state : "+status);
                disconnectGattServer();
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "join read successfully");
            super.onCharacteristicRead(gatt, characteristic, status);

            gatt.setCharacteristicNotification(characteristic, true);

            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG, "Characteristic read successfully");
                readCharacteristic(characteristic);
            } else {
                Log.d(TAG, "Characteristic read unsuccessful, state : " + status);
            }
        }

        private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
            byte[] msg = characteristic.getValue();
            Log.d(TAG, "read : " + msg.toString());
        }
    }

    private void startStimulation(BluetoothGattCharacteristic commandCharacteristic, String input) {
        commandCharacteristic.setValue(input);
        boolean success = bluetoothGatt.writeCharacteristic(commandCharacteristic);
        if( success ) {
            Log.d( TAG, input);
        } else {
            Log.d( TAG, "Failed to write command" );
        }
    }

    private void disconnectGattServer() {
        stateTextview.setText("Closing Connection");
        Log.d(TAG, "Closing Gatt connection");
        connected = false;
        if(bluetoothGatt != null){
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }
}