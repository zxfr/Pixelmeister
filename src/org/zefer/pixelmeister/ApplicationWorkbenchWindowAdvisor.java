package org.zefer.pixelmeister;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1000, 730));
		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
	}
	
	@Override
	public void postWindowOpen() {
		super.postWindowOpen();
		
//		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().
		
//		ArrayList list =  (ArrayList)getWindowConfigurer().getWindow().getShell().getData("org.eclipse.ui.internal.dnd.dropTarget"); 
//		ViewSashContainer viewSash = (ViewSashContainer) list.get(2);
//		Composite composite = viewSash.getParent();
//		Util.setForeground( PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(), composite, "org.zefer.darkertheme.HIGHLIGHT" );
	}
	
	@Override
	public boolean preWindowShellClose() {
		boolean ret = super.preWindowShellClose();
		if ( DataLayerView.findReplaceDialog != null ) {
			DataLayerView.findReplaceDialog.onExit();
		}
		return ret;
	}
}
