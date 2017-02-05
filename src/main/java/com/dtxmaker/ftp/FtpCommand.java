package com.dtxmaker.ftp;

public class FtpCommand {
	
	private static final int INVALID_COMMAND = -1;
	public static final int USER_NAME = 0;
	public static final int PASSWORD = 1;
	public static final int ACCOUNT = 2;
	public static final int CHANGE_WORKING_DIRECTORY = 3;
	public static final int CHANGE_TO_PARENT_DIRECTORY = 4;
	public static final int STRUCTURE_MOUNT = 5;
	public static final int REINITIALIZE = 6;
	public static final int LOGOUT = 7;
	public static final int DATA_PORT = 8;
	public static final int PASSIVE = 9;
	public static final int REPRESENTATION_TYPE = 10;
	public static final int FILE_STRUCTURE = 11;
	public static final int TRANSFER_MODE = 12;
	public static final int RETRIEVE = 13;
	public static final int STORE = 14;
	public static final int STORE_UNIQUE = 15;
	public static final int APPEND = 16;
	public static final int ALLOCATE = 17;
	public static final int RESTART = 18;
	public static final int RENAME_FROM = 19;
	public static final int RENAME_TO = 20;
	public static final int ABORT = 21;
	public static final int DELETE = 22;
	public static final int REMOVE_DIRECTORY = 23;
	public static final int MAKE_DIRECTORY = 24;
	public static final int PRINT_WORKING_DIRECTORY = 25;
	public static final int LIST = 26;
	public static final int NAME_LIST = 27;
	public static final int SITE_PARAMETERS = 28;
	public static final int SYSTEM = 29;
	public static final int STATUS = 30;
	public static final int HELP = 31;
	public static final int NOOP = 32;
	
	/**
 	 * The list of allowed commands. Indexes are associated to 
	 * the command codes defined above.
	 */
	private static final String[] COMMANDS = new String[] {
		"USER", "PASS", "ACCT", "CWD",  "CDUP", "SMNT",
		"REIN", "QUIT", "PORT", "PASV", "TYPE", "STRU",
		"MODE", "RETR", "STOR", "STOU", "APPE", "ALLO",
		"REST", "RNFR", "RNTO", "ABOR", "DELE", "RMD",
		"MKD",  "PWD",  "LIST", "NLST", "SITE", "SYST",
		"STAT", "HELP", "NOOP" };
	
	private int code = INVALID_COMMAND;
	private String argument = null;
	
	/**
	 * Construct a new instance of this class with <code>argument</code>
	 * set to <code>null</code>.
	 * @param code - FTP command to represent.
	 */
	public FtpCommand(int code) {
		this(code, null);
	}
	
	/**
	 * Construct a new instance of this class.
	 * @param code - FTP command to represent.
	 * @param argument - argument to the FTP <code>code</code>.
	 */
	public FtpCommand(int code, String argument) {
		this.code = code;
		this.argument = argument;
	}
	
	/**
	 * Returns the FTP code that has to be printed to the 
	 * control connection for the server to understand the 
	 * request.
	 * @return a valid FTP command to be interpreted by the server.
 	 */
	public String getCommand() {
		if ( argument == null || argument.length() == 0 ) {
			return COMMANDS[code];
		} else {
			return new StringBuffer(COMMANDS[code]).append(" ").append(argument).toString();
		}
	}

}
