/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.event;

import lombok.Getter;

/**
 * Base class for objects that dispatch named events.
 */
public abstract class EventObject {
    /**
     * Holds the consumed state of the event. Default is false.
     */
    @Getter
    protected boolean consumed = false;

    /**
     * Consumes the event.
     */
    public void consume() {
        consumed = true;
    }
}
