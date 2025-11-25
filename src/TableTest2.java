import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.util.TrueType;

public class TableTest2 {
	
	private static final String path = "o:/tmp/Dingleberries.TTF";
	private static Text text;
//	private static final String path = "c:/windows/fonts/arial.TTF";
//	private static final String path = "c:/windows/fonts/simhei.TTF";
	
	public static void main(String[] args) throws Exception {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setSize(240, 500);
		shell.setText("Glyph picker");
		shell.setLayout(new FillLayout());

		
		File file = new File(path);
		boolean loadFont = Display.getCurrent().loadFont(file.toString());
		System.out.println(loadFont);
		
		TrueType tt = new TrueType(path);
		System.out.println(tt.getFullName());
		System.out.println(tt.getRanges());
		
		final Composite glyphPicker = new Composite(shell, SWT.NONE);

		glyphPicker.setLayout(new FormLayout());

		final Table table = new Table(glyphPicker, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

		FormData fd_table = new FormData();
		fd_table.bottom = new FormAttachment(100, -40);
		fd_table.right = new FormAttachment(100, -10);
		fd_table.top = new FormAttachment(0, 10);
		fd_table.left = new FormAttachment(0, 10);
		table.setLayoutData(fd_table);

		table.setHeaderVisible(false);
		table.setFont(SWTResourceManager.getFont(tt.getFullName(), 32, SWT.NORMAL));

		final TableColumn column = new TableColumn(table, SWT.NULL);

		ArrayList l = tt.getGlyphList();
		Iterator ii = l.iterator();
		while( ii.hasNext() ) {
			Integer i = (Integer)ii.next();
			if ( i.intValue() == 32 ) {
				continue;
			}
			TableItem item = new TableItem(table, SWT.NULL);
			item.setText(0, "" + (char)i.intValue());
		}

		table.getColumn(0).pack();

		text = new Text(glyphPicker, SWT.NONE);
		FormData fd_text = new FormData();
		fd_text.bottom = new FormAttachment(100, -10);
		fd_text.left = new FormAttachment(table, 0, SWT.LEFT);
		fd_text.right = new FormAttachment(table, 0, SWT.RIGHT);
		text.setLayoutData(fd_text);
		text.setEditable(false);

		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem[] sel = table.getSelection();
				if ( sel != null && sel.length > 0 ) {
					TableItem item = sel[0];
					text.setText(item.getText());
				}
			}
		});

		glyphPicker.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle area = glyphPicker.getClientArea();
				Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int width = area.width - 2*table.getBorderWidth();
				if (preferredSize.y > area.height + table.getHeaderHeight()) {
					// Subtract the scrollbar width from the total column width
					// if a vertical scrollbar will be required
					Point vBarSize = table.getVerticalBar().getSize();
					width -= vBarSize.x;
				}
				Point oldSize = table.getSize();
				if (oldSize.x > area.width) {
					// table is getting smaller so make the columns 
					// smaller first and then resize the table to
					// match the client area width
					column.setWidth(width-20);
					table.setSize(area.width, area.height);
				} else {
					// table is getting bigger so make the table 
					// bigger first and then make the columns wider
					// to match the client area width
					table.setSize(area.width, area.height);
					column.setWidth(width-20);
				}
			}
		});		
		
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
