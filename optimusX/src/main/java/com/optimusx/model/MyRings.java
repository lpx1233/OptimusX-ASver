package com.optimusx.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//using the singleton design pattern
//to manage my registered rings
public class MyRings {
	private static final MyRings myRings = new MyRings();
	//一个Map代表一个戒指
	private static final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	
	private MyRings(){
		
	}
	
	public static MyRings getMyRings(){
		return myRings;
	}
	//由name和SID添加一个戒指
	public static void addRing(String name, String SID, String pwd){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("SID", SID);
		map.put("pwd", pwd);
		list.add(map);
	}
	//由name和SID删除一个戒指
	public static void deleteRing(String name, String SID){
		for(Map<String, Object> map : list){
			if( ((String)map.get("name")).equals(name) && ((String)map.get("SID")).equals(SID)){
				list.remove(map);
				break;
			}
		}
	}
	
	//获得现有戒指个数
	public static int getRingsNum(){
		return list.size();
	}
	
	public static List<Map<String, Object>> getMyRingsList(){
		return list;
	}
}
