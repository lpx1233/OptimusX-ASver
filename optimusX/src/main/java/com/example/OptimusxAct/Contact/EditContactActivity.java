package com.example.OptimusxAct.Contact;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.example.OptimusxAct.R;
import com.example.OptimusxAct.StartActivity;
import com.example.android.bluetoothlegatt.IDcard;
import com.optimusx.contacts.ContactsDatabaseHelper;

public class EditContactActivity extends ActionBarActivity {

    private EditText mName;
    private EditText mPhoneNum;
    private EditText mSchNum;
    private EditText mWeixin;
    private EditText mWeibo;
    private EditText mQQ;
    private EditText mLabel;
    private Toolbar mToolbar;
    private ContactsDatabaseHelper cdHelper;
    private int position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //deal with views
        setContentView(R.layout.activity_edit_personnal_info);
        mName = (EditText) findViewById(R.id.act_view_contact_name);
        mPhoneNum = (EditText) findViewById(R.id.phone);
        mSchNum = (EditText) findViewById(R.id.schnum);
        mWeixin = (EditText) findViewById(R.id.weixin);
        mWeibo = (EditText) findViewById(R.id.weibo);
        mQQ = (EditText) findViewById(R.id.qq);
        mLabel = (EditText) findViewById(R.id.label);
        mToolbar = (Toolbar) findViewById(R.id.act_edit_info_toolbar);

        //deal with info
        Bundle bundle=this.getIntent().getExtras();
        if(bundle != null){
            position = bundle.getInt("position", 0);
        }

        //deal with toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_action_back);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //deal with database
        cdHelper = new ContactsDatabaseHelper(this, "Contacts.db", null, 4);
        SQLiteDatabase db = cdHelper.getWritableDatabase();

        //set personal info
        mName.setText(cdHelper.getContactName(db, position));
        mPhoneNum.setText(cdHelper.getContactPhoneNumber(db, position));
        mSchNum.setText(cdHelper.getContactSchoolNumber(db, position));
        mWeixin.setText(cdHelper.getContactWxID(db, position));
        mWeibo.setText(cdHelper.getContactWbID(db, position));
        mQQ.setText(cdHelper.getContactQqID(db, position));
        mLabel.setText(cdHelper.getContactTag(db, position));
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
                //save idcard to database
                IDcard myIdc = new IDcard();
                myIdc.setName(mName.getText().toString());
                myIdc.setPhoneNumber(mPhoneNum.getText().toString());
                myIdc.setSchoolNumber(mSchNum.getText().toString());
                myIdc.setWxID(mWeixin.getText().toString());
                myIdc.setWbID(mWeibo.getText().toString());
                myIdc.setQqID(mQQ.getText().toString());
                myIdc.setTag(mLabel.getText().toString());
                cdHelper.addContact(cdHelper.getWritableDatabase(), myIdc);

                //go back to home
                Intent intent=new Intent(EditContactActivity.this, StartActivity.class);
                Bundle bundle = new Bundle();
                bundle.putInt("mPos", 1);
                intent.putExtras(bundle);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
