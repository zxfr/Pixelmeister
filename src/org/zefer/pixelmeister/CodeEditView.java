package org.zefer.pixelmeister;

import java.io.File;
import java.util.HashSet;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.commands.RunCommand;
import org.zefer.pixelmeister.commands.TerminateCommand;
import org.zefer.pixelmeister.device.ScriptBase;
import org.zefer.pixelmeister.device.pixels.Pixels;
import org.zefer.pixelmeister.util.Util;

public class CodeEditView extends DataLayerView {

	private Shell shell;
	
	public Button btnStop;
	public Button btnRun;

	private Label errorIcon;

	private Label instanceNameLabel2;

	public CodeEditView() {
	}

	public void init(final IViewSite site, final IMemento memento)
			throws PartInitException {
		init(site);
		if (memento == null) {
			return;
		}
		
		IMemento m1 = memento.getChild("sketch-view");
		if (m1 != null) {
			
			emulatingLibrary = 0;
			try {
				emulatingLibrary = Integer.parseInt(m1.getString("emu-library"));
			} catch (NumberFormatException e1) {
			} 
			
			recentImageResourcePath = m1.getString("last-image-folder");
			recentFontResourcePath = m1.getString("last-font-folder");
			recentSketchPath = m1.getString("last-sketch-folder");
			currentSketch = m1.getString("last-sketch");
			recentExportSketchPath = m1.getString("last-export-sketch-folder");
			currentExportSketch = m1.getString("last-export-sketch");

			oneBitColor = "true".equalsIgnoreCase(m1.getString("one-bit-color"));
			
			lastChosenItalic = "true".equalsIgnoreCase(m1.getString("last-chosen-italic"));
			lastChosenBold = "true".equalsIgnoreCase(m1.getString("last-chosen-bold"));
			lastChosenFont = m1.getString("last-chosen-font");
			fontTestString = m1.getString("font-text-string");
			
			exportToSetupSection = "true".equalsIgnoreCase(m1.getString("export-to-setup")); 
			exportTargetPlatform = 0;
			try {
				exportTargetPlatform = Integer.parseInt(m1.getString("export-target-platform"));
			} catch (NumberFormatException e1) {
			} 
			
			ICommandService commandService = (ICommandService)site.getService(ICommandService.class);
			Command command = commandService.getCommand("org.zefer.pixelmeister.view.show.rulers");
			State state = command.getState(RegistryToggleState.STATE_ID);
			if (state != null) {
				ruler = ((Boolean)state.getValue()).booleanValue();
			}
			command = commandService.getCommand("org.zefer.pixelmeister.view.show.grid");
			state = command.getState(RegistryToggleState.STATE_ID);
			if (state != null) {
				grid = ((Boolean)state.getValue()).booleanValue();
			}

			command = commandService.getCommand("org.zefer.pixelmeister.autosave");
			state = command.getState(RegistryToggleState.STATE_ID);
			if (state != null) {
				autosave = ((Boolean)state.getValue()).booleanValue();
			}

			command = commandService.getCommand("org.zefer.pixelmeister.bugfix.upload");
			state = command.getState(RegistryToggleState.STATE_ID);
			if (state != null) {
				fixUpload = ((Boolean)state.getValue()).booleanValue();
			}
			
			try {
				fixUpload = "true".equalsIgnoreCase(m1.getString("upload-bugfix"));
			} catch (Exception e) {
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
		if (dirty) {
			if( prompt("There is unsaved content in editor(s). Save it?     ") == SWT.YES ) {
				save(DataLayerView.currentSketch, true);
			}
		}
		
		IMemento m1 = memento.createChild("sketch-view");

		m1.putString("emu-library", ""+emulatingLibrary);
		
		m1.putString("last-image-folder", recentImageResourcePath);
		m1.putString("last-font-folder", recentFontResourcePath);
		m1.putString("last-sketch-folder", recentSketchPath);
		m1.putString("last-sketch", currentSketch);
		m1.putString("last-export-sketch-folder", recentExportSketchPath);
		m1.putString("last-export-sketch", currentExportSketch);
		m1.putString("device-width", ""+width);
		m1.putString("device-height", ""+height);
		m1.putString("one-bit-color", ""+oneBitColor);

		m1.putString("upload-bugfix", ""+fixUpload);
		
		m1.putString("grid-step", ""+gridStepPx);

		m1.putString("last-chosen-italic", ""+lastChosenItalic);
		m1.putString("last-chosen-bold", ""+lastChosenBold);
		m1.putString("last-chosen-font", lastChosenFont);
		m1.putString("font-text-string", fontTestString);

		m1.putString("export-to-setup", ""+exportToSetupSection);
		m1.putString("export-target-platform", ""+exportTargetPlatform);
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(final Composite codePane) {
		
//		Util.setBackground( getSite(), codePane, "org.eclipse.ui.workbench.INACTIVE_TAB_BG_END" );
		
		codePane.setLayout(new FormLayout());

		shell = codePane.getShell();
		final Display display = codePane.getDisplay();
		final Clipboard cb = new Clipboard(display);

		final Shell autocompletePopup = new Shell(display, SWT.ON_TOP);
		autocompletePopup.setLayout(new FillLayout());
		final Table table = new Table(autocompletePopup, SWT.SINGLE);
		for (int i = 0; i < 1; i++) {
			new TableItem(table, SWT.NONE);
		}

		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if ( autocompletePopup.isVisible() ) {
					if (autocompletePopup.isVisible() && table.getSelectionIndex() != -1) {
						String toInsert = currentSignatures.get(table.getSelection()[0].getText());
						sketchCode.replaceTextRange(sketchCode.getCaretOffset()-proposalPrefix.length(), proposalPrefix.length(), toInsert);
						if ( proposalPrefix.length() == 0 ) {
							sketchCode.setCaretOffset(sketchCode.getCaretOffset()-proposalPrefix.length() + toInsert.length());
						}
						autocompletePopup.setVisible(false);
					}
				}
			}
		});
		
		Font font = new Font(shell.getDisplay(), "Courier New", File.pathSeparatorChar == ':' ? 12 : 10, SWT.NORMAL);
		Color bg = SWTResourceManager.getColor(255,255,255);
		
		btnRun = new Button(codePane, SWT.FLAT);
		btnRun.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				runCode();
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
				if (commandService != null) {
					commandService.refreshElements(RunCommand.ID, null);
					commandService.refreshElements(TerminateCommand.ID, null);
				}
			}
		});
		FormData fd_runButton = new FormData();
		fd_runButton.bottom = new FormAttachment(100, -2);
		fd_runButton.right = new FormAttachment(100, -10);
		fd_runButton.height = 24;
		btnRun.setLayoutData(fd_runButton);
		btnRun.setText("   Run   ");
		btnRun.setImage(Util.getImageRegistry(btnRun.getDisplay()).get("run"));
		
		btnStop = new Button(codePane, SWT.FLAT);
		btnStop.setEnabled(false);
		btnStop.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				stop = true;
				btnRun.setEnabled(true);
				btnStop.setEnabled(false);
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				ICommandService commandService = (ICommandService) window.getService(ICommandService.class);
				if (commandService != null) {
					commandService.refreshElements(RunCommand.ID, null);
					commandService.refreshElements(TerminateCommand.ID, null);
				}
				
				terminateThreads();
			}
		});
		FormData fd_btnStop = new FormData();
		fd_btnStop.bottom = new FormAttachment(btnRun, 0, SWT.BOTTOM);
		fd_btnStop.right = new FormAttachment(btnRun, -6);
		fd_btnStop.height = 24;
		btnStop.setLayoutData(fd_btnStop);
		btnStop.setText(" Terminate ");
		btnStop.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_STOP));

		errorIcon = new Label(codePane, SWT.NONE);
		errorIcon.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				System.out.println(errorIcon.getToolTipText());
			}
		});
		FormData fd_errorIcon = new FormData();
		fd_errorIcon.bottom = new FormAttachment(100, -6);
		fd_errorIcon.right = new FormAttachment(btnStop, -10);
		fd_errorIcon.width = 16;
		fd_errorIcon.height = 16;
		errorIcon.setLayoutData(fd_errorIcon);
		errorIcon.setVisible(false);
		errorIcon.setImage(Util.getImageRegistry(errorIcon.getDisplay()).get("error"));
		
		sketchCode = new StyledText(codePane, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		sketchCode.setLayoutData(new FormData());
		sketchCode.setLayoutData(new FormData());
		
		
		sketchCode.addTraverseListener(new TraverseListener() {
		      public void keyTraversed(TraverseEvent e) {
		        if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
		          e.doit = false;
		          e.detail = SWT.TRAVERSE_NONE;
		        }
		      }
		    });
		
		sketchCode.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if ( e.keyCode == 97 && e.stateMask == 262144 ) {
					if (sketchCode == null || sketchCode.isDisposed() ) {
						return;
					}
					sketchCode.selectAll();
				}
			}
		});
		sketchCode.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
//				if ( btnSave != null && btnSaveAsTemplate != null ) {
//					btnSave.setEnabled(true);
//					btnApply.setEnabled(true);
//					if (template) {
//						btnSaveAsTemplate.setEnabled(true);
//					}
//				}

				if ( sketchCode == null || sketchCode.isDisposed() ) {
					return;
				}

				int rulerWidth = Integer.toString(sketchCode.getLineCount()+1).length() * 12;
				sketchCode.setTabStops(new int[] {rulerWidth, rulerWidth + 8 * 4} );
				
				String s = sketchCode.getText();
				String o = s.replaceAll("\\r\\n", "\n");
				if ( !s.equals(o) ) {
					sketchCode.setText(o);
				}

				preCompile();
				dirty = true;
			}
		});

		
		sketchCode.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if ( autocompletePopup.isVisible() ) {
					autocompletePopup.setVisible(false);
				}
			}
		});
		
		sketchCode.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				
				if ( sketchCode == null || sketchCode.isDisposed() ) {
					return;
				}

				int firstVisibleLine = sketchCode.getTopIndex();
				int lastVisibleLine = sketchCode.getLineIndex(sketchCode.getClientArea().height-1);
				int caretLine = sketchCode.getLineAtOffset(sketchCode.getCaretOffset());
				if ( event.keyCode == SWT.CR ) {
					caretLine++;
				}
				int scroll = 0;
				if ( caretLine > lastVisibleLine ) {
					scroll = caretLine - lastVisibleLine;
				}
				if ( caretLine < firstVisibleLine ) {
					scroll = caretLine - firstVisibleLine;
				}
				if ( scroll != 0 ) {
					sketchCode.setTopIndex(firstVisibleLine+scroll);
				}
				
				if (event.keyCode == ' ' && (event.stateMask & SWT.CTRL) != 0 ) {
					addToUndo(0, sketchCode.getText());
					showProposal(codePane, sketchCode, autocompletePopup, table, 0, (char)0);
					return;
				}

				if (event.keyCode == SWT.F11 && (event.stateMask & SWT.CTRL) != 0 ) {
					runCode();
					return;
				}

				processTab(event, sketchCode);

				if (event.keyCode == 'f' && (event.stateMask & SWT.CTRL) != 0 ) {
					String preselect = null;
					Point p = sketchCode.getSelectionRange();
					if ( p.y > 0 ) {
						preselect = sketchCode.getText(p.x, p.x+p.y-1);
					}

					openFindReplaceDialog( preselect, 0 );
					event.doit = false;
					return;
				}

				if (event.character == 'z' - 'a' + 1 && (event.stateMask & SWT.CTRL) != 0 ) {
					String current = sketchCode.getText();
					String text = undo(0, current);
					if ( text != null ) {
						int ind = sketchCode.getTopIndex();
						sketchCode.setText(text);
						for ( int i = 0; i < current.length() && i < text.length(); i++ ) {
							if ( text.charAt(i) != current.charAt(i) ) {
								sketchCode.setCaretOffset(i);
								sketchCode.setTopIndex(ind);
								return;
							}
						}
						sketchCode.setCaretOffset(Math.max(current.length()-1, text.length()-1));
						sketchCode.setTopIndex(ind);
					}
					return;
				}

				if (event.character == 'y' - 'a' + 1 && (event.stateMask & SWT.CTRL) != 0 ) {
					String current = sketchCode.getText();
					String text = redo(0, current);
					if ( text != null ) {
						int ind = sketchCode.getTopIndex();
						sketchCode.setText(text);
						
						for ( int i = 0; i < current.length() && i < text.length(); i++ ) {
							if ( text.charAt(i) != current.charAt(i) ) {
								sketchCode.setCaretOffset(i+1);
								sketchCode.setTopIndex(ind);
								return;
							}
						}
						sketchCode.setCaretOffset(Math.max(current.length(), text.length()));
						sketchCode.setTopIndex(ind);
					}
					return;
				}

				if (event.character == '/' && (event.stateMask & SWT.CTRL) != 0 ) {
					Point p = sketchCode.getSelectionRange();
					if ( p.y > 0 && (p.x == 0 || "\n".equals(sketchCode.getText(p.x-1, p.x-1)))) {
						String text = sketchCode.getText(p.x, p.x+p.y-1);
						boolean toComment = true;
						if ( text.startsWith("//")) {
							toComment = false;
							for ( int i = 0; i < text.length(); i++ ) {
								char cx = text.charAt(i);
								if ( cx == '\n' && i < text.length() - 2 && (text.charAt(i+1) != '/' || text.charAt(i+2) != '/') ) {
									toComment = true;
									break;
								}
							}
							
							if (!toComment) {
								StringBuffer sb = new StringBuffer();
								for ( int i = 2; i < text.length(); i++ ) {
									char cx = text.charAt(i);
									sb.append(cx);
									if ( cx == '\n' ) {
										i+=2;
									}
								}
								text = sb.toString();
							}
						} 
						
						if (toComment) {
							text = "//" + text.replaceAll("\\n", "\n//");
							if ( text.endsWith("//")) {
								text = text.substring(0, text.length()-2);
							}
						}
						addToUndo(0, sketchCode.getText());
						sketchCode.replaceTextRange(p.x, p.y, text);
					}
					return;
				}

				if ( !autocompletePopup.isVisible() ) {
					if (event.keyCode == SWT.CR) {
						event.doit = false;

						boolean brkt = false;
						String src = sketchCode.getText();
						int caret = sketchCode.getCaretOffset();
						for ( int i = caret - 1; i >= 0; i-- ) {
							char c = src.charAt(i);
							if ( Character.isWhitespace(c) ) {
								continue;
							}
							if ( c == '{' ) {
								brkt = true;
							}
							break;
						}

						boolean addBreak = false;
						if ( brkt ) {
							for ( int i = caret; i < src.length(); i++ ) {
								char c = src.charAt(i);
								if ( !Character.isWhitespace(c) ) {
									addBreak = true;
									break;
								}
								if ( c == '\n' ) {
									break;
								}
							}
						}
						
						boolean brkt2 = brkt;

						int op = 0;
						int cl = 0;
						boolean inComment = false;
						boolean blockComment = false;
						for ( int i = 0; brkt && i < src.length(); i++ ) {
							char c = src.charAt(i);
							if ( c == '\n' && inComment && !blockComment ) {
								inComment = false;
								continue;
							}
							if ( c == '*' && i < src.length() - 1 ) {
								char c2 = src.charAt(i+1);
								if ( inComment && blockComment && c2 == '/' ) {
									inComment = false;
									i++;
									continue;
								}
								
							}
							if ( c == '/' && i < src.length() - 1 ) {
								char c2 = src.charAt(i+1);
								if ( !inComment && c2 == '/' ) {
									inComment = true;
									blockComment = false;
									i++;
									continue;
								}
								if ( !inComment && c2 == '*' ) {
									inComment = true;
									blockComment = true;
									i++;
									continue;
								}
							}
							if ( !inComment && c == '{' ) {
								op++;
							}
							if ( !inComment && c == '}' ) {
								cl++;
							}
						}
						if ( op == cl ) {
							brkt = false;
						}

						String prevIndent = "";
						int ln = sketchCode.getLineAtOffset(caret);
						for ( int j = ln; j >= 0; j-- ) {
							String s = sketchCode.getLine(j);
							prevIndent = "";
							boolean hasContent = false;
							for ( int i = 0; i < s.length(); i++ ) {
								char c = s.charAt(i);
								if ( c == '\n' ) {
									break;
								}
								if ( Character.isWhitespace(c) ) {
									prevIndent += c;
								} else {
									hasContent = true;
									break;
								}
							}
							if ( hasContent ) {
								break;
							}
						}

						if ( brkt ) {
							sketchCode.insert( "\n"+prevIndent+"}" + (addBreak ? "\n"+prevIndent : "") );
						}
						String toInsert = "\n" + (brkt2 ? "\t" : "") + prevIndent + "";
						
						int toRemove = 0;
						for ( int i = caret; i < src.length(); i++ ) {
							char c = src.charAt(i);
							if ( c == '\n' || !Character.isWhitespace(c) ) {
								break;
							}
							if ( Character.isWhitespace(c) ) {
								toRemove++;
							}
						}

						addToUndo(0, sketchCode.getText());
						
						if ( toRemove > 0 ) {
							try {
								sketchCode.replaceTextRange(caret, toRemove, "");
							} catch (Exception e) {
							}
						}

						sketchCode.insert(toInsert);
						
						sketchCode.setCaretOffset(sketchCode.getCaretOffset() + toInsert.length());
						return;
					}
					
					if( sketchCode.isDisposed() ) {
						return;
					}
					
					addToUndo(0, sketchCode.getText());
					return;
				}
				
				switch (event.keyCode) {
				case SWT.ARROW_DOWN:
					event.doit = false;
					int index = (table.getSelectionIndex() + 1) % table.getItemCount();
					table.setSelection(index);
					break;
				case SWT.ARROW_UP:
					event.doit = false;
					index = table.getSelectionIndex() - 1;
					if (index < 0) {
						index = table.getItemCount() - 1;
					}
					table.setSelection(index);
					break;
				case SWT.ARROW_RIGHT: 
					if ( autocompletePopup.isVisible() ){
						addToUndo(0, sketchCode.getText());
						showProposal(codePane, sketchCode, autocompletePopup, table, 1, (char)0);
						return;
					}
					break;
				case SWT.ARROW_LEFT: 
					if ( autocompletePopup.isVisible() ){
						addToUndo(0, sketchCode.getText());
						showProposal(codePane, sketchCode, autocompletePopup, table, -1, (char)0);
						return;
					}
					break;
				case SWT.CR:
					event.doit = false;
					if (autocompletePopup.isVisible() && table.getSelectionIndex() != -1) {
						String toInsert = currentSignatures.get(table.getSelection()[0].getText());
						addToUndo(0, sketchCode.getText());
						sketchCode.replaceTextRange(sketchCode.getCaretOffset()-proposalPrefix.length(), proposalPrefix.length(), toInsert);
						if ( proposalPrefix.length() == 0 ) {
							sketchCode.setCaretOffset(sketchCode.getCaretOffset()-proposalPrefix.length() + toInsert.length());
						}
						autocompletePopup.setVisible(false);
					}
					break;
				case SWT.ESC:
					autocompletePopup.setVisible(false);
					break;
				default:
					if ( autocompletePopup.isVisible() && event.character > 0 ){
						addToUndo(0, sketchCode.getText());
						showProposal(codePane, sketchCode, autocompletePopup, table, 0, event.character);
						return;
					}
					break;
				}
			}
		});
		sketchCode.addLineStyleListener(new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent e) {
				if (sketchCode == null || sketchCode.isDisposed()) {
					return;
				}

				e.bulletIndex = sketchCode.getLineAtOffset(e.lineOffset);          
//				e.indent = 10;
				//Set the style, 12 pixles wide for each digit   
				int rulerWidth = Integer.toString(sketchCode.getLineCount()+1).length() * 12;
				StyleRange style = new StyleRange();         
				style.metrics = new GlyphMetrics(0, 0, rulerWidth);     
				style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				e.bullet = new Bullet(ST.BULLET_NUMBER, style);     
		} });

		FocusListener focusListener = new FocusListener() {
			public void focusLost(FocusEvent e) {
				if (sketchCode == null || sketchCode.isDisposed()) {
					return;
				}

				autocompletePopup.setVisible(false);
			}

			public void focusGained(FocusEvent e) {
			}
		};
		sketchCode.addFocusListener(focusListener);
		
//		int[] tt = sketchCode.getTabStops();
		
		FormData fd_sketchCode = new FormData();
		fd_sketchCode.bottom = new FormAttachment(btnRun, -2);
		fd_sketchCode.right = new FormAttachment(100, -10);
		fd_sketchCode.top = new FormAttachment(0, 10);
		fd_sketchCode.left = new FormAttachment(0, 10);
		sketchCode.setLayoutData(fd_sketchCode);
		sketchCode.addLineStyleListener(sketchLineStyler);
		sketchCode.setEditable(true);
		sketchCode.setFont(font);
		sketchCode.setBackground(bg);
		sketchCode.setLeftMargin(5);

		Label libraryTypeLabel = new Label(codePane, SWT.NONE);
		FormData fd_libraryTypeLbl = new FormData();
		fd_libraryTypeLbl.bottom = new FormAttachment(100, -7);
		fd_libraryTypeLbl.left = new FormAttachment(0, 14);
		libraryTypeLabel.setLayoutData(fd_libraryTypeLbl);
		libraryTypeLabel.setText("Emulate library:");
		
		if ( win ) {
			libraryTypeLabel.setFont(SWTResourceManager.getFont("Tahoma", 9, SWT.NONE));
		} else {
			libraryTypeLabel.setFont(SWTResourceManager.getFont("Lucida Grande", 12, SWT.NONE));
		}
		
		Combo libraryType = new Combo(codePane, SWT.NONE);
		libraryType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DataLayerView.emulatingLibrary = ((Combo)e.widget).getSelectionIndex();
				instanceNameLabel2.setText( (DataLayerView.emulatingLibrary == 0 ? PIXELS_INSTANCE_NAME : UTFT_INSTANCE_NAME) );
				System.out.println((DataLayerView.emulatingLibrary == 0 ? PIXELS_HINT : UTEXT_HINT));
				preCompile();
			}
		});
		FormData fd_libraryType = new FormData();
		fd_libraryType.width = 100;
		fd_libraryType.bottom = new FormAttachment(100, -3);
		fd_libraryType.left = new FormAttachment(libraryTypeLabel, 2);
		libraryType.setLayoutData(fd_libraryType);
		
		String[] types = { "Pixels", "UTFT"  };
		libraryType.setItems(types);
		libraryType.select(DataLayerView.emulatingLibrary);

		Label instanceNameLabel = new Label(codePane, SWT.NONE);
		FormData fd_instanceNameLabel = new FormData();
		fd_instanceNameLabel.bottom = new FormAttachment(100, -7);
		fd_instanceNameLabel.left = new FormAttachment(libraryType, 20);
		instanceNameLabel.setLayoutData(fd_instanceNameLabel);
		instanceNameLabel.setText("instance name: " );
		
		if ( win ) {
			instanceNameLabel.setFont(SWTResourceManager.getFont("Tahoma", 9, SWT.NONE));
		} else {
			instanceNameLabel.setFont(SWTResourceManager.getFont("Lucida Grande", 12, SWT.NONE));
		}
		
		instanceNameLabel2 = new Label(codePane, SWT.NONE);
		FormData fd_instanceNameLabel2 = new FormData();
		fd_instanceNameLabel2.bottom = new FormAttachment(100, -7);
		fd_instanceNameLabel2.left = new FormAttachment(instanceNameLabel, 0);
		fd_instanceNameLabel2.width = 50;
		instanceNameLabel2.setLayoutData(fd_instanceNameLabel2);
		instanceNameLabel2.setText( (DataLayerView.emulatingLibrary == 0 ? PIXELS_INSTANCE_NAME : UTFT_INSTANCE_NAME) );
		
		if ( win ) {
			instanceNameLabel2.setFont(SWTResourceManager.getFont("Tahoma", 9, SWT.BOLD));
		} else {
			instanceNameLabel2.setFont(SWTResourceManager.getFont("Lucida Grande", 12, SWT.BOLD));
		}
		
		instanceNameLabel.setVisible(false);
		instanceNameLabel2.setVisible(false);
		
		
		btnSlowdownWhile = new Button(codePane, SWT.CHECK);
		btnSlowdownWhile.setLayoutData(new FormData());
		btnSlowdownWhile.setSelection(true);
		btnSlowdownWhile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				slowdownWhile = ((Button)e.widget).getSelection();
				preCompile();
				dirty = true;
			}
		});
		FormData fd_btnSlowdownWhile = new FormData();
		fd_btnSlowdownWhile.bottom = new FormAttachment(btnRun, -3, SWT.BOTTOM);
		fd_btnSlowdownWhile.left = new FormAttachment(sketchCode, 10, SWT.LEFT);
		btnSlowdownWhile.setLayoutData(fd_btnSlowdownWhile);
		btnSlowdownWhile.setText("Slowdown while()");
		btnSlowdownWhile.setVisible(false);
		
		if ( currentSketch != null ) {
			loadSketch(currentSketch, false);
		}

		IHandlerService serv = (IHandlerService) getSite().getService(IHandlerService.class);
		serv.activateHandler(org.eclipse.ui.IWorkbenchCommandConstants.EDIT_PASTE, new AbstractHandler() {
			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {

				if (sketchCode == null || sketchCode.isDisposed()) {
					return null;
				}

				Object t = event.getTrigger();
				if( t instanceof Event && ((Event)t).widget == sketchCode ) {
					HandlerUtil.getActivePart(event);

					TextTransfer transfer = TextTransfer.getInstance();
					String data = (String)cb.getContents(transfer);
					if (data != null) {
						
						data = data.replaceAll("\\r\\n", "\n");
						
						HashSet instanceNames = emulatingLibrary == 0 ?
								findInstanceNames(data, pixelsSignatures) : 
								findInstanceNames(data, utftSignatures); // XXX
						if ( instanceNames.size() > 0 ) {
							String nx = (String)instanceNames.iterator().next();
							switch ( emulatingLibrary ) {
							case 0:
								if ( !PIXELS_INSTANCE_NAME.equals(nx) ) {
									data = data.replaceAll(nx, PIXELS_INSTANCE_NAME);
								}
								break;
							case 1:
								if ( !UTFT_INSTANCE_NAME.equals(nx) ) {
									data = data.replaceAll(nx, UTFT_INSTANCE_NAME);
								}
								break;
							}
						}
//						instanceNames = findInstanceNames(data, utextSignatures);
//						if ( instanceNames.size() > 0 ) {
//							String nx = (String)instanceNames.iterator().next();
//							if ( !UTEXT_INSTANCE_NAME.equals(nx) ) {
//								data = data.replaceAll(nx, UTEXT_INSTANCE_NAME);
//							}
//						}
						
						
						int off = sketchCode.getCaretOffset();
						int firstVisibleLine = sketchCode.getTopIndex();
						
						String prev = off == 0 || off == sketchCode.getText().length() ? "\n" : sketchCode.getText(off-1, off);
						if ( prev.charAt(0) == ' ' || prev.charAt(0) == '\t' ) {
							data = data.replaceAll("^\\s+", "");
						}
						
						Point p = sketchCode.getSelectionRange();
						
						sketchCode.insert(data);
						sketchCode.setCaretOffset(off+data.length()-p.y);
						sketchCode.setTopIndex(firstVisibleLine);

						sketchCode.setSelection(sketchCode.getCaretOffset(), sketchCode.getCaretOffset());
						sketchCode.showSelection();
					}
				}
				
				return null;
			}
		});
	}

	
	
	public void setFocus() {
		if ( sketchInit != null ) {
			if (sketchCode == null || sketchCode.isDisposed()) {
				return;
			}

			boolean sav = DataLayerView.dirty;
			sketchCode.setText(sketchInit);
			sketchInit = null;
			DataLayerView.dirty = sav;
		}
		
		DataLayerView.findActionTarget = 0;
	}

	public void redrawCanvas() {
		if (sketchCode == null || sketchCode.isDisposed()) {
			return;
		}

		DeviceView device = null;
		IViewReference[] refs = getSite().getPage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( ORG_ZEFER_LCDPAINTER_DEVICEVIEW.equals(refs[i].getId()) ) {
				device = (DeviceView)refs[i].getView(true);
				device.redrawCanvas(0, 0, width + rulerWidth, height + rulerHeight);
			}
		}
	}
	
	public String readResourceText() {
		if (sketchCode == null || sketchCode.isDisposed()) {
			return "";
		}

		ResourcesView resources = null;
		IViewReference[] refs = getSite().getPage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( ORG_ZEFER_LCDPAINTER_RESOURCESVIEW.equals(refs[i].getId()) ) {
				resources = (ResourcesView)refs[i].getView(true);
				return resources.readResourceText();
			}
		}
		return "";
	}

	public String readResourceRawText() {
		if (sketchCode == null || sketchCode.isDisposed()) {
			return "";
		}

		ResourcesView resources = null;
		IViewReference[] refs = getSite().getPage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( ORG_ZEFER_LCDPAINTER_RESOURCESVIEW.equals(refs[i].getId()) ) {
				resources = (ResourcesView)refs[i].getView(true);
				return resources.readResourceRawText();
			}
		}
		return "";
	}

	public void runCode() {
		terminateThreads();

		String src = preprocess( buildJavaClass( emulatingLibrary ) );
		if ( dumpSrc ) {
			System.out.println(src);
		}
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		instance = compileJava(src, diagnostics);
		
		if ( instance != null ) {
			redrawCanvas();
			btnStop.setEnabled(true);
			btnRun.setEnabled(false);
			if ( autosave ) {
				save(currentSketch, true);
			}
			DataLayerView.stop = false;
			DataLayerView.deviceScroll = 0;
			try {
				((ScriptBase)instance).setContextClassLoader(SWTResourceManager.class.getClassLoader());
				((ScriptBase)instance).prepare(shell.getDisplay(), canvas, width, height, parsedArrays);
				((ScriptBase)instance).setName(THREAD_PAINTER_PERFIX + System.currentTimeMillis());
				((ScriptBase)instance).start();
			} catch (IllegalThreadStateException e1) {
			}
		}
	}
}
