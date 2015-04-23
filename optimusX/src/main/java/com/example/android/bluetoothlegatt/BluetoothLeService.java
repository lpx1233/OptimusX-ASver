/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.optimusx.model.Callbackable;
import com.optimusx.model.Writable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private byte KEY[] =new byte[]{0,0,0,0,0,0,0,0};
    private final static byte MAX_WRITE_LOOP = 20;
    private final static byte MAX_READ_PAGE_LOOP = 30;
    private final static byte MAX_READ_LOOP = 30;
    private final static byte COMMAND_WRITE = 1;
    private final static byte COMMAND_WRITE_DONE = 2;
    private final static byte COMMAND_READ = 3;
    private final static byte COMMAND_READ_BALANCE = 4;
    
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private boolean mConnected = false;
    private String mDeviceAddress = null;
    public boolean isPasswordCorrect = false;
    public boolean isTimeout = false;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";

    private final static UUID UUID_NOTIFICATION =
            UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    private final static UUID UUID_READ_INDEX =
    		UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private final static UUID UUID_READ_BALANCE =
    		UUID.fromString("0000fff9-0000-1000-8000-00805f9b34fb");
    
    private BluetoothGattCharacteristic FFF1 = null;
    private BluetoothGattCharacteristic FFF2 = null;
    private BluetoothGattCharacteristic FFF3 = null;
    private BluetoothGattCharacteristic FFF4 = null;
    private BluetoothGattCharacteristic FFF6 = null;
    private BluetoothGattCharacteristic FFF7 = null;
    private BluetoothGattCharacteristic FFF8 = null;
    private BluetoothGattCharacteristic FFF9 = null;
    
    private byte NOTIFICATION = 0;
    private OnReading onReading = new OnReading();
    private IO mIO = new IO();
    private Handler handler = new Handler();
    private IDcard writeIDcardTemp = null;
    private Writable mWritable = null;
    private Callbackable mCallbackable = null;
    
    private Thread writeThread;
    private Thread readThread;
    private Thread readBalanceThread;
    private Thread connectThread;
    private Thread loginThread;
    private Thread iniThread;
    private Thread resetPasswordThread;
    private Thread pushNotificationThread;
    private Thread writeUniauthThread;
    private Thread readUniauthThread;


	public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        registerReceiver(mReceiver, mIntentFilter());
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        unregisterReceiver(mReceiver);
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        Log.d(TAG, "initialize succeed");
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    private boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        /*if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }*/

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    private void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.d(TAG, "disconnect()");
        isPasswordCorrect = false;
        mBluetoothGatt.disconnect();
        
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
   private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    private List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
    
    //////////////////////////////////////INTERNAL/////////////////////////////////////////
	private void broadcastUpdate(final String action) {
	    final Intent intent = new Intent(action);
	    sendBroadcast(intent);
	}

	private void ini(){
		Log.d(TAG, "ini called");
		iniThread = new Thread(new Runnable(){
			@Override
			public void run(){
				mIO.setCharacteristicNotification(FFF4,true);
	        	Log.d(TAG, "FFF4 set");
	        	try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
				FFF7.setValue(KEY);
	        	try {
					mIO.write(FFF7);
				} catch (InterruptedException e) {
				}
	        	Log.d(TAG, "KEY write");
			}
		});
	    iniThread.start();
	}

	//////////////////////////////////////////////////////////////////////////////////
	private void getChara(List<BluetoothGattService> gattServices){
		if (gattServices == null) return;
	    String uuid = null;
		for (BluetoothGattService gattService : gattServices) {
	        if(gattService.getUuid().toString().equalsIgnoreCase("0000fff0-0000-1000-8000-00805f9b34fb")){
	        	List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
	        	for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
	                uuid = gattCharacteristic.getUuid().toString();
	                //get certain characteristics
	                if(uuid.equalsIgnoreCase("0000fff1-0000-1000-8000-00805f9b34fb")) {FFF1=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff2-0000-1000-8000-00805f9b34fb")) {FFF2=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff3-0000-1000-8000-00805f9b34fb")) {FFF3=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff4-0000-1000-8000-00805f9b34fb")) {FFF4=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff6-0000-1000-8000-00805f9b34fb")) {FFF6=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff7-0000-1000-8000-00805f9b34fb")) {FFF7=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff8-0000-1000-8000-00805f9b34fb")) {FFF8=gattCharacteristic;}
	                if(uuid.equalsIgnoreCase("0000fff9-0000-1000-8000-00805f9b34fb")) {
	                	FFF9=gattCharacteristic;
	                	connectThread.interrupt();
	                	}
	            }
	        }
	    }
	}

	// Implements callback methods for GATT events that the app cares about.  For example,
	// connection change and services discovered.
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
	    @Override
	    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
	        String intentAction;
	        if(status == 0){
	        	if (newState == BluetoothProfile.STATE_CONNECTED) {
		            intentAction = ACTION_GATT_CONNECTED;
		            mConnectionState = STATE_CONNECTED;
		            mConnected = true;
		            broadcastUpdate(intentAction);
		            Log.i(TAG, "Connected to GATT server.");
		            // Attempts to discover services after successful connection.
		            Log.i(TAG, "Attempting to start service discovery:" +
		                    mBluetoothGatt.discoverServices());
		
		        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
		            intentAction = ACTION_GATT_DISCONNECTED;
		            mConnectionState = STATE_DISCONNECTED;
		            mConnected = false;
		            Log.i(TAG, "Disconnected from GATT server.");
		            broadcastUpdate(intentAction);
		            close();
		            if(isTimeout){
		            	sendIDcard(mWritable, writeIDcardTemp);
		            	isTimeout = false;
		            }
		        }
	        }else if(status == 133){
	        	isTimeout = true;
	        }
	    }
	
	    @Override
	    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
	        if (status == BluetoothGatt.GATT_SUCCESS) {
	            getChara(getSupportedGattServices());
	        } else if(status == 133){
	        	isTimeout = true;
	        }else {
	            Log.w(TAG, "onServicesDiscovered received: " + status);
	        }
	    }
	
	    @Override
	    public void onCharacteristicRead(BluetoothGatt gatt,
	                                     BluetoothGattCharacteristic characteristic,
	                                     int status) {
	    	
	        if (status == BluetoothGatt.GATT_SUCCESS) {
	            if(UUID_READ_INDEX.equals(characteristic.getUuid())){
	            	onReading.readIndex = (int)((characteristic.getValue()[0])&0x0FF);
	            	Log.i(TAG, "READ INDEX:  " + String.valueOf(onReading.readIndex));
	            	onReading.setBytes();
	            }else if(UUID_READ_BALANCE.equals(characteristic.getUuid())){
	            	readBalanceThread.interrupt();
	            	onReading.onReadBalance(characteristic.getValue());
	            } else{
	            	byte[] bytes  = characteristic.getValue();
	                onReading.addBytes(bytes);
	            }
	        }else if(status == 133){
	        	isTimeout = true;
	        }
	        if(mIO.IOThread != null) mIO.IOThread.interrupt();
	        if(FFF8.getUuid().equals(characteristic.getUuid())) readThread.interrupt();
	        if(FFF2.getUuid().equals(characteristic.getUuid())) readThread.interrupt();
	        if(FFF9.getUuid().equals(characteristic.getUuid())) 
	        	if(readBalanceThread != null) readBalanceThread.interrupt();
	    }
	    
	    @Override
	    public void onCharacteristicWrite(BluetoothGatt gatt,
	            BluetoothGattCharacteristic characteristic, int status) {
	    	if (status == BluetoothGatt.GATT_SUCCESS){
	    		if(FFF8.getUuid().equals(characteristic.getUuid())
		    			||FFF3.getUuid().equals(characteristic.getUuid())
		    			||FFF1.getUuid().equals(characteristic.getUuid())
		    			||FFF6.getUuid().equals(characteristic.getUuid())
		    			||FFF7.getUuid().equals(characteristic.getUuid())
		    			){
		    		if(mIO.IOThread != null){
		        		mIO.IOThread.interrupt();
		        	}
		    	}
	    	}else if(status == 133){
	        	isTimeout = true;
	        }
	    	if(FFF7.getUuid().equals(characteristic.getUuid())) connectThread.interrupt();
	    	if(FFF8.getUuid().equals(characteristic.getUuid())) writeThread.interrupt();
	    	if(FFF3.getUuid().equals(characteristic.getUuid())) readThread.interrupt();
	    	if(FFF6.getUuid().equals(characteristic.getUuid())) resetPasswordThread.interrupt();
	    }
	
	    @Override
	    public void onCharacteristicChanged(BluetoothGatt gatt,
	                                        BluetoothGattCharacteristic characteristic) {
	    	if(mIO.IOThread != null){
		    	getNotification(characteristic.getValue());
	    		mIO.IOThread.interrupt();
	    	}
	    	
	    }
	    
	    @Override
	    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                int status) {
	    	if (status == BluetoothGatt.GATT_SUCCESS){
		    	if(mIO.IOThread != null) mIO.IOThread.interrupt();//TODO delete?
		    	if(iniThread != null) iniThread.interrupt();
	    	}else if(status == 133){
	        	isTimeout = true;
	        }
	    }
	};

	//得到通知返回值
	private void getNotification(byte[] notification){
		NOTIFICATION = notification[0];
		Log.d(TAG, "【【【【【【NOTIFICATION】】】】】】 " + String.valueOf(NOTIFICATION));
		if(NOTIFICATION == (byte)1){
			isPasswordCorrect = true;
			if(resetPasswordThread != null) resetPasswordThread.interrupt();
		}
		if(NOTIFICATION == (byte)0){
			isPasswordCorrect = false;
			if(resetPasswordThread != null) resetPasswordThread.interrupt();
			handler.post(new Runnable() {
	              public void run() {
	            	  Toast.makeText(getApplicationContext(), "密码错误", Toast.LENGTH_SHORT).show();
	              }
			});

            mCallbackable.onSetPassword(false);
		}
		if(NOTIFICATION == (byte)2 || NOTIFICATION == (byte)3)
			if(writeThread != null) writeThread.interrupt();
		if(NOTIFICATION == (byte)4) if(readThread != null) readThread.interrupt();
		if(NOTIFICATION == (byte)4) if(pushNotificationThread != null) pushNotificationThread.interrupt();
	}

	private void interruptAll(){
		if(writeThread != null) writeThread.interrupt();
		if(readThread != null) readThread.interrupt();
		if(readBalanceThread != null) readBalanceThread.interrupt();
		if(loginThread != null) loginThread.interrupt();
		if(resetPasswordThread != null) resetPasswordThread.interrupt();
		if(pushNotificationThread != null) pushNotificationThread.interrupt();
	}

	//hash
	private static byte[] hash(byte[] hashThis) {
	    try {
	      byte[] hash = new byte[16];
	      MessageDigest md = MessageDigest.getInstance("MD5");
	      hash = md.digest(hashThis);
		  
	      final StringBuilder hashBuilder = new StringBuilder(hash.length);
	      for(byte byteChar : hash)
	      	hashBuilder.append(String.format("%02X ", byteChar));
	      Log.i(TAG, "hash length: " +  hash.length);
	      Log.i(TAG, "hash data:" +  hashBuilder.toString());
	      
	      return hash;
	    } catch (NoSuchAlgorithmException nsae) {
	      System.err.println("MD5 algorithm is not available...");
	      System.exit(2);
	    }
	    return null;
	  }

	//改变命令位
	private boolean setCommand(byte command){
		if(command<=5 && command>=0) {
			FFF1.setValue(new byte[]{command});
			Log.d(TAG, "set command "+ String.valueOf(command));
			try {
				mIO.write(FFF1);
			} catch (InterruptedException e) {
			}
			return true;
		}
		else return false;
	}

	private class IO{
		Thread IOThread = null;
		
		public IO(){
		}
		
		public void read(BluetoothGattCharacteristic chara) throws InterruptedException{//TODO
			if(IOThread!=null && IOThread.isAlive()){
				try {
					IOThread.join();
				} catch (InterruptedException e) {
					Log.w(TAG, "Outer thread waiting state of join is interrupted");
				}
			}
			Runnable read = new ReadCharacteristicRunnable(chara);
			IOThread = new Thread(read);
			IOThread.start();
		}
		
		public void write(BluetoothGattCharacteristic chara) throws InterruptedException{//TODO
			if(IOThread!=null && IOThread.isAlive()){
				try {
					IOThread.join();
				} catch (InterruptedException e) {
					Log.w(TAG, "Outer thread waiting state of join is interrupted");
				}
			}
			
			Runnable write = new WriteCharacteristicRunnable(chara);
			IOThread = new Thread(write);
			IOThread.start();
		}
		
		public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
	            boolean enabled) {
			if(IOThread!=null && IOThread.isAlive()){
				try {
					IOThread.join();
				} catch (InterruptedException e) {
					Log.w(TAG, "Outer thread waiting state of join is interrupted");
				}
			}
			Runnable setCharacteristicNotificationRunnable = new SetCharacteristicNotificationRunnable(characteristic, enabled);
			IOThread = new Thread(setCharacteristicNotificationRunnable);
			
			IOThread.start();
		}
		
		private class ReadCharacteristicRunnable implements Runnable{
			BluetoothGattCharacteristic obj = null;
			
			public ReadCharacteristicRunnable(BluetoothGattCharacteristic object){
				obj = object;
			}
			
			@Override
			public void run() {
				if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		            Log.w(TAG, "BluetoothAdapter not initialized");
		            return;
		        }
		        mBluetoothGatt.readCharacteristic(obj);
				
		        try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
			}
		
		}
	
		private class WriteCharacteristicRunnable implements Runnable{
			BluetoothGattCharacteristic obj = null;
			
			public WriteCharacteristicRunnable(BluetoothGattCharacteristic object){
				obj = object;
			}
			@Override
			public void run() {
				if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		            Log.w(TAG, "BluetoothAdapter not initialized");
		            return;
		        }
		        mBluetoothGatt.writeCharacteristic(obj);
		        try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
			}
		
		}
	
		private class SetCharacteristicNotificationRunnable implements Runnable{
			BluetoothGattCharacteristic characteristic;
			boolean enabled;
			
			public SetCharacteristicNotificationRunnable(BluetoothGattCharacteristic chara,boolean enabled) {
				characteristic = chara;
				this.enabled = enabled;
			}
			
			@Override
			public void run() {
				if (mBluetoothAdapter == null || mBluetoothGatt == null) {
	    			Log.w(TAG, "BluetoothAdapter not initialized");
	    			return;
	    			}
	    		mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
	
	    		// This is specific to Heart Rate Measurement.
	    		if (UUID_NOTIFICATION.equals(characteristic.getUuid())) {
	    			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
	    					UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
	    			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
	    			mBluetoothGatt.writeDescriptor(descriptor);
	    			}
	    		try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private class ConnectRunnable implements Runnable {
	
		@Override
		public void run(){
			initialize();
			connect(mDeviceAddress);
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				ini();
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					interruptAll();
				}
			}
		}
		
	}

	private class WriteRunnable implements Runnable {
		private byte[] input;
		
		WriteRunnable(byte[] input){
			this.input = input;
		}
	
		@Override
		public void run() {
			final StringBuilder stringBuilder = new StringBuilder(input.length);
	        for(byte byteChar : input)
	            stringBuilder.append(String.format("%d,", byteChar&0x0FF));
	        Log.i(TAG, "write input data length: " +  input.length);
	        Log.i(TAG, "write input data:" +  stringBuilder.toString());
	        
			if(!mConnected){
				Runnable connectRunnable = new ConnectRunnable();
		    	connectThread = new Thread(connectRunnable);
		    	connectThread.start();
		    	try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
			}
	    	if(mConnected&&isPasswordCorrect){
	
				setCommand((byte)1);
				try {Thread.sleep(2000);
	            } catch (InterruptedException e) {
	            }
				
				//the loop
				int writeIndex=0;
				for(int i=0, writeLoopIndex=0;i<=input.length/9&&mConnected;writeLoopIndex++){
					Log.d(TAG, "write loop " + String.valueOf(i));
					if(writeLoopIndex>MAX_WRITE_LOOP){
						Log.w(TAG, "Write loop > MAX WRITE LOOP, going to break write loop.");
						break;
					}
					byte tenbytes[] = new byte[10];
					tenbytes[0] = (byte) (i+1);//set app_state
					//set other 9 bytes
					for(int j=1;j<10;j++){
						if(writeIndex>=input.length) break;
						tenbytes[j] = input[writeIndex];
						writeIndex++;
					}
					
	        		//the fire loop
					for(byte fireIndex = 0;fireIndex<5&&mConnected;fireIndex++){
	        			Log.i(TAG, "trying to write:" + new String(tenbytes));
	        			FFF8.setValue(tenbytes);
	        			try{
	        				mIO.write(FFF8);
	        			}catch(InterruptedException e){
	        				Thread.interrupted();
	        			}
	        			try {Thread.sleep(10000);
	                    } catch (InterruptedException e) {
	                        Log.v(TAG, "Write thread interrupted");
	                    }
	            		
	            		if(NOTIFICATION == i+12){
	            			i++;
	            			Log.i(TAG, "NOTIFICATION get, write cheak loop break");
	            			break;
	            		}else{
	            			Log.i(TAG, "NOTIFICATION wrong, going to refire");//fire loop over
	            		}
					}
				}//the loop over
				setCommand((byte)2);
				try {Thread.sleep(10000);
	            } catch (InterruptedException e) {
	                Log.v(TAG, "Write thread interrupted");
                    mWritable.onSendIDCard(true);
                    disconnect();
	            }
				if(NOTIFICATION != 3) mWritable.onSendIDCard(false);
				//if(!mConnected) return;
				//readIDcard(mWritable);
	    	}
		}
		
	}

	private void write(final byte[] input){
		Runnable writeRunnable = new WriteRunnable(input);
	 	writeThread = new Thread(writeRunnable); 
	 	writeThread.start();
	}

	private class ReadRunnable implements Runnable{
		@Override
		public void run() {
			if(!mConnected){
				Runnable connectRunnable = new ConnectRunnable();
		    	connectThread = new Thread(connectRunnable);
		    	connectThread.start();
		    	try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
			}
			
			if(mConnected&&isPasswordCorrect){
				setCommand((byte)3);
				try {Thread.sleep(10000);
	            } catch (InterruptedException e) {
	            }
				//the page loop
				for(byte readPageLoopIndex = 0; readPageLoopIndex<MAX_READ_PAGE_LOOP && NOTIFICATION != (byte)6 && mConnected;readPageLoopIndex++){
					Log.d(TAG, "read page loop " + String.valueOf(readPageLoopIndex));
					onReading.nextpage();
					
					try{
	    				mIO.read(FFF2);
	    			}catch(InterruptedException e){
	    			}
	    			try {Thread.sleep(10000);
	                } catch (InterruptedException e) {
	                    Log.v(TAG, "Write thread interrupted");
	                }
	    			
	    			if(onReading.readIndex <= 0) break;
	    			else{//the read loop
						boolean readSuccess = false;
						for(byte i=1, readLoopIndex=0; readLoopIndex<MAX_READ_LOOP && mConnected; readLoopIndex++){
							readSuccess = false;
							Log.d(TAG, "read loop " + String.valueOf(i));
							
							FFF3.setValue(new byte[]{i});
							Log.d(TAG, "write FFF3: " + String.valueOf(i));
							try{
		        				mIO.write(FFF3);
		        			}catch(InterruptedException e){
		        				Thread.interrupted();
		        			}
		        			try {Thread.sleep(10000);
		                    } catch (InterruptedException e) {
		                        Log.v(TAG, "Write thread interrupted");
		                    }
							
		        			try{
		        				mIO.read(FFF8);
		        			}catch(InterruptedException e){
		        				Thread.interrupted();
		        			}
		        			try {Thread.sleep(10000);
		                    } catch (InterruptedException e) {
		                        Log.v(TAG, "Write thread interrupted");
		                    } 
			                
							if(onReading.bleState == i){
								i++;
								readSuccess = true;
								Log.d(TAG, "bleState correct, read check loop break");
							}
							
							if(NOTIFICATION == (byte)5&&readSuccess){
								Log.d(TAG, "NOTIFICATION 5 get, the read loop break"); 
								
								onReading.setIDcard();
								
								FFF3.setValue(new byte[]{50});
								Log.d(TAG, "write FFF3: 50");
								try{
			        				mIO.write(FFF3);
			        			}catch(InterruptedException e){
			        				Thread.interrupted();
			        			}
			        			try {Thread.sleep(10000);
			                    } catch (InterruptedException e) {
			                        Log.v(TAG, "Write thread interrupted");
			                    }
								
								NOTIFICATION = (byte)0;
								onReading.readIndex = 0;
								break;
							}
							
							if(NOTIFICATION == (byte)6&&readSuccess){
								Log.d(TAG, "NOTIFICATION 6 get, the read loop break"); 
								
								FFF3.setValue(new byte[]{50});
								Log.d(TAG, "write FFF3: 50");
								try{
			        				mIO.write(FFF3);
			        			}catch(InterruptedException e){
			        				Thread.interrupted();
			        			}
			        			try {Thread.sleep(10000);
			                    } catch (InterruptedException e) {
			                        Log.v(TAG, "Write thread interrupted");
			                    }
			        			
								onReading.setIDcard();
								break;
							}
						}//the loop over
	    			}
				}//the page loop over
				NOTIFICATION = (byte)0;
				disconnect();
				if(onReading.readIndex != 0) onReading.toIDcards();
				else{
					mWritable.onReadIDCard(null);
				}
			}
		}
	}

	//read strings
	private void read(){
		
		Runnable readRunnable = new ReadRunnable();
		readThread = new Thread(readRunnable);
		readThread.start();
	}

	private class ReadBalanceRunnable implements Runnable{
		public void run(){
			if(!mConnected){
				Runnable connectRunnable = new ConnectRunnable();
		    	connectThread = new Thread(connectRunnable);
		    	connectThread.start();
		    	try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
			}
			if(mConnected && isPasswordCorrect){
				setCommand((byte)4);
				try {Thread.sleep(500);
	            } catch (InterruptedException e) {
	            }
				disconnect();
				try {
					Thread.sleep(7000);
				} catch (InterruptedException e1) {
				}
				Runnable connectRunnable = new ConnectRunnable();
		    	connectThread = new Thread(connectRunnable);
		    	connectThread.start();
		    	try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
				}
	        	try {
					mIO.read(FFF9);
				} catch (InterruptedException e1) {
				}
				try {Thread.sleep(10000);
	            } catch (InterruptedException e) {
	            	
	            }
				disconnect();
			}
		}
	}

	private void readBalance(){
	 	Runnable readBalanceRunnable = new ReadBalanceRunnable();
		readBalanceThread = new Thread(readBalanceRunnable);
		readBalanceThread.start();
	}

	//process the read strings
    private class OnReading {
    	private byte[] bytes = null;
    	private int readIndex = 0;
    	private int index = 0;
    	private String str = null;
    	public byte bleState = 0;
    	
    	public List<IDcard> idcards = new ArrayList<IDcard>();
    	
    	public OnReading(){
    		Log.d(TAG, "onReading created");
    	}

    	public void setBytes(){
    		this.bytes = new byte[readIndex];
    	}
    	public void nextpage(){
			index = 0;
		}
    	public void reset(){
    		this.bytes = null;
    		index = 0;
    		str = null;
    		bleState = 0;
    	}

		public void addBytes(byte[] bytes){
    		Log.d(TAG, "addBytes called");
    		bleState = bytes[0];
    		Log.i(TAG, "bleState: " + String.valueOf(bleState));
    		if(bleState == 0){
    			mWritable.onReadIDCard(null);
    			readThread.stop();//TODO
    		}
    		final StringBuilder stringBuilder = new StringBuilder(bytes.length);
    		for(byte onebyte : bytes)
                stringBuilder.append(String.format("%d ", onebyte&0x0FF));
            Log.i(TAG, "tenbytes:" +  stringBuilder.toString());
            
            for(int x=1;x<10;x++) {
    			if(index<readIndex){
    				this.bytes[index] = bytes[x];
        			index++;
    			} else{
    				Log.d(TAG, "bigger than readIndex");
    				Log.i(TAG, "index: " + String.valueOf(index));
    				break;
    			}
    		}
    	}
    	public void setIDcard(){
    		Log.d(TAG, "setIDcard called");
    		byte[] onRead = this.bytes;
    		byte[] origin = new byte[onRead.length-18];
    		byte[] hash = new byte[16];
    		System.arraycopy(onRead, 2, hash, 0, 16);
    		System.arraycopy(onRead, 18, origin, 0, onRead.length-18);
    		if(Arrays.equals(hash, hash(origin))){
    			IDcard idcard = IDcard.toIDcard(origin);
    			if(idcard.isIdcardCorrect()){
    				idcards.add(idcard);
        			Log.d(TAG, "IDcard recorded!");
    			}else{
    				Log.w(TAG, "IDcard wrong!");
    			}
    		}else{
    			Log.d(TAG, "HASH WRONG!");
    		}
    	}
    	public void toIDcards(){
    		IDcard[] idcards =
    				((List<IDcard>) this.idcards).toArray(new IDcard[this.idcards.size()]);
			mWritable.onReadIDCard(idcards);
    	}
    	public void onReadBalance(byte[] origin){
    		byte[] balance = new byte[4];
    		System.arraycopy(origin, 0, balance, 0, 4);
			mWritable.onReadBalance(balance);
    	}
    	public String toString(){
    		final StringBuilder stringBuilder = new StringBuilder(this.bytes.length);
    		for(byte bytes : this.bytes)
                stringBuilder.append(String.format("%d ", bytes&0x0FF));
            Log.i(TAG, "input data:" +  stringBuilder.toString());
    		str = new String(this.bytes);
    		return str;
    	}
    }

    public final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pushNotification();
        }
    };

    private static IntentFilter mIntentFilter(){
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        return intentFilter;
    }
    /////////////////////////External interface///////////////////////
    /**
     * 向设备发送一张名片。
     * @param wrt 这个参数没有意义，应该删除
     * @param idcard 要发送的名片
     */
	public void sendIDcard(Writable wrt, IDcard idcard){
		mWritable = wrt;
		writeIDcardTemp = idcard;
		
		byte[] origin = idcard.toBytes();
		byte[] hash = hash(origin);
		byte[] head = new byte[2];
		int length = origin.length + hash.length +2;
		head[0] = (byte) ((length >> 8) & 0xFF);
		head[1] = (byte) (length & 0xFF);
		byte[] mix = new byte[length];
		System.arraycopy(head, 0, mix, 0, 2);
		System.arraycopy(hash, 0, mix, 2, 16);
		System.arraycopy(origin, 0, mix, 18, origin.length);
		write(mix);
	}

	/**
	 * 读取设备中保存的IDcard，并通过wrt提供的接口输出。
	 * @param wrt
	 */
	public void readIDcard(Writable wrt){
		mWritable = wrt;
		onReading.reset();
		read();
	}

	/**
	 * 读取公交卡的余额，并通过wrt提供的接口输出。
	 * @param wrt
	 */
	public void readBalance(Writable wrt){
		mWritable = wrt; 
		onReading.reset();
		readBalance();
	}
	
	/**
	 * 设置设备地址。这个地址是连接的关键，请确保在连接前这个地址已经设置正确。
	 * 这个地址在停止该蓝牙服务时会丢失。所以需要在每次运行该服务时重新设置。
	 * @param address
	 */
	public void setAddress(String address){
		mDeviceAddress = address;
	}
	/**
	 * 返回程序中保存的设备地址。
	 * @return 程序中保存的设备地址
	 */
	public String getAddress(){
		return mDeviceAddress;
	}
	/**
	 * 验证地址是否为空
	 * @return true 若地址不为空
	 * @return false 若地址为空
	 */
	public boolean isAddressSet(){
		if(mDeviceAddress == null) return false;
		else return true;
	}
	
	/**
	 * 设置程序内部保存的密码。这个密码在每次连接设备时都需要验证。
	 * 请确保这个密码和设备的地址匹配，否则将在连接时返回密码错误。
	 * @param psw
	 */
	public void setPassword(byte[] psw){
		KEY = psw;
	}
	
	/**
	 * 更改设备内存储的密码。
	 * @param oriPsw 原密码
	 * @param newPsw 新密码
	 */
	public void resetPassword(final byte[] oriPsw, final byte[] newPsw, Callbackable cbk){
        mCallbackable = cbk;
		resetPasswordThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				if(!mConnected){
					Runnable connectRunnable = new ConnectRunnable();
			    	connectThread = new Thread(connectRunnable);
			    	connectThread.start();
			    	try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
                        Log.d(TAG, "connect thread interrupted, reset password thread interrupted");
					}
				}
				if(mConnected && isPasswordCorrect){
					FFF6.setValue(oriPsw);
					try {
						mIO.write(FFF6);
						} catch (InterruptedException e1) {
							
						}
					try {Thread.sleep(10000);
		            }catch(InterruptedException e) {
		            	if(NOTIFICATION == 1){
                            Log.d(TAG, "Set password success.");
							mCallbackable.onSetPassword(true);
		            	}else{
                            Log.d(TAG, "NOTIFICATION NOT 1, set password fail.");
                            mCallbackable.onSetPassword(false);
		            	}
                        disconnect();
                        return;
		            }
                    Log.d(TAG, "Set password timeout, fail.");
                    mCallbackable.onSetPassword(false);
                    disconnect();
				}
				
			}
		});
		resetPasswordThread.start();
	}

	/**
	 * 向设备推送通知。
	 * 
	 */
	public void pushNotification(){
		pushNotificationThread = new Thread(new Runnable() {

			@Override
			public void run() {
                try {
                    if(writeThread != null) writeThread.join();
                    if(readThread != null) readThread.join();
                    if(readBalanceThread != null) readBalanceThread.join();

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!mConnected){
					Runnable connectRunnable = new ConnectRunnable();
			    	connectThread = new Thread(connectRunnable);
			    	connectThread.start();
			    	try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
					}
				}

				if(mConnected&&isPasswordCorrect){
					setCommand((byte)5);
					try {Thread.sleep(10000);
		            } catch (InterruptedException e) {
		            	Log.d(TAG, "pushNotificationThread interrupted, mission complete.");
		            }

				}
			}
		});
		pushNotificationThread.start();
	}

	/**
	 * 更改在设备内存储的统一认证账号和密码。
	 * @param schoolNum 统一认证账号
	 * @param password 统一认证密码
	 */
	public void writeUniauth(String schoolNum, String password){
		writeUniauthThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				
				if(!mConnected){
					Runnable connectRunnable = new ConnectRunnable();
			    	connectThread = new Thread(connectRunnable);
			    	connectThread.start();
			    	try {
						Thread.sleep(10000);
					} catch (InterruptedException e1) {
					}
				}
				
				if(mConnected && NOTIFICATION == 9){
					
				}
			}
		});
		writeUniauthThread.start();
	}
}