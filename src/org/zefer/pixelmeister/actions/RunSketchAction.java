package org.zefer.pixelmeister.actions;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.zefer.pixelmeister.Activator;
import org.zefer.pixelmeister.CodeEditView;
import org.zefer.pixelmeister.DataLayerView;

public class RunSketchAction extends Action implements IWorkbenchAction {

	private static final String ID = "org.zefer.pixelmeister.actions.RunSketchAction";

	public RunSketchAction() {
		setId(ID);
	}

	public void run() {
		CodeEditView editorView = null;
		IViewReference[] refs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( DataLayerView.ORG_ZEFER_LCDPAINTER_CODEVIEW.equals(refs[i].getId()) ) {
				editorView = (CodeEditView)refs[i].getView(true);
				editorView.runCode();
				editorView.btnRun.setEnabled(false);
				editorView.btnStop.setEnabled(true);
			}
		}
	}

	public void dispose() {
	}

	@Override
	public String getToolTipText() {
		return "Run Sketch (Ctrl-F11)";
	}
	
	@Override
	public ImageDescriptor getImageDescriptor() {
		try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), new Path("/icons/start_task.gif"), null);
			URL fullPathString = FileLocator.toFileURL(codeBase);
			return ImageDescriptor.createFromURL(fullPathString);
		} catch (Exception e) {
			return super.getImageDescriptor();
		}
	}
}