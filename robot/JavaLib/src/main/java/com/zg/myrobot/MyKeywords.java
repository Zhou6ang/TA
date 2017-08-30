package com.zg.myrobot;

import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;

@RobotKeywords
public class MyKeywords{
	
	@RobotKeyword
	public int min(int a,int b){
		return a+b;
	}

	@RobotKeyword("some doc here")
	@ArgumentNames({ "str" })
	public int add(String str){
		return 111;
	}
}
