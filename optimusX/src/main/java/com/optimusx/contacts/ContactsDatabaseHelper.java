package com.optimusx.contacts;

import com.example.android.bluetoothlegatt.IDcard;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;
/**
 * 在调用数据库的活动内声明一个private ContactsDatabaseHelper cdHelper，
 * 然后在onCreate方法中给cdHelper创建一个实例cdHelper = new ContactsDatabaseHelper(this, "Contacts.db", null, 1)，
 * 然后在任何地方执行cdHelper.getWritableDatabase。
 * 如果程序没有找到Contacts.db这个数据库，就会执行onCreate方法创建一个；
 * 如果找到了，就会返回这个数据库的SQLiteDatabase对象。
 * 这个SQLiteDatabase对象中默认创建一个名为Contacts的表，包括name,phonenumber,schoolnumber,wxID,wbID,qqID,tag七列。
 */
public class ContactsDatabaseHelper extends SQLiteOpenHelper {
	
	public static final String CREATE_DATABASE = "create table Contacts ("
			+ "id integer primary key autoincrement, "
			+ "name text, "
			+ "phonenumber text, "
			+ "schoolnumber text, "
			+ "wxID text, "
			+ "wbID text, "
			+ "qqID text, "
			+ "tag text)";
	public static final String CREATE_MY_IDCARD = "create table MyIdcard ("
			+ "id integer primary key autoincrement, "
			+ "name text, "
			+ "phonenumber text, "
			+ "schoolnumber text, "
			+ "wxID text, "
			+ "wbID text, "
			+ "qqID text, "
			+ "tag text)";
	private Context mContext;

	public ContactsDatabaseHelper(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_DATABASE);
		db.execSQL(CREATE_MY_IDCARD);
		Log.d("database helper", "DatabaseHelper onCreate() called.");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table if exists Contacts");
		db.execSQL("drop table if exists MyIdcard");
		onCreate(db);

	}
	
	public IDcard myIdcard(SQLiteDatabase db){
		IDcard idcard = new IDcard();
		Cursor cursor = db.query("MyIdcard", null, null, null, null, null, null);
		if(cursor.moveToFirst()){
				 idcard.n = cursor.getString(cursor.getColumnIndex("name"));
				 idcard.p = cursor.getString(cursor.getColumnIndex("phonenumber"));
				 idcard.s = cursor.getString(cursor.getColumnIndex("schoolnumber"));
				 idcard.wxID = cursor.getString(cursor.getColumnIndex("wxID"));
				 idcard.wbID = cursor.getString(cursor.getColumnIndex("wbID"));
				 idcard.qqID = cursor.getString(cursor.getColumnIndex("qqID"));
				 idcard.tag = cursor.getString(cursor.getColumnIndex("tag"));
			 return idcard;
		 }else{
			 Log.w("com.example.optimusx","myIdcard cursor null!");
			 ContentValues values = new ContentValues();
				values.put("id", "1");
				values.put("name", "");
				values.put("phonenumber", "");
				values.put("schoolnumber", "");
				values.put("wxID", "");
				values.put("wbID", "");
				values.put("qqID", "");
				values.put("tag", "");
				db.insert("MyIdcard", null, values);
				values.clear();
			 return idcard;
		 }
		
	}
	
	public void addMyIdcard(SQLiteDatabase db, IDcard idcard){
		ContentValues values = new ContentValues();
		values.put("name", idcard.n);
		values.put("phonenumber", idcard.p);
		values.put("schoolnumber", idcard.s);
		values.put("wxID", idcard.wxID);
		values.put("wbID", idcard.wbID);
		values.put("qqID", idcard.qqID);
		values.put("tag", idcard.tag);
		db.update("MyIdcard", values, "id = ?", new String[]{String.valueOf(1)});
		values.clear();
	}
	
	/**
	 * 
	 * @param db the SQLiteDatabase
	 * @param idcard the IDcard
	 */
	public void addContact(SQLiteDatabase db, IDcard idcard){
		ContentValues values = new ContentValues();
		
		values.put("name", idcard.n);
		values.put("phonenumber", idcard.p);
		values.put("schoolnumber", idcard.s);
		values.put("wxID", idcard.wxID);
		values.put("wbID", idcard.wbID);
		values.put("qqID", idcard.qqID);
		values.put("tag", idcard.tag);
		
		if(db.update("Contacts", values, "name = ?", new String[]{idcard.n}) == 0){
			db.insert("Contacts", null, values);
		}
		values.clear();
	}
	
	/**
	 * 
	 * @param db the SQLiteDatabase
	 * @param idcard the delete idcard
	 */
	public void deleteContact(SQLiteDatabase db, IDcard idcard){
		db.delete("Contacts", "name = ?", new String[]{idcard.n});
	}
	
	/**
	 * 
	 * @param db the SQLiteDatabase
	 */
	public IDcard[] queryAll(SQLiteDatabase db){
		Cursor cursor = db.query("Contacts",null,null,null,null,null,null);
		IDcard[] idcards = new IDcard[cursor.getCount()];
		if(cursor.moveToFirst()){
			 for(int i=0;cursor.moveToNext();i++){
				 idcards[i].n = cursor.getString(cursor.getColumnIndex("name"));
				 idcards[i].p = cursor.getString(cursor.getColumnIndex("phonenumber"));
				 idcards[i].s = cursor.getString(cursor.getColumnIndex("schoolnumber"));
				 idcards[i].wxID = cursor.getString(cursor.getColumnIndex("wxID"));
				 idcards[i].wbID = cursor.getString(cursor.getColumnIndex("wbID"));
				 idcards[i].qqID = cursor.getString(cursor.getColumnIndex("qqID"));
				 idcards[i].tag = cursor.getString(cursor.getColumnIndex("tag"));
			 }
			 return idcards;
		 }else{
			 Log.w("com.example.optimusx","queryAll cursor null!");
			 return null;
		 }
	}
	/**
	 * @param db the SQLiteDatabase
	 * 
	 */
	public int getContactCount(SQLiteDatabase db){
		Cursor cursor = db.query("Contacts",null,null,null,null,null,null);
		return cursor.getCount();
	}
	/**
	 * @param db the SQLiteDatabase
	 * @param position the position
	 * 
	 */
	public String getContactName(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"name"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("name"));
		}else{
			Log.w("com.example.optimusx","getContactName() cursor null!");
			return null;
		}
	}
	/**
	 * @param db the SQLiteDatabase
	 * @param position the position
	 * 
	 */
	public String getContactPhoneNumber(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"phonenumber"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("phonenumber"));
		}else{
			Log.w("com.example.optimusx","getContactPhoneNumber() cursor null!");
			return null;
		}
	}
	/**
	 * @param db the SQLiteDatabase
	 * @param position the position
	 * 
	 */
	public String getContactSchoolNumber(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"schoolnumber"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("schoolnumber"));
		}else{
			Log.w("com.example.optimusx","getContactSchoolNumber() cursor null!");
			return null;
		}
	}
	
	public String getContactWxID(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"wxID"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("wxID"));
		}else{
			Log.w("com.example.optimusx","getContactWxID() cursor null!");
			return null;
		}
	}
	
	public String getContactWbID(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"wbID"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("wbID"));
		}else{
			Log.w("com.example.optimusx","getContactWbIDr() cursor null!");
			return null;
		}
	}
	
	public String getContactQqID(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"qqID"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("qqID"));
		}else{
			Log.w("com.example.optimusx","getContactQqID() cursor null!");
			return null;
		}
	}
	
	public String getContactTag(SQLiteDatabase db, int position){
		Cursor cursor = db.query("Contacts",new String[]{"tag"},null,null,null,null,null);
		if(cursor.moveToPosition(position)){
			return cursor.getString(cursor.getColumnIndex("tag"));
		}else{
			Log.w("com.example.optimusx","getContactSchoolNumber() cursor null!");
			return null;
		}
	}

}
