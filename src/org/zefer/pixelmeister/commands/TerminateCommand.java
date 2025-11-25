package org.zefer.pixelmeister.commands;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.zefer.pixelmeister.CodeEditView;
import org.zefer.pixelmeister.DataLayerView;

public class TerminateCommand extends AbstractHandler implements IElementUpdater {
	
	final public static String ID = "org.zefer.pixelmeister.command.terminate";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		DataLayerView.stop = true;
		DataLayerView.instance = null;

		CodeEditView editorView = null;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IViewReference[] refs = window.getActivePage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( DataLayerView.ORG_ZEFER_LCDPAINTER_CODEVIEW.equals(refs[i].getId()) ) {
				editorView = (CodeEditView)refs[i].getView(true);
				editorView.btnRun.setEnabled(true);
				editorView.btnStop.setEnabled(false);
			}
		}
		
		ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
		if (commandService != null) {
			commandService.refreshElements(RunCommand.ID, null);
			commandService.refreshElements(TerminateCommand.ID, null);
		}

		DataLayerView.terminateThreads();
		
		return null;
	}

	@Override
	public void updateElement(UIElement element, Map parameters) {
		element.setText("");
	}
}
