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

public class ImportFontAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.ImportFontAction";

	public ImportFontAction() {
		setId(ID);
	}

	public void run() {
		ResourcesView.importFontActionHandler();
	}

	public void dispose() {
	}

	@Override
	public String getToolTipText() {
		return "Import Font";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/icons/import_font.png"), null);
			URL fullPathString = FileLocator.toFileURL(codeBase);
			return ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			return super.getImageDescriptor();
		}
	}
}