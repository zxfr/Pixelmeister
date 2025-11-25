package org.zefer.pixelmeister;

import java.io.File;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.util.Util;

public class ConsoleView extends ViewPart {

	public StyledText console;
    StringBuffer sb = new StringBuffer();
	
	public ConsoleView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FormLayout());

		Font font = new Font(parent.getDisplay(), "Courier New", File.pathSeparatorChar == ':' ? 12 : 10, SWT.NORMAL);
		Color bg = SWTResourceManager.getColor(255,255,255);
		
		Button btnClear = new Button(parent, SWT.FLAT);
		btnClear.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if ( console != null && !console.isDisposed() ) {
					console.setText("");
				}
			}
		});
		FormData fd_btnClear = new FormData();
		fd_btnClear.bottom = new FormAttachment(100, -2);
		fd_btnClear.right = new FormAttachment(100, -10);
		fd_btnClear.height = 24;
		btnClear.setLayoutData(fd_btnClear);
		btnClear.setText("  Clear  ");
		btnClear.setImage(Util.getImageRegistry(btnClear.getDisplay()).get("clear"));
		
		console = new StyledText(parent, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		console.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				if (event.keyCode == 'q' && (event.stateMask & SWT.CTRL) != 0 ) {
					System.out.println("********************************************************************");
					int n = 0;
					final StringBuilder dump = new StringBuilder();
					final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
					final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
					for (ThreadInfo threadInfo : threadInfos) {
			            dump.append('"');
			            dump.append(threadInfo.getThreadName());
			            dump.append("\" ");
			            final Thread.State state = threadInfo.getThreadState();
			            dump.append("\n   java.lang.Thread.State: ");
			            dump.append(state);
			            final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
			            for (final StackTraceElement stackTraceElement : stackTraceElements) {
			                dump.append("\n        at ");
			                dump.append(stackTraceElement);
			            }
			            dump.append("\n\n");
			            n++;
			        }
			        System.out.println( dump.toString() );
					System.out.println("" + n + " threads");
			  		return;
				}
			}
		});
		FormData fd_styledText = new FormData();
		fd_styledText.top = new FormAttachment(0, 10);
		fd_styledText.left = new FormAttachment(0, 10);
		fd_styledText.bottom = new FormAttachment(btnClear, -2);
		fd_styledText.right = new FormAttachment(100, -10);

		console.setFont(font);
		console.setBackground(bg);
		console.setLayoutData(fd_styledText);
		console.setLeftMargin(5);
		
		final PrintStream oldErr = System.err;
		final PrintStream oldStd = System.out;
		PrintStream console1 = new TeeStream(oldStd);
		PrintStream console2 = new TeeStream(oldErr);
		System.setOut(console1);
		System.setErr(console2);
		console.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				System.setErr(oldErr);
				System.setOut(oldStd);
			}
		});
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public class TeeStream extends PrintStream {

	    public TeeStream(PrintStream out1) {
	        super(out1);
	    }

	    public void write(byte buf[], int off, int len) {

			if (console == null || console.isDisposed()) {
				return;
			}

	    	final byte[] b = buf;
	    	final int o = off;
	    	final int l = len;

	    	sb.append( new String(b, o, l) );
	    	
	    	console.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (console == null || console.isDisposed()) {
						return;
					}

					console.append(sb.toString());
					sb = new StringBuffer();
					console.setTopIndex(console.getLineCount() - 1);
				}
			});
	    	
	    	try {
//	            super.write(buf, off, len);
	            out.write(buf, off, len);
	        } catch (Exception e) {
	        }
	    }

	    public void flush() {
	        super.flush();
	    }
	}
}
