package com.syro.bluetoothlegattclient1.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.syro.bluetoothlegattclient1.R;
import com.syro.bluetoothlegattclient1.Util.BleUtil;

/**
 * Created by Syro on 2016-01-13.
 */
public class WriteActivity extends AppCompatActivity {
    private BleUtil mBluetoothLeUtil;
    private EditText mEtInputMsg;
    private Button mBtnSend;
    private Toolbar mToolbar;
    private String mCharacName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);
        Intent intent = getIntent();// 返回启动此Activity的Intent
        mCharacName = intent.getStringExtra("CharacName");
        initToolbar(mCharacName);

        mBluetoothLeUtil = new BleUtil(this);
        mEtInputMsg = (EditText) findViewById(R.id.et_write);
        mBtnSend = (Button) findViewById(R.id.btn_start_to_write);
        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = mEtInputMsg.getText().toString();
                Intent intent = new Intent();
                intent.putExtra("inputString", str);
                setResult(RESULT_OK, intent);
                finish();// 关闭Activity
            }
        });
    }

    private void initToolbar(String characName) {
        mToolbar = (Toolbar) findViewById(R.id.activity_write_toolbar);
        mToolbar.setTitle(characName);
        mToolbar.setLogo(R.drawable.jeckson);
        setSupportActionBar(mToolbar);// 添加Toolbar到Activity
    }
}
