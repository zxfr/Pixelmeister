/*
 * ===========================================================================
 * Licensed Materials - Property of zefer.org
 *
 * PD4ML HTML to PDF Converter for Java
 * http://pd4ml.zefer.org, 2003-2006
 * ===========================================================================
 */

package org.zefer.pixelmeister.util;


public class PD4PngParser {

	public static final byte[] SIGNATURE = {(byte)0x89, (byte)0x50, (byte)0x4e, (byte)0x47, 
		                                (byte)0x0d, (byte)0x0a, (byte)0x1a, (byte)0x0a};
  
	// image data buffer    
	private byte[] imageBuffer;

	// buffer read pointer
	private int ctr = 0;

	public int width;
	public int height;

	int colorType;
	int bitDepth;
	
	public boolean unsupportedType = false;

	String type = "PNG";
	String typeSuffix = "";
	
	public String getType() {
		if ( typeSuffix.length() > 2 ) {
			typeSuffix = typeSuffix.substring(2);
//			System.out.println(type + " (" + typeSuffix + ")");
			return type + " (" + typeSuffix + ")";
		} else {
			return type;
		}
	}
	
	public PD4PngParser( byte[] is ) throws Exception {
		this.imageBuffer = is;
		parse();
	}

    private void parse() throws Exception {
        for (int i = 0; i < SIGNATURE.length; i++) {
            if ( SIGNATURE[i] != readByte() )	{
                throw new Exception( "file error 1." );
            }
        }
		ctr = 8;
		boolean done = false;

		while ( !done ) {
            int len = readInt();
            String marker = readString();
            if ( len < 0 ) {
				throw new Exception("file error 2.");
            }
			if ("IHDR".equals(marker)) {
				width = readInt();
				height = readInt();
				bitDepth = readByte();
				colorType = readByte();
				readByte(); // compressionMethod 
				readByte(); // filterMethod
				int interlaceMethod = readByte();
				
				if ( colorType == 3 ) {
					type += "8";
				} else if ( colorType == 0 ) {
					
				} else {
					type += "24";
				}
				if ( bitDepth != 8 ) {
					typeSuffix += ", " + bitDepth + "bit";
				}
				typeSuffix += getColorspace( colorType );
				if ( interlaceMethod > 0 ) {
					typeSuffix += ", intrl";
				}
			} else if ("IDAT".equals(marker)) {
				ctr += len;
			} else if ("iCCP".equals(marker)) {
                do {
                    len--;
                } while (imageBuffer[ctr++] != 0);
                ctr++;
                len--;
				ctr += len;
			} else if ("PLTE".equals(marker)) {
				ctr += len;
            } else if (	"tRNS".equals(marker) ) {
                ctr += len;
    			typeSuffix += ", trnsp";
            } else if ("IEND".equals(marker)) {
                done = true;
            } else {
                ctr += len;
            }
            ctr += 4;
        };
    }
    
    private final int readInt() {
    	int i = 0;
    	i += ((0xff & readByte()) << 24);
		i += ((0xff & readByte()) << 16); 
		i += ((0xff & readByte()) << 8); 
		i += 0xff & readByte();
		return i;
    }
    
	private final byte readByte() {
		return imageBuffer[ctr++];
	}
    
	private final String readString() throws Exception {
		char[] b = {
			(char)readByte(),
			(char)readByte(),
			(char)readByte(),
			(char)readByte(),
			};

		for (int i = 0; i < 4; i++) {
			if ((b[i] < 'a' || b[i] > 'z') && (b[i] < 'A' || b[i] > 'Z'))
				throw new Exception("file error 3.");
		}
		
        return new String(b);
    }
    
	private static String getColorspace( int colorType ) {
		switch (colorType) {
			case 0: 
				return ", BW";
			case 2: 
				return "";
			case 3:
				return "";
			case 4:
				return ", BW";
			case 6:
				return ", trnsl";
		}
		return "";
	}
}
