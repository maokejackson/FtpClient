package com.dtxmaker.ftp;

public class FtpReply {
	
	private static final int INVALID_REPLY = -1;
	public static final int RESTART_MARKER_REPLY = 110;
	public static final int SERVICE_READY_IN_MINUTES = 120;
	public static final int DATA_CONNECTION_ALREADY_OPENED = 125;
	public static final int ABOUT_TO_OPEN_DATA_CONNECTION = 150;
	public static final int COMMAND_OK = 200;
	public static final int SUPERFLOUS_COMMAND = 202;
	public static final int SYSTEM_STATUS_OR_HELP_REPLY = 211;
	public static final int DIRECTORY_STATUS = 212;
	public static final int FILE_STATUS = 213;
	public static final int HELP_MESSAGE = 214;
	public static final int SYSTEM_TYPE_NAME = 215;
	public static final int CONTROL_CONNECTION_OPENED = 220;
	public static final int CONTROL_CONNECTION_CLOSED = 221;
	public static final int DATA_CONNECTION_OPENED = 225;
	public static final int DATA_CONNECTION_CLOSED = 226;
	public static final int ENTERING_PASSIVE_MODE = 227;
	public static final int USER_LOGGED_IN = 230;
	public static final int REQUESTED_FILE_ACTION_OK = 250;
	public static final int PATHNAME_CREATED = 257;
	public static final int USERNAME_OK_NEED_PASSWORD = 331;
	public static final int NEED_ACCOUNT_FOR_LOGIN = 332;
	public static final int REQUESTED_FILE_ACTION_PENDING = 350;
	public static final int SERVICE_NOT_AVAILABLE = 421;
	public static final int CANT_OPEN_DATA_CONNECTION = 425;
	public static final int DATA_CONNECTION_ABORTED = 426;
	public static final int TRANSIENT_UNAVAILABLE_FILE = 450;
	public static final int TRANSIENT_LOCAL_PROCESSING_ERROR = 451;
	public static final int TRANSIENT_INSUFFICIENT_STORAGE_SPACE = 452;
	public static final int UNRECOGNIZED_COMMAND = 500;
	public static final int SYNTAX_ERROR_IN_ARGUMENTS = 501;
	public static final int COMMAND_NOT_IMPLEMENTED = 502;
	public static final int BAD_COMMAND_SEQUENCE = 503;
	public static final int COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER = 504;
	public static final int NOT_LOGGED_IN = 530;
	public static final int NEED_ACCOUNT_FOR_STORING_FILES = 532;
	public static final int UNAVAILABLE_FILE = 550;
	public static final int PAGE_TYPE_UNKNOWN = 551;
	public static final int EXCEEDED_STORAGE_ALLOCATION = 552;
	public static final int FINE_NAME_NOT_ALLOWED = 553;
	
	private int code = INVALID_REPLY;
	private String reply;
	private String[] message;
	
	/**
	 * Construct a new instance of this class.
	 * @param response - original response message received from FTP server.
	 */
	public FtpReply(String[] response) {
		code = parseCode(response[0]);
		reply = response[response.length-1];
		message = response;
	}
	
	/**
	 * Return the FTP code of this response.
	 * @return FTP code.
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * Return last message received fro the FTP server.
	 * @return last message.
	 */
	public String getReply() {
		return reply;
	}
	
	/**
	 * Return original message received from the FTP server.
	 * @return original message.
	 */
	public String[] getMessage() {
		return message;
	}
	
	/**
	 * Get the code contained in <code>reply</code>.
	 * The code is in fact the first 3 characters of the <code>reply</code>.
	 * @param reply - string to be interpreted.
	 * @return code.
	 */
	public static int parseCode(String reply) {
		return Integer.parseInt(reply.substring(0,3));
	}
	
	/**
	 * Get server working directory by parsing <code>reply</code>.
	 * Directory will be lie between double quote, e.g. "/pub" 
	 * @return directory.
	 */
	public String parseDir() {
		return getSubString(reply, "\"");
	}
	
	/**
	 * Get server port by parsing <code>reply</code>.
	 * The server ip and port will be in bracket, e.g. (127.0.0.1.21.69), 
	 * where first 4 numbers are ip and last 2 numbers are port.
	 * The port should be merged in this situation, 
	 * 1st number * 256 + 2nd number.
	 * @return port number.
	 */
	public int parseServerPassivePort() {
		String[] tmp = getSubString(reply, "(", ")").split(",");
		return (Integer.parseInt(tmp[4])<<8) + Integer.parseInt(tmp[5]);
	}
	
	/**
	 * Get strings that lie between <code>delim</code>.
	 * @param text - string to be filtered.
	 * @param delim - delimiter to filter out string.
	 * @return sub string.
	 */
	private String getSubString(String text, String delim) {
		return getSubString(text, delim, delim);
	}
	
	/**
	 * Get strings that lie between <code>start</code> and <code>stop</code>. 
	 * @param text - string to be filtered.
	 * @param start - start filtering from here.
	 * @param stop - stop filtering at here.
	 * @return sub string.
	 */
	private String getSubString(String text, String start, String stop) {
		int startIndex = text.indexOf(start);
		int stopIndex = text.lastIndexOf(stop);
		if ( (startIndex == stopIndex) || (startIndex < 0) ) {
			return null;
		} else {
			return text.substring(startIndex+1, stopIndex);
		}
	}
	
}
