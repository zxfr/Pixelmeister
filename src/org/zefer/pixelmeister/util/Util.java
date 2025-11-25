package org.zefer.pixelmeister.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.themes.IThemeManager;

public class Util {

	private static ImageRegistry image_registry;

	public static URL newURL(String url_name) {
		try {
			return new URL(url_name);
		} catch (MalformedURLException e) {
			throw new RuntimeException("Malformed URL " + url_name, e);
		}
	}

	public final static ImageRegistry getImageRegistry( Display d ) {
		if (image_registry == null) {
			image_registry = new ImageRegistry(d);
			image_registry.put("xy", ImageDescriptor
					.createFromFile(Util.class, "/resources/xy.png"));
			image_registry.put("wh", ImageDescriptor
					.createFromFile(Util.class, "/resources/wh.png"));
			image_registry.put("error", ImageDescriptor
					.createFromFile(Util.class, "/resources/error.gif"));
			image_registry.put("run", ImageDescriptor
					.createFromFile(Util.class, "/icons/start_task.gif"));
			image_registry.put("clear", ImageDescriptor
					.createFromFile(Util.class, "/icons/clear_co.gif"));
			image_registry.put("importfont", ImageDescriptor
					.createFromFile(Util.class, "/icons/import_font.png"));
			image_registry.put("importimage", ImageDescriptor
					.createFromFile(Util.class, "/icons/import_image.png"));
			image_registry.put("export", ImageDescriptor
					.createFromFile(Util.class, "/icons/export_wiz.gif"));
			image_registry.put("pixelmeister", ImageDescriptor
					.createFromFile(Util.class, "/icons/icon-16px.png"));

			image_registry.put("folder", ImageDescriptor
					.createFromFile(Util.class, "/icons/folder.gif"));
			image_registry.put("file", ImageDescriptor
					.createFromFile(Util.class, "/icons/file.gif"));
			image_registry.put("picture", ImageDescriptor
					.createFromFile(Util.class, "/icons/picture.gif"));
			image_registry.put("pdf", ImageDescriptor
					.createFromFile(Util.class, "/icons/pdf.gif"));
			image_registry.put("prev", ImageDescriptor
					.createFromFile(Util.class, "/icons/backward_nav.gif"));
			image_registry.put("next", ImageDescriptor
					.createFromFile(Util.class, "/icons/forward_nav.gif"));
		}
		
		return image_registry;
	}

	private static final byte[] HEX_CHAR = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };	

	public static String formatFileReference( boolean java, String name, int size, int width, int height ) {
		
		if ( height == 0 ) {
			height = 80;
		}
		
		File f = new File(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("// image file size: ").append(size).append("\n");
		if ( width > 0 ) {
			sb.append("// usage:            utft.loadBitmap(x, y, ").append(width).append(", ").append(height).append(", \"").append(f.getName()).append("\");\n");
		} 
		
		return sb.toString();
	}
	
	public static String formatIconReference( boolean java, String name, int size, String instanceName ) {
		
		File f = new File(name);
		
		StringBuffer sb = new StringBuffer();
		sb.append("/*\n  icon file size: ").append(size).append("\n");
		sb.append("  usage:\n\t").append(instanceName).append(".loadIcon(x, y, ").append("\"").append(f.getName()).append("\");\n*/\n");
		
		return sb.toString();
	}
	
	public static String formatImageBytes( boolean java, String name, byte[] bytes, int width, int height, String instanceName ) {
		
		if ( height == 0 ) {
			height = 80;
		}
		
		name = escapeName(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("int[] ").append(name).append(" = { //:Pxs").append("\n");
		sb.append("// prog_uint16_t ").append(name).append("[").append(bytes.length/2).append("] PROGMEM = { //:Wiring").append("\n");

		for ( int i = 0; i < bytes.length-1; i+=2 ) {
			byte b1 = bytes[i];
			byte b2 = bytes[i+1];
			
			sb.append("0x")
			.append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4])).append((char)(HEX_CHAR[b1 & 0x000F]))
			.append((char)(HEX_CHAR[(b2 & 0x00F0) >> 4])).append((char)(HEX_CHAR[b2 & 0x000F]))
			.append( "," );
			
			if ( ((i+2) % height*2) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n");
		sb.append("// array size: ").append(bytes.length/2).append("\n");
		if ( width > 0 ) {
			sb.append("/* usage:\n\t").append(instanceName).append(".drawBitmap(x, y, ").append(width).append(", ").append(height).append(", ").append(name).append(");\n*/\n");
		} 
		
		return sb.toString();
	}

	public static String formatCompressedImageBytes( boolean java, String name, byte[] bytes, int width, int height, String instanceName ) {
		
		int	wrap = 20;
		
		name = escapeName(name) + "_comp";
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("int[] ").append(name).append(" = { //:Pxs").append("\n");
		sb.append("// prog_uchar ").append(name).append("[").append(bytes.length).append("] PROGMEM = { //:Wiring").append("\n");

		for ( int i = 0; i < bytes.length; i++ ) {
			if ( i < bytes.length ) {
				byte b1 = bytes[i];
				sb.append("0x")
				  .append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4]))
				  .append((char)(HEX_CHAR[b1 & 0x000F]))
				  .append( "," );
			} else {
				sb.append("0x00,");
			}
			
			if ( ((i+1) % wrap) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n");
		
		sb.append("// array size:   ").append(bytes.length).append("\n");
		sb.append("// image width: ").append(width).append("\n");
		sb.append("// image height: ").append(height).append("\n");
		if ( width > 0 ) {
			sb.append("/* usage:\n\t").append(instanceName).append(".drawCompressedBitmap(x, y, ").append(name).append(");\n");
		} 
		sb.append("*/\n");
		
		return sb.toString();
	}

	public static String formatFontBytes( boolean java, String name, String range, byte[] bytes, int glyphHeight, int baseline, boolean antialiased, String instanceName ) {
		
		int	wrap = 20;
		
		name = escapeName(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("int[] ").append(name).append(" = { //:Pxs").append("\n");
		sb.append("// prog_uchar ").append(name).append("[").append(bytes.length+2).append("] PROGMEM = { //:Wiring").append("\n");

		for ( int i = 0; i < bytes.length + 2; i++ ) {
			if ( i < bytes.length ) {
				byte b1 = bytes[i];
				sb.append("0x")
				  .append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4]))
				  .append((char)(HEX_CHAR[b1 & 0x000F]))
				  .append( "," );
			} else {
				sb.append("0x00,");
			}
			
			if ( ((i+1) % wrap) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n");
		if ( antialiased ) {
			sb.append("// antialiased\n");
		}
		sb.append("// array size:   ").append(bytes.length+2).append("\n");
		sb.append("// glyph height: ").append(glyphHeight).append("\n");
		sb.append("// baseline:     ").append(baseline).append("\n");
		sb.append("// range:        ").append(range).append("\n");
		sb.append("/* usage:\n\t").append(instanceName).append(".setFont(").append(name).append(");\n");
		sb.append("\t").append(instanceName).append(".print(x, y, \"...\");\n*/\n");
		
		return sb.toString();
	}

	public static String formatIconBytes( boolean java, String name, byte[] bytes, int width, int height, String instanceName ) {
		
		int	wrap = 20;
		
		name = escapeName(name);
		
		StringBuffer sb = new StringBuffer();
		
		sb.append("int[] ").append(name).append(" = { //:Pxs").append("\n");
		sb.append("// prog_uchar ").append(name).append("[").append(bytes.length).append("] PROGMEM = { //:Wiring").append("\n");

		for ( int i = 0; i < bytes.length; i++ ) {
			int b1 = 0xFF & bytes[i];
			sb.append("0x")
			.append((char)(HEX_CHAR[(b1 & 0x00F0) >> 4]))
			.append((char)(HEX_CHAR[b1 & 0x000F]))
			.append( "," );
			
			if ( ((i+1) % wrap) == 0 ) {
				sb.append('\n');
			}
		}
		
		sb.append("};\n");
		sb.append("/*\narray size:   ").append(bytes.length).append("\n");
		sb.append("image size:   ").append(width).append("x").append(height).append("\n");
		sb.append("usage:\n\t").append(instanceName).append(".drawIcon(x, y, ").append(name).append(");\n");
		sb.append("\t").append(instanceName).append(".cleanIcon(x, y, ").append(name).append(");\n");
			sb.append("*/");
		
		return sb.toString();
	}

	public static String escapeName( String name ) {
		
		StringBuffer result = new StringBuffer();
		int len = name.length();
		for (int i = 0; i < len; i++) {
			char c = name.charAt(i);
			if ( i == 0 && (!Character.isJavaIdentifierStart(c) || !Character.isLetterOrDigit(c)) ) {
				result.append('_');
				continue;
			}
			if ( i != 0 && (!Character.isJavaIdentifierPart(c) || !Character.isLetterOrDigit(c)) ) {
				result.append('_');
				continue;
			}
			result.append(c);
		}
		
		return result.toString();
	}
	
	
	public static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0],	pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage
					.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(),
					bufferedImage.getHeight(), colorModel.getPixelSize(),
					palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

	static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask,
					palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width,
							data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		} else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red,
						green, blue, data.transparentPixel);
			} else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red,
						green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel,
					colorModel.createCompatibleWritableRaster(data.width,
							data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}
	
	public static Range getWord(String s, int pos, boolean semicolonExpected) {

		if ( s == null || s.length() == 0 ) {
			return new Range(0, 0);
		}

		int startPos = 0;
		
		if ( pos < 0 ) {
			pos = 0;
		}

		if ( pos > 0 && !Character.isLetterOrDigit(s.charAt(pos)) && semicolonExpected ) {
			pos--;
		}

		if ( pos < s.length() - 1 && !Character.isLetterOrDigit(s.charAt(pos)) ) {
			pos++;
		}
		
		if ( !Character.isLetterOrDigit(s.charAt(pos)) ) {
    		for ( int i = pos - 1; i >= 0; i-- ) {
    			if ( Character.isLetterOrDigit(s.charAt(i) ) ) {
    				pos = i;
    				break;
    			}
    		}
		}

		boolean found = false;
		int i = pos - 1;
		for ( ; i >= 0; i-- ) {
			char c = s.charAt(i);
			if ( !Character.isLetterOrDigit(c) ) {
				startPos = i + 1;
				found = true;
				break;
			}
		}
		if ( !found ) {
			startPos = 0;
		}

		found = false;
		i = pos + 1;
		for ( ; i < s.length(); i++ ) {
			char c = s.charAt(i);
			if ( !Character.isLetterOrDigit(c) ) {
				pos = i - 1;
				found = true;
				break;
			}
		}
		if ( !found ) {
			pos = i - 1;
		}

		return new Range(startPos, Math.max(1, pos-startPos+1));
	}

	public static class Range {
		public Range( int from, int len) {
			this.from = from;
			this.len = len;
		}
		public int from;
		public int len;
	}

	public static void setBackground( IWorkbenchPartSite site, Control element, String ID ) {
		setBackground(site, element, ID, null);
	}
	
	public static void setBackground( IWorkbenchPartSite site, Control element, String ID, Color def ) {
	    IThemeManager themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
	    Color color = themeManager.getCurrentTheme().getColorRegistry().get(ID);
	    if ( color != null ) {
	    	element.setBackground( color );
	    } else if ( def != null ) {
		    	element.setBackground( def );
	    }
	}

	public static void setForeground( IWorkbenchPartSite site, Control element, String ID ) {
		setForeground(site, element, ID, null);
	}
	
	public static void setForeground( IWorkbenchPartSite site, Control element, String ID, Color def ) {
	    IThemeManager themeManager = site.getWorkbenchWindow().getWorkbench().getThemeManager();
	    Color color = themeManager.getCurrentTheme().getColorRegistry().get(ID);
	    if ( color != null ) {
	    	element.setForeground( color );
	    } else if ( def != null ) {
	    	element.setForeground( def );
	    }
	}

	public static final String replaceString(String src, String find, String replacement) {
		final int len = src.length();
		final int findLen = find.length();

		int idx = src.indexOf(find);
		if (idx < 0) {
			return src;
		}

		StringBuffer buf = new StringBuffer();
		int beginIndex = 0;
		while (idx != -1 && idx < len) {
			buf.append(src.substring(beginIndex, idx));
			buf.append(replacement);
			
			beginIndex = idx + findLen;
			if (beginIndex < len) {
				idx = src.indexOf(find, beginIndex);
			} else {
				idx = -1;
			}
		}
		if (beginIndex<len) {
			buf.append(src.substring(beginIndex, (idx==-1?len:idx)));
		}
		return buf.toString();
	}

	public final static byte[] readFile( String path ) throws Exception {

		File f = new File( path );
		FileInputStream is = new FileInputStream(f);
		BufferedInputStream bis = new BufferedInputStream(is);
		
		ByteArrayOutputStream fos = new ByteArrayOutputStream();
		byte buffer[] = new byte[2048];

		int read;
		do {
			read = is.read(buffer, 0, buffer.length);
			if (read > 0) { // something to put down
				fos.write(buffer, 0, read); 
			}
		} while (read > -1);

		fos.close();
		bis.close();
		is.close();

		return fos.toByteArray();
	}
}
