package com.syro.bluetoothlegattclient1.Activity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.syro.bluetoothlegattclient1.Util.BleUtil;
import com.syro.bluetoothlegattclient1.GattProfile.GattProfile;
import com.syro.bluetoothlegattclient1.R;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides UI to connect,
 * display GATT services and characteristics supported by the device.
 * The Activity communicates with BluetoothLeService
 */
public class DisplayServiceActivity extends AppCompatActivity {
    private static final String TAG = "SyroZhang";
    private TextView mBleConnectionState;
    private TextView mBleReturnData;
    private Toolbar mToolbar;
    private String mDvcName;
    private String mDvcAddr;
    private boolean isConnecting;
    private BleUtil mBluetoothLeUtil;
    private ExpandableListView mExpandableListView;
    private SimpleExpandableListAdapter mSimpleExpandableListAdapter;
    private ArrayList<HashMap<String, String>> mServiceList =
            new ArrayList<HashMap<String, String>>();
    private ArrayList<ArrayList<HashMap<String, String>>> mCharacList =
            new ArrayList<ArrayList<HashMap<String, String>>>();
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mAllGattCharacs =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifiedCharacteristic;
    private BluetoothGattCharacteristic mChosedCharacteristic;
    public static final int REQUEST_CODE = 0xF;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();// 获取intent的类型
            if (BleUtil.ACTION_GATT_CONNECTED.equals(action)) {
                Log.v(TAG, "received Broadcast: BLE connected ");
                isConnecting = true;
                invalidateOptionsMenu(); // 刷新Toolbar menu
                mBleConnectionState.setText("connection status: connected...");
            } else if (BleUtil.ACTION_GATT_CONNECTING.equals(action)) {
                mBleConnectionState.setText("connection status: connecting...");
            } else if (BleUtil.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.v(TAG, "received Broadcast: BLE disconnected");
                isConnecting = false;
                invalidateOptionsMenu(); // 刷新Toolbar menu
                mBleConnectionState.setText("connection status: disconnected...");
            } else if (BleUtil.ACTION_GATT_SERVICE_DISCOVERED.equals(action)) {
                Log.v(TAG, "received Broadcast: GATT services discovered");
                dispAllGattServices(mBluetoothLeUtil.getAllServices()); // 获取远端设备的所有服务
//                dispCustomServices(mBluetoothLeUtil.getCustomServices(GattProfile.NORDIC_UART)); // 获取远端设备的所有服务
            } else if (BleUtil.ACTION_CHARAC_DATA_AVAILABLE.equals(action)) {
                String str = intent.getStringExtra(BleUtil.CHARAC_DATA);
                Log.v(TAG, "received Broadcast: GATT data available");
                Log.v(TAG, "intent.Extra = " + str);
                mBleReturnData.setText("Data: " + str);
            } else if (BleUtil.ACTION_CHARAC_DATA_WRITE.equals(action)) {
                Log.v(TAG, "received Broadcast: GATT data write");
                mBleReturnData.setText("Data: Write successfully...");
            }
        }
    };

    private final ExpandableListView.OnChildClickListener onChildClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            Log.v(TAG, "ExpandableListView: GroupPosition = " + groupPosition + " ChildPosition = " + childPosition);
            if (mAllGattCharacs != null) {
                mChosedCharacteristic = mAllGattCharacs.get(groupPosition).get(childPosition);// 获取被选中的characteristic
                Log.v(TAG, "charac-UUID----> " + mChosedCharacteristic.getUuid().toString());
                Log.v(TAG, "charac-Permission----> " + mChosedCharacteristic.getPermissions());
                Log.v(TAG, "charac-Property----> " + mChosedCharacteristic.getProperties());
                byte[] data = mChosedCharacteristic.getValue();
                if (data != null && data.length > 0) {
                    try {
                        Log.v(TAG, "charac-Value----> " + new String(data, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                int characProper = mChosedCharacteristic.getProperties();// 返回Characteristic的Properties值(read/write/notify)
                // Characteristic是Notify属性
                if ((characProper & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifiedCharacteristic = mChosedCharacteristic;
                    mBluetoothLeUtil.setCharacteristicNotification(mChosedCharacteristic, true);
                    return true;
                }
                // Characteristic是Read属性
                if ((characProper & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear it first
                    // so it doesn't update the data field on the user interface.
                    if (mNotifiedCharacteristic != null) {
                        mBluetoothLeUtil.setCharacteristicNotification(mNotifiedCharacteristic, false);
                        mNotifiedCharacteristic = null;
                    }
                    mBluetoothLeUtil.readCharacteristic(mChosedCharacteristic);
                    return true;
                }
                // Characteristic是Write属性
                if ((characProper & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    Intent intent = new Intent(DisplayServiceActivity.this, WriteActivity.class);
                    intent.putExtra("CharacName", GattProfile.getInfo(mChosedCharacteristic.getUuid().toString(), "Unknown Characteristic"));
                    startActivityForResult(intent, REQUEST_CODE);// 打开一个子Activity来接收输入
//                    mBluetoothLeUtil.writeCharacteristic(characteristic);
                    return true;
                }
            }
            return false;
        }
    };

    // 子Activity关闭后回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.v(TAG, "onActivityResult(): requestCode = " + requestCode + " resultCode = " + resultCode);
        switch (resultCode) {
            case RESULT_OK:
                String str = data.getStringExtra("inputString");
                mBluetoothLeUtil.writeCharacteristic(mChosedCharacteristic, str);
                break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service);

        mBleConnectionState = (TextView) findViewById(R.id.tv_ble_connection_state);
        mBleReturnData = (TextView) findViewById(R.id.tv_ble_return_value);
        mExpandableListView = (ExpandableListView) findViewById(R.id.expand_lv);
        mExpandableListView.setOnChildClickListener(onChildClickListener);
        mSimpleExpandableListAdapter = new SimpleExpandableListAdapter(
                this,
                mServiceList,
//                android.R.layout.simple_expandable_list_item_2,
                R.layout.expandable_listview_service_items,
                new String[]{"name", "uuid"},
//                new int[] {android.R.id.text1, android.R.id.text2},
                new int[]{R.id.tv_service_name, R.id.tv_service_uuid},
                mCharacList,
//                android.R.layout.simple_expandable_list_item_2,
                R.layout.expandable_listview_charac_items,
                new String[]{"name", "uuid"},
//                new int[] {android.R.id.text1, android.R.id.text2}
                new int[]{R.id.tv_charac_name, R.id.tv_charac_uuid}
        );
        mExpandableListView.setAdapter(mSimpleExpandableListAdapter);
        Intent intent = getIntent();
        mDvcName = intent.getStringExtra("name");
        mDvcAddr = intent.getStringExtra("addr");
        Log.v(TAG, "mDvcName = " + mDvcName);
        Log.v(TAG, "mDvcAddr = " + mDvcAddr);
        initToolbar();
        connectGattServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册符合filter的广播接收器
        IntentFilter inf = new IntentFilter();
        inf.addAction(BleUtil.ACTION_GATT_CONNECTED);
        inf.addAction(BleUtil.ACTION_GATT_DISCONNECTED);
        inf.addAction(BleUtil.ACTION_GATT_SERVICE_DISCOVERED);
        inf.addAction(BleUtil.ACTION_CHARAC_DATA_AVAILABLE);
        inf.addAction(BleUtil.ACTION_CHARAC_DATA_WRITE);
        registerReceiver(receiver, inf);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);// 注销广播接收器
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeUtil.close();
    }

    // 生成Toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bt_connect, menu);
        if (isConnecting) {
            menu.findItem(R.id.item_bluetooth_disconnect).setVisible(true);
            menu.findItem(R.id.item_bluetooth_connect).setVisible(false);
        } else {
            menu.findItem(R.id.item_bluetooth_connect).setVisible(true);
            menu.findItem(R.id.item_bluetooth_disconnect).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    // Toolbar menu item被选中时回调
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_bluetooth_connect:
                mServiceList.clear();
                mCharacList.clear();
                mSimpleExpandableListAdapter.notifyDataSetChanged();
                connectGattServer();
                break;
            case R.id.item_bluetooth_disconnect:
                disconnectGattServer();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.activity_service_toolbar);
        mToolbar.setTitle(mDvcName);
        mToolbar.setLogo(R.drawable.jeckson);
        setSupportActionBar(mToolbar);// 添加Toolbar到Activity
    }

    private void connectGattServer() {
        Log.v(TAG, "mDvcAddr = " + mDvcAddr);
        mBluetoothLeUtil = new BleUtil(this);
        mBluetoothLeUtil.init();
        mBluetoothLeUtil.connect(mDvcAddr);
    }

    private void disconnectGattServer() {
        mBluetoothLeUtil.disconnect();
        mBluetoothLeUtil.close();
    }

    private void dispAllGattServices(List<BluetoothGattService> allGattServices) {
        mServiceList.clear();
        mCharacList.clear();
        String uuid = null;
        for (BluetoothGattService btGattService : allGattServices) {
            HashMap<String, String> tmpService = new HashMap<String, String>();
            uuid = btGattService.getUuid().toString();
            tmpService.put("name", GattProfile.getInfo(uuid, "Unknown Service"));
            tmpService.put("uuid", uuid);
            mServiceList.add(tmpService);// 添加一个GATT Service

            ArrayList<BluetoothGattCharacteristic> tmpCharacs = new ArrayList<BluetoothGattCharacteristic>();
            ArrayList<HashMap<String, String>> characItem = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> allCharacs = btGattService.getCharacteristics(); // 获取每个Service的所有characteristics

            for (BluetoothGattCharacteristic charac : allCharacs) {
                tmpCharacs.add(charac);
                HashMap<String, String> characInfo = new HashMap<String, String>();
                uuid = charac.getUuid().toString();
                characInfo.put("name", GattProfile.getInfo(uuid, "Unknown Characteristic"));
                characInfo.put("uuid", uuid);
                characItem.add(characInfo);
            }

            mCharacList.add(characItem);// 添加一个GATT characteristic
            mAllGattCharacs.add(tmpCharacs);
        }
        mSimpleExpandableListAdapter.notifyDataSetChanged();// 提示映射相应数据集的View刷新
    }

    private void dispCustomServices(BluetoothGattService service) {
        mServiceList.clear();
        mCharacList.clear();
        String uuid = null;
        HashMap<String, String> tmpService = new HashMap<String, String>();
        uuid = service.getUuid().toString();
        tmpService.put("name", GattProfile.getInfo(uuid, "Unknown Service"));
        tmpService.put("uuid", uuid);
        mServiceList.add(tmpService);// 添加一个GATT Service

        ArrayList<BluetoothGattCharacteristic> tmpCharacs = new ArrayList<BluetoothGattCharacteristic>();
        ArrayList<HashMap<String, String>> characItem = new ArrayList<HashMap<String, String>>();

        BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString(GattProfile.NORDIC_UART_TX));
        tmpCharacs.add(charac);
        HashMap<String, String> characInfo = new HashMap<String, String>();
        uuid = charac.getUuid().toString();
        characInfo.put("name", GattProfile.getInfo(uuid, "Unknown Characteristic"));
        characInfo.put("uuid", uuid);
        characItem.add(characInfo);

        charac = service.getCharacteristic(UUID.fromString(GattProfile.NORDIC_UART_RX));
        tmpCharacs.add(charac);
        characInfo = new HashMap<String, String>();
        uuid = charac.getUuid().toString();
        characInfo.put("name", GattProfile.getInfo(uuid, "Unknown Characteristic"));
        characInfo.put("uuid", uuid);
        characItem.add(characInfo);


        mCharacList.add(characItem);// 添加一个GATT characteristic
        mAllGattCharacs.add(tmpCharacs);

        mSimpleExpandableListAdapter.notifyDataSetChanged();// 提示映射相应数据集的View刷新
    }

}