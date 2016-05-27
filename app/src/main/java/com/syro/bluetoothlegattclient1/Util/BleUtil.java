package com.syro.bluetoothlegattclient1.Util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.syro.bluetoothlegattclient1.GattProfile.GattProfile;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

/**
 * Created by Syro on 2016-01-06.
 */
public class BleUtil {
    public static final String CHARAC_DATA = "com.syro.bluetootleutil.CHARAC_DATA";
    private static final String TAG = "SyroZhang";
    public static final String ACTION_GATT_CONNECTED = "com.syro.bluetootleutil.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_CONNECTING = "com.syro.bluetootleutil.ACTION_GATT_CONNECTING";
    public static final String ACTION_GATT_DISCONNECTED = "com.syro.bluetootleutil.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_SERVICE_DISCOVERED = "com.syro.bluetootleutil.ACTION_GATT_SERVICE_DISCOVERED";
    public static final String ACTION_CHARAC_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_CHARAC_DATA_AVAILABLE";
    public static final String ACTION_CHARAC_DATA_WRITE = "com.example.bluetooth.le.ACTION_CHARAC_DATA_WRITE";
    public static final UUID UUID_HEART_RATE_MEASUREMENT = UUID.fromString(GattProfile.HEART_RATE_MEASUREMENT);
    public static final UUID UUID_BODY_SENSOR_LOCATION = UUID.fromString(GattProfile.BODY_SENSOR_LOCATION);
    public static final UUID UUID_BATTERY_LEVEL = UUID.fromString(GattProfile.BATTERY_LEVEL);
    public static final UUID UUID_TEMPERATURE_TYPE = UUID.fromString(GattProfile.TEMPERATURE_TYPE);
    public static final UUID UUID_NORDIC_UART_RX = UUID.fromString(GattProfile.NORDIC_UART_RX);
    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGattClient;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        //*BLE连接状态改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v(TAG, "onConnectionStateChange() : newState = " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v(TAG, "********* GATT-Client has connected to GATT-Server *********");

                broadcastUpdate(ACTION_GATT_CONNECTED);
                boolean ret = mBluetoothGattClient.discoverServices();

                Log.v(TAG, "discoverServices() return: " + ret); // discover services offered by remote devices
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.v(TAG, "GATT-Client connecting to GATT-Server...");

                broadcastUpdate(ACTION_GATT_CONNECTING);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v(TAG, "********* GATT-Client has disconnected to GATT-Server *********");

                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        //*发现Services
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.v(TAG, "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICE_DISCOVERED);
            } else {
                Log.v(TAG, "onServicesDiscovered() received status = " + status);
            }
        }

        //*读Characteristic值
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.v(TAG, "BluetoothGattCallback.onCharacteristicRead()");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_CHARAC_DATA_AVAILABLE, characteristic);
            } else {
                Log.v(TAG, "onCharacteristicRead() received status = " + status);
            }
        }

        //*写Characteristic值
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.v(TAG, "BluetoothGattCallback.onCharacteristicWrite()");
            Log.v(TAG, "onCharacteristicWrite() received status: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_CHARAC_DATA_WRITE);
            } else if (status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED) {
                Log.v(TAG, "onCharacteristicWrite() received status ---> Gatt write not permitted");
            }
        }

        //*Characteristic值改变
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.v(TAG, "BluetoothGattCallback.onCharacteristicChanged()");
            broadcastUpdate(ACTION_CHARAC_DATA_AVAILABLE, characteristic);
        }
    };

    public BleUtil(Context context) {
        this.mContext = context;
    }

    public void init() {
        Log.v(TAG, "BleUtil.init()");
        // 获取Bluetooth adapter，对于API level 18及以上，通过BluetoothManager来获取BluetoothAdapter的引用
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.v(TAG, "mBluetoothAdapter is null");
            return;
        }
    }

    public void connect(String rmtAddress) {
        Log.v(TAG, "BleUtil.connect()");
        BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(rmtAddress);// 获取要连接的远端设备对象
        mBluetoothGattClient = bluetoothDevice.connectGatt(mContext, false, bluetoothGattCallback);// 连接此远端设备上的Gatt服务器
        Log.v(TAG, "mBluetoothGattClient = " + mBluetoothGattClient);
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        Log.v(TAG, "BleUtil.disconnect()");
        if (mBluetoothAdapter == null || mBluetoothGattClient == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGattClient.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.v(TAG, "BleUtil.close()");
        if (mBluetoothGattClient == null) {
            return;
        }
        mBluetoothGattClient.close();
        mBluetoothGattClient = null;
    }

    //* send a broadcast
    private void broadcastUpdate(final String action) {
        Intent intent = new Intent(action);
        mContext.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic charac) {
        Intent intent = new Intent(action);
        //*** 下面是处理不同的characteristic的过程，根据其官方说明来解析数据 ***
        if (UUID_HEART_RATE_MEASUREMENT.equals(charac.getUuid())) {
            int heartRate;
            // getIntValue():返回此Characteristc的Value字节数组，Value[]一共8 Bytes
            int flags = charac.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).intValue();//Value[0] == "Flags"
            if ((flags & 0x01) == 0) {
                // Flags最后一位为0，Heart Rate值为一个字节; 否则为两个字节
                // '1'表示从Characteristic Value偏移为1的地方开始读取
                heartRate = charac.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1).intValue();
                Log.v(TAG, "Heart rate format UINT8." + String.format("Received heart rate: %d", heartRate));
            } else {
                heartRate = charac.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1).intValue();
                Log.v(TAG, "Heart rate format UINT16." + String.format("Received heart rate: %d", heartRate));
            }
            intent.putExtra(CHARAC_DATA, String.valueOf(heartRate));
        } else if (UUID_BODY_SENSOR_LOCATION.equals(charac.getUuid())) {
            //*按照十六进制数显示
            byte[] data = charac.getValue();
            if (data != null && data.length > 0) {
                String str0 = null;
                StringBuilder stringBuilder = new StringBuilder();
                for (byte tmp : data) {
                    str0 = String.format("%x", tmp);
                    stringBuilder.append(str0);
                }
                intent.putExtra(CHARAC_DATA, stringBuilder.toString());
            }
        } else if (UUID_BATTERY_LEVEL.equals(charac.getUuid())) {
            int batteryLevel = charac.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).intValue();
            intent.putExtra(CHARAC_DATA, String.valueOf(batteryLevel));
        } else if (UUID_TEMPERATURE_TYPE.equals(charac.getUuid())) {
            //*按照十六进制数显示
            byte[] data = charac.getValue();
            if (data != null && data.length > 0) {
                String str0 = null;
                StringBuilder stringBuilder = new StringBuilder();
                for (byte tmp : data) {
                    str0 = String.format("%x", tmp);
                    stringBuilder.append(str0);
                }
                intent.putExtra(CHARAC_DATA, stringBuilder.toString());
            }
        } else if (UUID_NORDIC_UART_RX.equals(charac.getUuid())) {
            //*按照字符串解析
            byte[] data = charac.getValue();
            if (data != null && data.length > 0) {
                String str = null;
                try {
                    str = new String(data, "UTF-8");
                    intent.putExtra(CHARAC_DATA, str);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //*默认下characteristic的值按照字符串解析
            byte[] data = charac.getValue();
            if (data != null && data.length > 0) {
                try {
                    intent.putExtra(CHARAC_DATA, new String(data, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        mContext.sendBroadcast(intent);
    }

    public List<BluetoothGattService> getAllServices() {
        return mBluetoothGattClient.getServices();// 返回一个远端设备所提供Service的list
    }

    public BluetoothGattService getCustomServices(String uuid) {
        return mBluetoothGattClient.getService(UUID.fromString(uuid));// 返回远端设备此UUID所对应的Service
    }

    /**
     * Request a read on a given BluetoothGattCharacteristic. The read result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicRead() callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGattClient == null) {
            Log.v(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGattClient.readCharacteristic(characteristic);
    }

    /**
     * Write a string on a given BluetoothGattCharacteristic. The write result is reported
     * asynchronously through the BluetoothGattCallback.onCharacteristicWrite() callback.
     *
     * @param characteristic The characteristic to write to.
     * @param inputMsg       the message that is to be written.
     */
    public void writeCharacteristic(final BluetoothGattCharacteristic characteristic, String inputMsg) {
        if (mBluetoothAdapter == null || mBluetoothGattClient == null) {
            Log.v(TAG, "BluetoothAdapter not initialized");
            return;
        }
//        characteristic.setWriteType(BluetoothGattCharacteristic.PERMISSION_WRITE);
//        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        Log.v(TAG, "writeCharacteristic() inputMsg = " + inputMsg);
        characteristic.setValue(inputMsg);
        mBluetoothGattClient.writeCharacteristic(characteristic);
    }

    /**
     * 使能关联Characteristic的notification功能
     *
     * @param charac  Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic charac, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGattClient == null) {
            Log.v(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGattClient.setCharacteristicNotification(charac, enabled);

        //*Enable notification of characteristics
//        if (UUID_HEART_RATE_MEASUREMENT.equals(charac.getUuid()) ||
//                UUID_BATTERY_LEVEL.equals(charac.getUuid()) ||
//                UUID_NORDIC_UART_RX.equals(charac.getUuid())) {

            // 先获取Characteristics的client characteristic configuration描述符(0x2902)
            BluetoothGattDescriptor desc = charac.getDescriptor(UUID.fromString(GattProfile.CLIENT_CHARACTERISTIC_CONFIGURATION));
            // 把Descriptor(0x2902)中的notification功能打开
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGattClient.writeDescriptor(desc);
//        }

    }
}
