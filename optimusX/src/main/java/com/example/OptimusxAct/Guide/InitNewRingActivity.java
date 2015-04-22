package com.example.OptimusxAct.Guide;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.OptimusxAct.R;
import com.example.OptimusxAct.Setting.EditPersonnalInfoActivity;
import com.example.OptimusxAct.StartActivity;
import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.optimusx.model.AESEncryptor;
import com.optimusx.model.Callbackable;

public class InitNewRingActivity extends ActionBarActivity implements Callbackable{

    private static final int REQUEST_ENABLE_BT = 1;
    private TextView sidTV;
	private EditText nameET;
	private Toolbar mToolbar;
	private EditText pwdET;
    private boolean isGuide = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService;
    private ServiceConnection mServiceConnection;
    private String mDeviceAddress;
    private String mDevicePassword;
    private EditText newpwdET;
    private ProgressDialog mProgressDialog;
    private boolean ifBleCommunicationFinished;
    private boolean pwdSetSuccess;
    private byte[] newPsw = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_init_new_ring);
		sidTV = (TextView) findViewById(R.id.act_init_ring_sid_tv);
		nameET = (EditText) findViewById(R.id.act_init_ring_name_et);
		pwdET = (EditText) findViewById(R.id.act_init_ring_pwd_et);
        newpwdET = (EditText) findViewById(R.id.act_init_ring_new_pwd_et);

		//deal with toolbar
		mToolbar = (Toolbar) findViewById(R.id.act_init_ring_toolbar);
		setSupportActionBar(mToolbar);
        if(!isGuide) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            mToolbar.setNavigationIcon(R.drawable.ic_action_back);
            mToolbar.setNavigationOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //bind BluetoothLeService
        Intent service = new Intent(this, BluetoothLeService.class);
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

        //deal with the income bundle
		Bundle bundle=this.getIntent().getExtras();
        if(bundle != null){
		    sidTV.setText((String)bundle.get("SID"));
            mDeviceAddress = (String)bundle.get("SID");
		    nameET.setText((String)bundle.get("title"));
            isGuide = bundle.getBoolean("isGuide");
        }

        //deal with ProgressDialog
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("正在绑定设备...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);
	}

    @Override
    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                mDevicePassword = pwdET.getText().toString();
                mBluetoothLeService.setAddress(mDeviceAddress);
                mBluetoothLeService.resetPassword(mDevicePassword.getBytes(), newpwdET.getText().toString().getBytes(), this);

                mProgressDialog.show();
                //social sync
                new Thread(new Runnable(){
                    @Override
                    public void run(){
                        ifBleCommunicationFinished = false;
                        byte[] oriPsw = mDevicePassword.getBytes();
                        for(int i=0; i < oriPsw.length; i++){
                            oriPsw[i] -= (byte) '0';
                        }
                        newPsw = newpwdET.getText().toString().getBytes();
                        for(int i=0; i < newPsw.length; i++){
                            newPsw[i] -= (byte) '0';
                        }
                        mBluetoothLeService.setPassword(oriPsw);
                        mBluetoothLeService.resetPassword(oriPsw, newPsw, InitNewRingActivity.this);
                        long time = 0;
                        pwdSetSuccess = true;
                        while(!ifBleCommunicationFinished){
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            time = time + 200;
                            if(time == 60000){
                                pwdSetSuccess = false;
                                break;
                            }
                        }
                        if(pwdSetSuccess){
                            mProgressDialog.dismiss();
                            InitNewRingActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "设备绑定成功", Toast.LENGTH_LONG).show();
                                }
                            });
                            if(isGuide){
                                //in the context of guide
                                //jump to the EditPersonnalInfoActivity
                                Intent intent = new Intent(InitNewRingActivity.this, EditPersonnalInfoActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putBoolean("isGuide", true);
                                intent.putExtras(bundle);
                                startActivity(intent);
                            }else{
                                //in the context of normal
                                //jump to StartActivity
                                Intent intent = new Intent(InitNewRingActivity.this, StartActivity.class);
                                startActivity(intent);
                            }
                            InitNewRingActivity.this.finish();
                        }else{
                            mProgressDialog.dismiss();
                            InitNewRingActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    new AlertDialog.Builder(InitNewRingActivity.this).setTitle("密码设置失败")//设置对话框标题
                                        .setMessage("请检查设备原始密码是否正确,并重试")//设置显示的内容
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

    public void onSetPassword(boolean isPasswordSet){
        ifBleCommunicationFinished = true;
        pwdSetSuccess = isPasswordSet;
        if(isPasswordSet) {
            //save device information
            SharedPreferences sp = getSharedPreferences(getString(R.string.device_preferences), MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            String password;
            try {
                password = new String(newPsw);
                password = AESEncryptor.encrypt(getString(R.string.preference_seed), password);
                editor.putString(getString(R.string.device_name), nameET.getText().toString());
                editor.putString(getString(R.string.device_address), sidTV.getText().toString());
                editor.putString(getString(R.string.device_pwd), password);
                editor.apply();
            } catch (Exception ex) {
                Log.v("InitNewRingActivity", "给密码加密时产生错误!");
                finish();
            }
        }
    }
}
