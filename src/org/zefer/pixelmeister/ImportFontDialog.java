package org.zefer.pixelmeister;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.fonts.RasterFont;
import org.zefer.pixelmeister.util.Util;



public class ImportFontDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	private Combo fontSizeCombo;
	private Text glyphHeightText;
	private Text charactersToImport;
	private ScrolledComposite fontPreviewScroller;
	private Composite fontPreviewPane;
	private Label lblTypeYourText;
	private Combo zoom;
	private int zoomFactor = 1;

	private Font font;
	private Label lblFontSize;
	
	private boolean antialiased = false;
	private Label lblFontDataSize;
	private RasterFont rasterFont;
	private Label lblGlyphHeight;
	private Label lblResultingRasterFont;
	
	private boolean showBounds = true;
	private boolean showBaseline;
	private boolean editable;
	
	private char selected = '\0';
	
	private boolean win;
	private int baseline;
	private int glyphHeight;
	private int bottom;
	private int top;
	
	private boolean guiInitialized;
	
	private int LIMIT = 100;
	
	private Button btnEnableEdit;
	
	private Label leftMarginLabel;
	private Label rightMarginLabel;	
	private Spinner spinnerL;
	private Spinner spinnerR;
	private int lTrim = 0;
	private int rTrim = 0;

	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 * @wbp.parser.constructor
	 */
	public ImportFontDialog(Shell parent, String fontPath) {
		super(parent);
		
		win = File.separatorChar == '\\';
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DataLayerView.ORG_ZEFER_LCDPAINTER_RESOURCESVIEW);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
		
		try {
			FileInputStream fis = new FileInputStream( fontPath );
			font = Font.createFont( Font.TRUETYPE_FONT, fis );
			font.getName();
			fis.close();
			setText(fontPath);
		} catch (Exception e) {
			e.printStackTrace();
			DataLayerView.displayError("Font file reading error:\n" + e.getMessage());
		}
	}

	public ImportFontDialog(Shell parent, Font font) {
		super(parent);
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DataLayerView.ORG_ZEFER_LCDPAINTER_RESOURCESVIEW);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
		
		this.font = font;
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		
		// Move the dialog to the center of the top level shell.
        Rectangle shellBounds = getParent().getBounds();
        Point dialogSize = shell.getSize();

        shell.setLocation(
          shellBounds.x + (shellBounds.width - dialogSize.x) / 2,
          shellBounds.y + (shellBounds.height - dialogSize.y) / 2);

		charactersToImport.setText("A-Z a-z . , - + #x0021-#x0024 #00048-#00057");
		
        generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
        // "abcdABCDXYZ}{- 1234567890:");

		shell.open();
		shell.layout();
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
		shell = new Shell(getParent(), SWT.RESIZE | SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setMinimumSize(new Point(580, 480));
		shell.setSize(580, 480);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		Image icon = Util.getImageRegistry(shell.getDisplay()).get("pixelmeister");
		shell.setImage(icon);
		
		fontPreviewScroller = new ScrolledComposite(shell, SWT.BORDER | SWT.V_SCROLL );
		fontPreviewScroller.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		FormData fd_fontPreviewScroller = new FormData();
		fd_fontPreviewScroller.top = new FormAttachment(0, 10);
		fd_fontPreviewScroller.right = new FormAttachment(100, -230);
		fd_fontPreviewScroller.left = new FormAttachment(0, 10);
		fd_fontPreviewScroller.bottom = new FormAttachment(100, -100);
		fontPreviewScroller.setLayoutData(fd_fontPreviewScroller);
		fontPreviewScroller.setExpandHorizontal(true);
		fontPreviewScroller.setExpandVertical(true);

		fontPreviewPane = new Composite(fontPreviewScroller, SWT.NONE);
		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.wrap = true;
		fontPreviewPane.setLayout(layout);
		
//		fontPreviewPane.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		fontPreviewScroller.setContent(fontPreviewPane);
//		fontPreviewScroller.setMinSize(fontPreviewPane.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		fontPreviewScroller.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				relayoutScroller();
			}
		});		

		lblTypeYourText = new Label(shell, SWT.NONE);
		FormData fd_lblTypeYourText = new FormData();
		fd_lblTypeYourText.left = new FormAttachment(0, 10);
		fd_lblTypeYourText.bottom = new FormAttachment(100, -77);
		lblTypeYourText.setLayoutData(fd_lblTypeYourText);
		lblTypeYourText.setText("Type your text or define character ranges:");

		
		charactersToImport = new Text(shell, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		
		if ( win ) {
			charactersToImport.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.NORMAL));
		} else {
			charactersToImport.setFont(SWTResourceManager.getFont("Lucida Grande", 12, SWT.NORMAL));
		}
		charactersToImport.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
			}
		});
		FormData fd_charactersToImport = new FormData();
		fd_charactersToImport.left = new FormAttachment(0, 10);
		fd_charactersToImport.right = new FormAttachment(100, -230);
		fd_charactersToImport.top = new FormAttachment(fontPreviewScroller, 30);
		fd_charactersToImport.bottom = new FormAttachment(100, -10);
		charactersToImport.setLayoutData(fd_charactersToImport);
		
		
		
        Label lblFontCaption = new Label(shell, SWT.NONE);
		if ( win ) {
	        lblFontCaption.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblFontCaption.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        FormData fd_lblFont = new FormData();
		fd_lblFont.top = new FormAttachment(0, 10);
		fd_lblFont.left = new FormAttachment(fontPreviewScroller, 10);
        lblFontCaption.setLayoutData(fd_lblFont);
        lblFontCaption.setText("True Type Font");

        Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        FormData fd_label = new FormData();
        fd_label.top = new FormAttachment(lblFontCaption, 6);
        fd_label.left = new FormAttachment(fontPreviewScroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);

        
		Label lblFontName = new Label(shell, SWT.NONE);
		FormData fd_lblFontName = new FormData();
		fd_lblFontName.top = new FormAttachment(lblFontCaption, 20, SWT.BOTTOM);
		fd_lblFontName.left = new FormAttachment(lblFontCaption, 0, SWT.LEFT);
		lblFontName.setLayoutData(fd_lblFontName);
		lblFontName.setText("Name:");
		
		Label fontNameCombo = new Label(shell, SWT.READ_ONLY);
		if ( win ) {
			fontNameCombo.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.BOLD));
		} else {
			fontNameCombo.setFont(SWTResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		}
		FormData fd_fontNameCombo = new FormData();
		fd_fontNameCombo.top = new FormAttachment(lblFontName, 0, SWT.TOP);
		fd_fontNameCombo.left = new FormAttachment(lblFontName, 10);
        fd_fontNameCombo.right = new FormAttachment(100, -10);
		fontNameCombo.setLayoutData(fd_fontNameCombo);
		fontNameCombo.setText(font.getName());
        
		
		lblFontSize = new Label(shell, SWT.NONE);
		FormData fd_lblFontSize = new FormData();
		fd_lblFontSize.top = new FormAttachment(lblFontName, 10, SWT.BOTTOM);
		fd_lblFontSize.left = new FormAttachment(lblFontName, 0, SWT.LEFT);
		lblFontSize.setLayoutData(fd_lblFontSize);
		lblFontSize.setText("Size:");
		
		fontSizeCombo = new Combo(shell, SWT.NONE);
		fontSizeCombo.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				try {
					double i = Double.parseDouble(fontSizeCombo.getText());
					if ( guiInitialized && i >= 121 ) {
						fontSizeCombo.select(17);
					}
					if ( guiInitialized && i <= 5 ) {
						fontSizeCombo.select(0);
					}
				} catch (NumberFormatException e1) {
					if ( guiInitialized ) {
						fontSizeCombo.select(14);
					}
				}
			}
		});
		fontSizeCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try {
					double i = Double.parseDouble(fontSizeCombo.getText());
					if ( guiInitialized && i > 5 && i < 121 ) {
		        		rasterFont.reset();
						generateGlyphImages(charactersToImport.getText(), i, -1, 0, 0);
					}
				} catch (NumberFormatException e1) {
					if ( guiInitialized ) {
						fontSizeCombo.select(14);
					}
				}
			}
		});
		fontSizeCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
        		rasterFont.reset();
				generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
			}
		});

		String[] items = {"6", "7", "8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72"};
		fontSizeCombo.setItems(items);
		fontSizeCombo.select(14);
		
		FormData fd_fontSizeCombo = new FormData();
		fd_fontSizeCombo.top = new FormAttachment(lblFontSize, -4, SWT.TOP);
		fd_fontSizeCombo.left = new FormAttachment(fontNameCombo, 0, SWT.LEFT);
        fd_fontSizeCombo.right = new FormAttachment(100, -10);
        fontSizeCombo.setLayoutData(fd_fontSizeCombo);
		

        Button btnAntialiased = new Button(shell, SWT.CHECK);
        btnAntialiased.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		antialiased = ((Button)e.widget).getSelection();
        		if (antialiased) {
            		btnEnableEdit.setEnabled(false);
            		btnEnableEdit.setSelection(false);
            		editable = false;
        		} else {
            		btnEnableEdit.setEnabled(zoomFactor == 8);
        		}
        		updateVisibilities();        			
        		rasterFont.reset();
        		rasterFont.setAntialiased(antialiased);
				generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
        	}
        });
        FormData fd_btnAntialiased = new FormData();
        fd_btnAntialiased.top = new FormAttachment(fontSizeCombo, 6);
        fd_btnAntialiased.left = new FormAttachment(fontSizeCombo, 0, SWT.LEFT);
        btnAntialiased.setLayoutData(fd_btnAntialiased);
        btnAntialiased.setText("  Antialiased");

        lblResultingRasterFont = new Label(shell, SWT.NONE);
		if ( win ) {
	        lblResultingRasterFont.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
			lblResultingRasterFont.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        FormData fd_lblResultingRasterFont = new FormData();
        fd_lblResultingRasterFont.top = new FormAttachment(btnAntialiased, 30);
        fd_lblResultingRasterFont.left = new FormAttachment(fontPreviewScroller, 10);
        lblResultingRasterFont.setLayoutData(fd_lblResultingRasterFont);
        lblResultingRasterFont.setText("Resulting Font");
        
        label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(lblResultingRasterFont, 6);
        fd_label.left = new FormAttachment(fontPreviewScroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);
        
		lblGlyphHeight = new Label(shell, SWT.NONE);
		FormData fd_lblGlyphHeight = new FormData();
		fd_lblGlyphHeight.left = new FormAttachment(fontPreviewScroller, 10);
		fd_lblGlyphHeight.top = new FormAttachment(lblResultingRasterFont, 20);
		lblGlyphHeight.setLayoutData(fd_lblGlyphHeight);
		lblGlyphHeight.setText("Glyph Height:");

		glyphHeightText = new Text(shell, SWT.NONE);
		glyphHeightText.setEditable(false);
		if ( win ) {
			glyphHeightText.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
			glyphHeightText.setFont(SWTResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		}
		glyphHeightText.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		glyphHeightText.setText("10");
		FormData fd_glyphHeightText = new FormData();
		fd_glyphHeightText.bottom = new FormAttachment(lblGlyphHeight, 1, SWT.BOTTOM);
		fd_glyphHeightText.left = new FormAttachment(lblGlyphHeight, 6);
		fd_glyphHeightText.right = new FormAttachment(100, -10);
		glyphHeightText.setLayoutData(fd_glyphHeightText);


		Label lblFontDataSizeCaption = new Label(shell, SWT.NONE);
		FormData fd_lblFontDataSizeCaption = new FormData();
		fd_lblFontDataSizeCaption.top = new FormAttachment(glyphHeightText, 10);
		fd_lblFontDataSizeCaption.left = new FormAttachment(fontPreviewScroller, 10);
		lblFontDataSizeCaption.setLayoutData(fd_lblFontDataSizeCaption);
		lblFontDataSizeCaption.setText("Estimated Font Data Size");
		
		
		lblFontDataSize = new Label(shell, SWT.NONE);
		lblFontDataSize.setAlignment(SWT.RIGHT);
		lblFontDataSize.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_MAGENTA));
		if ( win ) {
			lblFontDataSize.setFont(SWTResourceManager.getFont("Tahoma", 12, SWT.BOLD));
		} else {
			lblFontDataSize.setFont(SWTResourceManager.getFont("Lucida Grande", 14, SWT.BOLD));
		}
        FormData fd_lblFontDataSize = new FormData();
        fd_lblFontDataSize.top = new FormAttachment(lblFontDataSizeCaption, 4);
        fd_lblFontDataSize.right = new FormAttachment(100, -10);
        fd_lblFontDataSize.left = new FormAttachment(fontPreviewScroller, 10);
        lblFontDataSize.setLayoutData(fd_lblFontDataSize);
        lblFontDataSize.setText("0 bytes");
        

        label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(lblFontDataSize, 20);
        fd_label.left = new FormAttachment(fontPreviewScroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);

		Button btnShowGlyphBoundaries = new Button(shell, SWT.CHECK);
		btnShowGlyphBoundaries.setSelection(true);
		btnShowGlyphBoundaries.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showBounds = ((Button)(e.widget)).getSelection();
				generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
			}
		});
		FormData fd_btnShowGlyphBoundaries = new FormData();
		fd_btnShowGlyphBoundaries.top = new FormAttachment(label, 10);
		fd_btnShowGlyphBoundaries.left = new FormAttachment(fontPreviewScroller, 10);
		btnShowGlyphBoundaries.setLayoutData(fd_btnShowGlyphBoundaries);
		btnShowGlyphBoundaries.setText("Show glyph bounds");
        
		Button btnShowBaseline = new Button(shell, SWT.CHECK);
		btnShowBaseline.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showBaseline = ((Button)(e.widget)).getSelection();
				generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
			}
		});
		FormData fd_btnShowBaseline = new FormData();
		fd_btnShowBaseline.top = new FormAttachment(btnShowGlyphBoundaries, 4);
		fd_btnShowBaseline.left = new FormAttachment(fontPreviewScroller, 10);
		btnShowBaseline.setLayoutData(fd_btnShowBaseline);
		btnShowBaseline.setText("Show baseline");

		Label labelZoom = new Label(shell, SWT.NONE);
		FormData fd_labelZoom = new FormData();
		fd_labelZoom.top = new FormAttachment(btnShowGlyphBoundaries, 4);
		fd_labelZoom.left = new FormAttachment(btnShowBaseline, 6);
		fd_labelZoom.height = 16;
		labelZoom.setLayoutData(fd_labelZoom);
		labelZoom.setText("Zoom");


        zoom = new Combo(shell, SWT.NONE);
        zoom.add("x1");
        zoom.add("x2");
        zoom.add("x4");
        zoom.add("x8");
        zoom.select(0);
        zoom.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		zoomFactor = 1 << Math.max(0, zoom.getSelectionIndex());
        		btnEnableEdit.setEnabled(!antialiased && zoomFactor == 8);
        		btnEnableEdit.setSelection(!antialiased && editable && zoomFactor == 8);
        		editable &= !antialiased && zoomFactor == 8;
				updateVisibilities();

				generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
        	}
        });
        FormData fd_zoom = new FormData();
        fd_zoom.top = new FormAttachment(btnShowGlyphBoundaries, 0);
        fd_zoom.left = new FormAttachment(labelZoom, 8);
        fd_zoom.right = new FormAttachment(100, -10);
        zoom.setLayoutData(fd_zoom);

        label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(labelZoom, 10);
        fd_label.left = new FormAttachment(fontPreviewScroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);
		
		
		btnEnableEdit = new Button(shell, SWT.CHECK);
		btnEnableEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editable = ((Button)(e.widget)).getSelection();
				updateVisibilities();
			}
		});
		FormData fd_btnEnableEdit = new FormData();
		fd_btnEnableEdit.top = new FormAttachment(label, 10);
		fd_btnEnableEdit.left = new FormAttachment(fontPreviewScroller, 10);
		btnEnableEdit.setLayoutData(fd_btnEnableEdit);
		btnEnableEdit.setText("Enable edit (only at zoom x8)");
		btnEnableEdit.setEnabled(false);

		leftMarginLabel = new Label(shell, SWT.NONE);
		FormData fd_leftMargin = new FormData();
		fd_leftMargin.top = new FormAttachment(btnEnableEdit, 10);
		fd_leftMargin.left = new FormAttachment(fontPreviewScroller, 10);
		fd_leftMargin.height = 16;
		leftMarginLabel.setLayoutData(fd_leftMargin);
		leftMarginLabel.setText("Margis: Left");
		leftMarginLabel.setVisible(false);

		spinnerL = new Spinner(shell, SWT.BORDER);
        spinnerL.setMinimum(-5);
        spinnerL.setMaximum(5);
        spinnerL.setSelection(0);
        spinnerL.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	if (selected != 0) {
            		Control[] children = fontPreviewPane.getChildren();
            		for (int i = 0; i < children.length; i++) {
            			Control child = children[i];
            			if ((""+selected).equals(child.getToolTipText())) {
            				String value = spinnerL.getText();
            				int v = Integer.parseInt(value);
            				if (v > rTrim) {
            					// inc
            					int[] glyph = rasterFont.getOriginal(selected);
            					int w = rasterFont.getGlyphWidth(selected);
            					int h = rasterFont.getHeight();
            					int[] bytes = new int[(w+1) * h];
            					for (int y = 0; y < h; y++) {
	            					for (int x = 0; x < w; x++) {
	            						bytes[(y * (w+1)) + x + 1] = glyph[(y * w) + x];
	            					}
	            					bytes[(y * (w+1)) + 0] = 0xFFFFFF;
            					}
            					rasterFont.addGlyph(selected, bytes, w + 1);
            					BufferedImage bi = getCurrentGlyphImage(selected, w + 1, rasterFont.getBottom()+rasterFont.getTop());
            					updateLabelImage((Label)child, selected, bi, w + 1, rasterFont.getBottom()+rasterFont.getTop());
            				} else {
            					// dec
            					int w = rasterFont.getGlyphWidth(selected);
            					if (w > 1) {
            						int[] glyph = rasterFont.getOriginal(selected);
            						int h = glyph.length / w;
            						int[] bytes = new int[(w-1) * h];
            						for (int y = 0; y < h; y++) {
            							for (int x = 1; x < w; x++) {
            								bytes[(y * (w-1)) + x - 1] = glyph[(y * w) + x];
            							}
            						}
            						rasterFont.addGlyph(selected, bytes, w - 1);
            						BufferedImage bi = getCurrentGlyphImage(selected, w - 1, rasterFont.getBottom()+rasterFont.getTop());
            						updateLabelImage((Label)child, selected, bi, w - 1, rasterFont.getBottom()+rasterFont.getTop());
            					}
            				}
            				rTrim = v;
            			}
            		}
            		
            		fontPreviewScroller.redraw();
             	}
            }
        });
		FormData fd_spinnerL = new FormData();
		fd_spinnerL.top = new FormAttachment(btnEnableEdit, 8);
		fd_spinnerL.left = new FormAttachment(leftMarginLabel, 6);
		fd_spinnerL.height = 16;
		spinnerL.setLayoutData(fd_spinnerL);
		spinnerL.setVisible(false);
		
		
		rightMarginLabel = new Label(shell, SWT.NONE);
		FormData fd_rightMargin = new FormData();
		fd_rightMargin.top = new FormAttachment(btnEnableEdit, 10);
		fd_rightMargin.left = new FormAttachment(btnShowBaseline, 10);
		fd_rightMargin.height = 16;
		rightMarginLabel.setLayoutData(fd_rightMargin);
		rightMarginLabel.setText("  Right");
		rightMarginLabel.setVisible(false);

		spinnerR = new Spinner(shell, SWT.BORDER);
		spinnerR.setMinimum(-5);
		spinnerR.setMaximum(5);
		spinnerR.setSelection(0);
		spinnerR.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	if (selected != 0) {
            		Control[] children = fontPreviewPane.getChildren();
            		for (int i = 0; i < children.length; i++) {
            			Control child = children[i];
            			if ((""+selected).equals(child.getToolTipText())) {
            				String value = spinnerR.getText();
            				int v = Integer.parseInt(value);
            				if (v > rTrim) {
            					// inc
            					int[] glyph = rasterFont.getOriginal(selected);
            					int w = rasterFont.getGlyphWidth(selected);
            					int h = rasterFont.getHeight();
            					int[] bytes = new int[(w+1) * h];
            					for (int y = 0; y < h; y++) {
	            					for (int x = 0; x < w; x++) {
	            						bytes[(y * (w+1)) + x] = glyph[(y * w) + x];
	            					}
	            					bytes[(y * (w+1)) + w] = 0xFFFFFF;
            					}
            					rasterFont.addGlyph(selected, bytes, w + 1);
            					BufferedImage bi = getCurrentGlyphImage(selected, w + 1, rasterFont.getBottom()+rasterFont.getTop());
            					updateLabelImage((Label)child, selected, bi, w + 1, rasterFont.getBottom()+rasterFont.getTop());
            				} else {
            					// dec
            					int w = rasterFont.getGlyphWidth(selected);
            					if (w > 1) {
            						int[] glyph = rasterFont.getOriginal(selected);
            						int h = glyph.length / w;
            						int[] bytes = new int[(w-1) * h];
            						for (int y = 0; y < h; y++) {
            							for (int x = 0; x < w-1; x++) {
            								bytes[(y * (w-1)) + x] = glyph[(y * w) + x];
            							}
            						}
            						rasterFont.addGlyph(selected, bytes, w - 1);
            						BufferedImage bi = getCurrentGlyphImage(selected, w - 1, rasterFont.getBottom()+rasterFont.getTop());
            						updateLabelImage((Label)child, selected, bi, w - 1, rasterFont.getBottom()+rasterFont.getTop());
            					}
            				}
            				rTrim = v;
            			}
            		}
            		fontPreviewScroller.redraw();
             	}
            }
        });
		FormData fd_spinnerR = new FormData();
		fd_spinnerR.top = new FormAttachment(btnEnableEdit, 8);
		fd_spinnerR.left = new FormAttachment(rightMarginLabel, 6);
		fd_spinnerR.height = 16;
		spinnerR.setLayoutData(fd_spinnerR);
		spinnerR.setVisible(false);
		
		

		Button btnAddToResources = new Button(shell, SWT.FLAT);
		btnAddToResources.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String ntmp = font.getName();
				String name = "";
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
				if ( !apnd ) {
					name += "_" + System.currentTimeMillis();
				}
				
				name += fontSizeCombo.getText();
				name += (font.getStyle() & Font.BOLD) > 0 ? "b" : "";
				name += (font.getStyle() & Font.ITALIC) > 0 ? "i" : "";
				name += antialiased ? "a" : "";
				
				byte[] actualBytes;
				try {
					actualBytes = rasterFont.toByteArray( false, name );
					DataLayerView.appendGlobals(Util.formatFontBytes( true, name, charactersToImport.getText(), actualBytes, glyphHeight, baseline, antialiased, 
							(DataLayerView.emulatingLibrary == 0 ? DataLayerView.PIXELS_INSTANCE_NAME : DataLayerView.UTFT_INSTANCE_NAME) ));
				} catch (IOException e1) {
					DataLayerView.displayError("Cannot export converted font:\n" + e1.getMessage());
					e1.printStackTrace();
				}
				shell.dispose();
			}
		});
		FormData fd_btnAddToResources = new FormData();
		fd_btnAddToResources.bottom = new FormAttachment(100, -10);
		fd_btnAddToResources.right = new FormAttachment(100, -8);
		btnAddToResources.setLayoutData(fd_btnAddToResources);
		btnAddToResources.setText("Add To Resources");

        Button btnCancel = new Button(shell, SWT.FLAT);
        btnCancel.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		rasterFont = null;
        		shell.dispose();
        	}
        });
        FormData fd_btnCancel = new FormData();
        fd_btnCancel.top = new FormAttachment(btnAddToResources, 0, SWT.TOP);
        fd_btnCancel.right = new FormAttachment(btnAddToResources, -4);
        btnCancel.setLayoutData(fd_btnCancel);
        btnCancel.setText("Cancel");

        label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(btnCancel, -33);
        fd_label.left = new FormAttachment(fontPreviewScroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);

        guiInitialized = true;
	}

	private void updateVisibilities() {
		
		btnEnableEdit.setVisible(!antialiased);
		
		if (editable && selected != 0) {
			leftMarginLabel.setVisible(true);
			rightMarginLabel.setVisible(true);
			spinnerL.setVisible(true);
			spinnerR.setVisible(true);
		} else {
			leftMarginLabel.setVisible(false);
			rightMarginLabel.setVisible(false);
			spinnerL.setVisible(false);
			spinnerR.setVisible(false);
		}
		
		spinnerL.setSelection(lTrim);
		spinnerR.setSelection(rTrim);
	}

	private void relayoutScroller() {
		Rectangle r = fontPreviewScroller.getClientArea();
		fontPreviewScroller.setMinSize(fontPreviewPane.computeSize(r.width, SWT.DEFAULT));
	}

	private void generateGlyphImages( String chars, double fsize, int only, int trimL, int trimR ) {
		
		if ( chars == null ) {
			return;
		}
		
		Control[] children = fontPreviewPane.getChildren();
		for ( int i = 0; i < children.length; i++ ) {
			children[i].dispose();
			children[i] = null;
		}
		fontPreviewPane.layout(true);
		
		Point size = fontPreviewScroller.getSize();
		size.x -= 10;
		fontPreviewPane.setSize(size);
		
		chars = buildRange( chars );
		
		if ( fsize < 0 ) {
			int sel = fontSizeCombo.getSelectionIndex();
			String sizeString;
			if ( sel < 0 ) {
				sizeString = fontSizeCombo.getText();
			} else {
				sizeString = fontSizeCombo.getItem(fontSizeCombo.getSelectionIndex());
			}
			fsize = Double.parseDouble(sizeString);
		}

		fsize = (1.35 * fsize + .5);
		
		Font f = font.deriveFont((float)fsize);
		@SuppressWarnings("deprecation")
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics( f );
		int height = (int)((fm.getAscent() + fm.getDescent()) * 1.5);
		baseline = fm.getAscent();

		if (rasterFont == null) {
			rasterFont = new RasterFont(antialiased);
		}
		
		int top = height;
		int bottom = 0;

		int widthx = fm.charWidth(f.getMissingGlyphCode());
		if ( widthx <= 0 ) {
			widthx = 1;
		}
		BufferedImage bix = new BufferedImage(widthx, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D gx = bix.createGraphics();
		if ( antialiased ) {
			gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
		}
		gx.setColor(Color.WHITE);
		gx.fillRect(0, 0, widthx, height);
		gx.setColor(Color.BLACK);
		gx.setFont(f);
		gx.drawString("" + (char)f.getMissingGlyphCode(), 0, fm.getAscent());
		gx.dispose();

		if (only < 0 || rasterFont.getOriginal((char)only) == null) {
			for ( int i = 0; i < chars.length(); i++ ) {
				char c = chars.charAt(i);
				int width = fm.charWidth(c);
				
				if ( width == 0 ) {
					continue;
				}
				
				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = bi.createGraphics();
				if ( antialiased ) {
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
				}
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);
				g.setColor(Color.BLACK);
				g.setFont(f);
				g.drawString("" + c, 0, fm.getAscent());
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
						top = y-1;
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
						bottom = y+1;
						break;
					}
				}
			}
			
			glyphHeight = height;
			this.top = top;
			this.bottom = bottom;
		} else {
			top = this.top;
			bottom = this.bottom;
		}
		height = bottom+top;
		
		glyphHeightText.setText(glyphHeight < 0 ? "" : "" + glyphHeight + "px");
		
		rasterFont.setHeight(bottom-top, fm.getAscent());
		rasterFont.setTop(top);
		rasterFont.setBottom(bottom);
		
		for ( int i = 0; i < chars.length(); i++ ) {
			char c = chars.charAt(i);
			
			int[] glyphBytes = rasterFont.getOriginal(c);
			
			int width = 0;
			if (glyphBytes == null) {				
				width = fm.charWidth(c);
				
				if ( width == 0 || bottom-top <= 0 ) {
					continue;
				}
				
				BufferedImage bi = new BufferedImage(width, bottom-top, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = bi.createGraphics();
				if ( rasterFont.isAntialiased() ) {
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
				}
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);
				g.setColor(Color.BLACK);
				g.setFont(f);
				g.drawString("" + c, 0, fm.getAscent()-top);
				g.dispose();
				
				glyphBytes = getGlyphBytes( bi );
				rasterFont.addGlyph(c, glyphBytes, width);
			} else {
				width = rasterFont.getGlyphWidth(c);
			}
			
			BufferedImage bi = getCurrentGlyphImage(c, width, height);
			
			if ( width == widthx ) {
				boolean missingChar = true;
				for ( int y = 0; y < height; y++ ) {
					for ( int x = 0; x < width; x++ ) {
						
						int pix1 = bi.getRGB(x, y);
						int pix2 = bix.getRGB(x, y);

						if ( pix1 != pix2 ) {
							missingChar = false;
							break;
						}
					}
					if (!missingChar) {
						break;
					}
					
				}
				if (missingChar) {
					continue;
				}
			}
			
			Label l = new Label(fontPreviewPane, SWT.NONE);
			
			updateLabelImage(l, c, bi, width, height);
		}
		fontPreviewPane.layout(true);
		if ( lblFontDataSize != null && !lblFontDataSize.isDisposed() && rasterFont != null ) {
	        lblFontDataSize.setText(rasterFont.estimateSize() + " bytes");
		}
		relayoutScroller();
	}

	// -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true
	
	private void updateLabelImage(Label l, char c, BufferedImage bi, int width, int height) {
		ImageData id = Util.convertToSWT(bi);
		l.setSize(width * zoomFactor, height * zoomFactor);
		l.setImage(new Image(getParent().getDisplay(), id));
		l.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (editable) {
					
					if (selected == 0) {
						selected = c;
						spinnerL.setSelection(0);
						spinnerR.setSelection(0);
						updateVisibilities();
						generateGlyphImages(charactersToImport.getText(), -1, c, 0, 0);
						lTrim = 0;
						rTrim = 0;
					} else if (selected == c) {
						e.x /= zoomFactor;
						e.y /= zoomFactor;
						e.y -= rasterFont.getTop();
						
						int[] glyph = rasterFont.getOriginal(c);
						int w = rasterFont.getGlyphWidth(c);
						int h = rasterFont.getHeight();
						
						int ptr = e.y * w + e.x;
						if (ptr < glyph.length) {								
							glyph[ptr] = glyph[ptr] == 0 ? 0xFFFFFF : 0;
							rasterFont.addGlyph(c, glyph, w);								
							generateGlyphImages(charactersToImport.getText(), -1, c, 0, 0);
						}
					} else {
						selected = '\0';
						spinnerL.setSelection(0);
						spinnerR.setSelection(0);
						updateVisibilities();
						generateGlyphImages(charactersToImport.getText(), -1, -1, 0, 0);
						lTrim = 0;
						rTrim = 0;
					}
				}
			}
		});
		l.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				if (editable) {
					e.x /= zoomFactor;
					e.y /= zoomFactor;
					l.setToolTipText("" + c); // also used as ID to identify glyph label
				}
			}
		});
	}

	private BufferedImage getCurrentGlyphImage(char c, int width, int height) {
		BufferedImage bi = new BufferedImage(width * zoomFactor, height * zoomFactor, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bi.createGraphics();
		
		int top = rasterFont.getTop();
		int bottom = rasterFont.getBottom();

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, width * zoomFactor, height * zoomFactor);

		boolean converted = true;

		if ( converted ) {
			int w = rasterFont.getGlyphWidth( c );
			int h = rasterFont.getHeight();
			byte[] glyph = rasterFont.getGlyph( c );
			int ptr = 0;
			for ( int y = 0; y < h; y++ ) {
				for ( int x = 0; x < w; x++ ) {
					int b = 0xff & glyph[ptr++]; 
					Color color = new Color(b, b, b);
					g.setColor(color);
					for (int t = 0; t < zoomFactor; t++) {
						for (int n = 0; n < zoomFactor; n++) {
							g.drawLine(x * zoomFactor + n,
									(y+top) * zoomFactor + t,
									x * zoomFactor + n,
									(y+top) * zoomFactor + t);
						}
					}
				}
			}
			
//			} else {
//				if ( antialiased ) {
//					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);			
//				}
//				g.setColor(Color.BLACK);
//				g.setFont(f);
//				g.drawString("" + c, 0, fm.getAscent());
		}

		if ( showBounds ) {
			g.setColor(Color.RED);
			g.drawLine(0, top * zoomFactor, width * zoomFactor, top * zoomFactor);
			g.drawLine(0, bottom * zoomFactor, width * zoomFactor, bottom * zoomFactor);
		}
		
		if ( showBaseline ) {
			g.setColor(Color.GREEN);
			g.drawLine(0, baseline * zoomFactor, width * zoomFactor, baseline * zoomFactor);
		}

		if ( selected == c ) {
			g.setColor(Color.GREEN);
			g.drawRect(0, 0, width * zoomFactor - 1, height * zoomFactor - 1);
		}
		g.dispose();
		return bi;
	}
	
	private String buildRange(String chars) {
    	
		chars = resolveEntities(chars);

		HashSet set = new HashSet();
		char[] cx = chars.toCharArray();
		for (int i = 0; i < cx.length; i++) {
			char ch = cx[i];
			int rest = cx.length - i - 1;
			char prev = i > 0 ? cx[i-1] : ' ';
			
			if (ch == '-' && rest > 0 && i > 0 && prev != ' ' && cx[i+1] != ' ') {
				char ce = cx[i+1];
				if ( ce > prev ) {
					for (char j = (char)(prev + 1); j <= ce; j++) {
						Character c = new Character(j);
						set.add(c);
						if ( set.size() >= LIMIT ) {
							break;
						}
					}
					i++;
					continue;
				}
			}

			if ( set.size() >= LIMIT ) {
				break;
			}
			
			Character c = new Character(ch);
			set.add(c);
		}
		
		ArrayList list = new ArrayList(set);
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Character) o1).charValue() > ((Character) o2).charValue() ? 1 : -1;
			}
		});
		chars = "";
		Iterator ii = list.iterator();
		while (ii.hasNext()) {
			Character c = (Character) ii.next();
			chars += c.charValue();
		}
//		System.out.println(chars);
		return chars;
	}

	private String resolveEntities(String chars) {
		String res = "";
    	char[] cx = chars.toCharArray();
    	for ( int i = 0; i < cx.length; i++ ) {
    		char ch = cx[i];
    		int rest = cx.length - i - 1;
    		
    		if ( rest >= 5 ) {
    			if ( ch == '#' ) {
    				if ( cx[i + 1] == 'x' ) {
    					String num = chars.substring(i+2, i+6);
    					try {
							int ix = Integer.parseInt(num, 16);
							ch = (char)ix;
							i += 5;
						} catch (NumberFormatException e) {
						}
    					
    				} else {
    					String num = chars.substring(i+1, i+6);
    					try {
							int ix = Integer.parseInt(num);
							ch = (char)ix;
							i += 5;
						} catch (NumberFormatException e) {
						}
    				}
    			}
    		}
    		res += ch;
    	}
//    	System.out.println( "[" + res + "]" );
		return res;
	}

    private int[] getGlyphBytes(BufferedImage bufferedImage) {
    	int[] result = null;
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			
			result = new int[data.width * data.height];
		
			int i = 0;
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					result[i++] = pixel;
				}
			}
		} 
		
		return result;
	}
}
