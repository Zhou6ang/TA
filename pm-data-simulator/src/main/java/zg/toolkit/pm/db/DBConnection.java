package zg.toolkit.pm.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.nsn.oss.o2ml2koa.schemas.koala.Adaptation;
import com.nsn.oss.o2ml2koa.schemas.koala.Level;

import zg.toolkit.ssh.SSHLibrary;

public class DBConnection {

	private String username;
	private String password;
	private String host;
	public DBConnection(String username,String password,String host){
		this.username = username;
		this.password = password;
		this.host = host;
		
	}
	//select  b.co_dn, a.* from imscsf_p_meas_aggregat_o2 a, utp_common_objects b where a.global_id = b.co_gid and b.co_dn like 'PLMN-AUTO/CSCF-ines-17.0VI' order by a.starttime desc;
	public DBConnection(String username,String password,String host,boolean withDB) throws Exception {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		try (Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@"+host+":1521:oss",username,password)) {
			String init_sql_case_0 = "DROP TABLE IF EXISTS TEST;"
					+ "CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR(255));"
					+ "INSERT INTO TEST VALUES(1, 'Hello');" + "INSERT INTO TEST VALUES(2, 'World');";
			String init_sql_case_1 = "CREATE TABLE IF NOT EXISTS TEST(ID INT PRIMARY KEY, NAME VARCHAR(255));"
					+ "INSERT INTO TEST VALUES(3, 'Hello');" + "INSERT INTO TEST VALUES(4, 'World');";
			String query_sql = "select  b.co_dn, a.* from imscsf_p_meas_aggregat_o2 a, utp_common_objects b where a.global_id = b.co_gid and b.co_dn like 'PLMN-AUTO/CSCF-ines-17.0VI' order by a.starttime desc";
			String update_sql = "UPDATE TEST SET NAME='Hi' WHERE ID=1;";
			String del_sql = "DELETE FROM TEST WHERE ID=2;";
//			conn.prepareStatement(init_sql_case_0).execute();
			PreparedStatement ps = conn.prepareStatement(query_sql);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				System.out.println(rs.getNString(1)+","+rs.getNString(2)+","+rs.getNString(3));
//				System.out.println(rs.getArray(1)+","+rs.getArray(2));
			}
			// add application code here
		}
	}
	
	private int parseResult(String result) {
		Matcher match = Pattern.compile("SQL>[\\s\\S]*SQL>([\\s\\S]*)SQL>").matcher(result);
		if(match.find()){
			String output = match.group(1);
			if(output.contains("ERROR")){
				throw new RuntimeException("DB query error: \r\n"+output);
			}
			String s1 = output.replaceAll("\r\n", "").replaceAll("-", "").replaceAll(" ", "").trim();
			String rlt[] = s1.split("\t");
			System.out.println("keyword:"+rlt[0]+"->"+rlt[1]);
			return Integer.parseInt(rlt[1]);
		}else{
			throw new RuntimeException("DB query error: no found result.");
		}
		
	}
	
	
	public Map<String, Integer> getDataFromDB(Adaptation adap,String dn,String subsystem,ZonedDateTime timestamp) throws IOException{
		Map<String, Integer> resultMap = new HashMap<>();
		try (SSHLibrary ssh = new SSHLibrary(username, password, host)) {
			adap.getMeasurement().forEach(m -> {
				String tableName = m.getOMeSName() == null ? m.getID() : m.getOMeSName();
				String sqlCommand = "sqlplus "+username+"/"+password+"@"+host+"/oss << EOF\r\nALTER SESSION SET TIME_ZONE = '"+timestamp.getOffset()+"';\r\nselect count(*) as result from "+subsystem.toLowerCase()+"_p_meas_"+m.getID().toLowerCase()+"_o2 a, utp_common_objects b where a.global_id = b.co_gid and b.co_dn like '"+dn+"%' and a.starttime = TIMESTAMP '"+timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX")).toString()+"'  order by a.starttime desc;\r\nEOF\r\n";
				String allRecord="echo \"select * from (select SESSIONTIMEZONE, to_char(a.starttime,'YYYY-MM-DD hh24:MI:SS') as starttime ,count(*) as result from "+subsystem.toLowerCase()+"_p_meas_"+m.getID().toLowerCase()+"_o2 a, utp_common_objects b where a.global_id = b.co_gid and b.co_dn like '"+dn+"%' group by a.starttime order by starttime desc) where rownum <= 10;\"|sqlplus "+username+"/"+password+"@"+host+"/oss";
				ssh.execWithinSession(allRecord);
				String result = ssh.execWithinSession(sqlCommand);
				resultMap.put(tableName, parseResult(result));
			});
		}
		
		return resultMap;
	}
}
