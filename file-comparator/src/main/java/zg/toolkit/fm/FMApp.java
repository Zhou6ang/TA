package zg.toolkit.fm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Hello world!
 *
 */
public class FMApp 
{
	private static String RightStr = "Right";
	private static String LeftStr = "Left";
    public static void main( String[] args ) throws IOException
    {
    	
        run(args);
//        run(new String[]{"D:\\userdata\\ganzhou\\Desktop\\CSCF\\man-converter-17.06-release\\man-converter-17.06\\zip\\adaptation_com.nsn.cscf.man-18.0C-20170817T024928\\amanual","D:\\userdata\\ganzhou\\Desktop\\CSCF\\Ines_o2ml_com.nsn.cscf-18.0C\\adaptation_com.nsn.cscf.man-18.0C-20170810T184726\\amanual"});
    }
    
    
    public static void run(String[] args) throws IOException {
    	if(args.length != 2 && args.length != 4){
        	System.out.println("#################################################################################################");
        	System.out.println("#            Man files comparator tool                                                          #");
        	System.out.println("#                                                                                               #");
        	System.out.println("# usage: java -jar file-comparator*.jar -man /var/../left.man /var/../right.man                 #");
        	System.out.println("#        java -jar file-comparator*.jar -man /var/../left/dir /var/../right/dir                 #");
        	System.out.println("#        java -jar file-comparator*.jar -man /var/../left.man alias-A /var/../right.man alias-B #");
        	System.out.println("#        java -jar file-comparator*.jar -man /var/../left/dir alias-A /var/../right/dir alias-B #");
        	System.out.println("#################################################################################################");
        	System.exit(-1);
        }
    	Path leftPath = Paths.get(args[0]);
        Path rightPath = Paths.get(args[1]);
        String start = " "+LeftStr+": "+args[0];
    	String end = RightStr+": "+args[1];
    	if(args.length == 4){
    		LeftStr = args[1];
    		RightStr = args[3];
    		rightPath = Paths.get(args[2]);
    		end = " "+RightStr+": "+args[2];
    		
    	}
		int show = start.length() > end.length() ? start.length() : end.length();
    	System.out.println(createPlaceHolder(show+2,"#"));
    	System.out.println(createPlaceHolder(show," "));
        System.out.println(start);
        System.out.println(createPlaceHolder((show/2 -1), " ")+"VS"+createPlaceHolder((show/2-1), " "));
        System.out.println(end);
        System.out.println(createPlaceHolder(show," "));
        System.out.println(createPlaceHolder(show+2,"#"));
        if(Files.isDirectory(leftPath) && Files.isDirectory(rightPath)){
        	compare2directory(leftPath,rightPath);
        }else if(Files.isRegularFile(leftPath) && Files.isRegularFile(rightPath)){
        	compare2files(leftPath,rightPath);
        }else{
        	System.out.println("The 2 parameters are not consistent, both of them should be File or Directory.");
        	System.exit(-1);
        }
	}


	private static void compare2directory(Path leftPath, Path rightPath) throws IOException {
		Map<Path, Path> leftmap = getAllRegularSubFiles(leftPath);
		Map<Path, Path> rightmap = getAllRegularSubFiles(rightPath);
		
		SetView<Path> common = Sets.intersection(leftmap.keySet(), rightmap.keySet());
		SetView<Path> onlyOnLeft = Sets.difference(leftmap.keySet(), rightmap.keySet());
		SetView<Path> onlyOnRight = Sets.difference(rightmap.keySet(), leftmap.keySet());

		String show = common.toString();
		System.out.println("Common files: " + ( show.length() > 200 ? show.substring(0, 200)+" ..." : show));
		System.out.println(RightStr+" missing files: " + onlyOnLeft);
		System.out.println(LeftStr+" missing files: " + onlyOnRight);

		for (Path comm : common) {
			compare2files(leftmap.get(comm), rightmap.get(comm));
		}
	}


	private static Map<Path, Path> getAllRegularSubFiles(Path leftPath) throws IOException {
		return Files.list(leftPath).filter(p -> Files.isRegularFile(p))
				.collect(Collectors.toMap(Path::getFileName, (Path v) -> v));
	}


	public static void compare2files(Path leftPath,Path rightPath) throws IOException{
		List<String> list1 = Files.readAllLines(leftPath);
		List<String> list2 = Files.readAllLines(rightPath);
		String show = "--------"+LeftStr+":[" + leftPath.getFileName() + "] VS "+RightStr+":["+rightPath.getFileName()+"]-------";
		System.out.println(show);
		int i = 0;
		while(true){
			if (i >= list1.size() && i >= list2.size()) {
				break;
			}else if(i >= list1.size() && i < list2.size()){
				
				String right = list2.get(i);
				Map<String,String> leftMap = Maps.newHashMap();
				Map<String,String> rightMap = getLineContent(right);
				showDiff(leftMap,rightMap,i);
			}else if(i < list1.size() && i >= list2.size()){
				
				String left = list1.get(i);
				Map<String,String> leftMap = getLineContent(left);
				Map<String,String> rightMap = Maps.newHashMap();
				showDiff(leftMap,rightMap,i);
			}else if(i < list1.size() && i < list2.size()){
				String left = list1.get(i);
				String right = list2.get(i);
				Map<String,String> leftMap = getLineContent(left);
				Map<String,String> rightMap = getLineContent(right);
				showDiff(leftMap,rightMap,i);
			}else{
				throw new RuntimeException("Internal error. Line:"+i);
			}
			i++;
		}
		System.out.println(createPlaceHolder(show.length(),"-"));
	}


	private static String createPlaceHolder(int length, String symbol) {
		StringBuilder sb = new StringBuilder();
		IntStream.range(0,length).forEach(e->sb.append(symbol));
		return sb.toString();
	}
	
	private static void showDiff(Map<String, String> leftMap, Map<String, String> rightMap, int line) {
		MapDifference<String, String> mapDiff = Maps.difference(leftMap, rightMap);
		if(!mapDiff.areEqual()){
			
			System.out.println("Line["+line+"] "+LeftStr+" missing: "+mapDiff.entriesOnlyOnRight() + "    "+RightStr+" missing: "+mapDiff.entriesOnlyOnLeft()+"    Value diff: "+mapDiff.entriesDiffering());
			
//			System.out.println("Line["+line+"] "+"Left missing: "+mapDiff.entriesOnlyOnRight());
//			System.out.println("        Right missing: "+mapDiff.entriesOnlyOnLeft());
//			System.out.println("        Value diff: "+mapDiff.entriesDiffering());
		}
	}


	private static Map<String, String> getLineContent(String one) {
		Map<String,String> map = new HashMap<String, String>();
		Matcher matcher = Pattern.compile("([^ ]+)=(\"[^\"]+\")").matcher(one);
		while(matcher.find()){
//			String clazzMethod = matcher.group(0);
//			System.out.println(clazzMethod);
			map.put(matcher.group(1), matcher.group(2));
		}
		return map;
	}
}
