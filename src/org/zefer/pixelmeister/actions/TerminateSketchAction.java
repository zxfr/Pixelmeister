package org.zefer.pixelmeister.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.CodeEditView;
import org.zefer.pixelmeister.DataLayerView;

public class TerminateSketchAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.TerminateSketchAction";

	public TerminateSketchAction() {
		setId(ID);
	}

	public void run() {
		CodeEditView editorView = null;
		IViewReference[] refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( DataLayerView.ORG_ZEFER_LCDPAINTER_CODEVIEW.equals(refs[i].getId()) ) {
				editorView = (CodeEditView)refs[i].getView(true);
				editorView.btnRun.setEnabled(true);
				editorView.btnStop.setEnabled(false);
			}
		}
		
		DataLayerView.stop = true;
		DataLayerView.instance = null;
	}

	public void dispose() {
	}

	@Override
	public boolean isEnabled() {
		return !DataLayerView.stop;
	}
	
	@Override
	public String getToolTipText() {
		return "Terminate Running Sketch";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		if ( DataLayerView.stop ) { 
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP_DISABLED);
		} else {
			return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP);
		}
	}
}