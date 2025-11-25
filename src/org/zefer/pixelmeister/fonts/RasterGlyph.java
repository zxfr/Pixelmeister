package org.zefer.pixelmeister.fonts;


public class RasterGlyph {
	private RasterFont font;
	public byte[] glyph;
	public int[] original;

	int ctr = 0;

	public RasterGlyph(RasterFont rasterFont, byte[] data, int ptr) {
//		char c = (char)(((int)data[ptr + 0] << 8) + data[ptr + 1]);
		int length = (((int)(data[ptr + 2] & 0xff) << 8) + (int)(data[ptr + 3] & 0xff));
		glyph = new byte[length];
		System.arraycopy(data, ptr, glyph, 0, length);
	}

	public int getByteLength() {
		if ( glyph == null ) {
			return 0;
		}
		return glyph.length;
	}
	
	
	public RasterGlyph( RasterFont font, char c, int[] bytes, int width ) {
		
		this.font = font;

		original = bytes;
		
		if (width == 0) {
			glyph = new byte[0];
			return;
		}
		
		byte[] data = new byte[bytes.length*2+8];
		int ptr = 0;

		int marginLeft = -1;
		int marginRight = -1;
		int marginTop = 0;
		int marginBottom = 0;
		
		int height = bytes.length / width;
		
		for ( int i = 0; i < bytes.length; i++ ) {
			if ( bytes[i] != 0xffffff ) {
				marginTop = Math.min(255, i / width);
				break;
			}
		}
		for ( int i = bytes.length - 1; i >= 0; i-- ) {
			if ( bytes[i] != 0xffffff ) {
				marginBottom = Math.min(255, height - (i / width) - 1);
				break;
			}
		}
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				if ( bytes[j*width+i] != 0xffffff ) {
					marginLeft = Math.min(127, i);
					break;
				}
			}
			if ( marginLeft >= 0 ) {
				break;
			}
		}
		if ( marginLeft < 0 ) {
			marginLeft = Math.min(127, width);
		}
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				if ( bytes[j*width+(width-i-1)] != 0xffffff ) {
					marginRight = Math.min(127, i);
					break;
				}
			}
			if ( marginRight >= 0 ) {
				break;
			}
		}
		if ( marginRight < 0 ) {
			marginRight = Math.min(127, width);
		}
		
		if ( marginLeft + marginRight > width ) {
			marginLeft = 0;
			marginRight = 0;
		}
		
		if ( marginTop + marginBottom > height ) {
			marginTop = 0;
			marginBottom = 0;
		}
		
//		marginLeft = 0;
//		marginRight = 0;
//		marginTop = 0;
		
		if ( font.type == RasterFont.ANTIALIASED_FONT ) {

			data[0] = (byte)(((int)c & 0xff00)>>8);
			data[1] = (byte)((int)c & 0xff);
			data[4] = (byte)width; //it cannot be wider 255px
			data[5] = (byte)(marginLeft & 0x7f);
			data[6] = (byte)(marginTop & 0xff);
			data[7] = (byte)(marginRight & 0x7f);

			ptr = 8;
			int prev = 0;
			for (int y = 0; y < height - marginBottom - marginTop; y++) {
				for (int x = 0; x < width - marginLeft - marginRight; x++) {
					int i = y * (width - marginLeft - marginRight) + x;
					
					if ( i == 0 ) {
						prev = bytes[marginLeft + marginTop * width] & 0xff;
						if ( prev == 0 ) {
							data[ptr++] = (byte)(0x81);
						} else if ( prev == 255 ) {
							data[ptr++] = (byte)(0x41);
						} else {
							data[ptr++] = (byte)(prev/4);
						}
						continue;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; 
					if ( pixel == 0 || pixel == 255 ) {
						if ( prev == pixel && (0x3f & data[ptr-1]) < 63 ) {
							data[ptr-1]++;
						} else {
							if ( pixel == 0 ) {
								data[ptr++] = (byte)(0x81);
							} else {
								data[ptr++] = (byte)(0x41);
							}
						}
					} else {
						data[ptr++] = (byte)(pixel/4);
					}
					prev = pixel; 
				}
			}
			
			byte[] data2 = data;
			int ptr2 = ptr;

			data = new byte[bytes.length*2+8];
			
			data[0] = (byte)(((int)c & 0xff00)>>8);
			data[1] = (byte)((int)c & 0xff);
			data[4] = (byte)width; //it cannot be wider 255px
			data[5] = (byte)((marginLeft+128) & 0xff); // high bit is v-polarization flag 
			data[6] = (byte)(marginTop & 0xff);
			data[7] = (byte)(marginBottom & 0xff);

			ptr = 8;
			prev = 0;
			for (int x = 0; x < width - marginLeft - marginRight; x++) {
				for (int y = 0; y < height - marginBottom - marginTop; y++) {
					int i = y * (width - marginLeft - marginRight) + x;
					
					if ( i == 0 ) {
						prev = bytes[marginLeft + marginTop * width] & 0xff;
						if ( prev == 0 ) {
							data[ptr++] = (byte)(0x81);
						} else if ( prev == 255 ) {
							data[ptr++] = (byte)(0x41);
						} else {
							data[ptr++] = (byte)(prev/4);
						}
						continue;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; 
					if ( pixel == 0 || pixel == 255 ) {
						if ( prev == pixel && (0x3f & data[ptr-1]) < 63 ) {
							data[ptr-1]++;
						} else {
							if ( pixel == 0 ) {
								data[ptr++] = (byte)(0x81);
							} else {
								data[ptr++] = (byte)(0x41);
							}
						}
					} else {
						data[ptr++] = (byte)(pixel/4);
					}
					prev = pixel; 
				}
			}
			
			if ( ptr2 <= ptr ) {
				ptr = ptr2;
				data = data2;
//				System.out.println( c + " |");
			} else {
//				System.out.println( c + " -");
			}
			
			data[2] = (byte)((ptr & 0xff00)>>8);
			data[3] = (byte)(ptr & 0xff);
			
		} else if ( font.type == RasterFont.BITMASK_FONT ) {

			byte[] horRaw; // horizontal scan raw
			int hrPtr;
			byte[] horPack; // horizontal packed
			int hpPtr;
			byte[] verRaw; // vertical scan raw
			int vrPtr;
			byte[] verPack; // vertical packed
			int vpPtr;
			
			data[0] = (byte)(((int)c & 0xff00)>>8);
			data[1] = (byte)((int)c & 0xff);
			data[4] = (byte)width; //it cannot be wider 255px
			data[5] = (byte)(marginLeft & 0x7f);
			data[6] = (byte)(marginTop & 0xff);
			data[7] = (byte)(marginRight & 0x7f);
			
			ptr = 8;

			ptr--;
			for (int y = 0; y < height - marginBottom - marginTop; y++) {
				for (int x = 0; x < width - marginLeft - marginRight; x++) {
					int i = y * (width - marginLeft - marginRight) + x; 
					if ( i % 8 == 0 ) {
						ptr++;
						data[ptr] = (byte)0xff;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; // XXX
					if ( pixel == 0 ) {
						int mask = 1 << (7 - (i % 8));
						data[ptr] ^= mask;
					}
				}
			}
			ptr++;
			
			data[2] = (byte)((ptr & 0xff00)>>8);
			data[3] = (byte)(ptr & 0xff);
			
			horRaw = data;
			hrPtr = ptr;
			
			// ------------------------------------------------------------------------
			
			data = new byte[bytes.length*2+8];

			data[0] = (byte)(((int)c & 0xff00)>>8);
			data[1] = (byte)((int)c & 0xff);
			data[4] = (byte)width; //it cannot be wider 255px
			data[5] = (byte)(marginLeft & 0x7f);
			data[6] = (byte)(marginTop & 0xff);
			data[7] = (byte)(marginRight & 0x7f);
			
			ptr = 8;

			ptr--;
			for (int x = 0; x < width - marginLeft - marginRight; x++) {
				for (int y = 0; y < height - marginBottom - marginTop; y++) {
					int i = x * (height - marginTop - marginBottom) + y; 
					if ( i % 8 == 0 ) {
						ptr++;
						data[ptr] = (byte)0xff;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; // XXX
					if ( pixel == 0 ) {
						int mask = 1 << (7 - (i % 8));
						data[ptr] ^= mask;
					}
				}
			}
			ptr++;

			data[2] = (byte)((ptr & 0xff00)>>8);
			data[3] = (byte)(ptr & 0xff);

			verRaw = data;
			vrPtr = ptr;

			// ------------------------------------------------------------------------

			data = new byte[bytes.length*2+8];

			data[0] = (byte)(((int)c & 0xff00)>>8);
			data[1] = (byte)((int)c & 0xff);
			data[4] = (byte)width; //it cannot be wider 255px
			data[5] = (byte)(marginLeft & 0x7f);
			data[6] = (byte)(marginTop & 0xff);
			data[7] = (byte)((marginRight+128) & 0xff); // high bit is compression flag
			
			ptr = 8;

//			if ( c == 'B' ) {
//				for (int i = 8; i < hrPtr; i++) {
//					byte b = horRaw[i];
//					for (int j = 0; j < 8; j++) {
//						int mask = 1 << (7 - j);
//						boolean bitColor = (b & mask) == 0;
//						System.out.print(bitColor ? "*" : " " );
//						if ( ((i - 8) * 8 + j) % (width - marginLeft - marginRight) == (width - marginLeft - marginRight-1)) {
//							System.out.println();
//						}
//					}
//				}
//			}
			
			boolean prevColor = (horRaw[8] & 0x80) == 0;
			int ctr = 0;
			for (int i = 8; i < hrPtr; i++) {
				byte b = horRaw[i];
				for (int j = 0; j < 8; j++) {
					int mask = 1 << (7 - j);
					boolean bitColor = (b & mask) == 0;
					if ( prevColor == bitColor ) {
						ctr++;
						if ( ctr == 127 ) {
							data[ptr++] = (byte)(prevColor ? 0xFF : 0x7F);
							ctr = 0;
						}
					}
					if ( prevColor != bitColor ) {
						if ( ctr > 0 ) {
							data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
						}
						prevColor = bitColor;
						ctr = 1;
					}
				}
			}
			if ( ctr > 0 ) {
				data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
			}
			
			data[2] = (byte)((ptr & 0xff00)>>8);
			data[3] = (byte)(ptr & 0xff);

			horPack = data;
			hpPtr = ptr;
			
			// ------------------------------------------------------------------------

			data = new byte[bytes.length*2+8];

			data[0] = (byte)(((int)c & 0xff00)>>8);
			data[1] = (byte)((int)c & 0xff);
			data[4] = (byte)width; //it cannot be wider 255px
			data[5] = (byte)((marginLeft+128) & 0xff); // high bit is v-polarization flag
			data[6] = (byte)(marginTop & 0xff);
			data[7] = (byte)((marginBottom+128) & 0xff); // high bit is compression flag
			
			ptr = 8;
			
			prevColor = (verRaw[8] & 0x80) == 0;
			ctr = 0;
			for (int i = 8; i < vrPtr; i++) {
				byte b = verRaw[i];
				for (int j = 0; j < 8; j++) {
					int mask = 1 << (7 - j);
					boolean bitColor = (b & mask) == 0;
					if ( prevColor == bitColor ) {
						ctr++;
						if ( ctr == 127 ) {
							data[ptr++] = (byte)(prevColor ? 0xFF : 0x7F); 
							ctr = 0;
						}
					}
					if ( prevColor != bitColor ) {
						if ( ctr > 0 ) {
							data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
						}
						prevColor = bitColor;
						ctr = 1;
					}
				}
			}
			if ( ctr > 0 ) {
				data[ptr++] = (byte)(((prevColor ? 0x80 : 0) + ctr) & 0xff);
			}
			
			data[2] = (byte)((ptr & 0xff00)>>8);
			data[3] = (byte)(ptr & 0xff);

			verPack = data;
			vpPtr = ptr;

			data = horRaw;
			ptr = hrPtr;

			if ( hpPtr < ptr ) {
				data = horPack;
				ptr = hpPtr;
//				System.out.println( c + " -");
			}

			if ( vpPtr < ptr ) {
				data = verPack;
				ptr = vpPtr;
//				System.out.println( c + " |");
			}
		} 
		
		glyph = new byte[ptr];
		System.arraycopy(data, 0, glyph, 0, ptr);
	}

	public int write( byte[] data, int ptr, int type ) {
		return ptr;
	}

	public char getChar() {
		if ( glyph != null && glyph.length > 2 ) {
			return (char)(((int)(0xff & glyph[0]) << 8) + (0xff & glyph[1]));
		}
		return 0;
	}
	
	public static int getWidth( byte[] glyph ) {
		if ( glyph != null && glyph.length > 4 ) {
			return 0xff & glyph[4];
		}
		return 0;
	}

	public byte[] getGlyph() {
		return glyph;
	}

	public int[] getOriginal() {
		return original;
	}

	public static byte[] restoreImageData( int height, byte[] glyph, boolean antialiased ) {
//		int height = font.height;
		int width = getWidth( glyph );
		byte[] data = new byte[height * width];

		if ( antialiased ) {

			boolean polarisationVertical = (0x80 & glyph[5]) > 0;
			int marginLeft = 0x7f & glyph[5];
			int marginTop = 0xff & glyph[6];
			int marginRight = 0x7f & glyph[7];
			int marginBottom = 0;

//			int chunkLength = (0xff00 & ((int)glyph[2] << 8)) + (0xff & glyph[3]) - 8;
//			int effWidth = width - marginLeft - marginRight;
//			int effHeight = chunkLength / effWidth; 
					
			int ptr = 0;
			for ( ; ptr < width * marginTop; ptr++ ) {
				data[ptr] = (byte)0xff;
			}

			if ( polarisationVertical ) {
				
				marginBottom = marginRight;
				marginRight = 0;
				
				int i = 8;
				for (int x = 0; x < width; x++) {
					for (int y = marginTop; y < height - marginBottom; y++) {
						ptr = y * width + x;
						if ( x < marginLeft ) {
							data[ptr] = (byte)0xff;
							continue;
						}
						if ( i + 1 <= glyph.length ) {
							int b = 0xff & glyph[i++];
							if ( (0xc0 & b) > 0 ) {
								byte color = (0x80 & b) > 0 ? 0 : (byte)0xff;
								data[ptr] = color;
								for ( int j = 1; j < (0x3f & b); j++ ) {
									y++;
									if ( y == height - marginBottom ) {
										x++;
										y = marginTop; 
									}
									ptr = y * width + x;
									if ( ptr == data.length ) {
										break;
									}
									data[ptr] = color;
								}
							} else {
								data[ptr] = (byte)(0xff & (b * 4));
							}
						} else {
							data[ptr] = (byte)0xff;
						}
					}
				}
			} else {
				for ( int i = 8; i < glyph.length; i++ ) {
					
					if ( marginLeft > 0 && (ptr % width) == 0 ) {
						for ( int j = 0; j < marginLeft; j++ ) {
							data[ptr++] = (byte)0xff;
						}
					}

					int b = 0xff & glyph[i];
					
					if ( (0xc0 & b) > 0 ) {
						byte color = (0x80 & b) > 0 ? 0 : (byte)0xff;
						data[ptr++] = color;
						if ( marginRight > 0 && (ptr % width) == (width - marginRight) ) {
							for ( int z = 0; z < marginRight; z++ ) {
								data[ptr++] = (byte)0xff;
							}
						}
						for ( int j = 1; j < (0x3f & b); j++ ) {
							if ( marginLeft > 0 && (ptr % width) == 0 ) {
								for ( int z = 0; z < marginLeft; z++ ) {
									data[ptr++] = (byte)0xff;
								}
							}
							data[ptr++] = color;
							if ( marginRight > 0 && (ptr % width) == (width - marginRight) ) {
								for ( int z = 0; z < marginRight; z++ ) {
									data[ptr++] = (byte)0xff;
								}
							}
						}
					} else {
						data[ptr++] = (byte)(0xff & (b * 4));
					}

					if ( marginRight > 0 && (ptr % width) == (width - marginRight) ) {
						for ( int j = 0; j < marginRight; j++ ) {
							data[ptr++] = (byte)0xff;
						}
					}
				}
			}
			for ( ; ptr < data.length; ptr++ ) {
				data[ptr] = (byte)0xff;
			}
			
		} else { // if ( font.type == RasterFont.BITMASK_FONT ) {

			int i = 5;
			int marginLeft = 0x7f & glyph[i++];
			int marginTop = 0xff & glyph[i++];
			int marginRight = 0x7f & glyph[i++];
			
			int ptr = 0;
			for ( ; ptr < width * marginTop; ptr++ ) {
				data[ptr] = (byte)0xff;
			}

			boolean compressed = (glyph[7] & 0x80) > 0;
			if ( compressed ) {
				boolean vraster = (glyph[5] & 0x80) > 0;
				if ( vraster ) {
					int marginBottom = marginRight;
					byte[] bx = expandCompressed(glyph);

					int h = height - marginTop - marginBottom;
					
					for ( int j = 0; j < width * h; j++ ) {
						data[ptr + j] = (byte)0xff;
					}
					
					int p = 0;
					int x = 0;
					while ( p < bx.length ) {
						for ( int y = 0; y < h; y++ ) {
							if ( ptr + y * width + x + marginLeft == data.length) {
								break;
							}
							data[ptr + y * width + x + marginLeft] = bx[p++];
							if ( p == bx.length ) {
								break;
							}
						}
						x++;
					}
					
					ptr += width * h;
					
				} else {
					for ( ; i < glyph.length; i++ ) {
						int len = 0x7f & glyph[i];
						boolean color = (0x80 & glyph[i]) > 0;
						for ( int j = 0; j < len; j++ ) {
							if ( ptr >= data.length ) {
								break;
							}
							if ( marginLeft > 0 && (ptr % width) == 0 ) {
								for ( int z = 0; z < marginLeft; z++ ) {
									data[ptr++] = (byte)0xff;
								}
							}
							data[ptr++] = (byte)(color ? 0 : 0xff);
							if ( marginRight > 0 && (ptr % width) == (width - marginRight) ) {
								for ( int z = 0; z < marginRight; z++ ) {
									data[ptr++] = (byte)0xff;
								}
							}
						}
					}
				}
				
			} else {
				for ( ; i < glyph.length; i++ ) {
					int b = 0xff & glyph[i];
					for ( int j = 0; j < 8; j++ ) {
						if ( ptr >= data.length ) {
							break;
						}
						if ( marginLeft > 0 && (ptr % width) == 0 ) {
							for ( int z = 0; z < marginLeft; z++ ) {
								data[ptr++] = (byte)0xff;
							}
						}
						int mask = 1 << (7 - j);
						if ( (b & mask) == 0 ) {
							data[ptr++] = 0;
						} else {
							data[ptr++] = (byte)0xff;
						}
						if ( marginRight > 0 && (ptr % width) == (width - marginRight) ) {
							for ( int z = 0; z < marginRight; z++ ) {
								data[ptr++] = (byte)0xff;
							}
						}
					}
				}
			}
			for ( ; ptr < data.length; ptr++ ) {
				data[ptr] = (byte)0xff;
			}
		} 
		
		return data;
	}

	private static byte[] expandCompressed( byte[] glyph ) {
		int length = 0;
		for ( int i = 8; i < glyph.length; i++ ) {
			length += 0x7f & glyph[i];
		}

		int p = 0;
		byte[] result = new byte[length];
		for ( int i = 8; i < glyph.length; i++ ) {
			int len = 0x7f & glyph[i];
			boolean color = (0x80 & glyph[i]) > 0;
			for ( int j = 0; j < len; j++ ) {
				result[p++] =  (byte)(color ? 0 : 0xff);
			}
		}
		return result;
	}
	
	
	public int getSize() {
		return glyph.length - ctr;
	}
}
