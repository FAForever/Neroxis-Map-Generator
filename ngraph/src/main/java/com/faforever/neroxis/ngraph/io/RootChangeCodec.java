/**
 * Copyright (c) 2006-2013, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.io;

import com.faforever.neroxis.ngraph.model.GraphModel.RootChange;
import java.util.Map;
import org.w3c.dom.Node;

/**
 * Codec for ChildChanges. This class is created and registered
 * dynamically at load time and used implicitly via Codec
 * and the CodecRegistry.
 */
public class RootChangeCodec extends ObjectCodec {

    /**
     * Constructs a new model codec.
     */
    public RootChangeCodec() {
        this(new RootChange(), new String[]{"model", "previous", "root"}, null, null);
    }

    /**
     * Constructs a new model codec for the given arguments.
     */
    public RootChangeCodec(Object template, String[] exclude, String[] idrefs, Map<String, String> mapping) {
        super(template, exclude, idrefs, mapping);
    }


    @Override
    public Node afterEncode(Codec enc, Object obj, Node node) {
        if (obj instanceof RootChange) {
            enc.encodeCell(((RootChange) obj).getRoot(), node, true);
        }

        return node;
    }

    /**
     * Reads the cells into the graph model. All cells are children of the root
     * element in the node.
     */
    public Node beforeDecode(Codec dec, Node node, Object into) {
        if (into instanceof RootChange) {
            RootChange change = (RootChange) into;

            if (node.getFirstChild() != null && node.getFirstChild().getNodeType() == Node.ELEMENT_NODE) {
                // Makes sure the original node isn't modified
                node = node.cloneNode(true);

                Node tmp = node.getFirstChild();
                change.setRoot(dec.decodeCell(tmp, false));

                Node tmp2 = tmp.getNextSibling();
                tmp.getParentNode().removeChild(tmp);
                tmp = tmp2;

                while (tmp != null) {
                    tmp2 = tmp.getNextSibling();

                    if (tmp.getNodeType() == Node.ELEMENT_NODE) {
                        dec.decodeCell(tmp, true);
                    }

                    tmp.getParentNode().removeChild(tmp);
                    tmp = tmp2;
                }
            }
        }

        return node;
    }


    @Override
    public Object afterDecode(Codec dec, Node node, Object obj) {
        if (obj instanceof RootChange) {
            RootChange change = (RootChange) obj;
            change.setPrevious(change.getRoot());
        }

        return obj;
    }

}
