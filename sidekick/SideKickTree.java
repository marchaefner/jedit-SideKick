/*
 * SideKickTree.java
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2000, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package sidekick;

//{{{ Imports
import javax.swing.event.*;
import javax.swing.tree.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.msg.*;
import org.gjt.sp.jedit.textarea.*;
import org.gjt.sp.jedit.*;
//}}}

public class SideKickTree extends JPanel implements EBComponent
{
	//{{{ SideKickTree constructor
	public SideKickTree(View view, boolean docked)
	{
		super(new BorderLayout());

		this.view = view;

		// create toolbar with parse button
		JToolBar buttonBox = new JToolBar();
		buttonBox.setFloatable(false);

		parseBtn = new RolloverButton(GUIUtilities.loadIcon("Parse.png"));
		parseBtn.setToolTipText(jEdit.getProperty("sidekick-tree.parse"));
		parseBtn.setMargin(new Insets(0,0,0,0));
		parseBtn.setRequestFocusEnabled(false);
		parseBtn.addActionListener(new ActionHandler());
		buttonBox.add(parseBtn);
		buttonBox.add(Box.createGlue());

		add(BorderLayout.NORTH,buttonBox);

		// create a faux model that will do until a real one arrives
		DefaultTreeModel emptyModel = new DefaultTreeModel(
			new DefaultMutableTreeNode(null));
		tree = new CustomTree(emptyModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addKeyListener(new KeyHandler());
		if(docked)
			tree.addMouseMotionListener(new MouseHandler());

		// looks bad with the OS X L&F, apparently...
		if(!OperatingSystem.isMacOSLF())
			tree.putClientProperty("JTree.lineStyle", "Angled");

		tree.setVisibleRowCount(10);
		tree.setCellRenderer(new Renderer());

		add(BorderLayout.CENTER,new JScrollPane(tree));

		propertiesChanged();

		CaretHandler caretListener = new CaretHandler();

		EditPane[] editPanes = view.getEditPanes();
		for(int i = 0; i < editPanes.length; i++)
		{
			editPanes[i].getTextArea().addCaretListener(
				caretListener);
		}

		update();
	} //}}}

	//{{{ requestDefaultFocus() method
	public boolean requestDefaultFocus()
	{
		tree.requestFocus();
		return true;
	} //}}}

	//{{{ addNotify() method
	public void addNotify()
	{
		super.addNotify();
		EditBus.addToBus(this);
	} //}}}

	//{{{ removeNotify() method
	public void removeNotify()
	{
		super.removeNotify();
		EditBus.removeFromBus(this);
	} //}}}

	//{{{ handleMessage() method
	public void handleMessage(EBMessage msg)
	{
		if(msg instanceof EditPaneUpdate)
		{
			EditPaneUpdate epu = (EditPaneUpdate)msg;
			EditPane editPane = epu.getEditPane();

			if(epu.getWhat() == EditPaneUpdate.CREATED)
				editPane.getTextArea().addCaretListener(new CaretHandler());
		}
		else if(msg instanceof PropertiesChanged)
			propertiesChanged();
		else if(msg instanceof SideKickUpdate)
		{
			if(((SideKickUpdate)msg).getView() == view)
				update();
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private RolloverButton parseBtn;
	private JTree tree;

	private int showAttributes;

	private View view;
	private Timer caretTimer;
	//}}}

	//{{{ propertiesChanged() method
	private void propertiesChanged()
	{
		//showAttributes = jEdit.getIntegerProperty("xml.show-attributes",0);
	} //}}}

	//{{{ update() method
	private void update()
	{
		SideKickParsedData data = SideKickParsedData.getParsedData(view.getEditPane());
		if(SideKickPlugin.getParserForBuffer(view.getBuffer()) == null
			|| data == null)
		{
			DefaultMutableTreeNode root = new DefaultMutableTreeNode(view.getBuffer().getName());
			root.insert(new DefaultMutableTreeNode(
				jEdit.getProperty("sidekick-tree.not-parsed")),0);

			tree.setModel(new DefaultTreeModel(root));
		}
		else
		{
			tree.setModel(data.tree);
			expandTreeAt(view.getTextArea().getCaretPosition());
		}
	} //}}}

	//{{{ expandTreeWithDelay() method
	private void expandTreeWithDelay()
	{
		// if keystroke parse timer is running, do nothing
		// if(keystrokeTimer != null && keystrokeTimer.isRunning())
			// return;

		if(caretTimer != null)
			caretTimer.stop();

		caretTimer = new Timer(0,new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				JEditTextArea textArea = view.getTextArea();
				int caret = textArea.getCaretPosition();
				Selection s = textArea.getSelectionAtOffset(caret);
				expandTreeAt(s == null ? caret : s.getStart());
			}
		});

		caretTimer.setInitialDelay(500);
		caretTimer.setRepeats(false);
		caretTimer.start();
	} //}}}

	//{{{ expandTreeAt() method
	private void expandTreeAt(int dot)
	{
		DefaultMutableTreeNode root = (DefaultMutableTreeNode)tree
			.getModel().getRoot();

		if(root.getChildCount() == 0)
			return;

		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
			root.getChildAt(0);
		if(node.getUserObject() instanceof Asset)
		{
			Vector _path = new Vector();
			expandTreeAt(node,dot,_path);
			_path.addElement(node);
			_path.addElement(root);

			Object[] path = new Object[_path.size()];
			for(int i = 0; i < path.length; i++)
				path[i] = _path.elementAt(path.length - i - 1);

			TreePath treePath = new TreePath(path);
			tree.expandPath(treePath);
			tree.setSelectionPath(treePath);
			tree.scrollPathToVisible(treePath);
		}
	} //}}}

	//{{{ expandTreeAt() method
	private boolean expandTreeAt(TreeNode node, int dot, Vector path)
	{
		int childCount = node.getChildCount();
		Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
		Asset asset = (Asset)userObject;

		if(childCount != 0)
		{
			// check if any of our children contain the caret
			for(int i = childCount - 1; i >= 0; i--)
			{
				TreeNode _node = node.getChildAt(i);
				if(expandTreeAt(_node,dot,path))
				{
					path.addElement(_node);
					return true;
				}
			}
		}

		// check if the caret in inside this tag
		if(dot >= asset.start.getOffset() && (asset.end == null
			|| dot < asset.end.getOffset()))
		{
			//path.addElement(node);
			return true;
		}
		else
			return false;
	} //}}}

	//}}}

	//{{{ Inner classes

	//{{{ CustomTree class
	class CustomTree extends JTree
	{
		CustomTree(TreeModel model)
		{
			super(model);
		}

		protected void processMouseEvent(MouseEvent evt)
		{
			switch(evt.getID())
			{
			//{{{ MOUSE_PRESSED...
			case MouseEvent.MOUSE_PRESSED:
				TreePath path = getPathForLocation(
					evt.getX(),evt.getY());
				if(path != null)
				{
					Object value = ((DefaultMutableTreeNode)path
						.getLastPathComponent()).getUserObject();

					if(value instanceof Asset)
					{
						Asset asset = (Asset)value;

						JEditTextArea textArea = view.getTextArea();

						/* if(evt.getClickCount() == 2)
						{
							textArea.setCaretPosition(tag.start.getOffset());
							expandPath(path);
							XmlActions.showEditTagDialog(view);
							return;
						}
						else */
						if(evt.isShiftDown() && asset.end != null)
						{
							textArea.setCaretPosition(asset.end.getOffset());
							textArea.addToSelection(
								new Selection.Range(
									asset.start.getOffset(),
									asset.end.getOffset()));
						}
						else
							textArea.setCaretPosition(asset.start.getOffset());
					}
				}

				super.processMouseEvent(evt);
				break; //}}}
			//{{{ MOUSE_EXITED...
			case MouseEvent.MOUSE_EXITED:
				view.getStatus().setMessage(null);
				super.processMouseEvent(evt);
				break; //}}}
			default:
				super.processMouseEvent(evt);
				break;
			}
		}
	} //}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			//XmlPlugin.getParser(view).parse(true);
		}
	} //}}}

	//{{{ CaretHandler class
	class CaretHandler implements CaretListener
	{
		public void caretUpdate(CaretEvent evt)
		{
			if(evt.getSource() == view.getTextArea())
				expandTreeWithDelay();
		}
	} //}}}

	//{{{ KeyHandler class
	class KeyHandler extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			if(caretTimer != null)
				caretTimer.stop();

			if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			{
				evt.consume();

				TreePath path = tree.getSelectionPath();

				if(path != null)
				{
					Object value = ((DefaultMutableTreeNode)path
						.getLastPathComponent()).getUserObject();

					if(value instanceof Asset)
					{
						Asset asset = (Asset)value;

						JEditTextArea textArea = view.getTextArea();

						if(evt.isShiftDown() && asset.end != null)
						{
							textArea.setCaretPosition(asset.end.getOffset());
							textArea.addToSelection(
								new Selection.Range(
									asset.start.getOffset(),
									asset.end.getOffset()));
						}
						else
							textArea.setCaretPosition(asset.start.getOffset());
					}
				}
			}
		}
	} //}}}

	//{{{ MouseHandler class
	class MouseHandler extends MouseMotionAdapter
	{
		public void mouseMoved(MouseEvent evt)
		{
			TreePath path = tree.getPathForLocation(
				evt.getX(),evt.getY());
			if(path == null)
				view.getStatus().setMessage(null);
			else
			{
				Object value = ((DefaultMutableTreeNode)path
					.getLastPathComponent()).getUserObject();

				if(value instanceof Asset)
				{
					view.getStatus().setMessage(value.toString());
				}
			}
		}
	} //}}}

	//{{{ Renderer class
	class Renderer extends DefaultTreeCellRenderer
	{
		public Component getTreeCellRendererComponent(JTree tree,
			Object value, boolean sel, boolean expanded,
			boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree,value,sel,
				expanded,leaf,row,hasFocus);

			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object nodeValue = node.getUserObject();
			if(nodeValue instanceof Asset)
			{
				Asset asset = (Asset)node.getUserObject();

				setText(asset.getAttributeString(showAttributes));
				setIcon(asset.getIcon());
			}
			// is root?
			else if(node.getParent() == null)
			{
				setIcon(org.gjt.sp.jedit.browser.FileCellRenderer
					.fileIcon);
			}
			else
				setIcon(null);

			return this;
		}
	} //}}}

	//}}}
}
