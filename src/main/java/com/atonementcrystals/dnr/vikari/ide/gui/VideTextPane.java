package com.atonementcrystals.dnr.vikari.ide.gui;

import javax.swing.JTextPane;
import java.awt.Dimension;

public class VideTextPane extends JTextPane {

    private boolean wordWrap = false;

    public void toggleWordWrap() {
        this.wordWrap = !this.wordWrap;
    }

    public boolean getScrollableTracksViewportWidth() {
        if (wordWrap) {
            return super.getScrollableTracksViewportWidth();
        }

        // disable word wrap
        if (getSize().width < getParent().getSize().width) {
            return true;
        } else {
            return false;
        }
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
