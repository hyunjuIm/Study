package com.example.study;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BluetoothActivity2 extends Activity {

    private TextView stateTextview;
    private TextView readTextview;
    private EditText inputEdit;
    private Button scan_btn;
    private Button send_btn;

    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler handler;

    public final static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth2);

        stateTextview = findViewById(R.id.stateTextview);
        readTextview = findViewById(R.id.readTextview);
        inputEdit = findViewById(R.id.inputEdit);
        scan_btn = findViewById(R.id.scan_btn);
        send_btn = findViewById(R.id.send_btn);

        //전달하는 파라미터에 따라서 원하는 클래스형으로 형변환을 해야 한다는 것을 의미
        //파라미터로 전달되는 name값에 따라서 시스템 레벨의 서비스를 제어할 수 있는 핸들을 리턴
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
}