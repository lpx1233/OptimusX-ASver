package com.example.OptimusxAct.DeviceOperation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.bluetoothlegatt.IDcard;
import com.example.OptimusxAct.R;
import com.optimusx.contacts.ContactsDatabaseHelper;
import com.optimusx.model.Writable;

public class RingOptActivity extends ActionBarActivity implements Writable{

	private static final int REQUEST_ENABLE_BT = 1;
	boolean ifIDCardReadingFinished = false;
	BluetoothLeService mBluetoothLeService = null;
	ServiceConnection mServiceConnection = null;
	private Toolbar mToolbar;
	private ContactsDatabaseHelper cdHelper;
	private BluetoothAdapter mBluetoothAdapter;
	private String mDeviceAddress;
	protected MenuItem refreshItem;
	private int mIdcardLen = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ring_opt);
		
		//deal with toolbar
		mToolbar = (Toolbar) findViewById(R.id.act_ring_opt_toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mToolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		//deal with cdHelper
		cdHelper = new ContactsDatabaseHelper(this, "Contacts.db", null, 1);

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
        
		//handle bundle with name and SID
		Bundle bundle=this.getIntent().getExtras();
		if(bundle != null){
			Log.v("name", (String)bundle.get("name"));
			Log.v("SID", (String)bundle.get("SID"));
			mDeviceAddress = (String)bundle.get("SID");
			
		}
		
		//bind BluetoothLeService
		Intent service = new Intent(RingOptActivity.this, BluetoothLeService.class);
		mServiceConnection = new ServiceConnection(){
			@Override  
	        public void onServiceDisconnected(ComponentName name) {
	        }
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
	            //返回一个BluetoothLeService对象  
	            mBluetoothLeService = ((BluetoothLeService.LocalBinder)service).getService(); 
	            mBluetoothLeService.setAddress(mDeviceAddress);
	            //if (!mBluetoothLeService.initialize()) {
	            //    Log.e(RingOptActivity.class.getSimpleName(), "Unable to initialize Bluetooth");
	            //    finish();
	            //}
			}
		};
		bindService(service, mServiceConnection, BIND_AUTO_CREATE);
		
		//deal with list view
		String[] optString = new String[]{"社交模式同步", "读北京公交卡余额"};
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, optString);
		ListView listView = (ListView) findViewById(R.id.act_ring_opt_lv);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(position == 0){
					//social sync
					new Thread(new Runnable(){
						@Override
						public void run(){
							ifIDCardReadingFinished = false;
							mBluetoothLeService.sendIDcard(RingOptActivity.this, cdHelper.myIdcard(cdHelper.getWritableDatabase()));
							long time = 0;
							boolean readSuccess = true;
							while(!ifIDCardReadingFinished){
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								time = time + 200;
								if(time == 60000){
									readSuccess = false;
									break;
								}
							}
							if(readSuccess){
								RingOptActivity.this.runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(RingOptActivity.this, "同步了" + ((Integer)mIdcardLen).toString() + "名片", Toast.LENGTH_LONG).show();   
									}
								});
							}else{
								RingOptActivity.this.runOnUiThread(new Runnable() { 
									public void run()    
									{   
										Toast.makeText(RingOptActivity.this, "同步超时", Toast.LENGTH_LONG).show();   
									}
								});
							}
							ifIDCardReadingFinished = false;
							Log.v("ROA", "ifIDCardReading=" + ((Boolean)ifIDCardReadingFinished).toString());
						}
					}).start();
				}else if(position == 1){
					//reader
					Intent intent = new Intent(RingOptActivity.this, CardReadingActivity.class);
					startActivity(intent);
				}
			}
             
        });
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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.ring_opt, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
        /*case R.id.action_sync:
            showRefreshAnimation(item);
            return true;*/
        default:
            return super.onOptionsItemSelected(item);
        }
	}
	
	@Override
	public void onDestroy(){
		unbindService(mServiceConnection);
		super.onDestroy();
	}
	
	@SuppressLint("NewApi")
    private void showRefreshAnimation(MenuItem item) {
        hideRefreshAnimation();
 
        refreshItem = item;
 
        //这里使用一个ImageView设置成MenuItem的ActionView，这样我们就可以使用这个ImageView显示旋转动画了
        ImageView refreshActionView = (ImageView) getLayoutInflater().inflate(R.layout.action_view, null);
        refreshActionView.setImageResource(R.drawable.ic_action_refresh);
        refreshItem.setActionView(refreshActionView);
         
        //显示刷新动画
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        refreshActionView.startAnimation(animation);
    }
 
    @SuppressLint("NewApi")
    private void hideRefreshAnimation() {
        if (refreshItem != null) {
            View view = refreshItem.getActionView();
            if (view != null) {
                view.clearAnimation();
                refreshItem.setActionView(null);
            }
        }
    }

	@Override
	public void onReadIDCard(IDcard[] idcArray) {
		if(idcArray != null){
			for(IDcard idc : idcArray){
				cdHelper.addContact(cdHelper.getWritableDatabase(), idc);
			}
			mIdcardLen = idcArray.length;
		}
		ifIDCardReadingFinished = true;
	}

	@Override
	public void onReadBalance(byte[] arrayBalance) {
		// do nothing
	}
}
