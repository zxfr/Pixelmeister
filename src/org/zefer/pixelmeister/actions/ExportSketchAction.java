package org.zefer.pixelmeister.actions;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.Activator;
import org.zefer.pixelmeister.DataLayerView;
import org.zefer.pixelmeister.ExportSketchDialog;

public class ExportSketchAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.ExportSketchAction";

	public ExportSketchAction() {
		setId(ID);
	}

	public void run() {
		if (DataLayerView.dirty) {
			if( DataLayerView.prompt("There is unsaved content in editor(s). Save it?     ") == SWT.YES ) {
				DataLayerView.save(DataLayerView.currentSketch, true);
			}
		}

		ExportSketchDialog esd = new ExportSketchDialog(DataLayerView.getShell(), SWT.NONE);
		esd.open();
	}

	public void dispose() {
	}

	@Override
	public String getToolTipText() {
		return "Export Sketch Code";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/icons/export_wiz.gif"), null);
			URL fullPathString = FileLocator.toFileURL(codeBase);
			return ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			return super.getImageDescriptor();
		}
	}
}