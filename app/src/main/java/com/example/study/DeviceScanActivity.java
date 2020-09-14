package com.example.study;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.study.Constants.MAC_ADDRESS;
import static com.example.study.Constants.REQUEST_FINE_LOCATION;
import static com.example.study.Constants.TAG;

public class DeviceScanActivity extends Activity {

    private String TAG = "HYUNJU";

    private TextView stateTextview;
    private TextView readTextview;
    private EditText inputEdit;
    private Button scan_btn;
    private Button send_btn;

    // BluetoothAdapter : 블루투스관련 동작, 전체시스템을 위한 하나의 어댑터가 있고, 앱은 이 객체를 통해 상호작용
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private boolean mConnected = false;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    ArrayList<BluetoothDevice> mLeDevices = new ArrayList<BluetoothDevice>();
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private BluetoothLeService mBluetoothLeService;

    public final static int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public final static String MAC_ADDRESS = "D4:7C:44:40:09:5F";
    UUID[] uuid = new UUID[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mHandler = new Handler();

        stateTextview = findViewById(R.id.stateTextview);
        readTextview = findViewById(R.id.readTextview);
        inputEdit = findViewById(R.id.inputEdit);
        scan_btn = findViewById(R.id.scan_btn);
        send_btn = findViewById(R.id.send_btn);

        uuid[0] = UUID.fromString("F000C0E0-0451-4000-B000-000000000000");

        //전달하는 파라미터에 따라서 원하는 클래스형으로 형변환을 해야 한다는 것을 의미
        //파라미터로 전달되는 name값에 따라서 시스템 레벨의 서비스를 제어할 수 있는 핸들을 리턴
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();


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

        //블루투스 활성화 확인 -> 안되어있으면 권한 요청 대화 상자 표시
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        //블루투스 위치 퍼미션 체크
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION );
            stateTextview.setText("Scanning Failed: no fine location permission");
            return;
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(MAC_ADDRESS);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //resultCode: 창에서 어떤 버튼을 눌렀는지에 대한 결과값

        //사용자가 블루투스 사용을 원하지 않으면 종료
        if(requestCode == REQUEST_ENABLE_BT && requestCode == Activity.RESULT_CANCELED){
            finish();
            return;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    //블루투스 스캔 시작
    private void startScan(View v) {
        stateTextview.setText("Scanning...");

        scanLeDevice(true);
    }

    private void sendData(View view) {
        if(!mConnected){
            Log.d(TAG, "Failed to sendData due to no connection");
            return;
        }

        String input = inputEdit.getText().toString();
        startCommunication(mGattCharacteristics.get(0).get(0), input);
    }

    private void scanLeDevice(final boolean enable){
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    connectDevice(); //스레드 끝나고 연결 시도
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addDevice(device);
                }
            });
        }
    };

    public void addDevice(BluetoothDevice device) {
        final String deviceName = device.getName();
        final String deviceAddress = device.getAddress();

        if (!mLeDevices.contains(device) && deviceName != null && deviceName.length() > 0){
            Log.e(TAG, "deviceName : "+ deviceName);
            Log.e(TAG, "device : "+ deviceAddress);

            stateTextview.setText(deviceName + "\n" + deviceAddress);
            mLeDevices.add(device);
        }
    }

    //BLE 디바이스 연결
    private void connectDevice() {
        //mLeDevices에서 하나씩 연결 시도
        for(BluetoothDevice bluetoothDevice : mLeDevices){
            Log.d(TAG, "Found device" + bluetoothDevice.getAddress());
            if(MAC_ADDRESS.equals(bluetoothDevice.getAddress())){
                Log.d(TAG, "connecting device : " + bluetoothDevice.getAddress());
                stateTextview.setText("Connectiong to " + bluetoothDevice.getAddress());

                Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE); //인텐트로 서비스 특성 불러오기, Service 실행
            }
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(MAC_ADDRESS);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // 서비스에서 발생한 다양한 이벤트를 처리
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                stateTextview.setText("Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                stateTextview.setText("Disonnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // GATT 서비스를 발견 - 사용자 인터페이스에 지원되는 모든 서비스 및 특성을 표시
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // ACTION_DATA_AVAILABLE: 기기에서 수신 된 데이터 또는 알림 작업
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // 지원 되는 GATT Services/Characteristics
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null){
            return;
        }

        String uuid = null;
        String unknownServiceString ="Unknown service";
        String unknownCharaString = "Unknown characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // 사용 가능한 GATT 서비스
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // 사용 가능한 Characteristics
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.e(TAG, "gattCharacteristic.getUuid() : " + gattCharacteristic.getUuid()+"");
                Log.e(TAG, "UUID() : " + UUID.fromString(GattAttributes.CHARACTERISTIC_STRING)+"");

                if(gattCharacteristic.getUuid().equals(UUID.fromString(GattAttributes.CHARACTERISTIC_STRING))) {
                    charas.add(gattCharacteristic);
                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
                    uuid = gattCharacteristic.getUuid().toString();
                    currentCharaData.put(LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                    currentCharaData.put(LIST_UUID, uuid);
                    gattCharacteristicGroupData.add(currentCharaData);

                    mGattCharacteristics.add(charas);
                    gattCharacteristicData.add(gattCharacteristicGroupData);
                }
            }
        }

        matchCharacteristics();
    }

    private void matchCharacteristics(){
        if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(0).get(0);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(characteristic, true);
            }
        }
    }

    // 주어진 GATT 특성이 선택되면 지원되는 기능을 확인합니다. 이 샘플은 '읽기'및 '알림'기능을 보여줍니다.
    private void startCommunication(BluetoothGattCharacteristic bluetoothGattCharacteristic, String input){
        bluetoothGattCharacteristic.setValue(input);
        mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristic);
    }

    private void displayData(String data) {
        if (data != null) {
            readTextview.setText(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

}