

import org.robotframework.javalib.annotation.ArgumentNames;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.javalib.library.AnnotationLibrary;


public class MyLibrary extends AnnotationLibrary{
	
	public static final String ROBOT_LIBRARY_SCOPE = "GLOBAL";
	public MyLibrary() {
		super("com/zg/myrobot/**.class");
	}
	
	

	
	
}
