package org.zefer.pixelmeister.commands;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.zefer.pixelmeister.DataLayerView;
import org.zefer.pixelmeister.ExportSketchDialog;

public class ExportSketchCommand extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (DataLayerView.dirty) {
			if( DataLayerView.prompt("There is unsaved content in editor(s). Save it?     ") == SWT.YES ) {
				DataLayerView.save(DataLayerView.currentSketch, true);
			}
		}

		ExportSketchDialog esd = new ExportSketchDialog(DataLayerView.getShell(), SWT.NONE);
		esd.open();
		return null;
	}
}
