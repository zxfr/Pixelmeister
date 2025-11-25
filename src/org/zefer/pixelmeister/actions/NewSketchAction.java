package org.zefer.pixelmeister.actions;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.Activator;
import org.zefer.pixelmeister.DataLayerView;

public class NewSketchAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.NewSketchAction";

	public NewSketchAction() {
		setId(ID);
	}

	public void run() {
		newSketchAction();
	}

	public void dispose() {
	}

	public static Object newSketchAction() {
		if (DataLayerView.dirty) {
			if( DataLayerView.prompt("There is unsaved content in editor(s). Save it?     ") == SWT.YES ) {
				DataLayerView.save(DataLayerView.currentSketch, true);
			}
		}
		
        try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), 
					new Path("/" + (DataLayerView.emulatingLibrary == 0 ? 
							DataLayerView.TEMPLATE_FILE_NAME : DataLayerView.UTEXT_TEMPLATE_FILE_NAME)), null);
			URL classesUrl = FileLocator.toFileURL(codeBase);
			DataLayerView.loadSketch( classesUrl.getPath(), true );
			DataLayerView.currentSketch = null;
		} catch (IOException e) {
			e.printStackTrace();
		}		

        DataLayerView.getShell().setText("Pixelmeister");

		return null;
	}

	@Override
	public String getToolTipText() {
		return "New Sketch";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_NEW_WIZARD);
	}
}