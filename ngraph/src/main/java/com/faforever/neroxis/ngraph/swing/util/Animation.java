/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.util;

import com.faforever.neroxis.ngraph.event.DoneEvent;
import com.faforever.neroxis.ngraph.event.EventSource;
import com.faforever.neroxis.ngraph.event.ExecuteEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * Baseclass for all timer-based animations. Fires Event.DONE when the
 * stopAnimation method is called. Implement updateAnimation for the
 * actual animation or listen to Event.EXECUTE.
 */
public class Animation extends EventSource {
    /**
     * Specifies the default delay for animations in ms. Default is 20.
     */
    public static int DEFAULT_DELAY = 20;

    /**
     * Default is DEFAULT_DELAY.
     */
    protected int delay;

    /**
     * Time instance that is used for timing the animation.
     */
    protected Timer timer;

    /**
     * Constructs a new animation instance with the given repaint delay.
     */
    public Animation() {
        this(DEFAULT_DELAY);
    }

    /**
     * Constructs a new animation instance with the given repaint delay.
     */
    public Animation(int delay) {
        this.delay = delay;
    }

    /**
     * Returns the delay for the animation.
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the delay for the animation.
     */
    public void setDelay(int value) {
        delay = value;
    }

    /**
     * Returns true if the animation is running.
     */
    public boolean isRunning() {
        return timer != null;
    }

    /**
     * Starts the animation by repeatedly invoking updateAnimation.
     */
    public void startAnimation() {
        if (timer == null) {
            timer = new Timer(delay, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateAnimation();
                }

            });

            timer.start();
        }
    }

    /**
     * Hook for subclassers to implement the animation. Invoke stopAnimation
     * when finished, startAnimation to resume. This is called whenever the
     * timer fires and fires an Event.EXECUTE event with no properties.
     */
    public void updateAnimation() {
        fireEvent(new ExecuteEvent(null));
    }

    /**
     * Stops the animation by deleting the timer and fires Event.DONE.
     */
    public void stopAnimation() {
        if (timer != null) {
            timer.stop();
            timer = null;
            fireEvent(new DoneEvent());
        }
    }

}
