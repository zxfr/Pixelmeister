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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class TrueType {

	public static final String CP1252 = "Cp1252";
	
//	begin_pro_version		
	private static final String PD4ML = "_PDF_Subset";
	private static final String CMAP = "cmap";
	private static final String HMTX = "hmtx";
	private static final String TTCF = "ttcf";
	private static final String NAME = "name";
	private static final String POST = "post";
	private static final String HHEA = "hhea";
	private static final String HEAD = "head";
	private static final String CFF_ = "CFF ";
	private static final String OS_2 = "OS/2";
	
	public static final String PDF = "PDF";
	public static final String UNICODE = "Identity-H";
	
	private HashMap internTables;
	
	private String filePath = null;
	
	private String collectionName;
	private String collectionIndex = "";

	private int directoryOffset = 0;

	private String fontName; // taken from 'name'
	private String fullName[][];
	private String familyName[][];

	private String style = "";
	private boolean isMonospaced = false;
	private double italicAngle;
	private boolean decoFont = true; //  'true' for symbol fonts like Wingdings

	private boolean cff = false;
	protected int cffOffset;
//	private int cffLength;

	private Head head = new Head();
	private Hhea hhea = new Hhea();
	private Os2 os2 = new Os2();
	private int widthsTable[];
	
	private HashMap cmap10;
	private HashMap cmap31;
	
	private byte[] fontData;
	
	protected char unicodeDiffs[] = new char[256];

	/** table of characters widths for this encoding */
	protected int xwidths[] = new int[256];
    
	/** encoding names */
	protected String differences[] = new String[256];

	private BytesReader reader;
	
	public TrueType( String path ) 
			throws Exception {
		this( Util.readFile(path), path );
	}
	public TrueType( byte[] fontData, String path ) 
		throws IOException {

		this.fontData = fontData;
		this.filePath = path;
		
		String name = path;

		String nameBase = getBaseName( name );
		this.collectionName = getCollectionName(nameBase);
		
		if (collectionName.length() < nameBase.length()) {
			this.collectionIndex = nameBase.substring(collectionName.length() + 1);
		}

		if (nameBase.length() < name.length()) {
			style = name.substring(nameBase.length());
		}

		parse();
	}

	public HashMap setUsedChars( String text ) {

		HashMap usedGlyphs = new HashMap();

		int len = text.length();
		int metrics[] = null;


		if ( isDecoFont() ) {
			byte[] b = TrueType.convertToBytes(text, TrueType.CP1252);
			len = b.length;
			for (int i = 0; i < len; i++) {
				metrics = getMetricsTT(b[i] & 0xff);
				if (metrics == null)
					continue;
				usedGlyphs.put( new Integer(metrics[0]), new int[]{metrics[0], metrics[1], 
					unicodeDiffs[b[i] & 0xff]} );
			}
		} else {
			for (int i = 0; i < len; i++) {
				char c = text.charAt(i);
				metrics = getMetricsTT(c);
				if (metrics == null)
					continue;
				int m0 = metrics[0];
				Integer gl = new Integer(m0);
				if (!usedGlyphs.containsKey(gl))
					usedGlyphs.put(gl, new int[]{m0, metrics[1], c});
			}
		}

		Iterator ii = usedGlyphs.keySet().iterator();
		while ( ii.hasNext() ) {
			Object key = ii.next();
			if ( usedGlyphs.get(key) == null )
			{
				usedGlyphs.remove( key );
			}
		}

		return usedGlyphs;
	}

	public String convertToString(int[] glx, boolean arabic) {

		HashMap currentTable = null;
		
		int[] xdata = new int[ glx.length ];
		for (int i = 0; i < xdata.length; i++) {
			xdata[i] = 0;
		}

		if (!decoFont && cmap31 != null) {
			currentTable = cmap31;
		}

		if (decoFont && cmap10 != null) { 
			currentTable = cmap10;
		}

		if ( currentTable == null ) {
			return "";
		}

		Iterator ii = currentTable.keySet().iterator();
		while( ii.hasNext() ) {
			Integer k = (Integer)ii.next();
			int[] x = (int[])currentTable.get(k);
			if (x == null)
				continue;

			for (int i = 0; i < glx.length; i++) {
				if ( x[0] == glx[i] ) {
					xdata[i] = k.intValue();
				}
			}
		}

		StringBuffer result = new StringBuffer();
		for (int i = 0; i < xdata.length; i++) {
			if ( xdata[i] != 0 ) {
				result.append( (char)xdata[i] );
			}
		}

		return result.toString();
	}

	public String convertToHex( String text ) {

		StringBuffer result = new StringBuffer();

		int len = text.length();
		int metrics[] = null;

		if ( isDecoFont() ) {
			byte[] b = TrueType.convertToBytes(text, TrueType.CP1252);
			len = b.length;
			for (int i = 0; i < len; i++) {
				metrics = getMetricsTT(b[i] & 0xff);
				if (metrics == null)
					continue;
				result.append( asHex(metrics[0]) );//.append( asHex(metrics[2]) );
			}
		} else {
			for (int i = 0; i < len; i++) {
				char c = text.charAt(i);
				metrics = getMetricsTT(c);
				if (metrics == null)
					continue;
				result.append( asHex(metrics[0]) );//.append( asHex(c) );
			}
		}

		return result.toString();
	}

	public String getFullName(){
		return this.fullName[0][3];
	}

	public String[] getFullNames(){

		String[] names = new String[this.fullName.length];
		
		for ( int i = 0; i < this.fullName.length; i++ ) {
			names[i] = this.fullName[i][3];
		}
		return names;
	}

	public String getFamilyName(){
		return this.familyName[0][0];
	}

	/**
	 * Gets the width of a <CODE>String</CODE> in normalized 1000 units.
	 * @param text the <CODE>String</CODE> to get the witdth of
	 * @return the width in normalized 1000 units
	 */
	public int getWidth(String text)
	{
		int total = 0;
		if ( decoFont ) {
			byte b[] = convertToBytes(text, CP1252);
			int len = b.length;
			for (int i = 0; i < len; i++)
				total += getRawWidth(b[i] & 0xff, null);
		}
		else {
			int len = text.length();
			char[] ca = text.toCharArray();
			
			if ( len > 5 ) {
				Arrays.sort( ca );

				total += getRawWidth(ca[0], UNICODE);
				char last = ca[0];
				int lastWidth = total;
				for (int i = 1; i < len; i++) {
					if ( ca[i] != last ) {
						lastWidth = getRawWidth(ca[i], UNICODE);
						last = ca[i];
					}
					total += lastWidth;
				}
			} else { 
				for (int i = 0; i < len; i++) {
					total += getRawWidth(ca[i], UNICODE);
				}
			}
		}

		return total;
	}

	private void parse() throws IOException {

		String name = filePath;
		String loName = name.toLowerCase();
		if (loName.endsWith(".ttf") || loName.endsWith(".otf") || 
					loName.indexOf(".ttc_") > 0 ) {

//			if ( fontData == null ) {
//				fontData = dataCache.getObject( this.collectionName, true );
//			}
			
			process( fontData );
		} else {						
			throw new IOException("unsupported file format: " + name );
		}
		
	}


	/** Gets the symbolic flag of the font.
	 * @return <CODE>true</CODE> if the font is symbolic
	 */
	public boolean isDecoFont() {
		return decoFont;
	}

	/** Converts a <CODE>String</CODE> to a </CODE>byte</CODE> array according
	 * to the font's encoding.
	 * @return an array of <CODE>byte</CODE> representing the conversion according to the font's encoding
	 * @param encoding the encoding
	 * @param text the <CODE>String</CODE> to be converted
	 */
	static final byte[] convertToBytes(String text, String encoding) {
		if (text == null)
			return new byte[0];

		if ( encoding == null || encoding.length() == 0) {
			int len = text.length();
			byte b[] = new byte[len];
			for (int k = 0; k < len; ++k)
				b[k] = (byte)text.charAt(k);
			return b;
		}

//		StaticFontData.IntHashtable hash = null;
//		if (encoding.equals(CP1252)) {
//			hash = StaticFontData.winansi;
//		} else {
//			 if (encoding.equals(PDF)) {
//				hash = StaticFontData.pdfEncoding;
//			 }
//		}
//
//		if (hash != null) {
//			int len = text.length();
//			byte b[] = new byte[len];
//			int c = 0;
//			for (int k = 0; k < len; ++k) {
//				char char1 = text.charAt(k);
//				if (char1 < 128 || (char1 >= 160 && char1 <= 255))
//					c = char1;
//				else
//					c = hash.get(char1);
//				b[k] = (byte)c;
//			}
//			return b;
//		}

		if (encoding.equals(UNICODE)) {
			// workaround for jdk 1.2.2 bug
			char cc[] = text.toCharArray();
			int len = cc.length;
			byte b[] = new byte[cc.length * 2 + 2];
			b[0] = -2;
			b[1] = -1;
			int bptr = 2;
			for (int k = 0; k < len; ++k) {
				char c = cc[k];
				b[bptr++] = (byte)(c >> 8);
				b[bptr++] = (byte)(c & 0xff);
			}
			return b;
		}
		try {
			return text.getBytes(encoding);
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** Gets an hex string in the format "&lt;HHHH&gt;".
	 * @param n the number
	 * @return the hex string
	 */    
	private static String toHex(int n) {
		String s = Integer.toHexString(n);
		return "<0000".substring(0, 5 - s.length()) + s + ">";
	}

	/** Gets an hex string in the format "HHHH".
	 * @param n the number
	 * @return the hex string
	 */    
	private static String asHex(int n) {
		String s = Integer.toHexString(n);
		return "0000".substring(0, 4 - s.length()) + s;
	}

	/** Reads the font data.
	 * @param ttfAfm the font as a <CODE>byte</CODE> array, possibly <CODE>null</CODE>
	 * @throws DocumentException the font is invalid
	 * @throws IOException the font file could not be read
	 */
	private void process( byte[] fontData ) throws IOException {

		String name = filePath;

		this.internTables = new HashMap();

		this.reader = new BytesReader( fontData );

		try {

			if (collectionIndex.length() > 0) {
				int dirIdx = Integer.parseInt(collectionIndex);
				if (dirIdx < 0)
					throw new IOException("The font index in the collection " + name + " should be positive.");
				String mainTag = reader.readString(4);
				if (!mainTag.equals(TTCF))
					throw new IOException( name + " is invalid TT collection file." );
				reader.skipBytes(4);
				int dirCount = reader.readInt();
				if (dirIdx >= dirCount)
					throw new IOException( "The font index in the collection " + name + " should be less than " + (dirCount - 1) + ". Actual value is " + dirIdx);
				reader.skipBytes(dirIdx * 4);
				directoryOffset = reader.readInt();
			}
			reader.seek(directoryOffset);
			int ttId = reader.readInt();
			if (ttId != 0x00010000 && ttId != 0x4F54544F)
				throw new IOException(name + " is invalid TTF file.");
			int num_tables = reader.readUnsignedShort();
			reader.skipBytes(6);
			for (int k = 0; k < num_tables; ++k) {
				String tag = reader.readString(4);
				reader.skipBytes(4);
				int table_location[] = new int[2];
				table_location[0] = reader.readInt();
				table_location[1] = reader.readInt();
				internTables.put(tag, table_location);
			}
			checkCff();
			fontName = getBaseFont( reader );
			
			int iz;
			if ( fontName != null && (iz = fontName.indexOf('/')) >= 0 ) {
				fontName = fontName.substring(iz+1);
			}
			
			fullName = getNames(reader, 4); //full name
			familyName = getNames(reader, 1); //family name

			fillTables( reader );
			readGlyphWidths( reader );
			readCMaps( reader );

//			if ( isDecoFont() ) {			
//				decoFont = false;
//				createEncoding();
//				decoFont = true;
//			}
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	/**
	 * Gets the name without the modifiers Bold, Italic or BoldItalic.
	 * @param name the full name of the font
	 * @return the name without the modifiers Bold, Italic or BoldItalic
	 */
	private static String getBaseName(String name) {
		if (name.endsWith(",Bold"))
			return name.substring(0, name.length() - 5);
		else if (name.endsWith(",Italic"))
			return name.substring(0, name.length() - 7);
		else if (name.endsWith(",BoldItalic"))
			return name.substring(0, name.length() - 11);
		else
			return name;
	}

	/** Gets the name from a composed TTC file name.
	 * If I have for input "myfont.ttc_2" the return will
	 * be "myfont.ttc".
	 * @param name the full name
	 * @return the simple file name
	 */    
	private static String getCollectionName(String name) {
		int idx = name.toLowerCase().indexOf(".ttc_");
		if (idx < 0)
			return name;
		else
			return name.substring(0, idx + 4);
	}

	private void checkCff() {
		int table_location[];
		table_location = (int[])internTables.get(CFF_);
		if (table_location != null) {
			cff = true;
			cffOffset = table_location[0];
//			cffLength = table_location[1];
		}
	}

	/**
	 * Gets the Postscript font name.
	 * @throws IOException the font is invalid, the font file could not be read
	 * @return the Postscript font name
	 */
	private String getBaseFont( BytesReader rf ) throws IOException {
		int table_location[];
		table_location = (int[])internTables.get(NAME);

		if (table_location == null) {
			throw new IOException( "Font file format error (name): " + 
				filePath + this.style );
		}

		rf.seek(table_location[0] + 2);

		int numRecords = rf.readUnsignedShort();
		int startOfStorage = rf.readUnsignedShort();

		for (int k = 0; k < numRecords; ++k) {
			int platformID = rf.readUnsignedShort();
			rf.readUnsignedShort();
			rf.readUnsignedShort();
			int nameID = rf.readUnsignedShort();
			int length = rf.readUnsignedShort();
			int offset = rf.readUnsignedShort();
			if (nameID == 6) {
				rf.seek(table_location[0] + startOfStorage + offset);
				if (platformID == 0 || platformID == 3) {
					String res = rf.readUnicodeString(length);
					if ( res != null ) {
						res = res.replace(' ', '-');
					}
					return res;
				} else {
					String res = rf.readString(length);
					if ( res != null ) {
						res = res.replace(' ', '-');
					}
					return res;
				}
			}
		}

		return filePath.replace(' ', '-');
	}

	/** Extracts the names of the font in all the languages available.
	 * @param id the name id to retrieve
	 * @throws DocumentException on error
	 * @throws IOException on error
	 */    
	private String[][] getNames( BytesReader rf, int id ) throws IOException {
		int table_location[];
		table_location = (int[])internTables.get(NAME);
		if (table_location == null) {
			throw new IOException("Font file format error (name,2): " + filePath + style);
		}
		rf.seek(table_location[0] + 2);
		int numRecords = rf.readUnsignedShort();
		int startOfStorage = rf.readUnsignedShort();

		List names = new ArrayList();
		for (int k = 0; k < numRecords; ++k) {
			int platformID = rf.readUnsignedShort();
			int platformEncodingID = rf.readUnsignedShort();
			int languageID = rf.readUnsignedShort();
			int nameID = rf.readUnsignedShort();
			int length = rf.readUnsignedShort();
			int offset = rf.readUnsignedShort();
			if (nameID == id) {
				int pos = rf.getPointer();
				rf.seek(table_location[0] + startOfStorage + offset);
				String name;
				if (platformID == 0 || platformID == 3 || (platformID == 2 && platformEncodingID == 1)){
					name = rf.readUnicodeString(length);
				}
				else {
					name = rf.readString(length);
				}
				names.add(new String[]{String.valueOf(platformID),
					String.valueOf(platformEncodingID), String.valueOf(languageID), name});
				rf.seek(pos);
			}
		}
		String thisName[][] = new String[names.size()][];
		for (int k = 0; k < names.size(); ++k)
			thisName[k] = (String[])names.get(k);
		return thisName;
	}

	/**
	 * Reads the tables 'head', 'hhea', 'OS/2' and 'post' filling several variables.
	 * @throws DocumentException the font is invalid
	 * @throws IOException the font file could not be read
	 */
	private void fillTables( BytesReader breader ) throws IOException {
		int tablePtrs[];
		tablePtrs = (int[])internTables.get(HEAD);
		if (tablePtrs == null) {
			throw new IOException("Font file format error (5): " + filePath + style);
		}
		breader.seek(tablePtrs[0] + 16);
		head.flags = breader.readUnsignedShort();
		head.resolution = breader.readUnsignedShort();
		breader.skipBytes(16);
		head.xMin = breader.readShort();
		head.yMin = breader.readShort();
		head.xMax = breader.readShort();
		head.yMax = breader.readShort();
		head.mac = breader.readUnsignedShort();
        
		tablePtrs = (int[])internTables.get(HHEA);
		if (tablePtrs == null) {
			throw new IOException("Font file format error (6): " + filePath + style);
		}
		breader.seek(tablePtrs[0] + 4);
		breader.skipBytes(14);
		hhea.caretRise = breader.readShort();
		hhea.caretRun = breader.readShort();
		breader.skipBytes(12);
		hhea.metrixNum = breader.readUnsignedShort();
        
		tablePtrs = (int[])internTables.get(OS_2);
		if (tablePtrs == null) {
			throw new IOException("Font file format error (7): " + filePath + style);
		}
		breader.seek(tablePtrs[0]);
		int version = breader.readUnsignedShort();
		breader.skipBytes(51);

		os2.sTypoAscender = breader.readShort();
		os2.sTypoDescender = breader.readShort();
		breader.skipBytes(6);
		if (version > 0) {
			breader.skipBytes(8);
		}
		if (version > 1) {
			breader.skipBytes(2);
			os2.sCapHeight = breader.readShort();
		}
		else
			os2.sCapHeight = (int)(0.7 * head.resolution);
        
		tablePtrs = (int[])internTables.get(POST);
		if (tablePtrs == null) {
			italicAngle = -Math.atan2(hhea.caretRun, hhea.caretRise) * 180 / Math.PI;
			return;
		}
		breader.seek(tablePtrs[0] + 4);
		short mantissa = breader.readShort();
		int fraction = breader.readUnsignedShort();
		italicAngle = (double)mantissa + (double)fraction / 16384.0;
		breader.skipBytes(4);
		isMonospaced = breader.readInt() != 0;
	}

	/** Reads the glyphs widths. The widths are extracted from the table 'hmtx'.
	 *  The glyphs are normalized to 1000 units.
	 * @throws DocumentException the font is invalid
	 * @throws IOException the font file could not be read
	 */
	private void readGlyphWidths( BytesReader breader ) throws IOException {
		int table_location[];
		table_location = (int[])internTables.get(HMTX);
		if (table_location == null)
			throw new IOException("Font file format error (8): " + filePath + style);
		breader.seek(table_location[0]);
		widthsTable = new int[hhea.metrixNum];
		for (int k = 0; k < hhea.metrixNum; ++k) {
			widthsTable[k] = (breader.readUnsignedShort() * 1000) / head.resolution;
			breader.readUnsignedShort();
		}
	}
    
	/** Gets a glyph width.
	 * @param glyph the glyph to get the width of
	 * @return the width of the glyph in normalized 1000 units
	 */
	private int getGlyphWidth(int glyph) {
		if (glyph >= widthsTable.length)
			glyph = widthsTable.length - 1;
		return widthsTable[glyph];
	}
    
	/** Reads the several maps from the table 'cmap'. The maps of interest are 1.0 for symbolic
	 *  fonts and 3.1 for all others. A symbolic font is defined as having the map 3.0.
	 * @throws DocumentException the font is invalid
	 * @throws IOException the font file could not be read
	 */
	private void readCMaps( BytesReader rf ) throws IOException {
		int table_location[];
		table_location = (int[])internTables.get(CMAP);
		if (table_location == null)
			throw new IOException("Font file format error (9): " + filePath + style);
		rf.seek(table_location[0]);
		rf.skipBytes(2);
		int num_tables = rf.readUnsignedShort();
		decoFont = false;
		int map10 = 0;
		int map31 = 0;
		int map30 = 0;
		for (int k = 0; k < num_tables; ++k) {
			int platId = rf.readUnsignedShort();
			int platSpecId = rf.readUnsignedShort();
			int offset = rf.readInt();
			if (platId == 3 && platSpecId == 0) {
				decoFont = true;
				map30 = offset;
			}
			else if (platId == 3 && platSpecId == 1) {
				map31 = offset;
			}
			if (platId == 1 && platSpecId == 0) {
				map10 = offset;
			}
		}
		if (map10 > 0) {
			rf.seek(table_location[0] + map10);
			int format = rf.readUnsignedShort();
			switch (format) {
				case 0:
					cmap10 = readFormat0(rf);
					break;
				case 4:
					cmap10 = readFormat4(rf);
					break;
				case 6:
					cmap10 = readFormat6(rf);
					break;
			}
		}
		if (map31 > 0) {
			rf.seek(table_location[0] + map31);
			int format = rf.readUnsignedShort();
			if (format == 4) {
				cmap31 = readFormat4(rf);
			}
		}
		if (map30 > 0) {
			rf.seek(table_location[0] + map30);
			int format = rf.readUnsignedShort();
			if (format == 4) {
				cmap10 = readFormat4(rf);
			}
		}
	}
    
	/** The information in the maps of the table 'cmap' is coded in several formats.
	 *  Format 0 is the Apple standard character to glyph index mapping table.
	 * @return a <CODE>HashMap</CODE> representing this map
	 * @throws IOException the font file could not be read
	 */
	private HashMap readFormat0( BytesReader rf ) throws IOException {
		HashMap h = new HashMap();
		rf.skipBytes(4);
		for (int k = 0; k < 256; ++k) {
			int r[] = new int[2];
			r[0] = rf.readUnsignedByte();
			r[1] = getGlyphWidth(r[0]);
			h.put(new Integer(k), r);
		}
		return h;
	}
    
	/** The information in the maps of the table 'cmap' is coded in several formats.
	 *  Format 4 is the Microsoft standard character to glyph index mapping table.
	 * @return a <CODE>HashMap</CODE> representing this map
	 * @throws IOException the font file could not be read
	 */
	private HashMap readFormat4( BytesReader rf ) throws IOException {
		int mask = (decoFont ? 0xff : 0xffff);
		HashMap h = new HashMap();
		int table_lenght = rf.readUnsignedShort();
		rf.skipBytes(2);
		int segCount = rf.readUnsignedShort() / 2;
		rf.skipBytes(6);
		int endCount[] = new int[segCount];
		for (int k = 0; k < segCount; ++k) {
			endCount[k] = rf.readUnsignedShort();
		}
		rf.skipBytes(2);
		int startCount[] = new int[segCount];
		for (int k = 0; k < segCount; ++k) {
			startCount[k] = rf.readUnsignedShort();
		}
		int idDelta[] = new int[segCount];
		for (int k = 0; k < segCount; ++k) {
			idDelta[k] = rf.readUnsignedShort();
		}
		int idRO[] = new int[segCount];
		for (int k = 0; k < segCount; ++k) {
			idRO[k] = rf.readUnsignedShort();
		}
		int glyphId[] = new int[table_lenght / 2 - 8 - segCount * 4];
		for (int k = 0; k < glyphId.length; ++k) {
			glyphId[k] = rf.readUnsignedShort();
		}
		
		for (int k = 0; k < segCount; ++k) {
			int glyph;
			for (int j = startCount[k]; j <= endCount[k] && j != 0xFFFF; ++j) {
				if (idRO[k] == 0) {
					glyph = (j + idDelta[k]) & 0xFFFF;
				}
				else {
					int idx = k + idRO[k] / 2 - segCount + j - startCount[k];
					if (idx >= glyphId.length)
						continue;
					glyph = (glyphId[idx] + idDelta[k]) & 0xFFFF;
				}
				int r[] = new int[2];
				r[0] = glyph;
				r[1] = getGlyphWidth(r[0]);
				h.put(new Integer(j & mask), r);
			}
		}

		return h;
	}
    
	/** The information in the maps of the table 'cmap' is coded in several formats.
	 *  Format 6 is a trimmed table mapping. It is similar to format 0 but can have
	 *  less than 256 entries.
	 * @return a <CODE>HashMap</CODE> representing this map
	 * @throws IOException the font file could not be read
	 */
	private HashMap readFormat6( BytesReader rf ) throws IOException {
		HashMap h = new HashMap();
		rf.skipBytes(4);
		int start_code = rf.readUnsignedShort();
		int code_count = rf.readUnsignedShort();
		for (int k = 0; k < code_count; ++k) {
			int r[] = new int[2];
			r[0] = rf.readUnsignedShort();
			r[1] = getGlyphWidth(r[0]);
			h.put(new Integer(k + start_code), r);
		}
		return h;
	}
    
	/** Gets the glyph index and metrics for a character.
	 * @param c the character
	 * @return an <CODE>int</CODE> array with {glyph index, width}
	 */    
	private int[] getMetricsTT(int c) {
		if (!decoFont && cmap31 != null) {
			return (int[])cmap31.get(new Integer(c));
		}
		if (decoFont && cmap10 != null) 
			return (int[])cmap10.get(new Integer(c));
		return null;
	}
   
	/** Gets the width from the font according to the unicode char <CODE>c</CODE>.
	 * If the <CODE>name</CODE> is null it's a symbolic font.
	 * @param c the unicode char
	 * @param name the glyph name
	 * @return the width of the char
	 */
	private int getRawWidth(int c, String name) {
		HashMap map = null;

		if (name == null) {
			map = cmap10;
		} else {
			map = cmap31;
		}

		if (map == null) {
			return 0;
		}

		int metric[] = (int[])map.get(new Integer(c));

		if (metric == null) {
			return 0;
		}

		return metric[1];
	}

	public String getRanges() {
		
		ArrayList l = new ArrayList();
		if ( cmap31 != null ) {
			Set keys = cmap31.keySet();
			l.addAll(keys);
		} else if ( cmap10 != null ) {
			Set keys = cmap10.keySet();
			l.addAll(keys);
		}
		Collections.sort(l, new Comparator<Integer>() {
			@Override
			public int compare(Integer e1, Integer e2) {
				if(e1.intValue() < e2.intValue()){
					return -1;
				} else {
					return 1;
				}
			}
		});
		
		String result = "";

		int counter = 0;
		int last = -1;
		int trailing = -1;
		
		Iterator ii = l.iterator();
		while ( ii.hasNext() ) {
			Integer i = (Integer)ii.next();
			
			if ( counter++ == 0 ) {
				result += "0x" + Integer.toHexString(i).toUpperCase() + "-";
				trailing = i.intValue();
				last = i.intValue();
				continue;
			}
			
			if ( i.intValue() > last + 1 ) {
				if ( trailing == last ) {
					result = result.substring(0, result.length()-1);
					result += ", 0x" + Integer.toHexString(i).toUpperCase() + "-";
				} else {
					result += "0x" + Integer.toHexString(last).toUpperCase() + ", 0x" + Integer.toHexString(i).toUpperCase() + "-";
				}
				trailing = i.intValue();
			}

			last = i.intValue();
		}
		
		if ( trailing == last ) {
			result = result.substring(0, result.length()-1);
		} else {
			result += "0x" + Integer.toHexString(last).toUpperCase();
		}
		
		return result;
	}
	
	public ArrayList getGlyphList() {
		ArrayList l = new ArrayList();
		if ( cmap31 != null ) {
			Set keys = cmap31.keySet();
			l.addAll(keys);
		} else if ( cmap10 != null ) {
			Set keys = cmap10.keySet();
			l.addAll(keys);
		}
		Collections.sort(l);
		return l;
	}
	
	
	/** The components of table 'head'.
	 */
	protected class Head {
		short xMin;
		short yMin;
		short xMax;
		short yMax;
		int resolution;
		int flags;
		int mac;
	}
    
	/** The components of table 'hhea'.
	 */
	protected class Hhea {
		short caretRise; 
		short caretRun;
		int metrixNum;
	}
    
	/** 'OS/2'.
	 */
	protected class Os2 {
		// TODO
		short sTypoAscender;
		short sTypoDescender;
		int sCapHeight;
	}

	public int getAscent() {
		return (int)os2.sTypoAscender / head.resolution;
	}



	public int getDescent() {
		return (int)os2.sTypoDescender / head.resolution;
	}

//	end_pro_version		
}
