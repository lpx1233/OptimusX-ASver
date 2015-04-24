package com.example.OptimusxAct;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.OptimusxAct.Contact.AllBusinessCardFragment;
import com.example.OptimusxAct.DeviceOperation.MyRingsFragment;
import com.example.OptimusxAct.Guide.GuideActivity;
import com.example.OptimusxAct.Setting.SettingFragment;
import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.optimusx.model.AESEncryptor;

public class StartActivity extends ActionBarActivity{
	
	private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private Toolbar mToolbar;
	private ActionBarDrawerToggle mDrawerToggle;
	private String mTitle;
    private BluetoothAdapter mBluetoothAdapter;
	private ServiceConnection mServiceConnection;
	private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private String mDevicePassword;
	private int mPos = 0;
    private Handler mHandler;
    private boolean mScanning;

    @Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

        //deal with SharedPreferences
        SharedPreferences sp = getSharedPreferences(getString(R.string.device_preferences), MODE_PRIVATE);
        mDeviceAddress = sp.getString(getString(R.string.device_address), null);
        mDevicePassword = sp.getString(getString(R.string.device_pwd), null);
        if (mDeviceAddress == null || mDevicePassword == null){
            //没有绑定
            Log.v("StartActivity", "尚未设置设备");
            startActivity(new Intent(StartActivity.this, GuideActivity.class));
            finish();
        }
        //对密码进行AES解密
        try{
            mDevicePassword = AESEncryptor.decrypt(getString(R.string.preference_seed), mDevicePassword);
        }catch(Exception ex){
            Toast.makeText(this, "获取密码时产生解密错误!", Toast.LENGTH_SHORT);
            mDevicePassword = new String(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
        }

        //deal with views
		setContentView(R.layout.activity_start);
		mToolbar = (Toolbar) findViewById(R.id.act_start_toolbar);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.act_start_drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.act_start_left_drawer);
        //deal with toolbar
        setSupportActionBar(mToolbar);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open, R.string.close) {
			@Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mTitle = (String) mToolbar.getTitle();
                mToolbar.setTitle("OptimusX");
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                mToolbar.setTitle(mTitle);
                super.onDrawerClosed(drawerView);
            }
        };
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
		/*mToolbar.inflateMenu(R.menu.my_rings);
		mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				int id = arg0.getItemId();
				switch(id){
				case R.id.action_add:{
					Intent intent = new Intent(StartActivity.this, NewRingActivity.class);
					startActivity(intent);
				}break;
				}
				return false;
			}
		});*/

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
        //deal with bundle
        mPos = 0;
        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            mPos = bundle.getInt("mPos", 0);
        }
        mDrawerList.setItemChecked(mPos, true);

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
	            mBluetoothLeService.setAddress(mDeviceAddress);
                mBluetoothLeService.setPassword(mDevicePassword.getBytes());
			}
		};
		bindService(service, mServiceConnection, BIND_AUTO_CREATE);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, new String[]{"我的设备", "所有名片", "设置"}));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectFragment(position);
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

        // resume the selected fragment
        selectFragment(mPos);
        mToolbar.setTitle(mTitle);
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
        getMenuInflater().inflate(R.menu.my_rings, menu);
		switch(mPos){
		case 0:
            menu.findItem(R.id.action_add).setVisible(true);
			break;
		case 1:
            menu.findItem(R.id.action_add).setVisible(false);
			break;
		case 2:
            menu.findItem(R.id.action_add).setVisible(false);
			break;
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
        case R.id.action_add:
			Intent intent = new Intent(StartActivity.this, GuideActivity.class);
			startActivity(intent);
            break;
        }
        return true;
	}

    //getters
    public BluetoothLeService getmBluetoothLeService(){
        return mBluetoothLeService;
    }

    public String getmDeviceAddress() {
        return mDeviceAddress;
    }

    public String getmDevicePassword() {
        return mDevicePassword;
    }

    public BluetoothAdapter getmBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    //select fragment
	public void selectFragment(int position){
		mPos = position;
		invalidateOptionsMenu();
		switch(position){
		case 0:{
			// goto MyRingsFragment
			// Create a new fragment and specify the planet to show based on position
		    Fragment fragment = new MyRingsFragment();
		    // Insert the fragment by replacing any existing fragment
		    FragmentManager fragmentManager = getFragmentManager();
		    fragmentManager.beginTransaction()
		                   .replace(R.id.act_start_linear_layout, fragment)
		                   .commit();

		    // Highlight the selected item, update the title, and close the drawer
		    mDrawerList.setItemChecked(position, true);
		    mTitle = "我的设备";
		}break;
		case 1:{
			// goto AllBusinessCardFragment
			// Create a new fragment and specify the planet to show based on position
		    Fragment fragment = new AllBusinessCardFragment();

		    // Insert the fragment by replacing any existing fragment
		    FragmentManager fragmentManager = getFragmentManager();
		    fragmentManager.beginTransaction()
		                   .replace(R.id.act_start_linear_layout, fragment)
		                   .commit();

		    // Highlight the selected item, update the title, and close the drawer
		    mDrawerList.setItemChecked(position, true);
			mTitle = "所有名片";
		}break;
		case 2:{
			// goto SettingFragment
			// Create a new fragment and specify the planet to show based on position
		    Fragment fragment = new SettingFragment();

		    // Insert the fragment by replacing any existing fragment
		    FragmentManager fragmentManager = getFragmentManager();
		    fragmentManager.beginTransaction()
		                   .replace(R.id.act_start_linear_layout, fragment)
		                   .commit();

		    // Highlight the selected item, update the title, and close the drawer
		    mDrawerList.setItemChecked(position, true);
		    mTitle = "设置";
		}break;
		}
	    mDrawerLayout.closeDrawer(mDrawerList);
	}
}
