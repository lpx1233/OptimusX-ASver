package com.example.OptimusxAct.DeviceOperation;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.OptimusxAct.R;
import com.example.OptimusxAct.Setting.EditPersonnalInfoActivity;
import com.example.OptimusxAct.StartActivity;
import com.example.android.bluetoothlegatt.BluetoothLeService;
import com.example.android.bluetoothlegatt.IDcard;
import com.optimusx.contacts.ContactsDatabaseHelper;
import com.optimusx.model.Writable;

public class MyRingsFragment extends Fragment implements Writable{

    private static final long SCAN_PERIOD = 10000;
    private Button mSocialSyncBtn;
	private ImageButton mEditMeBtn;
	private BluetoothLeService mBluetoothLeService;
	private ContactsDatabaseHelper cdHelper;
	private int mIdcardLen = 0;
	private boolean ifIDCardReadingFinished;
	private ProgressDialog mProgressDialog;
	private boolean readSuccess;
    private TextView mTvName;
    private TextView mTvSchnum;
    private TextView mTvPhonenum;
    private Handler mHandler;
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private Button mReadBalanceBtn;
    private Button mJiaoWuBtn;

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_my_rings, container, false);
        //deal with views
        mEditMeBtn = (ImageButton) rootView.findViewById(R.id.edit_me);
        mEditMeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//edit personal info
				Intent intent = new Intent(getActivity().getApplicationContext(), EditPersonnalInfoActivity.class);
				startActivity(intent);
			}
		});
        mTvName = (TextView) rootView.findViewById(R.id.act_view_contact_name);
        mTvSchnum = (TextView) rootView.findViewById(R.id.tv_schnum);
        mTvPhonenum = (TextView) rootView.findViewById(R.id.tv_phonenum);

        //deal with handler
        mHandler = new Handler();

        //deal with ProgressDialog
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle("提示");
        mProgressDialog.setMessage("正在从设备中读出...");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(false);

        //deal with social sync button
        mSocialSyncBtn = (Button) rootView.findViewById(R.id.btn_func);
        mSocialSyncBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mProgressDialog.show();
				//social sync
				new Thread(new Runnable(){

					@Override
					public void run(){
						ifIDCardReadingFinished = false;
						mBluetoothLeService.readIDcard(MyRingsFragment.this);
						long time = 0;
						readSuccess = true;
						while(!ifIDCardReadingFinished){
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
							getActivity().runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(getActivity().getApplicationContext(), "同步了" + ((Integer)mIdcardLen).toString() + "张名片", Toast.LENGTH_LONG).show();
                                    if(mIdcardLen != 0) {
                                        ((StartActivity) getActivity()).selectFragment(1);
                                    }
								}
							});
						}else{
							mProgressDialog.dismiss();
							getActivity().runOnUiThread(new Runnable() {
								public void run() {
									Toast.makeText(getActivity().getApplicationContext(), "同步失败,请检查密码和地址设置,并重试", Toast.LENGTH_LONG).show();
								}
							});
						}
					}
				}).start();
			}
		});
        mSocialSyncBtn.setClickable(false);
        mSocialSyncBtn.setTextColor((getActivity().getApplicationContext().getResources()).getColorStateList(R.color.secondary_text));

        //deal with read balance button
        mReadBalanceBtn = (Button) rootView.findViewById(R.id.btn_read_balance);
        mReadBalanceBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity().getApplicationContext(), CardReadingActivity.class);
                getActivity().startActivity(intent);
            }
        });

        //deal with jiaowu button
        //TODO implement the specific logic for login jiaowu or else
        mJiaoWuBtn = (Button) rootView.findViewById(R.id.btn_login_jiaowu);
        mJiaoWuBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://202.112.132.147:7001/ieas2");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        mJiaoWuBtn = (Button) rootView.findViewById(R.id.btn_find_classroom);
        mJiaoWuBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://202.112.132.147:7001/ieas2");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        mJiaoWuBtn = (Button) rootView.findViewById(R.id.btn_grades);
        mJiaoWuBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://202.112.132.147:7001/ieas2");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        mJiaoWuBtn = (Button) rootView.findViewById(R.id.btn_watch_class);
        mJiaoWuBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://202.112.132.147:7001/ieas2");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

		//deal with cdHelper
		cdHelper = new ContactsDatabaseHelper(getActivity().getApplicationContext(), "Contacts.db", null, 4);

        //set the personal info
        mTvName.setText(cdHelper.myIdcard(cdHelper.getWritableDatabase()).n);
        mTvPhonenum.setText(cdHelper.myIdcard(cdHelper.getWritableDatabase()).p);
        mTvSchnum.setText(cdHelper.myIdcard(cdHelper.getWritableDatabase()).s);

        //deal with ble services
        mBluetoothLeService = ((StartActivity)getActivity()).getmBluetoothLeService();
        mBluetoothAdapter = ((StartActivity)getActivity()).getmBluetoothAdapter();

        //scan if device is there
        mSocialSyncBtn.setClickable(false);
        mSocialSyncBtn.setTextColor((getActivity().getApplicationContext().getResources()).getColorStateList(R.color.secondary_text));
        scanLeDevice(true);

        return rootView;
	}

    @SuppressWarnings("deprecation")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if(device.getAddress().equals(((StartActivity)getActivity()).getmDeviceAddress())){
                scanLeDevice(false);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSocialSyncBtn.setClickable(true);
                        mSocialSyncBtn.setTextColor((getActivity().getApplicationContext().getResources()).getColorStateList(R.color.primary_text));
                    }
                });
            }
        }
    };

	@Override
	public void onReadIDCard(IDcard[] idcArray) {
		if(idcArray != null){
			for(IDcard idc : idcArray){
				cdHelper.addContact(cdHelper.getWritableDatabase(), idc);
			}
			mIdcardLen = idcArray.length;
			readSuccess = true;
		}else{
			mIdcardLen = 0;
		}
		ifIDCardReadingFinished = true;
	}

	@Override
	public void onReadBalance(byte[] arrayBalance) {
        //do nothing
	}

    @Override
    public void onSendIDCard(boolean isSuccess){
        //do nothing
    }
}
