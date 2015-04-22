package com.example.OptimusxAct.DeviceOperation;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.OptimusxAct.R;
import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.bluetoothlegatt.IDcard;
import com.optimusx.model.Writable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class CardReadingActivity extends ActionBarActivity implements Writable{

	private ServiceConnection mServiceConnection = null;
	private BluetoothLeService mBluetoothLeService = null;
    private double mBalance = 0;
    private ProgressDialog mProgressDialog;
    private boolean readSuccess = false;
    private boolean ifCardReadingFinished = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_card_reading);

		//deal with toolbar
        Toolbar mToolbar = (Toolbar) findViewById(R.id.act_card_reading_toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_action_back);
		mToolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

		//bind service
		Intent service = new Intent(CardReadingActivity.this, BluetoothLeService.class);
		mServiceConnection = new ServiceConnection(){
			@Override  
	        public void onServiceDisconnected(ComponentName name) {  
	              
	        }
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
	            //返回一个BluetoothLeService对象 
	            mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();  
			}
		};
		bindService(service, mServiceConnection, BIND_AUTO_CREATE);

        //deal with ProgressDialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("正在从设备中读卡...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);

        //deal with read balance button
        Button mReadBjmBalance = (Button) findViewById(R.id.btn_read_bjm_balance);
        mReadBjmBalance.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.show();
                //social sync
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        ifCardReadingFinished = false;
                        mBluetoothLeService.readBalance(CardReadingActivity.this);
                        long time = 0;
                        readSuccess = true;
                        while(!ifCardReadingFinished){
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            time = time + 200;
                            if(time == 30000){
                                readSuccess = false;
                                break;
                            }
                        }
                        if(readSuccess){
                            mProgressDialog.dismiss();
                            CardReadingActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(CardReadingActivity.this).setTitle("公交卡余额")//设置对话框标题
                                        .setMessage("您的公交卡余额为 " + ((Double) mBalance).toString() + "元")//设置显示的内容
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加确定按钮
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                                                //do nothing
                                            }
                                        }).show();//在按键响应事件中显示此对话框
                                }
                            });
                        }else{
                            mProgressDialog.dismiss();
                            CardReadingActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(CardReadingActivity.this).setTitle("读取失败")//设置对话框标题
                                        .setMessage("读取失败,请检查卡是否贴紧感应区,并重试")//设置显示的内容
                                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {//添加确定按钮
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {//确定按钮的响应事件
                                            //do nothing
                                            }
                                        }).show();//在按键响应事件中显示此对话框
                                }
                            });
                        }
                    }
                }).start();
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.card_reading, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onDestroy(){
		unbindService(mServiceConnection);
		super.onDestroy();
	}

	@Override
	public void onReadIDCard(IDcard[] idc) {
		//do nothing
	}

	@Override
	public void onReadBalance(byte[] arrayBalance) {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(arrayBalance));
		try {
			mBalance = 0;
			mBalance = mBalance + dis.readUnsignedByte()*16777216;
			mBalance = mBalance + dis.readUnsignedByte()*65536;
			mBalance = mBalance + dis.readUnsignedByte()*256;
			mBalance = mBalance + dis.readUnsignedByte();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mBalance = mBalance/100.0;
        ifCardReadingFinished = true;
	}
}
