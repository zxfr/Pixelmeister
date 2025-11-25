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

public class ImportImageIconAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.ImportImageIconAction";

	public ImportImageIconAction() {
		setId(ID);
	}

	public void run() {
		ResourcesView.importClipartIconActionHandler();
	}

	public void dispose() {
	}
	
	@Override
	public String getToolTipText() {
		return "Import Icon from Clipart";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/icons/icons.gif"), null);
			URL fullPathString = FileLocator.toFileURL(codeBase);
			return ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			return super.getImageDescriptor();
		}
	}
}