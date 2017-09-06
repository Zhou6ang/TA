package zg.toolkit;

import java.io.IOException;
import java.util.Arrays;

import zg.toolkit.fm.FMApp;
import zg.toolkit.fm.aom.AoMApp;

public class App {

	public static void main(String[] args) throws IOException {
		if (args.length <= 1) {
			System.out.println("##############################################################");
			System.out.println("#            Files Comparator Tool                           #");
			System.out.println("#                                                            #");
			System.out.println("#  Usage: java -jar file-comparator.jar -man/-aom -h         #");
			System.out.println("#                                                            #");
			System.out.println("##############################################################");
			System.exit(-1);
		}

		switch (args[0]) {
		case "-man":
			FMApp.run(Arrays.copyOfRange(args, 1, args.length));
			break;
		case "-aom":
			AoMApp.run(Arrays.copyOfRange(args, 1, args.length));
			break;
		default:
			throw new RuntimeException("Not supported command: " + args[0]);
		}
	}

}
