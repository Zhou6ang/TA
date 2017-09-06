package zg.toolkit.fm.aom;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.nokia.oss.aom.AOM;
import com.nokia.oss.aom.AOM.Capability;
import com.nokia.oss.aom.AOM.MeasurementTypes;
import com.nokia.oss.aom.AoMPMRef;
import com.nokia.oss.aom.HRefType;
import com.nokia.oss.aom.MeasurementRef;
import com.nokia.oss.aom.MeasurementType;
import com.nokia.oss.aom.ParamGroupList;
import com.nokia.oss.aom.ParameterType;
import com.nokia.oss.aom.SupportedMeasurementIntervals;
import com.nokia.oss.aom.ValidValuesType;
import com.nokia.oss.aom.ValidationRuleType;

public class AoMApp {
	private String lStr;
	private String rStr;

	public static void main(String[] args) {
		
		String leftStr = "D:\\userdata\\ganzhou\\Desktop\\CSCF\\aom\\example\\NOKBSC1.aom";
		String rightStr = "D:\\userdata\\ganzhou\\Desktop\\CSCF\\aom\\example\\NOKBSC2.aom";
		new AoMApp().compare(leftStr,"111", rightStr,"222");

	}
	public static void run(String[] args){
		if(args.length != 2 && args.length != 4){
        	System.out.println("#################################################################################################");
        	System.out.println("#            Aom files comparator tool                                                          #");
        	System.out.println("#                                                                                               #");
        	System.out.println("# usage: java -jar file-comparator*.jar -aom /var/../left.aom /var/../right.aom                 #");
        	System.out.println("#        java -jar file-comparator*.jar -aom /var/../left.aom alias-A /var/../right.aom alias-B #");
        	System.out.println("#################################################################################################");
        	System.exit(-1);
        }
		
		switch (args.length) {
		case 2:
			new AoMApp().compare(args[0], args[1]);
			break;
		case 4:
			new AoMApp().compare(args[0], args[1],args[2], args[3]);
			break;
		default:
			throw new RuntimeException("Parameter errors.");
		}
		
	}
	public void compare(String leftStr,String rightStr){
		compare(leftStr, "Left", rightStr, "Right");
	}
	
	public void compare(String leftStr,String leftAlias,String rightStr,String rightAlias){
		this.rStr = rightAlias;
		this.lStr = leftAlias;
	
		System.out.println(rStr+" is "+rightStr);
		System.out.println(lStr+" is "+leftStr);
		Path left = Paths.get(leftStr);
		Path right = Paths.get(rightStr);
		AOM leftAom = getAom(left);
		AOM rightAom = getAom(right);
		compareAttributes(leftAom,rightAom);
		compareAdaptation(leftAom.getAdaptation(),rightAom.getAdaptation());
		compareCapability(leftAom.getCapability(),rightAom.getCapability());
		compareMeasurementTypes(leftAom.getMeasurementTypes(),rightAom.getMeasurementTypes());
		
	}
	
	private void compareMeasurementTypes(MeasurementTypes left, MeasurementTypes right) {
		Map<String, MeasurementType> leftMap = left.getMeasurementType().parallelStream().collect(Collectors.toMap((MeasurementType k)->k.getId(),(MeasurementType v)->v));
		Map<String, MeasurementType> rightMap = right.getMeasurementType().parallelStream().collect(Collectors.toMap((MeasurementType k)->k.getId(),(MeasurementType v)->v));
		
		showDiff("MeasurementTypes->MeasurementType",leftMap.keySet(),rightMap.keySet());
		
		SetView<String> common = Sets.intersection(leftMap.keySet(), rightMap.keySet());
		common.forEach(e->{
			MeasurementType leftMeasurement = leftMap.get(e);
			MeasurementType rightMeasurement = rightMap.get(e);
			compareMeasurementType(leftMeasurement,rightMeasurement);
		});
		
		
		Map<String, AoMPMRef>  leftAomPMRefMap = left.getAoMPMRef().parallelStream().collect(Collectors.toMap(k->("referredAoMAdaptationID="+k.getReferredAoMAdaptationID()+"&&referredAoMAdaptationRelease="+k.getReferredAoMAdaptationRelease()), v->v));
		Map<String, AoMPMRef>  rightAomPMRefMap = right.getAoMPMRef().parallelStream().collect(Collectors.toMap(k->("referredAoMAdaptationID="+k.getReferredAoMAdaptationID()+"&&referredAoMAdaptationRelease="+k.getReferredAoMAdaptationRelease()), v->v));
		showDiff("MeasurementTypes->AoMPMRef",leftAomPMRefMap.keySet(),rightAomPMRefMap.keySet());
		SetView<String> commonAomPMref = Sets.intersection(leftAomPMRefMap.keySet(), rightAomPMRefMap.keySet());
		commonAomPMref.forEach(e ->{
			AoMPMRef leftAoMPMRef = leftAomPMRefMap.get(e);
			AoMPMRef rightAoMPMRef = rightAomPMRefMap.get(e);
			compareAoMPMRef("MeasurementTypes->AoMPMRef:"+e,leftAoMPMRef,rightAoMPMRef);
		});
		
	}

	private void compareAoMPMRef(String keyword,AoMPMRef leftAoMPMRef, AoMPMRef rightAoMPMRef) {
		compareMeasurementRefs(keyword+"->MeasurementRef",leftAoMPMRef.getMeasurementRef(),rightAoMPMRef.getMeasurementRef());
		
	}
	private void compareMeasurementRefs(String keyword,List<MeasurementRef> left, List<MeasurementRef> right) {
		Map<String, MeasurementRef> leftMearRefMap = left.parallelStream().collect(Collectors.toMap(MeasurementRef::getAomMeasurementID, v->v));
		Map<String, MeasurementRef> rightMearRefMap = right.parallelStream().collect(Collectors.toMap(MeasurementRef::getAomMeasurementID, v->v));
		showDiff(keyword,leftMearRefMap.keySet(),rightMearRefMap.keySet());
		SetView<String> commonAomPMref = Sets.intersection(leftMearRefMap.keySet(), rightMearRefMap.keySet());
		commonAomPMref.forEach(e ->{
			MeasurementRef leftAoMPMRef = leftMearRefMap.get(e);
			MeasurementRef rightAoMPMRef = rightMearRefMap.get(e);
			compareMeasurementRef(keyword,leftAoMPMRef,rightAoMPMRef);
		});
	}
	private void compareMeasurementRef(String keyword, MeasurementRef leftAoMPMRef, MeasurementRef rightAoMPMRef) {
		
		equalString(keyword, leftAoMPMRef.getAomMeasurementID(), rightAoMPMRef.getAomMeasurementID());
		equalString(keyword, leftAoMPMRef.getCmMocInfo(), rightAoMPMRef.getCmMocInfo());
		equalString(keyword, leftAoMPMRef.getMeasurementIDinSourceAoMAdaptation(), rightAoMPMRef.getMeasurementIDinSourceAoMAdaptation());
	}
	private void showDiff(String keyword, Set<String> left, Set<String> right) {
		SetView<String> diff1 = Sets.difference(left, right);
		if(!diff1.isEmpty()){
			System.out.println("["+keyword+"] "+rStr+" missing:"+diff1);
		}
		
		SetView<String> diff2 = Sets.difference(right,left);
		if(!diff2.isEmpty()){
			System.out.println("["+keyword+"] "+lStr+" missing:"+diff2);
		}
		
	}
	private void compareMeasurementType(MeasurementType leftMeasurement, MeasurementType rightMeasurement) {
		equalString("MeasurementTypes->MeasurementType:"+leftMeasurement.getId()+" ", leftMeasurement.getCmMocInfo(), rightMeasurement.getCmMocInfo());
		equalString("MeasurementTypes->MeasurementType:"+leftMeasurement.getId()+" ", leftMeasurement.getPresentationName(), rightMeasurement.getPresentationName());
		compareContent("MeasurementTypes->MeasurementType:"+leftMeasurement.getId()+" ",leftMeasurement,rightMeasurement);
		
		
	}
	private void compareCapability(Capability capability, Capability capability2) {
		equalString("Capability",capability.getAomAppliedResourceTarget(), capability2.getAomAppliedResourceTarget());
		equalString("Capability",capability.getNumOfTimePeriodSupported()+"", capability2.getNumOfTimePeriodSupported()+"");
		equalString("Capability",capability.isIsEndDateSupported()+"", capability2.isIsEndDateSupported()+"");
		equalString("Capability",capability.isIsEndTimeSupported()+"", capability2.isIsEndTimeSupported()+"");
		equalString("Capability",capability.isIsStartDateSupported()+"", capability2.isIsStartDateSupported()+"");
		equalString("Capability",capability.isIsStartTimeSupported()+"", capability2.isIsStartTimeSupported()+"");
		equalString("Capability",capability.isIsWeekdaySelectionSupported()+"", capability2.isIsWeekdaySelectionSupported()+"");
		equalString("Capability",capability.isSameIntervalForEachTimePeriod()+"", capability2.isSameIntervalForEachTimePeriod()+"");
		compareSupportedMeasurementIntervals("Capability",capability.getSupportedMeasurementIntervals(),capability2.getSupportedMeasurementIntervals());
	}

	private void compareSupportedMeasurementIntervals(String keyword,SupportedMeasurementIntervals supportedMeasurementIntervals,
			SupportedMeasurementIntervals supportedMeasurementIntervals2) {
		equalString(keyword+"->supportedMeasurementIntervals", supportedMeasurementIntervals.getDefaultMeasurementInterval()+"", supportedMeasurementIntervals2.getDefaultMeasurementInterval()+"");
		equalList(keyword+"->supportedMeasurementIntervals->supportedMeasurementInterval", supportedMeasurementIntervals.getSupportedMeasurementInterval(), supportedMeasurementIntervals2.getSupportedMeasurementInterval());
	}

	private void compareAdaptation(HRefType adaptation, HRefType adaptation2) {
		equalString("Adaptation",adaptation.getHref(), adaptation2.getHref());
	}

	private void compareAttributes(AOM leftAom, AOM rightAom) {
		equalString("AOM attributes",leftAom.getInterfaceVersion(),rightAom.getInterfaceVersion());
		equalString("AOM attributes",leftAom.getSchemaVersion(),rightAom.getSchemaVersion());
	}

	private void equalString(String keyword,String str1, String str2) {
		str1 = str1 == null ? "" : str1;
		str2 = str2 == null ? "" : str2;
		if(!str1.equals(str2)){
			System.out.println("["+keyword+"->Attr] Not equal: "+lStr+"-["+str1+"] vs "+rStr+"-["+str2+"]");
		}
		
		
	}
	
	private void equalList(String keyword,List<? extends Object> left, List<? extends Object> right) {
		SetView<Object> diff1 = Sets.difference(new HashSet<>(left), new HashSet<>(right));
		if(!diff1.isEmpty()){
			System.out.println("["+keyword+"] "+rStr+" missing -"+diff1);
		}
		
		SetView<Object> diff2 = Sets.difference(new HashSet<>(right), new HashSet<>(left));
		if(!diff2.isEmpty()){
			System.out.println("["+keyword+"] "+lStr+" missing -"+diff2);
		}
	}

	private void compareContent(String keyword,MeasurementType left, MeasurementType right) {
		List<SupportedMeasurementIntervals> leftSMI = getSupportedMeasurementIntervals(left.getContent());
		List<SupportedMeasurementIntervals> rightSMI = getSupportedMeasurementIntervals(right.getContent());
		if(!leftSMI.isEmpty() && !rightSMI.isEmpty()){
			compareSupportedMeasurementIntervals(keyword,leftSMI.get(0),rightSMI.get(0));
		}else if(!leftSMI.isEmpty() && rightSMI.isEmpty()){
			System.out.println(keyword+rStr+" missing -"+leftSMI);
		}else if(leftSMI.isEmpty() && !rightSMI.isEmpty()){
			System.out.println(keyword+lStr+" missing -"+rightSMI);
		}
		
		List<ParameterType> leftPtypeList = getParameter(left.getContent());
		List<ParameterType> rightPtypeList = getParameter(right.getContent());
		compareParameterTypes(keyword,leftPtypeList,rightPtypeList);
		
		List<ParamGroupList> leftPGroupList =  getParamGroups(left.getContent());
		List<ParamGroupList> rightPGroupList =  getParamGroups(right.getContent());
		compareParamGroupList(keyword,leftPGroupList,rightPGroupList);
	}
	
	private void compareParameterTypes(String keyword,List<ParameterType> leftPtypeList, List<ParameterType> rightPtypeList) {
		
		Map<String,ParameterType> leftPtypeNameMap = leftPtypeList.parallelStream().collect(Collectors.toMap(ParameterType::getName, v->v));
		Map<String,ParameterType> rightPtypeNameMap = rightPtypeList.parallelStream().collect(Collectors.toMap(ParameterType::getName, v->v));
		
		showDiff(keyword+"->parameter", leftPtypeNameMap.keySet(), rightPtypeNameMap.keySet());
		
		SetView<String> common = Sets.intersection(leftPtypeNameMap.keySet(), rightPtypeNameMap.keySet());
		common.forEach(e->{
			ParameterType leftMeasurement = leftPtypeNameMap.get(e);
			ParameterType rightMeasurement = rightPtypeNameMap.get(e);
			compareParameterType(keyword+"->parameter:"+e,leftMeasurement,rightMeasurement);
		});
	}
	private void compareParameterType(String keyword,ParameterType leftMeasurement, ParameterType rightMeasurement) {
		
		equalString(keyword, leftMeasurement.getDefaultValue(), rightMeasurement.getDefaultValue());
		equalString(keyword, leftMeasurement.getDesc(), rightMeasurement.getDesc());
		equalString(keyword, leftMeasurement.getHelp(), rightMeasurement.getHelp());
		equalString(keyword, leftMeasurement.getType(), rightMeasurement.getType());
		equalString(keyword, leftMeasurement.isMandatory()+"", rightMeasurement.isMandatory()+"");
		compareValidationRule(keyword,leftMeasurement.getValidationRule(),rightMeasurement.getValidationRule());
	}
	private void compareValidationRule(String keyword,ValidationRuleType left, ValidationRuleType right) {
		equalString(keyword+"->validation-rule", left.getGreaterThan(), right.getGreaterThan());
		equalString(keyword+"->validation-rule", left.getGreaterThanEqualto(), right.getGreaterThanEqualto());
		equalString(keyword+"->validation-rule", left.getLessThan(), right.getLessThan());
		equalString(keyword+"->validation-rule", left.getLessThanEqualto(), right.getLessThanEqualto());
		equalString(keyword+"->validation-rule", left.getParamSelection(), right.getParamSelection());
		equalList(keyword+"->validation-rule", left.getTarget(), right.getTarget());
		compareValidValuesType(keyword,left.getValidValues(),right.getValidValues());
	}
	private void compareValidValuesType(String keyword,ValidValuesType left, ValidValuesType right) {
		equalList(keyword+"->values", left.getValue(), right.getValue());
	}
	private void compareParamGroupList(String keyword,List<ParamGroupList> leftPGroupList, List<ParamGroupList> rightPGroupList) {
		//TODO need to be improved.
		equalList(keyword+"->paramGroups @@Need improved@@", leftPGroupList, rightPGroupList);
	}
	private List<ParamGroupList> getParamGroups(List<Object> content) {
		return content.parallelStream().filter(e -> {
			if (e instanceof JAXBElement) {
				return "paramGroups".equals(((JAXBElement) e).getName().getLocalPart());
			}
			return false;
		}).map(e -> (ParamGroupList)((JAXBElement) e).getValue()).collect(Collectors.toList());
	}
	private List<ParameterType> getParameter(List<Object> content) {
		return content.parallelStream().filter(e -> {
			if (e instanceof JAXBElement) {
				return "parameter".equals(((JAXBElement) e).getName().getLocalPart());
			}
			return false;
		}).map(e ->(ParameterType)((JAXBElement) e).getValue()).collect(Collectors.toList());
	}
	private List<SupportedMeasurementIntervals> getSupportedMeasurementIntervals(List<Object> list) {

		return list.parallelStream().filter(e -> (e instanceof SupportedMeasurementIntervals))
				.map(e -> (SupportedMeasurementIntervals) e).collect(Collectors.toList());

	}
	

	private AOM getAom(Path path){
		
		try {
			JAXBContext jaxb = JAXBContext.newInstance(AOM.class);
			Unmarshaller unmarshaller = jaxb.createUnmarshaller();
			AOM obj = (AOM)unmarshaller.unmarshal(Files.newInputStream(path));
			
//			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
//			JAXBElement<OMeS> element = new JAXBElement<OMeS>(new QName("OMeS"),OMeS.class,omes);
//			marshaller.marshal(element, new File(omes_file_name));
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
}
