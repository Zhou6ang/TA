package zg.toolkit.pm.omes;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.net.UrlEscapers;
import com.nokia.oss.pm.omes.MO;
import com.nokia.oss.pm.omes.OMeS;
import com.nokia.oss.pm.omes.ObjectFactory;
import com.nokia.oss.pm.omes.PMMOResult;
import com.nokia.oss.pm.omes.PMSetup;
import com.nokia.oss.pm.omes.PMTarget;
import com.nokia.oss.pm.pmb2.Measurement;
import com.nokia.oss.pm.pmb2.PMBasic;
import com.nokia.oss.pm.pmb2.PMClassInfo;
import com.nsn.oss.pmbv2koa.converter.reader.Pmbv2FragmentReader;

public class OmesGenerater {
	private ObjectFactory objFactory = new ObjectFactory();
	private ZonedDateTime instantDateTime;
	private static javax.xml.validation.Schema schema;
	private OMeS omes;
	private PMBasic pmBasic;
	private int dataCount;
	static
    {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try
        {
        	schema = sf.newSchema(OmesGenerater.class.getResource("/OMeS_DataModel.xsd"));
        }
        catch (SAXException e)
        {
            throw new RuntimeException(e);
        }
    }
	public static void main(String[] args) throws Exception {
		
		new OmesGenerater(4).generate(args[0], args[1], args[2]);
		
//		new OmesGenerater().generate("D:\\userdata\\ganzhou\\Desktop\\PM\\Ines_o2ml_com.nsn.hssfe-18.0C\\adaptation_com.nsn.hssfe.pmb-18.0C-20170822T184300", "PLMN-PLMN%2FHSSFE-TEST", "D:\\userdata\\ganzhou\\Desktop\\PM");
//		new OmesGenerater().generate("D:\\userdata\\ganzhou\\Desktop\\PM\\Ines_o2ml_com.nsn.hssfe-18.0C\\adaptation_com.nsn.hssfe.pmb-18.0C-20170822T184300", "PLMN-PLMN%2FHSSFE-TEST", "D:\\userdata\\ganzhou\\Desktop\\PM");
	}
	
	public OmesGenerater(int dataCount) {
		this.dataCount = dataCount;
	}
	
	public String generate(String pmbPath,String netactDN,String outputPath) throws Exception{
		netactDN = UrlEscapers.urlPathSegmentEscaper().escape(netactDN);
		Path path = Paths.get(outputPath);
		if(!Files.exists(path)){
			Files.createDirectory(path);
		}
		if(!Files.isDirectory(path)){
			throw new Exception("the "+outputPath+" is not directory.");
		}
		constructDateTime();
		String omes_file_name = path.toString()+File.separator+"omes_fqdn_"+netactDN+"_"+instantDateTime.plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+".xml";
		Set<PMBasic> pmbSet = Pmbv2FragmentReader.getPmbAdaptations(new File(pmbPath));
		for(PMBasic pmb: pmbSet){
			//create OMeS
			OMeS oMeS = constructOmes(pmb);
			omes = oMeS;
			pmBasic = pmb;
			//do marshall for oMeS;
			doMarsharll(omes_file_name,oMeS);
			break;
		}
		return omes_file_name;
	}
	
	public OMeS getOmes(){
		return omes;
	}
	
	public PMBasic getPMBasic(){
		return pmBasic;
	}
	
	public ZonedDateTime getStartTimeStamp(){
		return instantDateTime;
	}

	private OMeS constructOmes(PMBasic pmb) {
		OMeS oMeS = objFactory.createOMeS();
		
		pmb.getMeasurement().parallelStream().forEach(measurement ->{
			//create PMSetup
			PMSetup pmSetup = constructPMSetup(measurement,pmb);
			pmSetup.setStartTime(instantDateTime.toOffsetDateTime().toString()+":00");
			oMeS.getPMSetup().add(pmSetup);
		});
		oMeS.setVersion("2.3");
		return oMeS;
	}

	private void constructDateTime() {
		ZonedDateTime instantTime = ZonedDateTime.now();
		int min = instantTime.getMinute()%30+15;
		instantDateTime =  instantTime.minusSeconds(min*60+instantTime.getSecond());
	}

	private PMSetup constructPMSetup(Measurement measurement,PMBasic pmb) {
		PMSetup pmSetup = objFactory.createPMSetup();
		pmSetup.setInterval(BigInteger.valueOf(measurement.getDefaultInterval()));
		
		//@TODO i should be ramdom instance for PMMOResult;
		IntStream.range(1, dataCount+1).forEach(i ->{
			PMMOResult pmMOResult = objFactory.createPMMOResult();
			pmMOResult.getMO().addAll(constructMO(measurement,i,pmb));
			pmMOResult.getPMTargetOrAny().addAll(constructPMtgt(measurement));
			pmSetup.getPMMOResult().add(pmMOResult);
		});
		return pmSetup;
	}

	private void doMarsharll(String omes_file_name,OMeS omes) {
		System.out.println(omes_file_name);
		try {
			JAXBContext jaxb = JAXBContext.newInstance(OMeS.class);
			Marshaller marshaller = jaxb.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			JAXBElement<OMeS> element = new JAXBElement<OMeS>(new QName("OMeS"),OMeS.class,omes);
			schema.newValidator().validate(new JAXBSource(jaxb,element));
			marshaller.marshal(element, new File(omes_file_name));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Collection<? extends Object> constructPMtgt(Measurement measurement) {
		List<JAXBElement<PMTarget>> list = new ArrayList<>();
		PMTarget PMtgt = objFactory.createPMTarget();
		PMtgt.setMeasurementType((measurement.getMeasurementTypeInOMeS() == null?measurement.getMeasurementType():measurement.getMeasurementTypeInOMeS()));

		measurement.getMeasuredIndicator().forEach(indicator -> {

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = null;
			try {
				db = dbf.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
			Element element = db.newDocument().createElement(indicator.getName());
			element.setTextContent(""+new Random().nextInt(100));
			PMtgt.getAny().add(element);
		});
		JAXBElement<PMTarget> element = new JAXBElement<PMTarget>(new QName("PMTarget"),PMTarget.class,PMtgt);
		list.add(element);
		return list;
	}
	
	private Collection<? extends MO> constructMO(Measurement measurement, int instanceId,PMBasic pmb) {
		List<MO> list = new ArrayList<>();
		measurement.getMeasuredTarget().forEach(tgt->{
			tgt.getHierarchy().forEach(hierarchy -> {
				MO mo = objFactory.createMO();
				mo.setDimension(tgt.getDimension());
				
				String str[] = hierarchy.getClasses().split("/");
				if(str[0].isEmpty()){
					throw new RuntimeException("error hierarchy for measurement: "+measurement.getMeasurementType());
				}
				StringBuilder dn = new StringBuilder(getOmesName(str[0], pmb)+"-1");
				for (int i = 1; i < str.length; i++) {
					String omesName = getOmesName(str[i], pmb);
					dn.append("/"+omesName + "-"+instanceId);

				}
				mo.getContent().add(new JAXBElement<String>(new QName("DN"), String.class, dn.toString()));
				list.add(mo);
			});
		});
		

		return list;
	}

	private String getOmesName(String obj, PMBasic pmb) {
		PMClassInfo opt = pmb.getPMClasses().getPMClassInfo().parallelStream()
				.filter(e -> e.getName().equalsIgnoreCase(obj)).findFirst().get();
		return opt.getNameInOMeS() != null ? opt.getNameInOMeS() : opt.getName();
	}
	
}
