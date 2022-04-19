package com.faforever.neroxis.ngraph.canvas;

import com.faforever.neroxis.ngraph.style.util.Direction;
import com.faforever.neroxis.ngraph.util.Constants;
import java.awt.Color;

/**
 * Requirements for implementing technologies:
 * <p>
 * - Path rendering (move, line, quad, curve, arc)
 * - Images, flip v/h, aspect, alpha (PNG, JPG, GIF)
 * - Linear gradients (in all four directions)
 * - Transparency, fill and stroke
 * - Rotation, flip v/h
 * - Font rendering
 * - Dash patterns
 * - Clipping by path (not just rectangle)
 * - Alpha gradients (for glass effect)
 * - Encode result as image (PNG, JPG)
 */
public interface ICanvas2D {
    /**
     * Saves the current state of the canvas.
     */
    void save();

    /**
     * Restores the previous state of the canvas.
     */
    void restore();

    /**
     * Uniformaly scales the canvas by the given amount.
     *
     * @param value The new scale value.
     */
    void scale(double value);

    /**
     * Translates the canvas by the given amount.
     *
     * @param dx X-coordinate of the translation.
     * @param dy Y-coordinate of the translation.
     */
    void translate(double dx, double dy);

    /**
     * Rotates the canvas by the given angle around the given center. This
     * method may add rendering overhead and should be used with care.
     *
     * @param theta Rotation angle in degrees (0 - 360).
     * @param flipH Specifies if drawing should be flipped horizontally.
     * @param flipV Specifies if drawing should be flipped vertically.
     * @param cx    X-coordinate of the center point.
     * @param cy    Y-coordinate of the center point.
     */
    void rotate(double theta, boolean flipH, boolean flipV, double cx, double cy);

    /**
     * Sets the stroke width. This should default to 1 if unset.
     *
     * @param value Width of the stroke. The value should be multiplied by the
     *              current scale.
     */
    void setStrokeWidth(double value);

    /**
     * Sets the stroke color. This should default to {@link Constants#NONE}
     * if unset.
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setStrokeColor(Color value);

    /**
     * Sets the dashed state. This should default to false if unset.
     *
     * @param value Boolean representing the dashed state.
     */
    void setDashed(boolean value);

    /**
     * Sets the dashed state. This should default to false if unset.
     *
     * @param value Boolean representing the dashed state.
     */
    void setDashed(boolean value, boolean fixDash);

    /**
     * Sets the dash pattern. This should default to "3 3" if unset.
     *
     * @param value Space separated list of floats representing the dash
     *              pattern. The value should be multiplied by the current scale.
     */
    void setDashPattern(String value);

    /**
     * Sets the linecap. This should default to "flat" if unset.
     *
     * @param value "flat", "square" or "round".
     */
    void setLineCap(String value);

    /**
     * Sets the linejoin. This should default to "miter" if unset.
     *
     * @param value "miter", "round" or "bevel".
     */
    void setLineJoin(String value);

    /**
     * Sets the miterlimit. This should default to 10 if unset.
     *
     */
    void setMiterLimit(double value);

    /**
     * Default value {@link Constants#DEFAULT_FONTSIZE}.
     *
     */
    void setFontSize(double value);

    /**
     * Default value "#000000".
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setFontColor(Color value);

    /**
     * Default value {@link Constants#DEFAULT_FONTFAMILY}.
     *
     */
    void setFontFamily(String value);

    /**
     * Default value 0. See {@link Constants#STYLE_FONTSTYLE}.
     *
     */
    void setFontStyle(int value);

    /**
     * Default value "#000000".
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setFontBackgroundColor(Color value);

    /**
     * Default value "#000000".
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setFontBorderColor(Color value);

    /**
     * Default value 1. This method may add rendering overhead and should be
     * used with care.
     *
     */
    void setAlpha(double value);

    /**
     * Default value {@link Constants#NONE}.
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setFillColor(Color value);

    /**
     * Prepares the canvas to draw a gradient.
     *
     * @param direction Direction may be null. Use default value
     *                  {@link Constants#DIRECTION_SOUTH}.
     */
    void setGradient(Color color1, Color color2, double x, double y, double w, double h, Direction direction, double alpha1, double alpha2);

    /**
     * Enables or disables the painting of shadows.
     *
     * @param enabled Whether the shadow should be enabled.
     */
    void setShadow(boolean enabled);

    /**
     * Default value {@link Constants#NONE}.
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setShadowColor(Color value);

    /**
     * Default value {@link Constants#NONE}.
     *
     * @param value Hex representation of the color or {@link Constants#NONE}.
     */
    void setShadowAlpha(double value);

    void setShadowOffset(double dx, double dy);

    /**
     * Next fill or stroke should draw a rectangle.
     *
     */
    void rect(double x, double y, double w, double h);

    /**
     * Next fill or stroke should draw a round rectangle.
     *
     */
    void roundrect(double x, double y, double w, double h, double dx, double dy);

    /**
     * Next fill or stroke should draw an ellipse.
     *
     */
    void ellipse(double x, double y, double w, double h);

    /**
     * Draws the given image.
     *
     */
    void image(double x, double y, double w, double h, String src, boolean aspect, boolean flipH, boolean flipV);

    /**
     * Draws the given string. Possible values for format are empty string for
     * plain text and html for HTML markup.
     *
     */
    void text(double x, double y, double w, double h, String str, String align, String valign, boolean wrap, String format, String overflow, boolean clip, double rotation, String dir);

    /**
     * Begins a new path.
     */
    void begin();

    /**
     * Moves to the given path.
     *
     */
    void moveTo(double x, double y);

    /**
     * Draws a line to the given path.
     *
     */
    void lineTo(double x, double y);

    /**
     * Draws a quadratic curve to the given point.
     *
     */
    void quadTo(double x1, double y1, double x2, double y2);

    /**
     * Draws a bezier curve to the given point.
     *
     */
    void curveTo(double x1, double y1, double x2, double y2, double x3, double y3);

    /**
     * Closes the current path.
     */
    void close();

    /**
     * Paints the outline of the current path.
     */
    void stroke();

    /**
     * Fills the current path.
     */
    void fill();

    /**
     * Fills and paints the outline of the current path.
     */
    void fillAndStroke();

}
