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
				
				// �]�w�ɮ׶ǿ髬�A��Ascii
				if ( cmd[0].equals("ascii") ) {
					ftp.setAscii();
					
				// ����ɮ�
				} else if ( cmd[0].equals("append") ) {
					// TODO ����ɮ�
					notImplement();
					
				//	�]�w�ɮ׶ǿ髬�A��Binary
				} else if ( cmd[0].equals("binary") ) {
					ftp.setBinary();
					
				// �n�X�õ���Ftp�{��
				} else if ( cmd[0].equals("bye") || cmd[0].equals("quit") ) {
					if ( ftp.isConnected() ) {
						ftp.disconnect();
					}
					break;
					
				// �������A�ݤu�@�ؿ�
				} else if ( cmd[0].equals("cd") ) {
					if ( argv >= 1 ) {
						ftp.setDir(cmd[1]);
					} else {
						usage(cmd[0], "directory");
					}
					
				// �������a�ݻP���A�ݪ��q�T�s��
				} else if ( cmd[0].equals("close") || cmd[0].equals("disconnect") ) {
					ftp.disconnect();
					
				// �R�����A���ɮ�
				} else if ( cmd[0].equals("delete") ) {
					if ( argv >= 1 ) {
						ftp.delete(cmd[1]);
					} else {
						usage(cmd[0], "filename");
					}
					
				// �C�X���A�ݸԲӥؿ��M��
				} else if ( cmd[0].equals("dir") ) {
					Enumeration<?> list;
					if ( argv >= 1 ) {
						list = ftp.getFileList(cmd[1], true);
					} else {
						list = ftp.getFileList(true);
					}
					while ( list.hasMoreElements() ) System.out.println(list.nextElement());
					
				// �q���A�ݤU���ɮ�
				} else if ( cmd[0].equals("get") || cmd[0].equals("recv") ) {
					if ( argv >= 1 ) {
						ftp.download(cmd[1]);
					} else {
						usage(cmd[0], "filename");
					}
					
				// FTP���O����
				} else if ( cmd[0].equals("help") || cmd[0].equals("?") ) {
					if ( argv >= 1 ) {
						help(cmd[1]);
					} else {
						help();
					}
					
				// �������a�ݷ�e�u�@�ؿ�
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
					
				// �C�X���A�ݥؿ��M��
				} else if ( cmd[0].equals("ls") ) {
					Enumeration<?> list;
					if ( argv >= 1 ) {
						list = ftp.getFileList(cmd[1], false);
					} else {
						list = ftp.getFileList(false);
					}
					while ( list.hasMoreElements() ) System.out.println(list.nextElement());
					
				// �b���A�ݫإߥؿ�
				} else if ( cmd[0].equals("mkdir") ) {
					if ( argv >= 1 ) {
						ftp.makeDir(cmd[1]);
					} else {
						usage(cmd[0], "directory");
					}
					
				// �P���A�ݫإ߳q�T�s��
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
					
				// �W���ɮר���A��
				} else if ( cmd[0].equals("put") || cmd[0].equals("send") ) {
					if ( argv >= 1 ) {
						ftp.upload(cmd[1]);
					} else {
						usage(cmd[0], "filename");
					}
					
				// ��ܦ��A�ݷ�e�u�@�ؿ�
				} else if ( cmd[0].equals("pwd") ) {
					ftp.getDir();
					
				// ���s�R�W���A���ɮ�
				} else if ( cmd[0].equals("rename") ) {
					if ( argv >= 2 ) ftp.rename(cmd[1], cmd[2]);
					else usage(cmd[0], "from to");
					
				// �������A�ݥؿ�
				} else if ( cmd[0].equals("rmdir") ) {
					if ( argv >= 1 ) {
						ftp.deleteDir(cmd[1]);
					} else {
						usage(cmd[0], "directory");
					}
					
				// �n�J���A��
				} else if ( cmd[0].equals("user") ) {
					if ( argv >= 2 ) {
						ftp.login(cmd[1], cmd[2]);
					} else if ( argv == 1 ) {
						ftp.login(cmd[1], "");
					} else {
						usage(cmd[0], "username [password]");
					}
					
				// �D�k���O
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
	
	// �ѪR���O��h�l���Ů�h���A�åB�����޸�������r�����@�ӰѼ�
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
	
	// ����n�J���ʧ@
	private static void doLogin(BufferedReader in, Ftp ftp) throws IOException, FtpException {
		System.out.print("username: ");
		String username = in.readLine();
		System.out.print("password: ");
		String password = in.readLine();
		ftp.login(username, password);
	}
	
	// ��ܥ��a�ݩҤ䴩�����O
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
	
	// ��ܫ��O��������r
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
	
	// ��ܫ��O���Ϊk
	private static void usage(String cmd, String arg) {
		System.out.println("Usage: " + cmd + " " + arg);
	}
	
	// �\�ॼ��@����ܪ��T��
	// �Ҧ��\�৹����N�Q����
	private static void notImplement() {
		System.out.println("Not implemented yet.");
	}
	
}
