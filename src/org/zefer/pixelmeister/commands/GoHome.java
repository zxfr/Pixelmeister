package org.zefer.pixelmeister.commands;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class GoHome extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
//		Command command = event.getCommand();
		IWorkbenchBrowserSupport bs = PlatformUI.getWorkbench().getBrowserSupport();		
		try {
			bs.createBrowser(IWorkbenchBrowserSupport.AS_VIEW, "Home", "Home", "").openURL(new URL("http://pd4ml.com/pixelmeister/index.htm"));
		} catch (PartInitException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
