package com.syro.bluetoothlegattclient1.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.syro.bluetoothlegattclient1.Adapter.DeviceListAdapter;
import com.syro.bluetoothlegattclient1.R;

import java.util.ArrayList;

public class ScanBleDeviceActivity extends AppCompatActivity {
    private static final String TAG = "SyroZhang";
    private Toolbar mToolbar;
    private ListView mDeviceListView;
    private boolean isScaning;
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();
    private BluetoothAdapter mBluetoothAdapter;
    private DeviceListAdapter mDeviceListAdapter;
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            // 发现BLE设备后的回调函数
            if (!mDeviceList.contains(device)) {
                mDeviceList.add(device);
            }
            mDeviceListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.v(TAG, "onCreate()");

        mDeviceListView = (ListView) findViewById(R.id.lv_device_found);
        mDeviceListAdapter = new DeviceListAdapter(
                this,
                R.layout.listview_device_found,
                mDeviceList
        );
        mDeviceListView.setAdapter(mDeviceListAdapter);
        mDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice bluetoothDevice = mDeviceList.get(position);
                if (bluetoothDevice != null) {
                    Intent intent = new Intent(ScanBleDeviceActivity.this, DisplayServiceActivity.class);
                    intent.putExtra("name", bluetoothDevice.getName());
                    intent.putExtra("addr", bluetoothDevice.getAddress());
                    startActivity(intent);
                }
                if (isScaning) {
                    mBluetoothAdapter.stopLeScan(leScanCallback);
                    isScaning = false;
                }
            }
        });
        initToolbar();
        initBle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume()");
        // 打开Bluetooth Adapter
        if (!mBluetoothAdapter.isEnabled()) {
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivity(intent);
            mBluetoothAdapter.enable();// 直接打开手机Bluetooth功能
        }

        mDeviceList.clear();
        mDeviceListAdapter.notifyDataSetChanged();
        scanLeDevice(true);// 开始扫描BLE设备
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(TAG, "onPause()");
        scanLeDevice(false);// 停止扫描BLE设备
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();// 关闭手机Bluetooth功能
        }
    }

    // 生成Toolbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bt_switch, menu);
        if (isScaning) {
            menu.findItem(R.id.action_bluetooth_stop).setVisible(true);
            menu.findItem(R.id.action_bluetooth_scan).setVisible(false);
            menu.findItem(R.id.action_bluetooth_refresh).setActionView(R.layout.progress_bar);
        } else {
            menu.findItem(R.id.action_bluetooth_scan).setVisible(true);
            menu.findItem(R.id.action_bluetooth_stop).setVisible(false);
//            menu.findItem(R.id.action_bluetooth_refresh).setActionView(null);
            menu.findItem(R.id.action_bluetooth_refresh).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    // Toolbar menu item被选中时回调
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_bluetooth_scan:
                mDeviceList.clear();
                mDeviceListAdapter.notifyDataSetChanged();
                scanLeDevice(true);// 开始扫描BLE设备
                break;
            case R.id.action_bluetooth_stop:
                scanLeDevice(false);// 停止扫描BLE设备
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        mToolbar.setTitle(R.string.toolbar_title);
        mToolbar.setLogo(R.drawable.jeckson);
        setSupportActionBar(mToolbar);// 添加Toolbar到Activity
    }

    private void initBle() {
        // 检测手机是否支持BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.v(TAG, "BluetoothLe is not supported...");
            finish();
        }

        // 获取Bluetooth adapter，对于API level 18及以上，通过BluetoothManager来获取BluetoothAdapter的引用
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.v(TAG, "BluetoothLe is not supported...");
            finish();
        }

        // 打开Bluetooth Adapter
        if (!mBluetoothAdapter.isEnabled()) {
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivity(intent);
            mBluetoothAdapter.enable();// 直接打开手机Bluetooth功能
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.v(TAG, "startLeScan()");
            isScaning = true;
            mBluetoothAdapter.startLeScan(leScanCallback);// 开始一次BLE设备扫描

            // 2000ms后停止扫描BLE设备
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "Handler().postDelayed(2000ms): stopLeScan()");
                    isScaning = false;
                    mBluetoothAdapter.stopLeScan(leScanCallback);// 停止扫描BLE设备
                    invalidateOptionsMenu(); // 刷新Toolbar menu
                }
            }, 2000);
        } else {
            Log.v(TAG, "stopLeScan()");
            isScaning = false;
            mBluetoothAdapter.stopLeScan(leScanCallback);// 停止扫描BLE设备
        }
        invalidateOptionsMenu(); // 刷新Toolbar menu
    }
}
