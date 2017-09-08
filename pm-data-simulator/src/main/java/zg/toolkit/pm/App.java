package zg.toolkit.pm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.nokia.oss.pm.pmb2.PMBasic;
import com.nsn.oss.o2ml2koa.conversionApi.ConversionFactory;
import com.nsn.oss.o2ml2koa.schemas.koala.Adaptation;
import com.nsn.oss.pmbv2koa.converter.Pmbv2koaApplication;
import com.nsn.oss.pmbv2koa.converter.Pmbv2koaConversionIF;

import zg.toolkit.pm.db.DBConnection;
import zg.toolkit.pm.omes.OmesGenerater;
import zg.toolkit.ssh.SSHLibrary;

public class App {

	public static void main(String[] args) throws Exception {
//		args = new String[10];
//		args[0] = "D:\\userdata\\ganzhou\\Desktop\\PM\\Ines_o2ml_com.nsn.cscf-17.0VI\\adaptation_com.nsn.cscf.pmb-17.0VI-20170211T152226";
//		args[1] = "D:\\userdata\\ganzhou\\Desktop\\PM\\Ines_o2ml_com.nsn.cscf-17.0VI\\adaptation_com.nsn.cscf.pmb-17.0VI-20170211T152226\\";
//		args[2] = "PLMN-AUTO/CSCF-ines-17.0VI";
//		args[3] = "omc";
//		args[4] = "omc";
//		args[5] = "10.92.18.156";
//		args[6] = "D:\\userdata\\ganzhou\\Desktop\\PM\\Ines_o2ml_com.nsn.cscf-17.0VI\\adaptation_com.nsn.cscf.pmb-17.0VI-20170211T152226";
//
//		String pmbDir = args[0];
//		String supplFile = args[1];
//		String dn = args[2];
//		String username = args[3];
//		String pwd = args[4];
//		String host = args[5];
//		String omesOuput = args[6];
		
//		String sqlCommand = "sqlplus omc/omc@10.92.18.156/oss << EOF\r\nALTER SESSION SET TIME_ZONE = '+08:00';\r\nselect count(*) as result from imscsf_p_meas_network_o2 a, utp_common_objects b where a.global_id = b.co_gid and b.co_dn like 'PLMN-AUTO/CSCF-ines-17.0VI%' and a.starttime = TIMESTAMP '2017-08-30 08:45:00+08:00'  order by a.starttime desc;\r\nEOF\r\n";
//		System.out.println(SSHLibrary.exec(sqlCommand, new SSHLibrary.SSHInfo("omc","omc","10.92.18.156")));
		
		
		if(args.length != 7){
        	System.out.println("####################################################################################################");
        	System.out.println("#            PM Real Data Simulator Tool                                                           #");
        	System.out.println("#                                                                                                  #");
        	System.out.println("# Please input 7 parameters as below.                                                              #");
        	System.out.println("#                                                                                                  #");
        	System.out.println("# Usage: java -jar app.jar <pmb-dir> <suppl-dir> <dn> <omc> <pwdOfOmc> <hostOfDB> <OMesOutput-dir> #");
        	System.out.println("#   e.g. java -jar app.jar /var/pmb/ /var/supp/xx.xml PLMN-XX/XX-17.0 omc omc 0.0.0.0 ./omes/      #");
        	System.out.println("#                                                                                                  #");
        	System.out.println("####################################################################################################");
        	System.exit(-1);
        }
		String pmbDir = args[0];
		String supplFile = args[1];
		String dn = args[2];
		String username = args[3];
		String pwd = args[4];
		String host = args[5];
		String omesOuput = args[6];
		new App().execute(pmbDir, supplFile, dn, username, pwd, host, omesOuput);
	}
	
	public void execute(String pmbDir,String supplFile,String dn,String username,String pwd,String host,String omesOuput) throws Exception{
		if(!"omc".equalsIgnoreCase(username)){
			throw new RuntimeException("User should be 'omc'.");
		}
		int dataSimulateCount = 5;
		//step-1:generate OMes files.
		OmesGenerater generator = new OmesGenerater(dataSimulateCount);
		String xmlPath = generator.generate(pmbDir, dn, omesOuput);
		ZonedDateTime datetime = generator.getStartTimeStamp();
		PMBasic pmb = generator.getPMBasic();
		
		//step-2:upload to DB node.
		try(SSHLibrary ssh = new SSHLibrary(username,pwd,host)){
			String s = Paths.get(xmlPath).getFileName().toString();
			ssh.sftpUploadWithinSession(xmlPath, s);
			ssh.execWithinSession("mv "+s+" /var/opt/nokia/oss/global/mediation/south/pm/import/com.nsn.oss.mediation.south.ne3soappm");
		}
		
		//step-3:check DB and put into a map.
//		Thread.sleep(10000);//waiting for ETload consuming OMes files.
		Pmbv2koaConversionIF pmbv2Convertor = new Pmbv2koaApplication();
		String pmwh = null;//currently not supported pmwh.
		Path suppPath = Paths.get(supplFile);
		if(Files.isDirectory(suppPath)){
			suppPath = Files.find(suppPath, 1, (a,b)->a.toString().endsWith("supplementary.xml")).findFirst().get();
		}
		
		List<Adaptation> adap = pmbv2Convertor.doConversion(Arrays.asList(pmbDir), suppPath.toString(), pmwh);
		if(adap.isEmpty()){
			throw new RuntimeException("Could not found koala file.");
		}
		DBConnection db = new DBConnection(username,pwd,host);
		String subsystem = ConversionFactory.createO2ml2koaConverter().getSubsystemName(suppPath.toString());
		
		//step-4:assert result based on map.
		//key: OMesName of omesfile in NE side. value: count of measurement(table) in NetAct side.
		Map<String,Integer> data = db.getDataFromDB(adap.get(0),dn,subsystem,datetime);
		assertData(dataSimulateCount,data,pmb);
	}

	private void assertData(int i, Map<String, Integer> data,PMBasic pmb) {
		data.forEach((a,b)->{
			System.out.println(a+"->"+b);
//			if(b.intValue() != i){
//				throw new RuntimeException("The PM data of measurement:["+a+"] is not correctly, please check.");
//			}
		});
		System.out.println("Total Measurements in DB:"+data.size());
		System.out.println("Total Measurements in PMB:"+pmb.getMeasurement().size());
	}
}

