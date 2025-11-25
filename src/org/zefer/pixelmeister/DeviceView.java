package org.zefer.pixelmeister;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.commands.ShowGridCommand;
import org.zefer.pixelmeister.device.pixels.Pixels;
import org.zefer.pixelmeister.util.Util;

public class DeviceView extends DataLayerView {

	private static final String TXT_CURSOR_POSITION = "Cursor position";
	private static final String TXT_MOUSE_DRAG_BOUNDARY = " Mouse drag box";
	private Button btnLandscape;
	private Button btn1bit;
	private StyledText dimensions;
	private StyledText gridStep;
	private ScrolledComposite scroller;
	private Button btnGrid;
	private Button btnRuler;
	private Label label_2;
	private Button btnCopy;

	private boolean dragInProgress;
	private int dragStartX;
	private int dragStartY;
	private Label mousePosition;
	private StyledText dragBox;
	private String dragBoxString = "";

	protected Cursor cursorCross;
	protected Cursor cursorDefault;
	protected Font regularFont;
	protected Font underlinedFont;
	private Label lblFontCaption_1;
	private Combo deviceName;
	private Combo zoom;
	private Properties devs;
	private int selectedDevice;
	public static int zoomFactor = 1;
	
	public DeviceView() {
        devs = getDeviceList();
	}

	public void init(final IViewSite site, final IMemento memento)
			throws PartInitException {
		init(site);
		if (memento == null) {
			return;
		}
		IMemento m1 = memento.getChild("device-view");
		if (m1 != null) {
			ruler = "true".equalsIgnoreCase(m1.getString("ruler"));
			grid = "true".equalsIgnoreCase(m1.getString("grid"));
			if ( ruler ) {
				rulerWidth = RULER_V_WIDTH;
				rulerHeight = RULER_H_WIDTH;
			} else {
				rulerWidth = 0;
				rulerHeight = 0;
			}
			try {
				gridStepPx = Integer.parseInt(m1.getString("grid-step"));
			} catch (Exception e) {
			}
			try {
				width = Integer.parseInt(m1.getString("device-width"));
			} catch (Exception e) {
			}
			try {
				height = Integer.parseInt(m1.getString("device-height"));
			} catch (Exception e) {
			}
			try {
				selectedDevice = Integer.parseInt(m1.getString("selected-device"));
			} catch (Exception e) {
				selectedDevice = 0;
			}
			controller = m1.getString("selected-device-controller");
			iface = m1.getString("selected-device-interface");

			if ( width > height ) {
				deviceHeight = width;
				deviceWidth = height;
				landscapeViewer = true;
				deviceOrientation = Pixels.LANDSCAPE;
			} else {
				deviceHeight = height;
				deviceWidth = width;
				landscapeViewer = false;
				deviceOrientation = Pixels.PORTRAIT;
			}
		}
	}

	public void saveState(final IMemento memento) {
		IMemento m1 = memento.createChild("device-view");
		m1.putString("ruler", ""+ruler);
		m1.putString("grid", ""+grid);
		m1.putString("grid-step", ""+gridStepPx);
		m1.putString("device-width", ""+width);
		m1.putString("device-height", ""+height);
		m1.putString("selected-device", ""+selectedDevice);
		m1.putString("selected-device-controller", controller);
		m1.putString("selected-device-interface", iface);
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FormLayout());
		final Clipboard cb = new Clipboard(parent.getDisplay());

		cursorCross = new Cursor(parent.getDisplay(), SWT.CURSOR_CROSS);
		cursorDefault = new Cursor(parent.getDisplay(), SWT.CURSOR_ARROW);

		if ( win ) {
			regularFont = SWTResourceManager.getFont("Tahoma", 10, SWT.NORMAL);
			underlinedFont = SWTResourceManager.getFont("Tahoma", 10, SWT.UNDERLINE_SINGLE);
		} else {
			regularFont = SWTResourceManager.getFont("Lucida Grande", 12, SWT.NORMAL);
			underlinedFont = SWTResourceManager.getFont("Lucida Grande", 12, SWT.UNDERLINE_SINGLE);
		}

		scroller = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		FormData fd_scrolledComposite = new FormData();
		fd_scrolledComposite.top = new FormAttachment(0, 10);
		fd_scrolledComposite.left = new FormAttachment(0, 10);
		fd_scrolledComposite.bottom = new FormAttachment(100, -10);
		fd_scrolledComposite.right = new FormAttachment(100, -200);
		scroller.setLayoutData(fd_scrolledComposite);
		scroller.setMinSize(width + rulerWidth, height + rulerHeight);
//		scroller.setExpandHorizontal(true);
//		scroller.setExpandVertical(true);

		canvas = new Canvas(scroller, SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		canvas.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		canvas.setSize(width + rulerWidth, height + rulerHeight);
		
		scroller.setContent(canvas);

	    canvas.addPaintListener(new PaintListener() { 
	        public void paintControl(PaintEvent e) { 
	        	doPaint( e );
	        } 
	    }); 

	    canvas.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				if (!stop) {
					return;
				}
				dragInProgress = true;
				dragStartX = Math.max(0, e.x - rulerWidth);
				dragStartY = Math.max(0, e.y - rulerHeight);
			}
			public void mouseUp(MouseEvent e) {
				dragInProgress = false;
				
				if ( dragStartX == Math.max(0, e.x - rulerWidth) &&	dragStartY == Math.max(0, e.y - rulerHeight) ) {
					dragBox.setText("");
					dragBoxString = "";

					GC gc = new GC(canvas);

					if (displayImage != null) {
						gc.drawImage(displayImage, rulerWidth, rulerHeight);
					} else {
						gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
						gc.fillRectangle(rulerWidth, rulerHeight, width * zoomFactor, height * zoomFactor);
						if (grid) {
							drawGrid(gc, width * zoomFactor, height * zoomFactor, gridStepPx * zoomFactor, ruler);
						}
					}
					gc.dispose();
					btnCopy.setVisible(false);
					dragBox.setEnabled(false);
					dragBox.setEditable(false);
					dragBox.setText(TXT_MOUSE_DRAG_BOUNDARY);
					dragBox.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
				}
			
			}
		});
		canvas.addMouseTrackListener(new MouseTrackAdapter() {
			public void mouseExit(MouseEvent e) {
				mousePosition.setText( TXT_CURSOR_POSITION );
				mousePosition.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
			}
			public void mouseEnter(MouseEvent e) {
				canvas.setCursor(cursorCross);
			}
		});
		canvas.addMouseMoveListener(new MouseMoveListener() {
			public void mouseMove(MouseEvent e) {
				
				e.x -= rulerWidth;
				e.y -= rulerHeight;
				e.x = Math.max(e.x, 0);
				e.y = Math.max(e.y, 0);
				e.x = Math.min(e.x, width * zoomFactor - 1);
				e.y = Math.min(e.y, height * zoomFactor - 1);
				
				e.x /= zoomFactor; 
				e.y /= zoomFactor; 
				
				mousePosition.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
				mousePosition.setText( e.x + ", " + e.y );
				if ( dragInProgress ) {
					int w = e.x - dragStartX / zoomFactor;
					if ( w < 0 ) {
						w *= -1;
					}
					int h = e.y - dragStartY / zoomFactor;
					if ( h < 0 ) {
						h *= -1;
					}
					int x = Math.min(e.x, dragStartX / zoomFactor);
					int y = Math.min(e.y, dragStartY / zoomFactor);
					dragBox.setText( " " + x + ", " + y + "  " + w + "x" + h);
					dragBoxString = x + ", " + y + ", " + (x+w) + ", " + (y+h);
					
					GC gc = new GC(canvas);

					if (displayImage != null) {
						gc.drawImage(displayImage, rulerWidth, rulerHeight);
					} else {
						gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
						gc.fillRectangle(rulerWidth, rulerHeight, width * zoomFactor, height * zoomFactor);
						if (grid) {
							drawGrid(gc, width * zoomFactor, height * zoomFactor, gridStepPx * zoomFactor, ruler);
						}
					}
					
					gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
					gc.drawRectangle(x * zoomFactor + rulerWidth, y * zoomFactor + rulerHeight, w * zoomFactor, h * zoomFactor);
					gc.dispose();
					btnCopy.setVisible(true);
					dragBox.setEnabled(true);
					dragBox.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
				}
			}
		});
		
        lblFontCaption_1 = new Label(parent, SWT.NONE);
		if ( win ) {
	        lblFontCaption_1.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblFontCaption_1.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        FormData fd_lblFont = new FormData();
		fd_lblFont.top = new FormAttachment(0, 10);
		fd_lblFont.left = new FormAttachment(scroller, 10);
        lblFontCaption_1.setLayoutData(fd_lblFont);
        lblFontCaption_1.setText("Device");

        Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        FormData fd_label = new FormData();
        fd_label.top = new FormAttachment(lblFontCaption_1, 6);
        fd_label.left = new FormAttachment(scroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);


        deviceName = new Combo(parent, SWT.NONE);
        deviceName.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		selectedDevice = deviceName.getSelectionIndex();
                for ( int i = 0; i < 1000; i++ ) {
                	String n = "device." + i + ".name";
                	String d = devs.getProperty(n);
                	if ( d != null && d.equals(deviceName.getText()) ) {
                    	String w = devs.getProperty("device." + i + ".width");
                    	String h = devs.getProperty("device." + i + ".height");
                    	controller = devs.getProperty("device." + i + ".controller");
                    	iface = devs.getProperty("device." + i + ".interface");
                    	codingHint = devs.getProperty("device." + i + ".hint");
                    	oneBitColor = "true".equalsIgnoreCase(devs.getProperty("device." + i + ".1bitcolor"));
                		String s = w + "x" + h;
                		dimensions.setText(s);
                		
            			if ( width > height ) {
            				deviceHeight = width;
            				deviceWidth = height;
            				landscapeViewer = true;
            				deviceOrientation = Pixels.LANDSCAPE;
            			} else {
            				deviceHeight = height;
            				deviceWidth = width;
            				landscapeViewer = false;
            				deviceOrientation = Pixels.PORTRAIT;
            			}
                		
        				btnLandscape.setSelection(landscapeViewer);
        				updateGui();
        				drawCanvasBackground();
        				redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
        	            DataLayerView.statusPane.setMessage(null, controller + " (" + iface + ")", null);
        				DataLayerView.updateStatusLine();
        				break;
                	}
                }
        	}
        });
        FormData fd_deviceName = new FormData();
        fd_deviceName.top = new FormAttachment(label, 6);
        fd_deviceName.left = new FormAttachment(scroller, 10);
        fd_deviceName.right = new FormAttachment(100, -10);
        deviceName.setLayoutData(fd_deviceName);
        
        for ( int i = 0, j = 0; i < 1000; i++ ) {
        	String n = "device." + i + ".name";
        	String d = devs.getProperty(n);
        	if ( d != null ) {
        		if ( j == selectedDevice ) {
                	controller = devs.getProperty("device." + i + ".controller");
                	iface = devs.getProperty("device." + i + ".interface");
                	codingHint = devs.getProperty("device." + i + ".hint");
                	oneBitColor = "true".equalsIgnoreCase(devs.getProperty("device." + i + ".1bitcolor"));
        		}
        		deviceName.add(d, j++);
        	}
        }
        deviceName.select(selectedDevice);
        
		Label lblWxh = new Label(parent, SWT.NONE);
		FormData fd_lblWxh = new FormData();
		fd_lblWxh.top = new FormAttachment(deviceName, 6);
		fd_lblWxh.left = new FormAttachment(scroller, 10);
		lblWxh.setLayoutData(fd_lblWxh);
		lblWxh.setText("Resolution (WxH):");
		
		
		dimensions = new StyledText(parent, SWT.NONE);
		dimensions.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				stop = true;
				instance = null;
				String s = dimensions.getText();
				parseDimensions(s);
			}
		});
		dimensions.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}

				if ( e.character == 13 ) {
					canvas.forceFocus();
				}
			}
		});
		dimensions.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}

				String s = dimensions.getText();
				parseDimensions(s);
				dimensions.setText(width + "x" + height);
				landscapeViewer = width > height;
				btnLandscape.setSelection(landscapeViewer);
				StyleRange range = new StyleRange();
				range.start = 0;
				range.length = dimensions.getText().length();
				range.underline = false;
				dimensions.setStyleRange(range);
				updateGui();
				drawCanvasBackground();
				redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
			}
			@Override
			public void focusGained(FocusEvent e) {
				StyleRange range = new StyleRange();
				range.start = 0;
				range.length = dimensions.getText().length();
				range.underline = true;
				dimensions.setStyleRange(range);
			}
		});

		dimensions.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		dimensions.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		FormData fd_dimensions = new FormData();
		fd_dimensions.top = new FormAttachment(lblWxh, 0, SWT.TOP);
		fd_dimensions.left = new FormAttachment(lblWxh, 6);
		fd_dimensions.right = new FormAttachment(100, -6);
		dimensions.setLayoutData(fd_dimensions);
		dimensions.setText(width + "x" + height);

		btnLandscape = new Button(parent, SWT.CHECK);
		btnLandscape.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
//				if ( landscape ) {
//					PlatformUI.getWorkbench().getThemeManager().setCurrentTheme("org.zefer.pixelmeister.darker.ui.theme");
//				} else {
//					PlatformUI.getWorkbench().getThemeManager().setCurrentTheme(IThemeManager.DEFAULT_THEME);
//				}
//				
//			    IThemeManager themeManager = getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
//				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setBackgroundMode(SWT.INHERIT_FORCE); 
//				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setBackground(themeManager.getCurrentTheme().getColorRegistry().get("org.zefer.darkertheme.BACKGROUND"));
//				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setForeground(themeManager.getCurrentTheme().getColorRegistry().get("org.zefer.darkertheme.FOREGROUND"));
				
//				FormColors formColor = new FormColors( Display.getCurrent()); 
//				formColor.createColor( FormColors.BORDER, 0, 0, 255); 
//				formColor.setBackground( new Color( Display.getCurrent(), 255, 0, 0)); 
//				FormToolkit formToolkit = new FormToolkit( formColor); 
//				formToolkit.paintBordersFor(scroller); 				
				
				stop = true;
				instance = null;
				landscapeViewer = btnLandscape.getSelection();
				
				if ( landscapeViewer ) {
					width = deviceHeight;
					height = deviceWidth;
				} else {
					height = deviceHeight;
					width = deviceWidth;
				}
				
				dimensions.setText(width + "x" + height);	    				
				updateGui();
				drawCanvasBackground();
				redrawCanvas(0, 0, deviceHeight + rulerWidth, deviceHeight + rulerHeight); // maximal area
			}
		});
		FormData fd_btnLandscape = new FormData();
		fd_btnLandscape.top = new FormAttachment(lblWxh, 6);
		fd_btnLandscape.left = new FormAttachment(scroller, 10);
		btnLandscape.setLayoutData(fd_btnLandscape);
		btnLandscape.setText(" Landscape");
		btnLandscape.setSelection(landscapeViewer);

		btn1bit = new Button(parent, SWT.CHECK);
		btn1bit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DataLayerView.oneBitColor = btn1bit.getSelection();
				updateGui();
				drawCanvasBackground();
				DataLayerView.render();
			}
		});
		FormData fd_btn1bit = new FormData();
		fd_btn1bit.top = new FormAttachment(btnLandscape, 6);
		fd_btn1bit.left = new FormAttachment(scroller, 10);
		btn1bit.setLayoutData(fd_btn1bit);
		btn1bit.setText(" 1-bit color");
		btn1bit.setSelection(oneBitColor);
//		btn1bit.setEnabled(false);

        Label lblFontCaption;
        lblFontCaption = new Label(parent, SWT.NONE);
		if ( win ) {
	        lblFontCaption.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblFontCaption.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        fd_lblFont = new FormData();
		fd_lblFont.top = new FormAttachment(btn1bit, 15);
		fd_lblFont.left = new FormAttachment(scroller, 10);
        lblFontCaption.setLayoutData(fd_lblFont);
        lblFontCaption.setText("Show");

        label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(lblFontCaption, 6);
        fd_label.left = new FormAttachment(scroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label.setLayoutData(fd_label);

		btnRuler = new Button(parent, SWT.CHECK);
		btnRuler.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ruler = ((Button)e.widget).getSelection();
				if ( ruler ) {
					rulerWidth = RULER_V_WIDTH;
					rulerHeight = RULER_H_WIDTH;
				} else {
					rulerWidth = 0;
					rulerHeight = 0;
				}

				final ICommandService commandService = (ICommandService)getViewSite().getService(ICommandService.class);
				final Command command = commandService.getCommand("org.zefer.pixelmeister.view.show.rulers");
				final State state = command.getState(RegistryToggleState.STATE_ID);
				if (state != null) {
					state.setValue(new Boolean(ruler));
				}
				
				updateGui();
				repaintRuler = true;
				drawCanvasBackground();
				redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
			}
		});
		FormData fd_btnRuler = new FormData();
		fd_btnRuler.top = new FormAttachment(label, 10);
		fd_btnRuler.left = new FormAttachment(scroller, 10);
		btnRuler.setLayoutData(fd_btnRuler);
		btnRuler.setText(" Rulers");
		btnRuler.setSelection(ruler);
		
		btnGrid = new Button(parent, SWT.CHECK);
		btnGrid.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				
				grid = ((Button)e.widget).getSelection();
				gridStep.setEnabled(grid);

				final ICommandService commandService = (ICommandService)getViewSite().getService(ICommandService.class);
				final Command command = commandService.getCommand("org.zefer.pixelmeister.view.show.grid");
				final State state = command.getState(RegistryToggleState.STATE_ID);
				if (state != null) {
					state.setValue(new Boolean(grid));
				}
				
//				if ( stop && !grid && displayImage != null ) {
//					displayImage = null;
//				}
				
				ICommandService service = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
				service.refreshElements(ShowGridCommand.ID, null);
				
				updateGui();
				drawCanvasBackground();
				redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
			}
		});
		FormData fd_btnGrid = new FormData();
		fd_btnGrid.top = new FormAttachment(btnRuler, 6);
		fd_btnGrid.left = new FormAttachment(btnRuler, 0, SWT.LEFT);
		btnGrid.setLayoutData(fd_btnGrid);
		btnGrid.setText(" Gridline every");
		btnGrid.setSelection(grid);

		gridStep = new StyledText(parent, SWT.NONE);
		gridStep.setEnabled(grid);
		gridStep.setText(gridStepPx+"px");
		gridStep.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		gridStep.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		FormData fd_gridStep = new FormData();
		fd_gridStep.right = new FormAttachment(100, -6);
		fd_gridStep.left = new FormAttachment(dimensions, 0, SWT.LEFT);
		fd_gridStep.bottom = new FormAttachment(btnGrid, -1, SWT.BOTTOM);
		gridStep.setLayoutData(fd_gridStep);
		gridStep.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (canvas == null || canvas.isDisposed()) {
					return;
				}

				if ( e.character == 13 ) {
					canvas.forceFocus();
				}
			}
		});
		gridStep.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				String s = gridStep.getText();
				parseGridStep(s);
				gridStep.setText(gridStepPx + "px");
				StyleRange range = new StyleRange();
				range.start = 0;
				range.length = gridStep.getText().length();
				range.underline = false;
				gridStep.setStyleRange(range);
				redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
			}
			@Override
			public void focusGained(FocusEvent e) {
				StyleRange range = new StyleRange();
				range.start = 0;
				range.length = gridStep.getText().length();
				range.underline = true;
				gridStep.setStyleRange(range);
			}
		});
		gridStep.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String s = gridStep.getText();
				parseGridStep(s);
			}
		});

        lblFontCaption = new Label(parent, SWT.NONE);
		if ( win ) {
	        lblFontCaption.setFont(SWTResourceManager.getFont("Tahoma", 10, SWT.BOLD));
		} else {
	        lblFontCaption.setFont(SWTResourceManager.getFont("Lucida Grande", 13, SWT.BOLD));
		}
        fd_lblFont = new FormData();
		fd_lblFont.top = new FormAttachment(btnGrid, 15);
		fd_lblFont.left = new FormAttachment(scroller, 10);
        lblFontCaption.setLayoutData(fd_lblFont);
        lblFontCaption.setText("Info");

        label_2 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(lblFontCaption, 6);
        fd_label.left = new FormAttachment(scroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label_2.setLayoutData(fd_label);

        
		Label labelXY = new Label(parent, SWT.NONE);
		FormData fd_labelXY = new FormData();
		fd_labelXY.top = new FormAttachment(label_2, 10);
		fd_labelXY.left = new FormAttachment(scroller, 10);
		fd_labelXY.width = 16;
		fd_labelXY.height = 16;
		labelXY.setLayoutData(fd_labelXY);
		Image icon = Util.getImageRegistry(parent.getDisplay()).get("xy");
		labelXY.setImage(icon);
	
		Label labelWH = new Label(parent, SWT.NONE);
		FormData fd_labelWH = new FormData();
		fd_labelWH.top = new FormAttachment(labelXY, 6);
		fd_labelWH.left = new FormAttachment(scroller, 10);
		fd_labelWH.width = 16;
		fd_labelWH.height = 16;
		labelWH.setLayoutData(fd_labelWH);
		icon = Util.getImageRegistry(parent.getDisplay()).get("wh");
		labelWH.setImage(icon);

        mousePosition = new Label(parent, SWT.NONE);
		FormData fd_mousePosition = new FormData();
		fd_mousePosition.left = new FormAttachment(labelXY, 8);
		fd_mousePosition.right = new FormAttachment(100, -6); 
		fd_mousePosition.top = new FormAttachment(labelXY, 0, SWT.TOP);
		mousePosition.setLayoutData(fd_mousePosition);
		mousePosition.setText(TXT_CURSOR_POSITION);
		mousePosition.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));

		dragBox = new StyledText(parent, SWT.NONE);
		dragBox.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		dragBox.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
		FormData fd_dragBox = new FormData();
		fd_dragBox.left = new FormAttachment(labelXY, 5);
		fd_dragBox.right = new FormAttachment(100, -65); 
		fd_dragBox.top = new FormAttachment(labelWH, 0, SWT.TOP);
		dragBox.setLayoutData(fd_dragBox);
		dragBox.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_GRAY));
		dragBox.setText(TXT_MOUSE_DRAG_BOUNDARY);
		
		
		btnCopy = new Button(parent, SWT.NONE);
		btnCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (dragBoxString.length() > 0) {
					TextTransfer textTransfer = TextTransfer.getInstance();
					cb.setContents(new Object[]{dragBoxString}, new Transfer[]{textTransfer});
				}
			}
		});
		FormData fd_btnCopy = new FormData();
		fd_btnCopy.top = new FormAttachment(dragBox, -1, SWT.TOP);
		fd_btnCopy.right = new FormAttachment(100, -6);
		fd_btnCopy.height = 20;
		btnCopy.setLayoutData(fd_btnCopy);
		btnCopy.setText("Copy");
		btnCopy.setToolTipText("Copy formatted as function parameters");
		btnCopy.setVisible(false);

        Label label_3 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(dragBox, 15);
        fd_label.left = new FormAttachment(scroller, 10);
        fd_label.right = new FormAttachment(100, -10);
        label_3.setLayoutData(fd_label);

		Label labelZoom = new Label(parent, SWT.NONE);
		FormData fd_labelZoom = new FormData();
		fd_labelZoom.top = new FormAttachment(label_3, 10);
		fd_labelZoom.left = new FormAttachment(scroller, 10);
//		fd_labelZoom.width = 40;
		fd_labelZoom.height = 16;
		labelZoom.setLayoutData(fd_labelZoom);
		labelZoom.setText("Zoom");


        zoom = new Combo(parent, SWT.NONE);
        zoom.add("x1");
        zoom.add("x2");
        zoom.add("x4");
        zoom.add("x8");
        zoom.select(0);
        zoom.addSelectionListener(new SelectionAdapter() {
        	public void widgetSelected(SelectionEvent e) {
        		zoomFactor = 1 << Math.max(0, zoom.getSelectionIndex());
        		System.out.println("ZOOM x" + zoomFactor);
				updateGui();
				drawCanvasBackground();
				redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
        	}
        });
        FormData fd_zoom = new FormData();
        fd_zoom.top = new FormAttachment(label_3, 6);
        fd_zoom.left = new FormAttachment(labelZoom, 8);
        fd_zoom.right = new FormAttachment(100, -10);
        zoom.setLayoutData(fd_zoom);

        
        
        Label label_4 = new Label(parent, SWT.SEPARATOR | SWT.VERTICAL);
        fd_label = new FormData();
        fd_label.top = new FormAttachment(scroller, 0, SWT.TOP);
        fd_label.left = new FormAttachment(scroller, 0);
        fd_label.bottom = new FormAttachment(100, -10);
        label_4.setLayoutData(fd_label);
        
        drawCanvasBackground();
		canvas.forceFocus();
	}

	public void updateGui() {
		
		if ( ruler != btnRuler.getSelection() ) {
			btnRuler.setSelection(ruler);
		}
		
		if ( grid != btnGrid.getSelection() ) {
			btnGrid.setSelection(grid);
		}
		
		btn1bit.setSelection(oneBitColor);

		scroller.setMinSize(width * zoomFactor + rulerWidth, height * zoomFactor + rulerHeight);
		canvas.setSize(width * zoomFactor + rulerWidth, height * zoomFactor + rulerHeight);
	}

	protected void drawCanvasBackground() {
		GC cgc = null;
		if (canvas != null && !canvas.isDisposed()) {
			cgc = new GC(canvas);
			if ( DataLayerView.ruler ) {
				Transform tt = new Transform(canvas.getDisplay());
				tt.translate(DataLayerView.rulerWidth, DataLayerView.rulerHeight);
				cgc.setTransform(tt);
			}

			Color sav1 = cgc.getForeground();
			Color sav2 = cgc.getBackground();
			Color color = SWTResourceManager.getColor(SWT.COLOR_GRAY);
			cgc.setBackground(color);
			cgc.fillRectangle(0, 0, width * zoomFactor, height * zoomFactor);
			color = new Color(canvas.getDisplay(), new RGB(0xa0, 0xa0, 0xa0));
			cgc.setForeground(color);
			for ( int i = 0; i < width * zoomFactor; i += zoomFactor*2 ) {
				for (int j = 0; j < zoomFactor; j++)
				{
					cgc.drawLine(i+j, 0, i+j, height * zoomFactor);
				}
			}
			cgc.setForeground(sav1);
			cgc.setBackground(sav2);
			color.dispose();
			cgc.dispose();
		}
	}
	
	protected void doPaint(PaintEvent e) {
		GC gc = new GC(canvas);
		if ( ruler ) {
			drawRuler(gc, e);
			repaintRuler = false;
		}
		if ( displayImage != null ) {
			
//			System.out.println("draw: " + displayImage.getBounds().width + "x" + displayImage.getBounds().height);
			
			gc.drawImage(displayImage, rulerWidth, rulerHeight);
		} else {
			drawCanvasBackground();
		}
			
		if ( grid && stop ) {
			drawGrid(gc, width * zoomFactor, height * zoomFactor, gridStepPx * zoomFactor, ruler);
		}
		gc.dispose();
	}

	public static void drawRuler(GC gc, PaintEvent e) {

		Rectangle r = new Rectangle(e.x, e.y, e.width * zoomFactor, e.height * zoomFactor);
		Rectangle rH = new Rectangle(0, 0, width * zoomFactor + rulerWidth, RULER_H_WIDTH);
		Rectangle rV = new Rectangle(0, 0, RULER_V_WIDTH, height * zoomFactor + rulerHeight);
		boolean updateHorizontal = r.intersects(rH) | repaintRuler;
		boolean updateVertical = r.intersects(rV) | repaintRuler;
		
		if ( deviceScroll != 0 ) {
			updateHorizontal |= landscapeViewer;
			updateVertical |= !landscapeViewer;
		}
		
		if ( win ) {
	        gc.setFont(SWTResourceManager.getFont("Tahoma", 8, SWT.NONE));
		} else {
	        gc.setFont(SWTResourceManager.getFont("Lucida Grande", 8, SWT.NONE));
		}
		
		gc.setBackground(SWTResourceManager.getColor(255,255,255));
		
		if ( updateHorizontal ) {
			gc.fillRectangle(0, 0, width * zoomFactor + rulerWidth, RULER_H_WIDTH);
		}
		if ( updateVertical ) {
			gc.fillRectangle(0, RULER_H_WIDTH, RULER_V_WIDTH, height * zoomFactor + rulerHeight);
		}
		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
		if ( updateHorizontal ) {
			gc.drawLine(RULER_V_WIDTH-1, RULER_H_WIDTH-1, width * zoomFactor + rulerWidth, RULER_H_WIDTH-1);
		}
		if ( updateVertical ) {
			gc.drawLine(RULER_V_WIDTH-1, RULER_H_WIDTH-1, RULER_V_WIDTH-1, height * zoomFactor + rulerHeight);
		}
		
		int bigTick = RULER_BIG_TICK;
		int smallTick = RULER_SMALL_TICK;
		
		if (zoomFactor > 4) {
			bigTick = 10;
			smallTick = 10;
			
		}
		
		if ( updateHorizontal ) {
			if (landscapeViewer) {
	            gc.drawLine( RULER_V_WIDTH, RULER_H_WIDTH - 1, RULER_V_WIDTH, - 1);
	            
	            int scroll = deviceScroll;
	            if (scroll != 0) { // && deviceOrientation == Pixels.LANDSCAPE) {
	            	scroll = deviceHeight - scroll;
	            }
	            
                int lastEdge = 0;
	            
		        for (int i = 0; i < width; i++) {
		        	
		        	boolean reverse = deviceOrientation == Pixels.LANDSCAPE_FLIP || deviceOrientation == Pixels.PORTRAIT_FLIP;  
		            int pos = reverse ? deviceHeight - 1 - ((i + scroll) % deviceHeight) : (i + scroll) % deviceHeight;

		            String text = "" + i; 
	                Point se = gc.stringExtent(text);

	                se.x /= zoomFactor;
		            
	                int tickLength = 0;
		            if (i % bigTick == 0 ) {
		                tickLength = RULER_V_WIDTH;
		            } else if (i % smallTick == 0) {
		                tickLength = 3;
		            }

	                if (i % bigTick == 0 || pos == 0 && bigTick - (i % bigTick) > se.x + 2 || pos == deviceHeight - 1) {

		                if ( reverse ) {
			                if ( pos == deviceHeight - 1 && lastEdge + se.x + 2 < deviceHeight ) {
			                	se = gc.stringExtent(text);
		                		gc.drawString(text, pos * zoomFactor + RULER_V_WIDTH - se.x - 1, 0);
			                } else if ( pos != 0 && se.x + pos + 20 < deviceHeight && se.x + i + 2 < deviceHeight || pos == 0 && pos + 2 < (i % bigTick) ) {
				                gc.drawString(text, pos * zoomFactor + RULER_V_WIDTH + 2, 0);
				                lastEdge = pos + 2 + se.x;
			                }
		                } else {
			                if ( pos == deviceHeight - 1 && lastEdge + se.x + 2 < deviceHeight ) {
			                	se = gc.stringExtent(text);
		                		gc.drawString(text, pos * zoomFactor + RULER_V_WIDTH - se.x - 1, 0);
			                } else if ( se.x + pos + 20 < deviceHeight && se.x + i + 2 < deviceHeight ) {
				                gc.drawString(text, pos * zoomFactor + RULER_V_WIDTH + 2, 0);
				                lastEdge = pos + 2 + se.x;
			                }
		                }
		            }  
		            	
	                if ( i == 0 && pos != 0 ) {
	            		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
	                	gc.drawLine(pos * zoomFactor + RULER_V_WIDTH, RULER_H_WIDTH - 1, pos * zoomFactor + RULER_V_WIDTH, RULER_H_WIDTH - tickLength - 1);
	            		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
	                } else if (tickLength > 0) {
			            gc.drawLine(pos * zoomFactor + RULER_V_WIDTH, RULER_H_WIDTH - 1, pos * zoomFactor + RULER_V_WIDTH, RULER_H_WIDTH - tickLength - 1);
		            }
		        	
		        }
				gc.drawLine(0, 0, 0, RULER_H_WIDTH);
			} else {
                int lastEdge = 0;
		        for (int i = 0; i < deviceWidth; i++) {
		        	int tickLength = 0;
		        	
		        	boolean reverse = deviceOrientation == Pixels.LANDSCAPE || deviceOrientation == Pixels.PORTRAIT_FLIP;  
		            int pos = reverse ? deviceWidth - 1 - i : i;

		            String text = "" + i; 
	                Point se = gc.stringExtent(text);
	                
	                se.x /= zoomFactor;
		            
		            if (i % bigTick == 0 || pos == deviceWidth - 1 || pos == 0) {
		                if ( reverse ) {
			                if ( pos == deviceWidth - 1 && lastEdge + se.x < deviceWidth ) {
			                	se = gc.stringExtent(text);
			                	gc.drawString(text, pos * zoomFactor + RULER_V_WIDTH - se.x - 1, 0);
			                } else if ( pos != 0 && se.x + pos + 20 < deviceWidth || pos == 0 && se.x + 2 < (i % bigTick) ) {
				                gc.drawString(text, pos * zoomFactor + 2 + RULER_V_WIDTH, 0);
				                lastEdge = pos;
			                }
		                } else {
			                if ( pos == deviceWidth - 1 && lastEdge + se.x < deviceWidth ) {
			                	se = gc.stringExtent(text);
			                	gc.drawString(text, pos * zoomFactor + RULER_V_WIDTH - se.x - 1, 0);
			                } else if ( se.x + pos + 10 < deviceWidth && se.x + i + 10 < deviceWidth ) {
			                	gc.drawString(text, pos * zoomFactor + 2 + RULER_V_WIDTH, 0);
				                lastEdge = pos + 2 + se.x;
			                }
		                }
					}
		            
		        	if (i % bigTick == 0) {
		                tickLength = RULER_H_WIDTH;
		            } else if (i % smallTick == 0) {
		                tickLength = 3;
		            }
		        	
		            if ( tickLength != 0 ) {
		            	gc.drawLine(pos * zoomFactor + RULER_V_WIDTH, RULER_H_WIDTH - 1, pos * zoomFactor + RULER_V_WIDTH, RULER_H_WIDTH - tickLength - 1);
		            }
		        }
			}
			gc.drawLine(0, 0, width * zoomFactor + RULER_V_WIDTH, 0);
			
			gc.drawLine(RULER_V_WIDTH, 0, RULER_V_WIDTH, RULER_H_WIDTH - 1);
			gc.drawLine(width * zoomFactor + RULER_V_WIDTH - 1, 0, width * zoomFactor + RULER_V_WIDTH - 1, RULER_H_WIDTH - 1);
		}

		if ( updateVertical ) {
			gc.drawLine(0, RULER_H_WIDTH, RULER_V_WIDTH - 1, RULER_H_WIDTH);
			
			if (!landscapeViewer) {
	            gc.drawLine( RULER_V_WIDTH - 1, RULER_H_WIDTH, -1, RULER_H_WIDTH);
	            int scroll = deviceScroll;
	            
	            if (scroll != 0) { // && deviceOrientation == Pixels.PORTRAIT) {
	            	scroll = deviceHeight - scroll;
	            }
	            
                int se = gc.getFontMetrics().getHeight();
 
                se /= zoomFactor;
	            

                int lastEdge = 0;
                
                for (int i = 0; i < deviceHeight; i++) {
                	
		        	boolean reverse = deviceOrientation == Pixels.PORTRAIT_FLIP || deviceOrientation == Pixels.LANDSCAPE_FLIP;  
		            int pos = reverse ? deviceHeight - 1 - ((i + scroll) % deviceHeight) : (i + scroll) % deviceHeight;

	                int tickLength = 0;
		            if (i % bigTick == 0 ) {
		                tickLength = RULER_V_WIDTH;
		            } else if (i % smallTick == 0) {
		                tickLength = 3;
		            }

		            if (i % bigTick == 0 || pos == 0 && bigTick - (i % bigTick) > se + 1 || pos == deviceHeight - 1) {
		            	String text = "" + i;
		                if ( reverse ) {
			                if ( pos == deviceHeight - 1 && lastEdge + se + 2 < deviceHeight ) {
			                	se = gc.getFontMetrics().getHeight();
			                	gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH - se);
			                } else if ( pos != 0 && se + pos + 2 < deviceHeight && se + i + 2 < deviceHeight || pos == 0 && se + 3 < (i % bigTick) ) {
				                gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH);
				                lastEdge = pos + 2 + se;
			                }
		                } else {
			            	if ( pos == deviceHeight - 1 && lastEdge + se < deviceHeight ) {
			                	se = gc.getFontMetrics().getHeight();
		                		gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH - se);
			                } else if ( se + pos + 2 < deviceHeight && se + i + 2 < deviceHeight ) {
				                gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH);
				                lastEdge = pos + 2 + se;
			                }
		                }
		            } 

	                if ( i == 0 && pos != 0 ) {
	            		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
						gc.drawLine(RULER_V_WIDTH - 1, pos * zoomFactor + RULER_H_WIDTH, RULER_V_WIDTH - tickLength - 1, pos * zoomFactor + RULER_H_WIDTH);
	            		gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_GRAY));
	                } else if ( tickLength > 0 ) {
		            	gc.drawLine(RULER_V_WIDTH - 1, pos * zoomFactor + RULER_H_WIDTH, RULER_V_WIDTH - tickLength - 1, pos * zoomFactor + RULER_H_WIDTH);
		            }
                }
				gc.drawLine(0, 0, RULER_V_WIDTH, 0);
			} else {
				for (int i = 0; i < height; i ++) {
	                int lastEdge = 0;
		        	int tickLength = 0;
		        	boolean reverse = deviceOrientation == Pixels.PORTRAIT || deviceOrientation == Pixels.LANDSCAPE_FLIP;  
		            int pos = reverse ? height - 1 - i : i;

	                int se = gc.getFontMetrics().getHeight();

	                se /= zoomFactor;
	                
	                String text = "" + i; 
		            
					if (i % bigTick == 0 || pos == height - 1 || pos == 0) {

		                if ( reverse ) {
			                if ( pos == height - 1 && lastEdge + se < height ) {
			                	se = gc.getFontMetrics().getHeight();
			                	gc.drawString(text, 3, pos * zoomFactor + RULER_H_WIDTH - se - 1);
			                } else if ( pos != 0 && se + pos + 8 < height || pos == 0 && se + 2 < (i % bigTick) ) {
			                	gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH);
				                lastEdge = pos;
			                }
		                } else {
			                if ( pos == height - 1 && lastEdge + se < height ) {
			                	se = gc.getFontMetrics().getHeight();
			                	gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH - se);
			                } else if ( se + pos + 8 < height && se + i + 8 < height ) {
			                	gc.drawString(text, 3, pos * zoomFactor + 1 + RULER_H_WIDTH);
				                lastEdge = pos + 1 + se;
			                }
		                }
					} 

		        	if (i % bigTick == 0) {
		                tickLength = RULER_V_WIDTH;
		            } else if (i % smallTick == 0) {
		                tickLength = 3;
		            }
			        	
					if ( tickLength != 0 ) {
		            	gc.drawLine(RULER_V_WIDTH - 1, pos * zoomFactor + RULER_H_WIDTH, RULER_V_WIDTH - tickLength - 1, pos * zoomFactor + RULER_H_WIDTH);
		            }
				}
			}
			
			gc.drawLine(0, 0, 0, height * zoomFactor + RULER_H_WIDTH);
			
			gc.drawLine(0, RULER_H_WIDTH, RULER_V_WIDTH-1, RULER_H_WIDTH);
			gc.drawLine(0, height * zoomFactor + RULER_H_WIDTH - 1, RULER_V_WIDTH - 1, height * zoomFactor + RULER_H_WIDTH - 1);
		}
	}
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	public Object redrawCanvas(final int x, final int y, final int width, final int height) {
//		System.out.println("redraw " + x + " " + width);
		canvas.redraw(x, y, width, height, true);
		return null;
	}

	protected void parseDimensions(String s) {
		String w = "";
		String h = "";
		boolean wdone = false;
		for ( int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ( c == ' ' ) { 
				if ( w.length() == 0 ) {
					continue;
				} else {
					break;
				}
			}
			if ( c == 'x' || c == 'X' ) { 
				if ( w.length() == 0 ) {
					break;
				}
				wdone = true;
				continue;
			}
			if ( Character.isDigit(c) ) {
				if ( !wdone ) {
					w += c;
				} else {
					h += c;
				}
			} else {
				break;
			}
		}

		try {
			int res1 = Integer.parseInt(w);
			int res2 = Integer.parseInt(h);
			if ( res1 > 0 && res2 > 0 ) {
				width = res1;
				height = res2;
			}
		} catch (Exception e2) {
		}
	}
	
	private void parseGridStep(String s) {
		String r = "";
		for ( int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ( c == ' ' ) { 
				if ( r.length() == 0 ) {
					continue;
				} else {
					break;
				}
			}
			if ( Character.isDigit(c) ) {
				r += c;
			} else {
				break;
			}
		}
		try {
			int res = Integer.parseInt(r);
			if ( res >= 5 ) {
				gridStepPx = res;
			}
		} catch (Exception e2) {
		}
	}
	
	private Properties getDeviceList() {
		Properties p = new Properties();
        try {
			URL codeBase = FileLocator.find(Platform.getBundle(Activator.PLUGIN_ID), 
					new Path("/" + DataLayerView.DEVICE_LIST), null);
			URL classesUrl = FileLocator.toFileURL(codeBase);
			p.load(new FileInputStream(classesUrl.getPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
        return p;
	}
}
