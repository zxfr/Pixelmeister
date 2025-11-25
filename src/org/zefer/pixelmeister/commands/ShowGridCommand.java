package org.zefer.pixelmeister.commands;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.zefer.pixelmeister.DataLayerView;

public class ShowGridCommand extends AbstractHandler {
	public static final String ID = "org.zefer.pixelmeister.view.show.grid";
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Command command = event.getCommand();
		DataLayerView.grid = !HandlerUtil.toggleCommandState(command); 
		DataLayerView.updateDeviceView();
		return null;
	}
}
