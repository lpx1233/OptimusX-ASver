package com.optimusx.model;

/**
 * Created by qiji on 2015/4/19.
 */
public interface Callbackable {
    /*
     * onSetPassword
     * 用于返回更改密码的结果
     * isPasswordSet为true时，更改成功，反之更改失败
     */
    public void onSetPassword(boolean isPasswordSet);
}
