package org.zefer.pixelmeister.device.itdb02;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.swt.widgets.Display;
import org.zefer.pixelmeister.DataLayerView;

public class uText {
	
	private LCD utft;
	
	public final static int BITMASK_FONT = 1;
	public final static int ANTIALIASED_FONT = 2;
	private static final int HEADER_LENGTH = 5;

	private int matteR = 0xa0;
	private int matteG = 0xa0;
	private int matteB = 0xa0;

	private int colorR = 0xff;
	private int colorG = 0xff;
	private int colorB = 0xff;

	private Display display;
	private int width;
	private int height;
	
	private int[] currentFont;
	
	private boolean autoKerning;
	private int forceKerningGap;
	private int[] buffer;
	private int bufw;		
	
	public uText( LCD utft, final Display display, final int width, final int height ) {
		this.utft = utft;
		this.display = display;
		this.width = width;
		this.height = height;
	}

	public void setFont( int[] font ) {
		
		String param;
		if ( font == null) {
			param = "null";
		} else {
			param = "int[" + font.length + "]";
		}

		if ( font == null || font.length < HEADER_LENGTH + 7 ) {
			System.out.println("utext.setFont(" + param + "): Invalid font");
			return;
		}

		int p1 = font[0];
		int p2 = font[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("utext.setFont(" + param + "): Invalid font prefix");
			return;
		}
		int fontType = font[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("utext.setFont(" + param + "): Unsupported font type");
			return;
		}
		currentFont = font;
	}

	public void setBackground( int r, int g, int b ) {
		matteR = r;
		matteG = g;
		matteB = b;
	}
	
	public void setForeground( int r, int g, int b ) {
		colorR = r;
		colorG = g;
		colorB = b;
	}

	public int getLineHeight() {
		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("utext.getLineHeight(): No font specified");
			return -1;
		}

		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("utext.getLineHeight(): Invalid font prefix");
			return -1;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("utext.getLineHeight(): Unsupported font type");
			return -1;
		}
		return currentFont[3];
	}
	
	public int getBaseline() {
		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("utext.getBaseline(): No font specified");
			return -1;
		}

		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("utext.getBaseline(): Invalid font prefix");
			return -1;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("utext.getBaseline(): Unsupported font type");
			return -2;
		}
		return currentFont[4];
	}

	public int getTextWidth(final String text) {
		return getTextWidth(text, null);
	}

	public int getTextWidth(final String text, final int[] kerning) {

		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("utext.getTextWidth(\"" + text + "\"): No font specified");
			return -1;
		}
		
		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("utext.getTextWidth(\"" + text + "\"): Invalid font prefix");
			return 0;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("utext.getTextWidth(\"" + text + "\"): Unsupported font type");
			return 0;
		}

		int kernPtr = 0;
		int kern = -100; // no kerning

		int x1 = 0;
		
		for (int t = 0; t < text.length(); t++) {
			char c = text.charAt(t);

			int width = 0;
			boolean found = false;
			int ptr = HEADER_LENGTH;
			while ( ptr < currentFont.length ) {
				char cx = (char)(((int)currentFont[ptr + 0] << 8) + currentFont[ptr + 1]);
				if ( cx == 0 ) {
					break;
				}
				int length = (((int)(currentFont[ptr + 2] & 0xff) << 8) + (int)(currentFont[ptr + 3] & 0xff));

				if ( cx == c ) {
					if ( length < 8 ) {
						System.out.println( "utext.getTextWidth(\"" + text + "\"): Invalid " + c + " glyph definition. Font corrupted?" );
						break;
					}
//					System.out.println( "" + c + " found" );
					found = true;

					width = 0xff & currentFont[ptr + 4];

					break;
				}
				
				ptr += length;
			}

			if ( kerning != null && kerning.length > 0 && kerning[kernPtr] > -100 ) {
				kern = kerning[kernPtr];
				if (kerning[kernPtr+1] > -100) {
					kernPtr++;
				}
			}
			
			if ( found ) {
				x1 += width;
				if ( kern > -100 ) {
					x1+= kern;
				}
			}
		}
		
		return x1;
	}
	
	public void print(final int xx, final int yy, final String text) {
		printString( xx, yy, text, null, false );
	}

	public void print(final int xx, final int yy, final String text, final int[] kerning) {
		printString( xx, yy, text, kerning, false );
	}

	public void clean(final int xx, final int yy, final String text) {
		printString( xx, yy, text, null, true );
	}
	
	public void clean(final int xx, final int yy, final String text, final int[] kerning) {
		printString( xx, yy, text, kerning, true );
	}

	public void autoKerning(boolean enable, int forceGap) {
		autoKerning = enable;
		forceKerningGap = forceGap;
	}
	
	private void drawVirtualLine(int x1, int y1, int x2, int y2) {
		if (x1 == x2) {
			for (int y = y1; y <= y2; y++) {
				buffer[y * bufw + x1] = colorR + colorB + colorB;
			}
		} else if (y1 == y2) {
			for (int x = x1; x <= x2; x++) {
				buffer[y1 * bufw + x] = colorR + colorB + colorB;
			}			
		}
	}

	private int[] getGlyphBytes(char c) {
		int ptr = getGlyphPtr(c);
		
		int glyphHeight = currentFont[3];
		int width = 0xff & currentFont[ptr + 4];

		buffer = new int[width * glyphHeight];
		bufw = width;

		int cr = colorR;
		int cg = colorG;
		int cb = colorB;

		int mr = matteR;
		int mg = matteG;
		int mb = matteB;

		renderGlyph(ptr, 0, 0, false, cr, cg, cb, mr, mg, mb, true); 

		/*
		System.out.println("\n" + c);
		for (int y = 0; y < glyphHeight; y++) {
			for (int x = 0; x < width; x++) {
				System.out.print(buffer[y * width + x] == 0 ? "O" : ".");
			}
			System.out.println();
		}
		*/
		
		return buffer;
	}

	private void renderGlyph(int ptr, int x1, final int yy, final boolean clean, 
			int cr, int cg, int cb, int mr, int mg, int mb, boolean virtual) {
		
		int glyphHeight = currentFont[3];
		int length = (((int)(currentFont[ptr + 2] & 0xff) << 8) + (int)(currentFont[ptr + 3] & 0xff));
		int width = 0xff & currentFont[ptr + 4];

		int marginLeft = 0x7f & currentFont[ptr + 5];
		int marginTop = 0xff & currentFont[ptr + 6];
		int marginRight = 0x7f & currentFont[ptr + 7];
		int effWidth = width - marginLeft - marginRight;

		int ctr = 0;

		int fontType = currentFont[2];
		if ( fontType == ANTIALIASED_FONT ) {

			boolean vraster = (0x80 & currentFont[ptr + 5]) > 0;

			if ( vraster ) {
				int marginBottom = marginRight;
				int effHeight = glyphHeight - marginTop - marginBottom;

//							System.out.println( "" + c + " ||" );
				
				
				for ( int i = 0; i < length - 8; i++ ) {
					int b = 0xff & currentFont[ptr + 8 + i];
					int x = ctr / effHeight;
					int y = ctr % effHeight;
					
					if ( (0xc0 & b) > 0 ) {
						int len = 0x3f & b;
						ctr += len;
						if ( (0x80 & b) > 0 ) {
							if ( clean ) {
								utft.setColor(mr, mg, mb);
							} else {
								utft.setColor(cr, cg, cb);
							}
							while ( y + len > effHeight ) {
								utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + effHeight - 1);
								len -= effHeight - y;
								y = 0;
								x++;
							}
							utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y + len - 1);
						}
					} else {
						if ( clean ) {
							utft.setColor(mr, mg, mb);
						} else {
							int opacity = (0xff & (b * 4)); 
							int sr = (cr * (255 - opacity) + mr * (opacity))/255;
							int sg = (cg * (255 - opacity) + mg * (opacity))/255;
							int sb = (cb * (255 - opacity) + mb * (opacity))/255;
							utft.setColor(sr, sg, sb);
						}

						if (virtual) {
							drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y);
						} else {
							utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y);
						}
						ctr++;
					}
				}

			} else {

//							System.out.println( "" + c + " =" );

				for ( int i = 0; i < length - 8; i++ ) {
					int b = 0xff & currentFont[ptr + 8 + i];
					int x = ctr % effWidth;
					int y = ctr / effWidth;
					
					if ( (0xc0 & b) > 0 ) {

						
						int len = 0x3f & b;
						ctr += len;
						if ( (0x80 & b) > 0 ) {
							if ( clean ) {
								utft.setColor(mr, mg, mb);
							} else {
								utft.setColor(cr, cg, cb);
							}
							while ( x + len > effWidth ) {
								if (virtual) {
									drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + effWidth - 1, yy + marginTop + y);
								} else {
									utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + effWidth - 1, yy + marginTop + y);
								}
								len -= effWidth - x;
								x = 0;
								y++;
							}
							if (virtual) {
								drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x + len - 1, yy + marginTop + y);
							} else {
								utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x + len - 1, yy + marginTop + y);
							}
						}
					} else {

//									System.out.println( "" + c + " -" );

						if ( clean ) {
							utft.setColor(mr, mg, mb);
						} else {
							int opacity = (0xff & (b * 4)); 
							int sr = (cr * (255 - opacity) + mr * (opacity))/255;
							int sg = (cg * (255 - opacity) + mg * (opacity))/255;
							int sb = (cb * (255 - opacity) + mb * (opacity))/255;
							utft.setColor(sr, sg, sb);
						}
						if (virtual) {
							drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y);
						} else {
							utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y);
						}
						ctr++;
					}
				}
			}
			
		} else if ( fontType == BITMASK_FONT ) {

			if ( clean ) {
				utft.setColor(mr, mg, mb);
			} else {
				utft.setColor( (int)cr, (int)cg, (int)cb );
			}

			boolean compressed = (currentFont[ptr + 7] & 0x80) > 0;
			if ( compressed ) {
				boolean vraster = (currentFont[ptr + 5] & 0x80) > 0;
				if ( vraster ) {

//								System.out.println( "" + c + " |" );

					int marginBottom = marginRight;
					int effHeight = glyphHeight - marginTop - marginBottom;
					
					for ( int i = 0; i < length - 8; i++ ) {
						int len = 0x7f & currentFont[ptr + 8 + i];
						boolean color = (0x80 & currentFont[ptr + 8 + i]) > 0;
						if ( color ) {
							int x = ctr / effHeight;
							int y = ctr % effHeight;
							while ( y + len > effHeight ) {
								if (virtual) {
									drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + effHeight - 1);
								} else {
									utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + effHeight - 1);
								}
								len -= effHeight - y;
								ctr += effHeight - y;
								y = 0;
								x++;
							}
							if (virtual) {
								drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y + len - 1);
							} else {
								utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x, yy + marginTop + y + len - 1);
							}
						}
						ctr += len;
					}
				} else {

//								System.out.println( "" + c + " -" );
					
					for ( int i = 0; i < length - 8; i++ ) {
						int len = 0x7f & currentFont[ptr + 8 + i];
						boolean color = (0x80 & currentFont[ptr + 8 + i]) > 0;
						if ( color ) {
							int x = ctr % effWidth;
							int y = ctr / effWidth;
							while ( x + len > effWidth ) {
								if (virtual) {
									drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + effWidth - 1, yy + marginTop + y);
								} else {
									utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + effWidth - 1, yy + marginTop + y);
								}
								len -= effWidth - x;
								ctr += effWidth - x;
								x = 0;
								y++;
							}
							if (virtual) {
								drawVirtualLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x + len - 1, yy + marginTop + y);
							} else {
								utft.drawLine(x1 + marginLeft + x, yy + marginTop + y, x1 + marginLeft + x + len - 1, yy + marginTop + y);
							}
						}
						ctr += len;
					}
				}
			} else {
				
//							System.out.println( "" + c + " --" );
				
				for ( int i = 0; i < length - 8; i++ ) {
					int b = 0xff & currentFont[ptr + 8 + i];
					int x = i * 8 % effWidth;
					int y = i * 8 / effWidth;
					for ( int j = 0; j < 8; j++ ) {
						if ( x + j == effWidth ) {
							x = -j;
							y++;
						}
						int mask = 1 << (7 - j);
						if ( (b & mask) == 0 ) {
							if (virtual) {
								drawVirtualLine(x1 + marginLeft + x + j, yy + marginTop + y, x1 + marginLeft + x + j, yy + marginTop + y);
							} else {
								utft.drawLine(x1 + marginLeft + x + j, yy + marginTop + y, x1 + marginLeft + x + j, yy + marginTop + y);
							}
						}
					}
				}
			}
		}
	}

	private int getGlyphPtr(char c) {
		int result = 0;
		int ptr = HEADER_LENGTH;
		while ( ptr < currentFont.length ) {
			char cx = (char)(((int)currentFont[ptr + 0] << 8) + currentFont[ptr + 1]);			
			if ( cx == 0 ) {
				break; // END OF FONT
			}
			
			int length = (((int)(currentFont[ptr + 2] & 0xff) << 8) + (int)(currentFont[ptr + 3] & 0xff));
			if ( length < 8 ) {
				break;
			}
			
			if ( cx == c ) {
				return ptr;
			}
			ptr += length;
		}
		return result;
	}

	private int[] computeKerning(String range) {
		
		int[] result = new int[range.length()+1];
		
		int h = currentFont[3];
		int[] bb1 = null;
		int w1 = 0;
		for ( int i = 0; i < range.length() - 1; i++ ) {
			char c1 = range.charAt(i);
			char c2 = range.charAt(i+1);
			if (c1 == ' ' || c2 == ' ') {
				continue;
			}
			
			int gap = forceKerningGap;
			if (c1 == ',' || c1 == '.' || c1 == '"' || c1 == '\'' || c1 == '_' || c1 == '`' || c1 == '^' ||
					c2 == ',' || c2 == '.' || c2 == '"' || c2 == '\'' || c2 == '_' || c2 == '`' || c2 == '^') {
				
				int ptr = getGlyphPtr(c1);
				int marginRight = 0x7f & currentFont[ptr + 7];				
				if ((0x80 & currentFont[ptr + 5]) > 0) {
					marginRight = 0;
					bb1 = getGlyphBytes(c1);
					int width = 0xff & currentFont[getGlyphPtr(c1) + 4];
					for (int nf = 0; nf < width; nf++ ) {
						for ( int y = 0; y < h; y++ ) {
							if (bb1[y * width + width - nf - 1] == 0) {
								marginRight = nf;
//								System.out.println(c1 + " marginRight: " + nf + "px");
								break;
							}
						}
						if (marginRight > 0) {
							break;
						}
					}
				}
				ptr = getGlyphPtr(c2);
				int marginLeft = 0x7f & currentFont[ptr + 5];
				int n = marginRight + marginLeft;
				
//				System.out.println(c1 + ":" + c2 + " " + marginRight + "+" + marginLeft);
				n -= gap - 1;
				result[i] = -(n-1);
				continue;
			}
						
			bb1 = getGlyphBytes(c1);
			w1 = 0xff & currentFont[getGlyphPtr(c1) + 4];

			int[] bb2 = getGlyphBytes(c2);
			int w2 = 0xff & currentFont[getGlyphPtr(c2) + 4];

			int lcol[] = new int[h]; 
			int rcol[] = new int[h]; 
			
			int n = 1;
			for (; n < w1 && n < w2; n++) {
				boolean overlap = false;
				for (int nf = 1; nf <= n; nf++ ) {
					for ( int y = 0; y < h; y++ ) {
						rcol[y] = bb2[y * w2 + nf - 1];
					}

					for ( int y = 0; y < h; y++ ) {
						lcol[y] = bb1[y * w1 + w1 - nf]; 
					}
					
					for ( int y = 0; y < h; y++ ) {
						if (lcol[y] != 0 && rcol[y] != 0) {
							overlap = true;
							break;
						}
						
						for (int z = 0; z < gap / 3 + 1; z++) {
							if (y > gap && lcol[y-1-z] != 0 && rcol[y] != 0 ||
									y < h-gap-1 && lcol[y+1+z] != 0 && rcol[y] != 0) {										
								overlap = true;
								break;
							}
						}
						if (overlap) {
							break;
						}										
					}
					if (overlap) {
						break;
					}
				}
				
				if (overlap) {
					break;
				}
			}

			n -= gap - 1;

			result[i] = -(n-1);
			
//			if (n > 0) {
//				System.out.println("'" + c1 + "', '" + c2 + "', " + (n - 1) + "," );
//			}
		}
		result[range.length()] = -100;

		return result;
	}

	private void printString(final int xx, final int yy, final String text, final int[] kerning, final boolean clean) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		final String name = clean ? "utext.clean" : "utext.print";
		
		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					utft.debug(name + "("+xx+", "+yy+","+text+", kerning)");
					printString(xx, yy, text, kerning, clean);
				}
			});
			return;
		}

		int cr = colorR;
		int cg = colorG;
		int cb = colorB;

		int mr = matteR;
		int mg = matteG;
		int mb = matteB;

		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println(name + "(\"" + text + "\"): No font specified");
			return;
		}
		
		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println(name + "(\"" + text + "\"): Invalid font prefix");
			return;
		}
		int type = currentFont[2];
		if ( type != ANTIALIASED_FONT && type != BITMASK_FONT ) {
			System.out.println(name + "(\"" + text + "\"): Unsupported font type");
			return;
		}
		
		int[] xkerning = kerning;
		if (autoKerning && kerning == null && type == BITMASK_FONT) {
			xkerning = computeKerning(text);
		}
				
//		int baseline = currentFont[4];

//		System.out.println( "glyph height: " + glyphHeight );
//		System.out.println( "baseline: " + baseline );
		
		utft.atomicAction++;
		
		int kernPtr = 0;
		int kern = -100; // no kerning

		HashSet<Character> missingGlyphs = new HashSet<Character>();
		
		int x1 = xx;
		for (int t = 0; t < text.length(); t++) {
			char c = text.charAt(t);

			boolean found = false;
			int ptr = HEADER_LENGTH;
			while ( ptr < currentFont.length ) {
				char cx = (char)(((int)currentFont[ptr + 0] << 8) + currentFont[ptr + 1]);
				
				if ( cx == 0 ) {
					missingGlyphs.add( new Character(c) );
					break; // END OF FONT
				}
				
				int length = (((int)(currentFont[ptr + 2] & 0xff) << 8) + (int)(currentFont[ptr + 3] & 0xff));

				if ( cx == c ) {
					if ( length < 8 ) {
						System.out.println( name + "(\"" + text + "\"): Invalid " + c + " glyph definition. Font corrupted?" );
						break;
					}
//					System.out.println( "" + c + " found" );
					found = true;
					
					width = 0xff & currentFont[ptr + 4];

					renderGlyph(ptr, x1, yy, clean, cr, cg, cb, mr, mg, mb, false); 
					
					break;
				}
				ptr += length;
			}

			if ( xkerning != null && xkerning.length > 0 && xkerning[kernPtr] > -100 ) {
				kern = xkerning[kernPtr];
				if (xkerning[kernPtr+1] > -100) {
					kernPtr++;
				}
			}
			
			if ( found ) {
				x1 += width;
				if ( kern > -100 ) {
					x1+= kern;
				}
			}
		}

		utft.setColor( (int)cr, (int)cg, (int)cb );
		
		utft.atomicAction--;
		utft.updateDeviceView(0, 0, width, height); // XXX
		
		if ( missingGlyphs.size() > 0 ) {
			String s = (missingGlyphs.size() > 1 ? "s" : "");
			String str = "";
			Iterator<Character> ii = missingGlyphs.iterator();
			while (ii.hasNext()) {
				Character ct = ii.next();
				str += "'" + ct.charValue() + "', ";
			}
			System.out.println( "Warning: missing glyph" + s + " in the current font: " + str.substring(0, str.length()-2) );
		}
	}
}
