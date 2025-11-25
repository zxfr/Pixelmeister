/*
 * ===========================================================================
 * Licensed Materials - Property of zefer.org
 *
 * PD4ML HTML to PDF Converter for Java
 * http://pd4ml.zefer.org, 2003-2006
 * ===========================================================================
 */

package org.zefer.pixelmeister.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class BytesReader {
//	begin_pro_version		
	private byte data[];
	private int ptr = 0;
	
	public BytesReader( byte[] data ) {
		this.data = data;    
	}
	
	public String readString( int n ) {
		if ( n <= 0 ) {
			return null;
		}
		
		byte[] tmp = new byte[ n ];
		
		System.arraycopy( data, ptr, tmp, 0, n ); 
		ptr += n;

		try {
			return new String(tmp, TrueType.CP1252);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Reads a Unicode <CODE>String</CODE> from the byte array. Each character is
	 *  represented by two bytes.
	 * @param length the length of bytes to read. The <CODE>String</CODE> will have <CODE>length</CODE>/2
	 * characters
	 * @return the <CODE>String</CODE> read
	 * @throws IOException the font file could not be read
	 */
	protected String readUnicodeString(int length) throws IOException {
		StringBuffer buf = new StringBuffer();
		length /= 2;
		for (int k = 0; k < length; ++k) {
			buf.append( readChar() );
		}
		return buf.toString();
	}

	public char readChar() throws IOException {
		int ch1 = (0xff & data[ptr++]);
		int ch2 = (0xff & data[ptr++]);
		if ((ch1 | ch2) < 0)
			throw new IOException();
		return (char)((ch1 << 8) + ch2);
	}

	public int readInt() throws IOException {
		int ch1 = (0xff & data[ptr++]);
		int ch2 = (0xff & data[ptr++]);
		int ch3 = (0xff & data[ptr++]);
		int ch4 = (0xff & data[ptr++]);
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new IOException();
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
	}

	public int readUnsignedShort() throws IOException {
		int ch1 = (0xff & data[ptr++]);
		int ch2 = (0xff & data[ptr++]);
		if ((ch1 | ch2) < 0)
			throw new IOException();
		return (ch1 << 8) + ch2;
	}

	public short readShort() throws IOException {
		int ch1 = (0xff & data[ptr++]);
		int ch2 = (0xff & data[ptr++]);
		if ((ch1 | ch2) < 0)
			throw new IOException();
		return (short)((ch1 << 8) + ch2);
	}

	public int readUnsignedByte() throws IOException {
		int ch = (0xff & data[ptr++]);
		if (ch < 0)
			throw new IOException();
		return ch;
	}

	public void readFully(byte b[]) throws IOException {
		int n = b.length;
		System.arraycopy( data, ptr, b, 0, n ); 
		ptr += n;
	}

	public void readFully(byte b[], int off, int len) throws IOException {
		System.arraycopy( data, ptr, b, off, len ); 
		ptr += len;
	}

	public void skipBytes( int n ) {
		if ( n > 0 ) {
			ptr += n;
		}
	}

	public void seek( int n ) {
		if ( n >= 0 ) {
			ptr = n;
		}
	}

	public void close() {
		ptr = 0;
	}

	public int getPointer() {
		return ptr;
	}
//	end_pro_version		
}