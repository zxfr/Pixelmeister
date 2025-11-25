package org.zefer.pixelmeister.device.itdb02;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.DataLayerView;
import org.zefer.pixelmeister.DeviceView;

public class LCD {
	
	public final static int LEFT = 0;
	public final static int RIGHT = 9999;
	public final static int CENTER = 9998;
	
	private Display display;
	private int width;
	private int height;
	private int x1;
	private int y1;
	private int x2;
//	private int y2;
	
	private Image img;
	private GC gc;
	
	public long lastUpdate;
	private long updatePeriod = 1;
	
	public int atomicAction;
	
	private Color bgColor;
	private Color fgColor;

	private long pixelCounter = 0;
	public int pixelsPerUpdate = 10;
	
	boolean debug = false;
	public void debug( String msg ) {
		if (debug) {
			System.out.println(msg);
		}
	}
	
	private int deviceDelayMs = 0;
	private int deviceDelayNs = 1;
	
	public static class CurrentFont {
		int x_size;
		int y_size;
		int offset;
		int numchars;
		int[] font;
	};
	private CurrentFont cfont = new CurrentFont();
	
	public LCD ( final Display display, final Canvas canvas, final int width, final int height ) {
		this.display = display;
		this.width = width;
		this.height = height;
		this.img = new Image( display, width, height );
		display.syncExec(new Runnable() {
			public void run() {
				debug("new LCD()");
				gc = new GC(img);

				fgColor = SWTResourceManager.getColor(SWT.COLOR_GRAY);
				
				gc.setBackground(fgColor);
				gc.fillRectangle(0, 0, width, height);

				fgColor = new Color(display, new RGB(0xa0, 0xa0, 0xa0));
				gc.setForeground(fgColor);
				for ( int i = 0; i < width; i+=2 ) {
					gc.drawLine(i+1, 0, i+1, height);
				}
				
				fgColor = SWTResourceManager.getColor(255,255,255);
				bgColor = SWTResourceManager.getColor(0,0,0);
			}
		});
	}

	public void InitLCD() {
	}

	public void InitLCD(int orientation){
	}

	public void clrScr() {

		pixelCounter = 0;

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					clrScr();
				}
			});
			return;
		}
		
		fgColor = SWTResourceManager.getColor(255,255,255); 
		bgColor = SWTResourceManager.getColor(0,0,0);
		gc.setBackground(bgColor);
		gc.setForeground(fgColor);
		gc.fillRectangle(0, 0, width, height);
		updateDeviceView(0, 0, width, height);
	}
	
	public void dispose() {
		atomicAction = 0;
		lastUpdate = 0;
		DataLayerView.stop = true;
		updateDeviceView(0, 0, width, height);
	}

	public void updateDeviceView(final int x, final int y, final int lwidth, final int lheight) {

		if ( Display.getCurrent() == null && display != null && !display.isDisposed() ) {
			display.syncExec(new Runnable() {
				public void run() {
					updateDeviceView(x, y, lwidth, lheight);
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
		
		Image clone;

		if ( DataLayerView.deviceScroll != 0 ) {
			clone = new Image( display, width * DeviceView.zoomFactor, height * DeviceView.zoomFactor );
			GC gc = new GC(clone);
			gc.drawImage(img, 0, 0, width, height, DataLayerView.deviceScroll, 0, width * DeviceView.zoomFactor, height * DeviceView.zoomFactor);
			gc.dispose();
		} else {
			clone = new Image( display, img.getImageData() );
		}
		
		DataLayerView.setDisplayImage(clone, x, y, width * DeviceView.zoomFactor, height * DeviceView.zoomFactor, true);
	}

	private String asRGB(Color color) {
		return ""; // " rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
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

		gc.drawPoint(x, y);
		if ( (pixelCounter % pixelsPerUpdate) == 0 ) {
			updateDeviceView(x, y, 1, 1);
		}
		pixelCounter++;
	}
	
	public void drawPixel(final double x, final double y) {
		drawPixel((int)x, (int)y);
	}
	
	public void fillRect(final int x1, final int y1, final int x2, final int y2) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillRect("+x1+","+y1+","+x2+","+y2+")" + asRGB(bgColor));
					fillRect(x1, y1, x2, y2);
				}
			});
			return;
		}
		
		final int xx1 = x1 > x2 ? x2 : x1;
		final int xx2 = x1 > x2 ? x1 : x2;
		final int yy1 = y1 > y2 ? y2 : y1;
		final int yy2 = y1 > y2 ? y1 : y2;

		gc.setBackground(fgColor);
		gc.fillRectangle(xx1, yy1, xx2 - xx1, yy2 - yy1);
		gc.setBackground(bgColor);
		updateDeviceView(xx1, yy1, xx2 - xx1, yy2 - yy1);
	}
	
	public void setColor(final int r, final int g, final int b) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setColor("+r+","+g+","+b+")");
					setColor(r, g, b);
				}
			});
			return;
		}

		if ( fgColor != null ) {
			if ( fgColor.getRed() == r && fgColor.getGreen() == g && fgColor.getBlue() == b ) {
				return;
			}
		}

		fgColor = SWTResourceManager.getColor(r, g, b);
		gc.setForeground(fgColor);
	}
	
	public void setBackColor(final int r, final int g, final int b) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setBackColor("+r+","+g+","+b+")");
					setBackColor(r, g, b);
				}
			});
			return;
		}

		if ( bgColor != null ) {
			if ( bgColor.getRed() == r && bgColor.getGreen() == g && bgColor.getBlue() == b ) {
				return;
			}
		}
		
		bgColor = SWTResourceManager.getColor(r, g, b);
		if (DataLayerView.stop) {
			return;
		}

		gc.setBackground(bgColor);
	}

	public void setFont(final int[] fontBytes) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setFont(...)");
					setFont(fontBytes);
				}
			});
			return;
		}

		cfont = new CurrentFont();
		cfont.x_size = fontBytes[0];
		cfont.y_size = fontBytes[1];
		cfont.offset = fontBytes[2];
		cfont.numchars = fontBytes[3];
		cfont.font = fontBytes;
	}

	public void fillScr(final int r, final int g, final int b) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillScr("+r+","+g+","+b+")" + asRGB(bgColor));
					fillScr(r, g, b);
				}
			});
			return;
		}

		fillRect(0, 0, width, height);
		updateDeviceView(0, 0, width, height);
	}
	
	public void drawLine(double x1, double y1, double x2, double y2) {
		drawLine((int)x1, (int)y1, (int)x2, (int)y2);
	}
	
	public void drawLine(final int x1, final int y1, final int x2, final int y2) {

		pixelCounter = 0;

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

		final int xx1 = x1 > x2 ? x2 : x1;
		final int xx2 = x1 > x2 ? x1 : x2;
		final int yy1 = y1 > y2 ? y2 : y1;
		final int yy2 = y1 > y2 ? y1 : y2;

		gc.drawLine(x1, y1, x2, y2);
		updateDeviceView(xx1, yy1, xx2-xx1, yy2-yy1);
	}
	
	public void drawRect(final int x1, final int y1, final int x2, final int y2) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawRect("+x1+","+y1+","+x2+","+y2+")");
					drawRect(x1, y1, x2, y2);
				}
			});
			return;
		}

		final int xx1 = x1 > x2 ? x2 : x1;
		final int xx2 = x1 > x2 ? x1 : x2;
		final int yy1 = y1 > y2 ? y2 : y1;
		final int yy2 = y1 > y2 ? y1 : y2;

		gc.drawRectangle(xx1, yy1, xx2-xx1, yy2-yy1);
		updateDeviceView(xx1, yy1, xx2-xx1, yy2-yy1);
	}
	
	public void drawRoundRect(final int x1, final int y1, final int x2, final int y2) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawRoundRect("+x1+","+y1+","+x2+","+y2+")");
					drawRoundRect(x1, y1, x2, y2);
				}
			});
			return;
		}

		atomicAction++;
		
		final int xx1 = x1 > x2 ? x2 : x1;
		final int xx2 = x1 > x2 ? x1 : x2;
		final int yy1 = y1 > y2 ? y2 : y1;
		final int yy2 = y1 > y2 ? y1 : y2;
		
		if ((xx2-xx1)>4 && (yy2-yy1)>4) {
			drawPixel(xx1+1, yy1+1);
			drawPixel(xx2-1, yy1+1);
			drawPixel(xx1+1, yy2-1);
			drawPixel(xx2-1, yy2-1);
			
			drawLine(xx1+2, yy1, xx2-2, yy1);
			drawLine(xx1+2, yy2, xx2-2, yy2);
			drawLine(xx1, yy1+2, xx1, yy2-2);
			drawLine(xx2, yy1+2, xx2, yy2-2);
		}
		atomicAction--;
		updateDeviceView(xx1, yy1, xx2-xx1, yy2-yy1);
	}
	
	public void fillRoundRect(final int x1, final int y1, final int x2, final int y2) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillRoundRect("+x1+","+y1+","+x2+","+y2+")");
					fillRoundRect(x1, y1, x2, y2);
				}
			});
			return;
		}

		final int xx1 = x1 > x2 ? x2 : x1;
		final int xx2 = x1 > x2 ? x1 : x2;
		final int yy1 = y1 > y2 ? y2 : y1;
		final int yy2 = y1 > y2 ? y1 : y2;

		atomicAction++;

		if ((xx2-xx1)>4 && (yy2-yy1)>4) {
			for (int i=0; i<((yy2-yy1)/2)+1; i++) {
				switch(i) {
				case 0:
					drawLine(xx1+2, yy1+i, xx2-2, yy1+i);
					drawLine(xx1+2, yy2-i, xx2-2, yy2-i);
					break;
				case 1:
					drawLine(xx1+1, yy1+i, xx2-1, yy1+i);
					drawLine(xx1+1, yy2-i, xx2-1, yy2-i);
					break;
				default:
					drawLine(xx1, yy1+i, xx2, yy1+i);
					drawLine(xx1, yy2-i, xx2, yy2-i);
				}
			}
		}
		atomicAction--;
		updateDeviceView(xx1, yy1, xx2-xx1, yy2-yy1);
	}
	
	public void drawCircle(final int x, final int y, final int radius) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawCircle("+x+","+y+","+radius+")");
					drawCircle(x, y, radius);
				}
			});
			return;
		}

		gc.drawOval(x - radius, y - radius, radius * 2, radius * 2);
		updateDeviceView(x - radius, y - radius, radius * 2, radius * 2);
	}

	public void fillCircle(final int x, final int y, final int radius) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("fillCircle("+x+","+y+","+radius+")");
					fillCircle(x, y, radius);
				}
			});
			return;
		}

		gc.setBackground(fgColor);
		gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
		gc.setBackground(bgColor);
		updateDeviceView(x - radius, y - radius, radius * 2, radius * 2);
	}

	protected void setXY(final int x1, final int y1, final int x2, final int y2) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setXY("+x1+","+y1+","+x2+","+y2+")");
					setXY(x1, y1, x2, y2);
				}
			});
			return;
		}

		LCD.this.x1 = x1;
		LCD.this.y1 = y1;
		LCD.this.x2 = x2;
//		LCD.this.y2 = y2;
	}

	protected void setPixel(final Color color) {

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("setPixel("+color.toString()+")");
					setPixel(color);
				}
			});
			return;
		}

		Color c = gc.getForeground();
		gc.setForeground(color);
		if ( x2 != x1 ) {
			drawPixel(x1++, y1);
			updateDeviceView(x1-1, y1, 1, 1);
		} else {
			drawPixel(x1, y1++);
			updateDeviceView(x1, y1-1, 1, 1);
		}
		gc.setForeground(c);
	}
	
	public void printChar(final CurrentFont cfont, final char c, final int x, final int y) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("printChar(...,"+c+","+x+","+y+")");
					printChar(cfont, c, x, y);
				}
			});
			return;
		}

		atomicAction++;
		int temp = ((c-cfont.offset)*((cfont.x_size/8)*cfont.y_size)) + 4;

		for(int j = 0; j < ((cfont.x_size/8) * cfont.y_size); j += (cfont.x_size/8)) {
			
			setXY(x, y+(j/(cfont.x_size/8)), x+cfont.x_size-1, y+(j/(cfont.x_size/8)));
			
			for (int zz = 0; zz < (cfont.x_size/8); zz++) {
				try {
					byte ch = (byte)cfont.font[temp+zz]; // XXX

					for(int i = 0; i < 8; i++) {   
						if(( ch & (1<<(7-i))) != 0) {
							setPixel(fgColor);
						} else {
							setPixel(bgColor);
						}   
					}
					
				} catch (ArrayIndexOutOfBoundsException e) {
					System.err.println( "utft.print(): cannot print '" + c + "' with current font (do not be uText.print() call here?)" );
					break;
				}
			}
			temp+=(cfont.x_size/8);
		}
		atomicAction--;
		updateDeviceView(x, y, x+cfont.x_size-1, y+cfont.y_size-1);
	}

	public void rotateChar(final CurrentFont cfont, final char c, final int x, final int y, final int pos, final int deg) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("roteteChar(...,"+c+","+x+","+y+"...)");
					rotateChar(cfont, c, x, y, pos, deg);
				}
			});
			return;
		}
		
		final double radian = deg * 0.0175;  

		atomicAction++;
		int temp=((c-cfont.offset)*((cfont.x_size/8)*cfont.y_size))+4;
		for(int j = 0; j < cfont.y_size; j++)	{
			for (int zz = 0; zz < (cfont.x_size/8); zz++) {
				byte ch = (byte)cfont.font[temp+zz]; 
				for(int i = 0; i < 8; i++) {   
					int newx = (int)(x + (((i+(zz*8)+(pos*cfont.x_size))* Math.cos(radian))-((j) * Math.sin(radian))));
					int newy = (int)(y + (((j) * Math.cos(radian))+((i+(zz*8)+(pos*cfont.x_size)) * Math.sin(radian))));

					setXY(newx, newy, newx+1, newy+1);
					
					if((ch&(1<<(7-i)))!=0) {
						setPixel(gc.getForeground());
					} else {
						setPixel(gc.getBackground());
					}   
				}
			}
			temp+=(cfont.x_size/8);
		}
		atomicAction--;
		updateDeviceView(0, 0, width, height);
	}

	public void print(String st, int x, int y) {
		print(st, x, y, 0);
	}

	public void print(final String st, final int x, final int y, final int deg) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("print("+st+","+x+","+y+","+deg+")");
					print(st, x, y, deg);
				}
			});
			return;
		}

		
		atomicAction++;
				
		final int stl = st.length();
		final CurrentFont font = cfont;
		
		if (DataLayerView.stop) {
			return;
		}

		int x1 = x;
		if (width < height) {
			if (x == RIGHT) {
				x1 = 240-(stl*font.x_size);
			}
			if (x == CENTER) {
				x1=(240-(stl*font.x_size))/2;
			}
		} else {
			if (x == RIGHT) {
				x1 = 320-(stl*font.x_size);
			}
			if (x == CENTER) {
				x1 = (320-(stl*font.x_size))/2;
			}
		}

		int ptr = 0;
		for (int i = 0; i < stl; i++) {
			if (deg == 0) {
				printChar(font, st.charAt(ptr++), x1 + (i*(font.x_size)), y);
			} else {
				rotateChar(font, st.charAt(ptr++), x1, y, i, deg);
			}
		}

		atomicAction--;
		updateDeviceView(0, 0, width, height); // XXX
	}

	public void printNumI(long num, int x, int y) {
		print("" + num, x, y);
	}

	public void printNumF(double num, byte dec, int x, int y) {
		String str = String.format( "%7f." + dec, num );
		print(str, x, y);
	}

	public void drawBitmap(int x, int y, int sx, int sy, int[] data)	{
		drawBitmap(x, y, sx, sy, data, 1);
	}
	
	public void drawBitmap(final int x, final int y, final int sx, final int sy, final int[] data, final int scale)	{

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawBitmap("+x+","+y+","+sx+","+sy+",...,"+scale+")");
					drawBitmap(x, y, sx, sy, data, scale);
				}
			});
			return;
		}

		atomicAction++;
		if (scale == 1) {
			if (DataLayerView.stop) {
				return;
			}
			int ptr = 0;
			for ( int j = 0; j < sy; j++ ) {
				for ( int i = 0; i < sx; i++ ) {
					int p;
					try {
						p = data[ptr++];
					} catch (Exception e1) {
						String ddd = (data == null ? "null" : "int[" + data.length + "]" );
						System.out.println( "utft.drawBitmap("+x+", "+y+", "+sx+", "+sy+", "+ddd+", "+scale+"): provided binary data does not match specified image size. Expected: int["+(sx*sy)+"]" );
						atomicAction--;
						return;
					}
					int r = ((0xf800 & p)>>11) * 255 / 31;
					int g = ((0x7e0 & p)>>5) * 255 / 63;
					int b = (0x1f & p) * 255 / 31;
		
					try {
						RGB rgb = new RGB(r, g, b);
						Color cx = new Color(display, rgb);
						gc.setForeground(cx);
						gc.drawPoint(x + i, y + j);
					} catch (Exception e) {
						System.err.println(r + ", " + g + ", " + b);
						e.printStackTrace();
						break;
					}
				}
			}
		} else {
			for (int ty = 0; ty < sy; ty++) {
				for (int tsy = 0; tsy < scale; tsy++) {
					setXY(x, y+(ty*scale)+tsy, x+((sx*scale)-1), y+(ty*scale)+tsy);
					for (int tx = sx; tx >= 0; tx--) {
//XXX						int col = data[(ty*sx)+tx];
						for (int tsx = 0; tsx < scale; tsx++) {
//XXX								LCD_Write_DATA(col>>8,col & 0xff);
						}
					}
				}
			}
		}
		atomicAction--;
		updateDeviceView(0, 0, width, height);
	}

	public void drawBitmap(final int x, final int y, final int sx, final int sy, final int[] data, final int deg, final int rox, final int roy) {

		pixelCounter = 0;

		if (DataLayerView.stop || display == null || display.isDisposed()) {
			return;
		}

		if ( Display.getCurrent() == null ) {
			display.syncExec(new Runnable() {
				public void run() {
					debug("drawBitmap("+x+","+y+","+sx+","+sy+",...,"+deg+","+rox+","+roy+")");
					drawBitmap(x, y, sx, sy, data, deg, rox, roy);
				}
			});
			return;
		}

		atomicAction++;

		final double radian = deg*0.0175;  

		if ( deg==0 ) {
			drawBitmap(x, y, sx, sy, data);
		} else {
			for (int ty = 0; ty < sy; ty++) {
				for (int tx = 0; tx < sx; tx++) {
//					int col = data[(ty*sx)+tx];

					int newx = (int)(x+rox+(((tx-rox) * Math.cos(radian)) - ((ty-roy) * Math.sin(radian))));
					int newy = (int)(y+roy+(((ty-roy) * Math.cos(radian)) + ((tx-rox) * Math.sin(radian))));

					setXY(newx, newy, newx, newy);
// XXX				LCD_Write_DATA(col>>8,col & 0xff);
				}
			}
		}

		atomicAction--;
		updateDeviceView(0, 0, width, height);
	}

	public byte loadBitmap(final int x, final int y, final int sx, final int sy, final String filename) {

		pixelCounter = 0;

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
		updateDeviceView(0, 0, width, height);
		return 0;
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
}
