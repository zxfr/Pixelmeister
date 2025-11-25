package org.zefer.pixelmeister.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.zefer.pixelmeister.DataLayerView;

public class AutosaveCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Command command = event.getCommand();
		DataLayerView.autosave = !HandlerUtil.toggleCommandState(command); 
		DataLayerView.updateStatusLine();
		return null;
	}


}
