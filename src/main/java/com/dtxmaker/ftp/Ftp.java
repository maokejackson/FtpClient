package com.dtxmaker.ftp;

/**
 * @(#)Ftp.java
 *
 *
 * @author Maoke Jackson
 * @version 0.5 2007/4/17
 */

import java.io.*;
import java.net.*;
import java.util.*;

// FIXME 統一註解格式

public class Ftp {

	private Socket controlSocket = null;	// 傳送指令與接收伺服端回應所使用的socket
	private FtpSocket dataSocket = null;	// 與伺服端之間傳送檔案所使用的socket
	private BufferedReader reader = null;	// 讀取伺服端回應所用的資料流
	private BufferedWriter writer = null;	// 傳送指令到伺服端所用的資料流
	
	private String host;				// 伺服端的位址
	private int port;					// 伺服端的連接埠
	private String username;			// 登入伺服端所用的使用者名稱
	private String password;			// 登入伺服端所用的密碼
	private InetAddress serverhost;		// 伺服端網路位址
	private InetAddress localhost;		// 本地端網路位址
	private File localdir;				// 本地端工作目錄
	private boolean passive = false;		// 被動模式
	
	private static boolean TOGGLE_COMMAND = false;	// 顯示本地端送出的指令，方便除錯
	private static final int BUFFER_SIZE = 4096;	// 傳送檔案時使用的緩衝大小
	
//	private static final String FILE_SEPARATOR = System.getProperty("file.separator");
	
	public Ftp() {
		this(null, 0, null, null);
	}
	
	public Ftp(String host) {
		this(host, 21, null, null);
	}
	
	public Ftp(String host, int port) {
		this(host, port, null, null);
	}
	
	public Ftp(String host, String username, String password) {
		this(host, 21, username, password);
	}
	
	public Ftp(String host, int port, String username, String password) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		localdir = new File(System.getProperty("user.dir"));
	}
	
	/**
	 * Determine if connection is established.
	 * @return true if established.
	 */
	public boolean isConnected() {
		return ( controlSocket != null );
	}
	
	/**
	 * Return host name if connection established or <code>null</code> if not.
	 * @return host.
	 */
	public String getHost() {
		return host;
	}
	
	/**
	 * Return server port number if connection established or 0 if not.
	 * @return port number.
	 */
	public int getPort() {
		return port;
	}
	
	
	
	
	
	// ------------ Method of access control command ------------
	
	public synchronized void connect() throws IOException, FtpException {
		connect(host, port);
	}
	
	// 與伺服端建立連線
	public synchronized void connect(String host, int port) throws IOException, FtpException {
		if ( isConnected() ) {
			throw new FtpException("Already connected to " + getHost() + ", disconnect first.");
		}
		
		try {
			controlSocket = new Socket(host, port);
		} catch ( UnknownHostException uhe ) {
			throw new FtpException("Unknown host " + host);
		}
		reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
		serverhost = controlSocket.getInetAddress();
		localhost = controlSocket.getLocalAddress();
		
		getReply();
		
		this.host = host;
		this.port = port;
	}
	
	public synchronized void login() throws IOException, FtpException {
		login(username, password);
	}
	
	// 登入伺服端
	public synchronized void login(String username, String password) throws IOException, FtpException {
		int code = sent(new FtpCommand(FtpCommand.USER_NAME, username)).getCode();
		
		if ( code == FtpReply.USERNAME_OK_NEED_PASSWORD ) {
			sent(new FtpCommand(FtpCommand.PASSWORD, password), "PASS *****");
		}
		
		this.username = username;
		this.password = password;
	}
	
	// 結束本地端與伺服端的通訊連結
	public synchronized void disconnect() throws IOException, FtpException {
		try {
			sent(new FtpCommand(FtpCommand.LOGOUT));
			reader.close();
			writer.close();
			controlSocket.close();
		} finally {
			close();
		}
	}
	
	// 關閉通訊連結
	private synchronized void close() {
		controlSocket = null;
		reader = null;
		writer = null;
		host = null;
		port = 0;
		username = null;
		password = null;
	}
	
	// 切換伺服端工作目錄
	public synchronized boolean setDir(String dir) throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.CHANGE_WORKING_DIRECTORY, dir));
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	// 切換伺服端工作目錄到上一層
	public synchronized boolean setDirUp() throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.CHANGE_TO_PARENT_DIRECTORY));
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	// 切換本地端工作目錄
	public boolean setLocalDir(String path) throws IOException {
		File dir = mixPath(path);
		
		if ( !dir.exists() || !dir.isDirectory() ) {
			return false;
		}
		
		localdir = dir;
		return true;
	}
	
	/**
	 * Mix local working directory with the input <code>path</code>.
	 * If <code>path</code> is not a absolute path, 
	 * local working directory will be replaced. 
	 * @param path - new path name.
	 * @return new path.
	 * @throws IOException problem of socket, stream...
	 */
	private File mixPath(String path) throws IOException {
		File file = new File(path);
		if ( !file.isAbsolute() ) {
			file = new File(localdir, path);
		}
		return new File(file.getCanonicalPath());	// remove extra pathname like "." and ".."
	}
	
	/**
	 * Get local working directory.
	 * @return local working directory.
	 */
	public String getLocalDir() {
		return localdir.getAbsolutePath();
	}
	
	
	
	
	
	// ------------ Method about data transfer ------------
	
	/**
	 * Set transfer type as ASCII.
	 * Command "TYPE A" will be sent to server.
	 * @return true if server accepted this command.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized boolean setAscii() throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.REPRESENTATION_TYPE, "A"));
		return ( reply.getCode() == FtpReply.COMMAND_OK );
	}
	
	/**
	 * Set transfer type as Binary or Image.
	 * Command "TYPE I" will be sent to server.
	 * @return true is server accepted this command.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized boolean setBinary() throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.REPRESENTATION_TYPE, "I"));
		return ( reply.getCode() == FtpReply.COMMAND_OK );
	}
	
	/**
	 * Toggle passive mode.
	 * Clients behind firewall or NAT must set passive mode on to work correctly.
	 * @param mode - true to set passive mode on or false to set it off.
	 */
	public void setPassive(boolean mode) {
		passive = mode;
	}
	
	/**
	 * Request FTP server to listen on a data port and wait for connection.
	 * @return port number.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	private synchronized int getPassivePort() throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.PASSIVE));
		return reply.parseServerPassivePort();
	}
	
	/**
	 * Sent local host and port to FTP server to enable PORT data transfer.
	 * @param host - local host to be sent.
	 * @param port - local port to be sent.
	 * @return true if command successfully be executed.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	private synchronized boolean setDataPort(int[] host, int port) throws IOException, FtpException {
		StringBuffer argument = new StringBuffer();
		
		for ( int i = 0; i < host.length; i++ ) {
			argument.append(host[i]).append(",");
		}
		argument.append((port>>8)&0xFF).append(",").append(port&0xFF);
		FtpReply reply = sent(new FtpCommand(FtpCommand.DATA_PORT, argument.toString()));
		return ( reply.getCode() == FtpReply.COMMAND_OK );
	}
	
	/**
	 * Performs the first half of data connection establishment.
	 * 
	 * If <code>passive</code> data transfer will be used, it 
	 * opens a <code>java.net.Socket</code> on a specific port 
	 * of the FTP server.
	 *
	 * If <code>passive</code> data transfer will not be used, 
	 * it opens a new <code>ServerSocket</code> on the local host 
	 * and tells the FTP Server on which port to connect.
	 * 
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	private synchronized void establishDataSocket() throws IOException, FtpException {
		if ( passive ) {
			// get a port from server
			int port = getPassivePort();
			Socket socket = new Socket(serverhost, port);
			dataSocket = new FtpSocket(socket);
		} else {
			// get a free port on localhost and send it to server
			ServerSocket socket = new ServerSocket(0,1);
			int[] host = toInt(localhost.getAddress());
			int port = socket.getLocalPort();
			setDataPort(host, port);
			dataSocket = new FtpSocket(socket);
		}
	}
	
	/**
	 * Transfer data from <code>InputStream</code> to <code>OutputStream</code>.
	 * @param in - <code>InputStream</code> to be read.
	 * @param out - <code>OutputStream</code> to be written.
	 * @throws IOException problem of socket, stream...
	 */
	private synchronized void dataTransfer(InputStream in, OutputStream out) throws IOException {
		int amount;
		byte[] buffer = new byte[BUFFER_SIZE];
		
		while ( (amount = in.read(buffer)) > 0 ) {
			out.write(buffer, 0, amount);
		}
		dataSocket.close();
		out.close();
		in.close();
	}
	
	/**
	 * Read data from <code>InputStream</code>, append it to a <code>Vector</code> list.
	 * @param in - <code>InputStream</code> to be read.
	 * @param list - <code>Vector</code> to be appended.
	 * @throws IOException problem of socket, stream...
	 */
	private synchronized void dataTransfer(InputStream in, Vector<String> list) throws IOException {
		int amount;
		byte[] buffer = new byte[BUFFER_SIZE];
		StringBuffer sb = new StringBuffer();
		StringTokenizer token;
		
		while ( (amount = in.read(buffer)) > 0 ) {
			sb.append(new String(buffer, 0, amount));
		}
		dataSocket.close();
		in.close();
		
		token = new StringTokenizer(sb.toString(), "\r\n");
		while ( token.hasMoreTokens() ) {
			list.add(token.nextToken());
		}
	}
	
	
	
	
	
	// ------------ Method about service command ------------
	
	/**
	 * Get current working directory of server.
	 * Command "PWD" will be sent to server.
	 * @return current working directory.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized String getDir() throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.PRINT_WORKING_DIRECTORY)); 
		return reply.parseDir();
	}
	
	/**
	 * Get current working directory list of server.
	 * Either command "NLST path" or "LIST path" will be sent to server.
	 * @param details - false to send command NLST and true to send command LIST.
	 * @return directory list.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized Enumeration<String> getFileList(boolean details) throws IOException, FtpException {
		return getFileList(null, details);
	}
	
	/**
	 * Get specifc path directory list of server.
	 * Either command "NLST path" or "LIST path" will be sent to server.
	 * @param path - path to get directory list.
	 * @param details - false to send command NLST and true to send command LIST.
	 * @return directory list.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized Enumeration<String> getFileList(String path, boolean details) throws IOException, FtpException {
		getDir();
		setAscii();
		establishDataSocket();
		if ( details ) {
			sent(new FtpCommand(FtpCommand.LIST, path));
		} else {
			sent(new FtpCommand(FtpCommand.NAME_LIST, path));
		}
		Vector<String> list = new Vector<String>();
		dataTransfer(dataSocket.getInputStream(), list);
		getReply();
		return list.elements();
	}
	
	/**
	 * Download a file from server.
	 * @param filename - file to be download.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized void download(String filename) throws IOException, FtpException {
		setBinary();
		establishDataSocket();
		sent(new FtpCommand(FtpCommand.RETRIEVE, filename));
		RandomAccessFile file = new RandomAccessFile(new File(localdir, filename), "rw");
		dataTransfer(dataSocket.getInputStream(), new FileOutputStream(file.getFD()));
		file.close();
		getReply();
	}
	
	/**
	 * Upload a local file to server.
	 * @param filename - file to be uploaded.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized void upload(String filename) throws IOException, FtpException {
		RandomAccessFile file = new RandomAccessFile(mixPath(filename), "r");
		setBinary();
		establishDataSocket();
		sent(new FtpCommand(FtpCommand.STORE, filename));
		dataTransfer(new FileInputStream(file.getFD()), dataSocket.getOutputStream());
		file.close();
		getReply();
	}
	
	// 續傳檔案
	public synchronized void resume(String filename) throws IOException, FtpException {
		// TODO 續傳檔案
	}
	
	/**
	 * Delete remote file.
	 * Command "DELE filename" will be sent to server.
	 * @param filename - file to be removed.
	 * @return true if command successfully be executed.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized boolean delete(String filename) throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.DELETE, filename));
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	
	/**
	 * Rename remote filename.
	 * Command "RNFR filename" will be sent to notify server which file to be renamed.
	 * Command "RNTO filename" will be sent to notify server of the new filename.
	 * @param from - old filename.
	 * @param to - new filename.
	 * @return true if command successfully be executed.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized boolean rename(String from, String to) throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.RENAME_FROM, from));
		if ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_PENDING ) {
			reply = sent(new FtpCommand(FtpCommand.RENAME_TO, to));
		}
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	
	/**
	 * Create a directory at server.
	 * Command "MKD dir" will be sent to server.
	 * @param dir - directory name to be created.
	 * @return true if command successfully be executed.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */ 
	public synchronized boolean makeDir(String dir) throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.MAKE_DIRECTORY, dir));
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	
	/**
	 * Delete remote directory.
	 * Command "RMD dir" will be sent to server.
	 * @param dir - remote directory to be removed. 
	 * @return true if command successfully be executed.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	public synchronized boolean deleteDir(String dir) throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.REMOVE_DIRECTORY, dir));
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	// 中斷伺服端上一次本地端所下的指令
	public synchronized boolean abort() throws IOException, FtpException {
		FtpReply reply = sent(new FtpCommand(FtpCommand.ABORT));
		dataSocket.close();
		return ( reply.getCode() == FtpReply.REQUESTED_FILE_ACTION_OK );
	}
	
	
	
	
	
	// ------------ Method for sending command and receiving response ------------
	
	/**
	 * Send a command to server and read its response.
	 * @param command - FTP command to be sent to server.
	 * @return A <code>FtpReply</code> object that contains server response.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	protected FtpReply sent(FtpCommand command) throws IOException, FtpException {
		return sent(command, command.getCommand());
	}
	
	/**
	 * Send a command to server and read its response.
	 * @param command - FTP command to be sent to server.
	 * @param text - string to be displayed on screen, a <code>null</code> value will be
	 *               interpreted as not printing.
	 * @return A <code>FtpReply</code> object that contains server response.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	protected FtpReply sent(FtpCommand command, String text) throws IOException, FtpException {
		if ( !isConnected() ) {
			throw new FtpException("Not connected.");
		}
		try {
			writer.write(command.getCommand() + "\r\n");
			writer.flush();
		} catch ( IOException ioe ) {
			close();
			throw new FtpException(ioe.getMessage());
		}
		if ( (text != null) && (text.length() != 0) ) {
			commandMessage(text);
		}
		FtpReply reply =  getReply();
		return reply;
	}
	
	/**
	 * Read the response of server.  
	 * @return A <code>FtpReply</code> object that contains server response.
	 * @throws IOException problem of socket, stream...
	 * @throws FtpException negative response from server.
	 */
	protected FtpReply getReply() throws IOException, FtpException {
		int counter = 0;
		String line, c4 = null;
		Vector<String> response = new Vector<String>();
		
		// read response from server
		// response would be multi-line if the 4th character is '-'
		// continue reading until response starts with code and one space, e.g. "220 "
		do {
			line = reader.readLine();
			if ( line == null ) {
				close();
				throw new FtpException("Connection closed by host.");
			}
			response.add(line);
			if ( counter == 0 ) c4 = line.substring(0,3) + " ";
			counter++;
		} while ( !line.startsWith(c4) );
		
		// assign a variable to store response
		FtpReply reply = new FtpReply((String[]) response.toArray(new String[counter]));
		int code = reply.getCode();
		
		// determine if the response is positive
		if ( code >= 100 && code < 400 ) {
			responseMessage(reply.getMessage());
		} else {
			if ( code == FtpReply.SERVICE_NOT_AVAILABLE ) {
				close();
			}
			throw new FtpException(reply.getReply());
		}
		
		return reply;
	}
	
	/**
	 * Display command that sent by client.
	 * @param command - string to be displayed on screen.
	 */
	protected void commandMessage(String command) {
		if ( TOGGLE_COMMAND ) System.out.println("-> " + command);
	}
	
	/**
	 * Display response of server.
	 * @param message - string to be displayed on screen.
	 */
	protected void responseMessage(String[] message) {
		StringBuffer output = new StringBuffer();
		for ( int i = 0; i < message.length; i++ ) {
			output.append(message[i] + "\n");
		}
		System.out.print(output.toString());
	}
	
	
	
	
	
	// ------------ Method for transcripting data type ------------
	
	/**
	 * Transcript a <code>byte</code> value into an <code>int</code> value.
	 * @param value - value to be transcripted. 
	 * @return int value.
	 */
	private int toInt(byte value) {
		return (int) value & 0xFF;
	}
	
	/**
	 * Transcript a <code>byte</code> array value into an <code>int</code> array value.
	 * @param value - value to be transcripted.
	 * @return int value.
	 */
	private int[] toInt(byte[] value) {
		int[] out = new int[value.length];
		for ( int i = 0; i < value.length; i++ ) {
			out[i] = toInt(value[i]);
		}
		return out;
	}
}
