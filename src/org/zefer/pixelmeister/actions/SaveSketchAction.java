package org.zefer.pixelmeister.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.DataLayerView;

public class SaveSketchAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.SaveSketchAction";

	public SaveSketchAction() {
		setId(ID);
	}

	public void run() {
		DataLayerView.save(DataLayerView.currentSketch, true);
	}

	public void dispose() {
	}

	@Override
	public String getToolTipText() {
		return "Save Sketch";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT);
	}
}