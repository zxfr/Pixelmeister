package org.zefer.pixelmeister;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.util.CenteredFileDialog;
import org.zefer.pixelmeister.util.Compressor;
import org.zefer.pixelmeister.util.ImageChecker;
import org.zefer.pixelmeister.util.Util;



public class ImportImageDialog extends Dialog {

	protected Object result;
	protected Shell shell;

	private String imagePath;
	private Image originalImage;
	private int imgWidth;
	private int imgHeight;

	private int currentWidth;
	private int currentHeight;

	private Label lblImage;
	private Combo scaleCombo;
	private ScrolledComposite previewPane;
	private Button btnDeviceBounds;
	private Button btnCrop;
	private Button btnCompress;
	private Label lblExpectedImageSize;
	private Label lblType;
	private Label lblResolution;
	
	private int boundX = 0;
	private int boundY = 0;
	
	private byte[] actualBytes;
	private int actualWidth = 0;
	private int actualHeight = 0;
	private Label lblHint;
	
	String imageType = "UNKNOWN";
	private Button btnSaveAsRaw;
	private Button btnAddToResources;

	private Label lblImageName;

	private Button btnCancel;

	private boolean win;
	private Button btnDoNotOutput;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ImportImageDialog(Shell parent, String imagePath) {
		super(parent);

		win = File.separatorChar == '\\';

		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DataLayerView.ORG_ZEFER_LCDPAINTER_RESOURCESVIEW);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
		
		setText(imagePath);
		this.imagePath = imagePath;
		
		try {
			byte[] imageData = ImageChecker.readBinaryData(new FileInputStream(new File(imagePath)));
			imageType = ImageChecker.getImageType(imageData);
		} catch (Exception e) {
			e.printStackTrace();
			DataLayerView.displayError("Image file reading/parsing error:\n" + e.getMessage());
		}
		
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();

        Rectangle shellBounds = getParent().getBounds();
        Point dialogSize = shell.getSize();

        shell.setLocation(
          shellBounds.x + (shellBounds.width - dialogSize.x) / 2,
          shellBounds.y + (shellBounds.height - dialogSize.y) / 2);
        
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
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.PRIMARY_MODAL);
		shell.setMinimumSize(new Point(600, 450));
		shell.setSize(700, 450);
		shell.setText(getText());
		shell.setLayout(new FormLayout());
		Image icon = Util.getImageRegistry(shell.getDisplay()).get("pixelmeister");
		shell.setImage(icon);
		
		previewPane = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
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
						updateImage(btnCrop != null && !btnCrop.isDisposed() && btnCrop.getSelection());
					}
				}
			}
		});

        Label lblSrcImageCaption = new Label(shell, SWT.NONE);
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

        Label label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        FormData fd_label = new FormData();
        fd_label.top = new FormAttachment(lblSrcImageCaption, 6);
        fd_label.left = new FormAttachment(previewPane, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);

        lblImageName = new Label(shell, SWT.NONE);
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
        lblImageName.setText("file.jpg");

        Label lblResolutionCaption = new Label(shell, SWT.NONE);
        FormData fd_lblResolution = new FormData();
        fd_lblResolution.top = new FormAttachment(lblImageName, 10);
        fd_lblResolution.left = new FormAttachment(previewPane, 10);
        lblResolutionCaption.setLayoutData(fd_lblResolution);
        lblResolutionCaption.setText("Resolution:");
        
        lblResolution = new Label(shell, SWT.NONE);
		if ( win ) {
	        lblResolution.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.BOLD));
		} else {
	        lblResolution.setFont(SWTResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		}
        FormData fd_lblx = new FormData();
        fd_lblx.bottom = new FormAttachment(lblResolutionCaption, 0, SWT.BOTTOM);
        fd_lblx.left = new FormAttachment(lblResolutionCaption, 6);
        fd_lblx.right = new FormAttachment(100, -10);
        lblResolution.setLayoutData(fd_lblx);
        
        
        Label lblTypeCaption = new Label(shell, SWT.NONE);
        FormData fd_lblType = new FormData();
        fd_lblType.top = new FormAttachment(lblResolutionCaption, 4);
        fd_lblType.left = new FormAttachment(previewPane, 10);
        lblTypeCaption.setLayoutData(fd_lblType);
        lblTypeCaption.setText("Type:");
        
        lblType = new Label(shell, SWT.NONE);
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
        lblType.setText(imageType);
        
		Label lblZoom = new Label(shell, SWT.NONE);
		FormData fd_lblZoom = new FormData();
		fd_lblZoom.top = new FormAttachment(lblTypeCaption, 6);
		fd_lblZoom.left = new FormAttachment(previewPane, 10);
		lblZoom.setLayoutData(fd_lblZoom);
		lblZoom.setText("Scale:");
		
		scaleCombo = new Combo(shell, SWT.NONE);
		scaleCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boundX = 0;
				boundY = 0;
				
				if ( btnCrop.getSelection() ) {
					btnCrop.setSelection(false);
				}
				
				updateImage(true);
        		if ( (currentWidth > DataLayerView.width || currentHeight > DataLayerView.height) && !btnCrop.getSelection() ) {
            		btnCrop.setEnabled(btnDeviceBounds.getSelection());
                    lblHint.setVisible(btnDeviceBounds.getSelection());
        		} else {
            		btnCrop.setEnabled(false);
                    lblHint.setVisible(false);
        		}
       			previewPane.setFocus();
			}
		});
		FormData fd_scaleCombo = new FormData();
		fd_scaleCombo.top = new FormAttachment(lblZoom, -3, SWT.TOP);
		fd_scaleCombo.left = new FormAttachment(lblType, 0, SWT.LEFT);
		fd_scaleCombo.right = new FormAttachment(100, -10);
		scaleCombo.setLayoutData(fd_scaleCombo);
		String[] items = {"10%", "25%", "50%", "66%", "75%", "100%", "Fit Device", "125%", "150%", "200%", "400%", "800%"};
		scaleCombo.setItems(items);
		scaleCombo.select(5);
		

        Label lblTargetImageCaption = new Label(shell, SWT.NONE);
		if ( win ) {
	        lblTargetImageCaption.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblTargetImageCaption.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        FormData fd_lblTargetImageCaption = new FormData();
		fd_lblTargetImageCaption.top = new FormAttachment(scaleCombo, 20);
		fd_lblTargetImageCaption.left = new FormAttachment(previewPane, 10);
        lblTargetImageCaption.setLayoutData(fd_lblTargetImageCaption);
        lblTargetImageCaption.setText("Resulting Image");

        label = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(lblTargetImageCaption, 6);
        fd_label.left = new FormAttachment(previewPane, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);
		
		

        btnDeviceBounds = new Button(shell, SWT.CHECK);
        btnDeviceBounds.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		updateImage(false);
        		if ( currentWidth > DataLayerView.width || currentHeight > DataLayerView.height ) {
            		btnCrop.setEnabled(((Button)e.widget).getSelection());
                    lblHint.setVisible(((Button)e.widget).getSelection());
        		} else {
            		btnCrop.setEnabled(false);
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
        
        btnCrop = new Button(shell, SWT.CHECK);
        btnCrop.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		updateImage(true);

//        		if ( ((Button)e.widget).getSelection() ) {
//                    lblHint.setVisible(false);
//        		}
        		
        		previewPane.setFocus();
        	}
        });
        btnCrop.setEnabled(false);
        FormData fd_btnCrop = new FormData();
        fd_btnCrop.top = new FormAttachment(btnDeviceBounds, 2);
        fd_btnCrop.left = new FormAttachment(previewPane, 10);
        fd_btnCrop.right = new FormAttachment(100, -10);
        btnCrop.setLayoutData(fd_btnCrop);
        btnCrop.setText("Crop Image");
		
        btnCompress = new Button(shell, SWT.CHECK);
        btnCompress.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		
        		if ( btnCompress.getSelection() ) {
        			btnDoNotOutput.setSelection(false);
        			btnDoNotOutput.setEnabled(false);
        		} else {
        			btnDoNotOutput.setEnabled(true);
        		}
        		
        		updateImage(true);

//        		if ( ((Button)e.widget).getSelection() ) {
//                    lblHint.setVisible(false);
//        		}
        		
        		previewPane.setFocus();
        	}
        });
        btnCompress.setEnabled(true);
        FormData fd_btnCompress = new FormData();
        fd_btnCompress.top = new FormAttachment(btnCrop, 2);
        fd_btnCompress.left = new FormAttachment(previewPane, 10);
        fd_btnCompress.right = new FormAttachment(100, -10);
        btnCompress.setLayoutData(fd_btnCompress);
        btnCompress.setText("Compress Image Data");
		
		Label lblFontDataSizeCaption = new Label(shell, SWT.NONE);
		FormData fd_lblFontDataSizeCaption = new FormData();
		fd_lblFontDataSizeCaption.top = new FormAttachment(btnCompress, 10);
		fd_lblFontDataSizeCaption.left = new FormAttachment(previewPane, 10);
		lblFontDataSizeCaption.setLayoutData(fd_lblFontDataSizeCaption);
		lblFontDataSizeCaption.setText("Estimated Image Size");
		
		
		lblExpectedImageSize = new Label(shell, SWT.NONE);
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

//		
//        
//        lblExpectedImageSize = new Label(shell, SWT.NONE);
//        FormData fd_lblExpectedImageSize = new FormData();
//        fd_lblExpectedImageSize.top = new FormAttachment(previewPane, 6);
//        fd_lblExpectedImageSize.left = new FormAttachment(lblZoom, 0, SWT.LEFT);
//        lblExpectedImageSize.setLayoutData(fd_lblExpectedImageSize);
        
        
        
        if ( actualBytes != null ) {
			lblExpectedImageSize.setText(actualBytes.length + " bytes");
		} else {
	        lblExpectedImageSize.setText("0 bytes");
		}

        lblHint = new Label(shell, SWT.WRAP);
        lblHint.setText("Use arrow keys to precisely move crop bounds, arrows+SHIFT for jumps");
        lblHint.setToolTipText("");
        FormData fd_lblHint = new FormData();
        fd_lblHint.top = new FormAttachment(lblExpectedImageSize, 10);
        fd_lblHint.left = new FormAttachment(previewPane, 7);
        fd_lblHint.right = new FormAttachment(100, -6);
        fd_lblHint.height = 60;
        lblHint.setLayoutData(fd_lblHint);
        lblHint.setVisible(false);
        
		btnAddToResources = new Button(shell, SWT.FLAT);
		btnAddToResources.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				File tmp = new File(imagePath);
				String ntmp = tmp.getName();
				String name = "img_";
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
				
				if ( actualBytes.length > 32767 ) {
					System.out.println("WARNING: array size is too big for some platforms");
				}
				
				if ( btnCompress.getSelection() ) {
					DataLayerView.appendGlobals(Util.formatCompressedImageBytes( true, name, actualBytes, actualWidth, actualHeight,
							(DataLayerView.emulatingLibrary == 0 ? DataLayerView.PIXELS_INSTANCE_NAME : DataLayerView.UTFT_INSTANCE_NAME)));
				} else {
					DataLayerView.appendGlobals(Util.formatImageBytes( true, name, actualBytes, actualWidth, actualHeight,
							(DataLayerView.emulatingLibrary == 0 ? DataLayerView.PIXELS_INSTANCE_NAME : DataLayerView.UTFT_INSTANCE_NAME)));
				}
		        btnCancel.setText("  Close  ");
//				shell.dispose();
			}
		});
		
		FormData fd_btnAddToResources = new FormData();
		fd_btnAddToResources.bottom = new FormAttachment(100, -10);
		fd_btnAddToResources.right = new FormAttachment(100, -10);
		btnAddToResources.setLayoutData(fd_btnAddToResources);
		btnAddToResources.setText("Add To Resources");

        btnCancel = new Button(shell, SWT.FLAT);
        btnCancel.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		if ( originalImage != null && !originalImage.isDisposed() ) {
        			originalImage.dispose();
        		}
        		shell.dispose();
        	}
        });
        FormData fd_btnCancel = new FormData();
        fd_btnCancel.top = new FormAttachment(btnAddToResources, 0, SWT.TOP);
        fd_btnCancel.right = new FormAttachment(btnAddToResources, -4);
//        fd_btnCancel.left = new FormAttachment(previewPane, 10);
        btnCancel.setLayoutData(fd_btnCancel);
        btnCancel.setText("Cancel");

        btnSaveAsRaw = new Button(shell, SWT.FLAT);
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
					DataLayerView.appendGlobals(Util.formatFileReference( true, f, actualBytes.length, actualWidth, actualHeight ));
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
        btnSaveAsRaw.setText("Save as raw data file");

        btnDoNotOutput = new Button(shell, SWT.CHECK);
        btnDoNotOutput.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		DataLayerView.fixUpload = btnDoNotOutput.getSelection();
        		updateImage(true);
        	}
        });
        FormData fd_btnDoNotOutput = new FormData();
        fd_btnDoNotOutput.bottom = new FormAttachment(btnSaveAsRaw, -2);
        fd_btnDoNotOutput.left = new FormAttachment(previewPane, 10);
        btnDoNotOutput.setSelection(DataLayerView.fixUpload);
        btnDoNotOutput.setLayoutData(fd_btnDoNotOutput);
        btnDoNotOutput.setText("Output 0xFFDF instead of 0xFFFF");
        btnDoNotOutput.setToolTipText("Workarounds 'avrdude: verification error, first mismatch at byte 0xXXXX' sketch upload problem");
        
		lblImage = new Label(previewPane, SWT.NONE);
		lblImage.setAlignment(SWT.CENTER);
		lblImage.setText("           ");
		previewPane.setContent(lblImage);

		originalImage = new Image(shell.getDisplay(), imagePath);
		StringTokenizer st = new StringTokenizer(imagePath, "\\/");
		String fileName = "";
		while(st.hasMoreTokens()) {
			fileName = st.nextToken();
		}
		lblImageName.setText(fileName);
		
		Rectangle bnds = originalImage.getBounds();
		imgWidth = bnds.width;
		imgHeight = bnds.height;
		if ( imgWidth > 800 || imgHeight > 600 ) {
			imgWidth = Math.min(800, imgWidth); 
			imgHeight = Math.min(600, imgHeight);
			
			Image i = new Image(shell.getDisplay(), imgWidth, imgHeight);
			
			GC gc = new GC(i);
//			gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
//			gc.fillRectangle(0, 0, imgWidth, imgHeight);
			gc.drawImage(originalImage, 0, 0, imgWidth, imgHeight, 0, 0, imgWidth, imgHeight);
		    gc.dispose();
		    originalImage.dispose();
		    originalImage = i;
		    scaleCombo.remove(8);
		    scaleCombo.remove(8);
		    scaleCombo.remove(8);
		    scaleCombo.remove(8);
		}

		currentWidth = imgWidth;
		currentHeight = imgHeight;

        lblResolution.setText(currentWidth+ "x" + currentHeight);
		
		updateImage(true);
	}
	
	private void updateImage( boolean recompress ) {
		
//		System.out.println("+++");
		
		String s = scaleCombo.getItem(scaleCombo.getSelectionIndex());
		if ( s.endsWith("%") ) {
			s = s.substring(0, s.length()-1).trim();
		}
		
		Image scaled = null;
		if ( "100".equals(s) ) {
			currentWidth = imgWidth;
			currentHeight = imgHeight;
			scaled = new Image(shell.getDisplay(), originalImage, SWT.IMAGE_COPY);
		} else {
			try {
				int i = Integer.parseInt(s);
				currentWidth = imgWidth * i / 100;
				currentHeight = imgHeight * i / 100;
			} catch (Exception e2) {
				double f1 = (double)imgWidth / imgHeight;
				double f2 = (double)DataLayerView.width / DataLayerView.height;
				if ( f1 >= f2 ) {
					currentWidth = DataLayerView.width;
					currentHeight = (int)(DataLayerView.width / f1);
				} else {
					currentHeight = DataLayerView.height;
					currentWidth = (int)(DataLayerView.height * f1);
				}
			}
			scaled = new Image(shell.getDisplay(), originalImage.getImageData().scaledTo(currentWidth, currentHeight));
		}

		if ( btnDeviceBounds.getSelection() ) {
			Image i;
			if ( btnCrop.getSelection() ) {
				i = new Image(shell.getDisplay(), DataLayerView.width, DataLayerView.height);
			} else {
				i = new Image(shell.getDisplay(), Math.max(currentWidth, DataLayerView.width), Math.max(currentHeight, DataLayerView.height));
			}
			
			GC gc = new GC(i);
		    if ( btnCrop.getSelection() && currentWidth >= DataLayerView.width && currentHeight >= DataLayerView.height ) {
		    	gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		    	gc.fillRectangle(0, 0, currentWidth, currentHeight);
	    		gc.drawImage(scaled, boundX, boundY, DataLayerView.width, DataLayerView.height, 0, 0, DataLayerView.width, DataLayerView.height);
		    } else {
		    	gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
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
	    	gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
	    	gc.fillRectangle(0, 0, currentWidth, currentHeight);
		    gc.drawImage(scaled, 0, 0, currentWidth, currentHeight, 0, 0, currentWidth, currentHeight);
		    gc.dispose();
		    scaled.dispose();
		    scaled = i;
		}

		lblImage.setImage(scaled);
		previewPane.setMinSize(lblImage.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if ( currentWidth > DataLayerView.width || currentHeight > DataLayerView.height ) {
            lblHint.setVisible(btnDeviceBounds.getSelection());
		} else {
            lblHint.setVisible(false);
		}
		
		actualBytes = getImageBytes(btnDoNotOutput.getSelection(), recompress, actualBytes);
		if ( lblExpectedImageSize != null ) {
			lblExpectedImageSize.setText(actualBytes.length + " bytes");
		}
	}

	private byte[] getImageBytes( boolean fixit, boolean changed, byte[] oldState ) {
		
		if( !changed && oldState != null && oldState.length != 0 ) {
			return oldState;
		}

		boolean compress = false;
		if ( btnCompress.getSelection() ) {
			compress = true;
		}
		
		String s = scaleCombo.getItem(scaleCombo.getSelectionIndex());
		if ( s.endsWith("%") ) {
			s = s.substring(0, s.length()-1).trim();
		}
		
		int width;
		int height;
		Image scaled;
		if ( "100".equals(s) ) {
			width = imgWidth;
			height = imgHeight;
			scaled = new Image(shell.getDisplay(), originalImage, SWT.IMAGE_COPY);
		} else {
			try {
				int i = Integer.parseInt(s);
				width = imgWidth * i / 100;
				height = imgHeight * i / 100;
			} catch (Exception e2) {
				double f1 = (double)imgWidth / imgHeight;
				double f2 = (double)DataLayerView.width / DataLayerView.height;
				if ( f1 >= f2 ) {
					width = DataLayerView.width;
					height = (int)(DataLayerView.width / f1);
				} else {
					height = DataLayerView.height;
					width = (int)(DataLayerView.height * f1);
				}
			}
			scaled = new Image(shell.getDisplay(), originalImage.getImageData().scaledTo(width, height));
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ImageData id = scaled.getImageData();
		
		int x = 0; 
		int y = 0;
		if ( btnCrop.getSelection() ) {
			x = boundX;
			y = boundY;
			width = Math.min(DataLayerView.width, actualWidth); 
			height = Math.min(DataLayerView.height, actualHeight);
		}

		boolean repeat = true;
		int xlimit = 0;
		int ylimit = 0;
		
		while ( repeat ) {
			repeat = false;
			byte[] bx = new byte[2];
			for ( int j = y; j < height + y; j++  ) {
				for ( int i = x; i < width + x; i++  ) {
					if ( i >= width + x - xlimit || j >= height + y - ylimit ) {
						continue;
					}
					int px = id.getPixel(i, j);
					RGB rgb = id.palette.getRGB(px);
					int px565 = (((rgb.red >> 3) << 11) + ((rgb.green >> 2) << 5) + (rgb.blue >> 3));
					if ( fixit && px565 == 0xffff ) {
						bx[0] = (byte) 0xff;
						bx[1] = (byte) 0xdf;
					} else {
						bx[0] = (byte)((px565 & 0xff00) >> 8);
						bx[1] = (byte)(px565 & 0x00ff);
					}
					try {
						baos.write(bx);
					} catch (OutOfMemoryError e) {
						baos.reset();
						repeat = true;
						xlimit += width/2 + xlimit;
						ylimit += height/2 + ylimit;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		actualWidth = width - xlimit;
		actualHeight = height - ylimit;
		
		if ( compress ) {
			byte[] bb = baos.toByteArray();
			if ( bb == null || bb.length < 8 ) {
				System.err.println("Compression failed! Corrupted image bytes." );
				return bb;
			} 

//			System.out.println("Original: " + bb.length + "b" );

			byte[] result = Compressor.compress( bb, actualWidth, actualHeight );

//			System.out.println("Compressed: " + result.length + "b" );
//			if ( result == null || result.length < 8 ) {
//				System.out.println("Hmm... " + result );
//			} else {
//				System.out.println("Window length: " + (0xFF & result[7]) + "b" );
//			}

			byte[] test = Compressor.uncompress(result);

			if(!Arrays.equals(bb, test)) {
				System.err.println("Compression failed! " + (bb.length != test.length ? bb.length + " vs. " + test.length : "") );
				for ( int i = 0; i < bb.length; i++ ) {
					System.err.print(hex(bb[i]));
					if ( bb[i] != test[i] ) {
						System.err.println( "!= " + hex(test[i]) + "(" + i + ")" );
						break;
					}
				}
			}

			return result;
			
		} else {
			return baos.toByteArray();
		}
	}
	
	private String hex( byte i ) {
		String s = Integer.toHexString(0xFF & i);
		if ( s.length() == 1 ) {
			s = "0" + s;
		}
		return (s + " ").toUpperCase();
	}
}

