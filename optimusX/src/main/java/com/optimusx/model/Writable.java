package com.optimusx.model;

import com.example.android.bluetoothlegatt.IDcard;

public interface Writable {
	/*
	 * onReadIDCard接口
	 * 用于存放读取到的多个IDcard对象
	 * 调用时传入所获得的IDcard[]对象*/
	public void onReadIDCard(IDcard[] idc);

    /*
	 * onReadIDCard接口
	 * 用于存放读取到的多个IDcard对象
	 * 调用时传入所获得的IDcard[]对象*/
    public void onSendIDCard(boolean isSuccess);

	/*
	 * onReadBalance接口
	 * 用于读取公交卡余额
	 * 调用时传入所获得的byte[]对象*/
	public void onReadBalance(byte[] arrayBalance);
}
