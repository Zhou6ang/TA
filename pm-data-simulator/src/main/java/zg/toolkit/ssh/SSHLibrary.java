package zg.toolkit.ssh;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;

import zg.toolkit.ssh.logging.SSHLogger;

/**
 * more example: http://www.jcraft.com/jsch/examples/
 * 
 * @author ganzhou
 *
 */
public class SSHLibrary implements Closeable {

	private static final Logger logger = LogManager.getLogger(SSHLibrary.class);
	private static final String SFTP = "sftp";
	private static final String EXEC = "exec";
	private static final String SHELL = "shell";
	private ChannelFactory<Channel> factory;

	public SSHLibrary(String username, String pwd, String host, int port) {
		factory = new ChannelFactory<>(username, pwd, host, port);
	}

	public SSHLibrary(String username, String pwd, String host) {
		this(username, pwd, host, 22);
	}

	public static void main(String[] args) throws JSchException, SftpException, IOException {
//		SSHLibrary.sftpUpload("D:\\userdata\\ganzhou\\Desktop\\PM\\crash.distrib-1.3.1-spring.tar.gz","/root/crash.distrib-1.3.1-spring.tar.gz",new SSHInfo("root", "arthur", "10.92.18.160")); //upload case.
//		SSHLibrary.sftpDownload("/root/install.log", "D:\\install.log",new SSHInfo("root", "arthur","10.92.18.160"));//download case.
//
//		System.out.println(SSHLibrary.exec("tail -f /var/opt/oss/log/ne3sws_dynamicadaptation/oss_activity0_0.log",new SSHInfo("omc", "omc","10.92.18.160")));//normal case
//		System.out.println(SSHLibrary.exec("sh /root/1.sh&&date",new SSHInfo("omc", "omc", "10.92.18.160"))); //error case
//		System.out.println(SSHLibrary.exec("date;cat /root/1.sh",new SSHInfo("root", "arthur", "10.92.18.160"))); //normal case
//
//		SSHLibrary.shellInteraction(new SSHInfo("omc", "omc", "10.92.18.160"));
//		String rlt = SSHLibrary.shell("ll",new SSHInfo("omc", "omc", "10.92.18.160")); //error case
//		System.out.println("#####"+rlt);
		
		//several channel within a session
		try(SSHLibrary ssh = new SSHLibrary("omc", "omc", "10.92.18.160")){
			ssh.execWithinSession("echo 'do some command in 1 channel of session'");
			ssh.execWithinSession("echo 'do some command in 2 channel but same session'");
			ssh.shellWithinSession("echo 'do some command in 3 channel but same session'");
		}
	}

	/**
	 * Upload file from local to remote location. Not support directory.
	 * @param localFile file path in local.
	 * @param remoteFile file path in remote.
	 */
	public static void sftpUpload(String localFile, String remoteFile, SSHInfo info) {
		try (ChannelFactory<ChannelSftp> factory = new ChannelFactory<>()) {
			ChannelSftp sftp = factory.getChannel(info.getUsername(), info.getPwd(), info.getHost(), info.getPort(), SFTP);
			sftp.connect();
			sftp.put(localFile,remoteFile, new SSHSftpProgressMonitor("upload"), ChannelSftp.OVERWRITE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Download file from remote to local. Not support directory.
	 * @param remoteFile file path in remote.
	 * @param localFile file path in local.
	 */
	public static void sftpDownload(String remoteFile, String localFile, SSHInfo info) {

		try (ChannelFactory<ChannelSftp> factory = new ChannelFactory<>()) {
			ChannelSftp sftp = factory.getChannel(info.getUsername(), info.getPwd(), info.getHost(), info.getPort(), SFTP);
			sftp.connect();
			sftp.get(remoteFile, localFile, new SSHSftpProgressMonitor("download"), ChannelSftp.OVERWRITE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Execute a command similar with SSH in shell-terminate(e.g putty).<br>
	 * It can capture output log from server side.<br>
	 * And it can get/check exit-code when error happened during command execution.<br>
	 * If no need to get/check exit-code, you can refer to shell.<br>
	 * Normally, it's not long connection, multiple commands should be executed one by one.<br>
	 * @param command which need to be executed.
	 * @return String captured output log.
	 */
	public static String exec(String command,SSHInfo info) {
		logger.info("starting to execute command(exec): "+command);
		try (ChannelFactory<ChannelExec> factory = new ChannelFactory<>()) {
			ChannelExec exec = factory.getChannel(info.getUsername(), info.getPwd(), info.getHost(), info.getPort(), EXEC);
			exec.setCommand(command);

			String standardOutput;
			String errorOutput;
			try (BufferedReader input = new BufferedReader(new InputStreamReader(exec.getInputStream()));
					BufferedReader extra = new BufferedReader(new InputStreamReader(exec.getExtInputStream()));) {
				//the exec#connect() must be invoke after exec#getInputStream() or exec#getExtInputStream()
				exec.connect();
				standardOutput = getStringByStream(exec, input);
				errorOutput = getStringByStream(exec, extra);
			}

			if (exec.getExitStatus() != 0) {
				String log = "command: \"" + command + "\" exection failed with exit code: " + exec.getExitStatus();
				logger.error(errorOutput);
				logger.error(log);
				throw new RuntimeException(log, new Exception(errorOutput));
			}

			logger.debug("output: " + standardOutput);
			logger.info("command: \"" + command + "\" exection successfully with exit code: " + exec.getExitStatus());
			return standardOutput;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
	
	/**
	 * Execute command similar with shell-terminate(e.g putty).<br>
	 * It can capture output log from server side.<br>
	 * But it couldn't get/check exit-code when error happened during execution.<br>
	 * To get/check exit-code, you can refer to exec.<br>
	 * Normally, it's long connection, it can develop a shell-terminate like putty.<br>
	 * @param command which need to be executed.<br>
	 * @return captured output log.
	 */
	public static String shell(String command,SSHInfo info) {
		logger.info("starting to execute command(shell): "+command);
		try (ChannelFactory<ChannelShell> factory = new ChannelFactory<>()) {
			ChannelShell shell = factory.getChannel(info.getUsername(), info.getPwd(), info.getHost(), info.getPort(), SHELL);
			
			  // Enable agent-forwarding.
		      //((ChannelShell)channel).setAgentForwarding(true);
			
		      // Choose the pty-type "vt102".
//		      ((ChannelShell)channel).setPtyType("vt102");

		      // Set environment variable "LANG" as "ja_JP.eucJP".
//		      ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");

			String standardOutput;
			try (BufferedReader input = new BufferedReader(new InputStreamReader(shell.getInputStream()));
					OutputStream output = shell.getOutputStream()) {
				//NOTE: shell#connect() invocation must be after shell#getInputStream(),shell#getOutStream(),shell#getExtStream()
				shell.connect();
				String expect = "@end@";
				output.write((command+(command.endsWith(";")?"echo "+expect+"|tr a-z A-Z\r":";echo "+expect+"|tr a-z A-Z\r")).getBytes());
				output.flush();
				standardOutput = getStringByStream(shell, input,expect.toUpperCase());
				
			}
			logger.debug("Execution output: \r\n" + standardOutput);
			return standardOutput;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * It's a long connection between terminate and server side, and terminate
	 * is holding a channel of session until channel closed by terminate or
	 * server side.
	 * Note: actually it's not supported shell#getExitStatus() to get exit code from long connection perspective.
	 */
	public static void shellInteraction(SSHInfo info) {
		try (ChannelFactory<ChannelShell> factory = new ChannelFactory<>()) {
			ChannelShell shell = factory.getChannel(info.getUsername(), info.getPwd(), info.getHost(), info.getPort(), SHELL);
			 // Enable agent-forwarding.
		      //shell.setAgentForwarding(true);
			
		      // Choose the pty-type "vt102".
//		      shell.setPtyType("vt102");

		      // Set environment variable "LANG" as "ja_JP.eucJP".
//		      shell.setEnv("LANG", "ja_JP.eucJP");
			
			try (OutputStream output = shell.getOutputStream()) {
				
//				shell.setExtOutputStream(System.err);
				
				//below is prompt show after cmd execution.
				new Thread(() -> {
					try (BufferedReader input = new BufferedReader(new InputStreamReader(shell.getInputStream()))) {
						StringBuilder sb = new StringBuilder();
						while (true) {
							String s = input.readLine();
							sb.append(s + "\r\n");
							logger.debug(s);
							if (shell.isClosed()) {
//								System.out.println("show:"+shell.getExitStatus());
								logger.info("The prompt show will be closing...");
								break;
							}
//							try {Thread.sleep(1000);} catch (InterruptedException e) {logger.error(e);}
						}
						logger.info("All Content: \r\n"+sb.toString());
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					
				}).start();
				
				//NOTE: shell#connect() invocation must be after shell#getInputStream(),shell#getOutStream(),shell#getExtStream()
				shell.connect();
				
				//below is input of terminate.
				Scanner scan = new Scanner(System.in);
				while (true) {
					if (scan.hasNext()) {
						String tmp = scan.nextLine();
						if (tmp.contains("end")) {
							scan.close();
							break;
						}
						output.write((tmp+"\r").getBytes());
						output.flush();
					}
				}
			}
			
			 

//			String standardOutput;
//			String errorOutput;
//			try (BufferedReader input = new BufferedReader(new InputStreamReader(shell.getInputStream()));
//					BufferedReader extra = new BufferedReader(new InputStreamReader(shell.getExtInputStream()));) {
////				shell.connect();
//				standardOutput = getStringByStream(shell, input);
//				errorOutput = getStringByStream(shell, extra);
//			}
//
//			if (shell.getExitStatus() != 0) {
//				String log = "command: \"" + command + "\" exection failed with exit code: " + shell.getExitStatus();
//				logger.error(errorOutput);
//				logger.error(log);
//				throw new RuntimeException(log, new Exception(errorOutput));
//			}
//
//			logger.debug("output: " + standardOutput);
//			logger.info("command: \"" + command + "\" exection successfully with exit code: " + shell.getExitStatus());
//			return standardOutput;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void scp() {

	}

	public void subsystem() {

	}

	public void sudo() {

	}

	public void openSSHConfig() {

	}

	static class SSHUserInfo implements UserInfo {
		private String password;

		public SSHUserInfo(String pwd) {
			this.password = pwd;
		}

		@Override
		public void showMessage(String message) {
			logger.info(message);
		}

		@Override
		public boolean promptYesNo(String message) {
			logger.info("promptYesNo: " + message + " Yes.");
			return true;
		}

		@Override
		public boolean promptPassword(String message) {
			logger.info("promptPassword: Yes. " + message);
			return true;
		}

		@Override
		public boolean promptPassphrase(String message) {
			logger.info("promptPassphrase:" + message);
			return false;
		}

		@Override
		public String getPassword() {
			return password;
		}

		@Override
		public String getPassphrase() {
			return null;
		}
	}

	static class SSHSftpProgressMonitor implements SftpProgressMonitor {

		private double count = 0;
		private double max = 0;
		private String comments;

		public SSHSftpProgressMonitor(String comments) {
			this.comments = comments;
		}

		@Override
		public void init(int op, String src, String dest, long max) {
			this.max = max;
			logger.info("trying to " + comments + " file from " + src + " to " + dest);
			logger.info("mode is " + op + ", file size: " + max + " byte.");
		}

		@Override
		public void end() {
			logger.info(comments + " file done.");
		}

		@Override
		public boolean count(long count) {
			this.count += count;
			logger.debug(comments + "ed:" + this.count + " byte, percent:" + ((this.count / max) * 100) + "%");
			return true;
		}

	}

	static class ChannelFactory<T extends Channel> implements Closeable {
		private final Logger logger = LogManager.getLogger(ChannelFactory.class);
		private Session session;
		private Channel channel;

		public ChannelFactory() {
		}
		@SuppressWarnings("unchecked")
		public T getChannel(String username, String pwd, String host, int port, String type) {
			try {
				JSch jsch = new JSch();
				if(logger.isDebugEnabled())JSch.setLogger(new SSHLogger());
				session = jsch.getSession(username, host, port);
				session.setUserInfo(new SSHUserInfo(pwd));
				session.connect();
				channel = session.openChannel(type);
				// channel.connect();
				return (T) channel;
			} catch (JSchException e) {
				throw new RuntimeException(e);
			}
		}
		
		@Override
		public void close() {
			closeChannel();
			closeSession();
		}
		
		/**
		 * this is to open several channel for one session.<br>
		 * 1. ChannelFactory#new ChannelFactory(username,pwd,host,port);<br>
		 * 2. ChannelFactory#getChannel(String type).<br>
		 * 3. ChannelFactory#closeChannel().<br>
		 * 4. ChannelFactory#closeSession().<br>
		 * @param username
		 * @param pwd
		 * @param host
		 * @param port
		 */
		public ChannelFactory(String username, String pwd, String host, int port) {
			try {
				JSch jsch = new JSch();
				if (logger.isDebugEnabled())
					JSch.setLogger(new SSHLogger());
				session = jsch.getSession(username, host, port);
				session.setUserInfo(new SSHUserInfo(pwd));
				session.connect();
			} catch (JSchException e) {
				throw new RuntimeException(e);
			}
		}
		
		public Channel getChannel(String type) {
			try {
				channel = session.openChannel(type);
				// channel.connect();
				return channel;
			} catch (JSchException e) {
				throw new RuntimeException(e);
			}
		}

		public void closeChannel() {
			channel.disconnect();
			logger.info("The channel disconnected has been initiated by terminate side.");
		}
		
		public void closeSession() {
			session.disconnect();
			logger.info("The session disconnected has been initiated by terminate side.");
		}
	}
	
 public static class SSHInfo{
		private String username;
		private String pwd;
		private String host;
		private int port = 22;
		public SSHInfo(String username,String pwd,String host) {
			this.username = username;
			this.pwd = pwd;
			this.host = host;
		}
		public SSHInfo() {
		}
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPwd() {
			return pwd;
		}
		public void setPwd(String pwd) {
			this.pwd = pwd;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
		
	}
	
	private static String getStringByStream(Channel exec, BufferedReader input) throws IOException {
		return getStringByStream(exec, input,null);
	}
	private static String getStringByStream(Channel exec, BufferedReader input,String expect) throws IOException {
		StringBuilder sb = new StringBuilder();
		while (true) {
			String str = input.readLine();
			str = str == null ? "" : str;
			logger.debug(str);
			sb.append(str+"\r\n");
			if (expect != null && sb.toString().contains(expect)) {
				logger.info("Execution has completed for command(SHELL).");
				sb = sb.delete(sb.length()-(expect.length()+2), sb.length());
				break;
			}
			if (exec.isClosed()) {
				if (input.ready()) {
					continue;
				}
				logger.info("Execution has completed for channel closed(EXEC).");
				break;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.error(e);
			}
		}
		return sb.toString();
	}
	
	/**
	 * Execute a command similar with SSH in shell-terminate(e.g putty).<br>
	 * It can capture output log from server side.<br>
	 * And it can get/check exit-code when error happened during command execution.<br>
	 * If no need to get/check exit-code, you can refer to shell.<br>
	 * Normally, it's not long connection, multiple commands should be executed one by one.<br>
	 * @param command which need to be executed.
	 * @return String captured output log.
	 */
	public String execWithinSession(String command) {
		try{
			logger.info("starting to execute command(exec): "+command);
			ChannelExec exec =(ChannelExec)factory.getChannel(EXEC);
			exec.setCommand(command);

			String standardOutput;
			String errorOutput;
			try (BufferedReader input = new BufferedReader(new InputStreamReader(exec.getInputStream()));
					BufferedReader extra = new BufferedReader(new InputStreamReader(exec.getExtInputStream()));) {
				//the exec#connect() must be invoke after exec#getInputStream() or exec#getExtInputStream()
				exec.connect();
				standardOutput = getStringByStream(exec, input);
				errorOutput = getStringByStream(exec, extra);
			}

			if (exec.getExitStatus() != 0) {
				String log = "command: \"" + command + "\" exection failed with exit code: " + exec.getExitStatus();
				logger.error(errorOutput);
				logger.error(log);
				throw new RuntimeException(log, new Exception(errorOutput));
			}

			logger.debug("output: " + standardOutput);
			logger.info("command: \"" + command + "\" exection successfully with exit code: " + exec.getExitStatus());
			return standardOutput;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			factory.closeChannel();
		}

	}
	
	/**
	 * Upload file from local to remote location. Not support directory.
	 * @param localFile file path in local.
	 * @param remoteFile file path in remote.
	 */
	public void sftpUploadWithinSession(String localFile, String remoteFile) {
		try {
			ChannelSftp sftp = (ChannelSftp)factory.getChannel(SFTP);
			sftp.connect();
			sftp.put(localFile,remoteFile, new SSHSftpProgressMonitor("upload"), ChannelSftp.OVERWRITE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally {
			factory.closeChannel();
		}
	}

	/**
	 * Download file from remote to local. Not support directory.
	 * @param remoteFile file path in remote.
	 * @param localFile file path in local.
	 */
	public void sftpDownloadWithinSession(String remoteFile, String localFile) {

		try{
			ChannelSftp sftp = (ChannelSftp)factory.getChannel(SFTP);
			sftp.connect();
			sftp.get(remoteFile, localFile, new SSHSftpProgressMonitor("download"), ChannelSftp.OVERWRITE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally {
			factory.closeChannel();
		}
	}
	
	/**
	 * Execute command similar with shell-terminate(e.g putty).<br>
	 * It can capture output log from server side.<br>
	 * But it couldn't get/check exit-code when error happened during execution.<br>
	 * To get/check exit-code, you can refer to exec.<br>
	 * Normally, it's long connection, it can develop a shell-terminate like putty.<br>
	 * @param command which need to be executed.<br>
	 * @return captured output log.
	 */
	public String shellWithinSession(String command) {
		logger.info("starting to execute command(shell): "+command);
		try{
			ChannelShell shell = (ChannelShell)factory.getChannel(SHELL);
			
			  // Enable agent-forwarding.
		      //((ChannelShell)channel).setAgentForwarding(true);
			
		      // Choose the pty-type "vt102".
//		      ((ChannelShell)channel).setPtyType("vt102");

		      // Set environment variable "LANG" as "ja_JP.eucJP".
//		      ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");

			String standardOutput;
			try (BufferedReader input = new BufferedReader(new InputStreamReader(shell.getInputStream()));
					OutputStream output = shell.getOutputStream()) {
				//NOTE: shell#connect() invocation must be after shell#getInputStream(),shell#getOutStream(),shell#getExtStream()
				shell.connect();
				String expect = "@end@";
				output.write((command+(command.endsWith(";")?"echo "+expect+"|tr a-z A-Z\r":";echo "+expect+"|tr a-z A-Z\r")).getBytes());
				output.flush();
				standardOutput = getStringByStream(shell, input,expect.toUpperCase());
				
			}
			logger.debug("Execution output: \r\n" + standardOutput);
			return standardOutput;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			factory.closeChannel();
		}
	}
	
	@Override
	public void close() throws IOException {
		factory.close();
	}
	
	
}
