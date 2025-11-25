package org.zefer.pixelmeister.util;
/*
 * ===========================================================================
 * Licensed Materials - Property of zefer.org
 *
 * PD4ML HTML to PDF Converter for Java
 * http://pd4ml.zefer.org, 2003-2006
 * ===========================================================================
 */


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageChecker {

	private static int ctr = 0;

	public static final int UNKNOWN_TYPE = -1;
	public static final int GIF_TYPE = 1;
	public static final int JPEG_TYPE = 2;
	public static final int PNG_TYPE = 3;
	public static final int UNSUPPORTED_PNG_TYPE = 4;
	public static final int TIFF_TYPE = 5;
	public static final int BMP_TYPE = 6;
	
	public static int jpegBpp = 0;
	
	public static String getImageType( byte[] idata ) {

		String type = "UNKNOWN";

		if ( idata.length < 2 ) {
			return type;
		}

		try {
			int b1 = readByte(idata) & 0xff;
			int b2 = readByte(idata) & 0xff;
//			int b3 = readByte(idata) & 0xff;

			if (b1 == 0x47 && b2 == 0x49) {
				type = checkGif( idata );
			} else if (b1 == 0x89 && b2 == 0x50) {
				type = checkPng( idata ); 
			} else if (b1 == 0x49 && b2 == 0x49 || b1 == 0x4d && b2 == 0x4d) {
				type = "TIFF"; 
			} else if (b1 == 0x42 && b2 == 0x4d) {
					type = "BMP"; 
			} else if (b1 == 0xff && b2 == 0xd8) {
				type = checkJpeg( idata ); 
			}
		} catch (IOException ioe) {
		} 
		
		ctr = 0;
		jpegBpp = 0;
		return type;
	}

	private static String checkGif( byte[] idata ) throws IOException {
		final byte[] GIF_MAGIC_87A = {0x46, 0x38, 0x37, 0x61};
		final byte[] GIF_MAGIC_89A = {0x46, 0x38, 0x39, 0x61};
		if ( equals(idata, 2, GIF_MAGIC_89A, 0, 4) ) {
			return "GIF (89a)";
		}
		if ( equals(idata, 2, GIF_MAGIC_87A, 0, 4) ) {
			return "GIF (87a)";
		}
		return "UNKNOWN";
	}

	private static String checkJpeg( byte[] idata ) throws IOException {

		String type = "JPEG";
		String failed = "UNKNOWN";
		
		byte[] data = new byte[12];
		while (true) {
			if (read(idata, data, 0, 4) != 4) {
				return failed;
			}
			int key = getShortBigEndian(data, 0);
			int size = getShortBigEndian(data, 2);
			if ((key & 0xff00) != 0xff00) {
				return failed; // not a valid marker
			}
			
			if (key >= 0xffc0 && key <= 0xffcf && key != 0xffc4 && key != 0xffc8) {
				if (read(idata, data, 0, 6) != 6) {
					return failed;
				}

				String suffix = "";
				jpegBpp = (data[0] & 0xff) * (data[5] & 0xff);
				if ( jpegBpp == 8 ) {
					suffix = " (grayscale)";
				}

				if ( key == 0xffc2 || key == 0xffc6 ||
					key == 0xffca || key == 0xffce ) {
					// progressive
				}
				return type + suffix;
			} else {
				skip(size - 2);
			}
		}
	}

	private static String checkPng(byte[] idata) throws IOException {
		try {
			PD4PngParser pngParser = new PD4PngParser( idata );
			return pngParser.getType();
		} catch (Exception e) {
			return "UNKNOWN";
		}
	}

	public static byte[] readBinaryData( InputStream is ) {
		// reading the image data to a byte array
		ByteArrayOutputStream fos = new ByteArrayOutputStream();

		BufferedInputStream bis = null;
		byte buffer[] = new byte[2048];
		try {

			bis = new BufferedInputStream(is, buffer.length);

			int read = -1;
			do {
				read = is.read(buffer, 0, buffer.length);
				if (read > 0) { // something to put down
					fos.write(buffer, 0, read);
				}
			} while (read > 0);
			fos.close();
			bis.close();
			is.close();
			return fos.toByteArray();
			
		} catch (IOException ioe) {
			ioe.printStackTrace(System.err);
		}

		return null;
	}

	private static int read(byte[] idata, byte[] data, int i, int len) {
		if ( ctr + len >= idata.length ) {
			return -1; 
		}
		System.arraycopy( idata, ctr, data, i, len );
		ctr += len;
		return len;
	}

	private static final byte readByte(byte[] idata) {
		return idata[ctr++];
	}
	
	private static boolean equals(byte[] a1, int offs1, byte[] a2, int offs2, int num) {
		while (num-- > 0) {
			if (a1[offs1++] != a2[offs2++]) {
				return false;
			}
		}
		return true;
	}

	private static int getShortBigEndian(byte[] a, int offs) {
		return
			(a[offs] & 0xff) << 8 |
			(a[offs + 1] & 0xff);
	}

	@SuppressWarnings("unused")
	private static int getShortLittleEndian(byte[] a, int offs) {
		return (a[offs] & 0xff) | (a[offs + 1] & 0xff) << 8;
	}

	private static void skip(int num) {
		ctr += num;
	}
}
