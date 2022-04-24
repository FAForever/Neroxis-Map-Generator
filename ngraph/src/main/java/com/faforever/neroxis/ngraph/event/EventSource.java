/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for objects that dispatch named events.
 */
@Getter
@Setter
public class EventSource {

    /**
     * Holds the event names and associated listeners in an array. The array
     * contains the event name followed by the respective listener for each
     * registered listener.
     */
    protected final transient Map<Class<? extends EventObject>, List<IEventListener<?>>> eventListeners = new HashMap<>();
    /**
     * Holds the source object for this event source.
     */
    protected Object eventSource;
    /**
     * Specifies if events can be fired. Default is true.
     */
    protected boolean eventsEnabled = true;

    /**
     * Constructs a new event source using this as the source object.
     */
    public EventSource() {
        this(null);
    }

    /**
     * Constructs a new event source for the given source object.
     */
    public EventSource(Object source) {
        setEventSource(source);
    }

    /**
     * Binds the specified function to the given event name. If no event name
     * is given, then the listener is registered for all events.
     */
    public <T extends EventObject> void addListener(Class<T> eventClass, IEventListener<T> listener) {
        eventListeners.computeIfAbsent(eventClass, clazz -> new ArrayList<>()).add(listener);
    }

    /**
     * Function: removeListener
     * <p>
     * Removes all occurances of the given listener from the list of listeners.
     */
    public <T extends EventObject> void removeListener(IEventListener<T> listener) {
        eventListeners.values().forEach(listeners -> listeners.remove(listener));
    }

    /**
     * Dispatches the given event name with this object as the event source.
     * <code>fireEvent(new EventObject("eventName", key1, val1, .., keyN, valN))</code>
     */
    public void fireEvent(EventObject evt) {
        fireEvent(evt, null);
    }

    /**
     * Dispatches the given event name, passing all arguments after the given
     * name to the registered listeners for the event.
     */
    public <T extends EventObject> void fireEvent(T event, Object sender) {
        if (!eventListeners.isEmpty() && isEventsEnabled()) {
            if (sender == null) {
                sender = getEventSource();
            }
            if (sender == null) {
                sender = this;
            }
            Object finalSender = sender;
            eventListeners.getOrDefault(event.getClass(), List.of())
                          .forEach(listener -> ((IEventListener<T>) listener).invoke(finalSender, event));
        }
    }

    /**
     * Defines the requirements for an object that listens to an event source.
     */
    public interface IEventListener<T extends EventObject> {

        /**
         * Called when the graph model has changed.
         *
         * @param sender Reference to the source of the event.
         * @param event  Event object to be dispatched.
         */
        void invoke(Object sender, T event);
    }
}
