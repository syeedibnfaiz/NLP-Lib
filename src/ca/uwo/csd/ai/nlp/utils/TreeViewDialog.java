package ca.uwo.csd.ai.nlp.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

//import edu.stanford.nlp.parser.ui.TreeJPanel;
import edu.stanford.nlp.trees.Tree;

/**
 * Parsed Tree View
 *
 * @author Bernard Bou
 */
public class TreeViewDialog extends JDialog
{
	private static final long serialVersionUID = 1L;

	/**
	 * Tree panel
	 */
	private TreeJPanel theTreePanel = null;

	/**
	 * Wrapping component
	 */
	private JComponent theComponent = null;

	/**
	 * Use scroll pane
	 */
	public boolean scroll = true;

	/**
	 * Constructor
	 *
	 * @param theOwner
	 *            owner frame
	 */
	public TreeViewDialog(final Frame theOwner)
	{
		super(theOwner);
		initialize();
	}

	/**
	 * Initialize
	 */
	private void initialize()
	{
		this.theTreePanel = new TreeJPanel(SwingConstants.LEFT, SwingConstants.TOP);
		this.theTreePanel.setBackground(Color.WHITE);
		this.theTreePanel.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
		this.theTreePanel.setMinFontSize(14);
		this.theTreePanel.setMaxFontSize(14);
		this.theComponent = this.scroll ? new JScrollPane(this.theTreePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED) : this.theTreePanel;
		setContentPane(this.theComponent);
		setTitle("Phrase structure");
	}

	/**
	 * Set theTree
	 *
	 * @param thisTree
	 *            parsed theTree
	 */
	public void setTree(final Tree thisTree)
	{
		final Font thisFont = this.theTreePanel.pickFont();
		final Dimension thisTreeDimension = this.theTreePanel.getTreeDimension(thisTree, thisFont);
		this.theComponent.setPreferredSize(thisTreeDimension);
		this.theTreePanel.setPreferredSize(thisTreeDimension);

		final Dimension thisDimension = new Dimension(thisTreeDimension.width + 20, thisTreeDimension.height + 50);

		// no larger than screen
		final Dimension thisScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (thisDimension.height > thisScreenSize.height)
			thisDimension.height = thisScreenSize.height;
		if (thisDimension.width > thisScreenSize.width)
			thisDimension.width = thisScreenSize.width;

		setSize(thisDimension);
		this.theTreePanel.setTree(thisTree);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.awt.Dialog#setVisible(boolean)
	 */
	@Override
	public void setVisible(final boolean show)
	{
		if (show)
		{
			DialogUtils.center(this);
		}
		super.setVisible(show);
	}
}
