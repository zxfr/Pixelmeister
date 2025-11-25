package org.zefer.pixelmeister.actions;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.Activator;
import org.zefer.pixelmeister.ResourcesView;

public class ImportImageAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.ImportImageAction";

	public ImportImageAction() {
		setId(ID);
	}

	public void run() {
		ResourcesView.importImageActionHandler();
	}

	public void dispose() {
	}
	
	@Override
	public String getToolTipText() {
		return "Import Image";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/icons/import_image.png"), null);
			URL fullPathString = FileLocator.toFileURL(codeBase);
			return ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			return super.getImageDescriptor();
		}
	}
}