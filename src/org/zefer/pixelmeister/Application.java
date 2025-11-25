package org.zefer.pixelmeister;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.Preferences;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {

	public static final String LCD_PAINTER = "Pixelmeister";
	
	public static boolean guiHanderlsAllocationDebug = false;

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) {
		
	    Display display;
	    
	    if (guiHanderlsAllocationDebug) {
		    DeviceData data = new DeviceData();
		    data.tracking = true;
	    	display = new Display(data);
//		    Sleak sleak = new Sleak();  // GUI handlers debug window
//		    sleak.open();
	    } else {
	    	display = PlatformUI.createDisplay();
	    }

		Preferences preferences = ConfigurationScope.INSTANCE.getNode(LCD_PAINTER);
		Preferences sub1 = preferences.node("licensing");
//		sub1.clear();
//		sub1.flush();
//		DataLayerView.currentSketch = sub1.get("sketch", null);

		try {
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		} finally {
			
			DataLayerView.terminatePrecompilerThreads();
			DataLayerView.terminateThreads();
			
			display.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (!display.isDisposed())
					workbench.close();
			}
		});
	}
}
