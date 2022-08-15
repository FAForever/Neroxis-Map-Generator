/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ui.transfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.plaf.UIResource;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GraphMethodTransferable implements Transferable, UIResource, Serializable {
    @Serial
    private static final long serialVersionUID = 5123819419918087664L;
    private static final Logger log = Logger.getLogger(GraphMethodTransferable.class.getName());
    /**
     * Serialized Data Flavor. Use the following code to switch to local
     * reference flavor:
     * <code>
     * try
     * {
     * GraphTransferable.dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
     * + "; class=com.faforever.neroxis.ui.transfer.GraphMethodTransferable");
     * }
     * catch (ClassNotFoundException cnfe)
     * {
     * // do nothing
     * }
     * </code>
     * <p>
     * If you get a class not found exception, try the following instead:
     * <code>
     * GraphTransferable.dataFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType
     * + "; class=com.faforever.neroxis.ui.transfer.GraphMethodTransferable", null,
     * new com.faforever.neroxis.ui.transfer.GraphMethodTransferable(null, null).getClass().getClassLoader());
     * </code>
     */
    public static DataFlavor dataFlavor;

    /**
     * Local Machine Reference Data Flavor.
     */
    static {
        try {
            dataFlavor = new DataFlavor(DataFlavor.javaSerializedObjectMimeType
                                        + "; class=com.faforever.neroxis.ui.transfer.GraphMethodTransferable");
        } catch (ClassNotFoundException e) {
            log.log(Level.SEVERE, "Error initializing dataFlavor", e);
        }
    }

    private final String executingClassName;
    private final String methodClassName;
    private final String methodName;
    private final List<String> paramClassNames;

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{dataFlavor};
    }

    /**
     * Returns whether or not the specified data flavor is supported for this
     * object.
     *
     * @param flavor the requested flavor for the data
     * @return boolean indicating whether or not the data flavor is supported
     */
    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Arrays.stream(getTransferDataFlavors()).anyMatch(flav -> flav != null && flav.equals(flavor));
    }

    /**
     * Returns an object which represents the data to be transferred. The class
     * of the object returned is defined by the representation class of the
     * flavor.
     *
     * @param flavor the requested flavor for the data
     * @throws IOException                if the data is no longer available in the requested
     *                                    flavor.
     * @throws UnsupportedFlavorException if the requested data flavor is not supported.
     * @see DataFlavor#getRepresentationClass
     */
    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(dataFlavor)) {
            return this;
        }

        throw new UnsupportedFlavorException(flavor);
    }
}
