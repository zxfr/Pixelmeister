package org.zefer.pixelmeister;

import java.io.IOException;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class FindReplaceDialog extends Dialog {

	protected Object result;
	protected Shell shell;
	private Combo findCombo;
	private Combo replaceCombo;
	
	private String preselect;
	private FormData fd_btnReplaceAll;
	private Button btnReplace;
	private Button btnReplaceFind;
	private Button btnFind;
	
	private boolean backwards;
	private boolean caseSensitive;
	private boolean wrap = true;
	private boolean inSelection;
	private Button btnReplaceAll;
	private Label messageLabel;
	private PreferenceStore preferenceStore;
	private int dlgX;
	private int dlgY;
	private int dlgW;
	private int dlgH;
	
	private String[] findHistory = {};
	private String[] replaceHistory = {};
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public FindReplaceDialog(Shell parent, int style) {
		super(parent, style);
	}

	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open( String preselect ) {
		
		loadState();
		
		this.preselect = preselect;
		
		createContents();
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
		shell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.RESIZE);
		shell.setMinimumSize(new Point(280, 350));
		shell.setSize(dlgW, dlgH);
        shell.setLocation(dlgX, dlgY);

		shell.setText("Find/Replace");
		shell.setLayout(new FormLayout());
		
		Label lblFind = new Label(shell, SWT.NONE);
		FormData fd_lblFind = new FormData();
		fd_lblFind.left = new FormAttachment(0, 10);
		fd_lblFind.top = new FormAttachment(0, 15);
		lblFind.setLayoutData(fd_lblFind);
		lblFind.setText("Find: ");
		
		Label lblReplaceWith = new Label(shell, SWT.NONE);
		FormData fd_lblReplaceWith = new FormData();
		fd_lblReplaceWith.left = new FormAttachment(0, 10);
		fd_lblReplaceWith.top = new FormAttachment(lblFind, 12, SWT.BOTTOM);
		lblReplaceWith.setLayoutData(fd_lblReplaceWith);
		lblReplaceWith.setText("Replace With: ");

		findCombo = new Combo(shell, SWT.NONE);
		findCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateUIAvailability();
			}
		});
		findCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateUIAvailability();
			}
		});
		FormData fd_findCombo = new FormData();
		findCombo.setLayoutData(fd_findCombo);
		fd_findCombo.top = new FormAttachment(lblFind, -5, SWT.TOP);
		fd_findCombo.left = new FormAttachment(lblReplaceWith, 20, SWT.RIGHT);
		fd_findCombo.right = new FormAttachment(100, -10);
		
		for ( int i = 0; i < findHistory.length; i++ ) {
			findCombo.add(findHistory[i], i);
		}
		
		replaceCombo = new Combo(shell, SWT.NONE);
		replaceCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateUIAvailability();
			}
		});
		replaceCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateUIAvailability();
			}
		});
		fd_lblReplaceWith.right = new FormAttachment(replaceCombo, -20);
		FormData fd_replaceCombo = new FormData();
		fd_replaceCombo.top = new FormAttachment(lblReplaceWith, -5, SWT.TOP);
		fd_replaceCombo.left = new FormAttachment(0, 100);
		fd_replaceCombo.right = new FormAttachment(100, -10);
		replaceCombo.setLayoutData(fd_replaceCombo);

		for ( int i = 0; i < replaceHistory.length; i++ ) {
			replaceCombo.add(replaceHistory[i], i);
		}

		Group grpDirection = new Group(shell, SWT.NONE);
		grpDirection.setText("Direction");
		grpDirection.setLayout(new FormLayout());
		FormData fd_grpDirection = new FormData();
		fd_grpDirection.top = new FormAttachment(replaceCombo, 14);
		fd_grpDirection.left = new FormAttachment(0, 10);
		fd_grpDirection.right = new FormAttachment(50, -5);
		fd_grpDirection.height = 55;
		grpDirection.setLayoutData(fd_grpDirection);
		
		Group grpScope = new Group(shell, SWT.NONE);
		grpScope.setText("Scope");
		grpScope.setLayout(new FormLayout());
		FormData fd_grpScope = new FormData();
		fd_grpScope.right = new FormAttachment(100, -10);
		fd_grpScope.left = new FormAttachment(grpDirection, 10);
		fd_grpScope.top = new FormAttachment(replaceCombo, 14);
		fd_grpScope.height = 55;
		grpScope.setLayoutData(fd_grpScope);
		
		Button all = new Button(grpScope, SWT.RADIO);
		all.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				inSelection = false;
				updateUIAvailability();
			}
		});
		FormData fd_all = new FormData();
		fd_all.top = new FormAttachment(0, 10);
		fd_all.left = new FormAttachment(0, 10);
		all.setLayoutData(fd_all);
		all.setSelection(true);
		all.setText("All");
		
		Button selectedLines = new Button(grpScope, SWT.RADIO);
		selectedLines.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				inSelection = true;
				updateUIAvailability();
			}
		});
		FormData fd_selectedLines = new FormData();
		fd_selectedLines.top = new FormAttachment(0, 30);
		fd_selectedLines.left = new FormAttachment(0, 10);
		selectedLines.setLayoutData(fd_selectedLines);
		selectedLines.setText("Selected Lines");
		
		final Button forward = new Button(grpDirection, SWT.RADIO);
		forward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				backwards = false;
				updateUIAvailability();
			}
		});
		forward.setSelection(true);
		FormData fd_forward = new FormData();
		fd_forward.top = new FormAttachment(0, 10);
		fd_forward.left = new FormAttachment(0, 10);
		forward.setLayoutData(fd_forward);
		forward.setText("Forward");
		
		final Button backward = new Button(grpDirection, SWT.RADIO);
		backward.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				backwards = true;
				updateUIAvailability();
			}
		});
		FormData fd_backward = new FormData();
		fd_backward.top = new FormAttachment(0, 30);
		fd_backward.left = new FormAttachment(0, 10);
		backward.setLayoutData(fd_backward);
		backward.setText("Backward");
		
		
		Group grpOptions = new Group(shell, SWT.NONE);
		grpOptions.setText("Options");
		grpOptions.setLayout(new FormLayout());
		FormData fd_grpOptions = new FormData();
		fd_grpOptions.top = new FormAttachment(grpDirection, 6);
		fd_grpOptions.right = new FormAttachment(100, -10);
		fd_grpOptions.left = new FormAttachment(0, 10);
		fd_grpOptions.height = 35;
		grpOptions.setLayoutData(fd_grpOptions);
		
		final Button btnCaseSensitive = new Button(grpOptions, SWT.CHECK);
		btnCaseSensitive.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				caseSensitive = btnCaseSensitive.getSelection();
				updateUIAvailability();
			}
		});
		FormData fd_btnCaseSensitive = new FormData();
		fd_btnCaseSensitive.top = new FormAttachment(0, 10);
		fd_btnCaseSensitive.left = new FormAttachment(0, 10);
		btnCaseSensitive.setLayoutData(fd_btnCaseSensitive);
		btnCaseSensitive.setText("Case Sensitive");
		
		final Button btnWrapSearch = new Button(grpOptions, SWT.CHECK);
		btnWrapSearch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				wrap = btnWrapSearch.getSelection();
				updateUIAvailability();
			}
		});
		FormData fd_btnWrapSearch = new FormData();
		fd_btnWrapSearch.top = new FormAttachment(0, 10);
		fd_btnWrapSearch.left = new FormAttachment(btnCaseSensitive, 20);
		btnWrapSearch.setLayoutData(fd_btnWrapSearch);
		btnWrapSearch.setSelection(true);
		btnWrapSearch.setText("Wrap Search");

		Button btnClose = new Button(shell, SWT.NONE);
		btnClose.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				onExit();
			}
		});
		FormData fd_btnClose = new FormData();
		fd_btnClose.bottom = new FormAttachment(100, -10);
		fd_btnClose.right = new FormAttachment(100, -10);
		fd_btnClose.width = 100;
		btnClose.setLayoutData(fd_btnClose);
		btnClose.setText("Close");

		messageLabel = new Label(shell, SWT.NONE);
		FormData fd_messageLabel = new FormData();
		fd_messageLabel.bottom = new FormAttachment(btnClose, 0, SWT.BOTTOM);
		fd_messageLabel.left = new FormAttachment(lblFind, 0, SWT.LEFT);
		fd_messageLabel.right = new FormAttachment(btnClose, -10);
		messageLabel.setLayoutData(fd_messageLabel);
		
		
		btnReplaceAll = new Button(shell, SWT.NONE);
		btnReplaceAll.setEnabled(false);
		btnReplaceAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				messageLabel.setText("");
				int amount = DataLayerView.replaceAll(findCombo.getText(), replaceCombo.getText(), inSelection, caseSensitive);
				if ( amount > 1 ) {
					messageLabel.setText(amount +" Matches Replaced");
				} else if ( amount > 0 ) {
					messageLabel.setText("One Match Replaced");
				} else {
					messageLabel.setText("String Not Found");
				}
				updateUIAvailability();
				addComboOptions();
			}
		});
		fd_btnReplaceAll = new FormData();
		fd_btnReplaceAll.bottom = new FormAttachment(btnClose, -6);
		fd_btnReplaceAll.right = new FormAttachment(100, -10);
		fd_btnReplaceAll.width = 100;
		btnReplaceAll.setLayoutData(fd_btnReplaceAll);
		btnReplaceAll.setText("Replace All");
		
		btnReplace = new Button(shell, SWT.NONE);
		btnReplace.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				messageLabel.setText("");
				boolean res = DataLayerView.replace(findCombo.getText(), replaceCombo.getText(), inSelection, caseSensitive);
				if ( !res ) {
					messageLabel.setText("String Not Found");
				}
				updateUIAvailability();
				addComboOptions();
			}
		});
		btnReplace.setEnabled(false);
		FormData fd_btnReplace = new FormData();
		fd_btnReplace.right = new FormAttachment(btnReplaceAll, -6);
		fd_btnReplace.bottom = new FormAttachment(btnClose, -6);
		fd_btnReplace.width = 100;
		btnReplace.setLayoutData(fd_btnReplace);
		btnReplace.setText("Replace");

		btnReplaceFind = new Button(shell, SWT.NONE);
		btnReplaceFind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				messageLabel.setText("");
				boolean res = DataLayerView.replace(findCombo.getText(), replaceCombo.getText(), inSelection, caseSensitive);
				if ( !res ) {
					messageLabel.setText("String Not Found");
				} else {
					res = DataLayerView.findNext(findCombo.getText(), !caseSensitive, backwards, wrap, inSelection);
					if ( !res ) {
						messageLabel.setText("String Not Found");
					}
				}
				updateUIAvailability();
				addComboOptions();
			}
		});
		btnReplaceFind.setEnabled(false);
		FormData fd_btnReplaceFind = new FormData();
		fd_btnReplaceFind.bottom = new FormAttachment(btnReplaceAll, -6);
		fd_btnReplaceFind.right = new FormAttachment(100, -10);
		fd_btnReplaceFind.width = 100;
		btnReplaceFind.setLayoutData(fd_btnReplaceFind);
		btnReplaceFind.setText("Replace/Find");

		btnFind = new Button(shell, SWT.NONE);
		btnFind.setEnabled(false);
		btnFind.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ( findCombo == null || findCombo.isDisposed() ) {
					return;
				}
				
				messageLabel.setText("");
				String pattern = findCombo.getText();
				boolean res = DataLayerView.findNext(pattern, !caseSensitive, backwards, wrap, inSelection);
				if ( !res ) {
					messageLabel.setText("String Not Found");
				}
				updateUIAvailability();
				addComboOptions();
			}
		});
		FormData fd_btnFind = new FormData();
		fd_btnFind.bottom = new FormAttachment(btnReplaceAll, -6);
		fd_btnFind.width = 100;
		fd_btnFind.right = new FormAttachment(btnReplaceFind, -6);
		btnFind.setLayoutData(fd_btnFind);
		btnFind.setText("Find");
		

	    shell.addShellListener(new ShellListener() {
			public void shellClosed(ShellEvent arg0) {
				onExit();
			}
			public void shellDeactivated(ShellEvent arg0) {}
			public void shellDeiconified(ShellEvent arg0) {}
			public void shellIconified(ShellEvent arg0) {}
			public void shellActivated(ShellEvent event) {}
		});
		
		if ( preselect != null ) {
			int ind = preselect.indexOf('\n');
			if ( ind == 0 ) {
				preselect = null;
			} else if ( ind > 0 ) {
				preselect = preselect.substring(0, ind);
			}

			if ( preselect != null ) {
				findCombo.setText(preselect);
				btnFind.setEnabled(true);
			}
		}
	}

	public void addComboOptions() {
		String current = findCombo.getText();
		if ( current.trim().length() > 0 ) {
			String[] items = findCombo.getItems();
			int found = -1;
			for ( int i = 0; i < items.length; i++ ) {
				if ( items[i].equals(current) ) {
					found = i;
					break;
				}
			}
			if ( found < 0 ) {
				findCombo.add(current, 0);
			} else if ( found > 0 ) {
				findCombo.remove(found);
				findCombo.add(current, 0);
			}
			
			if ( findCombo.getItemCount() > 10 ) {
				findCombo.remove(10);
			}
		}

		current = replaceCombo.getText();
		if ( current.trim().length() > 0 ) {
			String[] items = replaceCombo.getItems();
			int found = -1;
			for ( int i = 0; i < items.length; i++ ) {
				if ( items[i].equals(current) ) {
					found = i;
					break;
				}
			}
			if ( found < 0 ) {
				replaceCombo.add(current, 0);
			} else if ( found > 0 ) {
				replaceCombo.remove(found);
				replaceCombo.add(current, 0);
			}
			
			if ( replaceCombo.getItemCount() > 10 ) {
				replaceCombo.remove(10);
			}
		}
	}

	public void updateUIAvailability() {
		
		if ( findCombo == null || findCombo.isDisposed() ) {
			return;
		}
		
		if ( replaceCombo == null || replaceCombo.isDisposed() ) {
			return;
		}
		
		if ( btnFind == null || btnFind.isDisposed() ) {
			return;
		}
		
		if ( btnReplace == null || btnReplace.isDisposed() ) {
			return;
		}
		
		boolean hasFindPattern = findCombo.getText().length() > 0;
		boolean hasReplaceData = replaceCombo.getText().length() > 0;
		boolean pointsToPattern = DataLayerView.pointsToPattern(findCombo.getText(), inSelection, hasReplaceData);
		boolean hasSelection = inSelection ? DataLayerView.hasSelectionScope() : true;
		
		btnFind.setEnabled(hasFindPattern && hasSelection);
		btnReplace.setEnabled(hasFindPattern && hasReplaceData && pointsToPattern && hasSelection);
		btnReplaceFind.setEnabled(hasFindPattern && hasReplaceData && pointsToPattern && hasSelection);
		btnReplaceAll.setEnabled(hasFindPattern && hasReplaceData && hasSelection);
	}

	public void onExit() {
		if ( shell.isDisposed() ) {
			return;
		}
		saveState();
		DataLayerView.closeFindReplaceDialog();
		shell.dispose();
	}

	private void saveState() {
		if ( findCombo == null || findCombo.isDisposed() ) {
			return;
		}
		
		if ( replaceCombo == null || replaceCombo.isDisposed() ) {
			return;
		}
		
		if ( preferenceStore == null ) {
			return;
		}

        Rectangle b = shell.getBounds();
	    preferenceStore.setValue("x", b.x);
	    preferenceStore.setValue("y", b.y);
	    preferenceStore.setValue("width", b.width);
	    preferenceStore.setValue("height", b.height);

	    int num = findCombo.getItemCount();
	    preferenceStore.setValue("find.num", num);
	    for ( int i = 0; i < num; i++ ) {
		    preferenceStore.setValue("find."+i, findCombo.getItem(i));
	    }

	    num = replaceCombo.getItemCount();
	    preferenceStore.setValue("replace.num", num);
	    for ( int i = 0; i < num; i++ ) {
		    preferenceStore.setValue("replace."+i, replaceCombo.getItem(i));
	    }

	    try {
	    	preferenceStore.save();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    preferenceStore = null;
	}

	private void loadState() {
		if ( preferenceStore != null ) {
			return;
		}
		preferenceStore = new PreferenceStore("findreplace.properties");
	    try {
	    	preferenceStore.load();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	    
	    dlgX = preferenceStore.getInt("x");
	    dlgY = preferenceStore.getInt("y");
	    dlgW = preferenceStore.getInt("width");
	    dlgH = preferenceStore.getInt("height");
	    
	    if ( dlgW == 0 ) {
	        Rectangle shellBounds = getParent().getBounds();
			dlgW = 280;
			dlgH = 350;
	        dlgX = shellBounds.x + (shellBounds.width - dlgW) / 2;
	        dlgY = shellBounds.y + (shellBounds.height - dlgH) / 2;
	    } else {
		    int num = preferenceStore.getInt("find.num");
		    findHistory = new String[num];
		    for ( int i = 0; i < num; i++ ) {
		    	findHistory[i] = preferenceStore.getString("find."+i);
		    }

		    num = preferenceStore.getInt("replace.num");
		    replaceHistory = new String[num];
		    for ( int i = 0; i < num; i++ ) {
		    	replaceHistory[i] = preferenceStore.getString("replace."+i);
		    }
	    }
	}

	public void setPattern(String pattern) {
		if ( findCombo == null || findCombo.isDisposed() ) {
			return;
		}
		
		findCombo.setText(pattern);
	}
}
