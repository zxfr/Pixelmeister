package org.zefer.pixelmeister.device.itdb02;

public class tinyFAT {
	
	public static class _directory_entry	{
		String filename;
		String fileext;
		byte attributes;
		int time;
		int date;
		int startCluster = 0;
		int fileSize;
	};


	public tinyFAT() {
	}

	public byte initFAT(byte speed) {
		return 0x00;
	}

	public byte findFirstFile(_directory_entry tempDE) {
		return 0;
	}

	public byte findNextFile(_directory_entry tempDE) {
		return 0;
	}

	public byte openFile(String fn, byte mode) {
		return 0;
	}

	public int readBinary() {
		return 0;
	}

	public int readLn(String st, int bufSize) {
		return 0;
	}

	public int writeLn(String st)	{
		return 0;
	}

	public void closeFile() {
	}

	public boolean exists(String fn) {
		return true;
	}

	public boolean rename(String fn1, String fn2) {
		return true;
	}

	public boolean delFile(String fn) {
		return true;
	}

	public boolean create(String fn) {
		return true;
	}

	/* Private */
//	private char uCase(char c) {
//		if ((c>='a') && (c<='z')) {
//			return (char)(c-0x20);
//		} else {
//			return c;
//		}
//	}
//
//	private boolean validChar(char c) {
//		char[] valid = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!#$&'()-@^_`{}~.".toCharArray();
//
//		for (int i=0; i < valid.length; i++) {
//			if (c==valid[i]) {
//				return true;
//			}
//		}
//		return false;
//	}
}

