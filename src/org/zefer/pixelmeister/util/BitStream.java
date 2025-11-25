package org.zefer.pixelmeister.util;

public class BitStream {
			
	byte[] data;
	private int ptr = 0;
	private StringBuffer bits;

	public BitStream() {
		data = new byte[0];
	}

	public BitStream( byte[] data ) {
		this.data = data;
	}

	public BitStream( byte[] data, int offset ) {
		this.data = data;
		if ( offset > 0 && offset <= data.length * 8 ) {
			ptr = offset;
		}
	}

	public void write( int b, int ctr ) {
		if ( bits == null ) {
			bits = new StringBuffer();
		}

		String s = asBits( b, ctr );
		bits.append(s);
	}

	public void write( String s ) {
		bits.append(s);
	}

	public void writePrefix( int originalSize, int maxOffset, int width, int height ) {
		if ( bits == null ) {
			return;
		}

		int len = bits.length();
		int size = len / 8 + ((len % 8) == 0 ? 0 : 1);

		String prefix = asBits('Z', 8) +
				asBits(size, 24) +
				asBits(originalSize, 24) +
				asBits(maxOffset, 8) +
				asBits(width, 16) +
				asBits(height, 16);

		bits.insert(0, prefix);
	}

	public void writeDone() {
		if ( bits == null ) {
			return;
		}

		int len = bits.length();
		int size = len / 8 + ((len % 8) == 0 ? 0 : 1);
		data = new byte[size];
		int cnt = 0;

		int i = 0;
		for ( ; i + 8 < len; i += 8 ) {
			String chunk = bits.substring(i, i+8);
			int b = Integer.parseInt(chunk, 2);
			data[cnt++] = (byte)(0xff & b);
		}

		if ( (len % 8) != 0 ) {
			String chunk = bits.substring(i);
			chunk += "11111111".substring(0, 8 - chunk.length());
			int b = Integer.parseInt(chunk, 2);
			data[cnt++] = (byte)(0xff & b);
		} else {
			String chunk = bits.substring(i, i+8);
			int b = Integer.parseInt(chunk, 2);
			data[cnt++] = (byte)(0xff & b);
		}

		bits = null;
	}

	int testBit() {
		int byte_offset = ptr >> 3;
		int i = (int)(0xff & data[byte_offset]);
		i = (i >> (8 - ((ptr & 7) + 1))) & 1;
		return i;
	}

	public int readBit() {
		int byte_offset = ptr >> 3;
		if ( byte_offset >= data.length ) {
			return -1;
		}
		int i = (int)(0xff & data[byte_offset]);
		i = (i >> (8 - ((ptr & 7) + 1))) & 1;
		ptr += 1;
		return i;
	}

	int testBits( int len ) {
		if ( len == 0 ) {
			return 0;
		}
		if ( len > 16 ) {
			return 0; 
		}
		int byte_offset = ptr >> 3;
		int i = ((int)(0xff & data[byte_offset]) << 16) + 
				(byte_offset + 1 < data.length ? ((int)(0xff & data[byte_offset+1]) << 8) : 0)  + 
				(byte_offset + 2 < data.length ? (int)(0xff & data[byte_offset+2]) : 0);
		i = (i >> (24 - ((ptr & 7) + len))) & ((1 << len) - 1);
		return i;
	}

//	int readBits( int len ) {
//		if ( len == 0 ) {
//			return 0;
//		}
//		if ( len > 16 ) {
//			return 0; 
//		}
//		int byte_offset = ptr >> 3;
//		int i = ((int)(0xff & data[byte_offset]) << 16) + 
//				(byte_offset + 1 < data.length ? ((int)(0xff & data[byte_offset+1]) << 8) : 0)  + 
//				(byte_offset + 2 < data.length ? (int)(0xff & data[byte_offset+2]) : 0);
//		i = (i >> (24 - ((ptr & 7) + len))) & ((1 << len) - 1);
//		
//		ptr += len;
//		return i;
//	}

	
	public int readBits( int len ) {
	    int end_offset = (ptr + len - 1)>>3;
	    if ( end_offset >= data.length ) {
	        return 0;
	    }

	    int i;
	    int byte_offset = ptr >> 3;
	    if (byte_offset == end_offset) {
	    	int x = (int)(0xff & data[byte_offset]);
	        i = (int)(x >> (8 - ((ptr & 7) + len))) & ((1 << len) - 1);
	    } else {
	    	int x = ((int)(0xff & data[byte_offset]) << 8) + (int)(0xff & data[end_offset]);
	        i = (int)((x >> (16 - ((ptr & 7) + len))) & ((1 << len) - 1));
	    }
	    ptr += len;
	    return i;
	}
	
	public int readNumber( boolean limited ) {
		int count;

		try {

			int ctr = 1;
			int base = 2;
			do {
				if ( readBit() == 0 ) {
					int bits = readBits(ctr);
					count = base + bits;
					break;
				} else {
					ctr++;
					base *= 2;
					if ( ctr == 7 ) {
						int bits = readBits(ctr);
						count = base + bits;
						break;
					}
				}
			} while ( true );
			
		} catch (Exception e) {
			return -1;
		}
		return count;
	}

	public static String encodeNumber( int i ) {
		
		if ( i < 2 ) { // unlikely
			return "";
		}
		
		if ( i < 4 ) {
			return "0" + BitStream.asBits(i - 2, 1);
		} else if ( i < 8 ) {
			return "10" + BitStream.asBits(i - 4, 2);
		} else if ( i < 16 ) {
			return "110" + BitStream.asBits(i - 8, 3);
		} else if ( i < 32 ) {
			return "1110" + BitStream.asBits(i - 16, 4);
		} else if ( i < 64 ) {
			return "11110" + BitStream.asBits(i - 32, 5);
		} else if ( i < 128 ) {
			return "111110" + BitStream.asBits(i - 64, 6);
		} else {
			return "111111" + BitStream.asBits(i - 128, 7);
		}
	}

	public static int decodeNumber( String s ) {
		int count;
		
		int ptr = 0;
		char bit = s.charAt(ptr++);
		if ( bit == '0' ) {
			bit = s.charAt(ptr++);
			count = 2 + Integer.parseInt("" + bit, 2);
		} else {
			bit = s.charAt(ptr++);
			if ( bit == '0' ) {
				String bits = s.substring(ptr, ptr+2);
				ptr+= 2;
				count = 4 + Integer.parseInt("" + bits, 2);
			} else {
				bit = s.charAt(ptr++);
				if ( bit == '0' ) {
					String bits = s.substring(ptr, ptr+3);
					ptr += 3;
					count = 8 + Integer.parseInt("" + bits, 2);
				} else {
					bit = s.charAt(ptr++);
					if ( bit == '0' ) {
						String bits = s.substring(ptr, ptr+4);
						ptr += 4;
						count = 16 + Integer.parseInt("" + bits, 2);
					} else {
						bit = s.charAt(ptr++);
						if ( bit == '0' ) {
							String bits = s.substring(ptr, ptr+5);
							ptr += 5;
							count = 32 + Integer.parseInt("" + bits, 2);
						} else {
							bit = s.charAt(ptr++);
							if ( bit == '0' ) {
								String bits = s.substring(ptr, ptr+6);
								ptr += 6;
								count = 64 + Integer.parseInt("" + bits, 2);
							} else {
								String bits = s.substring(ptr, ptr+7);
								ptr += 7;
								count = 128 + Integer.parseInt("" + bits, 2);
							}
						}
					}
				}
			}
		}
		return count;
	}
	
	static String asBits(int i, int size) {
		String s = Integer.toBinaryString(i);
		if ( s.length() > size ) {
			s = s.substring( s.length() - size );
		} else if ( s.length() < size ) {
			s = "00000000000000000000000000000000".substring(0, size - s.length()) + s;
		}
		return s;
	}
}