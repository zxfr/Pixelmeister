package org.zefer.pixelmeister.device.pixels;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.Application;
import org.zefer.pixelmeister.DataLayerView;
import org.zefer.pixelmeister.DeviceView;
import org.zefer.pixelmeister.util.BitStream;

public class Pixels {
	
	public final static int LEFT = 0;
	public final static int RIGHT = 1;
	public final static int CENTER = 2;

	public final static int BITMASK_FONT = 1;
	public final static int ANTIALIASED_FONT = 2;

	public final static int SCROLL_SMOOTH = 1;
	public final static int SCROLL_CLEAN = 2;

	public final static int FILL_TOPDOWN = 0;
	public final static int FILL_LEFTRIGHT = 0;
	public final static int FILL_DOWNTOP = 1;
	public final static int FILL_RIGHTLEFT = 2;

	private static final int HEADER_LENGTH = 5;

	public final static int PORTRAIT = 0;
	public final static int LANDSCAPE = 1;
	public final static int PORTRAIT_FLIP = 2;
	public final static int LANDSCAPE_FLIP = 3;
	
	public final static int TRANSPARENT_TEXT_BACKGROUND = 0;
	public final static int FILL_TEXT_BACKGROUND = 1;
	
	private RGB background = new RGB(0, 0, 0);
	private RGB foreground = new RGB(255, 255, 255);

	private Color bgColor;
	private Color fgColor;
	
	private double lineWidth = 1;

	private boolean gramReadSupported;
	private boolean gramReadEnabled;
	
	private boolean antialiasingEnabled;

	private int[] currentFont;
	
	private int glyphPrintMode;
	
	private Display display;
//	private int width;
//	private int height;
//	private int deviceWidth;
//	private int deviceHeight;

	private int x1;
	private int y1;
	private int x2;
	private int rasterPtr;
	
//	private int y2;
	
	private Image img;
	private GC gc;
	private GC cgc;
	
	public long lastUpdate;
	private long updatePeriod = 1;
	
	public int atomicAction;
	
	boolean debug = false;
	public void debug( String msg ) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	private int deviceDelayMs = 0;
	private int deviceDelayNs = 1;
	private boolean relativeOrigin;
	private int fillDirection;

	private boolean scrollSupported;
	private boolean scrollEnabled;
    
	private boolean scrollCleanMode;
	
	private int scrollX;
	private int scrollY;
	
	private int extraScrollDelay = 0;

	private int caretY;
	private int caretX;
	private boolean wrapText;
	private boolean textWrapScroll;
	private int textWrapLineGap;
	private int textWrapMarginRight;
	private int textWrapMarginLeft;
	private int textWrapMarginBottom;
	private RGB textWrapScrollFill;
	
	HashMap<String, TreeSet> fontStats = new HashMap<String, TreeSet>();
	
    public void setSpiPins(int scl, int sda, int cs, int rst, int wr) {
    }

    public void setPpiPins(int rs, int wr, int cs, int rst, int rd) {
    }

	public int getWidth() {
		return DataLayerView.landscapeViewer && (DataLayerView.deviceOrientation % 2) == 0 ||
			   !DataLayerView.landscapeViewer && (DataLayerView.deviceOrientation % 2) != 0
				? 
				DataLayerView.height : DataLayerView.width;
	}

	public int getHeight() {
		return DataLayerView.landscapeViewer && (DataLayerView.deviceOrientation % 2) == 0 ||
			   !DataLayerView.landscapeViewer && (DataLayerView.deviceOrientation % 2) != 0
				? 
				DataLayerView.width : DataLayerView.height;
	}
	
//	public static class RGB {
//		public int red;
//		public int green;
//		public int blue;
//		public RGB( int r, int g, int b ) {
//			red = r;
//			green = g;
//			blue = b;
//		}
//	}
	
	public Pixels ( final Display display, final Canvas canvas, final int width, final int height ) {
		
		this.display = display;
//		this.width = width;
//		this.height = height;

		this.scrollSupported = true;
		this.scrollEnabled = true;
		this.relativeOrigin = true;
		
//		deviceHeight = height > width ? height : width;
//		deviceWidth = height < width ? height : width;

//		DataLayerView.landscapeViewer = height < width;
	    
		DataLayerView.deviceOrientation = (DataLayerView.landscapeViewer ? LANDSCAPE : PORTRAIT);
	    
		this.img = new Image( display, DataLayerView.deviceWidth, DataLayerView.deviceHeight );
		display.syncExec(new Runnable() {
			public void run() {
				debug("new LCD()");
				gc = new GC(img);
				
				if (canvas != null && !canvas.isDisposed()) {
					cgc = new GC(canvas);
					if ( DataLayerView.ruler ) {
						Transform tt = new Transform(display);
						tt.translate(DataLayerView.rulerWidth, DataLayerView.rulerHeight);
						cgc.setTransform(tt);
					}
				}

				fgColor = SWTResourceManager.getColor(SWT.COLOR_GRAY);
				
				gc.setBackground(fgColor);
				gc.fillRectangle(0, 0, width, height);
				if ( cgc != null ) {
					cgc.setBackground(fgColor);
					cgc.fillRectangle(0, 0, width, height);
				}

				fgColor = new Color(display, new RGB(0xa0, 0xa0, 0xa0));
				gc.setForeground(fgColor);
				for ( int i = 0; i < width; i+=2 ) {
					gc.drawLine(i+1, 0, i+1, height);
				}
				
				if ( cgc != null ) {
					cgc.setForeground(fgColor);
					for ( int i = 0; i < width; i+=2 ) {
						cgc.drawLine(i+1, 0, i+1, height);
					}
				}
				
				fgColor = SWTResourceManager.getColor(255,255,255);
				bgColor = SWTResourceManager.getColor(0,0,0);
				gc.setForeground(fgColor);
				gc.setBackground(bgColor);

				if ( cgc != null ) {
					cgc.setForeground(fgColor);
					cgc.setBackground(bgColor);
				}
			}
		});
	}

	public void setOrientation( int direction ){
		if ( DataLayerView.deviceOrientation < 2 && direction > 1 || DataLayerView.deviceOrientation > 1 && direction < 2 ) {
			DataLayerView.deviceScroll = 2 * DataLayerView.deviceHeight - DataLayerView.deviceScroll;
	    	DataLayerView.deviceScroll %= DataLayerView.deviceHeight;
		}
		DataLayerView.deviceOrientation = direction;
		DataLayerView.repaintRuler = true;
	}

	public int getOrientation() {
	    return DataLayerView.deviceOrientation;
	}

	public void enableAntialiasing(boolean enable){
	    antialiasingEnabled = enable;
	}

	public boolean isAntialiased(){
	    return antialiasingEnabled;
	}

	public void enableScroll(boolean enable){
	    scrollEnabled = enable;
	}

	public boolean canScroll(){
	    return scrollEnabled & scrollSupported;
	}

	public void setOriginRelative() { // origin relative to a current scroll position
	    relativeOrigin = true;
	}

	public void setOriginAbsolute() { // origin matches physical device pixel coordinates
	    relativeOrigin = false;
	}

	public boolean isOriginRelative(){
	    return relativeOrigin;
	}

	public void setFillDirection(int direction) {
	    fillDirection = direction;
	}

	public void clear() {
		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					clear();
				}
			});
			return;
		}
		
		if (fgColor == null) {
			fgColor = SWTResourceManager.getColor(255,255,255); 
		}
		if (bgColor == null) {
			bgColor = SWTResourceManager.getColor(0,0,0);
		}
    	Color sav = fgColor;
    	gc.setForeground(bgColor);
    	gc.fillRectangle(0, 0, DataLayerView.deviceWidth, DataLayerView.deviceHeight);
    	
    	if ( cgc != null ) {
	    	cgc.setBackground(bgColor);
	    	cgc.setForeground(fgColor);
	    	cgc.fillRectangle(0, 0, DataLayerView.width, DataLayerView.height);
    	}
    	
    	gc.setForeground(sav);
	}
	
	public void drawPixel(final int x, final int y) {
		
		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawPixel("+x+","+y+")" + asRGB(bgColor));
					drawPixel(x, y);
				}

			});
			return;
		}

		if ( x < 0 || y < 0 ) {
			return;
		}
		
		int xx = x;
		int yy = y;

		if( (DataLayerView.deviceOrientation % 2) == 0 ) { // PORTRAIT(_FLIP)
			if ( x >= DataLayerView.deviceWidth || y > DataLayerView.deviceHeight ) {
				return;
			}
		} else {
			if ( y >= DataLayerView.deviceWidth || x > DataLayerView.deviceHeight ) {
				return;
			}
		}

		int s = getScroll();
		
		
		if ( relativeOrigin ) {
			switch( DataLayerView.deviceOrientation ) {
			case PORTRAIT:
				if ( s > 0 && y >= s ) {
					return;
				}
				break;
			case LANDSCAPE:
				if ( s > 0 && x >= s ) {
					return;
				}
		        xx = DataLayerView.deviceWidth - y - 1;
		        yy = x;
				break;
			case PORTRAIT_FLIP:
				if ( s > 0 && y >= s ) {
					return;
				}
		        xx = DataLayerView.deviceWidth - x - 1;
		        yy = DataLayerView.deviceHeight - y - 1;
				break;
			case LANDSCAPE_FLIP:
				if ( s > 0 && x >= s ) {
					return;
				}
		        xx = y;
		        yy = DataLayerView.deviceHeight - x - 1;
				break;
			}
		} else {
			switch( DataLayerView.deviceOrientation ) {
			case PORTRAIT:
				yy += s;
				break;
			case LANDSCAPE:
		        xx = DataLayerView.deviceWidth - y - 1;
		        yy = x + s;
				break;
			case PORTRAIT_FLIP:
		        xx = DataLayerView.deviceWidth - x - 1;
		        yy = 2 * DataLayerView.deviceHeight - y - 1 - s;
				break;
			case LANDSCAPE_FLIP:
		        xx = y;
		        yy = 2 * DataLayerView.deviceHeight - x - 1 - s;
				break;
			}
			yy %= DataLayerView.deviceHeight;
		}
		
		gc.drawPoint(xx, yy);
	}
	
	public RGB getPixel(final int x, final int y) {
		
		if (x < 0 || y < 0 || !gramReadEnabled || !gramReadSupported || DataLayerView.stop || display == null || display.isDisposed()) {
			return getBackground();
		}

		int color = 0;
		
		if ( relativeOrigin ) {
			if ( scrollX != 0 ) {
				int edge = DataLayerView.deviceHeight - scrollX;
				if ( !scrollCleanMode && x == edge || x > edge ) {
					return getBackground();
				}
			} else if ( scrollY != 0 ) {
				int edge = DataLayerView.deviceHeight - scrollY;
				if ( !scrollCleanMode && y == edge || y > edge ) {
					return getBackground();
				}
			}
			color = img.getImageData().getPixel(x, y);
		} else {
			if ( x >= DataLayerView.width || y >= DataLayerView.height ) {
				return getBackground();
			}
    		switch( DataLayerView.deviceOrientation ) { // XXX
    		case PORTRAIT:
    			break;
    		case LANDSCAPE:
    			break;
    		case PORTRAIT_FLIP:
    			break;
    		case LANDSCAPE_FLIP:
    			break;
    		}
//    		if ( landscape ) {
//    			color = img.getImageData().getPixel((x + maxScroll - scrollX) % maxScroll, y);
//    		} else {
//    			color = img.getImageData().getPixel(x, (y + maxScroll - scrollY) % maxScroll);
//    		}
		}
		
		return img.getImageData().palette.getRGB(color);		
	}
	
	public void drawPixel(final double x, final double y) {
		drawPixel((int)x, (int)y);
	}

	public void setColor(final int r, final int g, final int b) {
		setColor(new RGB(r, g, b));
	}
	
	public void setColor(final RGB color) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		foreground = color;

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setForeground("+color.red+","+color.green+","+color.blue+")");
					setColor(color);
				}
			});
			return;
		}

		if ( fgColor != null ) {
			if ( fgColor.getRed() == color.red && fgColor.getGreen() == color.green && fgColor.getBlue() == color.blue ) {
				return;
			}
		}

		fgColor = SWTResourceManager.getColor(color.red, color.green, color.blue);
		if (DataLayerView.stop) {
			return;
		}

//		System.out.println( "color: " + color.red + "," + color.green + "," + color.blue + " vs " + fgColor.getRed() + "," + fgColor.getGreen() + "," + fgColor.getBlue() );
		
		gc.setForeground(fgColor);
		if ( cgc != null ) {
			cgc.setForeground(fgColor);
		}
	}

	public RGB getColor() {
		return foreground;
	}
	
	public void setBackground(final int r, final int g, final int b) {
		setBackground(new RGB(r, g, b));
	}
	
	public void setBackground(final RGB color) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setBackColor("+color.red+","+color.green+","+color.blue+")");
					setBackground(color);
				}
			});
			return;
		}

		if ( bgColor != null ) {
			if ( bgColor.getRed() == color.red && bgColor.getGreen() == color.green && bgColor.getBlue() == color.blue ) {
				return;
			}
		}
		
		bgColor = SWTResourceManager.getColor(color.red, color.green, color.blue);
		if (DataLayerView.stop) {
			return;
		}

		background = color;
		gc.setBackground(bgColor);
		if ( cgc != null ) {
			cgc.setBackground(bgColor);
		}
	}

	public RGB getBackground() {
		return background;
	}
	
	public void setLineWidth(double width) {
		lineWidth = width;
	}
	
	public double getLineWidth() {
		return lineWidth;
	}
	
	public void drawLine(double x1, double y1, double x2, double y2) {
		drawLine((int)x1, (int)y1, (int)x2, (int)y2);
	}
	
	public void drawLine(final int x1, final int y1, final int x2, final int y2) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawLine("+x1+","+y1+","+x2+","+y2+")" + asRGB(bgColor));
					drawLine(x1, y1, x2, y2);
				}
			});
			return;
		}

		atomicAction++;

		if ( x1 == x2 && lineWidth == 1 ) {
			if ( y1 > y2 ) {
				for ( int i = y2; i <= y1; i++ ) {
					drawPixel(x1, i);
				}
			} else {
				for ( int i = y1; i <= y2; i++ ) {
					drawPixel(x1, i);
				}
			}
		} else if ( y1 == y2 && lineWidth == 1 ) {
			if ( x1 > x2 ) {
				for ( int i = x2; i <= x1; i++ ) {
					drawPixel(i, y1);
				}
			} else {
				for ( int i = x1; i <= x2; i++ ) {
					drawPixel(i, y1);
				}
			}
		} else {
			if ( lineWidth == 1 ) {
				if ( antialiasingEnabled ) {
					drawLineAntialiased(x1, y1, x2, y2);
				} else {
					int dx; 
					int dy; 
					int sx; 
					int sy; 

					if ( x2 > x1 ) {
						dx = x2 - x1;
						sx = 1;
					} else {
						dx = x1 - x2;
						sx = -1;
					}

					if ( y2 > y1 ) {
						dy = y2 - y1;
						sy = 1;
					} else {
						dy = y1 - y2;
						sy = -1;
					}
					
					int x = x1;
					int y = y1;
					int err = dx - dy;
					int e2;
					while (true) {
						drawPixel(x, y);
						if (x == x2 && y == y2) {
							break;
						}
						e2 = 2 * err;
						if (e2 > -dy) {
							err = err - dy;
							x = x + sx;
						}
						if (e2 < dx) {
							err = err + dx;
							y = y + sy;
						}
					}			
				}
			} else {
				drawFatLineAntialiased(x1, y1, x2, y2);
			}
		}

		atomicAction--;
	}
	
	public void drawRectangle(final int x, final int y, final int width, final int height) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawRect("+x+","+y+","+width+","+height+")");
					drawRectangle(x, y, width, height);
				}
			});
			return;
		}

		atomicAction++;
		
	    drawLine(x, y, x+width-2, y);
	    drawLine(x+width-1, y, x+width-1, y+height-2);
	    drawLine(x+1, y+height-1, x+width-1, y+height-1);
	    drawLine(x, y+1, x, y+height-1);

		atomicAction--;
	}

	public void fillRectangle(final int x, final int y, final int width, final int height) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillRect("+x+","+y+","+width+","+height+")" + asRGB(bgColor));
					fillRectangle(x, y, width, height);
				}
			});
			return;
		}
		
		atomicAction++;

		int yy;
		for (yy = y; yy < y+height; yy++) {
			drawLine( x, yy, x+width-1, yy );
		}
		
		atomicAction--;
	}
	
	public void drawRoundRectangle(final int x, final int y, final int width, final int height, final int r) {
		
		if ( r < 1 ) {
			drawRectangle(x, y, width, height);
			return;
		}

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawRoundRectangle("+x+","+y+","+width+","+height+")" + asRGB(bgColor));
					drawRoundRectangle(x, y, width, height, r);
				}
			});
			return;
		}

		int radius = r;
		if ( radius > (height-1) / 2 ) {
			radius = (height-1) / 2;
		}
		if ( radius > (width-1) / 2 ) {
			radius = (width-1) / 2;
		}
		
//		RGB sav = getForeground();
//		setForeground(background);
//		gc.drawRoundRectangle(x, y, width, height, radius*2, radius*2);
//		setForeground(sav);
		
		atomicAction++;

		if ( antialiasingEnabled ) {
			drawRoundRectangleAntialiased(x, y, width-1, height-1, radius, radius, 0);
		} else {
			drawLine(x + radius, y + height-1, x + width-1 - radius, y + height-1);
			drawLine(x + radius, y, x + width-1 - radius, y );
			drawLine(x + width-1, y + radius, x + width-1, y + height-1 - radius);
			drawLine(x, y + radius, x, y + height-1 - radius);
			
			int shiftX = width-1 - radius * 2; 
			int shiftY = height-1 - radius * 2; 
			int f = 1 - radius;
			int ddF_x = 1;
			int ddF_y = -radius * 2;
			int x1 = 0;
			int y1 = radius;

			int xx = x + radius;
			int yy = y + radius;
			
			while (x1 < y1) {
				if (f >= 0) {
					y1--;
					ddF_y += 2;
					f += ddF_y;
				}
				x1++;
				ddF_x += 2;
				f += ddF_x;
				
				drawPixel(xx + x1 + shiftX, yy + y1 + shiftY);
				drawPixel(xx - x1, yy + y1 + shiftY);
				drawPixel(xx + x1 + shiftX, yy - y1);
				drawPixel(xx - x1, yy - y1);
				drawPixel(xx + y1 + shiftX, yy + x1 + shiftY);
				drawPixel(xx - y1, yy + x1 + shiftY);
				drawPixel(xx + y1 + shiftX, yy - x1);
				drawPixel(xx - y1, yy - x1);
			}
		}
		
		
		atomicAction--;
	}
	
	public void fillRoundRectangle(final int x, final int y, final int width, final int height, final int r) {

		if ( r < 1 ) {
			fillRectangle(x, y, width, height);
			return;
		}

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillRoundRectangle("+x+","+y+","+width+","+height+")" + asRGB(bgColor));
					fillRoundRectangle(x, y, width, height, r);
				}
			});
			return;
		}

		int radius = r;
		if ( radius > (height-1) / 2 ) {
			radius = (height-1) / 2;
		}
		if ( radius > (width-1) / 2 ) {
			radius = (width-1) / 2;
		}
		
		atomicAction++;

		if ( antialiasingEnabled ) {
			drawRoundRectangleAntialiased(x, y, width-1, height-1, radius, radius, 1);
		}
		
		int corr = 0;
		for (int j = 0; j < height; j++ ) {
			if ( j < radius ||  j > height - radius ) {
				corr = radius;
			} else {
				corr = 0;
			}
			drawLine( x + corr, y+j, x+width-1-corr, y+j );
		}
	
		int shiftX = width-1 - radius * 2; 
		int shiftY = height-1 - radius * 2; 
		int f = 1 - radius;
		int ddF_x = 1;
		int ddF_y = -radius * 2;
		int x1 = 0;
		int y1 = radius;

		int xx = x + radius;
		int yy = y + radius;
		
		while (x1 < y1) {
			if (f >= 0) {
				y1--;
				ddF_y += 2;
				f += ddF_y;
			}
			x1++;
			ddF_x += 2;
			f += ddF_x;

			drawLine(xx + shiftX, yy - y1, xx + shiftX + x1, yy - y1);
			drawLine(xx - x1, yy - y1, xx, yy - y1);
			drawLine(xx + shiftX, yy - x1, xx + shiftX + y1, yy - x1);
			drawLine(xx - y1, yy - x1, xx, yy - x1);
			
			drawLine(xx + shiftX, yy + y1 + shiftY,  xx + x1 + shiftX, yy + y1 + shiftY);
			drawLine(xx + shiftX, yy + x1 + shiftY, xx + shiftX + y1, yy + x1 + shiftY);
			drawLine(xx - x1, yy + y1 + shiftY, xx, yy + y1 + shiftY);
			drawLine(xx - y1, yy + x1 + shiftY, xx, yy + x1 + shiftY);
		}

//		gc.drawRoundRectangle(x, y, width, height, radius*2, radius*2);

		atomicAction--;
	}
	
	public void drawCircle(final int x, final int y, final int r) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawCircle("+x+","+y+","+r+")");
					drawCircle(x, y, r);
				}
			});
			return;
		}

		aaCounter = 0;
		long start = System.currentTimeMillis();
		
		atomicAction++;

		if ( antialiasingEnabled ) {
			drawCircleAntialiaced(x, y, r, 0);
		} else {
			int f = 1 - r;
			int ddF_x = 1;
			int ddF_y = -2 * r;
			int x1 = 0;
			int y1 = r;

			drawPixel(x, y + r);
			drawPixel(x, y - r);
			drawPixel(x + r, y);
			drawPixel(x - r, y);

			while (x1 < y1) {
				if (f >= 0) {
					y1--;
					ddF_y += 2;
					f += ddF_y;
				}
				x1++;
				ddF_x += 2;
				f += ddF_x;
				drawPixel(x + x1, y + y1);
				drawPixel(x - x1, y + y1);
				drawPixel(x + x1, y - y1);
				drawPixel(x - x1, y - y1);
				drawPixel(x + y1, y + x1);
				drawPixel(x - y1, y + x1);
				drawPixel(x + y1, y - x1);
				drawPixel(x - y1, y - x1);
			}
		}

		atomicAction--;
//		System.out.println( "circle  operations: " + aaCounter + " " + (System.currentTimeMillis() - start) );
	}

	public void fillCircle(final int x, final int y, final int r) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillCircle("+x+","+y+","+r+")");
					fillCircle(x, y, r);
				}
			});
			return;
		}

		atomicAction++;
//		RGB sav = getColor();
//		setColor(background);

		if ( antialiasingEnabled ) {
			drawCircleAntialiaced(x, y, r, 1);
		}
		
		int yy;
		int xx;

		for (yy = -r; yy <= r; yy++) {
			for (xx = -r; xx <= r; xx++) {
				if ((xx * xx) + (yy * yy) <= (r * r)) {
					drawPixel(x+xx, y+yy);
				}
			}
		}
		
//		setColor(sav);
		atomicAction--;
	}

	public void drawOval(final int x, final int y, final int width, final int height) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawOval("+x+","+y+","+width+","+height+")");
					drawOval(x, y, width, height);
				}
			});
			return;
		}

		
		atomicAction++;
		if ( antialiasingEnabled ) {
			drawRoundRectangleAntialiased(x, y, width-1, height-1, (width-1)/2, (height-1)/2, 0);
		} else {
			int ix, iy;
			int h, i, j, k;
			int oh, oi, oj, ok;
			int xmh, xph, ypk, ymk;
			int xmi, xpi, ymj, ypj;
			int xmj, xpj, ymi, ypi;
			int xmk, xpk, ymh, yph;
			
			int rx = (width-1) / 2;
			int ry = (height-1) / 2;
			
			int xx = x + rx;
			int yy = y + ry;

			if ((width <= 0) || (height <= 0)) {
				return;
			}

			if (width == 1) {
				drawLine(xx, yy, xx, yy + height - 1);
				return;
			}
			if (height == 1) {
				drawLine(xx, yy, xx + width - 1, yy);
				return;
			}

			oh = oi = oj = ok = 0xFFFF;

			if (width > height) {
				ix = 0;
				iy = rx * 64;

				do {
					h = (ix + 32) >> 6;
					i = (iy + 32) >> 6;
					j = (h * ry) / rx;
					k = (i * ry) / rx;

					if (((ok != k) && (oj != k)) || ((oj != j) && (ok != j)) || (k != j)) {
						xph = xx + h;
						xmh = xx - h;
						if (k > 0) {
							ypk = yy + k;
							ymk = yy - k;
							drawPixel(xmh, ypk);
							drawPixel(xph, ypk);
							drawPixel(xmh, ymk);
							drawPixel(xph, ymk);
						} else {
							drawPixel(xmh, yy);
							drawPixel(xph, yy);
						}
						ok = k;
						xpi = xx + i;
						xmi = xx - i;
						if (j > 0) {
							ypj = yy + j;
							ymj = yy - j;
							drawPixel(xmi, ypj);
							drawPixel(xpi, ypj);
							drawPixel(xmi, ymj);
							drawPixel(xpi, ymj);
						} else {
							drawPixel(xmi, yy);
							drawPixel(xpi, yy);
						}
						oj = j;
					}

					ix = ix + iy / rx;
					iy = iy - ix / rx;

				} while (i > h);
			} else {
				ix = 0;
				iy = ry * 64;

				do {
					h = (ix + 32) >> 6;
					i = (iy + 32) >> 6;
					j = (h * rx) / ry;
					k = (i * rx) / ry;

					if (((oi != i) && (oh != i)) || ((oh != h) && (oi != h) && (i != h))) {
						xmj = xx - j;
						xpj = xx + j;
						if (i > 0) {
							ypi = yy + i;
							ymi = yy - i;
							drawPixel(xmj, ypi);
							drawPixel(xpj, ypi);
							drawPixel(xmj, ymi);
							drawPixel(xpj, ymi);
						} else {
							drawPixel(xmj, yy);
							drawPixel(xpj, yy);
						}
						oi = i;
						xmk = xx - k;
						xpk = xx + k;
						if (h > 0) {
							yph = yy + h;
							ymh = yy - h;
							drawPixel(xmk, yph);
							drawPixel(xpk, yph);
							drawPixel(xmk, ymh);
							drawPixel(xpk, ymh);
						} else {
							drawPixel(xmk, yy);
							drawPixel(xpk, yy);
						}
						oh = h;
					}

					ix = ix + iy / ry;
					iy = iy - ix / ry;

				} while (i > h);
			}
		}
		atomicAction--;
	}

	public void fillOval(final int xx, final int yy, final int width, final int height) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillOval("+xx+","+yy+","+width+","+height+")");
					fillOval(xx, yy, width, height);
				}
			});
			return;
		}

		int rx = (width-1)/2;
		int ry = (height-1)/2;
		
		int x = xx + rx;
		int y = yy + ry;
		
		int ix, iy;
		int h, i, j, k;
		int oh, oi, oj, ok;
		int xmh, xph;
		int xmi, xpi;
		int xmj, xpj;
		int xmk, xpk;

		if ((rx < 0) || (ry < 0)) {
			return;
		}
		
		if (width < 2) {
			drawLine(xx, yy, xx, yy+height-1);
			return;
		}

		if (height < 2) {
			drawLine(xx, yy, xx+width-1, yy);
			return;
		}


		if ( antialiasingEnabled ) {
			drawRoundRectangleAntialiased(x-rx, y-ry, rx*2, ry*2, rx, ry, 1);
		}
		
		oh = oi = oj = ok = 0xFFFF;

		if (rx > ry) {
			ix = 0;
			iy = rx * 64;

			do {
				h = (ix + 32) >> 6;
				i = (iy + 32) >> 6;
				j = (h * ry) / rx;
				k = (i * ry) / rx;

				if ((ok != k) && (oj != k)) {
					xph = x + h;
					xmh = x - h;
					if (k > 0) {
						drawLine(xmh, y + k, xph, y + k);
						drawLine(xmh, y - k, xph, y - k);
					} else {
						drawLine(xmh, y, xph, y);
					}
					ok = k;
				}
				if ((oj != j) && (ok != j) && (k != j)) {
					xmi = x - i;
					xpi = x + i;
					if (j > 0) {
						drawLine(xmi, y + j, xpi, y + j);
						drawLine(xmi, y - j, xpi, y - j);
					} else {
						drawLine(xmi, y, xpi, y);
					}
					oj = j;
				}

				ix = ix + iy / rx;
				iy = iy - ix / rx;

			} while (i > h);
		} else {
			ix = 0;
			iy = ry * 64;

			do {
				h = (ix + 32) >> 6;
				i = (iy + 32) >> 6;
				j = (h * rx) / ry;
				k = (i * rx) / ry;

				if ((oi != i) && (oh != i)) {
					xmj = x - j;
					xpj = x + j;
					if (i > 0) {
						drawLine(xmj, y + i, xpj, y + i);
						drawLine(xmj, y - i, xpj, y - i);
					} else {
						drawLine(xmj, y, xpj, y);
					}
					oi = i;
				}
				if ((oh != h) && (oi != h) && (i != h)) {
					xmk = x - k;
					xpk = x + k;
					if (h > 0) {
						drawLine(xmk, y + h, xpk, y + h);
						drawLine(xmk, y - h, xpk, y - h);
					} else {
						drawLine(xmk, y, xpk, y);
					}
					oh = h;
				}

				ix = ix + iy / ry;
				iy = iy - ix / ry;

			} while (i > h);
		}
	}

	public int getScroll() {
//		if( orientation > PORTRAIT ) {
//			int base = Math.max( width, height );
//			return (base - DataLayerView.deviceScroll) % base;
//		} else {
		return DataLayerView.deviceScroll;
//		}
	}
	
	public void setScrollStepDelay(int ms) {
		extraScrollDelay = ms;
	}
	
	public void scroll(final int dy, final int flags) {
		scroll(dy, 0, DataLayerView.deviceWidth, flags);
	}
	
	public void scroll(final int dy, final int x1, final int x2, final int flags) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

	    int mdy = dy > 0 ? dy : -dy;

	    if (mdy > 1 && (flags & SCROLL_SMOOTH) > 0) {

	    	DataLayerView.suppressGrid = true;
	    	
	    	int easingLen = 8;
	        if ( mdy / 2 < easingLen) {
	            easingLen = mdy / 2;
	        }

	        int dlx = (flags & SCROLL_CLEAN) > 0 ? 0 : 7;
	        int factor = 1;
	        
			int s = atomicAction;
			atomicAction = 0;
	        
	        int step = dy < 0 ? -1 : 1;
	        for ( int i = 0; i < easingLen; i++ ) {
	            delay((dlx+(easingLen-i)*(easingLen-i)/2)*factor+extraScrollDelay);
	            scroll(step, x1, x2, flags & SCROLL_CLEAN);
	        }
	        for ( int i = 0; i < mdy - easingLen*2; i++ ) {
	            delay((dlx)*factor+extraScrollDelay);
	            scroll(step, x1, x2, flags & SCROLL_CLEAN);
	        }
	        for ( int i = 1; i <= easingLen; i++ ) {
	            delay((dlx+i*i/2)*factor+extraScrollDelay);
	            scroll(step, x1, x2, flags & SCROLL_CLEAN);
	        }
	        
			atomicAction = s;

	    	DataLayerView.suppressGrid = false;

	    } else {

			if ( Display.getCurrent() == null ) {
				display.syncExec(new Runnable() {
					public void run() {
						debug("scroll("+dy+","+x1+","+x2+","+flags+")");
						scroll(dy, x1, x2, flags);
					}
				});
				return;
			}

			int delta = dy;

            RGB sav = getColor();
            setColor(getBackground());
        	boolean savorigin = relativeOrigin;
       		relativeOrigin = false;

	        if ( (flags & SCROLL_CLEAN) > 0 && dy > 0 ) {
    			if( (DataLayerView.deviceOrientation % 2) == 0 ) { // PORTRAIT(_FLIP)
               		fillRectangle(0, 0, DataLayerView.deviceWidth, mdy);
    			} else {
    				fillRectangle(0, 0, mdy, DataLayerView.deviceWidth);
    			}
	        }

	    	DataLayerView.deviceScroll += delta;
	        while ( DataLayerView.deviceScroll < 0 ) {
	        	DataLayerView.deviceScroll += DataLayerView.deviceHeight;
	        } 
	    	DataLayerView.deviceScroll %= DataLayerView.deviceHeight;

	        if ( (flags & SCROLL_CLEAN) > 0 && dy < 0 ) {
    			if( (DataLayerView.deviceOrientation % 2) == 0 ) { // PORTRAIT(_FLIP)
               		fillRectangle(0, 0, DataLayerView.deviceWidth, mdy);
    			} else {
    				fillRectangle(0, 0, mdy, DataLayerView.deviceWidth);
    			}
	        }

       		relativeOrigin = savorigin;
            setColor(sav);
	        
			DataLayerView.repaintRuler = true;
			updateDevice(0, 0, DataLayerView.width, DataLayerView.height, true);
	    }
	}

	public void drawBitmap(final int x, final int y, final int width, final int height, final int[] data)	{

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawBitmap("+x+","+y+","+width+","+height+",...)");
					drawBitmap(x, y, width, height, data);
				}
			});
			return;
		}

		atomicAction++;

		if (DataLayerView.stop) {
			return;
		}
		
		setRegion(x, y, x+width, y+height);
		
		int ptr = 0;
		for ( int j = 0; j < height; j++ ) {
			for ( int i = 0; i < width; i++ ) {
				int p;
				try {
					p = data[ptr++];
				} catch (Exception e1) {
					String ddd = (data == null ? "null" : "int[" + data.length + "]" );
					System.out.println( "drawBitmap("+x+", "+y+", "+width+", "+height+", "+ddd+"): provided binary data does not match specified image size. Expected: int["+(width*height)+"]" );
					atomicAction--;
					return;
				}
				int r = ((0xf800 & p)>>11) * 255 / 31;
				int g = ((0x7e0 & p)>>5) * 255 / 63;
				int b = (0x1f & p) * 255 / 31;
	
				try {
					RGB rgb = new RGB(r, g, b);
					setCurrentPixel(rgb);
					
//					Color cx = new Color(display, rgb);
//					gc.setForeground(cx);
//					gc.drawPoint(x + i, y + j);
				} catch (Exception e) {
					System.err.println(r + ", " + g + ", " + b);
					e.printStackTrace();
					break;
				}
			}
		}
		
		atomicAction--;
	}

	public int drawCompressedBitmap( int x, int y, int[] intData ) {

		if ( intData == null || intData.length < 12 ) {
			return -1;
		}
		
		if ( intData[0] != 'Z' ) {
			System.out.println( "Unknown compression method" );
			return -2;
		}
		
		int compressedLen = ((0xFF & intData[1]) << 16) + ((0xFF & intData[2]) << 8) + (0xFF & intData[3]);
		if ( compressedLen < 0 || compressedLen != intData.length - 12 ) {
			System.out.println( "Unknown compression method or compressed data inconsistence" );
			return -3;
		}
		
		int resultLen = ((0xFF & intData[4]) << 16) + ((0xFF & intData[5]) << 8) + (0xFF & intData[6]);
		if ( resultLen < 0 ) {
			System.out.println( "Unknown compression method or compression format error" );
			return -4;
		}

		int windowLen = 0xFF & intData[7];

		int width = ((0xFF & intData[8]) << 8) + (0xFF & intData[9]);
		if ( width <= 0 ) {
			System.out.println( "Unknown compression method or compression format error (width parameter is invalid)" );
			return -5;
		}
		
		int height = ((0xFF & intData[10]) << 8) + (0xFF & intData[11]);
		if ( height <= 0 ) {
			System.out.println( "Unknown compression method or compression format error (height parameter is invalid)" );
			return -6;
		}
		
		byte[] data = new byte[intData.length];
		for ( int i = 0; i < intData.length; i++ ) {
			data[i] = (byte)(0xFF & intData[i]);
		}
		
		atomicAction++;

		setRegion(x, y, x+width, y+height);
		
		byte[] window = new byte[windowLen];
		int wptr = 0;
		int ctr = 0;
		
	    int buf = 0;
	    boolean bufEmpty = true;
		
		BitStream bs = new BitStream( data, 96 ); // 96bit = 12bytes of prefix
		while ( true ) {
			
			int bit = bs.readBit();
			if ( bit == 0 ) { // literal
				int bits = bs.readBits(8);
	            if ( bufEmpty ) {
	                buf = bits;
	                bufEmpty = false;
	            } else {
	                int px = buf;
	                px <<= 8;
	                px |= bits;
	                
					int r = ((0xf800 & px)>>11) * 255 / 31;
					int g = ((0x7e0 & px)>>5) * 255 / 63;
					int b = (0x1f & px) * 255 / 31;
					try {
						RGB rgb = new RGB(r, g, b);
						setCurrentPixel(rgb);
					} catch (Exception e) {
						System.err.println(r + ", " + g + ", " + b);
						e.printStackTrace();
						break;
					}
	                bufEmpty = true;
	            }
	            ctr++;
				window[wptr++] = (byte)(0xFF & bits);
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
						
			            if ( bufEmpty ) {
			                buf = window[p1];
			                bufEmpty = false;
			            } else {
			                int px = buf;
			                px <<= 8;
			                px |= (0xFF & window[p1]);
							int r = ((0xf800 & px)>>11) * 255 / 31;
							int g = ((0x7e0 & px)>>5) * 255 / 63;
							int b = (0x1f & px) * 255 / 31;
							try {
								RGB rgb = new RGB(r, g, b);
								setCurrentPixel(rgb);
							} catch (Exception e) {
								System.err.println(r + ", " + g + ", " + b);
								e.printStackTrace();
								break;
							}
			                bufEmpty = true;
			            }
						window[p2] = window[p1];
					}
				} catch (Exception e) {
					e.printStackTrace();
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
		
		atomicAction--;
		return 0;
	}
	
	public byte loadBitmap(final int x, final int y, final int sx, final int sy, final String filename) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return 1;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("loadBitmap("+x+","+y+","+sx+","+sy+","+filename+")");
					loadBitmap(x, y, sx, sy, filename);
				}
			});
			return 0;
		}

		atomicAction++;

		
		int[] data;
		try {
			data = loadFileBytes( filename );
		} catch (Exception e) {
			System.out.println( "Cannot load " + new File(filename).getAbsolutePath() );
			return 1;
		}
		
		drawBitmap(x, y, sx, sy, data);

		atomicAction--;
		return 0;
	}

	/*  -------   Antialiasing ------- */

	private void drawLineAntialiased(int x1, int y1, int x2, int y2) {

		aaCounter++;
		long start = System.currentTimeMillis();
		
	    boolean steep = (y2 > y1 ? y2 - y1 : y1 - y2) > (x2 > x1 ? x2 - x1 : x1 - x2);
	    if (steep) {
	    	int tmp = x1;
	    	x1 = y1;
	    	y1 = tmp;
	    	tmp = x2;
	    	x2 = y2;
	    	y2 = tmp;
	    }
	    if (x1 > x2) { 
	    	int tmp = x1;
	    	x1 = x2;
	    	x2 = tmp;
	    	tmp = y1;
	    	y1 = y2;
	    	y2 = tmp;
	    }
	    int deltax = x2 - x1;
	    int deltay = y2 - y1;
	    double gradient = 1.0 * deltay / deltax;
	 
	    int xend = x1; // round(x1);
	    double yend = y1 + gradient * (xend - x1);
	    double xgap = rfpart(x1 + 0.5);
	    int xpxl1 = xend;
	    int ypxl1 = ipart(yend);
	    putColor(xpxl1, ypxl1, steep, rfpart(yend)*xgap);
	    putColor(xpxl1, ypxl1 + 1, steep, fpart(yend)*xgap);
	    double intery = yend + gradient;
	 
	    xend = x2; // round(x2);
	    yend = y2 + gradient * (xend - x2);
	    xgap = rfpart(x2 + 0.5);
	    int xpxl2 = xend;
	    int ypxl2 = ipart(yend);
	    putColor(xpxl2, ypxl2, steep, rfpart(yend)*xgap);
	    putColor(xpxl2, ypxl2 + 1, steep, fpart(yend)*xgap);
	 
	    for ( int x = xpxl1 + 1; x < xpxl2 - 1; x++ ) {
		      putColor(x, ipart(intery), steep, rfpart(intery));
		      putColor(x, ipart(intery) + 1, steep, fpart(intery));
		      intery += gradient;
	    }

//		System.out.println( "width=1 operations: " + aaCounter + " " + (System.currentTimeMillis() - start) );
	
	}
	 
	int aaCounter;
	
	private void drawFatLineAntialiased(int x0, int y0, int x1, int y1) { 

		double wd = lineWidth;
		
		int dx = Math.abs(x1 - x0); 
		int sx = x0 < x1 ? 1 : -1;
		int dy = Math.abs(y1 - y0);
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy; 

		int e2; 
		int x2; 
		int y2;

		
		aaCounter = 0;
		long start = System.currentTimeMillis();
		
		double ed = dx + dy == 0 ? 1 : Math.sqrt((double) dx * dx + (double) dy * dy);

		wd = (wd + 1) / 2;
		while ( true ) { 
			putColor(x0, y0, false, 1 - Math.max(0, Math.abs(err-dx+dy)/ed - wd + 1));
			e2 = err;
			x2 = x0;
			boolean out = false;
			if (2 * e2 >= -dx) { /* x step */
				for (e2 += dy, y2 = y0; e2 < ed * wd && (y1 != y2 || dx > dy); e2 += dx) {
					putColor(x0, y2 += sy, false, 1 - Math.max(0, Math.abs(e2)/ed - wd + 1));
				}
				if (x0 == x1) {
					out = true;
				}
				e2 = err;
				err -= dy;
				x0 += sx;
			}
			if (2 * e2 <= dy) { /* y step */
				for (e2 = dx - e2; e2 < ed * wd && (x1 != x2 || dx < dy); e2 += dy) {
					putColor(x2 += sx, y0, false, 1 - Math.max(0, Math.abs(e2)/ed - wd + 1));
				}
				if (y0 == y1) {
					out = true;
				}
				err += dx;
				y0 += sy;
			}
			if ( out ) {
				break;
			}
		}
		
//		System.out.println( "width!=1  operations: " + aaCounter + " " + (System.currentTimeMillis() - start) );
	}
	
	public void drawRoundRectangleAntialiased(final int x, final int y, final int width, final int height, final int rx, final int ry, int bordermode) {

		int i;
		int a2, b2, ds, dt, dxt, t, s, d;
		int xp, yp, xs, ys, dyt, od, xx, yy, xc2, yc2;
		float cp;
		double sab;
		double weight, iweight;

		if ((rx < 0) || (ry < 0)) {
			return;
		}

		if (rx == 0) {
			drawLine(x, y - ry, x, y + ry);
			return;
		}

		if (ry == 0) {
			drawLine(x - rx, y, x + rx, y);
			return;
		}

		a2 = rx * rx;
		b2 = ry * ry;

		ds = 2 * a2;
		dt = 2 * b2;

		xc2 = 2 * x;
		yc2 = 2 * y;

		sab = Math.sqrt((double)(a2 + b2));
		od = (int)round(sab*0.01) + 1; 
		dxt = (int)round((double)a2 / sab) + od;

		t = 0;
		s = -2 * a2 * ry;
		d = 0;

		xp = x + rx;
		yp = y;
		
		drawLine(x + rx, y + height, x + width - rx, y + height);
		drawLine(x + rx, y, x + width - rx, y );
		drawLine(x + width, y + ry, x + width, y + height - ry);
		drawLine(x, y + ry, x, y + height - ry);
		
		for (i = 1; i <= dxt; i++) {
			xp--;
			d += t - b2;

			if (d >= 0) {
				ys = yp - 1;
			} else if ((d - s - a2) > 0) {
				if ((2 * d - s - a2) >= 0) {
					ys = yp + 1;
				} else {
					ys = yp;
					yp++;
					d -= s + a2;
					s += ds;
				}
			} else {
				yp++;
				ys = yp + 1;
				d -= s + a2;
				s += ds;
			}

			t -= dt;

			if (s != 0) {
				cp = (float) Math.abs(d) / (float) Math.abs(s);
				if (cp > 1.0) {
					cp = 1.0f;
				}
			} else {
				cp = 1.0f;
			}

			weight = cp;
			iweight = 1 - weight;

			if( bordermode == 1 ) {
				iweight = yp > ys ? 1 : iweight;
				weight = ys > yp ? 1 : weight;
			}
			
			/* Upper half */
			xx = xc2 - xp;
			putColor(xp, yp, false, iweight);
			putColor(xx+width, yp, false, iweight);

			putColor(xp, ys, false, weight );
			putColor(xx+width, ys, false, weight);

			/* Lower half */
			yy = yc2 - yp;
			putColor(xp, yy+height, false, iweight);
			putColor(xx+width, yy+height, false, iweight);

			yy = yc2 - ys;
			putColor(xp, yy+height, false, weight);
			putColor(xx+width, yy+height, false, weight);
		}

		/* Replaces original approximation code dyt = abs(yp - yc); */
		dyt = (int)round((double)b2 / sab ) + od;    

		for (i = 1; i <= dyt; i++) {
			yp++;
			d -= s + a2;

			if (d <= 0) {
				xs = xp + 1;
			} else if ((d + t - b2) < 0) {
				if ((2 * d + t - b2) <= 0) {
					xs = xp - 1;
				} else {
					xs = xp;
					xp--;
					d += t - b2;
					t -= dt;
				}
			} else {
				xp--;
				xs = xp - 1;
				d += t - b2;
				t -= dt;
			}

			s += ds;

			if (t != 0) {
				cp = (float) Math.abs(d) / (float) Math.abs(t);
				if (cp > 1.0) {
					cp = 1.0f;
				}
			} else {
				cp = 1.0f;
			}

			weight = cp;
			iweight = 1 - weight;

			/* Left half */
			xx = xc2 - xp;
			yy = yc2 - yp;
			putColor(xp, yp, false, iweight);
			putColor(xx+width, yp, false, iweight);

			putColor(xp, yy+height, false, iweight);
			putColor(xx+width, yy+height, false, iweight);

			/* Right half */
			xx = xc2 - xs;
			putColor(xs, yp, false, weight);
			putColor(xx+width, yp, false, weight);

			putColor(xs, yy+height, false, weight);
			putColor(xx+width, yy+height, false, weight);
		}
	}

	private void drawCircleAntialiaced( int x, int y, int radius, int bordermode )	{
		drawRoundRectangleAntialiased(x-radius, y-radius, radius*2, radius*2, radius, radius, bordermode);
	}

	public int getIconHeight(final int[] bytes) {
		return bytes[4];
	}
	
	public int getIconWidth(final int[] bytes) {
		return bytes[5];
	}
	
	public void drawIcon(final int xx, final int yy, final int[] bytes) {
		
		int fontType = BITMASK_FONT;
		if ( bytes[1] == (byte)'a' ) {
			fontType = ANTIALIASED_FONT;
		}
		
		int height = bytes[4];
		
		drawGlyph(fontType, false, xx, yy, height, bytes, 1, bytes.length-1); 
	}

	public void cleanIcon(final int xx, final int yy, final int[] bytes) {
		
		int fontType = BITMASK_FONT;
		if ( bytes[1] == (byte)'a' ) {
			fontType = ANTIALIASED_FONT;
		}
		
		int height = bytes[4];
		
		drawGlyph(fontType, true, xx, yy, height, bytes, 1, bytes.length-1); 
	}

	
	/*  -------   TEXT   -------   */
	
	public void setFont( int[] font ) {
		
		String param;
		if ( font == null) {
			param = "null";
		} else {
			param = "int[" + font.length + "]";
		}

		if ( font == null || font.length < HEADER_LENGTH + 7 ) {
			System.out.println("pxs.setFont(" + param + "): Invalid font");
			return;
		}

		int p1 = font[0];
		int p2 = font[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("pxs.setFont(" + param + "): Invalid font prefix");
			return;
		}
		int fontType = font[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("pxs.setFont(" + param + "): Unsupported font type");
			return;
		}
		currentFont = font;
	}

	public int getTextLineHeight() {
		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("pxs.getTextLineHeight(): No font specified");
			return -1;
		}

		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("pxs.getTextLineHeight(): Invalid font prefix");
			return -1;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("pxs.getTextLineHeight(): Unsupported font type");
			return -1;
		}
		return currentFont[3];
	}
	
	public int getTextBaseline() {
		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("pxs.getBaseline(): No font specified");
			return -1;
		}

		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("pxs.getTextBaseline(): Invalid font prefix");
			return -1;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("pxs.getTextBaseline(): Unsupported font type");
			return -2;
		}
		return currentFont[4];
	}

	public int getTextWidth(final String text) {
		return getTextWidth(text, null);
	}

	public int getTextWidth(final String text, final int[] kerning) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return -1;
		}

		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("pxs.getTextWidth(\"" + text + "\"): No font specified");
			return -1;
		}
		
		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("pxs.getTextWidth(\"" + text + "\"): Invalid font prefix");
			return 0;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("pxs.getTextWidth(\"" + text + "\"): Unsupported font type");
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
				if ( length <= 0 ) {
					break;
				}

				if ( cx == c ) {
					if ( length < 8 ) {
						System.out.println( "pxs.getTextWidth(\"" + text + "\"): Invalid " + c + " glyph definition. Font corrupted?" );
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
	
	public int getCharWidth(final char ch) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return -1;
		}

		if ( currentFont == null || currentFont.length < HEADER_LENGTH + 7 ) {
			System.out.println("pxs.getCharWidth('" + ch + "'): No font specified");
			return -1;
		}
		
		int p1 = currentFont[0];
		int p2 = currentFont[1];
		if ( p1 != 'Z' || p2 != 'F' ) {
			System.out.println("pxs.getCharWidth('" + ch + "'): Invalid font prefix");
			return 0;
		}
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println("pxs.getCharWidth('" + ch + "'): Unsupported font type");
			return 0;
		}

		int ptr = HEADER_LENGTH;
		while ( ptr < currentFont.length ) {
			char cx = (char)(((int)currentFont[ptr + 0] << 8) + currentFont[ptr + 1]);
			if ( cx == 0 ) {
				break;
			}
			int length = (((int)(currentFont[ptr + 2] & 0xff) << 8) + (int)(currentFont[ptr + 3] & 0xff));
			if ( length <= 0 ) {
				return 0;
			}

			if ( cx == ch ) {
				if ( length < 8 ) {
					System.out.println( "pxs.getTextWidth('" + ch + "'): Invalid glyph definition. Font corrupted?" );
					break;
				}

				return 0xff & currentFont[ptr + 4];
			}
			
			ptr += length;
		}

		return 0;
	}
	
	public void print(final int xx, final int yy, final String text) {
		printString( xx, yy, text, null, false );
	}

	public void print(final int xx, final int yy, final String text, final int[] kerning) {
		printString( xx, yy, text, kerning, false );
	}

	public void cleanText(final int xx, final int yy, final String text) {
		printString( xx, yy, text, null, true );
	}
	
	public void cleanText(final int xx, final int yy, final String text, final int[] kerning) {
		printString( xx, yy, text, kerning, true );
	}
	
	private int computeBreakPos(final String text, int t) {
		int breakPos = -1;
		String s = t == 0 ? text : text.substring(t, text.length()); 
		int w = getTextWidth(s);
		if ( w + caretX > DataLayerView.width - textWrapMarginRight || text.indexOf('\n') > 0 ) {
			char prev = 0;
			w = 0;
			for ( int j = t; j < text.length(); j++ ) {
				char cc = text.charAt(j);
				w += getTextWidth(""+cc);
				if ( cc == ' ' && prev != ' ' || cc == '\n' && breakPos >= 0 ) {
					if ( caretX + w > DataLayerView.width - textWrapMarginRight ) {
						break;
					} else {
						breakPos = j;
						if ( cc == '\n' ) {
							break;
						}
					}
				} else if ( cc == '\n' ) {
					breakPos = j;
					break;
				}
				prev = cc;
			}
		}
		return breakPos;
	}

	private void printString(final int xx, final int yy, final String text, final int[] kerning, final boolean clean) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		final String name = clean ? "pxs.clean" : "pxs.print";
		
		if ( currentFont == null ) {
			System.out.println("ERROR: no font specified. pxs.setFont() expected before pxs.print()");
			return;
		}
		
		String hash = ""+currentFont.hashCode();
		TreeSet<Character> set = fontStats.get(hash);
		if ( set == null ) {
			set = new TreeSet();
			fontStats.put(hash, set);
		}
		char[] chs = text.toCharArray();
		for ( int i = 0; i < text.length(); i++ ) {
			Character c = new Character(chs[i]);
			set.add( c );
		}

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
		int fontType = currentFont[2];
		if ( fontType != ANTIALIASED_FONT && fontType != BITMASK_FONT ) {
			System.out.println(name + "(\"" + text + "\"): Unsupported font type");
			return;
		}
		int glyphHeight = currentFont[3];
//		int baseline = currentFont[4];

//		System.out.println( "glyph height: " + glyphHeight );
//		System.out.println( "baseline: " + baseline );
		
		atomicAction++;
		
		int kernPtr = 0;
		int kern = -100; // no kerning

		HashSet<Character> missingGlyphs = new HashSet<Character>();
		
		caretX = xx;
		caretY = yy;

		int glyphWidth = 0;
		int breakPos = -1;

		boolean relOrigin = isOriginRelative();
		if ( wrapText ) {
			breakPos = computeBreakPos(text, 0);
		}
		
		for (int t = 0; t < text.length(); t++) {
			char c = text.charAt(t);
			
			if ( t == breakPos ) {
				if ( c == ' ' || c == '\n' ) {
					breakPos++;
					continue;
				}
				caretX = textWrapMarginLeft;
				caretY = caretY + glyphHeight + textWrapLineGap;
				if ( textWrapScroll && !DataLayerView.landscapeViewer && 
						caretY + glyphHeight + textWrapMarginBottom > DataLayerView.height ) {
					RGB sav = null;
					if ( textWrapScrollFill != null ) {
						sav = getBackground();
						setBackground(textWrapScrollFill);
					}

					int s = atomicAction;
					atomicAction = 0;
//					DataLayerView.repaintRuler = true;
					updateDevice(0, 0, DataLayerView.width, DataLayerView.height, true);
					atomicAction = s;

					scroll(-(DataLayerView.height - caretY - glyphHeight - textWrapMarginBottom), SCROLL_CLEAN | SCROLL_SMOOTH);
					
					
					if ( sav != null ) {
						setBackground(sav);
					}
					setOriginAbsolute();
					caretY = DataLayerView.height - glyphHeight - textWrapMarginBottom;
				}
				breakPos = computeBreakPos(text, t);
			}
			
			boolean repeat = false;
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

					glyphWidth = 0xff & currentFont[ptr + 4];
					
					if ( wrapText && caretX + glyphWidth > DataLayerView.width - textWrapMarginRight ) {
						breakPos = t;
						repeat = true;
						break;
					}

					drawGlyph(fontType, clean, caretX, caretY, glyphHeight, currentFont, ptr, length); 
					break;
				}
				ptr += length;
			}
			
			if ( repeat ) {
				t--;
				continue;
			}

			if ( kerning != null && kerning.length > 0 && kerning[kernPtr] > -100 ) {
				kern = kerning[kernPtr];
				if (kerning[kernPtr+1] > -100) {
					kernPtr++;
				}
			}
			
			if ( found ) {
				caretX += glyphWidth;
				if ( kern > -100 ) {
					caretX += kern;
				}
			}
		}
		
		if ( relOrigin ) {
			setOriginRelative();
		} else {
			setOriginAbsolute();
		}

		atomicAction--;
		
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

	public int getPrintMode() {
		return glyphPrintMode;
	}

	public void setPrintMode(int glyphPrintMode) {
		this.glyphPrintMode = glyphPrintMode;
	}

    public void enableTextWrap(int marginLeft, int marginRight, int lineGap) {
        wrapText = true;
        textWrapMarginLeft = marginLeft;
        textWrapMarginRight = marginRight;
        textWrapLineGap = lineGap;
    }

    public void enableTextWrapScroll(int marginBottom, RGB scrollFill) {
    	textWrapMarginBottom = marginBottom;
    	textWrapScrollFill = scrollFill;
        textWrapScroll = true;
    }

    public void disableTextWrap() {
        wrapText = false;
    }

    public void disableTextWrapScroll() {
        textWrapScroll = false;
    }

    public int getCaretX() {
        return caretX;
    }

    public int getCaretY() {
        return caretY;
    }

    public void scrollText( int x, int y, String text, int scrollStep, int repeat, int maxScroll ) {
    	
    	int extraRowDelay = 0; // increase to slow down
    	
    	if ( getOrientation() % 2 == 0 ) {
    		setOrientation(LANDSCAPE);
    	}
    	
    	if ( repeat != 0  ) {
    		repeat++;
    	}

    	int maxX = getWidth() - 1;
    	int tw = getTextWidth(text);
    	
    	int skip = -1; 
    	int loopLen = tw + x;
    	int space = maxX - x;
    	
    	if (getScroll() > 0) {
    		skip = (x - getScroll()) % getWidth();
    		loopLen += maxX - getScroll();
    		space = getScroll() - x; 
    	}
    	
    	int easingLen = 5;
    	if ( loopLen / 2 < easingLen) {
    		easingLen = loopLen / 2;
    	}
    	loopLen += easingLen;				
    	
    	int dlx = 8;
    	int factor = 3 + extraRowDelay;
    	int remains = 0;

//    	long maxLatency = 0;
//    	boolean firstLoop = true;

    	if ( maxScroll > 0 ) {
    		loopLen = maxScroll;
    		repeat = 2;
    	}
    	
    	do {
    		for ( int i = 0; i < loopLen; i+=scrollStep ) {

//    			long startMillis = millis();
    			
    			int p = -1;
    			int e = -1;
    			int xx = 0;
    			int cw = 0;
    			
    			int l = 0;
    			int f = 0;
    			for (int t = 0; t < (int)text.length(); t++) {
    				char c = text.charAt(t);
    				f = l;
    				cw = getCharWidth(c);
    				if ( cw < 0 ) {
    					return;
    				}
    				l += cw;
    				if ( l > remains && p < 0 ) {
    					xx = f;
    					p = t;
    				}
    				if ( l > space ) {
    					e = t + 1;
    					break;
    				}
    			}
    			if ( p < 0 ) {
    				p = text.length();  
    			}
    			if ( e < 0 ) {
    				e = text.length();  
    			}
    			
    			String s = text.substring(p, e);
    			int q = (x + xx) % getWidth();
    			
    			if ( q != 0 || x != getWidth() || getScroll() != 0 ) {
    				if (i > skip) {
    					print(q, y, s);
    				}
    			}
    			if ( q > maxX - cw && q > getScroll() && getScroll() != 0 ) {
    				print(q - getWidth(), y, s);
    			}
    			
    			remains = space;
    			space += scrollStep;
    			scroll(scrollStep, SCROLL_CLEAN);

//    			long endMillis = millis();
//    #ifndef PIXELMEISTER					
//    			if ( s.length() < 3 ) {
//    				long latency = endMillis - startMillis;
//
//    				if (maxLatency > latency) {
//    					delay((int)(maxLatency - latency));
//    				} else {
//    					if( firstLoop ) {
//    						maxLatency = latency;
//    					}
//    				}
//    			}
//    #endif					
    			
    			if ( i < easingLen ) {
    				delay(dlx+(easingLen-i)*(easingLen-i)*factor/2);
    			} else {
    				if ( loopLen > 150 ) {
    					delay(factor);
    				} else {
    					delay(dlx+factor);
    				}
     			}
//    			startMillis = endMillis;
    		}
    		
//    		firstLoop = false;
    					
    		remains = 0;
    		space = 0;
    		easingLen = 0;
    		skip = -1;
    		
    		x = getWidth();
    		loopLen = x + tw;

    		if ( maxScroll <= 0 ) {
    			scroll(-getScroll(), 0); 
    		}

    		if ( repeat != 0 ) {
    			repeat--;
    		}
    	} while ( repeat == 0 || repeat > 1 );
    }
    
	
	private void drawGlyph(final int fontType, final boolean clean, final int xx, final int yy, 
			final int glyphHeight, final int[] data, final int ptr, final int length) {

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
//					debug(name + "("+xx+", "+yy+","+text+", kerning)");
					drawGlyph(fontType, clean, xx, yy, glyphHeight, data, ptr, length);
				}
			});
			return;
		}
		
		int glyphWidth = 0xff & data[ptr + 4];
		int marginLeft = 0x7f & data[ptr + 5];
		int marginTop = 0xff & data[ptr + 6];
		int marginRight = 0x7f & data[ptr + 7];
		
		int effWidth = glyphWidth - marginLeft - marginRight;

		boolean vraster = (0x80 & data[ptr + 5]) > 0;
		boolean compressed = (data[ptr + 7] & 0x80) > 0;

		boolean quickDebug = false;
		if ( quickDebug ) {
			char c = (char)(((int)data[ptr + 0] << 8) + data[ptr + 1]);
			if ( fontType == ANTIALIASED_FONT ) {
				if ( vraster ) {
					System.out.println( "'" + c + "' ||" );
				} else {
					System.out.println( "'" + c + "' =" );
				}
			} else if ( fontType == BITMASK_FONT ) {
				if ( compressed ) {
					if ( vraster ) {
						System.out.println( "" + c + " |" );
					} else {
						System.out.println( "'" + c + "' -" );
					}
				} else {
					System.out.println( "" + c + " -x-" );
				}
			}
//			System.out.println("'" + c + "' w:" + width + ", h:" + height + ", ml:" + marginLeft + ", mt:" + marginTop + ", mr:" + marginRight + ", l:" + length);
		}
		
		RGB fg = foreground;
		RGB bg = background;
		
		int ctr = 0;
		int prev = -1;
		int last = -1;

		int eff = vraster ? 
				glyphHeight - marginTop - marginRight : 
				glyphWidth - marginLeft - marginRight;

		marginLeft += xx;
		marginTop += yy;

		if ( !(fontType == BITMASK_FONT && !compressed) ) {

			for ( int i = 0; i < length - 8; i++ ) {
				
				int p1 = ctr / eff;
				int p2 = ctr % eff;
				
				int b = 0xff & data[ptr + 8 + i];
				int len = fontType == BITMASK_FONT ? 0x7f & b : 0;
				boolean color = fontType == BITMASK_FONT ? (0x80 & b) > 0 : true;
				
				if ( color || glyphPrintMode == FILL_TEXT_BACKGROUND ) {

					if ( glyphPrintMode == FILL_TEXT_BACKGROUND && prev != p1 ) {
						setColor(bg);
						if ( vraster ) {
							if ( prev < 0 ) {
								fillRectangle(xx, yy, marginLeft - xx + 1, glyphHeight + 1);
							} else {
								drawLine(marginLeft + p1, yy, marginLeft + p1, yy + glyphHeight);
							}
						} else {
							if ( prev < 0 ) {
								fillRectangle(xx, yy, glyphWidth + 1, marginTop - yy + 1);
							} else {
								drawLine(xx, marginTop + p1, xx + glyphWidth, marginTop + p1);
							}
						}
						prev = p1;
					}
	
					int x = vraster ? marginLeft + p1 : marginLeft + p2;
					int y = vraster ? marginTop + p2 : marginTop + p1;
	
					if ( color && !clean ) {
						setColor(fg);
					} else { 
						setColor(bg);
					} 
					
					if ( fontType == BITMASK_FONT || (0xc0 & b) > 0 ) {
						if ( fontType == ANTIALIASED_FONT ) {
							len = 0x3f & b;
							ctr += len;
						}
	
						if ( fontType == BITMASK_FONT || (0x80 & b) > 0 ) {
							
							while ( p2 + len > eff ) {
								if ( color ) {
									if ( vraster ) {
										drawLine(x, y, x, marginTop + eff - 1);
									} else {
										drawLine(x, y, marginLeft + eff - 1, y);
									}
								}
								if (fontType == BITMASK_FONT) {
									ctr += eff - p2;
								}
								len -= eff - p2;
								p2 = 0;
								p1++;
								x = vraster ? marginLeft + p1 : marginLeft + p2;
								y = vraster ? marginTop + p2 : marginTop + p1;
								if ( glyphPrintMode == FILL_TEXT_BACKGROUND ) {
									setColor(bg);
									if ( vraster ) {
										drawLine(x, yy, x, yy + glyphHeight);
									} else {
										drawLine(xx, y, xx + glyphWidth, y);
									}
									if ( !clean ) {
										setColor(fg);
									}
									prev = p1;
								}
							}
							if ( color ) {
								if ( vraster ) {
									drawLine(x, y, x, y + len - 1);
								} else {
									drawLine(x, y, x + len - 1, y);
								}
							} else {
								setColor(fg);
							}
						} else if ( fontType == ANTIALIASED_FONT && glyphPrintMode == FILL_TEXT_BACKGROUND ) {
							setColor(bg);
							while ( p2 + len > eff ) {
								len -= eff - p2;
								p2 = 0;
								p1++;
								x = vraster ? marginLeft + p1 : marginLeft + p2;
								y = vraster ? marginTop + p2 : marginTop + p1;
	
								if ( vraster ) {
									drawLine(x, yy, x, yy + glyphHeight);
								} else {
									drawLine(xx, y, xx + glyphWidth, y);
								}
							}
							prev = p1;
							if ( !clean ) {
								setColor(fg);
							}
						}
					} else if (fontType == ANTIALIASED_FONT) {
						if ( clean ) {
							setColor(bg);
						} else {
							int opacity = (0xff & (b * 4)); 
							int sr = (fg.red * (255 - opacity) + bg.red * (opacity))/255;
							int sg = (fg.green * (255 - opacity) + bg.green * (opacity))/255;
							int sb = (fg.blue * (255 - opacity) + bg.blue * (opacity))/255;
							setColor(sr, sg, sb);
						}
	
						drawLine(x, y, x, y);
						ctr++;
					}
					last = p1;
				}
				if ( fontType == BITMASK_FONT ) {
					ctr += len;
				}
			}

			if ( glyphPrintMode == FILL_TEXT_BACKGROUND ) {
				setColor(bg);
				if ( vraster ) {
					fillRectangle(marginLeft + last + 1, yy, glyphWidth - marginLeft + xx - last - 1, glyphHeight + 1);
				} else {
					fillRectangle(xx, marginTop + last + 1, glyphWidth + 1, glyphHeight - marginTop + yy - last);
				}
			}
			
		} else {
			
			if ( clean ) {
				setColor(bg);
			}
			
			for ( int i = 0; i < length - 8; i++ ) {
				int b = 0xff & data[ptr + 8 + i];
				int x = i * 8 % effWidth;
				int y = i * 8 / effWidth;

				if ( glyphPrintMode == FILL_TEXT_BACKGROUND && prev != y ) {
					setColor(bg);
					if ( prev < 0 ) {
						fillRectangle(xx, yy, glyphWidth + 1, marginTop - yy + 1);
					} else {
						drawLine(xx, marginTop + y, xx + glyphWidth, marginTop + y);
					}
					if ( !clean ) {
						setColor(fg);
					}
					prev = y;
				}
				
				for ( int j = 0; j < 8; j++ ) {
					if ( x + j == effWidth ) {
						x = -j;
						y++;
						if ( glyphPrintMode == FILL_TEXT_BACKGROUND && prev != y ) {
							setColor(bg);
							drawLine(xx, marginTop + y, xx + glyphWidth, marginTop + y);
							if ( !clean ) {
								setColor(fg);
							}
							prev = y;
						}
					}
					int mask = 1 << (7 - j);
					if ( (b & mask) == 0 ) {
						drawLine(marginLeft + x + j, marginTop + y, marginLeft + x + j, marginTop + y);
					} 
				}
				last = y;
			}

			if ( glyphPrintMode == FILL_TEXT_BACKGROUND ) {
				setColor(bg);
				fillRectangle(xx, marginTop + last + 1, glyphWidth + 1, glyphHeight - marginTop + yy - last);
			}
		}

		setColor(fg);
	}

	protected void setRegion(final int x1, final int y1, final int x2, final int y2) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setXY("+x1+","+y1+","+x2+","+y2+")");
					setRegion(x1, y1, x2, y2);
				}
			});
			return;
		}

		rasterPtr = x1;
		Pixels.this.x1 = x1;
		Pixels.this.y1 = y1;
		Pixels.this.x2 = x2;
//		LCD.this.y2 = y2;
	}

	protected void setCurrentPixel(final RGB color) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setRegionPixel("+color.toString()+")");
					setCurrentPixel(color);
				}
			});
			return;
		}

		RGB sav = getColor();
		setColor(color);
		if ( x2 != rasterPtr + 1 ) {
			drawPixel(rasterPtr++, y1);
		} else {
			rasterPtr = x1;
			drawPixel(rasterPtr, y1++);
		}
		setColor(sav);
	}

	// UTIL

	private int ipart(double x) {
		return (int)x;
	}

	private int round(double x) {
		return (int)(x + 0.5);
	}

	private double fpart(double x) {
		return x - (double)ipart(x);
	}

	private double rfpart(double x) {
		return 1.0 - fpart(x);
	}
	
	private void putColor(int x, int y, boolean steep, double alpha) {
		
		aaCounter++;
		
		if ( steep ) {
	    	int tmp = x;
	    	x = y;
	    	y = tmp;
		}
		
		RGB result;
		if ( alpha != 1 ) {
			RGB bg = getPixel(x, y);
			result = computeColor(bg, alpha);
			RGB sav = getColor();
			setColor(result);
			drawPixel(x,y);
			setColor(sav);
		} else {
			drawPixel(x,y);
		}
	}

	private RGB computeColor(RGB bg, double alpha) {
		double max = 0.78;
		
		if ( alpha < 0 ) {
			alpha = 0;
		}
		if ( alpha > max ) {
			alpha = max;
		}
		int sr = (int)(bg.red * (max - alpha) + foreground.red * alpha);
		int sg = (int)(bg.green * (max - alpha) + foreground.green * alpha);
		int sb = (int)(bg.blue * (max - alpha) + foreground.blue * alpha);
		return new RGB(sr, sg, sb);
	}
	
	private int[] loadFileBytes( String filename ) throws Exception {
		File f = new File(DataLayerView.recentImageResourcePath, filename);
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
		
		
		byte[] b = fos.toByteArray();
		int[] result = new int[ b.length/2 ];
		
		for ( int i = 0; i < result.length; i++ ) {
			result[i] = (( b[i*2] & 0xff ) << 8) + ( b[i*2+1] & 0xff ); 
		}
		
		return result;
	}
	
	private String asRGB( Color c ) {
		String s = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
		return s;
	}
	
	// SWT infrastructure
	
	public void dispose() {
		atomicAction = 0;
		lastUpdate = 0;
		DataLayerView.stop = true;
		updateDevice(0, 0, DataLayerView.width, DataLayerView.height, true);
	}

	public void updateDevice(final int x, final int y, final int lwidth, final int lheight, final boolean updateView) {

		if ( Display.getCurrent() == null && display != null && !display.isDisposed() ) {
			display.syncExec(new Runnable() {
				public void run() {
					updateDevice(x, y, lwidth, lheight, updateView);
				}
			});
			return;
		}

		if ( atomicAction == 0 ) {
			try {
				Thread.sleep(deviceDelayMs, deviceDelayNs);
			} catch (InterruptedException e) {
			}
		}
		
		if ( atomicAction > 0 || updatePeriod != 0 && System.currentTimeMillis() - lastUpdate < updatePeriod  ) {
			return;
		}
		lastUpdate = System.currentTimeMillis();
		
		
		if ( display == null || display.isDisposed() ) {
			return;
		}
		
		if ( Application.guiHanderlsAllocationDebug ) {
			DeviceData info = display.getDeviceData();
			if (info.tracking) {
				Object[] objects = info.objects;
				Error[] newErrors = info.errors;
				
				int colors = 0, cursors = 0, fonts = 0, gcs = 0, images = 0, regions = 0;
				for (int i = 0; i < objects.length; i++) {
					Object object = objects[i];
					if (object instanceof Color)
						colors++;
					if (object instanceof Cursor)
						cursors++;
					if (object instanceof Font)
						fonts++;
					if (object instanceof GC)
						gcs++;
					if (object instanceof Image)
						images++;
					if (object instanceof Region)
						regions++;
				}
				System.out.println("o: " + objects.length + ", gcs: " + gcs + ", img: " + images);
			}
		}
		
		boolean disposeTmp = true;
		Image clone;
		
		int zoom = DeviceView.zoomFactor;
		if ( DataLayerView.deviceScroll != 0 ) {
			clone = new Image( display, DataLayerView.deviceWidth * zoom, DataLayerView.deviceHeight * zoom );
			GC gc = new GC(clone);
			if ( DataLayerView.deviceOrientation > LANDSCAPE ) {
				gc.drawImage(img, 0, DataLayerView.deviceScroll - DataLayerView.deviceHeight);
				gc.drawImage(img, 0, DataLayerView.deviceScroll);
			} else {
				gc.drawImage(img, 0, -DataLayerView.deviceScroll);
				gc.drawImage(img, 0, DataLayerView.deviceHeight - DataLayerView.deviceScroll);
			}
			gc.dispose();
		} else {
			if (zoom > 1) {
				ImageData id = img.getImageData();
				ImageData newData = new ImageData(DataLayerView.deviceWidth * zoom, DataLayerView.deviceHeight * zoom, id.depth, id.palette);

				clone = new Image( display, DataLayerView.deviceWidth * zoom, DataLayerView.deviceHeight * zoom );
				GC gc = new GC(clone);
				gc.drawLine(0, 0, DataLayerView.deviceWidth * zoom, DataLayerView.deviceHeight * zoom);
				
				Color cc = null;
				int prev = 0;
				for (int yy = 0; yy < DataLayerView.deviceHeight; yy++ ) {
					for (int xx = 0; xx < DataLayerView.deviceWidth; xx++ ) {
						
						int px = id.getPixel(xx, yy);
						
						for (int i = 0; i < zoom; i++) {
							for (int j = 0; j < zoom; j++) {
								newData.setPixel(xx*zoom + i, yy*zoom + j, px);
							}
						}
					}
				}
				clone = new Image(display,  newData);
				
				gc.dispose();				
			} else {				
				if ( DataLayerView.landscapeViewer ) {
					clone = img; // save one cloning operation
					disposeTmp = false;
				} else {
					clone = new Image( display, img.getImageData() );
				}
			}
		}		

		if ( DataLayerView.landscapeViewer ) {
			ImageData id = clone.getImageData();
			ImageData newData = new ImageData(DataLayerView.deviceHeight*zoom, DataLayerView.deviceWidth*zoom, id.depth, id.palette);

//			if ( DataLayerView.deviceOrientation == LANDSCAPE_FLIP ) {
//				for ( int i = 0; i < DataLayerView.deviceWidth; i++ ) {
//					for ( int j = 0; j < DataLayerView.deviceHeight; j++ ) {
//						newData.setPixel(DataLayerView.deviceHeight - 1 - j, i, id.getPixel(i, j));
//					}
//				}
//			} else {
				for ( int i = 0; i < DataLayerView.deviceWidth*zoom; i++ ) {
					for ( int j = 0; j < DataLayerView.deviceHeight*zoom; j++ ) {
						newData.setPixel(j, DataLayerView.deviceWidth*zoom - 1 - i, id.getPixel(i, j));
					}
				}
//			}
			
			if ( disposeTmp ) {
				clone.dispose();
			}
			clone = new Image(display,  newData);
		}
		
		DataLayerView.setDisplayImage(clone, x, y, lwidth, lheight, updateView);
		
//		gc = new GC(img);
	}

	
	private void delay(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}
	
	public void fontStatistics() {
		int counter = 1;
		Iterator<String> ii =  fontStats.keySet().iterator();
		while ( ii.hasNext() ) {
			String hash = ii.next();
			String result = "[";
			TreeSet<Character> set = fontStats.get(hash);
			System.out.println("font" + counter);
			Iterator<Character> jj = set.iterator();
			while( jj.hasNext() ) {
				Character c = jj.next();
				result += c.charValue();
			}
			result += "]";
			System.out.println(result);
			System.out.flush();
			counter++;
		}
		delay(2000);
	}
}
