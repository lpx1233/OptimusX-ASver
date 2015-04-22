package com.example.android.bluetoothlegatt;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.example.android.bluetoothlegatt.DeviceControlActivity;

import android.util.Log;

public class IDcard implements Serializable {
	private final static transient String TAG = IDcard.class.getSimpleName();
	
	public String n = null;//name
	public String s = null;//school number
	public String p = null;//phone number
	public String wxID = null;
	public String wbID = null;
	public String qqID = null;
	public String tag = null;
	
	public IDcard(){
		//null
	}
	public IDcard(String name){
		this.n = name;
	}
	
	public boolean setName(String name){
		if(name.length() < 0) {
			Log.d(TAG, "name length = 0");
			return false;
		}else if(name.length() > 4){
			Log.d(TAG, "name length too long");
			return false;
		}else{
			this.n = name;
			Log.d(TAG, "set name successfully");
			return true;
		}
	}
	
	public boolean setSchoolNumber(String schoolNumber){
		if(schoolNumber.length() != 8) {
			Log.d(TAG, "school number is not 8");
			return false;
		}else{
			this.s = schoolNumber;
			Log.d(TAG, "set schoolNumber successfully");
			return true;
		}
	}
	
	public boolean setPhoneNumber(String phoneNumber){
		if(phoneNumber.length() != 11) {
			Log.d(TAG, "phone number is not 13");
			return false;
		}else{
			this.p = phoneNumber;
			Log.d(TAG, "set schoolNumber successfully");
			return true;
		}
	}
	
	public void setWxID(String wxID){
		this.wxID = wxID;
	}
	
	public void setWbID(String wbID){
		this.wbID = wbID;
	}
	
	public void setQqID(String qqID){
		this.qqID = qqID;
	}
	
	public void setTag(String tag){
		this.tag = tag;
	}
	
	public byte[] toBytes(){
		String str;
		byte[] bytes = null;
		str = n + "$" + p + "$" + s + "$" + wxID + "$" + wbID + "$" + qqID + "$" + tag;
		try {
			bytes = str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return bytes;
	}
	
	public static IDcard toIDcard(byte[] bytes){
		IDcard idcard = new IDcard();
		String str = null;
		try {
			str = new String(bytes, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String[] strArray = str.split("\\$");
		idcard.n = strArray[0];
		idcard.p = strArray[1];
		idcard.s = strArray[2];
		idcard.wxID = strArray[3];
		idcard.wbID = strArray[4];
		idcard.qqID = strArray[5];
		idcard.tag = strArray[6];
		return idcard;
	}
	
	public boolean isIdcardCorrect(){
		if(n != null && p != null && s != null) return true;
		else return false;
	}
}
