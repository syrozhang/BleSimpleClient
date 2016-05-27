package com.syro.bluetoothlegattclient1.Adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.syro.bluetoothlegattclient1.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Syro on 2016-01-06.
 */
public class DeviceListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> list;
    private int layoutRes;
    private LayoutInflater layoutInflater;
    private Context context;

    public DeviceListAdapter(Context context, int layoutRes, ArrayList<BluetoothDevice> list) {
        this.context = context;
        this.layoutRes = layoutRes;
        this.list = list;
        this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) { //第一次创建的item
            convertView = layoutInflater.inflate(layoutRes, null);
            viewHolder = new ViewHolder();
            viewHolder.dvc_name = (TextView) convertView.findViewById(R.id.tv_device_name);
            viewHolder.dvc_addr = (TextView) convertView.findViewById(R.id.tv_device_address);
            convertView.setTag(viewHolder);
        } else { //缓存中已有的item
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice bluetoothDevice = list.get(position);
        String name = bluetoothDevice.getName();
        if (name != null && name.length() > 0) {
            viewHolder.dvc_name.setText(bluetoothDevice.getName());
        } else {
            viewHolder.dvc_name.setText("Unknown Device");
        }
        viewHolder.dvc_addr.setText(bluetoothDevice.getAddress());
        return convertView;
    }

    static class ViewHolder {
        TextView dvc_name;
        TextView dvc_addr;
    }
}
