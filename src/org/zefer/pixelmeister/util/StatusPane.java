package org.zefer.pixelmeister.util;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class StatusPane extends ControlContribution {
	private CLabel label;
	private int width;

	public StatusPane(String id, int width) {
		super(id);
		this.width = width;
	}

	protected Control createControl(Composite parent) {

		int widthHint = width;
		int heightHint = 0;
		
		new Label(parent, SWT.SEPARATOR);
		label = new CLabel(parent, SWT.SHADOW_NONE);

		GC gc = new GC(parent);
		gc.setFont(parent.getFont());
		FontMetrics fm = gc.getFontMetrics();
		heightHint = fm.getHeight();
		gc.dispose();

		StatusLineLayoutData statusLineLayoutData = new StatusLineLayoutData();
		statusLineLayoutData.widthHint = widthHint;
		statusLineLayoutData.heightHint = heightHint;
		label.setLayoutData(statusLineLayoutData);
//		label.setImage(Util.getImageRegistry(parent.getShell().getDisplay()).get("error"));
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				String s = label.getToolTipText();
				if ( s != null ) {
					System.out.println(s);
				}
			}
		});
		
//		Button butt = new Button(parent, SWT.CHECK);
//		butt.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//			}
//		});
//		statusLineLayoutData = new StatusLineLayoutData();
//		statusLineLayoutData.widthHint = 50;
//		statusLineLayoutData.heightHint = heightHint;
//		butt.setLayoutData(statusLineLayoutData);
		  
		return null;
	}
	
	public void setMessage( Image errorIcon, String text, String fullText ) {
			label.setText(text);
			label.setToolTipText(fullText);
			label.setImage(errorIcon);
	}
}
