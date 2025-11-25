package org.zefer.pixelmeister;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.util.CenteredFileDialog;
import org.zefer.pixelmeister.util.Util;

public class ChooseFontDialog extends Dialog {

	protected Object result;
	protected Shell shlImportFontLocation;
	private Button btnImportSystemFont;
	private Button btnCancel;
	private FormData fd_btnImportSystemFont;
	private Button btnImportWebFont;
	private Text txtAbcabc;
	private Button btnImportFontFrom;
	private List listFonts;
	private Label lblStyle;
	private Button btnItalic;
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ChooseFontDialog(Shell parent, int style) {
		super(parent, style);
		setText("Import Font");
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		// Move the dialog to the center of the top level shell.
        Rectangle shellBounds = getParent().getBounds();
        Point dialogSize = shlImportFontLocation.getSize();

        shlImportFontLocation.setLocation(
          shellBounds.x + (shellBounds.width - dialogSize.x) / 2,
          shellBounds.y + (shellBounds.height - dialogSize.y) / 2);
        
        
        shlImportFontLocation.open();
		shlImportFontLocation.layout();
		Display display = getParent().getDisplay();
		while (!shlImportFontLocation.isDisposed()) {
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
		shlImportFontLocation = new Shell(getParent(), SWT.CLOSE | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shlImportFontLocation.setSize(558, 380);
		shlImportFontLocation.setText("Import Font");
		shlImportFontLocation.setLayout(new FormLayout());

		Image icon = Util.getImageRegistry(shlImportFontLocation.getDisplay()).get("pixelmeister");
		shlImportFontLocation.setImage(icon);

        listFonts = new List(shlImportFontLocation, SWT.BORDER | SWT.V_SCROLL);
        listFonts.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		DataLayerView.lastChosenFont = listFonts.getItem(listFonts.getSelectionIndex());
        		updatePreview();
        	}
        });
        listFonts.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.NORMAL));
        FormData fd_listFonts = new FormData();
        fd_listFonts.bottom = new FormAttachment(100, -140);
        fd_listFonts.right = new FormAttachment(100, -200);
        fd_listFonts.top = new FormAttachment(0, 10);
        fd_listFonts.left = new FormAttachment(0, 10);
        listFonts.setLayoutData(fd_listFonts);

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fontNames = ge.getAvailableFontFamilyNames(Locale.getDefault());
		Arrays.sort(fontNames);
		
		int ind = 0;
		listFonts.setItems(fontNames);
		for ( int i = 0; fontNames != null && i < fontNames.length; i++ ) {
			if ( fontNames[i].equalsIgnoreCase(DataLayerView.lastChosenFont ) ) {
				ind = i;
				break;
			}
		}
		if ( ind == 0 ) {
			DataLayerView.lastChosenFont = fontNames[0];
		}
		listFonts.select(ind);
		listFonts.showSelection();
        
        
        Button btnBold = new Button(shlImportFontLocation, SWT.CHECK);
        btnBold.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		DataLayerView.lastChosenBold = ((Button)e.widget).getSelection();
        		updatePreview();
        	}
        });
        btnBold.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.BOLD));
        FormData fd_btnBold = new FormData();
        fd_btnBold.top = new FormAttachment(listFonts, 6);
        fd_btnBold.right = new FormAttachment(listFonts, -4, SWT.RIGHT);
        btnBold.setLayoutData(fd_btnBold);
        btnBold.setText("Bold");
        btnBold.setSelection(DataLayerView.lastChosenBold);
        
        btnItalic = new Button(shlImportFontLocation, SWT.CHECK);
        btnItalic.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		DataLayerView.lastChosenItalic = ((Button)e.widget).getSelection();
        		updatePreview();
        	}
        });
        btnItalic.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.ITALIC));
        FormData fd_btnItalic = new FormData();
        fd_btnItalic.top = new FormAttachment(listFonts, 6);
        fd_btnItalic.right = new FormAttachment(btnBold, -10);
        btnItalic.setLayoutData(fd_btnItalic);
        btnItalic.setText("Italic");
        btnItalic.setSelection(DataLayerView.lastChosenItalic);

        lblStyle = new Label(shlImportFontLocation, SWT.NONE);
        FormData fd_lblStyle = new FormData();
        fd_lblStyle.top = new FormAttachment(listFonts, 7);
        fd_lblStyle.right = new FormAttachment(btnItalic, -18);
        lblStyle.setLayoutData(fd_lblStyle);
        lblStyle.setText("Style:");

        txtAbcabc = new Text(shlImportFontLocation, SWT.CENTER);
        txtAbcabc.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		DataLayerView.fontTestString = txtAbcabc.getText();
        	}
        });
        txtAbcabc.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
//        txtAbcabc.setFont(SWTResourceManager.getFont(fontNames != null && fontNames.length > 0 ? fontNames[0] : "Arial", 32, SWT.NORMAL));
        txtAbcabc.setText(DataLayerView.fontTestString == null ? "ABCabc01234" : DataLayerView.fontTestString);
        txtAbcabc.setForeground(SWTResourceManager.getColor(SWT.COLOR_TITLE_BACKGROUND));
        FormData fd_txtAbcabc = new FormData();
        fd_txtAbcabc.bottom = new FormAttachment(100, -45);
        fd_txtAbcabc.height = 60;
        fd_txtAbcabc.left = new FormAttachment(listFonts, 0, SWT.LEFT);
        fd_txtAbcabc.right = new FormAttachment(listFonts, 0, SWT.RIGHT);
        txtAbcabc.setLayoutData(fd_txtAbcabc);

		btnImportFontFrom = new Button(shlImportFontLocation, SWT.FLAT);
		btnImportFontFrom.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CenteredFileDialog resourceDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
				resourceDialog.dialog.setFilterExtensions(new String[] { "*.ttf;*.otf;*.ttc" });
				resourceDialog.dialog.setFilterNames(new String[] { "True Type Font (TTF, OTF, TTC)" });
				resourceDialog.dialog.setFilterPath(DataLayerView.recentFontResourcePath);
				shlImportFontLocation.dispose();
				String f = resourceDialog.dialog.open();
				if ( f != null ) {
					DataLayerView.recentFontResourcePath = resourceDialog.dialog.getFilterPath();
					ImportFontDialog ifd = new ImportFontDialog(DataLayerView.getShell(), f);
					ifd.open();
				}
			}
		});
		FormData fd_btnImportFontFrom = new FormData();
		fd_btnImportFontFrom.top = new FormAttachment(0, 10);
		fd_btnImportFontFrom.width = 180;
		fd_btnImportFontFrom.right = new FormAttachment(100, -10);
		btnImportFontFrom.setLayoutData(fd_btnImportFontFrom);
		btnImportFontFrom.setText("Import font from file...");

		btnImportWebFont = new Button(shlImportFontLocation, SWT.FLAT);
		btnImportWebFont.setEnabled(false);
		FormData fd_btnImportWebFont = new FormData();
		fd_btnImportWebFont.top = new FormAttachment(btnImportFontFrom, 10);
		fd_btnImportWebFont.left = new FormAttachment(btnImportFontFrom, 0, SWT.LEFT);
		fd_btnImportWebFont.right = new FormAttachment(btnImportFontFrom, 0, SWT.RIGHT);
		btnImportWebFont.setLayoutData(fd_btnImportWebFont);
		btnImportWebFont.setText("Import Web font...");

		btnImportSystemFont = new Button(shlImportFontLocation, SWT.FLAT);
		btnImportSystemFont.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				java.awt.Font f = new java.awt.Font(DataLayerView.lastChosenFont, Font.PLAIN + (DataLayerView.lastChosenBold ? Font.BOLD : 0)  + (DataLayerView.lastChosenItalic ? Font.ITALIC : 0), 28);
				shlImportFontLocation.dispose();
				ImportFontDialog ifd = new ImportFontDialog(DataLayerView.getShell(), f);
				ifd.open();
			}
		});
		fd_btnImportSystemFont = new FormData();
		fd_btnImportSystemFont.right = new FormAttachment(listFonts, 0, SWT.RIGHT);
		fd_btnImportSystemFont.bottom = new FormAttachment(100, -10);
		fd_btnImportSystemFont.width = 180;
		btnImportSystemFont.setLayoutData(fd_btnImportSystemFont);
		btnImportSystemFont.setText("Open selected");
		
		btnCancel = new Button(shlImportFontLocation, SWT.FLAT);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shlImportFontLocation.dispose();
			}
		});
		FormData fd_btnCancel = new FormData();
		fd_btnCancel.bottom = new FormAttachment(100, -10);
		fd_btnCancel.left = new FormAttachment(btnImportFontFrom, 0, SWT.LEFT);
		fd_btnCancel.right = new FormAttachment(btnImportFontFrom, 0, SWT.RIGHT);
		btnCancel.setLayoutData(fd_btnCancel);
		btnCancel.setText("Cancel");

		updatePreview();
	}

	protected void updatePreview() {
        txtAbcabc.setFont(SWTResourceManager.getFont(DataLayerView.lastChosenFont, 32, SWT.NORMAL + (DataLayerView.lastChosenBold ? SWT.BOLD : 0)  + (DataLayerView.lastChosenItalic ? SWT.ITALIC : 0)));
	}
}
