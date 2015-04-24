package com.example.OptimusxAct.Contact;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.example.OptimusxAct.R;
import com.optimusx.contacts.ContactsDatabaseHelper;

public class ViewContactActivity extends ActionBarActivity {

	private Toolbar mToolbar;
	private TextView mTVName;
	private TextView mTVSchoolNumber;
	private TextView mTVPhoneNumber;
	private ContactsDatabaseHelper cdHelper;
	private static int position = 0;
    private TextView mTVWeiXin;
    private TextView mTVWeibo;
    private TextView mTVQQ;
    private TextView mTVLabel;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//deal with layout and views
		setContentView(R.layout.activity_view_contact);
		mTVName = (TextView) findViewById(R.id.act_view_contact_name);
		mTVSchoolNumber = (TextView) findViewById(R.id.act_view_contact_school_number);
		mTVPhoneNumber = (TextView) findViewById(R.id.act_view_contact_phone_number);
        mTVWeiXin = (TextView) findViewById(R.id.act_view_contact_weixin);
        mTVWeibo = (TextView) findViewById(R.id.act_view_contact_weibo);
        mTVQQ = (TextView) findViewById(R.id.act_view_contact_qq);
        mTVLabel = (TextView) findViewById(R.id.act_view_contact_label);

		//deal with ContactsDatabaseHelper
		cdHelper = new ContactsDatabaseHelper(this, "Contacts.db", null, 4);
		
		//deal with Toolbar
		mToolbar = (Toolbar) findViewById(R.id.act_view_contact_toolbar);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mToolbar.setNavigationIcon(R.drawable.ic_action_back);
		mToolbar.setNavigationOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		//deal with info
        Bundle bundle=this.getIntent().getExtras();
        if(bundle != null){
        	position = bundle.getInt("position", 0);
		}
		
        mTVName.setText(cdHelper.getContactName(cdHelper.getWritableDatabase(), position));
        mTVSchoolNumber.setText(cdHelper.getContactSchoolNumber(cdHelper.getWritableDatabase(), position));
        mTVPhoneNumber.setText(cdHelper.getContactPhoneNumber(cdHelper.getWritableDatabase(), position));
        mTVWeiXin.setText(cdHelper.getContactWxID(cdHelper.getWritableDatabase(), position));
        mTVWeibo.setText(cdHelper.getContactWbID(cdHelper.getWritableDatabase(), position));
        mTVQQ.setText(cdHelper.getContactQqID(cdHelper.getWritableDatabase(), position));
        mTVLabel.setText(cdHelper.getContactTag(cdHelper.getWritableDatabase(), position));
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.menu_edit){
            Intent intent = new Intent(ViewContactActivity.this, EditContactActivity.class);
            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
