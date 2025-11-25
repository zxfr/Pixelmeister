package org.zefer.pixelmeister.fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.zefer.pixelmeister.DataLayerView;

public class RasterFont {
	
	public static class InvalidFontException extends Exception {
		public InvalidFontException( String message ) {
			super(message);
		}
	}
	
	public final static int BITMASK_FONT = 1;
	public final static int ANTIALIASED_FONT = 2;
	private static final int HEADER_LENGTH = 5;
	
	/*
	 Prefix:
	 0-1: ZF
	 2: flags
	 3: height
	 4: baseline
	 
	 */
	
	
	public int type;
	public int height;
	private int baseline;
	private int top;
	private int bottom;
	
	private HashMap glyphs = new HashMap();
	
	public RasterFont( boolean antialiased ) {
		if ( antialiased ) {
			type = ANTIALIASED_FONT;
		} else {
			type = BITMASK_FONT;
		}
	}
	
	public RasterFont( byte[] rasterFont ) throws InvalidFontException {

		byte p1 = rasterFont[0];
		byte p2 = rasterFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			throw new InvalidFontException("Invalid font prefix");
		}
		type = rasterFont[2];
		if ( type != ANTIALIASED_FONT && type != BITMASK_FONT ) {
			throw new InvalidFontException("Unsupported font type");
		}
		height = rasterFont[3];
		baseline = rasterFont[4];

		int ptr = HEADER_LENGTH;

		while ( ptr < rasterFont.length ) {
			if ( rasterFont[ptr+0]== 0 && rasterFont[ptr+1]== 0 ) {
				break;
			}
			RasterGlyph rg = new RasterGlyph(this, rasterFont, ptr);
			char c = rg.getChar();
			ptr += rg.getByteLength();
			glyphs.put(new Character(c), rg);
			
//			System.out.println( c + ": " + rg.getByteLength() + "bytes");
		}
	}

	public byte[] toByteArray( boolean deforce, String fontName ) throws IOException {
		
		ByteArrayOutputStream fontStream = new ByteArrayOutputStream();
		fontStream.write('Z');
		fontStream.write('F');
		fontStream.write(type);
		fontStream.write(height);
		fontStream.write(baseline);

		String skip = "";
		int skipCtr = 0;
		boolean filter = false;
		if ( deforce ) {
			filter = true;
		}
		
		Iterator ii = glyphs.keySet().iterator();
		while ( ii.hasNext() ) {
			Object key = ii.next();
			
			if ( glyphs.size() > 2 && filter ) {
				
				if ( fontName.indexOf(((Character)key).charValue()) >= 0 ) {
					skip += "'" + key + "', ";
					skipCtr++;
					continue;
				}
				
				double d = Math.random();
				if ( d < .08 ) {
					skip += "'" + key + "', ";
					skipCtr++;
					continue;
				}
			}
			
			RasterGlyph glyph = (RasterGlyph)glyphs.get(key);
			byte[] g = glyph.getGlyph();
			fontStream.write(g);
		}
		
		fontStream.write(new byte[] {0, 0});
		
		if ( skip.length() > 0 ) {
			skip = skip.substring(0, skip.length()-2);
			String msg = "WARNING! Evaluation version limitation:\n" + skipCtr + " glyph" + (skipCtr>1?"s":"") + " (" + skip + ") of " + glyphs.size() + " " + (skipCtr>1?"have":"has") + " not been converted";
			DataLayerView.displayError(msg);
			System.out.println(msg);
		}
		
		return fontStream.toByteArray();
	}
	
	public boolean isAntialiased() {
		return type == ANTIALIASED_FONT;
	}
	
	public void setAntialiased(boolean val) {
		type = val ? ANTIALIASED_FONT : BITMASK_FONT;
	}
	
	public void setHeight( int height, int baseline ) {
		this.height = height;
		this.baseline = baseline;
	}
	
	public int getBaseline() {
		return this.baseline;
	}
	
	public void addGlyph( char c, int[] glyph, int width ) {
		
		if ( glyph == null ) {
			return;
		}
		
		glyphs.put(new Character(c), new RasterGlyph(this, c, glyph, width));
		
//		for ( int y = 0; y < glyph.length; y++ ) {
//			for ( int x = 0; x < glyph[y].length; x++ ) {
//				System.out.print( (glyph[y][x] & 0xffffff) == 0 ? " " : "*" );
//			}
//			System.out.println();
//		}
//		System.out.println("-------------------------------");
	}
	
	public int estimateSize() {
		int result = 0;
		Iterator ii = glyphs.keySet().iterator();
		while ( ii.hasNext() ) {
			Object key = ii.next();
			RasterGlyph glyph = (RasterGlyph)glyphs.get(key);
			result += glyph.getSize();
		}
		return result + HEADER_LENGTH + 2; // 2 trailing zeros
	}

	public int[] getOriginal(char c) {
		RasterGlyph glyph = (RasterGlyph)glyphs.get(new Character(c));
		if (glyph == null) {
			return null;
		}
		if ( c != glyph.getChar() ) {
			System.out.println( "got wrong glyph for '" + (int)c + "'" );
		}
		return glyph.getOriginal();
	}

	public byte[] getGlyph(char c) {
		RasterGlyph glyph = (RasterGlyph)glyphs.get(new Character(c));
		if ( c != glyph.getChar() ) {
			System.out.println( "got wrong glyph for '" + (int)c + "'" );
		}
		return RasterGlyph.restoreImageData(height, glyph.glyph, type == RasterFont.ANTIALIASED_FONT );
	}

	public int getGlyphWidth(char c) {
		RasterGlyph glyph = (RasterGlyph)glyphs.get(new Character(c));
		if ( c != glyph.getChar() ) {
			System.out.println( "got wrong glyph '" + (int)glyph.getChar() + "' for '" + (int)c + "'" );
		}
		return RasterGlyph.getWidth(glyph.glyph);
	}

	public int getHeight() {
		return height;
	}

	public void reset() {
		glyphs.clear();
	}

	public int getTop() {
		return top;
	}

	public void setTop(int top) {
		this.top = top;
	}

	public int getBottom() {
		return bottom;
	}

	public void setBottom(int bottom) {
		this.bottom = bottom;
	}
}
