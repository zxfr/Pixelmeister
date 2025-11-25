package org.zefer.pixelmeister;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.util.CenteredFileDialog;
import org.zefer.pixelmeister.util.Util;

public class ExportSketchDialog extends Dialog {

	private static final String FLAG_WIRING = "//:Wiring";
	private static final String FLAG_JAVA = "//:Java";
	private static final String FLAG_PIXELMEISTER = "//:PIXELMEISTER";
	private static final String FLAG_PXS = "//:Pxs";
	protected Object result;
	protected Shell shell;
	private StyledText sketchCode;
	protected static CLineStyler exportLineStyler = new CLineStyler();
	
	private String template;
	private Button btnSaveAs;
	private Button btnMoveCodeTo;

	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ExportSketchDialog(Shell parent, int style) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(DataLayerView.ORG_ZEFER_LCDPAINTER_RESOURCESVIEW);
		} catch (PartInitException e1) {
			e1.printStackTrace();
		}
		
		URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), 
				new Path("/" + (DataLayerView.emulatingLibrary == 0 ? DataLayerView.EXPORT_PIXELS_TEMPLATE_FILE_NAME : DataLayerView.EXPORT_TEMPLATE_FILE_NAME)), null);

		try {
			URL classesUrl = FileLocator.toFileURL(codeBase);

			if (classesUrl != null && classesUrl.getPath().length() > 0 ) {
				File file = new File(classesUrl.getPath());
				if (!file.exists()) {
					String message = "Cannot open template file: " + file.getAbsolutePath();
					DataLayerView.displayError(message);
					System.out.println(message);
				} else {
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
							template = buffer.toString();
							stream.close();
						} catch (IOException e) {
							// Err_file_io
							String message = "File read error" + file.getName();
							DataLayerView.displayError(message);
							System.out.println(message);
						}
					} catch (FileNotFoundException e) {
						String message = "File not found" + file.getName();
						DataLayerView.displayError(message);
						System.out.println(message);
					}
				}
			}
		} catch (IOException e) {
			DataLayerView.displayError(e.getMessage());
			e.printStackTrace();
		}
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
		shell = new Shell(getParent(), getStyle());
		shell.setSize(800, 600);
		shell.setText("Export Sketch");
		shell.setLayout(new FormLayout());
		Image icon = Util.getImageRegistry(shell.getDisplay()).get("pixelmeister");
		shell.setImage(icon);
		final Clipboard cb = new Clipboard(getParent().getDisplay());

		Font font = new Font(shell.getDisplay(), "Courier New", File.pathSeparatorChar == ':' ? 12 : 10, SWT.NORMAL);
		Color bg = SWTResourceManager.getColor(255,255,255);
		
		sketchCode = new StyledText(shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		FormData fd_styledText = new FormData();
		fd_styledText.bottom = new FormAttachment(100, -40);
		fd_styledText.right = new FormAttachment(100, -10);
		fd_styledText.top = new FormAttachment(0, 10);
		fd_styledText.left = new FormAttachment(0, 10);
		sketchCode.setLayoutData(fd_styledText);
		
		sketchCode.addLineStyleListener(new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent e) {
				e.bulletIndex = sketchCode.getLineAtOffset(e.lineOffset);          
				int rulerWidth = Integer.toString(sketchCode.getLineCount()+1).length() * 12;
				StyleRange style = new StyleRange();         
				style.metrics = new GlyphMetrics(0, 0, rulerWidth);     
				style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				e.bullet = new Bullet(ST.BULLET_NUMBER, style);     
		} });
		
		sketchCode.addLineStyleListener(exportLineStyler);
		sketchCode.setFont(font);
		sketchCode.setBackground(bg);
		sketchCode.setLeftMargin(5);

        Label lblTargetPlatform = new Label(shell, SWT.NONE);
        FormData fd_lblTargetPlatform = new FormData();
        fd_lblTargetPlatform.top = new FormAttachment(sketchCode, 8);
        fd_lblTargetPlatform.left = new FormAttachment(sketchCode, 0, SWT.LEFT);
        lblTargetPlatform.setLayoutData(fd_lblTargetPlatform);
        lblTargetPlatform.setText("Target Platform:");
        
        Combo combo = new Combo(shell, SWT.NONE);
        combo.setItems(DataLayerView.exportTargetPlatforms);
        combo.select(DataLayerView.exportTargetPlatform);
        combo.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		DataLayerView.exportTargetPlatform = ((Combo)e.widget).getSelectionIndex();
        		sketchCode.setText(populate(template));
        	}
        });
        FormData fd_combo = new FormData();
        fd_combo.top = new FormAttachment(sketchCode, 6);
        fd_combo.left = new FormAttachment(lblTargetPlatform, 6);
        combo.setLayoutData(fd_combo);
        
        btnMoveCodeTo = new Button(shell, SWT.CHECK);
        btnMoveCodeTo.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		DataLayerView.exportToSetupSection = ((Button)e.widget).getSelection();
        		sketchCode.setText(populate(template));
        	}
        });
        FormData fd_btnMoveCodeTo = new FormData();
        fd_btnMoveCodeTo.top = new FormAttachment(sketchCode, 8);
        fd_btnMoveCodeTo.left = new FormAttachment(combo, 40);
        btnMoveCodeTo.setLayoutData(fd_btnMoveCodeTo);
        btnMoveCodeTo.setText("Export code to setup() method");
        btnMoveCodeTo.setSelection(DataLayerView.exportToSetupSection);
		
		sketchCode.setEditable(false);
		sketchCode.setText(populate(template));
		
		exportLineStyler.parseBlockComments(sketchCode.getText());

		
		btnSaveAs = new Button(shell, SWT.FLAT);
		btnSaveAs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				save(null, false);
			}
		});
		FormData fd_btnSaveAs = new FormData();
		fd_btnSaveAs.top = new FormAttachment(sketchCode, 6);
		fd_btnSaveAs.right = new FormAttachment(100, -10);
		btnSaveAs.setLayoutData(fd_btnSaveAs);
		btnSaveAs.setText("Save As...");
		btnSaveAs.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));
		
		Button btnCopyToClipboard = new Button(shell, SWT.FLAT);
		FormData fd_btnCopyToClipboard = new FormData();
		fd_btnCopyToClipboard.top = new FormAttachment(btnSaveAs, 0, SWT.TOP);
		fd_btnCopyToClipboard.right = new FormAttachment(btnSaveAs, -6);
		btnCopyToClipboard.setLayoutData(fd_btnCopyToClipboard);
		btnCopyToClipboard.setText("Copy To Clipboard");
		btnCopyToClipboard.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY));
		btnCopyToClipboard.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (sketchCode.getText().length() > 0) {
					TextTransfer textTransfer = TextTransfer.getInstance();
					cb.setContents(new Object[]{sketchCode.getText()}, new Transfer[]{textTransfer});
				}
			}
		});
	}

	private String populate(String template) {
		
		String protos = "";
		
		String sketch = DataLayerView.sketchCode.getText();
//		sketch = Util.replaceString(sketch, "\n", "\n\t\t");
//		sketch = "\t\t" + sketch.trim();

		StringBuffer setup = new StringBuffer();
		
		BufferedReader reader = new BufferedReader(new StringReader(sketch));
		try {
			boolean ppsection = false;

			String line;
			StringBuffer sb = new StringBuffer();
			while( (line = reader.readLine()) != null ) {

				String l = line.trim();
				if ( l.length() > 0 && l.charAt(0) == '#' ) {
					if ( l.startsWith("#ifdef") ) {
						String key = l.substring(6).trim();
						if ( "EXPORT_TO_SETUP_METHOD".equals(key) ) {
							ppsection = true;
							sb.append(DataLayerView.convertToBlanks(line)).append('\n');
							continue;
						}
					} else if (l.startsWith("#endif")) {
						if ( ppsection ) {
							ppsection = false;
							continue;
						}
					}
				} 

				if ( ppsection ) {
					sb.append(DataLayerView.convertToBlanks(line)).append('\n');
					setup.append("\t\t").append(line).append("\n");
					continue;
				}
				
				if ( line.indexOf(FLAG_WIRING) > 0 ) {
					line = line.trim();
					if ( line.startsWith("//") ) {
						line = line.substring(2).trim();
					}
				}

				if ( line.indexOf(FLAG_JAVA) > 0 || line.indexOf(FLAG_PIXELMEISTER) > 0 || line.indexOf(FLAG_PXS) > 0 ) {
					line = "// " + line;
				}

				sb.append("\t\t").append(line).append("\n");
			}
			sketch = sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sketch = "\t\t" + sketch.trim();
		
		
		if ( setup.length() > 0 ) {
			btnMoveCodeTo.setEnabled(false);
			template = Util.replaceString(template, "$[sketchcode_s]", setup.toString());
			template = Util.replaceString(template, "$[sketchcode_l]", sketch);
		} else {
			btnMoveCodeTo.setEnabled(true);
			if ( DataLayerView.exportToSetupSection ) {
				template = Util.replaceString(template, "$[sketchcode_s]", sketch);
				template = Util.replaceString(template, "$[sketchcode_l]", "");
			} else {
				template = Util.replaceString(template, "$[sketchcode_s]", "");
				template = Util.replaceString(template, "$[sketchcode_l]", sketch);
			}
		}

		if (DataLayerView.oneBitColor) {
			template = Util.replaceString(template, "$[onebit]", "#include <Pixels_Monochrome.h>");
			template = Util.replaceString(template, "$[antialiasing]\n", "");
		} else {
			template = Util.replaceString(template, "$[onebit]\n", "");
			template = Util.replaceString(template, "$[antialiasing]", "#include <Pixels_Antialiasing.h> // optional (a removal does not impact fonts antialiasing)");
		}
		
		template = Util.replaceString(template, "$[hint]", DataLayerView.codingHint);
		
		template = Util.replaceString(template, "$[width]", ""+DataLayerView.width);
		template = Util.replaceString(template, "$[height]", ""+DataLayerView.height);

		template = Util.replaceString(template, "$[controller]", ""+DataLayerView.controller);
		template = Util.replaceString(template, "$[iface]", ""+DataLayerView.iface);

		template = Util.replaceString(template, "$[orientation]", DataLayerView.landscapeViewer ? "LANDSCAPE" : "PORTRAIT");
		
		String resources = DataLayerView.globalsCode.getText();

		reader = new BufferedReader(new StringReader(resources));
		
		String functionsAndClasses = "";
		
		try {
			boolean resourceBegin = false;
			String line;
			StringBuffer functions = new StringBuffer();
			StringBuffer resourceData = new StringBuffer();
			while( (line = reader.readLine()) != null ) {
				
				if ( line.indexOf(FLAG_WIRING) > 0 ) {
					line = line.trim();
					if ( line.startsWith("//") ) {
						line = line.replaceFirst("\\/\\/", "");
					}
					
					if ( line.indexOf("PROGMEM") > 0 ) {
						String proto = line;
						int i = proto.indexOf(FLAG_WIRING);
						proto = proto.substring(0, i); 
						i = proto.lastIndexOf("{");
						if ( i > 0 ) {
							proto = proto.substring(0, i); 
						}
						i = proto.lastIndexOf("=");
						if ( i > 0 ) {
							proto = proto.substring(0, i); 
						}
						proto = proto.trim();
						resourceBegin = true;
						protos += (i > 0 ? "extern " : "") + proto + ";\n";
					}
				}

				if ( line.indexOf(FLAG_JAVA) > 0 || line.indexOf(FLAG_PIXELMEISTER) > 0 || line.indexOf(FLAG_PXS) > 0 ) {
					line = "// " + line;
				}

				if ( resourceBegin ) {
					resourceData.append("\t").append(line).append("\n");
				} else {
					functions.append("\t").append(line).append("\n");
				}
			}
			resources = resourceData.toString();
			functionsAndClasses = functions.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		resources = "\t" + resources.trim();

		
//		if ( resources.indexOf("::") > 0 && resources.indexOf("class") > 0 ) {
//			protos = resources;
//			resources = "";
//		}
		
		template = Util.replaceString(template, "$[functions]", functionsAndClasses);
		
		template = Util.replaceString(template, "$[resources]", resources);

		template = Util.replaceString(template, "$[declarations]", protos);

		template = Util.replaceString(template, "$slowWhile()", "1");
		return template;
	}

	public void save(String name, boolean force) {
		
		if ( name == null ) {
			CenteredFileDialog saveSketchDialog = new CenteredFileDialog(shell, SWT.SAVE);
			saveSketchDialog.dialog.setFilterExtensions(new String[] { "*.ino" });
			saveSketchDialog.dialog.setFilterNames(new String[] { "Arduino Sketch (.INO)" });
			saveSketchDialog.dialog.setFilterPath(DataLayerView.recentExportSketchPath);
			saveSketchDialog.dialog.setFileName(DataLayerView.currentExportSketch);

			String f = saveSketchDialog.dialog.open();
			if ( f != null ) {
				name = f;
				DataLayerView.currentExportSketch = f;
				DataLayerView.recentExportSketchPath = saveSketchDialog.dialog.getFilterPath();
			} else {
				return;
			}
		}
		
		File file = new File(name);
		if (file.exists() && !force) {
			String message = "File " + file.getAbsolutePath() + " already exists. Overwrite?";
			int res = DataLayerView.prompt(message);
			if ( res == SWT.NO ) {
				return;
			}
		}

		try {
			FileOutputStream fos = new FileOutputStream(file);
			String str = sketchCode == null ? "" : sketchCode.getText();
			fos.write(str.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
