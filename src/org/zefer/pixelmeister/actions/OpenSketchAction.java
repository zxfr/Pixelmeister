package org.zefer.pixelmeister.actions;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.Activator;
import org.zefer.pixelmeister.DataLayerView;
import org.zefer.pixelmeister.util.CenteredFileDialog;

public class OpenSketchAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.SaveSketchAction";

	public OpenSketchAction() {
		setId(ID);
	}

	public void run() {
		open();
	}

	public static void open() {
		if (DataLayerView.dirty) {
			if( DataLayerView.prompt("There is unsaved content in editor(s). Save it?     ") == SWT.YES ) {
				DataLayerView.save(DataLayerView.currentSketch, true);
			}
		}
		
		CenteredFileDialog openSketchDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
		openSketchDialog.dialog.setFilterExtensions(new String[] { "*.scq" });
		openSketchDialog.dialog.setFilterNames(new String[] { "LCD Sketch (.SCQ)" });
		openSketchDialog.dialog.setFilterPath(DataLayerView.recentSketchPath);

		String f = (String)openSketchDialog.open();
		if ( f != null ) {
			DataLayerView.currentSketch = f;
			DataLayerView.recentSketchPath = openSketchDialog.dialog.getFilterPath();
			DataLayerView.loadSketch( f, false );		
		}
	}

	public void dispose() {
	}

	@Override
	public String getToolTipText() {
		return "Open Sketch";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/icons/open_file.png"), null);
			URL fullPathString = FileLocator.toFileURL(codeBase);
			return ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			return super.getImageDescriptor();
		}
	}
}