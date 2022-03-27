/**
 * Copyright (c) 2006, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.io;

import com.faforever.neroxis.ngraph.model.GraphModel.TerminalChange;
import java.util.Map;
import org.w3c.dom.Node;

/**
 * Codec for ChildChanges. This class is created and registered
 * dynamically at load time and used implicitely via Codec
 * and the CodecRegistry.
 */
public class TerminalChangeCodec extends ObjectCodec {

    /**
     * Constructs a new model codec.
     */
    public TerminalChangeCodec() {
        this(new TerminalChange(), new String[]{"model", "previous"}, new String[]{"cell", "terminal"}, null);
    }

    /**
     * Constructs a new model codec for the given arguments.
     */
    public TerminalChangeCodec(Object template, String[] exclude, String[] idrefs, Map<String, String> mapping) {
        super(template, exclude, idrefs, mapping);
    }


    @Override
    public Object afterDecode(Codec dec, Node node, Object obj) {
        if (obj instanceof TerminalChange) {
            TerminalChange change = (TerminalChange) obj;

            change.setPrevious(change.getTerminal());
        }

        return obj;
    }

}
