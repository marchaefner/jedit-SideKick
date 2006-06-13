/*
 * SideKickOptionPane.java - SideKick options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
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
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.Hashtable;
import org.gjt.sp.jedit.gui.*;
import org.gjt.sp.jedit.*;
//}}}

public class SideKickOptionPane extends AbstractOptionPane
{
	//{{{ GeneralOptionPane constructor
	public SideKickOptionPane()
	{
		super("sidekick");
	} //}}}

	//{{{ _init() method
	protected void _init()
	{
		addSeparator();
		addComponent(new JLabel("General Settings"));
		addSeparator();
		addComponent(showToolTips = new JCheckBox(jEdit.getProperty(
			"options.sidekick.showToolTips.label")));
		showToolTips.setSelected(jEdit.getBooleanProperty(
			"options.sidekick.showToolTips"));
		addComponent(showStatusWindow = new JCheckBox(jEdit.getProperty(
			"options.sidekick.showStatusWindow.label")));
		showStatusWindow.setSelected(jEdit.getBooleanProperty(
			"options.sidekick.showStatusWindow"));
		addComponent(treeFollowsCaret = new JCheckBox(jEdit.getProperty(
			"options.sidekick.tree-follows-caret")));
		treeFollowsCaret.setSelected(jEdit.getBooleanProperty(
			"sidekick-tree.follows-caret"));
		treeFollowsCaret.addActionListener(new ActionHandler());
		
		addComponent(jEdit.getProperty("options.sidekick.auto-expand-tree-depth"),
			autoExpandTreeDepth = new JComboBox());
		autoExpandTreeDepth.addActionListener(new ActionHandler());
		autoExpandTreeDepth.addItem("All");
		for (int i = 0; i <= 10; i++)
			autoExpandTreeDepth.addItem(String.valueOf(i));
		String depth = String.valueOf(jEdit.getIntegerProperty(
			"sidekick-tree.auto-expand-tree-depth", 1));
		autoExpandTreeDepth.setSelectedItem(depth);

		addSeparator();
		addComponent(new JLabel("Parsing Settings"));
		addSeparator();
		addComponent(bufferChangeParse = new JCheckBox(jEdit.getProperty(
			"options.sidekick.buffer-change-parse")));
		bufferChangeParse.setSelected(jEdit.getBooleanProperty(
			"buffer.sidekick.buffer-change-parse"));
		bufferChangeParse.addActionListener(new ActionHandler());

		addComponent(keystrokeParse = new JCheckBox(jEdit.getProperty(
			"options.sidekick.keystroke-parse")));
		keystrokeParse.setSelected(jEdit.getBooleanProperty(
			"buffer.sidekick.keystroke-parse"));
		keystrokeParse.addActionListener(new ActionHandler());

		int autoParseDelayValue;
		try
		{
			autoParseDelayValue = Integer.parseInt(jEdit.getProperty("sidekick.auto-parse-delay"));
		}
		catch(NumberFormatException nf)
		{
			autoParseDelayValue = 1500;
		}

		addComponent(new JLabel(jEdit.getProperty("options.sidekick.auto-parse-delay")));
		addComponent(autoParseDelay = new JSlider(200,3000,autoParseDelayValue),
			GridBagConstraints.BOTH);
		Hashtable labelTable = new Hashtable();
		for(int i = 500; i <= 3000; i += 500)
		{
			labelTable.put(new Integer(i),new JLabel(
				String.valueOf((double)i / 1000.0)));
		}
		autoParseDelay.setLabelTable(labelTable);
		autoParseDelay.setPaintLabels(true);
		autoParseDelay.setMajorTickSpacing(500);
		autoParseDelay.setPaintTicks(true);

		autoParseDelay.setEnabled(keystrokeParse.isSelected());

		addSeparator();
		addComponent(new JLabel("Code Completion Settings"));
		addSeparator();
		addComponent(completeInstantToggle = new JCheckBox(jEdit.getProperty(
			"options.sidekick.complete-instant.toggle")));
		completeInstantToggle.setSelected(jEdit.getBooleanProperty("sidekick.complete-instant.toggle"));
		completeInstantToggle.addActionListener(new ActionHandler());

		addComponent(completeDelayToggle = new JCheckBox(jEdit.getProperty(
			"options.sidekick.complete-delay.toggle")));
		completeDelayToggle.setSelected(jEdit.getBooleanProperty("sidekick.complete-delay.toggle"));
		completeDelayToggle.addActionListener(new ActionHandler());

		int completeDelayValue = jEdit.getIntegerProperty("sidekick.complete-delay",500);

		addComponent(new JLabel(jEdit.getProperty("options.sidekick.complete-delay")));
		addComponent(completeDelay = new JSlider(0,1500,completeDelayValue),
		GridBagConstraints.BOTH);

		labelTable = new Hashtable();
		for(int i = 0; i <= 1500; i += 250)
		{
			labelTable.put(new Integer(i),new JLabel(
				String.valueOf((double)i / 1000.0)));
		}
		completeDelay.setLabelTable(labelTable);
		completeDelay.setPaintLabels(true);
		completeDelay.setMajorTickSpacing(250);
		completeDelay.setPaintTicks(true);

		completeDelay.setEnabled(completeDelayToggle.isSelected());
	} //}}}

	//{{{ _save() method
	protected void _save()
	{
		jEdit.setBooleanProperty("buffer.sidekick.buffer-change-parse",
			bufferChangeParse.isSelected());
		jEdit.setBooleanProperty("buffer.sidekick.keystroke-parse",
			keystrokeParse.isSelected());
		jEdit.setProperty("sidekick.auto-parse-delay",String.valueOf(
			autoParseDelay.getValue()));
		jEdit.setBooleanProperty("sidekick-tree.follows-caret",
			treeFollowsCaret.isSelected());
		jEdit.setBooleanProperty("options.sidekick.showToolTips", showToolTips.isSelected());
		jEdit.setBooleanProperty("options.sidekick.showStatusWindow", showStatusWindow.isSelected());
		int depth = 0;
		String value = (String)autoExpandTreeDepth.getSelectedItem();
		depth = value.equals("All") ? -1 : Integer.parseInt(value);
		jEdit.setIntegerProperty("sidekick-tree.auto-expand-tree-depth",
			depth);
		jEdit.setBooleanProperty("sidekick.complete-instant.toggle",
			completeInstantToggle.isSelected());
		jEdit.setBooleanProperty("sidekick.complete-delay.toggle",
			completeDelayToggle.isSelected());
		jEdit.setIntegerProperty("sidekick.complete-delay",
			completeDelay.getValue());
	} //}}}

	//{{{ Private members
	private JCheckBox bufferChangeParse;
	private JCheckBox keystrokeParse;
	private JSlider autoParseDelay;
	private JCheckBox treeFollowsCaret;
	private JComboBox autoExpandTreeDepth;
	private JCheckBox completeInstantToggle;
	private JCheckBox completeDelayToggle;
	private JSlider completeDelay;
	private JCheckBox showToolTips;
	private JCheckBox showStatusWindow;
	//}}}

	//{{{ ActionHandler class
	class ActionHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent evt)
		{
			Object source = evt.getSource();
			if(source == keystrokeParse)
			{
				autoParseDelay.setEnabled(keystrokeParse.isSelected());
				if(keystrokeParse.isSelected())
					bufferChangeParse.setSelected(true);
			}
			else if(source == completeDelayToggle)
			{
				completeDelay.setEnabled(completeDelayToggle.isSelected());
			}
		}
	} //}}}
}
