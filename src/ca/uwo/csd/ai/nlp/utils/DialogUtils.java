package ca.uwo.csd.ai.nlp.utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

/**
 * Utilities
 *
 * @author Bernard Bou
 */
public class DialogUtils
{
	// C E N T E R

	/**
	 * Center on screen
	 *
	 * @param thisComponent
	 *            component to center
	 */
	static public void center(final Component thisComponent)
	{
		final Dimension thisScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
		final Dimension thisComponentSize = thisComponent.getSize();
		if (thisComponentSize.height > thisScreenSize.height)
		{
			thisComponentSize.height = thisScreenSize.height;
		}
		if (thisComponentSize.width > thisScreenSize.width)
		{
			thisComponentSize.width = thisScreenSize.width;
		}
		thisComponent.setLocation((thisScreenSize.width - thisComponentSize.width) / 2, (thisScreenSize.height - thisComponentSize.height) / 2);
	}

	/**
	 * Make file dialog
	 *
	 * @param thisTitle
	 *            dialog title
	 * @param thisExtension
	 *            file extension
	 * @param thisDescription
	 *            description of file type
	 * @return JFileChooser
	 */
	static public JFileChooser makeFileDialog(final String thisTitle, final String thisExtension, final String thisDescription)
	{
		final FileFilter thisFileFilter = new FileFilter()
		{
			private final String[] theExtensions = { thisExtension };

			@Override
			public boolean accept(final File thisFile)
			{
				for (final String thisExtension : this.theExtensions)
					if (thisFile.getName().toLowerCase().endsWith(thisExtension))
						return true;
				return thisFile.isDirectory();
			}

			@Override
			public String getDescription()
			{
				return thisDescription;
			}
		};
		JFileChooser thisFileChooser;
		thisFileChooser = new JFileChooser();
		thisFileChooser.setDialogTitle(thisTitle);
		thisFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		thisFileChooser.setFileFilter(thisFileFilter);
		thisFileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		return thisFileChooser;
	}
}
