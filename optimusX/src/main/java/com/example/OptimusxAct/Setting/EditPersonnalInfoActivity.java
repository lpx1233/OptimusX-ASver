package com.example.OptimusxAct.Setting;

import android.content.ComponentName;
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
                IDcard myIdc = new IDcard();
                myIdc.setName(mName.getText().toString());
                myIdc.setPhoneNumber(mPhoneNum.getText().toString());
                myIdc.setSchoolNumber(mSchNum.getText().toString());
                myIdc.setWxID(mWeixin.getText().toString());
                myIdc.setWbID(mWeibo.getText().toString());
                myIdc.setQqID(mQQ.getText().toString());
                myIdc.setTag(mLabel.getText().toString());
                cdHelper.addMyIdcard(cdHelper.getWritableDatabase(), myIdc);

                //send my idcard to device
                mBluetoothLeService.sendIDcard(null, myIdc);

                if(isGuide){
                    //jump to EnterActivity
                    Intent intent = new Intent(this, EnterActivity.class);
                    startActivity(intent);
                }
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onReadIDCard(IDcard[] idc) {

    }

    @Override
    public void onSendIDCard(boolean isSuccess) {

    }

    @Override
    public void onReadBalance(byte[] arrayBalance) {

    }
}
