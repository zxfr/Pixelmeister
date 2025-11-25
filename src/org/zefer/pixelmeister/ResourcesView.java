package org.zefer.pixelmeister;

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.wb.swt.SWTResourceManager;
import org.zefer.pixelmeister.util.CenteredFileDialog;

public class ResourcesView extends DataLayerView {
	
	public ResourcesView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		IViewReference[] refs = getSite().getPage().getViewReferences();
		for ( int i = 0; i < refs.length; i++ ) {
			if ( ORG_ZEFER_LCDPAINTER_CODEVIEW.equals(refs[i].getId()) ) {
				refs[i].getView(true);
			}
		}
	}
	
	@Override
	public void createPartControl(final Composite globalsPane) {
		
		globalsPane.setLayout(new FormLayout());
		
		final Display display = globalsPane.getDisplay();
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
						globalsCode.replaceTextRange(globalsCode.getCaretOffset()-proposalPrefix.length(), proposalPrefix.length(), toInsert);
						if ( proposalPrefix.length() == 0 ) {
							globalsCode.setCaretOffset(globalsCode.getCaretOffset()-proposalPrefix.length() + toInsert.length());
						}
						autocompletePopup.setVisible(false);
					}
				}
			}
		});
		
		
		Font font = new Font(globalsPane.getDisplay(), "Courier New", File.pathSeparatorChar == ':' ? 12 : 10, SWT.NORMAL);
		Color bg = SWTResourceManager.getColor(255,255,255);

//		Button btnImportImage = new Button(globalsPane, SWT.NONE);
//		btnImportImage.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				importImageActionHandler();
//			}
//		});
//		FormData fd_btnImportResource = new FormData();
//		fd_btnImportResource.right = new FormAttachment(100, -8);
//		fd_btnImportResource.bottom = new FormAttachment(100, -2);
//		btnImportImage.setLayoutData(fd_btnImportResource);
//		btnImportImage.setText("Import Image...");
//		btnImportImage.setImage(Util.getImageRegistry(btnImportImage.getDisplay()).get("importimage"));
//
//		Button btnImportFont = new Button(globalsPane, SWT.NONE);
//		btnImportFont.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				importFontActionHandler();
//			}
//		});
//		FormData fd_btnImportFont = new FormData();
//		fd_btnImportFont.bottom = new FormAttachment(btnImportImage, 0, SWT.BOTTOM);
//		fd_btnImportFont.right = new FormAttachment(btnImportImage, -6);
//		btnImportFont.setLayoutData(fd_btnImportFont);
//		btnImportFont.setText("Import Font...");
//		btnImportFont.setImage(Util.getImageRegistry(btnImportFont.getDisplay()).get("importfont"));

		globalsCode = new StyledText(globalsPane, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		
		globalsCode.addTraverseListener(new TraverseListener() {
		      public void keyTraversed(TraverseEvent e) {
		        if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
		          e.doit = false;
		          e.detail = SWT.TRAVERSE_NONE;
		        }
		      }
		    });
		
		globalsCode.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if ( e.keyCode == 97 && e.stateMask == 262144 ) {
					if (globalsCode == null || globalsCode.isDisposed()) {
						return;
					}
					globalsCode.selectAll();
				}
			}
		});
		
		globalsCode.addCaretListener(new CaretListener() {
			public void caretMoved(CaretEvent event) {
				if ( findReplaceDialog != null ) {
					findReplaceDialog.updateUIAvailability();
				}
			}
		});
		
		globalsCode.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
//				if ( btnSave != null && btnSaveAsTemplate != null ) {
//					btnSave.setEnabled(true);
//					btnApply.setEnabled(true);
//					if (template) {
//						btnSaveAsTemplate.setEnabled(true);
//					}
//				}

				if (globalsCode == null || globalsCode.isDisposed()) {
					return;
				}
				
				String s = globalsCode.getText();
				String o = s.replaceAll("\\r\\n", "\n");
				if ( !s.equals(o) ) {
					globalsCode.setText(o);
				}

				preCompile();
				dirty = true;
			}
		});
		
		globalsCode.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				
				if (globalsCode == null || globalsCode.isDisposed()) {
					return;
				}

				int firstVisibleLine = globalsCode.getTopIndex();
				int lastVisibleLine = globalsCode.getLineIndex(globalsCode.getClientArea().height-1);
				int caretLine = globalsCode.getLineAtOffset(globalsCode.getCaretOffset());
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
					globalsCode.setTopIndex(firstVisibleLine+scroll);
				}
				
				if (event.keyCode == ' ' && (event.stateMask & SWT.CTRL) != 0 ) {
					addToUndo(0, globalsCode.getText());
					showProposal(globalsPane, globalsCode, autocompletePopup, table, 0, (char)0);
					return;
				}

//				if (event.keyCode == SWT.F11 && (event.stateMask & SWT.CTRL) != 0 ) {
//					runCode();
//					return;
//				}
				
				processTab(event, globalsCode);

				if (event.keyCode == 'f' && (event.stateMask & SWT.CTRL) != 0 ) {
					String preselect = null;
					Point p = globalsCode.getSelectionRange();
					if ( p.y > 0 ) {
						preselect = globalsCode.getText(p.x, p.x+p.y-1);
					}

					openFindReplaceDialog( preselect, 1 );
					event.doit = false;
					return;
				}
				
				if (event.keyCode == 'z' && (event.stateMask & SWT.CTRL) != 0 ) {
					String current = globalsCode.getText();
					String text = undo(1, current);
					if ( text != null ) {
						int ind = globalsCode.getTopIndex();
						globalsCode.setText(text);
						for ( int i = 0; i < current.length() && i < text.length(); i++ ) {
							if ( text.charAt(i) != current.charAt(i) ) {
								globalsCode.setCaretOffset(i);
								globalsCode.setTopIndex(ind);
								return;
							}
						}
						globalsCode.setCaretOffset(Math.max(current.length()-1, text.length()-1));
						globalsCode.setTopIndex(ind);
					}
					return;
				}

				if (event.character == 'y' - 'a' + 1 && (event.stateMask & SWT.CTRL) != 0 ) {
					String current = globalsCode.getText();
					String text = redo(1, current);
					if ( text != null ) {
						int ind = globalsCode.getTopIndex();
						globalsCode.setText(text);
						for ( int i = 0; i < current.length() && i < text.length(); i++ ) {
							if ( text.charAt(i) != current.charAt(i) ) {
								globalsCode.setCaretOffset(i+1);
								globalsCode.setTopIndex(ind);
								return;
							}
						}
						globalsCode.setCaretOffset(Math.max(current.length(), text.length()));
						globalsCode.setTopIndex(ind);
					}
					return;
				}

				if (event.character == '/' && (event.stateMask & SWT.CTRL) != 0 ) {
					Point p = globalsCode.getSelectionRange();
					if ( p.y > 0 && (p.x == 0 || "\n".equals(globalsCode.getText(p.x-1, p.x-1)))) {
						String text = globalsCode.getText(p.x, p.x+p.y-1);
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
						addToUndo(1, globalsCode.getText());
						globalsCode.replaceTextRange(p.x, p.y, text);
					}
					return;
				}

				if ( !autocompletePopup.isVisible() ) {
					if (event.keyCode == SWT.CR) {
						event.doit = false;

						boolean brkt = false;
						String src = globalsCode.getText();
						int caret = globalsCode.getCaretOffset();
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
						int ln = globalsCode.getLineAtOffset(caret);
						for ( int j = ln; j >= 0; j-- ) {
							String s = globalsCode.getLine(j);
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
							globalsCode.insert( "\n"+prevIndent+"}" + (addBreak ? "\n"+prevIndent : "") );
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

						addToUndo(1, globalsCode.getText());
						
						if ( toRemove > 0 ) {
							try {
								globalsCode.replaceTextRange(caret, toRemove, "");
							} catch (Exception e) {
							}
						}

						globalsCode.insert(toInsert);
						
						globalsCode.setCaretOffset(globalsCode.getCaretOffset() + toInsert.length());
						return;
					}
					
					if( globalsCode.isDisposed() ) {
						return;
					}
					addToUndo(1, globalsCode.getText());
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
						addToUndo(0, globalsCode.getText());
						showProposal(globalsPane, globalsCode, autocompletePopup, table, 1, (char)0);
						return;
					}
					break;
				case SWT.ARROW_LEFT: 
					if ( autocompletePopup.isVisible() ){
						addToUndo(0, globalsCode.getText());
						showProposal(globalsPane, globalsCode, autocompletePopup, table, -1, (char)0);
						return;
					}
					break;
				case SWT.CR:
					event.doit = false;
					if (autocompletePopup.isVisible() && table.getSelectionIndex() != -1) {
						String toInsert = currentSignatures.get(table.getSelection()[0].getText());
						addToUndo(0, globalsCode.getText());
						globalsCode.replaceTextRange(globalsCode.getCaretOffset()-proposalPrefix.length(), proposalPrefix.length(), toInsert);
						if ( proposalPrefix.length() == 0 ) {
							globalsCode.setCaretOffset(globalsCode.getCaretOffset()-proposalPrefix.length() + toInsert.length());
						}
						autocompletePopup.setVisible(false);
					}
					break;
				case SWT.ESC:
					autocompletePopup.setVisible(false);
					break;
				default:
					if ( autocompletePopup.isVisible() && event.character > 0 ){
						addToUndo(0, globalsCode.getText());
						showProposal(globalsPane, globalsCode, autocompletePopup, table, 0, event.character);
						return;
					}
					break;
				}
			}
		});
		
		
		globalsCode.addLineStyleListener(new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent e) {
				if (globalsCode == null || globalsCode.isDisposed()) {
					return;
				}

				e.bulletIndex = globalsCode.getLineAtOffset(e.lineOffset);          
				e.indent = 10;
				//Set the style, 12 pixles wide for each digit         
				StyleRange style = new StyleRange();         
				style.metrics = new GlyphMetrics(0, 0, Integer.toString(globalsCode.getLineCount()+1).length() * 12);     
				style.foreground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
				e.bullet = new Bullet(ST.BULLET_NUMBER, style);     
		} }); 

		FocusListener focusListener = new FocusListener() {
			public void focusLost(FocusEvent e) {
				if ( autocompletePopup != null ) {
					autocompletePopup.setVisible(false);
				}
			}

			public void focusGained(FocusEvent e) {
			}
		};
		globalsCode.addFocusListener(focusListener);
		
		
		FormData fd_globalsCode = new FormData();
		fd_globalsCode.bottom = new FormAttachment(100, -8);
		fd_globalsCode.right = new FormAttachment(100, -10);
		fd_globalsCode.top = new FormAttachment(0, 10);
		fd_globalsCode.left = new FormAttachment(0, 10);
		globalsCode.setLayoutData(fd_globalsCode);
		globalsCode.addLineStyleListener(globalsLineStyler); 
		globalsCode.setEditable(true);
		globalsCode.setFont(font);
		globalsCode.setBackground(bg);
		globalsCode.setLeftMargin(5);

	
		IHandlerService serv = (IHandlerService) getSite().getService(IHandlerService.class);
		serv.activateHandler(org.eclipse.ui.IWorkbenchCommandConstants.EDIT_PASTE, new AbstractHandler() {
			@Override
			public Object execute(ExecutionEvent event) throws ExecutionException {

				if (globalsCode == null || globalsCode.isDisposed()) {
					return null;
				}

				Object t = event.getTrigger();
				if( t instanceof Event && ((Event)t).widget == globalsCode ) {
					HandlerUtil.getActivePart(event);
					
					TextTransfer transfer = TextTransfer.getInstance();
					String data = (String)cb.getContents(transfer);
					if (data != null) {
						
						data = data.replaceAll("\\r\\n", "\n");
						
						int off = globalsCode.getCaretOffset();
						int firstVisibleLine = globalsCode.getTopIndex();
						
						String prev = off == 0 || off == globalsCode.getText().length() ? "\n" : globalsCode.getText(off-1, off);
						if ( prev.charAt(0) == ' ' || prev.charAt(0) == '\t' ) {
							data = data.replaceAll("^\\s+", "");
						}

						Point p = globalsCode.getSelectionRange();
						
						globalsCode.insert(data);
						globalsCode.setCaretOffset(off+data.length()-p.y);
						globalsCode.setTopIndex(firstVisibleLine);	
						
						globalsCode.setSelection(globalsCode.getCaretOffset(), globalsCode.getCaretOffset());
						globalsCode.showSelection();
					}
				}
				
				return null;
			}
		});
	}

	@Override
	public void setFocus() {
		if (globalsCode == null || globalsCode.isDisposed() ) {
			return;
		}

		if ( globalsInit != null ) {
			boolean sav = DataLayerView.dirty;
			globalsCode.setText(globalsInit);
			globalsInit = null;
			DataLayerView.dirty = sav;
		}
		
		DataLayerView.findActionTarget = 1;
	}
	
	public static void importFontActionHandler() {
		
		ChooseFontDialog ifmd = new ChooseFontDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
		ifmd.open();
		
//		CenteredFileDialog resourceDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
//		resourceDialog.dialog.setFilterExtensions(new String[] { "*.ttf;*.otf;*.ttc" });
//		resourceDialog.dialog.setFilterNames(new String[] { "True Type Font (TTF, OTF, TTC)" });
//		resourceDialog.dialog.setFilterPath(recentFontResourcePath);
//		String f = resourceDialog.dialog.open();
//		if ( f != null ) {
//			recentFontResourcePath = resourceDialog.dialog.getFilterPath();
//			ImportFontDialog ifd = new ImportFontDialog(DataLayerView.getShell(), f);
//			ifd.open();
//		}
	}

	public static void importImageActionHandler() {
		CenteredFileDialog resourceDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
		resourceDialog.dialog.setFilterExtensions(new String[] { "*.jpg;*.jpeg;*.png;*.gif;*.bmp" });
		resourceDialog.dialog.setFilterNames(new String[] { "Image (JPEG, PNG, GIF, BMP)" });
		resourceDialog.dialog.setFilterPath(recentImageResourcePath);
		String f = resourceDialog.dialog.open();
		if ( f != null ) {
			recentImageResourcePath = resourceDialog.dialog.getFilterPath();
			ImportImageDialog iid = new ImportImageDialog(DataLayerView.getShell(), f);
			iid.open();
		}
	}

	public static void importClipartIconActionHandler() {
//		CenteredFileDialog resourceDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
//		resourceDialog.dialog.setFilterExtensions(new String[] { "*.jpg;*.jpeg;*.png;*.gif;*.bmp" });
//		resourceDialog.dialog.setFilterNames(new String[] { "Image (JPEG, PNG, GIF, BMP)" });
//		resourceDialog.dialog.setFilterPath(recentImageResourcePath);
//		String f = resourceDialog.dialog.open();
//		if ( f != null ) {
//			recentImageResourcePath = resourceDialog.dialog.getFilterPath();
			ImportIconDialog iid = new ImportIconDialog(DataLayerView.getShell());
			iid.open();
//		}
	}

	public static void importFontIconActionHandler() {
//		CenteredFileDialog resourceDialog = new CenteredFileDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.OPEN);
//		resourceDialog.dialog.setFilterExtensions(new String[] { "*.jpg;*.jpeg;*.png;*.gif;*.bmp" });
//		resourceDialog.dialog.setFilterNames(new String[] { "Image (JPEG, PNG, GIF, BMP)" });
//		resourceDialog.dialog.setFilterPath(recentImageResourcePath);
//		String f = resourceDialog.dialog.open();
//		if ( f != null ) {
//			recentImageResourcePath = resourceDialog.dialog.getFilterPath();
			ImportIconDialog iid = new ImportIconDialog(DataLayerView.getShell());
			iid.open();
//		}
	}
}
