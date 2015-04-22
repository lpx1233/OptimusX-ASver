package com.example.OptimusxAct.Setting;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.example.OptimusxAct.R;

public class SettingFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_setting, container, false);
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(), R.layout.list_view_item_text_view, new String[]{"个人信息编辑", "登陆到OptimusX.com", "意见反馈", "关于"});
		
		ListView listView = (ListView) rootView.findViewById(R.id.act_setting_lv);
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.v("MRA", "onItemClick");
				switch(position){
                    case 0:{
                        //edit personal info
                        Intent intent = new Intent(getActivity().getApplicationContext(), EditPersonnalInfoActivity.class);
                        startActivity(intent);
                    }break;
                    case 1:{
                        //login to OptimusX.com
                        Intent intent = new Intent(getActivity().getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    }break;
                    case 2: {
                        //feedback
                        Intent intent = new Intent(getActivity().getApplicationContext(), FeedbackActivity.class);
                        startActivity(intent);
                    }break;
                    case 3:{
                        //about
                        Intent intent = new Intent(getActivity().getApplicationContext(), AboutActivity.class);
                        startActivity(intent);
                    }break;
				}
			}

        });
        return rootView;
	}
/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.setting, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}*/
}
