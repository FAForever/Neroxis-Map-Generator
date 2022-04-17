/**
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.util;

import java.awt.Font;
import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * @author Administrator
 */
public class LightweightLabel extends JLabel {

    private static final Logger log = Logger.getLogger(LightweightLabel.class.getName());
    @Serial
    private static final long serialVersionUID = -6771477489533614010L;

    protected static LightweightLabel sharedInstance;

    /**
     * Initializes the shared instance.
     */
    static {
        try {
            sharedInstance = new LightweightLabel();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to initialize the shared instance", e);
        }
    }

    public LightweightLabel() {
        setFont(new Font(Constants.DEFAULT_FONTFAMILY, 0, Constants.DEFAULT_FONTSIZE));
        setVerticalAlignment(SwingConstants.TOP);
    }

    public static LightweightLabel getSharedInstance() {
        return sharedInstance;
    }

    /**
     * Overridden for performance reasons.
     */
    public void validate() {
    }

    /**
     * Overridden for performance reasons.
     */
    public void revalidate() {
    }

    /**
     * Overridden for performance reasons.
     */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void repaint(RectangleDouble r) {
    }

    /**
     * Overridden for performance reasons.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        // Strings get interned...
        if ("text".equals(propertyName) || "font".equals(propertyName)) {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }

}
