package org.zefer.pixelmeister.commands;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.zefer.pixelmeister.DataLayerView;


public class UndoCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		DataLayerView.undo(false);
		return null;
	}
}
