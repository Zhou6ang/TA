package com.nokia.oss.ofas.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.validation.SchemaFactory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

import com.google.common.net.UrlEscapers;
import com.nokia.oss.mj.MeasSchedule;
import com.nokia.oss.mj.MeasSchedule.ScheduleItem;
import com.nokia.oss.ofas.AlarmNew;
import com.nokia.oss.ofas.EventType;
import com.nokia.oss.ofas.Notification;
import com.nokia.oss.ofas.ObjectFactory;
import com.nokia.oss.ofas.PerceivedSeverity;


public class App {
	private ObjectFactory obj = new ObjectFactory();
	private static javax.xml.validation.Schema schema;
	private static javax.xml.validation.Schema mj_schema;
	private Random random = new Random();
	private static List<Object> man = new ArrayList<>();
	private Runtime rt = Runtime.getRuntime();
	private String agentId = null;
	static
    {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try
        {
        	schema = sf.newSchema(App.class.getResource("/Ofas.xsd"));
        	mj_schema = sf.newSchema(App.class.getResource("/MJ.xsd"));
        }
        catch (SAXException e)
        {
            throw new RuntimeException(e);
        }
        
       
        Resource res = new ClassPathResource("man.properties");
        Properties prop = new Properties();
		try {
			prop.load(res.getInputStream());
			man.addAll(prop.values());
		} catch (IOException e) {
			 throw new RuntimeException(e);
		}
		
    }
	
	public static void showPrompt(){
		System.out.println("###########################################################################################################");
    	System.out.println("#            FM Data File Generator                                                                       #");
    	System.out.println("#                                                                                                         #");
    	System.out.println("# Please input correct parameters as below.                                                               #");
    	System.out.println("#                                                                                                         #");
    	System.out.println("# Usage: java -jar ofas-generator-cli.jar -g <input_csv_file> [output_ofas_dir]                           #");
    	System.out.println("# Note: -g indicates generate OFaS file.                                                                  #");
    	System.out.println("#       <input_csv_file> is input path of CSV file and mandantory parameter.                              #");
    	System.out.println("#       [output_ofas_dir] is output directory of OFaS file and optional parameter, cur-dir is by default. #");
    	System.out.println("# e.g. java -jar ofas-generator-cli.jar -g /var/xx.csv                                                    #");
    	System.out.println("#                                                                                                         #");
    	System.out.println("# Usage: java -jar ofas-generator-cli.jar -r <src_file> <src_interval> <tgt_interval> [measurement]       #");
    	System.out.println("# Note: -r indicates replaced <src_interval> by <tgt_interval> for file <src_file>.                       #");
    	System.out.println("#       <src_file> is a MJ file which will be done some replacement operation.                            #");
    	System.out.println("#       <src_interval> is value of src interval in souce file and it will be replaced by tgt_interval.    #");
    	System.out.println("#       <tgt_interval> is value of tgt interval and it will relace src_interval.                          #");
    	System.out.println("#       [measurement] is measurement id and optional parameter. If present, will do replacement only for  #");
    	System.out.println("#                     this measurement.                                                                   #");
    	System.out.println("# e.g. java -jar ofas-generator-cli.jar -r /var/xx/MJ.netact.xml 15 20 CPU                                #");
    	System.out.println("#                                                                                                         #");
    	System.out.println("###########################################################################################################");
    	System.exit(-1);
	}
	
	public static void main(String[] args) throws Exception {
		
//		args=new String[]{"-g","PLMN-XX/XX-17.0","D:\\github\\TA\\ofas-generator\\src\\test\\resources\\mapping.csv"};
//		args=new String[]{"-r","D:\\github\\TA\\ofas-generator\\src\\test\\resources\\1.xml","15","20","CPU"};
//		args=new String[]{"-r","D:\\github\\TA\\ofas-generator\\src\\test\\resources\\1.xml","15","20"};
		
		if(args.length < 1 || args.length > 5 ){
			showPrompt();
        }
		App app = new App();
		switch (args[0]) {
		case "-g":
			Path output = null,input = null;
			if(args.length == 3){
				output = Paths.get(args[2]);
			}else if(args.length == 2){
				//do some special things.
			}else{
				throw new RuntimeException("Input parameter error.");
			}
			input = Paths.get(args[1]);
			System.out.println(LocalDateTime.now()+" Starting to generate OFas file.");
			app.generateOFas(input, output);
			System.out.println(LocalDateTime.now()+" Ending to generate OFas file.");
			break;
		case "-r":
			String os = System.getProperty("os.name").toLowerCase();
			if(os.contains("win") || os.contains("nix") || os.contains("nux") || os.contains("aix")){
				String src, tgt, measurement = null;
				Path src_file;
				if(args.length == 5){
					src_file = Paths.get(args[1]);
					src = args[2];
					tgt = args[3];
					measurement = args[4];
				}else if(args.length == 4){
					src_file = Paths.get(args[1]);
					src = args[2];
					tgt = args[3];
				}else{
					throw new RuntimeException("Input parameter error.");
				}
				app.replaceStr(src_file,src,tgt,measurement);
				app.restartFMPMagent();
			}else{
				throw new RuntimeException("This operation only can execute in Linux.");
			}
			break;
		default:
			showPrompt();
			break;
		}
		
		System.out.println(LocalDateTime.now()+" Executing successfully!!!");
		
	}
	
	private void replaceStr(Path src_file, String src, String tgt, String measurement) throws IOException {
		if (src_file == null || src == null || src.isEmpty() || tgt == null || tgt.isEmpty()) {
			throw new RuntimeException("The value of input parameters are invalid.");
		}
		System.out.println(LocalDateTime.now() + " try to read file:"+src_file+".");
		MeasSchedule measSchedule = readMJFile(src_file.toFile());
		if (measurement != null && !measurement.isEmpty()) {
			Optional<ScheduleItem> opt = measSchedule.getScheduleItem().parallelStream()
					.filter(e -> measurement.equalsIgnoreCase(e.getMeasurements().getMeasurement())).findFirst();
			if (opt.isPresent()) {
				ScheduleItem si = opt.get();
				if (si.getMeasPeriods().getPeriod().getInterval() == Integer.parseInt(src)) {
					si.getMeasPeriods().getPeriod().setInterval(Integer.parseInt(tgt));
					System.out.println(LocalDateTime.now() + " replacing {" + src + "} to {" + tgt
							+ "} for interval of measurement:{" + measurement + "} successfully.");
				} else {
					throw new RuntimeException(
							" the source interval:{" + src + "} is not found in measurement:{" + measurement + "}.");
				}
			} else {
				throw new RuntimeException(" the measurement:{" + measurement + "} is not found in " + src_file + ".");
			}
		} else {
			int count = 0;
			for (ScheduleItem e : measSchedule.getScheduleItem()) {
				if (e.getMeasPeriods().getPeriod().getInterval().intValue() == Integer.parseInt(src)) {
					e.getMeasPeriods().getPeriod().setInterval(Integer.parseInt(tgt));
					System.out.println(LocalDateTime.now() + " replacing {" + src + "} to {" + tgt
							+ "} for interval of measurement:{" + e.getMeasurements().getMeasurement() + "} successfully.");
					count++;
				}
			}
			System.out.println(LocalDateTime.now() + " total replaced:"+count+" in "+src_file+".");
		}

		Files.copy(src_file, Paths.get(src_file.toString()+".backup"));
		System.out.println(LocalDateTime.now() + " replacing operation has been done in "+src_file+".");
		doMarshallerFor(measSchedule, src_file.toFile());
	}
	
	private void restartFMPMagent() throws IOException, InterruptedException{
		int rlt = executeCMD("ps -aux|grep /opt/cafbase/esymac",true);
		if(rlt != 0){
			System.out.println(LocalDateTime.now()+" Could not found process: /opt/cafbase/esymac");
		}
		rlt = executeCMD("ll /opt/SMAW/SMAWrtp/bin/RtpEsymac.pl",true);
		if (rlt != 0) {
			System.out.println(LocalDateTime.now()+" Could not found /opt/SMAW/SMAWrtp/bin/RtpEsymac.pl file.");
		}
		executeCMD("/opt/SMAW/SMAWrtp/bin/RtpEsymac.pl -stop");
		executeCMD("/opt/SMAW/SMAWrtp/bin/RtpEsymac.pl -start");
	}

	private int executeCMD(String command) throws IOException, InterruptedException {
		return executeCMD(command, false);
	}
	private int executeCMD(String command,boolean ignoreExitCode) throws IOException, InterruptedException {
        Process proc = rt.exec(command);
        String msg = getMsg(proc.getInputStream());
        System.out.println(msg);
        String errMsg = getMsg(proc.getErrorStream());
        System.out.println(errMsg);
        int exitVal = proc.waitFor();
        if(proc.isAlive()){
        	proc.destroy();
        }
        System.out.println(LocalDateTime.now()+" process exit-code: " + exitVal);
        if(!ignoreExitCode){
        	if (exitVal != 0) {
    			throw new RuntimeException("CMD:[" + command + "] execution failed.");
    		}
        }
        
		return exitVal;
	}

	private String getMsg(InputStream inputStream) throws IOException  {
		byte[] b = new byte[1024];
		int i = 0;
		StringBuilder sb = new StringBuilder();
		while((i = inputStream.read(b)) > 0){
			sb.append(new String(b,0,i));
		}
		
//		
//        InputStreamReader isr = new InputStreamReader(inputStream);
//        BufferedReader br = new BufferedReader(isr);
//        String line = null;
//        System.out.println("<ERROR>");
//        while ( (line = br.readLine()) != null)
//            System.out.println(line);
//        System.out.println("</ERROR>");
		return sb.toString();
	}

	public void generateOFas(Path csvPath,Path output) throws Exception{
		if(!Files.isRegularFile(csvPath)){
			throw new RuntimeException("input-csv-file parameter is not a file.");
		}
		if(output != null){
			if (!Files.isDirectory(output) || Files.notExists(output)) {
				System.out.println(LocalDateTime.now()+" Try to create output directory for OFas file.");
				Files.createDirectories(output);
			}
		}else{
			output = Paths.get(new File(".").toURI());
		}
		
		
		Map<String, String> map = getMapping(csvPath);
		Notification notification = createNotification(map);
		System.out.println(LocalDateTime.now()+" Construct notification element done.");
		doMarshaller(notification,output.toString()+File.separator+"an_fqdn_"+UrlEscapers.urlPathSegmentEscaper().escape(agentId)+"_"+ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+".xml");
	}
	
	private void doMarshaller(Notification notification, String string) {
		try {
			JAXBContext jaxb = JAXBContext.newInstance(Notification.class);
			Marshaller marshaller = jaxb.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			JAXBElement<Notification> element = new JAXBElement<Notification>(new QName("notification"),Notification.class,notification);
			schema.newValidator().validate(new JAXBSource(jaxb,element));
			marshaller.marshal(element, new File(string));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void doMarshallerFor(MeasSchedule measSchedule, File file) {
		try {
			JAXBContext jaxb = JAXBContext.newInstance(MeasSchedule.class);
			Marshaller marshaller = jaxb.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//			JAXBElement<MeasSchedule> element = new JAXBElement<Notification>(new QName("notification"),Notification.class,notification);
			mj_schema.newValidator().validate(new JAXBSource(jaxb,measSchedule));
			marshaller.marshal(measSchedule, file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}	
	
	public Notification createNotification(Map<String, String> map){
		
		Notification ntf = obj.createNotification();
		map.forEach((k,v)->{
			ntf.getAlarmNewOrAlarmClearedOrAlarmChanged().add(createAlarmNew(k,v));
		});
		
		return ntf;
	}

	private AlarmNew createAlarmNew(String k, String v) {
		AlarmNew alarmNew = obj.createAlarmNew();
		try {
			alarmNew.setSystemDN(k);
			XMLGregorianCalendar xml = DatatypeFactory.newInstance().newXMLGregorianCalendar(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx")));
			alarmNew.setEventTime(xml);
			
			int seed = random.nextInt(man.size());
//			System.out.println(seed+","+man.get(seed));
			String str[] = man.get(seed).toString().split(",");
			alarmNew.setSpecificProblem(Integer.parseInt(str[0]));
			alarmNew.setAlarmText(str[1]);
			alarmNew.setPerceivedSeverity(PerceivedSeverity.fromValue(str[3].toLowerCase()));
			alarmNew.setAdditionalText3(v);
			alarmNew.setEventType(EventType.fromValue(str[4]));
			alarmNew.setProbableCause(Integer.parseInt(str[2]));
			alarmNew.setAlarmId("15066619006470000000"+random.nextInt(1000));
			alarmNew.setNotificationId(random.nextInt(100)+"");
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
		
		return alarmNew;
	}

	public Map<String, String> getMapping(Path csvPath) throws IOException {
		Map<String, String> map = new HashMap<>();
		Files.readAllLines(csvPath).forEach(e -> {
			String tmpArr[] = e.split(",");
			String fullDn = tmpArr[0];

			if (!fullDn.startsWith("PLMN"))
				throw new RuntimeException("The DN of csv file is incorrect, it should be starting with PLMN-*.");

			String s[] = fullDn.split("/", 3);

			if (s.length != 3)
				throw new RuntimeException(
						"The DN of csv file is incorrect, it should be starting with PLMN-*/xxx-*/xxx-*.");

			String tmpAgentId = s[0] + "/" + s[1];
			if(agentId != null && !agentId.equals(tmpAgentId)){
				throw new RuntimeException(
						"The DN of csv file is incorrect, all DN should have same prefix, e.g. PLMN-*/xxx-*.");
			}
			agentId = tmpAgentId;
			map.put(s[2], tmpArr[1]);
		});

		return map;
	}
	
	
	private MeasSchedule readMJFile(File mj_file) {
		try {
			JAXBContext jaxb = JAXBContext.newInstance(MeasSchedule.class);
			Unmarshaller ummarshaller = jaxb.createUnmarshaller();
//			mj_schema.newValidator().validate(new JAXBSource(jaxb,mj_file));
			return (MeasSchedule)ummarshaller.unmarshal(mj_file);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}


