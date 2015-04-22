package com.example.OptimusxAct;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.example.OptimusxAct.Guide.GuideActivity;


public class LogoActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);

        //读取SharedPreferences中需要的数据,使用SharedPreferences来记录程序启动的使用次数
        SharedPreferences preferences = getSharedPreferences(getString(R.string.device_preferences), MODE_PRIVATE);
        //取得相应的值,如果没有该值,说明还未写入,用true作为默认值
        boolean isFirstIn = preferences.getBoolean("isFirstIn", true);
        Handler mHandler = new Handler();
        //判断程序第几次启动
        if (!isFirstIn) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LogoActivity.this, StartActivity.class);
                    LogoActivity.this.startActivity(intent);
                    LogoActivity.this.finish();
                }
            }, 2000);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(LogoActivity.this, GuideActivity.class);
                    LogoActivity.this.startActivity(intent);
                    LogoActivity.this.finish();
                }
            }, 2000);
        }

        // 提交修改使
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isFirstIn", false);
        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_logo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
