package com.zg.myrobot;

import org.robotframework.RobotFramework;

/**
 * Hello world!
 *
 */


public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        new App().a();
    }
    
    public void a(){
    	String path = this.getClass().getResource("/abc.robot").getPath();
    	RobotFramework.run(new String[]{path});
    }
}
