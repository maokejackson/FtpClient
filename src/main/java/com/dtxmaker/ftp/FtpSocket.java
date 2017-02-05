package com.dtxmaker.ftp;

import java.io.*;
import java.net.*;

public class FtpSocket extends Object {
	
	private Object socket = null;
	
	public FtpSocket(Socket socket) {
		this.socket = socket;
	}
	
	public FtpSocket(ServerSocket socket) {
		this.socket = socket;
	}
	
	public boolean isSocket() {
		return (socket instanceof Socket);
	}
	
	public boolean isServerSocket() {
		return (socket instanceof ServerSocket);
	}
	
	public InetAddress getInetAddress() throws IOException {
		if ( isSocket() ) {
			return ((Socket)socket).getInetAddress();
		} else {
			return ((ServerSocket)socket).accept().getInetAddress();
		}
	}
	
	public int getLocalPort() throws IOException {
		if ( isSocket() ) {
			return ((Socket)socket).getLocalPort();
		} else {
			return ((ServerSocket)socket).accept().getLocalPort();
		}
	}
	
	public InputStream getInputStream() throws IOException {
		if ( isSocket() ) {
			return ((Socket)socket).getInputStream();
		} else {
			return ((ServerSocket)socket).accept().getInputStream();
		}
	}
	
	public OutputStream getOutputStream() throws IOException {
		if ( isSocket() ) {
			return ((Socket)socket).getOutputStream();
		} else {
			return ((ServerSocket)socket).accept().getOutputStream();
		}
	}
	
	public void close() throws IOException {
		if ( isSocket() ) {
			((Socket)socket).close();
		} else {
			((ServerSocket)socket).close();
		}
	}
	
}
