package org.zefer.pixelmeister.util;

import java.util.ArrayList;


public class Compressor {

	public static final int WINDOW_LEN = 254;
	public static final int MAX_MATCH = 254;
	
	private static String ruler = ".123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789";
	
//	private static byte[] rawData = ("a123456789b123456789c123456789d123456789e123456789f123456789g123456789h123456789i123456789j123456789k123456789l123456789m12345678a123456789b123456789").getBytes();

	
	private static byte[] rawData = ("                                             Miss Kalissippi from Mississippi is a "
			+ "cowgirl who yells yippppppppppi when she rides her horse in           "
			+ "the horse show in Mississippi.").getBytes();

//	private static byte[] rawData = ("     ....    .                     ").getBytes();
	
	
	private static int searchWindowLen = WINDOW_LEN;
	private static boolean debugOn = false;
	
	public static void main(String[] args) {
		
		byte[] result = Compressor.compress(rawData, 100, 200);
		int compLength = result.length;
		System.out.println("\n" + ruler);
		display120(rawData);
		result = Compressor.uncompress(result);
		System.out.println("\n" + ruler);
		display120(result);

		int msgLength = rawData.length;
		System.out.println("\nRaw data length = " + msgLength + " bytes.");
		System.out.println("Compressed length = " + compLength + " bytes.");
		System.out.println("Compression factor = " + (double) msgLength / compLength);
	}

	public static byte[] compress ( byte[] data, int width, int height ) {
		
		BitStream bbs = new BitStream();
		
		int maxOffset = 0;
		byte[] searchWindow;
		int ptr = 0;
		while (ptr < data.length) {
			
			int searchWindowStart = (ptr - searchWindowLen >= 0) ? ptr - searchWindowLen : 0;

			if (ptr == 0) {
				// Begin with an empty search window.
				searchWindow = new byte[0];
			} else {
				searchWindow = new byte[ptr - searchWindowStart];
				System.arraycopy(data, searchWindowStart, searchWindow, 0, ptr - searchWindowStart);
			}
			
			int[] ii = indicesOf(searchWindow, data[ptr]);
			
			if ( ii != null ) {
				int bestLength = 0;
				int bestOffset = 0;
				
				for ( int i = 0; i < ii.length; i++ ) {
					int matchLength = matchingBytes( searchWindow, ii[i], data, ptr, MAX_MATCH );
					if ( matchLength == bestLength ) {
						if ( (searchWindow.length - ii[i]) < bestOffset ) {
							bestOffset = searchWindow.length - ii[i];
						}
					} else if ( matchLength > bestLength ) {
						bestLength = matchLength;
						bestOffset = searchWindow.length - ii[i];
					}
				}

				String o = BitStream.encodeNumber(bestOffset + 1);
				String c = BitStream.encodeNumber(bestLength + 1);

				int allocatesUncompressed = bestLength * 9;
				int allocatesCompressed = o.length() + c.length() + 1;
				
				if ( maxOffset < bestOffset ) {
					maxOffset = bestOffset;
				}
				
				if ( allocatesCompressed > allocatesUncompressed ) {
					debug("'"+(char)data[ptr]+"'");
					
					bbs.write(0, 1);
					bbs.write(data[ptr], 8);
				} else {
					ptr += bestLength;
					
					debug(bestOffset+","+bestLength);
					bbs.write(1, 1);
					bbs.write(o);
					bbs.write(c);
					bbs.write(0, 1);
					if (ptr < data.length) {
						debug("'"+(char)data[ptr]+"'");
						bbs.write(data[ptr], 8);
					}
				}
			} else {
				debug("'"+(char)data[ptr]+"'");
				bbs.write(0, 1);
				bbs.write(data[ptr], 8);
			}

			ptr++;
		}

		bbs.writePrefix( data.length, maxOffset, width, height );
		bbs.writeDone();
		
		return bbs.data;
	}
	
	public static byte[] uncompress ( byte[] data ) {

		if ( data == null || data.length < 12 ) {
			return null;
		}
		
		if ( data[0] != 'Z' ) {
			System.out.println( "Unknown compression method" );
			return null;
		}
		
		int compressedLen = ((0xFF & data[1]) << 16) + ((0xFF & data[2]) << 8) + (0xFF & data[3]);
		if ( compressedLen < 0 || compressedLen != data.length - 12 ) {
			System.out.println( "Unknown compression method or compressed data inconsistence" );
			return null;
		}
		
		int resultLen = ((0xFF & data[4]) << 16) + ((0xFF & data[5]) << 8) + (0xFF & data[6]);
		if ( resultLen < 0 ) {
			System.out.println( "Unknown compression method or compression format error" );
			return null;
		}

		int windowLen = 0xFF & data[7];

		int width = ((0xFF & data[8]) << 8) + (0xFF & data[9]);
		if ( width <= 0 ) {
			System.out.println( "Unknown compression method or compression format error (width parameter is invalid)" );
			return null;
		}
		
		int height = ((0xFF & data[10]) << 8) + (0xFF & data[11]);
		if ( height <= 0 ) {
			System.out.println( "Unknown compression method or compression format error (height parameter is invalid)" );
			return null;
		}
		
		byte[] window = new byte[windowLen];
		int wptr = 0;
		
		byte[] result = new byte[resultLen];
		int ctr = 0;
		
		BitStream bs = new BitStream( data, 96 ); // 96bit = 12bytes of prefix
		while ( true ) {
			
			int bit = bs.readBit();
			if ( bit == 0 ) { // literal
				int bits = bs.readBits(8);
				result[ctr++] = (byte)bits;
				window[wptr++] = (byte)bits;
				if ( wptr == windowLen ) {
					wptr = 0;
				}
			} else {
				int offset = bs.readNumber(true) - 1;
				if ( offset < 0 ) {
					break;
				}
				int matchCount = bs.readNumber(true) - 1;
				if ( matchCount < 0 ) {
					break;
				}

				try {
					for( int i = 0; i < matchCount; i++ ) {
						int p1 = wptr - offset + i;
						while ( p1 < 0 ) {
							p1 += windowLen;
						}
						while ( p1 >= windowLen ) {
							p1 -= windowLen;
						}
						int p2 = wptr + i;
						while ( p2 >= windowLen ) {
							p2 -= windowLen;
						}
						result[ctr+i] = window[p1];
						window[p2] = window[p1];
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println(new String(result));
				}
				ctr += matchCount;
				wptr += matchCount;
				while ( wptr >= windowLen ) {
					wptr -= windowLen;
				}
			}
			
			if ( ctr >= resultLen ) {
				break;
			}
		}
		return result;
	}

	private static int matchingBytes(byte[] searchWindow, int searchPtr, byte[] data, int ptr, int maxmatch) {
		
		int counter = 0;

		int i;
		do {
			for ( i = 0; counter < maxmatch && i + searchPtr < searchWindow.length && counter + ptr < data.length; i++, counter++ ) {
				if ( searchWindow[i + searchPtr]  != data[counter + ptr] ) {
					return counter;
				}
			}
		} while(i + searchPtr == searchWindow.length);
		
		return counter;
	}

	private static int[] indicesOf(byte[] data, byte c) {
		
		ArrayList l = new ArrayList();
		
		for ( int i = 0; i < data.length; i++ ) {
			if ( data[i] == c ) {
				l.add(new Integer(i));
			}
		}
		
		if ( l.size() == 0 ) {
			return null;
		}
		
		int[] ii = new int[l.size()];
		for ( int i = 0; i < ii.length; i++ ) {
			ii[i] = ((Integer)l.get(i)).intValue();
		}
		
		return ii;
	}

	private static void debug(String string) {
		if ( debugOn ) {
			System.out.println( string);
		}
	}
	
	private static void display120(byte[] data) {
		if ( !debugOn ) {
			return;
		}
		for (int cnt = 0; cnt < data.length; cnt += 120) {
			if ((cnt + 120) < data.length) {
				System.out.println(new String(data, cnt, 120) + '*');
			} else {
				System.out.println(new String(data, cnt, data.length - cnt) + '*');
			}
		}
	}
}
