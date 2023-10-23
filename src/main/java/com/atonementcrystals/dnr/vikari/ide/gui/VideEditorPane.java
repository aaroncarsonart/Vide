package com.atonementcrystals.dnr.vikari.ide.gui;

import javax.swing.JTextPane;
import java.awt.Dimension;

/**
 * A custom JTextPane with modified behavior to enable and disable word wrap functionality.
 */
public class VideEditorPane extends JTextPane {

    private boolean wordWrap = false;

    public void toggleWordWrap() {
        this.wordWrap = !this.wordWrap;
    }

    public boolean getScrollableTracksViewportWidth() {
        if (wordWrap) {
            return super.getScrollableTracksViewportWidth();
        }

        // disable word wrap
        return getSize().width < getParent().getSize().width;
    }

    public void setSize(Dimension dimension) {
        if (wordWrap) {
            super.setSize(dimension);
            return;
        }

        // disable word wrap
        if (dimension.width < getParent().getSize().width) {
            dimension.width = getParent().getSize().width;
        }
        super.setSize(dimension);
    }
}
