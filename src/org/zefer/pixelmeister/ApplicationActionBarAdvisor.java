package org.zefer.pixelmeister;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.zefer.pixelmeister.util.StatusPane;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

//    private IWorkbenchAction newAction;
//    private IWorkbenchAction openAction;
//    private IWorkbenchAction saveAction;
//    private IWorkbenchAction saveAsAction;
//    private IWorkbenchAction runAction;
//    private IWorkbenchAction terminateAction;
//    private IWorkbenchAction importFontAction;
//    private IWorkbenchAction importImageAction;
//    private IWorkbenchAction importImageIconAction;
//    private IWorkbenchAction exportSketchAction;

	// Actions - important to allocate these only in makeActions, and then use
	// them
	// in the fill methods. This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	@Override
	protected void fillStatusLine(IStatusLineManager statusLine) {
		DataLayerView.statusItem = statusLine;
		DataLayerView.statusPane = new StatusPane("status.message", 400);
		DataLayerView.statusPane2 = new StatusPane("status.message2", 200);
	    statusLine.add(DataLayerView.statusPane2);
	    statusLine.add(DataLayerView.statusPane);
	}
	
//	@Override
//	protected void fillCoolBar(ICoolBarManager coolBar) {
//        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
//        
//        coolBar.add(new ToolBarContributionItem(toolbar, "main"));   
//
//        toolbar.add(newAction);
//        toolbar.add(openAction);
//        toolbar.add(saveAction);
//        toolbar.add(saveAsAction);
//        toolbar.add(new Separator());
//        toolbar.add(runAction);
//        toolbar.add(terminateAction);
//        toolbar.add(new Separator());
//        toolbar.add(importImageAction);
//        toolbar.add(importFontAction);
//        toolbar.add(importImageIconAction);
//        toolbar.add(new Separator());
//        toolbar.add(exportSketchAction);
//	}
//	
//	@Override
//	protected void makeActions(IWorkbenchWindow window) {
//		// TODO Auto-generated method stub
//		super.makeActions(window);
//
//		newAction = new NewSketchAction();
//        register(newAction);
//		openAction = new OpenSketchAction();
//        register(openAction);
//		saveAction = new SaveSketchAction();
//        register(saveAction);
//		saveAsAction = new SaveSketchAsAction();
//        register(saveAsAction);
//		runAction = new RunSketchAction();
//        register(runAction);
//		terminateAction = new TerminateSketchAction();
//        register(terminateAction);
//		importImageAction = new ImportImageAction();
//        register(importImageAction);
//		importImageIconAction = new ImportImageIconAction();
//        register(importImageIconAction);
//		importFontAction = new ImportFontAction();
//        register(importFontAction);
//		exportSketchAction = new ExportSketchAction();
//        register(exportSketchAction);
// 	}
}
