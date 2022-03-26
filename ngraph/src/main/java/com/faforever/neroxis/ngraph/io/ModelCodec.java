/**
 * Copyright (c) 2006-2013, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.io;

import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.ICell;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;

/**
 * Codec for GraphModels. This class is created and registered
 * dynamically at load time and used implicitly via Codec
 * and the CodecRegistry.
 */
public class ModelCodec extends ObjectCodec {

    /**
     * Constructs a new model codec.
     */
    public ModelCodec() {
        this(new GraphModel());
    }

    /**
     * Constructs a new model codec for the given template.
     */
    public ModelCodec(Object template) {
        this(template, null, null, null);
    }

    /**
     * Constructs a new model codec for the given arguments.
     */
    public ModelCodec(Object template, String[] exclude, String[] idrefs, Map<String, String> mapping) {
        super(template, exclude, idrefs, mapping);
    }

    /**
     * Encodes the given GraphModel by writing a (flat) XML sequence
     * of cell nodes as produced by the CellCodec. The sequence is
     * wrapped-up in a node with the name root.
     */
    protected void encodeObject(Codec enc, Object obj, Node node) {
        if (obj instanceof GraphModel) {
            Node rootNode = enc.document.createElement("root");
            GraphModel model = (GraphModel) obj;
            enc.encodeCell(model.getRoot(), rootNode, true);
            node.appendChild(rootNode);
        }
    }

    /**
     * Reads the cells into the graph model. All cells are children of the root
     * element in the node.
     */
    public Node beforeDecode(Codec dec, Node node, Object into) {
        if (node instanceof Element) {
            Element elt = (Element) node;
            GraphModel model = null;

            if (into instanceof GraphModel) {
                model = (GraphModel) into;
            } else {
                model = new GraphModel();
            }

            // Reads the cells into the graph model. All cells
            // are children of the root element in the node.
            Node root = elt.getElementsByTagName("root").item(0);
            ICell rootCell = null;

            if (root != null) {
                Node tmp = root.getFirstChild();

                while (tmp != null) {
                    ICell cell = dec.decodeCell(tmp, true);

                    if (cell != null && cell.getParent() == null) {
                        rootCell = cell;
                    }

                    tmp = tmp.getNextSibling();
                }

                root.getParentNode().removeChild(root);
            }

            // Sets the root on the model if one has been decoded
            if (rootCell != null) {
                model.setRoot(rootCell);
            }
        }

        return node;
    }

}
