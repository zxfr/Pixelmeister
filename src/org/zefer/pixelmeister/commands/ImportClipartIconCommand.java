package org.zefer.pixelmeister.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.zefer.pixelmeister.ResourcesView;

public class ImportClipartIconCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ResourcesView.importClipartIconActionHandler();
		return null;
	}
}
