package org.zefer.pixelmeister;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.services.IEvaluationService;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.compiler.CharSequenceJavaFileObject;
import org.zefer.pixelmeister.compiler.ClassFileManager;
import org.zefer.pixelmeister.util.CenteredFileDialog;
import org.zefer.pixelmeister.util.StatusPane;
import org.zefer.pixelmeister.util.Util;


public abstract class DataLayerView extends ViewPart {

	protected static final String SEPARATOR_PATTERN = "//------ SKETCH CODE DELIMITER ------";

	public static final String ORG_ZEFER_LCDPAINTER_DEVICEVIEW = "org.zefer.pixelmeister.deviceview";
	public static final String ORG_ZEFER_LCDPAINTER_RESOURCESVIEW = "org.zefer.pixelmeister.resourcesview";
	public static final String ORG_ZEFER_LCDPAINTER_CODEVIEW = "org.zefer.pixelmeister.codeeditview";

	private static final String LIB1_METHOD_SIGNATURES = "lib/utft.signatures";
	private static final String LIB2_METHOD_SIGNATURES = "lib/ufat.signatures";
	private static final String LIB3_METHOD_SIGNATURES = "lib/utext.signatures";
	private static final String LIB4_METHOD_SIGNATURES = "lib/pixels.signatures";

	protected static final String UTFT_INSTANCE_NAME = "utft";
	protected static final String PIXELS_INSTANCE_NAME = "pxs";
	protected static final String UTEXT_INSTANCE_NAME = "utext";
	
	protected static final String PIXELS_HINT =	"Instance name changed to '" + PIXELS_INSTANCE_NAME + "'\n\n" +
			"Example: " + PIXELS_INSTANCE_NAME + ".print(10, 10, \"Hello!\");\n" + 
			"Hint:    Type " + PIXELS_INSTANCE_NAME + ". and Ctrl-Space for the auto completetion pop-up\n";
			
	protected static final String UTEXT_HINT = "Instance name changed to '" + UTFT_INSTANCE_NAME + "'\n\n" +
			"Example: " + UTFT_INSTANCE_NAME + ".drawPixel(10, 10);\n" + 
			"Hint:    Type " + UTFT_INSTANCE_NAME + ". or " + UTEXT_INSTANCE_NAME + ". and Ctrl-Space for the auto completetion pop-up\n";
	
	protected static final String SD_INSTANCE_NAME = "tinyfat";

	public static final String DEVICE_LIST = "devices.ini";
	public static final String TEMPLATE_FILE_NAME = "blank_template.scq";
	public static final String UTEXT_TEMPLATE_FILE_NAME = "blank_template_utext.scq";
	public static final String EXPORT_TEMPLATE_FILE_NAME = "export_template.ino";
	public static final String EXPORT_PIXELS_TEMPLATE_FILE_NAME = "export_pixels_template.ino";
	
	protected static final String THREAD_PAINTER_PERFIX = "lcd_painter_runtime_";
	protected static final String THREAD_PRECOMP_PERFIX = "lcd_precompiler_runtime_";

	protected static final int RULER_H_WIDTH = 16;
	protected static final int RULER_V_WIDTH = 32;
	protected static final int RULER_SMALL_TICK = 10;
	protected static final int RULER_BIG_TICK = 50;

	public static final String FULL_DYNAMIC_CLASS_NAME = "DynaClass";
	public boolean errors;

	public static boolean exportToSetupSection;
	public static int exportTargetPlatform;
	public static String[] exportTargetPlatforms = {"Arduino"};
	
	public static final String STATUS_LINE_ID = "status.line.id";
	public static IStatusLineManager statusItem;
	public static StatusPane statusPane;
	public static StatusPane statusPane2;

	protected static JavaLineStyler globalsLineStyler = new JavaLineStyler();
	protected static JavaLineStyler sketchLineStyler = new JavaLineStyler();
	protected static StyledText sketchCode;
	protected static StyledText globalsCode;
	protected static String sketchInit;
	protected static String globalsInit;
	protected Button btnSlowdownWhile;


	private int posCount;
	private int posResCount;
	
	public static String recentImageResourcePath;
	protected static String recentFontResourcePath;
	public static String recentSketchPath;
	public static String currentSketch;
	public static String recentExportSketchPath;
	public static String currentExportSketch;

	protected static HashMap parsedArrays = new HashMap();
	protected static String parsedArraysInitCode = "";
	
	public static boolean stop = false;
	public static boolean autosave = false;
	public static boolean dumpSrc;
	public static boolean slowdownWhile = true;

	public static Object instance;
	
	public static Image displayImage;

	public static int deviceWidth = 320;
	public static int deviceHeight = 240;

	public static int width = 320;
	public static int height = 240;


	public static int rulerHeight = RULER_H_WIDTH; // initially rulers are on
	public static int rulerWidth = RULER_V_WIDTH;

	public static boolean repaintRuler;
	public static boolean suppressGrid;

	public static boolean ruler = true;
	public static boolean grid = false;
	protected static int gridStepPx = 50;
	public static boolean fixUpload = false;
	
	protected static boolean win = File.separatorChar == '\\';
//	protected static boolean landscape = true;
	
	protected static String controller;
	protected static String iface;
	protected static String codingHint;
	
	public static boolean dirty;

	protected static HashMap<String, String> utftSignatures;
	protected static HashMap<String, String> sdSignatures;
	protected static HashMap<String, String> utextSignatures;
	protected static HashMap<String, String> pixelsSignatures;

	private Image errorIcon;
	
	public static boolean lastChosenItalic;
	public static boolean lastChosenBold;
	public static String lastChosenFont;
	public static String fontTestString;

	public static int emulatingLibrary = 0;

	public static int deviceScroll = 0;
	public static int deviceOrientation;
	public static boolean landscapeViewer;

	protected static Canvas canvas;
	
	public static boolean oneBitColor;
	
	protected static LinkedList<String>[] undo;
	protected static LinkedList<String>[] redo;
	protected final static int UNDO_DEPTH = 500; 

	protected String proposalPrefix;
	protected HashMap<String,String> currentSignatures;
	
	protected static int findActionTarget = -1;
	protected static FindReplaceDialog findReplaceDialog;
	
	static {
		try {
			initMethodSignaturesMap();
		} catch (Exception e) {
			e.printStackTrace();
		}
	    undo = new LinkedList[2];
	    redo = new LinkedList[2];
	    undo[0] = new LinkedList<String>();
	    redo[0] = new LinkedList<String>();
	    undo[1] = new LinkedList<String>();
	    redo[1] = new LinkedList<String>();
	}

	public static void updateUndoMenu() {
		IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
		service.requestEvaluation("org.zefer.pixelmeister.UNDO");		
		service.requestEvaluation("org.zefer.pixelmeister.REDO");		
	}
	
	public static void addToUndo(int i, String text) {
		text = text.replaceFirst("\\s+$", "");
		if ( !undo[i].isEmpty() && undo[i].getLast().equals(text) ) {
			return;
		}
        undo[i].add(text);
        if ( undo[i].size() > UNDO_DEPTH ) {
        	undo[i].removeFirst();
        }
        redo[i].clear();
        updateUndoMenu();
	}

	
	public static int getActiveViewIndex() {
		int i = -1;
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null || PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null ) {
			return i;
		}
		IWorkbenchPartReference view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePartReference();
		if ( view == null ) {
			return i;
		}
		
		String id = view.getId();
		if ( "org.zefer.pixelmeister.codeeditview".equals(id) ) {
			i = 0;
		} else if ( "org.zefer.pixelmeister.resourcesview".equals(id) ) {
			i = 1;
		}
		
		return i;
	}
	
	public static boolean hasUndo() {
		int i = getActiveViewIndex();
		if ( i < 0 ) {
			return false;
		}
		return !undo[i].isEmpty();
	}
	
	public static boolean hasRedo() {
		int i = getActiveViewIndex();
		if ( i < 0 ) {
			return false;
		}
		return !redo[i].isEmpty();
	}

	public static String undo(int i, String text) {
		if ( undo[i].isEmpty() ) {
	        updateUndoMenu();
			return null;
		}
		text = text.replaceFirst("\\s+$", "");
		if ( !undo[i].isEmpty() && undo[i].getLast().equals(text) ) {
			undo[i].removeLast();
		}
		if ( undo[i].isEmpty() ) {
	        updateUndoMenu();
			return null;
		}
		String item = undo[i].removeLast();
		redo[i].add(item);
        updateUndoMenu();
		return item;
	}

	public static String redo(int i, String text) {
		if ( redo[i].isEmpty() ) {
	        updateUndoMenu();
			return null;
		}
		text = text.replaceFirst("\\s+$", "");
		if ( !redo[i].isEmpty() && redo[i].getLast().equals(text) ) {
			redo[i].removeLast();
		}
		if ( redo[i].isEmpty() ) {
	        updateUndoMenu();
			return null;
		}
		String item = redo[i].removeLast();
		undo[i].add(item);
        updateUndoMenu();
		return item;
	}
	
	public static void undo( boolean doRedo ) {
		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return;
		}

		int p = getActiveViewIndex();
		if ( p < 0 ) {
			return;
		}
		
		StyledText editor = sketchCode;
		if ( p == 1 ) {
			editor = globalsCode;
		}

		String current = editor.getText();
		String text = doRedo ? redo(p, current) : undo(p, current);
		
		if ( text != null ) {
			int ind = editor.getTopIndex();
			editor.setText(text);
			for ( int i = 0; i < current.length() && i < text.length(); i++ ) {
				if ( text.charAt(i) != current.charAt(i) ) {
					editor.setCaretOffset(i);
					editor.setTopIndex(ind);
					return;
				}
			}
			editor.setCaretOffset(Math.max(current.length()-1, text.length()-1));
			editor.setTopIndex(ind);
		}
	
	}
	
	public static void openFindReplaceDialog( String pattern, int target ) {
		if ( findReplaceDialog == null ) {
			findActionTarget = target;
			findReplaceDialog = new FindReplaceDialog(DataLayerView.getShell(), SWT.MODELESS);
			findReplaceDialog.open( pattern );
		} else {
			findReplaceDialog.setPattern( pattern );
		}
	}
	
	public static void closeFindReplaceDialog() {
		findReplaceDialog = null;
		findActionTarget = -1;
	}
	
	public static boolean hasSelectionScope() {
		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return false;
		}

		int p = findActionTarget;
		if ( p < 0 ) {
			return false;
		}
		
		StyledText editor = sketchCode;
		if ( p == 1 ) {
			editor = globalsCode;
		}
		
		Point pt = editor.getSelectionRange();
		if ( pt.y <= 0 ) {
			return false;
		}
		return true;
	}
	
	public static boolean findNext( String pattern, boolean ignoreCase, boolean backwards, boolean wrap, boolean inSelection ) {
		
		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return false;
		}

		if ( pattern == null || pattern.length() == 0 ) {
			return false;
		}
		
		int p = findActionTarget;
		if ( p < 0 ) {
			return false;
		}
		
		StyledText editor = sketchCode;
		if ( p == 1 ) {
			editor = globalsCode;
		}
		
		String current = editor.getText();
		if ( ignoreCase ) {
			pattern = pattern.toLowerCase();
			current = current.toLowerCase();
		}

		int scopeStart = 0;
		int scopeEnd = current.length();
		if ( inSelection ) {
			Point pt = editor.getSelectionRange();
			if ( pt.y <= 0 ) {
				return false;
			} else {
				scopeStart = pt.x;
				scopeEnd = pt.x + pt.y;
			}
		}
		
		if ( current.length() > 0 ) {
			
			if ( inSelection ) {
				current = current.substring(0, scopeEnd);
			}

			int caret = scopeStart > 0 ? (backwards ? scopeEnd : scopeStart) : editor.getCaretOffset();

			int pos = backwards ? current.substring(scopeStart, caret - pattern.length()).lastIndexOf(pattern) + scopeStart : current.indexOf(pattern, caret);
			
			if ( pos < scopeStart ) {
				if ( wrap ) {
					if ( backwards ) {
						pos = current.substring(caret, current.length()).lastIndexOf(pattern);
						if ( pos >= 0 ) {
							pos += caret;
						}
					} else {
						pos = current.indexOf(pattern, 0);
					}
					if ( pos < 0 ) {
						return false;
					}
				} else {
					return false;
				}
			}
			
//			int ind = editor.getTopIndex();
//			editor.setText(text);
//			for ( int i = 0; i < current.length() && i < text.length(); i++ ) {
//				if ( text.charAt(i) != current.charAt(i) ) {
//					editor.setCaretOffset(i);
//					editor.setTopIndex(ind);
//					return;
//				}
//			}
//			editor.setCaretOffset(Math.max(current.length()-1, text.length()-1));
//			editor.setTopIndex(ind);

//			if ( backwards ) {
//				editor.setSelection(pos, pos + pattern.length());
////				editor.setCaretOffset(pos);
//			} else {
////				editor.setCaretOffset(pos);
				editor.setSelection(pos, pos + pattern.length());
//			}
			
//			editor.setTopIndex(ind);
			
			return true;
		}
		
		return false;
	}
	
	public static boolean replace( String pattern, String data, boolean inSelected, boolean caseSensitive ) {

		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return false;
		}

		if ( pattern == null || pattern.length() == 0 ) {
			return false;
		}
		
		if ( data == null ) {
			data = "";
		}
		
		int p = findActionTarget;
		if ( p < 0 ) {
			return false;
		}
		
		StyledText editor = sketchCode;
		if ( p == 1 ) {
			editor = globalsCode;
		}
		
		int car = editor.getCaretOffset();
		if ( car <= 0 ) {
			return false;
		}

		String test = editor.getText(Math.max(0, car - pattern.length()), car-1);
		
		if ( caseSensitive && test.equals(pattern) || !caseSensitive && test.equalsIgnoreCase(pattern) ) {
			editor.replaceTextRange(car - pattern.length(), pattern.length(), data);
			editor.setSelectionRange(car - pattern.length(), data.length());
			editor.showSelection();
			return true;
		}

		return false;
	}
	
	public static int replaceAll( String pattern, String data, boolean inSelected, boolean caseSensitive ) {

		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return 0;
		}

		if ( pattern == null || pattern.length() == 0 ) {
			return 0;
		}
		
		if ( data == null ) {
			data = "";
		}
		
		int selStart = -1;
		int selLen = -1;
		
		int pt = findActionTarget;
		if ( pt < 0 ) {
			return 0;
		}
		
		StyledText editor = sketchCode;
		if ( pt == 1 ) {
			editor = globalsCode;
		}
		
		String text;
		if ( inSelected ) {
			Point p = editor.getSelectionRange();
			if ( p.y <= 0 ) {
				return 0;
			}
			selStart = p.x;
			selLen = p.y;
			text = editor.getText(selStart, selStart + selLen - 1);
			if ( text.length() == 0 ) {
				return 0;
			}
		} else {
			text = editor.getText();
		}
		
		String result = text;
		String input = text;
		if ( !caseSensitive ) {
			 input = text.toLowerCase();
			 pattern = pattern.toLowerCase();
		}
		
		int counter = 0;
		int ind = input.indexOf(pattern);
		while (ind != -1) {
			result = result.substring(0, ind) + data + result.substring(ind + pattern.length());
			if ( caseSensitive ) {
				input = result;
			} else {
				input = input.substring(0, ind) + data + input.substring(ind + pattern.length());
			}
			ind += data.length();
			ind = input.indexOf(pattern, ind);
			counter++;
		}
		
		if ( !result.equals(text) ) {
			if ( selLen > 0 ) {
				editor.replaceTextRange(selStart, selLen, result);
			} else {
				editor.setText(result);
			}
		}
		
		return counter;
	}

	public static boolean pointsToPattern( String pattern, boolean inSelected, boolean caseSensitive ) {

		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return false;
		}

		if ( pattern == null || pattern.length() == 0 ) {
			return false;
		}
		
		int p = findActionTarget;
		if ( p < 0 ) {
			return false;
		}
		
		StyledText editor = sketchCode;
		if ( p == 1 ) {
			editor = globalsCode;
		}
		
		int car = editor.getCaretOffset();
		if ( car <= 0 ) {
			return false;
		}

		String test = editor.getText(Math.max(0, car - pattern.length()), car-1);
		
		if ( caseSensitive && test.equals(pattern) || !caseSensitive && test.equalsIgnoreCase(pattern) ) {
			return true;
		}

		return false;
	}

	protected void processTab(VerifyEvent event, StyledText editor) {
		if ( event.keyCode == 9 ) {
			int selStart = -1;
			int selLen = -1;
			
			Point p = editor.getSelectionRange();
			
			if ( p.y <= 0 && (event.stateMask & SWT.SHIFT) != 0 ) {
				selStart = editor.getCaretOffset();
				selLen = 1;
			}

			if ( p.y > 0 ) {
				selStart = p.x;
				selLen = p.y;
			}

			if ( selLen > 0 ) {
				
				boolean toIndent = true;
				if ((event.stateMask & SWT.SHIFT) != 0 ) {
					toIndent = false;
				} else {
				}
				
				String text = editor.getText(selStart, selStart + selLen - 1);
				
				for ( int i = selStart - 1; i >= 0; i-- ) {
					String cs = editor.getText(i, i);
					if ( "\n".equals(cs) ) {
						break;
					}
					text = cs + text;
					selStart = i;
					selLen++;
				}
				
				int l = editor.getText().length();
				String cs = editor.getText(selStart+selLen-1, selStart+selLen-1);
				if ( !"\n".equals(cs) ) {
					for ( int i = selStart + selLen; i < l; i++ ) {
						cs = editor.getText(i, i);
						if ( "\n".equals(cs) ) {
							break;
						}
						selLen++;
						text += cs;
					}
				} else {
					selLen--;
				}
				
				text = editor.getText(selStart, selStart + selLen - 1);
										
				if (toIndent) {
					text = "\t" + text.replaceAll("\\n", "\n\t");
				} else {
					String res = "";
					boolean done = false;
					for( int i = 0; i < text.length(); i++ ) {
						char ch = text.charAt(i);
						
						if ( !done && (ch == '\t' || ch == ' ') ) {
							boolean tabs = true;
							int pos = i;
							i--;
							for( int j = pos; j < pos+4; j++ ) {
								char cx = text.charAt(j);
								if ( cx == ' ' ) {
									tabs = false;
									i++;
									continue;
								}
								if ( cx == '\t' && !tabs ) {
									res += ch;
									i++;
								}
								if ( cx == '\t' && tabs ) {
									i++;
								}
								break;
							}
							done = true;
							continue;
						} else {
							done = true;
						}

						if ( ch == '\n' ) {
							done = false;
						}
						
						res += ch;
					}
					text = res; 
				}
				
				addToUndo(editor == globalsCode ? 1 : 0, editor.getText());
				editor.replaceTextRange(selStart, selLen+1, text + '\n');
								
				editor.setSelectionRange(selStart, text.length());
				editor.showSelection();
				event.doit = false;
			}
			return;
		}
	}
	
	
//	public static IMenuManager menuManager;

	public String readResourceRawText() {
		if ( globalsCode == null || globalsCode.isDisposed() ) {
			return "";
		}
		return globalsCode.getText();
	}
	
	private String trim( String s, int start ) {
		return trim(s, start, s.length());
	}
	private String trim( String s, int start, int end ) {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < s.length(); i++ ) {
			if ( i >= start && i < end ) {
				sb.append(s.charAt(i));
			} else {
				sb.append(' ');
			}
		}
		return sb.toString();
	}
	
	public String readResourceText() {
		if ( globalsCode == null || globalsCode.isDisposed() ) {
			return "";
		}
		
		parsedArrays.clear();
		parsedArraysInitCode = "";
		
		String t = globalsCode.getText();
		
		StringBuffer res = new StringBuffer();

		// int[] img_a_png = {
		// int[] img_a_png = new int[] {
		
		StringBuffer arrayData = null;
		String arrayName = null;
		BufferedReader br = new BufferedReader(new StringReader(t)); 
		try {
			String line;
			while ((line = br.readLine()) != null) {
				
				int cm = line.indexOf("//");
				if ( cm >= 0 ) {
//					System.out.println("line before: [" + line + "]");
					line = trim(line, 0, cm);
//					System.out.println("line after:  [" + line + "]");
				}
				
				if ( arrayData != null ) {
					int ind1 = line.toLowerCase().indexOf("}");
					int ind2 = line.toLowerCase().indexOf(";");
					if ( ind1 >= 0 && ind2 > ind1 ) {
						arrayData.append( line.substring(0, ind1) );
						res.append( trim(line, 0, ind1) ).append('\n');
						
						try {
							processArray(arrayName, arrayData.toString());
						} catch (Exception e) {
							res.append(arrayData.toString());
							e.printStackTrace();
							System.err.println(e.getMessage());
						}
						
						arrayData = null;
					    res.append("};").append(trim(line, ind2+1));
					    res.append('\n');
					} else {
						arrayData.append( line ).append('\n');
						res.append( trim(line, 0) ).append('\n');
					}
				} else {
					int ind1 = line.toLowerCase().indexOf("int");
					int ind2 = line.toLowerCase().indexOf("]");
					int ind3 = line.toLowerCase().indexOf("=");
					int ind4 = line.toLowerCase().indexOf("{");
					int ind5 = line.toLowerCase().indexOf("}");
					int ind6 = line.toLowerCase().indexOf(";");
					int ind7 = line.toLowerCase().indexOf(")");
					int ind8 = line.toLowerCase().indexOf("(");
					if ( ind5 < 0 && ind6 < 0 && ind7 < 0 && ind8 < 0 && ind1 >= 0 && ind2 > ind1 && ind3 > ind2 && ind4 > ind3 ) {
//						System.out.println(line);
						res.append( trim(line, 0, ind4+1) );
						arrayName = line.substring(ind2+1, ind3).trim();
						arrayData = new StringBuffer();
						arrayData.append( line.substring(ind4+1) );
					} else {
						res.append(line);
					    res.append('\n');
					}
				}
			}
		} catch (IOException e) {
		}
		
		return res.toString();
	}

	private void processArray(String arrayName, String arrayData) throws Exception {
		StringTokenizer st = new StringTokenizer(arrayData, ",");
		int size = st.countTokens();
		int[] array = new int[size];
		int i = 0;
		while ( st.hasMoreTokens() ) {
			String token = st.nextToken().trim();
			if ( token.length() == 0 ) {
				if ( i == size - 1 ) {
					break;
				} else {
					throw new Exception( "invalid array (empty value #" + i + " of " + size + ")" );
				}
			}
			int value;
			if ( token.toLowerCase().startsWith( "0x" ) ) {
				value = Integer.parseInt(token.substring(2), 16);
			} else {
				try {
					value = Integer.parseInt(token);
				} catch (Exception e) {
					value = 0;
				}
			}
			if ( i > array.length - 1 ) {
				throw new Exception( "invalid array (number of values does not match)" );
			}
			array[i++] = value;
		}
		
		if ( i < array.length-1 ) {
			System.err.println("WARNING: number of array values does not match pre-counter");
		}
		
		parsedArrays.put(arrayName, array);
		parsedArraysInitCode += arrayName + " = (int[])parsedArrays.get(\"" + arrayName + "\");\n";
	}

	public static void updateDeviceView() {
		updateDeviceView(0, 0, width + rulerWidth, height + rulerHeight);
	}
	
	
	public static void updateDeviceView(final int x, final int y, final int width, final int height) {
		DeviceView device = null;
		if (PlatformUI.getWorkbench() == null || PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ||
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null ) {
			return;
		}
		
		IViewReference[] refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( ORG_ZEFER_LCDPAINTER_DEVICEVIEW.equals(refs[i].getId()) ) {
				device = (DeviceView)refs[i].getView(true);
				device.updateGui();
				device.redrawCanvas(x, y, width, height);
			}
		}
	}

	public static void appendGlobals(String text) {
		globalsCode.append('\n' + text);
		globalsCode.setTopIndex(globalsCode.getLineCount() - 1);
	}

	public static void displayError(String msg) {
		MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR);
		box.setMessage(msg);
		box.open();
	}

	public Image getTitleImage() {
	    return new Image(getSite().getShell().getDisplay(), new Rectangle(0, 0, 1, 1));
	}	
	
	public static int prompt(String msg) {
		MessageBox box = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		box.setMessage(msg);
		return box.open();
	}

	public static void updateStatusLine() {

		IWorkbenchPartSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .getActivePart().getSite();
		
		IActionBars actionBars;
		if (site instanceof IViewSite) {
			actionBars = ((IViewSite) site).getActionBars();
        } else if (site instanceof IEditorSite) {
        	actionBars = ((IEditorSite) site).getActionBars();
        } else {
        	return;
        }

		IStatusLineManager statusLine = actionBars.getStatusLineManager();
		if ( statusLine instanceof SubStatusLineManager ) {
			statusLine = (IStatusLineManager)((SubStatusLineManager)statusLine).getParent();
		}

//		IContributionItem[] items = statusLine.getItems();
//		for ( int i = 0; i < items.length; i++ ) {
//			if( items[i] instanceof StatusLineContributionItem && STATUS_LINE_ID.equals(items[i].getId()) ) {
//				((StatusLineContributionItem)items[i]).setText("Ready2");
//			}
//		}
		
		String message = "" +
		(autosave ? "  Autosave | " : ""); 
		// + (slowdownWhile ? "Slow while(1) | " : "");
		
		if ( message.length() > 3 ) {
			message = message.substring(0, message.length() - 3);
		}

        DataLayerView.statusPane2.setMessage(null, message, null);
		
		
//		statusItem.setMessage(message);
	}

	public static Shell getShell() {
		if ( globalsCode != null && !globalsCode.isDisposed() && globalsCode.isVisible() ) {
			return globalsCode.getShell();
		}
		if ( sketchCode != null && !sketchCode.isDisposed() && sketchCode.isVisible() ) {
			return sketchCode.getShell();
		}
		return PlatformUI.getWorkbench().getDisplay().getActiveShell();
	}
	
	public static void save(String name, boolean force) {
		
		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return;
		}

		if ( name == null ) {
			CenteredFileDialog saveSketchDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
			saveSketchDialog.dialog.setFilterExtensions(new String[] { "*.scq" });
			saveSketchDialog.dialog.setFilterNames(new String[] { "LCD Sketch (.SCQ)" });
			saveSketchDialog.dialog.setFilterPath(recentSketchPath);
			saveSketchDialog.dialog.setFileName(currentSketch);

			String f = (String)saveSketchDialog.open();
			if ( f != null ) {
				name = f;
				currentSketch = f;
				recentSketchPath = saveSketchDialog.dialog.getFilterPath();
			} else {
				return;
			}
		}
		
		File file = new File(name);
		if (file.exists() && !force) {
			String message = "File " + file.getAbsolutePath() + " already exists. Overwrite?";
			int res = prompt(message);
			if ( res == SWT.NO ) {
				return;
			}
		}

		try {
			FileOutputStream fos = new FileOutputStream(file);
			
			String str = sketchCode == null ? sketchInit : sketchCode.getText();
			fos.write(str.getBytes());
			fos.write(SEPARATOR_PATTERN.getBytes());
			str = globalsCode == null ? globalsInit : globalsCode.getText();
			fos.write(str.getBytes());
			fos.close();
			dirty = false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if ( file != null ) {
			getShell().setText("Pixelmeister - " + file.getAbsolutePath());
		}
	}
	
	
	public static void loadSketch(final String name, final boolean blank) {

		String sketchString;
		String globalsString = "";

		if ((name == null) || (name.length() == 0))
			return;

		final File file = new File(name);
		if (!file.exists()) {
			String message = "Cannot open recent file: " + file.getAbsolutePath();
			displayError(message);
			return;
		}

		try {
			FileInputStream stream = new FileInputStream(file.getPath());
			try {
				BufferedReader in = new BufferedReader(new InputStreamReader(stream));
				StringBuffer buffer = new StringBuffer((int) file.length());
				String line;
				while ((line = in.readLine()) != null) {
					buffer.append(line);
					buffer.append('\n');
				}
				sketchString = buffer.toString();
				stream.close();
			} catch (IOException e) {
				// Err_file_io
				String message = "File read error" + file.getName();
				displayError(message);
				return;
			}
		} catch (FileNotFoundException e) {
			String message = "File not found" + file.getName();
			displayError(message);
			return;
		}
		
		int pos = sketchString.indexOf(SEPARATOR_PATTERN);
		if ( pos >= 0 ) {
			globalsString = sketchString.substring( pos + SEPARATOR_PATTERN.length() );
			sketchString = sketchString.substring(0, pos);
		}
		
		HashSet instanceNames = emulatingLibrary == 0 ?
				findInstanceNames(sketchString, pixelsSignatures) : 
				findInstanceNames(sketchString, utftSignatures); // XXX
		
		if ( instanceNames.size() > 0 ) {
			String nx = (String)instanceNames.iterator().next();

			switch ( emulatingLibrary ) {
			case 0:
				if ( !PIXELS_INSTANCE_NAME.equals(nx) ) {
					sketchString = sketchString.replaceAll(nx, PIXELS_INSTANCE_NAME);
				}
				break;
			case 1:
				if ( !UTFT_INSTANCE_NAME.equals(nx) ) {
					sketchString = sketchString.replaceAll(nx, UTFT_INSTANCE_NAME);
				}
				break;
			}
		}
		
		final String finalSketch = sketchString;
		final String finalGlobals = globalsString;
		
//		if ( getShell() == null ) {
//			return;
//		}
		
		// Guard against superfluous mouse move events -- defer action until
		// later
		Display display = getShell().getDisplay();
		display.asyncExec(new Runnable() {
			public void run() {
				
//				IViewReference[] refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
//				for ( int i = 0; i < refs.length; i++ ) {
//					if ( ORG_ZEFER_LCDPAINTER_RESOURCESVIEW.equals(refs[i].getId()) ) {
//						ResourcesView device = (ResourcesView)refs[i].getView(true);
//						globalsCode.setText(finalGlobals);
//					}
//					if ( ORG_ZEFER_LCDPAINTER_CODEVIEW.equals(refs[i].getId()) ) {
//						CodeEditView device = (CodeEditView)refs[i].getView(true);
//						globalsCode.setText(finalGlobals);
//					}
//				}
				
				if ( sketchCode != null && !sketchCode.isDisposed() ) {
					sketchCode.setText(finalSketch);
				} else {
					sketchInit = finalSketch;
				}
				if ( globalsCode != null && !globalsCode.isDisposed() ) {
					globalsCode.setText(finalGlobals);
				} else {
					globalsInit = finalGlobals;
				}
				DataLayerView.dirty = false;

				if ( blank ) {
					getShell().setText("Pixelmeister");
				} else if ( name != null ) {
					getShell().setText("Pixelmeister - " + file.getAbsolutePath());
				}
			}
		});
		

		// parse the block comments up front since block comments can go across
		// lines - inefficient way of doing this
		sketchLineStyler.parseBlockComments(sketchString);
	}

	protected static HashSet findInstanceNames(String sketchString, HashMap signatures) {
		HashSet instanceNames = new HashSet();
		Iterator ii = signatures.keySet().iterator();
		while ( ii.hasNext() ) {
			String method = (String)ii.next();
			method = method.substring(0, method.indexOf('('));
			int ind = sketchString.indexOf(method);
			boolean dot = false;
			boolean id = false;
			String nx = "";
			for ( int i = ind-1; i >= 0; i-- ) {
				char c = sketchString.charAt(i);
				if ( Character.isWhitespace(c) ) {
					if ( !id ) {
						continue;
					} else {
						break;
					}
				}
				if ( c == '.' ) {
					if (!dot) {
						dot = true;
						continue;
					} else {
						break;
					}
				}
				if ( Character.isLetterOrDigit(c) ) {
					if ( !dot ) {
						break;
					}
					id = true;
					nx = c + nx;
				}
			}
			
			if (nx.length()>0 && !nx.equals("err") && !nx.equals("out") && !nx.equals("utext") && !nx.equals("utft") ) {
				instanceNames.add(nx);
//				System.out.println( nx+"."+method+"()");
			}
		}
		return instanceNames;
	}

	
	@SuppressWarnings("deprecation")
	public static void drawGrid(GC gc, int deviceWidth, int deviceHeight, int step, boolean ruler) {

		if ( suppressGrid ) {
			return;
		}
		
		int rulerX = 0;
		int rulerY = 0;
		if ( ruler ) {
			rulerX = RULER_V_WIDTH;
			rulerY = RULER_H_WIDTH;
		}

	    boolean scrollVertical = deviceHeight > deviceWidth;
	    
		if ( win ) {
			gc.setXORMode(true);
		} else {
			gc.setLineStyle(SWT.LINE_DOT);
		}
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));

	    // draw vertical lines
		if ( deviceScroll != 0 && !scrollVertical) {
            int scroll = deviceScroll % DataLayerView.deviceWidth;
            if ( scroll < 0 ) {
            	scroll += DataLayerView.deviceWidth;
            }
	        for (int i = 0; i < DataLayerView.deviceWidth; i++) {
	            int pos = (i + scroll) % DataLayerView.deviceWidth;
            	if (i % step == 0 && pos != 0) {
                	gc.drawLine(pos + rulerX, rulerY, pos + rulerX, rulerY + deviceHeight);
            		if ( !win ) {
            			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
                    	gc.drawLine(pos + rulerX, rulerY+3, pos + rulerX, rulerY + deviceHeight);
            			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
            		}
	            }
	        }
		} else {
	        for (int i = step; i < deviceWidth; i += step) {
	            gc.drawLine(i + rulerX, rulerY, i + rulerX, rulerY + deviceHeight);
        		if ( !win ) {
        			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
                	gc.drawLine(i + rulerX, rulerY+3, i + rulerX, rulerY + deviceHeight);
        			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        		}
	        }
		}

	    // draw horizontal lines
		if ( deviceScroll != 0 && scrollVertical ) {
            int scroll = deviceScroll % DataLayerView.height;
            if ( scroll < 0 ) {
            	scroll += DataLayerView.height;
            }
	        for (int i = 0; i < DataLayerView.height; i++) {
	            int pos = (i + scroll) % DataLayerView.height;
	            if (i % step == 0 && pos != 0) {
					gc.drawLine(rulerX, pos + rulerY, rulerX + deviceWidth, pos + rulerY);
	        		if ( !win ) {
	        			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
	                	gc.drawLine(rulerX+3, pos + rulerY, rulerX + deviceWidth, pos + rulerY);
	        			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
	        		}
	            }
	        	
	        }
		} else {
	        for (int i = step; i < deviceHeight; i += step) {
	            gc.drawLine(rulerX, i + rulerY, rulerX + deviceWidth, i + rulerY);
        		if ( !win ) {
        			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
                	gc.drawLine(rulerX+3, i + rulerY, rulerX + deviceWidth, i + rulerY);
        			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
        		}
	        }
		}
	}
	
	public static void setDisplayImage(Image newImage, int x, int y, int width, int height, boolean updateView) {

		if (oneBitColor) {
			ImageData id = newImage.getImageData();
			for(int i = 0; i < id.width; i++){
		        for(int j = 0; j < id.height; j++){
					int px = id.getPixel(i, j);
					int red = (px & 0xFF0000) >> 16; 
		        	int green = (px & 0x00FF00) >> 8; 
					int blue = (px & 0x0000FF); 
					boolean col = red > 127 || green > 127 || blue > 127;
		            id.setPixel(i, j, !col ? 0x00000000 : 0xFFFFFFFF);
		        }
		    }
			newImage.dispose();  
			if (getShell() != null) { // null may happen by a quitting of Pixelmeister, while a sketch runs
				newImage = new Image( getShell().getDisplay(), id );
			}
		}
		
		Image di = displayImage; 
		displayImage = newImage;
		if ( updateView ) {
			updateDeviceView(rulerWidth + x, rulerHeight + y, width, height);
		}
		if ( di != null && !di.isDisposed() ) {
			di.dispose();
		}
	}
	
	public static void render() {
		if ( displayImage == null ) {
			return;
		}
		
		if (oneBitColor) {
			ImageData id = displayImage.getImageData();
			for ( int i = 0; i < id.width; i++ ) {
		        for ( int j = 0; j < id.height; j++ ) {
					int px = id.getPixel(i, j);
					int red = (px & 0xFF0000) >> 16; 
		        	int green = (px & 0x00FF00) >> 8; 
					int blue = (px & 0x0000FF); 
					boolean col = red > 127 || green > 127 || blue > 127;
		            id.setPixel(i, j, !col ? 0x00000000 : 0xFFFFFFFF);
		        }
		    }
			displayImage.dispose();
			displayImage = new Image( getShell().getDisplay(), id );
		}

		updateDeviceView(rulerWidth + 0, rulerHeight + 0, width, height);
	}
	
	public static void initMethodSignaturesMap() throws Exception {
        utftSignatures = new HashMap<String, String>();
        String url = LIB1_METHOD_SIGNATURES;
        
		URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/" + url), null);
		URL file = FileLocator.toFileURL(codeBase);
	    InputStream rcReader = new FileInputStream(file.getPath());
	    
	    BufferedReader br = new BufferedReader(new InputStreamReader(rcReader));
	    String line;
	    try {
			while( (line = br.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				if ( st.countTokens() != 2 ) {
					continue;
				}
				utftSignatures.put( st.nextToken(), st.nextToken() );
			}
		    br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        sdSignatures = new HashMap<String, String>();
        url = LIB2_METHOD_SIGNATURES;
        
		codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/" + url), null);
		file = FileLocator.toFileURL(codeBase);
	    rcReader = new FileInputStream(file.getPath());
	    
	    br = new BufferedReader(new InputStreamReader(rcReader));
	    try {
			while( (line = br.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				if ( st.countTokens() != 2 ) {
					continue;
				}
				sdSignatures.put( st.nextToken(), st.nextToken() );
			}
		    br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        utextSignatures = new HashMap<String, String>();
        url = LIB3_METHOD_SIGNATURES;
        
		codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/" + url), null);
		file = FileLocator.toFileURL(codeBase);
	    rcReader = new FileInputStream(file.getPath());
	    
	    br = new BufferedReader(new InputStreamReader(rcReader));
	    try {
			while( (line = br.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				if ( st.countTokens() != 2 ) {
					continue;
				}
				utextSignatures.put( st.nextToken(), st.nextToken() );
			}
		    br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

        pixelsSignatures = new HashMap<String, String>();
        url = LIB4_METHOD_SIGNATURES;
        
		codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/" + url), null);
		file = FileLocator.toFileURL(codeBase);
	    rcReader = new FileInputStream(file.getPath());
	    
	    br = new BufferedReader(new InputStreamReader(rcReader));
	    try {
			while( (line = br.readLine()) != null ) {
				StringTokenizer st = new StringTokenizer(line, "\t");
				if ( st.countTokens() != 2 ) {
					continue;
				}
				pixelsSignatures.put( st.nextToken(), st.nextToken() );
			}
		    br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String getEditorText() {
		if ( sketchCode.isDisposed() ) {
			return "";
		}
		
		String t = sketchCode.getText();

		final boolean visibility = t.indexOf("while") >= 0;
		getShell().getDisplay().syncExec(new Runnable() {
			public void run() {
				if ( btnSlowdownWhile != null && !btnSlowdownWhile.isDisposed() ) {
					btnSlowdownWhile.setVisible(visibility);
				}
			}
		});
		
		if ( !slowdownWhile ) {
			return t;
		}
		
		StringBuffer res = new StringBuffer();
		BufferedReader br = new BufferedReader(new StringReader(t)); 
		try {
			String line;
			while ((line = br.readLine()) != null) {
				int ind = line.toLowerCase().indexOf("while");
				if ( ind >= 0 ) {
					int ind2 = line.indexOf("{");
					int ind3 = line.indexOf(";");
					if ( ind2 < 0 && ind3 < 0 ) {
						line = "while( slowWhile() )";
					} else {
						int i = 0;
						if ( ind2 < 0 && ind3 > 0 ) {
							i = ind3;
						} else if ( ind2 > 0 && ind3 < 0 ) {
							i = ind2;
						} else if ( ind2 > 0 && ind3 > 0 ) {
							i = Math.min(ind2, ind3);
						}						
						
						line = line.substring(0, ind) + "while( slowWhile() )" + line.substring(i); 
					}
				} 
			    res.append(line).append('\n');
			}
		} catch (IOException e) {
		}

		return res.toString();
	}

	protected String getParsedArraysInitCode() {
		IViewReference[] refs = getSite().getPage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( ORG_ZEFER_LCDPAINTER_RESOURCESVIEW.equals(refs[i].getId()) ) {
				refs[i].getView(true);
				return DataLayerView.parsedArraysInitCode;
			}
		}
		return "";
	}

	protected String preprocess( String src ) {
		
		StringBuffer result = new StringBuffer();
		
		BufferedReader br = new BufferedReader(new StringReader(src)); 
		try {
			String line;
			boolean exclusion = false;
			boolean ppsection = false;
			while ((line = br.readLine()) != null) {
				
				String l = line.trim();
				
				if ( l.length() > 0 && l.charAt(0) == '#' ) {
					if ( l.startsWith("#ifdef") ) {
						String key = l.substring(6).trim();
						if ( "PIXELMEISTER".equals(key) ) {
							ppsection = true;
							result.append(convertToBlanks(line)).append('\n');
							continue;
						}
						if ( "EXPORT_TO_SETUP_METHOD".equals(key) ) {
							ppsection = true;
							result.append(convertToBlanks(line)).append('\n');
							continue;
						}
					} else if (l.startsWith("#ifndef")) {
						String key = l.substring(7).trim();
						if ( "PIXELMEISTER".equals(key) ) {
							ppsection = true;
							exclusion = true;
							result.append(convertToBlanks(line)).append('\n');
							continue;
						}
					} else if (l.startsWith("#else")) {
						if ( ppsection ) {
							exclusion = !exclusion;
							result.append(convertToBlanks(line)).append('\n');
							continue;
						}
					} else if (l.startsWith("#endif")) {
						if ( ppsection ) {
							exclusion = false;
							ppsection = false;
							result.append(convertToBlanks(line)).append('\n');
							continue;
						}
					}
				} 

				if ( exclusion ) {
					result.append(convertToBlanks(line)).append('\n');
				} else {

					
					int ind = -1;
					if ( line.length() > 0 && (ind = line.indexOf("virtual")) >= 0  ) {
						if ( ind == 0 || ind > 0 && Character.isWhitespace(line.charAt(ind-1)) ) {
							if ( ind + 7 == line.length() || ind + 7 < line.length() && Character.isWhitespace(line.charAt(ind + 7)) ) {
								line = line.replaceFirst("virtual", "       ");	
							}
						}
					}

					if ( line.length() > 0 && line.trim().indexOf("class") == 0  ) {
						line = Util.replaceString(line, "public", "      ");	
						line = Util.replaceString(line, ":", " extends ");	
					}
					
					if ( line.length() > 0 && line.trim().indexOf("const") == 0  ) {
						line = Util.replaceString(line, "const", "final");	
					}
					
					if ( line.length() > 0 && line.trim().indexOf("int*") == 0  ) {
						line = line.replaceFirst("int\\*", "int[]");	
					}
					
					if ( line.length() > 0 && line.trim().indexOf("*") > 0 && line.trim().indexOf("=") < 0  ) {
						line = updateDeclaration(line);	
					}
					
					line = Util.replaceString(line, "prog_uchar*", "int[]      ");	
					line = Util.replaceString(line, "prog_uchar", "int       ");	
					line = Util.replaceString(line, "RGB*", "RGB ");	
					line = Util.replaceString(line, "->", ". ");	
					line = Util.replaceString(line, "public:", "       ");	
					line = Util.replaceString(line, "private:", "        ");	
					line = Util.replaceString(line, "protected:", "         ");	

					result.append(line).append('\n');
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return src;
		}
		return result.toString(); 
	}

	private String updateDeclaration(String line) {
		String l = line.trim();
		if ( !Character.isUpperCase(l.charAt(0)) ) {
			return line;
		}
		if ( l.charAt(l.length()-1) != ';') {
			return line;
		}
		l = l.substring(0, l.length()-1).trim();
		l = l.replaceFirst("\\*", " ");
		StringTokenizer st = new StringTokenizer(l, " \t");
		if ( st.countTokens() != 2 ) {
			return line;
		}
		while ( st.hasMoreTokens() ) {
			String t = st.nextToken().trim();
			for ( int i = 0; i < t.length(); i++ ) {
				if ( i == 0 && !Character.isLetter(t.charAt(i)) || !Character.isJavaIdentifierPart(t.charAt(i)) ) {
					return line;
				}
			}
		}
		return l + ";";
	}

	public static final String convertToBlanks( String line ) {
		byte[] res = line.getBytes();
		for ( int i = 0; i < res.length; i++ ) {
			res[i] = 0x20;
		}
		return new String(res);
	}
	
	protected String buildJavaClass( int library ) {

        if ( library == 0 ) {
        	StringBuilder src = new StringBuilder();
        	src.append("import org.eclipse.swt.graphics.RGB;\n");
        	src.append("import org.zefer.pixelmeister.device.ScriptBase;\n");
            src.append("import org.zefer.pixelmeister.device.pixels.Pixels;\n");
        
            src.append("public class " + FULL_DYNAMIC_CLASS_NAME + " extends ScriptBase { \n");
        
            posResCount = src.toString().length();
        
        	src.append( readResourceText() );

        	src.append( '\n' );
        	src.append("  private Pixels ").append(PIXELS_INSTANCE_NAME).append(";\n");

            src.append("  public void run() { \n");
            src.append( getParsedArraysInitCode() );
            
            src.append("    ").append(PIXELS_INSTANCE_NAME).append(" = new Pixels( display, canvas, width, height );\n");

            posCount = src.toString().length();

            src.append( getEditorText() );

            src.append(PIXELS_INSTANCE_NAME).append(".dispose();\n");
            src.append("    stopScript();\n");
            src.append("  }\n");

            src.append("    	protected void _refresh() {\n");
            src.append("    		").append(PIXELS_INSTANCE_NAME).append(".lastUpdate = 0;\n");
            src.append("    		").append(PIXELS_INSTANCE_NAME).append(".updateDevice(0, 0, width, height, true);\n");
            src.append("    	}\n");
            
            src.append("}\n");
            
//            System.out.println(src.toString());
            return src.toString();

        } else {

        	StringBuilder src = new StringBuilder();
        	src.append("import org.zefer.pixelmeister.device.ScriptBase;\n");
        
            src.append("import org.zefer.pixelmeister.device.itdb02.LCD;\n");
            src.append("import org.zefer.pixelmeister.device.itdb02.uText;\n");
            src.append("import org.zefer.pixelmeister.device.itdb02.tinyFAT;\n");
        
            src.append("public class " + FULL_DYNAMIC_CLASS_NAME + " extends ScriptBase { \n");
        
            posResCount = src.toString().length();
        
        	src.append( readResourceText() );

        	src.append( '\n' );
        	src.append("    private LCD ").append(UTFT_INSTANCE_NAME).append(";\n");
        	src.append("    private uText ").append(UTEXT_INSTANCE_NAME).append(";\n");
        	src.append("    private tinyFAT ").append(SD_INSTANCE_NAME).append(";\n");

            src.append("  public void run() { \n");
            
            src.append( getParsedArraysInitCode() );
            
            src.append("    ").append(UTFT_INSTANCE_NAME).append(" = new LCD( display, canvas, width, height );\n");
            src.append("    ").append(UTEXT_INSTANCE_NAME).append(" = new uText( "+UTFT_INSTANCE_NAME+", display, width, height );\n");
            src.append("    ").append(SD_INSTANCE_NAME).append(" = new tinyFAT();\n");

            posCount = src.toString().length();

            src.append( getEditorText() );

            src.append(UTFT_INSTANCE_NAME).append(".dispose();\n");
            src.append("    stopScript();\n");
            src.append("  }\n");

            src.append("    	protected void _refresh() {\n");
            src.append("    		").append(UTFT_INSTANCE_NAME).append(".lastUpdate = 0;\n");
            src.append("    		").append(UTFT_INSTANCE_NAME).append(".updateDeviceView(0, 0, width, height);\n");
            src.append("    	}\n");
            
            src.append("}\n");

            return src.toString();
        }
	}


	protected void preCompile() {

		final String src = preprocess( buildJavaClass( emulatingLibrary ) );
		
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        final Display display = getShell().getDisplay();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if ( compiler == null ) {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR);
			mb.setMessage("ERROR: Obviously the application runs with JRE instead of required JDK 1.6 (or above)   ");
			mb.open();
			return;
		}
        
        terminatePrecompilerThreads();
        
		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(1200);
				} catch (InterruptedException e) {
				}

	    		display.syncExec(new Runnable() {
	    			public void run() {
	    		        Object iii = compileJava(src, diagnostics);
	    		        if ( iii == null ) {
	    		        	stop = true;
	    		        	deviceScroll = 0;
	    		        	parseErrors(src, diagnostics);
	    		        } else {
	    		        	((JavaLineStyler)sketchLineStyler).resetErrors();
	    		        	((JavaLineStyler)globalsLineStyler).resetErrors();
	    		            DataLayerView.statusPane.setMessage(null, "Ready", null);
	    					DataLayerView.updateStatusLine();
	    		        }
	    				parseEditorContent();
	    			}
	    		});
			}
		};
		t.setName(THREAD_PRECOMP_PERFIX + System.currentTimeMillis());
		t.start();
	}
	
	private void parseErrors( String src, DiagnosticCollector<JavaFileObject> diagnostics ) {

		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return;
		}

		int lineCount = 0;
		for ( int i = 0; i < posCount && i < src.length(); i++ ) {
			if ( src.charAt(i) == '\n' ) {
				lineCount++;
			}
		}
		int lineResCount = 0;
		for ( int i = 0; i < posResCount && i < src.length(); i++ ) {
			if ( src.charAt(i) == '\n' ) {
				lineResCount++;
			}
		}

    	((JavaLineStyler)sketchLineStyler).resetErrors();
    	((JavaLineStyler)globalsLineStyler).resetErrors();
		
		String errorMessage = null;
        if (diagnostics.getDiagnostics().size() > 0 ) {

        	int counter = 0;
        	errorMessage = "";
        	Iterator<?> ii = diagnostics.getDiagnostics().iterator();
            while (ii.hasNext()){
            	Diagnostic<?> diagnostic = (Diagnostic<?>)ii.next();
            	counter++;
            	
            	JavaLineStyler styler = sketchLineStyler;
            	StyledText textEditor = sketchCode;
            	int lCount = lineCount;
            	String locationPrefix = "";
            	
            	int ln = (int)diagnostic.getLineNumber();
            	
            	if ( ln - lineCount - 1 < 0 ) {
            		styler = globalsLineStyler;
            		textEditor = globalsCode;
            		lCount = lineResCount;
            		locationPrefix = "resources: ";
            	}
            	
            	int lineNr = ln - lCount - 1;
            	
            	int columnNumber = (int)diagnostic.getColumnNumber() - 1;
            	if ( lineNr >= textEditor.getLineCount() ) {
            		lineNr = textEditor.getLineCount() - 1;
            		columnNumber = textEditor.getLine(lineNr).length() - 1;
            	}

//            	if ( textEditor.getLineCount() <= lineNr || lineNr < 0 ) {
//            		System.out.println("??? " + textEditor.getLineCount() + " vs. " + lineNr );
//            	} else 
            	if ( lineNr >= 0 && textEditor.getLine(lineNr).length() == 0 ) {
            		lineNr--;
            	}

            	if ( lineNr < 0 ) {
                	System.out.println(diagnostic.toString());
            		continue;
            	}
            	
            	String line = textEditor.getLine(lineNr);
            	int ptr = 0;
            	for ( int ctr = 0; ctr < columnNumber && ptr < line.length(); ptr++, ctr++ ) {
            		char c = line.charAt(ptr);
            		if ( c == '\t' ) {
            			ctr += 8 - (ctr % 8);
            		}
            	}
            	columnNumber = ptr;
            	
            	if ( columnNumber >= textEditor.getLine(lineNr).length() ) {
            		columnNumber = textEditor.getLine(lineNr).length() - 1;
            	}
            	
        		int startPos = textEditor.getOffsetAtLine(lineNr) + columnNumber;
            	long len = 0;
        		Util.Range rg = Util.getWord(line, columnNumber, diagnostic.getMessage(null).indexOf("';'") >= 0 );
        		startPos = textEditor.getOffsetAtLine(lineNr) + rg.from;
        		len = rg.len;
            	
            	((JavaLineStyler)styler).addError( startPos, (int)len, diagnostic.getMessage(null) );

            	String m = diagnostic.getMessage(null);
            	if ( m != null ) {
            		int ind = m.indexOf("java:");
            		if ( ind + 5 < m.length() ) {
                		ind = m.indexOf(":", ind + 5);
                		if ( ind > 0 ) {
                			m = m.substring(ind);
                		}
            		}
            	}
            	
            	if ( errorMessage.length() > 0 ) {
            		errorMessage += "\n";
            	}
            	errorMessage += locationPrefix + (diagnostic.getLineNumber() - lCount) + m;
            }
            
            if ( errorIcon == null ) {
            	errorIcon = Util.getImageRegistry(getShell().getDisplay()).get("error");
            }
            DataLayerView.statusPane.setMessage(errorIcon, counter + " compilation error" + (counter != 1 ? "s" : "") + " (click for report)", errorMessage);
			DataLayerView.updateStatusLine();
        }
	}
	
	public Object compileJava(String src, DiagnosticCollector<JavaFileObject> diagnostics) {

		Object instance = null;
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if ( compiler == null ) {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR);
			mb.setMessage("ERROR: Obviously the application runs with JRE instead of required JDK 1.6 (or above)   ");
			mb.open();
			return null;
		}
		
		JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));

        java.util.List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
        jfiles.add(new CharSequenceJavaFileObject(FULL_DYNAMIC_CLASS_NAME, src));
//        jfiles.add(new CharSequenceJavaFileObject(FULL_DYNAMIC_CLASS_NAME + "$Console", src));

		List<String> optionList = new ArrayList<String>();
        try {
	        URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/"), null);
			URL classesUrl = FileLocator.toFileURL(codeBase);
			
	        URL libBase = FileLocator.find(Platform.getBundle("org.eclipse.swt."+
	        			Platform.getWS()+"."+Platform.getOS()+"."+Platform.getOSArch()), new Path("/"), null);  //swt.WS.OS.ARCH_version.jar
			URL libUrl = FileLocator.toFileURL(libBase);

			String libUrlPath = libUrl.getPath();
			String classpathUrlPath = classesUrl.getPath();
			
			if (libUrlPath.startsWith("/") && libUrlPath.charAt(2) == ':') {
				libUrlPath = libUrlPath.substring(1);
			}

		if (classpathUrlPath.startsWith("/") && classpathUrlPath.charAt(2) == ':') {
			classpathUrlPath = classpathUrlPath.substring(1);
		}

			optionList.addAll(Arrays.asList("-classpath", libUrlPath + File.pathSeparatorChar +
														  classpathUrlPath + File.pathSeparatorChar +
	        		                                      classpathUrlPath + "/bin"));
//	        System.out.println(Arrays.asList("-classpath", libUrlPath + File.pathSeparatorChar +
//	        		classpathUrlPath + File.pathSeparatorChar +
//                    classpathUrlPath + "/bin"));
        } catch (IOException e2) {
			e2.printStackTrace();
		}
        
        boolean success = compiler.getTask(null, fileManager, diagnostics, optionList, null, jfiles).call();
		errors = !success;
        if ( success ) {
    		try {
    			instance = fileManager.getClassLoader(null).loadClass(FULL_DYNAMIC_CLASS_NAME).newInstance();
    			
    		} catch (NullPointerException e1) {
    			StackTraceElement[] ss = e1.getStackTrace();
    			System.out.println("NullPointerException");
    			if ( ss != null && ss.length > 0 ) {
        			System.out.println("\tat " + ss[0]);
        			if ( ss.length > 1 ) {
            			System.out.println("\tat " + ss[1]);
        			}
    			}
    		} catch (Exception e1) {
    			e1.printStackTrace();
    		}
        } else {
        	instance = null;
        }
		try {
			fileManager.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return instance;
	}

	protected void parseEditorContent() {
		if (globalsCode == null || sketchCode == null || globalsCode.isDisposed() || sketchCode.isDisposed() ) {
			return;
		}
		((JavaLineStyler)sketchLineStyler).parseBlockComments(sketchCode.getText());
		sketchCode.redraw();
		((JavaLineStyler)globalsLineStyler).parseBlockComments(globalsCode.getText());
		globalsCode.redraw();
	}

	public static void terminatePrecompilerThreads() {
		ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
		ThreadGroup parentGroup;
		while ( ( parentGroup = rootGroup.getParent() ) != null ) {
		    rootGroup = parentGroup;
		}
		goGroup(rootGroup, THREAD_PRECOMP_PERFIX);
	}

	public static void terminateThreads() {
		ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
		ThreadGroup parentGroup;
		while ( ( parentGroup = rootGroup.getParent() ) != null ) {
		    rootGroup = parentGroup;
		}
		goGroup(rootGroup, THREAD_PAINTER_PERFIX);
	}
	
	@SuppressWarnings("deprecation")
	protected static void goThread(Thread t, String prefix) {
		if (t == null) {
			return;
		}
		if ( t.getName().startsWith(prefix) ) {
			t.stop();
//			System.out.println( t.getName() + " thread interrupted.");
		}
	}
	  
	protected static void goGroup(ThreadGroup g, String prefix) {
		if (g == null) {
			return;
		}
		int numThreads = g.activeCount();
		int numGroups = g.activeGroupCount();
		Thread[] threads = new Thread[numThreads];
		ThreadGroup[] groups = new ThreadGroup[numGroups];

		g.enumerate(threads, false);
		g.enumerate(groups, false);
		
//		System.out.println("threads: " + threads.length + ", groups: " + groups.length);

		for (int i = 0; i < numThreads; i++) {
			goThread(threads[i], prefix);
		}
		for (int i = 0; i < numGroups; i++) {
			goGroup(groups[i], prefix);
		}
	}
	
	protected void showProposal( final Composite parent, StyledText editor, final Shell popupShell, final Table table, int step, char c ) {
		
		String mainLibInstanceName = UTFT_INSTANCE_NAME;
		String subLibInstanceName = UTEXT_INSTANCE_NAME;
		String utilLibInstanceName = SD_INSTANCE_NAME;
		
		HashMap mainLibSigns = utftSignatures;
		HashMap subLibSigns = utextSignatures;
		HashMap utilLibSigns = sdSignatures;
		
		if ( emulatingLibrary == 0 ) {
			mainLibInstanceName = PIXELS_INSTANCE_NAME;
			subLibInstanceName = "nosuchlib";
			utilLibInstanceName = "nosuchlib";
			
			mainLibSigns = pixelsSignatures;
		}
		
		final Display display = parent.getDisplay();
		String string = editor.getText();
		if ( c == SWT.BS ) { // backspace
			step = -1;
		}
		
		int curpos = editor.getCaretOffset() + step;
		if ( c >= ' ' ) {
			step = 1;
		}
		
		String currentInstanceName = "";
		currentSignatures = null;
		
		proposalPrefix = string.substring(0, curpos) + (c < 32 ? "" : ""+c);
		int beg1 = proposalPrefix.lastIndexOf(mainLibInstanceName);
		int beg2 = proposalPrefix.lastIndexOf(subLibInstanceName);
		int beg3 = proposalPrefix.lastIndexOf(utilLibInstanceName);
		if ( beg1 < 0 && beg2 < 0 && beg3 < 0 ) {
			popupShell.setVisible(false);
			return;
		}

		int beg;
		if ( beg1 < beg2 && beg3 < beg2 ) {
			currentInstanceName = subLibInstanceName;
			currentSignatures = subLibSigns;
			beg = beg2;
		} else if ( beg2 < beg1 && beg3 < beg1 ) {
			currentInstanceName = mainLibInstanceName;
			currentSignatures = mainLibSigns;
			beg = beg1;
		} else {
			currentInstanceName = utilLibInstanceName;
			currentSignatures = utilLibSigns;
			beg = beg3;
		}
		
		proposalPrefix = proposalPrefix.substring(beg + currentInstanceName.length());
		boolean founddot = false;
		int i;
		for ( i = 0; i < proposalPrefix.length(); i++ ) {
			char ch = proposalPrefix.charAt(i);
			if ( ch == '.' ) {
				founddot = true;
			}
			if ( ch != '.' && ch != ' ' && ch != '\t' && ch != '\n' && ch != '\r' ) {
				break;
			}
		}
		if ( i != 0 ) {
			proposalPrefix = proposalPrefix.substring(i);
		}
		
		if ( !founddot ) {
			popupShell.setVisible(false);
			return;
		}
		
		for ( i = 0; i < proposalPrefix.length(); i++ ) {
			char ch = proposalPrefix.charAt(i);
			if ( !Character.isJavaIdentifierStart(ch) ) {
				popupShell.setVisible(false);
				return;
			}
		}

//		System.out.println("buf [" + proposalPrefix + "]"); // XXX
		Caret car = editor.getCaret();

		table.removeAll();
		
		boolean matchFound = false;
        TreeSet<String> keys = new TreeSet<String>(currentSignatures.keySet());
        for (String sign : keys) { 
//           String value = pd4mlSignatures.get(sign);
           if ( sign.toLowerCase().startsWith(proposalPrefix.toLowerCase()) ) {
        	   TableItem ti = new TableItem(table, SWT.NONE);
        	   ti.setText(sign);
        	   table.setSelection(0);
        	   matchFound = true;
           }
        }        
        
        if (!matchFound) {
			popupShell.setVisible(false);
			return;
        }

        Rectangle caretBounds = car.getBounds();
		Rectangle editorBounds = display.map(parent, null, editor.getBounds());
		popupShell.setBounds(editorBounds.x + caretBounds.x + step * 8, editorBounds.y + caretBounds.y + caretBounds.height, editorBounds.width + 150, 150);
		popupShell.setVisible(true);
	}
	
//	private void showErrorIcon( boolean success, String message ) {
//		if ( success ) {
//			infoIcon.setImage(null);
//		} else {
//			infoIcon.setImage(Util.getImageRegistry(infoIcon.getDisplay()).get("error"));
//		}
//		infoIcon.setToolTipText(message);
//	}

}
