package com.example.OptimusxAct.Setting;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.OptimusxAct.Guide.EnterActivity;
import com.example.OptimusxAct.R;
import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.bluetoothlegatt.IDcard;
import com.optimusx.contacts.ContactsDatabaseHelper;
import com.optimusx.model.Writable;

public class EditPersonnalInfoActivity extends ActionBarActivity implements Writable {

	private Toolbar mToolbar;
	private ContactsDatabaseHelper cdHelper;
    private EditText mName;
    private EditText mPhoneNum;
    private EditText mSchNum;
    private EditText mWeixin;
    private EditText mQQ;
    private EditText mLabel;
    private EditText mWeibo;
    private boolean isGuide = false;
    private ServiceConnection mServiceConnection;
    private BluetoothLeService mBluetoothLeService;
    private ProgressDialog mProgressDialog;
    private boolean personalInfoSetSuccess;
    private boolean ifBleCommunicationFinished;
    private IDcard myIdc;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        //deal with the income bundle
        Bundle bundle=this.getIntent().getExtras();
        if(bundle != null){
            isGuide = bundle.getBoolean("isGuide");
        }

		//deal with views and layout
		setContentView(R.layout.activity_edit_personnal_info);
        mName = (EditText) findViewById(R.id.act_view_contact_name);
        mPhoneNum = (EditText) findViewById(R.id.phone);
        mSchNum = (EditText) findViewById(R.id.schnum);
        mWeixin = (EditText) findViewById(R.id.weixin);
        mWeibo = (EditText) findViewById(R.id.weibo);
        mQQ = (EditText) findViewById(R.id.qq);
        mLabel = (EditText) findViewById(R.id.label);
        mToolbar = (Toolbar) findViewById(R.id.act_edit_info_toolbar);

		//deal with toolbar
		setSupportActionBar(mToolbar);

        if(!isGuide) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationIcon(R.drawable.ic_action_back);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

		//deal with database
		cdHelper = new ContactsDatabaseHelper(this, "Contacts.db", null, 3);
		SQLiteDatabase db = cdHelper.getWritableDatabase();

		//set personal info
        mName.setText(cdHelper.myIdcard(db).n);
        mPhoneNum.setText(cdHelper.myIdcard(db).p);
        mSchNum.setText(cdHelper.myIdcard(db).s);
        mWeixin.setText(cdHelper.myIdcard(db).wxID);
        mWeibo.setText(cdHelper.myIdcard(db).wbID);
        mQQ.setText(cdHelper.myIdcard(db).qqID);
        mLabel.setText(cdHelper.myIdcard(db).tag);


        //bind BluetoothLeService
        Intent service = new Intent(this, BluetoothLeService.class);
        mServiceConnection = new ServiceConnection(){
            @Override
            public void onServiceDisconnected(ComponentName name) {
            }
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                //返回一个BluetoothLeService对象
                Log.v("StartActivity", "BleService binded");
                mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService();
            }
        };
        bindService(service, mServiceConnection, BIND_AUTO_CREATE);

        //deal with ProgressDialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("正在设定个人信息...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
	}

    @Override
    public void onDestroy(){
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.save, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_save:
                myIdc = new IDcard();
                myIdc.setName(mName.getText().toString());
                myIdc.setPhoneNumber(mPhoneNum.getText().toString());
                myIdc.setSchoolNumber(mSchNum.getText().toString());
                myIdc.setWxID(mWeixin.getText().toString());
                myIdc.setWbID(mWeibo.getText().toString());
                myIdc.setQqID(mQQ.getText().toString());
                myIdc.setTag(mLabel.getText().toString());
                cdHelper.addMyIdcard(cdHelper.getWritableDatabase(), myIdc);

                mProgressDialog.show();
                //social sync
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        //send my idcard to device
                        mBluetoothLeService.sendIDcard(EditPersonnalInfoActivity.this, myIdc);
                        long time = 0;
                        personalInfoSetSuccess = true;
                        while(!ifBleCommunicationFinished){
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            time = time + 200;
                            if(time == 60000){
                                personalInfoSetSuccess = false;
                                break;
                            }
                        }
                        if(personalInfoSetSuccess){
                            mProgressDialog.dismiss();
                            EditPersonnalInfoActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "个人信息设置成功", Toast.LENGTH_LONG).show();
                                }
                            });
                            if(isGuide){
                                //jump to EnterActivity
                                Intent intent = new Intent(EditPersonnalInfoActivity.this, EnterActivity.class);
                                startActivity(intent);
                            }
                            EditPersonnalInfoActivity.this.finish();
                        }else{
                            mProgressDialog.dismiss();
                            EditPersonnalInfoActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(EditPersonnalInfoActivity.this).setTitle("个人信息设置失败")//设置对话框标题
                                        .setMessage("请检查设备是否正确运行,并重试")//设置显示的内容
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
                break;
        }
        return true;
    }

    @Override
    public void onReadIDCard(IDcard[] idc) {
        //do nothing
    }

    @Override
    public void onSendIDCard(boolean isSuccess) {
        ifBleCommunicationFinished = true;
        personalInfoSetSuccess = isSuccess;
    }

    @Override
    public void onReadBalance(byte[] arrayBalance) {
        //do nothing
    }
}
