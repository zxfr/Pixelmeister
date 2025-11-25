package org.zefer.pixelmeister.commands;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.zefer.pixelmeister.DataLayerView;

public class FindReplaceCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		int target = DataLayerView.getActiveViewIndex();
		if ( target < 0 ) {
			return null;
		}
		DataLayerView.openFindReplaceDialog( null, target );
		return null;
	}
}
