package com.dtxmaker.ftp.client;

import java.util.*;

import com.dtxmaker.ftp.*;

import java.io.*;

public class FtpClient {
	
	private static final String[] HELP = new String[] {
			"?",			"Print local help information",
// TODO		"append",		"Append to a file",
			"ascii",		"Set ascii transfer type",
			"binary",		"Set binary transfer type",
			"bye",			"Terminate ftp session and exit",
			"cd",			"Change remote working directory",
			"close",		"Terminate ftp session",
			"delete",		"Delete remote file",
			"dir",			"List contents of remote directory",
			"disconnect",	"Terminate ftp session",
			"get",			"Receive file",
			"help",			"Print local help information",
			"lcd",			"Change local working directory",
			"ls",			"List contents of remote directory",
			"mkdir",		"Make directory on the remote machine",
			"open",			"Connect to remote tftp",
			"put",			"Send one file",
			"pwd",			"Print working directory on remote machine",
			"quit",			"Terminate ftp session and exit",
			"recv",			"Receive file",
			"rename",		"Rename file",
			"rmdir",		"Remove directory on the remote machine",
			"send",			"Send one file",
			"user",			"Send new user information"
		};
	
	
	public static void main(String[] args) throws IOException {
		Ftp ftp = null;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			if ( args.length == 0 ) {
				ftp = new Ftp();
			} else if ( args.length == 1 ) {
				ftp = new Ftp(args[0]);
				ftp.connect();
				if ( ftp.isConnected() ) doLogin(input, ftp);
			} else {
				ftp = new Ftp(args[0], Integer.parseInt(args[1]));
				ftp.connect();
				if ( ftp.isConnected() ) doLogin(input, ftp);
			}
		} catch ( FtpException ftpe ) {
			System.out.println("FTP error: " + ftpe.getMessage());
		} catch ( IOException ioe ) {
			System.out.println("I/O error: " + ioe.getMessage());
		}
		
		while ( true ) {
			try {
				System.out.print("ftp> ");
				String[] cmd = parseCommand(input.readLine());
				int argv = cmd.length - 1;
				
				// 設定檔案傳輸型態為Ascii
				if ( cmd[0].equals("ascii") ) {
					ftp.setAscii();
					
				// 續傳檔案
				} else if ( cmd[0].equals("append") ) {
					// TODO 續傳檔案
					notImplement();
					
				//	設定檔案傳輸型態為Binary
				} else if ( cmd[0].equals("binary") ) {
					ftp.setBinary();
					
				// 登出並結束Ftp程式
				} else if ( cmd[0].equals("bye") || cmd[0].equals("quit") ) {
					if ( ftp.isConnected() ) {
						ftp.disconnect();
					}
					break;
					
				// 切換伺服端工作目錄
				} else if ( cmd[0].equals("cd") ) {
					if ( argv >= 1 ) {
						ftp.setDir(cmd[1]);
					} else {
						usage(cmd[0], "directory");
					}
					
				// 結束本地端與伺服端的通訊連結
				} else if ( cmd[0].equals("close") || cmd[0].equals("disconnect") ) {
					ftp.disconnect();
					
				// 刪除伺服端檔案
				} else if ( cmd[0].equals("delete") ) {
					if ( argv >= 1 ) {
						ftp.delete(cmd[1]);
					} else {
						usage(cmd[0], "filename");
					}
					
				// 列出伺服端詳細目錄清單
				} else if ( cmd[0].equals("dir") ) {
					Enumeration<?> list;
					if ( argv >= 1 ) {
						list = ftp.getFileList(cmd[1], true);
					} else {
						list = ftp.getFileList(true);
					}
					while ( list.hasMoreElements() ) System.out.println(list.nextElement());
					
				// 從伺服端下載檔案
				} else if ( cmd[0].equals("get") || cmd[0].equals("recv") ) {
					if ( argv >= 1 ) {
						ftp.download(cmd[1]);
					} else {
						usage(cmd[0], "filename");
					}
					
				// FTP指令說明
				} else if ( cmd[0].equals("help") || cmd[0].equals("?") ) {
					if ( argv >= 1 ) {
						help(cmd[1]);
					} else {
						help();
					}
					
				// 切換本地端當前工作目錄
				} else if ( cmd[0].equals("lcd")) {
					if ( argv >= 1 ) {
						if ( ftp.setLocalDir(cmd[1]) ) {
							System.out.println("Directory changed to " + ftp.getLocalDir());
						} else {
							System.out.println("Directory " + cmd[1] + " not found.");
						}
					} else {
						System.out.println(ftp.getLocalDir());
					}
					
				// 列出伺服端目錄清單
				} else if ( cmd[0].equals("ls") ) {
					Enumeration<?> list;
					if ( argv >= 1 ) {
						list = ftp.getFileList(cmd[1], false);
					} else {
						list = ftp.getFileList(false);
					}
					while ( list.hasMoreElements() ) System.out.println(list.nextElement());
					
				// 在伺服端建立目錄
				} else if ( cmd[0].equals("mkdir") ) {
					if ( argv >= 1 ) {
						ftp.makeDir(cmd[1]);
					} else {
						usage(cmd[0], "directory");
					}
					
				// 與伺服端建立通訊連結
				} else if ( cmd[0].equals("open") ) {
					if ( argv >= 2 ) {
						ftp.connect(cmd[1], Integer.parseInt(cmd[2]));
						if ( ftp.isConnected() ) {
							doLogin(input, ftp);
						}
					} else if ( argv == 1 ) {
						ftp.connect(cmd[1], 21);
						if ( ftp.isConnected() ) {
							doLogin(input, ftp);
						}
					} else {
						usage(cmd[0], "hostname [port]");
					}
					
				// 上傳檔案到伺服端
				} else if ( cmd[0].equals("put") || cmd[0].equals("send") ) {
					if ( argv >= 1 ) {
						ftp.upload(cmd[1]);
					} else {
						usage(cmd[0], "filename");
					}
					
				// 顯示伺服端當前工作目錄
				} else if ( cmd[0].equals("pwd") ) {
					ftp.getDir();
					
				// 重新命名伺服端檔案
				} else if ( cmd[0].equals("rename") ) {
					if ( argv >= 2 ) ftp.rename(cmd[1], cmd[2]);
					else usage(cmd[0], "from to");
					
				// 移除伺服端目錄
				} else if ( cmd[0].equals("rmdir") ) {
					if ( argv >= 1 ) {
						ftp.deleteDir(cmd[1]);
					} else {
						usage(cmd[0], "directory");
					}
					
				// 登入伺服端
				} else if ( cmd[0].equals("user") ) {
					if ( argv >= 2 ) {
						ftp.login(cmd[1], cmd[2]);
					} else if ( argv == 1 ) {
						ftp.login(cmd[1], "");
					} else {
						usage(cmd[0], "username [password]");
					}
					
				// 非法指令
				} else {
					System.out.println("Invalid command.");
				}
				
			} catch ( FtpException ftpe ) {
				System.out.println("FTP error: " + ftpe.getMessage());
			} catch ( IOException ioe ) {
				System.out.println("I/O error: " + ioe.getMessage());
			}
		}
		input.close();
	}
	
	// 解析指令把多餘的空格去掉，並且把雙引號內的文字視為一個參數
	private static String[] parseCommand(String command) {
		int counter = 0;
		boolean start = true;
		Vector<String> v = new Vector<String>();
		StringTokenizer st = new StringTokenizer(command, "\"", true);
		
		while ( st.hasMoreTokens() ) {
			String hold = st.nextToken();
			if ( hold.equals("\"") ) {
				if ( start ) {
					v.add(st.nextToken());
					counter++;
					start = false;
				} else {
					start = true;
				}
			} else {
				StringTokenizer st2 = new StringTokenizer(hold, "\t ");
				while ( st2.hasMoreTokens() ) {
					v.add(st2.nextToken());
					counter++;
				}
			}
		}
		
		return (String[]) v.toArray(new String[counter]);
	}
	
	// 執行登入的動作
	private static void doLogin(BufferedReader in, Ftp ftp) throws IOException, FtpException {
		System.out.print("username: ");
		String username = in.readLine();
		System.out.print("password: ");
		String password = in.readLine();
		ftp.login(username, password);
	}
	
	// 顯示本地端所支援的指令
	private static void help() {
		for ( int i = 0; i < HELP.length; i+=2 ) {
			if ( (i%10) == 8 ) {
				System.out.println(HELP[i]);
			} else {
				System.out.print(HELP[i]);
				for ( int j = HELP[i].length(); j < 16; j++ ) {
					System.out.print(" ");
				}
			}
		}
		System.out.println("");
	}
	
	// 顯示指令的說明文字
	private static void help(String cmd) {
		for ( int i = 0; i < HELP.length; i+=2 ) {
			if ( HELP[i].equals(cmd) ) {
				System.out.print(cmd + "\t");
				if ( HELP[i].length() < 8 ) System.out.print("\t");
				System.out.println(HELP[i+1]);
				return;
			}
		}
		System.out.println("Invalid help command " + cmd + ".");
	}
	
	// 顯示指令的用法
	private static void usage(String cmd, String arg) {
		System.out.println("Usage: " + cmd + " " + arg);
	}
	
	// 功能未實作時顯示的訊息
	// 所有功能完成後將被移除
	private static void notImplement() {
		System.out.println("Not implemented yet.");
	}
	
}
