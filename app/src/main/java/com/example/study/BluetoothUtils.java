package com.example.study;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.study.Constants.CHARACTERISTIC_COMMAND_STRING;
import static com.example.study.Constants.CHARACTERISTIC_RESPONSE_STRING;
import static com.example.study.Constants.SERVICE_STRING;

public class BluetoothUtils {
    /*
    BLE 특성 찾는 클래스
     */

    public static List<BluetoothGattCharacteristic> findBLECharacteristics(BluetoothGatt gatt) {
        List<BluetoothGattCharacteristic> matchingCharacteristics = new ArrayList<>();
        List<BluetoothGattService> serviceList = gatt.getServices();
        BluetoothGattService service = findGattService(serviceList);

        if(service == null){
            return matchingCharacteristics;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList){
            if (isMatchingCharacteristic(characteristic)){
                matchingCharacteristics.add(characteristic);
            }
        }

        return matchingCharacteristics;
    }

    //주변 장치의 명령 Characteristic 찾기
    @Nullable
    public static BluetoothGattCharacteristic findCommandCharacteristic(BluetoothGatt gatt){
        return findCharacteristic(gatt, CHARACTERISTIC_COMMAND_STRING);
    }

    @Nullable
    public static BluetoothGattCharacteristic findResponseCharacteristic( BluetoothGatt gatt ) {
        return findCharacteristic( gatt, CHARACTERISTIC_RESPONSE_STRING );
    }

    //UUID Characteristic 찾기
    @Nullable
    private static BluetoothGattCharacteristic findCharacteristic(BluetoothGatt gatt, String uuidString) {
        List<BluetoothGattService> serviceList = gatt.getServices();
        BluetoothGattService service = BluetoothUtils.findGattService(serviceList);
        if(service == null){
            return null;
        }

        List<BluetoothGattCharacteristic> characteristicList = service.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristicList){
            if(matchingCharacteristic(characteristic, uuidString)){
                return characteristic;
            }
        }

        return null;
    }

    private static boolean matchingCharacteristic(BluetoothGattCharacteristic characteristic, String uuidString) {
        if(characteristic == null){
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return matchUUIDs(uuid.toString(), uuidString);
    }

    //서버의 서비스와 일치하는 GATT 서비스 찾기
    private static BluetoothGattService findGattService(List<BluetoothGattService> serviceList) {
        for(BluetoothGattService service : serviceList){
            String serviceUUIDString =  service.getUuid().toString();
            if(matchServiceUUIDString(serviceUUIDString)){
                return service;
            }
        }
        return null;
    }

    private static boolean matchServiceUUIDString(String serviceUUIDString) {
        return matchUUIDs(serviceUUIDString, SERVICE_STRING);
    }

    private static boolean isMatchingCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(characteristic == null){
            return false;
        }
        UUID uuid = characteristic.getUuid();
        return matchingCharacteristicUUID(uuid.toString());
    }

    private static boolean matchingCharacteristicUUID(String characteristicUUIDString) {
        return matchUUIDs(characteristicUUIDString, CHARACTERISTIC_COMMAND_STRING, CHARACTERISTIC_RESPONSE_STRING);
    }

    private static boolean matchUUIDs(String uuidString, String... matchs) {
        for (String match : matchs){
            if(uuidString.equalsIgnoreCase(match)){
                return true;
            }
        }
        return false;
    }

}
