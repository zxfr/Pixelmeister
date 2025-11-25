package org.zefer.pixelmeister;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.fonts.RasterGlyph;
import org.zefer.pixelmeister.util.CenteredFileDialog;
import org.zefer.pixelmeister.util.ImageChecker;
import org.zefer.pixelmeister.util.TrueType;
import org.zefer.pixelmeister.util.Util;



public class ImportIconDialog extends Dialog {

	private static final String DIALOG_SETTINGS = "import-clipart-icon";

	private static final String CLIPART_URL = "http://pd4ml.com/cliparts.htm";
	
	protected Object result;
	protected Shell shell;
	
	private SashForm sash;
	
	private Composite leftPane;
	private Composite glyphPicker;
	private Composite clipartNavigation;

	private String imagePath;
	private Image originalImage;
	private int imgWidth;
	private int imgHeight;

	private int currentWidth;
	private int currentHeight;

	private Label lblImage;
	private ScrolledComposite previewPane;
	private Button btnDeviceBounds;
	private Button btnMonochrome;
	private Label lblExpectedImageSize;
	private Label lblType;
	private Label lblResolution;
	private Label lblFontSize;
	private Combo fontSizeCombo;
	
	private Label lblWhite;
	private Scale hiedgeSlider;
	
	private boolean monochrome;
	private int edge = 0;
	private int edgeMax = 10;

	private boolean initShowBounds;
	private int initFontSize = 14;
	
	private int boundX = 0;
	private int boundY = 0;
	private int weight1 = 25;
	private int weight2 = 75;
	
	private byte[] actualBytes;
	private int actualWidth = 0;
	private Label lblHint;
	
	private String imageType = "UNKNOWN";
	private Button btnSaveAsRaw;
	private Button btnAddToResources;

	private Label lblImageName;

	private Button btnCancel;

	private boolean win;

	private java.awt.Font awtFont;
	private Table glyphsTable;
	private String chosenGlyph;
	
	private TreeViewer tv;
	private Label lblRootPath;
	private Combo lblRootPathCombo;

	private ArrayList<String> recentRoots = new ArrayList<String>();
	
	private String root = System.getProperty("user.dir");
	private String lastFolder;
	
	private File selectedFile;
	
	private int width = 700;
	private int height = 450;
	
	
	private TrueType tt; // = new TrueType(path);

	protected Cursor cursorWait;
	protected Cursor cursorDefault;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ImportIconDialog(Shell parent) {
		super(parent);

		cursorWait = new Cursor(parent.getDisplay(), SWT.CURSOR_WAIT);
		cursorDefault = new Cursor(parent.getDisplay(), SWT.CURSOR_ARROW);
		
		parent.setCursor(cursorWait);
		
		win = File.separatorChar == '\\';

		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DataLayerView.ORG_ZEFER_LCDPAINTER_RESOURCESVIEW);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {

		loadState();
	    
		createContents();

        Rectangle shellBounds = getParent().getBounds();
        Point dialogSize = shell.getSize();

        shell.setLocation(
        		shellBounds.x + (shellBounds.width - dialogSize.x) / 2,
        		shellBounds.y + (shellBounds.height - dialogSize.y) / 2);
        
		shell.open();
		shell.layout();
		
		shell.setCursor(cursorDefault);
		
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}
	
	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.PRIMARY_MODAL);
		shell.setMinimumSize(new Point(600, 450));
		shell.setSize(width, height);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		Image icon = Util.getImageRegistry(shell.getDisplay()).get("pixelmeister");
		shell.setImage(icon);
		
		shell.addShellListener(new ShellListener() {
			public void shellClosed(ShellEvent arg0) {
				saveState();
			}

			public void shellDeactivated(ShellEvent arg0) {}
			public void shellDeiconified(ShellEvent arg0) {}
			public void shellIconified(ShellEvent arg0) {}
			public void shellActivated(ShellEvent event) {}
		});
		
		sash = new SashForm(shell, SWT.HORIZONTAL);
		FormData fd_sash = new FormData();
		fd_sash.bottom = new FormAttachment(100, -10);
		fd_sash.right = new FormAttachment(100, -10);
		fd_sash.top = new FormAttachment(0, 10);
		fd_sash.left = new FormAttachment(0, 10);
		sash.setLayoutData(fd_sash);

		leftPane = new Composite(sash, SWT.NONE);
		leftPane.setLayout(new FormLayout());
		FormData fd_leftPane = new FormData();
		fd_leftPane.bottom = new FormAttachment(100, -10);
		fd_leftPane.right = new FormAttachment(100, 0);
		fd_leftPane.top = new FormAttachment(0, 10);
		fd_leftPane.left = new FormAttachment(0, 0);
		leftPane.setLayoutData(fd_leftPane);

		lblRootPath = new Label(leftPane, SWT.NONE);
		FormData fd_lblRootPath = new FormData();
		fd_lblRootPath.left = new FormAttachment(0, 4);
		fd_lblRootPath.top = new FormAttachment(0, 15);
		fd_lblRootPath.width = 46;
		lblRootPath.setLayoutData(fd_lblRootPath);
		lblRootPath.setText("Clipart:" );

		Button chooseFontButton = new Button(leftPane, SWT.FLAT);
		chooseFontButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				CenteredFileDialog dlg = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
				dlg.dialog.setFilterExtensions(new String[] { "*.ttf;*.otf" });
				dlg.dialog.setFilterNames(new String[] { "True Type Font (TTF, OTF)" });
				//				dlg.dialog.setFilterPath(DataLayerView.recentFontResourcePath);

				dlg.dialog.setFilterPath(root);
				dlg.dialog.setText("Open iconic font");

				String f = dlg.dialog.open();
				if ( f == null ) {
					return;
				}
				
				root = f;

				int count = lblRootPathCombo.getItemCount();
				if ( count > 9 ) {
					for ( int i = count - 1; i >= 8; i++ ) {
						lblRootPathCombo.remove(i);
					}
				}

				boolean alreadyThere = false;
				count = lblRootPathCombo.getItemCount();
				for ( int i = 0; i < count; i++ ) {
					if ( f.equals(lblRootPathCombo.getItem(i)) ) {
						lblRootPathCombo.select(i);
						alreadyThere = true;
						break;
					}
				}

				if ( !alreadyThere ) {
					lblRootPathCombo.add( f, 0 ); 
					lblRootPathCombo.select(0);
				}

				updatePicker(true);

				if ( tv != null ) {
					tv.setInput(new File(root));
				}
			}
		});
		FormData fd_chooseFontButton = new FormData();
		fd_chooseFontButton.top = new FormAttachment(0, 10);
		fd_chooseFontButton.right = new FormAttachment(100, -1);
		chooseFontButton.setLayoutData(fd_chooseFontButton);
		chooseFontButton.setText(" Aa ");
		chooseFontButton.setToolTipText("Open iconic font");

		Button chooseClipartButton = new Button(leftPane, SWT.FLAT);
		chooseClipartButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
		        DirectoryDialog dlg = new DirectoryDialog(shell);
		        dlg.setFilterPath(root);
		        dlg.setText("Open a clipart directory");
		        dlg.setMessage("Select a directory");
		        String dir = dlg.open();
		        if (dir != null) {
		          root = dir;
		          
		          int count = lblRootPathCombo.getItemCount();
		          if ( count > 9 ) {
			          for ( int i = count - 1; i >= 8; i++ ) {
			        	  lblRootPathCombo.remove(i);
			          }
		          }
		          
		          boolean alreadyThere = false;
		          count = lblRootPathCombo.getItemCount();
		          for ( int i = 0; i < count; i++ ) {
		        	  if ( dir.equals(lblRootPathCombo.getItem(i)) ) {
		        		  lblRootPathCombo.select(i);
		        		  alreadyThere = true;
		        		  break;
		        	  }
		          }
		          
		          if ( !alreadyThere ) {
			          lblRootPathCombo.add( root, 0 ); 
	        		  lblRootPathCombo.select(0);
		          }

		          updatePicker(false);

		          tv.setInput(new File(root));
		        }				
			}
		});
		FormData fd_changeRootButton = new FormData();
		fd_changeRootButton.top = new FormAttachment(0, 10);
		fd_changeRootButton.right = new FormAttachment(chooseFontButton, -4);
		chooseClipartButton.setLayoutData(fd_changeRootButton);
		chooseClipartButton.setText(" ... ");
		chooseClipartButton.setToolTipText("Open clipart directory");
		
		lblRootPathCombo = new Combo(leftPane, SWT.NONE);
		lblRootPathCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int ind = ((Combo)e.widget).getSelectionIndex();
				String dir = ((Combo)e.widget).getItem(ind);
				root = dir;

				boolean fontView = false;
				if ( root != null && (root.toLowerCase().endsWith(".ttf") || root.toLowerCase().endsWith(".otf") )) {
					fontView = true;
				}
				
				setText(root);

				updatePicker(fontView);
			}
		});
		fd_lblRootPath = new FormData();
		fd_lblRootPath.left = new FormAttachment(lblRootPath, 0);
		fd_lblRootPath.top = new FormAttachment(0, 11);
		fd_lblRootPath.right = new FormAttachment(chooseClipartButton, -4);
		lblRootPathCombo.setLayoutData(fd_lblRootPath);
		
		if ( recentRoots.size() == 0 ) {
			lblRootPathCombo.add(root);
			lblRootPathCombo.select(0);
		} else {
			int sel = 0;
			for ( int i = 0; i < recentRoots.size(); i++ ) {
				lblRootPathCombo.add(recentRoots.get(i));
				if ( root != null && root.equals(recentRoots.get(i)) ) {
					sel = i;
				}
			}
			lblRootPathCombo.select(sel);
		}

		
		if ( root != null && (root.toLowerCase().endsWith(".ttf") || root.toLowerCase().endsWith(".otf") )) {
			buildGlyphPicker();
		} else {
			buildClipartNavigationTree();
		}
		
		Button btnNext = new Button(leftPane, SWT.FLAT);
		btnNext.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( glyphsTable != null ) {
					int sel;
					int[] sels = glyphsTable.getSelectionIndices();
					if ( sels == null || sels.length == 0 ) {
						sel = 0;
					} else {
						sel = sels[0];
						if ( sel < glyphsTable.getItemCount() - 1 ) {
							sel++;
						} else {
							sel = 0;
						}
					}
					glyphsTable.setSelection(sel);
					glyphsTable.notifyListeners(SWT.Selection, new Event());
				}
				if ( tv != null ) {
					IStructuredSelection selection = (IStructuredSelection)tv.getSelection();
					File f = (File)selection.getFirstElement();
					if ( f == null ) {
						TreeItem[] ti = tv.getTree().getItems();
						if ( ti.length > 0 ) {
							StructuredSelection ss = new StructuredSelection(ti[0].getData());
							tv.setSelection(ss);
						}
					} else {
						TreeItem[] ti = tv.getTree().getItems();
						Vector<Object> r = new Vector<Object>();
						dumpTree(ti, r);
						
						File next = null;
						boolean found = false;
						Iterator<Object> ii = r.iterator();
						while ( ii.hasNext() ) {
							File x = (File)ii.next();
							if ( found ) {
								next = x;
								break;
							}
							if ( f.equals(x) ) {
								found = true;
							}
						}
						if ( next != null ) {
							StructuredSelection ss = new StructuredSelection(next);
							tv.setSelection(ss);
						}
					}
				}
			}
		});
		FormData fd_btnNext = new FormData();
		fd_btnNext.bottom = new FormAttachment(100, -10);
		fd_btnNext.right = new FormAttachment(100, -2);
		fd_btnNext.height = 32;
		btnNext.setLayoutData(fd_btnNext);
		btnNext.setImage(Util.getImageRegistry(shell.getDisplay()).get("next"));
		
		Button btnPrev = new Button(leftPane, SWT.FLAT);
		btnPrev.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( glyphsTable != null ) {
					int sel;
					int[] sels = glyphsTable.getSelectionIndices();
					if ( sels == null || sels.length == 0 ) {
						sel = glyphsTable.getItemCount()-1;
					} else {
						sel = sels[0];
						if ( sel > 0 ) {
							sel--;
						} else {
							sel = glyphsTable.getItemCount()-1;
						}
					}
					glyphsTable.setSelection(sel);
					glyphsTable.notifyListeners(SWT.Selection, new Event());
				}
				if ( tv != null ) {
					IStructuredSelection selection = (IStructuredSelection)tv.getSelection();
					File f = (File)selection.getFirstElement();
					if ( f == null ) {
						TreeItem[] ti = tv.getTree().getItems();
						if ( ti.length > 0 ) {
							StructuredSelection ss = new StructuredSelection(ti[0].getData());
							tv.setSelection(ss);
						}
					} else {
						TreeItem[] ti = tv.getTree().getItems();
						Vector<Object> r = new Vector<Object>();
						dumpTree(ti, r);
						
						File prev = null;
						Iterator<Object> ii = r.iterator();
						while ( ii.hasNext() ) {
							File x = (File)ii.next();
							if ( f.equals(x) ) {
								break;
							}
							prev = x;
						}
						if ( prev != null ) {
							StructuredSelection ss = new StructuredSelection(prev);
							tv.setSelection(ss);
						}
					}
				}
			}
		});
		FormData fd_btnPrev = new FormData();
		fd_btnPrev.bottom = new FormAttachment(btnNext, 0, SWT.BOTTOM);
		fd_btnPrev.right = new FormAttachment(btnNext, -2);
		fd_btnPrev.height = 32;
		btnPrev.setLayoutData(fd_btnPrev);
		btnPrev.setImage(Util.getImageRegistry(shell.getDisplay()).get("prev"));
		
		Link link = new Link(leftPane, SWT.NONE);
		FormData fd_link = new FormData();
		fd_link.bottom = new FormAttachment(100, -10);
		fd_link.left = new FormAttachment(0, 10);
		link.setLayoutData(fd_link);
		link.setText("<a href=\"" + CLIPART_URL + "\">Get clipart...</a>");
		link.setToolTipText( CLIPART_URL );
		link.addSelectionListener(new SelectionAdapter() { // XXX
			@Override
			public void widgetSelected(SelectionEvent e) {
		        if( java.awt.Desktop.isDesktopSupported() ) {
			        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
			        if( desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
			            try {
//			                java.net.URI uri = new java.net.URI(e.text + "/" + Util.escapeName(acl.owner + " " + acl.remains));
//			                desktop.browse( uri );
			            } catch ( Exception ez ) {
			                System.err.println( ez.getMessage() );
			            }
			        }
		        }
			}
		});
		
		Composite main = new Composite(sash, SWT.NONE);

		main.setLayout(new FormLayout());
		FormData fd_main = new FormData();
		fd_main.bottom = new FormAttachment(100, -10);
		fd_main.right = new FormAttachment(100, -10);
		fd_main.top = new FormAttachment(0, 10);
		fd_main.left = new FormAttachment(0, 10);
		main.setLayoutData(fd_main);



		previewPane = new ScrolledComposite(main, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		FormData fd_previewPane = new FormData();
		fd_previewPane.bottom = new FormAttachment(100, -10);
		fd_previewPane.right = new FormAttachment(100, -230);
		fd_previewPane.top = new FormAttachment(0, 10);
		fd_previewPane.left = new FormAttachment(0, 10);
		previewPane.setLayoutData(fd_previewPane);
		previewPane.setExpandHorizontal(true);
		previewPane.setExpandVertical(true);
		previewPane.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {

				final int step = 1;
				final int ctrlStep = 9;
				
				if ( btnDeviceBounds.getSelection() ) {
					boolean toUpdate = false;
					switch( e.keyCode ) {
					case SWT.ARROW_LEFT:
						if ( (e.stateMask & SWT.SHIFT) != 0 ) {
							boundX = Math.max(0, boundX-ctrlStep);
						}
						boundX = Math.max(0, boundX-step);
						toUpdate = true;
						break;
					case SWT.ARROW_RIGHT:
						if ( (e.stateMask & SWT.SHIFT) != 0 ) {
							boundX = Math.min(Math.max(0,currentWidth - DataLayerView.width), boundX+ctrlStep);
						}
						boundX = Math.min(Math.max(0,currentWidth - DataLayerView.width), boundX+step);
						toUpdate = true;
						break;
					case SWT.ARROW_UP:
						if ( (e.stateMask & SWT.SHIFT) != 0 ) {
							boundY = Math.max(0, boundY-ctrlStep);
						}
						boundY = Math.max(0, boundY-step);
						toUpdate = true;
						break;
					case SWT.ARROW_DOWN:
						if ( (e.stateMask & SWT.SHIFT) != 0 ) {
							boundY = Math.min(Math.max(0,currentHeight - DataLayerView.height), boundY+ctrlStep);
						}
						boundY = Math.min(Math.max(0,currentHeight - DataLayerView.height), boundY+step);
						toUpdate = true;
						break;
					}
					if ( toUpdate ) {
						updateImage(btnMonochrome != null && !btnMonochrome.isDisposed() && !btnMonochrome.getSelection());
					}
				}
			}
		});

        Label lblSrcImageCaption = new Label(main, SWT.NONE);
		if ( win ) {
	        lblSrcImageCaption.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblSrcImageCaption.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        FormData fd_lblSrcImageCaption = new FormData();
		fd_lblSrcImageCaption.top = new FormAttachment(0, 10);
		fd_lblSrcImageCaption.left = new FormAttachment(previewPane, 10);
        lblSrcImageCaption.setLayoutData(fd_lblSrcImageCaption);
        lblSrcImageCaption.setText("Original Image");

        Label label = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        FormData fd_label = new FormData();
        fd_label.top = new FormAttachment(lblSrcImageCaption, 6);
        fd_label.left = new FormAttachment(previewPane, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);

        lblImageName = new Label(main, SWT.NONE);
		if ( win ) {
	        lblImageName.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.BOLD));
		} else {
	        lblImageName.setFont(SWTResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		}
        FormData fd_lbImgName = new FormData();
        fd_lbImgName.top = new FormAttachment(label, 10);
        fd_lbImgName.left = new FormAttachment(previewPane, 10);
        fd_lbImgName.right = new FormAttachment(100, -10);
        lblImageName.setLayoutData(fd_lbImgName);
        lblImageName.setText("nothing selected");

		lblFontSize = new Label(main, SWT.NONE);
		FormData fd_lblFontSize = new FormData();
		fd_lblFontSize.top = new FormAttachment(lblImageName, 10);
		fd_lblFontSize.left = new FormAttachment(lblImageName, 0, SWT.LEFT);
//		fd_lblFontSize.width = 60;
		lblFontSize.setLayoutData(fd_lblFontSize);
		lblFontSize.setText("Font size:");
		
		fontSizeCombo = new Combo(main, SWT.NONE);
		fontSizeCombo.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					int i = Integer.parseInt(fontSizeCombo.getText());
					if ( i >= 121 ) {
						fontSizeCombo.select(17);
					}
					if ( i <= 5 ) {
						fontSizeCombo.select(0);
					}
				} catch (NumberFormatException e1) {
					fontSizeCombo.select(14);
				}
			}
		});
		fontSizeCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try {
					int i = Integer.parseInt(fontSizeCombo.getText());
					if ( i > 5 && i < 121 ) {
						updateImage(true);
					}
				} catch (NumberFormatException e1) {
					fontSizeCombo.select(14);
				}
			}
		});
		fontSizeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateImage(true);
			}
		});

		String[] items = {"6", "7", "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72"};
		fontSizeCombo.setItems(items);
		fontSizeCombo.select(initFontSize);
		
		FormData fd_fontSizeCombo = new FormData();
		fd_fontSizeCombo.top = new FormAttachment(lblFontSize, -4, SWT.TOP);
		fd_fontSizeCombo.left = new FormAttachment(lblFontSize, 20);
        fd_fontSizeCombo.right = new FormAttachment(100, -10);
        fontSizeCombo.setLayoutData(fd_fontSizeCombo);
        
        Label lblResolutionCaption = new Label(main, SWT.NONE);
        FormData fd_lblResolution = new FormData();
        fd_lblResolution.top = new FormAttachment(lblFontSize, 7);
        fd_lblResolution.left = new FormAttachment(previewPane, 10);
        lblResolutionCaption.setLayoutData(fd_lblResolution);
        lblResolutionCaption.setText("Resolution:");
        
        lblResolution = new Label(main, SWT.NONE);
		if ( win ) {
	        lblResolution.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.BOLD));
		} else {
	        lblResolution.setFont(SWTResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		}
        FormData fd_lblx = new FormData();
        fd_lblx.bottom = new FormAttachment(lblResolutionCaption, 0, SWT.BOTTOM);
        fd_lblx.left = new FormAttachment(fontSizeCombo, 0, SWT.LEFT);
        fd_lblx.right = new FormAttachment(100, -10);
        lblResolution.setLayoutData(fd_lblx);
        
        
        Label lblTypeCaption = new Label(main, SWT.NONE);
        FormData fd_lblType = new FormData();
        fd_lblType.top = new FormAttachment(lblResolutionCaption, 4);
        fd_lblType.left = new FormAttachment(previewPane, 10);
        lblTypeCaption.setLayoutData(fd_lblType);
        lblTypeCaption.setText("Type:");
        
        lblType = new Label(main, SWT.NONE);
		if ( win ) {
	        lblType.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.BOLD));
		} else {
	        lblType.setFont(SWTResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		}
        FormData fd_lblJpeg = new FormData();
        fd_lblJpeg.bottom = new FormAttachment(lblTypeCaption, 0, SWT.BOTTOM);
        fd_lblJpeg.left = new FormAttachment(lblResolution, 0, SWT.LEFT);
        fd_lblJpeg.right = new FormAttachment(100, -10);
        lblType.setLayoutData(fd_lblJpeg);
		

        Label lblTargetImageCaption = new Label(main, SWT.NONE);
		if ( win ) {
	        lblTargetImageCaption.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblTargetImageCaption.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        FormData fd_lblTargetImageCaption = new FormData();
        fd_lblTargetImageCaption.top = new FormAttachment(lblTypeCaption, 46);
		fd_lblTargetImageCaption.left = new FormAttachment(previewPane, 10);
        lblTargetImageCaption.setLayoutData(fd_lblTargetImageCaption);
        lblTargetImageCaption.setText("Resulting Image");

        label = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(lblTargetImageCaption, 6);
        fd_label.left = new FormAttachment(previewPane, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);
		
		

        btnDeviceBounds = new Button(main, SWT.CHECK);
        btnDeviceBounds.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		updateImage(false);
        		if ( currentWidth > DataLayerView.width || currentHeight > DataLayerView.height ) {
                    lblHint.setVisible(((Button)e.widget).getSelection());
        		} else {
                    lblHint.setVisible(false);
        		}
       			previewPane.setFocus();
        	}
        });
        FormData fd_btnDeviceBounds = new FormData();
        fd_btnDeviceBounds.top = new FormAttachment(label, 10, SWT.TOP);
        fd_btnDeviceBounds.left = new FormAttachment(previewPane, 10);
        fd_btnDeviceBounds.right = new FormAttachment(100, -10);
        btnDeviceBounds.setLayoutData(fd_btnDeviceBounds);
        btnDeviceBounds.setText("Show Target Device Bounds");
        btnDeviceBounds.setSelection(initShowBounds);
        
        btnMonochrome = new Button(main, SWT.CHECK);
        btnMonochrome.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		
        		if ( ((Button)e.widget).getSelection() ) {
        			monochrome = true;
        			hiedgeSlider.setVisible(true);
        			lblWhite.setVisible(true);
        		} else {
        			monochrome = false;
        			hiedgeSlider.setVisible(false);
        			lblWhite.setVisible(false);
        		}
        		
        		updateImage(true);
        		previewPane.setFocus();
        	}
        });
        btnMonochrome.setEnabled(true);
        btnMonochrome.setSelection(monochrome);
        FormData fd_btnMonochrome = new FormData();
        fd_btnMonochrome.top = new FormAttachment(btnDeviceBounds, 2);
        fd_btnMonochrome.left = new FormAttachment(previewPane, 10);
        fd_btnMonochrome.right = new FormAttachment(100, -10);
        btnMonochrome.setLayoutData(fd_btnMonochrome);
        btnMonochrome.setText("Monochrome");
        

		hiedgeSlider = new Scale(main, SWT.NONE);
		hiedgeSlider.setMaximum(edgeMax);
		hiedgeSlider.setSelection(edge);

		hiedgeSlider.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edge = ((Scale)e.widget).getSelection();
				lblWhite.setText("Threshold (" + edge + ")");
        		updateImage(true);
			}
		});

        FormData fd_hiedgeSlider = new FormData();
        fd_hiedgeSlider.top = new FormAttachment(btnMonochrome, -3);
        fd_hiedgeSlider.left = new FormAttachment(btnMonochrome, 15, SWT.LEFT);
        fd_hiedgeSlider.width = 110;
        hiedgeSlider.setLayoutData(fd_hiedgeSlider);
        
		lblWhite = new Label(main, SWT.NONE);
		lblWhite.setText("Threshold (" + edge + ")");

        FormData fd_btnWhite = new FormData();
		if ( win ) {
	        fd_btnWhite.top = new FormAttachment(hiedgeSlider, 10, SWT.TOP);
		} else {
	        fd_btnWhite.top = new FormAttachment(hiedgeSlider, 0, SWT.TOP);
		}
        fd_btnWhite.left = new FormAttachment(hiedgeSlider, 10);
        fd_btnWhite.right = new FormAttachment(100, -10);
        lblWhite.setLayoutData(fd_btnWhite);

		if ( monochrome ) {
			hiedgeSlider.setVisible(true);
			lblWhite.setVisible(true);
		} else {
			hiedgeSlider.setVisible(false);
			lblWhite.setVisible(false);
		}
		
		Label lblFontDataSizeCaption = new Label(main, SWT.NONE);
		FormData fd_lblFontDataSizeCaption = new FormData();
		fd_lblFontDataSizeCaption.top = new FormAttachment(hiedgeSlider, 10);
		fd_lblFontDataSizeCaption.left = new FormAttachment(previewPane, 10);
		lblFontDataSizeCaption.setLayoutData(fd_lblFontDataSizeCaption);
		lblFontDataSizeCaption.setText("Estimated Icon Data Size");
		
		
		lblExpectedImageSize = new Label(main, SWT.NONE);
		lblExpectedImageSize.setAlignment(SWT.RIGHT);
		lblExpectedImageSize.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_MAGENTA));
		if ( win ) {
			lblExpectedImageSize.setFont(SWTResourceManager.getFont("Tahoma", 12, SWT.BOLD));
		} else {
			lblExpectedImageSize.setFont(SWTResourceManager.getFont("Lucida Grande", 14, SWT.BOLD));
		}
        FormData fd_lblFontDataSize = new FormData();
        fd_lblFontDataSize.top = new FormAttachment(lblFontDataSizeCaption, 4);
        fd_lblFontDataSize.right = new FormAttachment(100, -10);
        fd_lblFontDataSize.left = new FormAttachment(previewPane, 10);
        lblExpectedImageSize.setLayoutData(fd_lblFontDataSize);
        lblExpectedImageSize.setText("0 bytes");

        if ( actualBytes != null ) {
			lblExpectedImageSize.setText(actualBytes.length + " bytes");
		} else {
	        lblExpectedImageSize.setText("0 bytes");
		}

        lblHint = new Label(main, SWT.WRAP);
        lblHint.setToolTipText("");
        FormData fd_lblHint = new FormData();
        fd_lblHint.top = new FormAttachment(lblExpectedImageSize, 10);
        fd_lblHint.left = new FormAttachment(previewPane, 7);
        fd_lblHint.right = new FormAttachment(100, -6);
        fd_lblHint.height = 60;
        lblHint.setLayoutData(fd_lblHint);
        lblHint.setVisible(false);
        
		btnAddToResources = new Button(main, SWT.FLAT);
        btnAddToResources.setEnabled(false);
		btnAddToResources.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String name = "icon_";
				String ntmp;
				String sufx = "";
				if ( imagePath != null ) {
					File tmp = new File(imagePath);
					ntmp = tmp.getName() + (btnMonochrome.getSelection() ? "" : "_a");
					sufx = "" + System.currentTimeMillis();
				}  else {
					ntmp = tt.getFullName();
					sufx = "u" + Integer.toHexString(chosenGlyph.charAt(0)).toUpperCase() +
							(btnMonochrome.getSelection() ? "" : "_a");
				}
				
				boolean apnd = false;
				for ( int i = 0; i < ntmp.length(); i++ ) {
					char ch = ntmp.charAt(i);
					if ( ch == ' ' || ch == '\\' || ch == '/' || ch == ':' ) {
						continue;
					}
					if ( ch == '.' || ch == '-' ) {
						ch = '_';
					}
					name += ch;
					apnd = true;
				}
				if ( !apnd || imagePath == null ) {
					name += "_" + sufx;
				}

				if ( actualBytes.length > 32767 ) {
					System.out.println("WARNING: array size is too big for some platforms");
				}
				
				DataLayerView.appendGlobals(Util.formatIconBytes( true, name, actualBytes, imgWidth, imgHeight,
						(DataLayerView.emulatingLibrary == 0 ? DataLayerView.PIXELS_INSTANCE_NAME : DataLayerView.UTFT_INSTANCE_NAME)));

				btnCancel.setText("  Close  ");
			}
		});
		
		FormData fd_btnAddToResources = new FormData();
		fd_btnAddToResources.bottom = new FormAttachment(100, -10);
		fd_btnAddToResources.right = new FormAttachment(100, -10);
		btnAddToResources.setLayoutData(fd_btnAddToResources);
		btnAddToResources.setText("Add To Resources");

        btnCancel = new Button(main, SWT.FLAT);
        btnCancel.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		saveState();
        		if ( originalImage != null && !originalImage.isDisposed() ) {
        			originalImage.dispose();
        		}
        		shell.dispose();
        	}
        });
        FormData fd_btnCancel = new FormData();
        fd_btnCancel.top = new FormAttachment(btnAddToResources, 0, SWT.TOP);
        fd_btnCancel.right = new FormAttachment(btnAddToResources, -4);
        btnCancel.setLayoutData(fd_btnCancel);
        btnCancel.setText("Cancel");

        btnSaveAsRaw = new Button(main, SWT.FLAT);
        btnSaveAsRaw.setEnabled(false);
        btnSaveAsRaw.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
				CenteredFileDialog resourceDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
				resourceDialog.dialog.setFilterExtensions(new String[] { "*.raw", "*.*" });
				resourceDialog.dialog.setFilterNames(new String[] { "Raw Data Files (RAW)", "All Files" });
				resourceDialog.dialog.setFilterPath(DataLayerView.recentImageResourcePath);
				String name = lblImageName.getText();
				int ind = name.lastIndexOf('.');
				if ( ind > 0 ) {
					name = name.substring(0, ind) + ".raw";
				} else {
					name += ".raw";
				}
				resourceDialog.dialog.setFileName(name);
				String f = resourceDialog.dialog.open();
				if ( f != null ) {
					DataLayerView.appendGlobals(Util.formatIconReference( true, f, actualBytes.length, (DataLayerView.emulatingLibrary == 0 ? DataLayerView.PIXELS_INSTANCE_NAME : DataLayerView.UTFT_INSTANCE_NAME) ));
					DataLayerView.recentImageResourcePath = resourceDialog.dialog.getFilterPath();
					try {
						FileOutputStream fos = new FileOutputStream(f);
						fos.write(actualBytes);
						fos.flush();
						fos.close();
				        btnCancel.setText("  Close  ");
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
        	}
        });
        FormData fd_btnSaveAsRaw = new FormData();
        fd_btnSaveAsRaw.bottom = new FormAttachment(btnAddToResources, -6);
        fd_btnSaveAsRaw.right = new FormAttachment(100, -10);
        btnSaveAsRaw.setLayoutData(fd_btnSaveAsRaw);
        btnSaveAsRaw.setText("Save as data file");
        
		lblImage = new Label(previewPane, SWT.NONE);
		lblImage.setAlignment(SWT.CENTER);
		lblImage.setText("           ");
		previewPane.setContent(lblImage);

		currentWidth = imgWidth;
		currentHeight = imgHeight;

		sash.setWeights(new int[] {weight1, weight2});

		if ( root != null && (root.toLowerCase().endsWith(".ttf") || root.toLowerCase().endsWith(".otf") )) {
			lblFontSize.setVisible(true);
			fontSizeCombo.setVisible(true);
		} else {
			lblFontSize.setVisible(false);
			fontSizeCombo.setVisible(false);
		}
		
		updateImage(true);
	}

	private void buildGlyphPicker() {
		
		try {
//			boolean loadFont = 
					Display.getCurrent().loadFont(root);
//			System.out.println(loadFont);
			tt = new TrueType(root);
			System.out.println(tt.getFullName());
			System.out.println(tt.getRanges());
			
			try {
				FileInputStream fis = new FileInputStream(root);
				awtFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fis);
			} catch (Exception e1) {
				awtFont = null;
				System.out.println("WARNING: incompatible font");
			}
			setText(root);
		} catch (IOException e) {
			e.printStackTrace();
			DataLayerView.displayError("Font file reading error:\n" + e.getMessage());
		} catch (Exception e3) {
			e3.printStackTrace();
			return;
		}
		
		glyphPicker = new Composite(leftPane, SWT.NONE);
		FormData fd_glyphPicker = new FormData();
		fd_glyphPicker.bottom = new FormAttachment(100, -44);
		fd_glyphPicker.right = new FormAttachment(100, 0);
		fd_glyphPicker.top = new FormAttachment(lblRootPathCombo, 0);
		fd_glyphPicker.left = new FormAttachment(0, 0);
		glyphPicker.setLayoutData(fd_glyphPicker);

		glyphPicker.setLayout(new FormLayout());

		glyphsTable = new Table(glyphPicker, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

		FormData fd_table = new FormData();
		fd_table.bottom = new FormAttachment(100, 0);
		fd_table.right = new FormAttachment(100, -2);
		fd_table.top = new FormAttachment(0, 3);
		fd_table.left = new FormAttachment(0, 2);
		glyphsTable.setLayoutData(fd_table);

		glyphsTable.setHeaderVisible(false);
		glyphsTable.setFont(SWTResourceManager.getFont(tt.getFullName(), 24, SWT.NORMAL));

		final TableColumn column = new TableColumn(glyphsTable, SWT.NULL);

		ArrayList l = tt.getGlyphList();
		Iterator ii = l.iterator();
		while( ii.hasNext() ) {
			Integer i = (Integer)ii.next();

			Image img = new Image(shell.getDisplay(), 16, 16);
			
			GC gc = new GC(img);
			gc.setFont(SWTResourceManager.getFont(tt.getFullName(), 16, SWT.NORMAL));
//			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
//			gc.fillRectangle(0, 0, 16, 16);
			gc.drawString("" + (char)i.intValue(), 0, 0);
		    gc.dispose();
		    
		    boolean blank = isBlank(img.getImageData());
		    
		    img.dispose();

			if ( blank ) {
				continue;
			}

			TableItem item = new TableItem(glyphsTable, SWT.NULL);
			item.setText(0, "" + (char)i.intValue());
		}

		glyphsTable.getColumn(0).pack();

//		final Text text = new Text(glyphPicker, SWT.NONE);
//		FormData fd_text = new FormData();
//		fd_text.bottom = new FormAttachment(100, -10);
//		fd_text.left = new FormAttachment(table, 0, SWT.LEFT);
//		fd_text.right = new FormAttachment(table, 0, SWT.RIGHT);
//		text.setLayoutData(fd_text);
//		text.setEditable(false);

		glyphsTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem[] sel = glyphsTable.getSelection();
				if ( sel != null && sel.length > 0 ) {
					TableItem item = sel[0];
					String s = item.getText();
					lblImageName.setText("" + s + "    (0x" + Integer.toHexString(s.charAt(0)).toUpperCase() + ")");
			        imageType = "font glyph";

			        chosenGlyph = s;
			        redrawGlyphImage();

			        lblResolution.setText(currentWidth + "x" + currentHeight);
			        
					if ( btnAddToResources != null ) {
						btnAddToResources.setEnabled(true);
						btnSaveAsRaw.setEnabled(true);
					}
				    
					updateImage(true);
				}
			}
		});

		glyphPicker.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle area = glyphPicker.getClientArea();
				Point preferredSize = glyphsTable.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - 2*glyphsTable.getBorderWidth();
				if (preferredSize.y > area.height + glyphsTable.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = glyphsTable.getVerticalBar().getSize();
					width -= vBarSize.x;
				}
				Point oldSize = glyphsTable.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns 
					// smaller first and then resize the table to
					// match the client area width
					column.setWidth(width-5);
					glyphsTable.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table 
					// bigger first and then make the columns wider
					// to match the client area width
					glyphsTable.setSize(area.width, area.height);
					column.setWidth(width-5);
				}
			}
		});		
	}

	private boolean isBlank(ImageData imageData) {
		byte[] bb = imageData.data;
		for ( int i = 0; i < bb.length; i++ ) {
			if ( (i + 1) % 4 == 0 ) {
				continue;
			}
			if ( (bb[i] & 0xFF) != 0xFF  ) {
				return false;
			}
		}
		return true;
	}

	private void buildClipartNavigationTree() {
		clipartNavigation = new Composite(leftPane, SWT.NONE);
		clipartNavigation.setLayout(new FormLayout());
		FormData fd_navigation = new FormData();
		fd_navigation.bottom = new FormAttachment(100, -44);
		fd_navigation.right = new FormAttachment(100, 0);
		fd_navigation.top = new FormAttachment(lblRootPathCombo, 0);
		fd_navigation.left = new FormAttachment(0, 0);
		clipartNavigation.setLayoutData(fd_navigation);

		tv = new TreeViewer(clipartNavigation, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL );
		Tree tree = tv.getTree();
		FormData fd_tree = new FormData();
		fd_tree.bottom = new FormAttachment(100, 0);
		fd_tree.right = new FormAttachment(100, -2);
		fd_tree.top = new FormAttachment(0, 3);
		fd_tree.left = new FormAttachment(0, 2);
		tree.setLayoutData(fd_tree);
		
		tv.setContentProvider(new FileTreeContentProvider());
		tv.setLabelProvider(new FileTreeLabelProvider());
		tv.setSorter(new FileSorter());
		tv.addFilter(new AllowOnlyImportableFilter());
		tv.setInput(new File(root));
		tv.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				File f = (File)selection.getFirstElement();
				if ( f != null ) {
					String path = f.getAbsolutePath();
					File fx = new File(path);
					if (!fx.isDirectory()) {
//						IStatusLineManager manager = getViewSite().getActionBars().getStatusLineManager();
//						manager.setMessage("file: " + path);
						
						if ( fx.equals(selectedFile) ) {
							return;
						}
						
						selectedFile = fx;
						imagePath = selectedFile.getAbsolutePath();

						setText(selectedFile.getName());
						lblImageName.setText(selectedFile.getName());

						try {
							byte[] imageData = ImageChecker.readBinaryData(new FileInputStream(selectedFile));
							imageType = ImageChecker.getImageType(imageData);
						} catch (Exception e) {
							e.printStackTrace();
							DataLayerView.displayError("Image file reading/parsing error:\n" + e.getMessage());
						}
						
						originalImage = new Image(shell.getDisplay(), imagePath);
						
						Rectangle bnds = originalImage.getBounds();
						imgWidth = bnds.width;
						imgHeight = bnds.height;
						if ( imgWidth > 800 || imgHeight > 600 ) {
							imgWidth = Math.min(800, imgWidth); 
							imgHeight = Math.min(600, imgHeight);
							
							Image i = new Image(shell.getDisplay(), imgWidth, imgHeight);
							
							GC gc = new GC(i);
//							gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
//							gc.fillRectangle(0, 0, imgWidth, imgHeight);
							gc.drawImage(originalImage, 0, 0, imgWidth, imgHeight, 0, 0, imgWidth, imgHeight);
						    gc.dispose();
						    originalImage.dispose();
						    originalImage = i;
						}
						
						if ( btnAddToResources != null ) {
							btnAddToResources.setEnabled(true);
							btnSaveAsRaw.setEnabled(true);
						}
					} else if ( f.isDirectory() ) {
						tv.expandToLevel(f, 1);
					}
				}
				
				updateImage(true);
//				tv.getControl().setFocus();
			}
		});
		
		if ( lastFolder != null ) {
			File f = new File(lastFolder);
			StructuredSelection ss = new StructuredSelection(f);
			tv.setSelection(ss,true);
			tv.refresh();
			lastFolder = null;
		}
	}
	
	private void redrawGlyphImage() {
		
		if ( chosenGlyph == null ) {
			return;
		}
		
		int fontSize = 28;
		try {
			fontSize = Integer.parseInt(fontSizeCombo.getText());
		} catch (NumberFormatException e) {
		}

		fontSize = (int)(1.35 * fontSize + .5);

		int height;
		int width;
		
		Image img;
		
		if ( awtFont != null ) {
			java.awt.Font f = awtFont.deriveFont((float)fontSize);
			@SuppressWarnings("deprecation")
			FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics( f );
			height = (int)((fm.getAscent() + fm.getDescent()) * 1.5);

			int top = height;
			int bottom = 0;

			width = fm.charWidth(chosenGlyph.charAt(0));
				
			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bi.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			g.setColor(Color.BLACK);
			g.setFont(f);
			g.drawString(chosenGlyph, 0, fm.getAscent());
			g.dispose();

			for ( int y = 0; y < top; y++ ) {
				boolean blank = true;
				for ( int x = 0; x < width; x++ ) {
					int pix = bi.getRGB(x, y);
					if ( (0xffffff & pix) != 0xffffff ) {
						blank = false;
						break;
					}
				}
				if ( !blank ) {
					top = y;
					break;
				}
			}

			for ( int y = height-1; y >= bottom; y-- ) {
				boolean blank = true;
				for ( int x = 0; x < width; x++ ) {
					int pix = bi.getRGB(x, y);
					if ( (0xffffff & pix) != 0xffffff ) {
						blank = false;
						break;
					}
				}
				if ( !blank ) {
					bottom = y;
					break;
				}
			}

			if ( (top != 0 || bottom != height-1) && bottom - top + 1 > 0) {
				height = bottom - top + 1;

				BufferedImage bi2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				g = bi2.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);
				g.drawImage(bi, 0, -top, null);
				g.dispose();
				bi = bi2;
			}

			ImageData id = Util.convertToSWT(bi);
			img = new Image(shell.getDisplay(), id);

		} else {
			Font font = SWTResourceManager.getFont(tt.getFullName(), fontSize, SWT.NORMAL);
			
			GC gc = new GC(font.getDevice());
			gc.setFont(font);
			gc.setTextAntialias(SWT.ON);
			gc.setAntialias(SWT.ON);
			gc.setAdvanced(true);
			Point bounds = gc.stringExtent(chosenGlyph);
			gc.dispose();
			
			height = bounds.y;
			width = bounds.x;
			
			img = new Image(shell.getDisplay(), width, height);
			gc = new GC(img);
			gc.setFont(font);
			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
			gc.fillRectangle(0, 0, width, height);
			gc.setTextAntialias(SWT.ON);
			gc.setAntialias(SWT.ON);
			gc.setAdvanced(true);
			gc.drawString(chosenGlyph, 0, 0);
			gc.dispose();

			
			int top = height;
			int bottom = 0;
			
			ImageData id = img.getImageData();
			
			for ( int y = 0; y < top; y++ ) {
				boolean blank = true;
				for ( int x = 0; x < width; x++ ) {
					int pix = id.getPixel(x, y);
					if ( (0xffffff00 & pix) != 0xffffff00 ) {
						blank = false;
						break;
					}
				}
				if ( !blank ) {
					top = y;
					break;
				}
			}

			for ( int y = height-1; y >= bottom; y-- ) {
				boolean blank = true;
				for ( int x = 0; x < width; x++ ) {
					int pix = id.getPixel(x, y);
					if ( (0xffffff00 & pix) != 0xffffff00 ) {
						blank = false;
						break;
					}
				}
				if ( !blank ) {
					bottom = y;
					break;
				}
			}
			
			if ( (top != 0 || bottom != height-1) && bottom - top + 1 > 0) {
				height = bottom - top + 1;
			    Image img2 = new Image(shell.getDisplay(), width, height);
				gc = new GC(img2);
				gc.setAntialias(SWT.ON);
				gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
				gc.fillRectangle(0, 0, width, height);
				gc.drawImage(img, 0, -top);
			    gc.dispose();
			    img.dispose();
			    img = img2;
			}
			
		}
		
		
		

		imgWidth = width;
		imgHeight = height;
		currentWidth = width;
		currentHeight = height;
		
		if ( originalImage != null ) {
			originalImage.dispose();
		}
		originalImage = img;
	}

	
	private void updateImage( boolean recompress ) {
		
		if ( recompress ) {
			redrawGlyphImage();
		}
		
		if ( originalImage != null ) {
			currentWidth = imgWidth;
			currentHeight = imgHeight;

			boolean antialiased = !monochrome;
			
			actualBytes = getImageBytes(antialiased, recompress, actualBytes);
			if ( actualBytes != null ) {
				lblExpectedImageSize.setText(actualBytes.length + " bytes");
			}
						
			int restoredWidth = 0xFF & actualBytes[5];
			int restoredHeight = 0xFF & actualBytes[4];
			boolean restoredAntialiasing = actualBytes[1] == (byte)'a';
			
			if ( antialiased != restoredAntialiasing ) {
				System.out.println("Corrupted icon data (Err4)");
				return;
			}
			
			if ( imgHeight != restoredHeight && restoredHeight != 255 ) {
				System.out.println("Corrupted icon data (Err5) " + restoredHeight);
				return;
			}
			
			if ( imgWidth != restoredWidth && restoredWidth != 255 ) {
				System.out.println("Corrupted icon data (Err6)");
				return;
			}
			
			byte[] shifted = new byte[actualBytes.length-1]; // shift the array 1 byte left to match a font glyph format
			System.arraycopy(actualBytes, 1, shifted, 0, shifted.length);
			
			byte[] restored = RasterGlyph.restoreImageData(restoredHeight, shifted, restoredAntialiasing);

			BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = bi.createGraphics();

			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
			int ptr = 0;
			for ( int y = 0; y < restoredHeight; y++ ) {
				for ( int x = 0; x < restoredWidth; x++ ) {
					int b = 0xff & restored[ptr++]; 
					Color color = new Color(b, b, b);
					g.setColor(color);
					g.drawLine(x,y,x,y);
				}
			}
			g.dispose();
			
			ImageData id = Util.convertToSWT(bi);
			
			Image scaled = new Image(shell.getDisplay(), id);

			if ( btnDeviceBounds.getSelection() ) {
				Image i = new Image(shell.getDisplay(), Math.max(currentWidth, DataLayerView.width), Math.max(currentHeight, DataLayerView.height));
				
				GC gc = new GC(i);
			    if ( currentWidth >= DataLayerView.width && currentHeight >= DataLayerView.height ) {
			    	gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
			    	gc.fillRectangle(0, 0, currentWidth, currentHeight);
		    		gc.drawImage(scaled, boundX, boundY, DataLayerView.width, DataLayerView.height, 0, 0, DataLayerView.width, DataLayerView.height);
			    } else {
			    	gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
			    	gc.fillRectangle(0, 0, Math.max(currentWidth, DataLayerView.width), Math.max(currentHeight,DataLayerView.height));
			    	gc.setAlpha(150);
				    gc.drawImage(scaled, 0, 0, currentWidth, currentHeight, 0, 0, currentWidth, currentHeight);
			    	gc.setAlpha(255);
			    	gc.drawRectangle(boundX-1, boundY-1, DataLayerView.width+1, DataLayerView.height+1);
			    	gc.setClipping(boundX, boundY, DataLayerView.width, DataLayerView.height);
				    gc.drawImage(scaled, 0, 0, currentWidth, currentHeight, 0, 0, currentWidth, currentHeight);
			    	gc.setForeground(SWTResourceManager.getColor(255,255,255));
			    	gc.drawRectangle(-1, -1, currentWidth+1, currentHeight+1);
			    }
			    gc.dispose();
			    scaled.dispose();
			    scaled = i;
			} else {
				Image i = new Image(shell.getDisplay(), currentWidth, currentHeight);
				GC gc = new GC(i);
		    	gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		    	gc.fillRectangle(0, 0, currentWidth, currentHeight);
			    gc.drawImage(scaled, 0, 0, width, height, 0, 0, width, height);
			    gc.dispose();
			    scaled.dispose();
			    scaled = i;
			}

			lblImage.setImage(scaled);
	        lblType.setText(imageType);
	        lblResolution.setText(currentWidth+ "x" + currentHeight);
		
			previewPane.setMinSize(lblImage.computeSize(SWT.DEFAULT, SWT.DEFAULT));

			if ( currentWidth > DataLayerView.width || currentHeight > DataLayerView.height ) {
				lblHint.setVisible(btnDeviceBounds.getSelection());
			} else {
				lblHint.setVisible(false);
			}
		}
	}

	private byte[] getImageBytes(boolean antialiased, boolean changed, byte[] oldState) {
		
		if( !changed && oldState != null && oldState.length != 0 ) {
			return oldState;
		}
		
		int width = Math.min(imgWidth, 255);
		int height = Math.min(imgHeight, 255);
		actualWidth = width;

		Image scaled = new Image(shell.getDisplay(), width, height);
		GC gc = new GC(scaled);
		gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		gc.fillRectangle(0, 0, width, height);
		gc.drawImage(originalImage, 0, 0, currentWidth, currentHeight, 0, 0, width, height);
		
		gc.dispose();
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ImageData id = scaled.getImageData();
		
		byte[] bx = new byte[1];
		for ( int j = 0; j < height; j++  ) {
			for ( int i = 0; i < width; i++  ) {
				if ( i >= width || j >= height ) {
					continue;
				}
				int px = id.getPixel(i, j);
				RGB rgb = id.palette.getRGB(px);
				bx[0] = (byte)(0xFF & Math.round(0.2126*rgb.red + 0.7152*rgb.green + 0.0722*rgb.blue));
				try {
					baos.write(bx);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		byte[] bb = baos.toByteArray();
		if ( bb == null ) {
			System.err.println("Compression failed! Corrupted image bytes." );
			return bb;
		} 
		
//		int pp = 0;
//		for ( int j = 0; j < height; j++ ) {
//			for ( int i = 0; i < width; i++ ) {
//				byte b = bb[pp++];
//				if ( b == 0 ) {
//					System.out.print("#");
//				} else if ( (0xff & b) == 255 ) {
//					System.out.print(" ");
//				} else {
//					System.out.print("+");
//				}
//			}
//			System.out.println();
//		}
		
//		if(!Arrays.equals(bb, test)) {
//			System.err.println("Compression failed! " + (bb.length != test.length ? bb.length + " vs. " + test.length : "") );
//			for ( int i = 0; i < bb.length; i++ ) {
//				System.err.print(hex(bb[i]));
//				if ( bb[i] != test[i] ) {
//					System.err.println( "!= " + hex(test[i]) + "(" + i + ")" );
//					break;
//				}
//			}
//		}
		
		return formatIconBytes(bb, actualWidth, antialiased);
	}

	public byte[] formatIconBytes( byte[] bytes, int width, boolean antialiased ) {

		byte[] data = new byte[bytes.length*2+8];
		int ptr = 0;

		int marginLeft = -1;
		int marginRight = -1;
		int marginTop = 0;
		int marginBottom = 0;
		
		int height = bytes.length / width;
		
		for ( int i = 0; i < bytes.length; i++ ) {
			if ( (0xff & bytes[i]) != 0xff ) {
				marginTop = Math.min(255, i / width);
				break;
			}
		}
		for ( int i = bytes.length - 1; i >= 0; i-- ) {
			if ( (0xff & bytes[i]) != 0xff ) {
				marginBottom = Math.min(255, height - (i / width) - 1);
				break;
			}
		}
		for ( int i = 0; i < width; i++ ) {
			for ( int j = 0; j < height; j++ ) {
				if ( (0xff & bytes[j*width+i]) != 0xff ) {
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
				if ( (0xff & bytes[j*width+(width-i-1)]) != 0xff ) {
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
		
		if ( antialiased ) {

			data[0] = (byte)'Z';
			data[1] = (byte)'a';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginRight & 0x7f);

			ptr = 9;
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
			
			data[0] = (byte)'Z';
			data[1] = (byte)'a';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)((marginLeft+128) & 0xff); // high bit is v-polarization flag 
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginBottom & 0xff);

			ptr = 9;
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
//				System.out.println(" |");
			} else {
//				System.out.println(" -");
			}
			
		} else {

			byte[] horRaw; // horizontal scan raw
			int hrPtr;
			byte[] horPack; // horizontal packed
			int hpPtr;
			byte[] verRaw; // vertical scan raw
			int vrPtr;
			byte[] verPack; // vertical packed
			int vpPtr;
			
			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginRight & 0x7f);
			
			ptr = 9;

			ptr--;
			for (int y = 0; y < height - marginBottom - marginTop; y++) {
				for (int x = 0; x < width - marginLeft - marginRight; x++) {
					int i = y * (width - marginLeft - marginRight) + x; 
					if ( i % 8 == 0 ) {
						ptr++;
						data[ptr] = (byte)0xff;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; // XXX
					if ( pixel < 255 - edge * 10 ) {
						int mask = 1 << (7 - (i % 8));
						data[ptr] ^= mask;
					}
				}
			}
			ptr++;
			
			horRaw = data;
			hrPtr = ptr;
			
			// ------------------------------------------------------------------------
			
			data = new byte[bytes.length*2+9];

			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)(marginRight & 0x7f);
			
			ptr = 9;

			ptr--;
			for (int x = 0; x < width - marginLeft - marginRight; x++) {
				for (int y = 0; y < height - marginBottom - marginTop; y++) {
					int i = x * (height - marginTop - marginBottom) + y; 
					if ( i % 8 == 0 ) {
						ptr++;
						data[ptr] = (byte)0xff;
					}
					int pixel = bytes[marginLeft + x + (y + marginTop) * width] & 0xff; // XXX
					if ( pixel < 255 - edge * 10 ) {
						int mask = 1 << (7 - (i % 8));
						data[ptr] ^= mask;
					}
				}
			}
			ptr++;

			verRaw = data;
			vrPtr = ptr;

			// ------------------------------------------------------------------------

			data = new byte[bytes.length*2+9];

			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)(marginLeft & 0x7f);
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)((marginRight+128) & 0xff); // high bit is compression flag
			
			ptr = 9;

			boolean prevColor = (horRaw[ptr] & 0x80) == 0;
			int ctr = 0;
			for (int i = 9; i < hrPtr; i++) {
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
			
			horPack = data;
			hpPtr = ptr;
			
			// ------------------------------------------------------------------------

			data = new byte[bytes.length*2+9];

			data[0] = (byte)'Z';
			data[1] = (byte)'b';
			data[4] = (byte)(height & 0xff); //it cannot be higher 255px
			data[5] = (byte)(width & 0xff); //it cannot be wider 255px
			data[6] = (byte)((marginLeft+128) & 0xff); // high bit is v-polarization flag
			data[7] = (byte)(marginTop & 0xff);
			data[8] = (byte)((marginBottom+128) & 0xff); // high bit is compression flag
			
			ptr = 9;
			
			prevColor = (verRaw[ptr] & 0x80) == 0;
			ctr = 0;
			for (int i = 9; i < vrPtr; i++) {
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
			
			verPack = data;
			vpPtr = ptr;

			data = horRaw;
			ptr = hrPtr;

			if ( hpPtr < ptr ) {
				data = horPack;
				ptr = hpPtr;
//				System.out.println(" -");
			}

			if ( vpPtr < ptr ) {
				data = verPack;
				ptr = vpPtr;
//				System.out.println(" |");
			}
		} 
		
		data[2] = (byte)((ptr & 0xff00)>>8);
		data[3] = (byte)(ptr & 0xff);
		
		byte[] result = new byte[ptr];
		System.arraycopy(data, 0, result, 0, ptr);
		return result;
	}

	public void saveState() {
		
	    IDialogSettings settings = Activator.getDefault().getDialogSettings();
	    IDialogSettings section = settings.getSection(DIALOG_SETTINGS);
	    if (section == null) {
	    	section = settings.addNewSection(DIALOG_SETTINGS);
	    }

        Point dialogSize = shell.getSize();
	    
		section.put("width", ""+dialogSize.x);
		section.put("height", ""+dialogSize.y);
		
		section.put("weight-1", sash.getWeights()[0]);
		section.put("weight-2", sash.getWeights()[1]);

		section.put("root", root);

		section.put("monochrome", monochrome);
		section.put("hiedge", edge);
		section.put("font-size", fontSizeCombo.getSelectionIndex());
		section.put("show-bounds", btnDeviceBounds.getSelection());
		
		if ( tv != null ) {
			IStructuredSelection selection = (IStructuredSelection)tv.getSelection();
			if ( selection != null ) {
				File f = (File)selection.getFirstElement();
				if ( f != null ) {
					if ( f.isDirectory() ) {
						lastFolder = f.getAbsolutePath();
					} else {
						lastFolder = f.getParentFile().getAbsolutePath();
					}
					section.put("last-folder", lastFolder);
				}
			}
		}
		if ( lblRootPathCombo != null ) {
			for ( int i = 0; i < lblRootPathCombo.getItemCount(); i++ ) {
				String rrr = lblRootPathCombo.getItem(i);
				section.put("recent-root-" + i, rrr);
			}
		}
	}

	public void loadState() {
		
	    IDialogSettings settings = Activator.getDefault().getDialogSettings();
	    IDialogSettings section = settings.getSection(DIALOG_SETTINGS);
	    if (section == null) {
	    	return;
	    }

		try {
			width = Integer.parseInt(section.get("width"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			height = Integer.parseInt(section.get("height"));
		} catch (Exception e) {
		}
		try {
			weight1 = Integer.parseInt(section.get("weight-1"));
		} catch (Exception e) {
		}
		try {
			weight2 = Integer.parseInt(section.get("weight-2"));
		} catch (Exception e) {
		}
		
	    String r = section.get("root");
	    if ( r != null ) {
	    	root = r;
	    }

	    for ( int i = 0; i < 10; i++ ) {
	    	String rd = section.get("recent-root-" + i);
	    	if ( rd != null ) {
	    		recentRoots.add( rd );
	    	}
	    }

	    r = section.get("last-folder");
	    if ( r != null ) {
	    	lastFolder = r;
	    }
	    
		monochrome = section.getBoolean("monochrome");

		try {
			edge = Integer.parseInt(section.get("hiedge"));
		} catch (Exception e) {
		}
		try {
			initFontSize = Integer.parseInt(section.get("font-size"));
		} catch (Exception e) {
		}

		initShowBounds = section.getBoolean("show-bounds");
	}
	
	private String hex( byte i ) {
		String s = Integer.toHexString(0xFF & i);
		if ( s.length() == 1 ) {
			s = "0" + s;
		}
		return (s + " ").toUpperCase();
	}

	protected void dumpTree(TreeItem[] ti, Vector<Object> result) {
		for ( int i = 0; i < ti.length; i++ ) {
			TreeItem t = ti[i];
			if (t.getData() != null) {
				result.addElement(t.getData());
			}
			TreeItem[] nn = t.getItems();
			if (nn.length > 0) {
				dumpTree(nn, result);
			}
		}
	}

	private void updatePicker(boolean fontView) {
		if ( fontView ) {
			if ( clipartNavigation != null && !clipartNavigation.isDisposed() ) {
				if ( tv != null ) {
					tv.getTree().dispose();
					tv = null;
				}
				clipartNavigation.dispose();
				clipartNavigation = null;
			}

			if ( glyphPicker != null && !glyphPicker.isDisposed() ) {
				if ( glyphsTable != null ) {
					glyphsTable.dispose();
					glyphsTable = null;
				}
				glyphPicker.dispose();
				glyphPicker = null;
			}
			
			buildGlyphPicker();
			leftPane.layout();

			lblFontSize.setVisible(true);
			fontSizeCombo.setVisible(true);
			
		} else {
			chosenGlyph = null;
			
			if ( glyphPicker != null && !glyphPicker.isDisposed() ) {
				if ( glyphsTable != null ) {
					glyphsTable.dispose();
					glyphsTable = null;
				}
				glyphPicker.dispose();
				glyphPicker = null;
				buildClipartNavigationTree();
				leftPane.layout();
			}

			if ( clipartNavigation != null && !clipartNavigation.isDisposed() ) {
				if ( tv != null ) {
					tv.setInput(new File(root));
				}
			}

			lblFontSize.setVisible(false);
			fontSizeCombo.setVisible(false);
		}
	}

	public class FileTreeLabelProvider extends LabelProvider { 
		public String getText(Object element) {
			return ((File) element).getName();
		}

		public Image getImage(Object element) {
			if (((File) element).isDirectory()) {
				return Util.getImageRegistry(shell.getDisplay()).get("folder");
			} else {
				return Util.getImageRegistry(shell.getDisplay()).get("picture");
			}
		}
	}

	public class FileTreeContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object element) {
			Object[] kids = ((File) element).listFiles();
			return kids == null ? new Object[0] : kids;
		}

		public Object[] getElements(Object element) {
			return getChildren(element);
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public Object getParent(Object element) {
			return ((File) element).getParentFile();
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object old_input,
				Object new_input) {
		}
	}

	public class AllowOnlyImportableFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parent, Object element) {
			String name = ((File) element).getName().toLowerCase();
			
			if (name.startsWith(".")) {
				return false;
			}
			
			
			if (((File) element).isDirectory()) {
				return true;
			}

			if (name.endsWith("gif") || name.endsWith("png") || name.endsWith("jpg") || name.endsWith("jpeg")) {
				return true;
			}

			return false;
		}
	}

	public class FileSorter extends ViewerSorter {
		public int category(Object element) {
			return ((File) element).isDirectory() ? 0 : 1;
		}
	}
}

