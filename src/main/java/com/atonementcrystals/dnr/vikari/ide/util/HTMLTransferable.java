package com.atonementcrystals.dnr.vikari.ide.util;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

// TODO: Create short, succinct example in separate project demonstrating the additional spaces error.
public class HTMLTransferable implements Transferable {
    private String hmtlFormattedText;
    private DataFlavor[] dataFlavors;

    public HTMLTransferable(String hmtlFormattedText) {
        this.hmtlFormattedText = hmtlFormattedText;
        this.dataFlavors = new DataFlavor[] {
                DataFlavor.allHtmlFlavor
        };
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor supportedFlavor : dataFlavors) {
            if (supportedFlavor.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == DataFlavor.allHtmlFlavor) {
            return hmtlFormattedText;
        }
        return null;
    }
}
