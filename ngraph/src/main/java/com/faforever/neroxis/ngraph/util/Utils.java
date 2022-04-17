/**
 * Copyright (c) 2007-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.util;

import com.faforever.neroxis.ngraph.model.CellPath;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.shape.SwimlaneShape;
import com.faforever.neroxis.ngraph.style.Direction;
import com.faforever.neroxis.ngraph.style.FontModifier;
import com.faforever.neroxis.ngraph.style.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.style.SwimlaneStyle;
import com.faforever.neroxis.ngraph.style.VerticalAlignment;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.w3c.dom.Element;

/**
 * Contains various helper methods for use with Graph.
 */
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    /**
     * True if the machine is a Mac.
     */
    public static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");

    /**
     * True if the machine is running a linux kernel.
     */
    public static boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("linux");

    /**
     * Static Graphics used for Font Metrics.
     */
    protected static transient Graphics fontGraphics;

    // Creates a renderer for HTML markup (only possible in
    // non-headless environment)
    static {
        try {
            fontGraphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getGraphics();
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to initialize font graphics", e);
        }
    }

    /**
     * Returns the size for the given label.
     */
    public static RectangleDouble getLabelSize(String label, Style style, double scale) {
        return getSizeForString(label, getFont(style), scale);
    }

    /**
     * Returns the body part of the given HTML markup.
     */
    public static String getBodyMarkup(String markup, boolean replaceLinefeeds) {
        String lowerCase = markup.toLowerCase();
        int bodyStart = lowerCase.indexOf("<body>");

        if (bodyStart >= 0) {
            bodyStart += 7;
            int bodyEnd = lowerCase.lastIndexOf("</body>");

            if (bodyEnd > bodyStart) {
                markup = markup.substring(bodyStart, bodyEnd).trim();
            }
        }

        if (replaceLinefeeds) {
            markup = markup.replaceAll("\n", "<br>");
        }

        return markup;
    }

    /**
     * Returns the paint bounds for the given label.
     */
    public static RectangleDouble getLabelPaintBounds(String label, Style style, PointDouble offset, RectangleDouble vertexBounds, double scale) {
        return getLabelPaintBounds(label, style, offset, vertexBounds, scale, false);
    }

    /**
     * Returns the paint bounds for the given label.
     */
    public static RectangleDouble getLabelPaintBounds(String label, Style style, PointDouble offset, RectangleDouble vertexBounds, double scale, boolean isEdge) {
        RectangleDouble size = Utils.getLabelSize(label, style, scale);
        // Measures font with full scale and scales back
        size.setWidth(size.getWidth() / scale);
        size.setHeight(size.getHeight() / scale);
        double x = offset.getX();
        double y = offset.getY();
        double width = 0;
        double height = 0;
        if (vertexBounds != null) {
            x += vertexBounds.getX();
            y += vertexBounds.getY();
            if (style.getShape().getShape() instanceof SwimlaneStyle) {
                // Limits the label to the swimlane title
                boolean horizontal = style.getCellProperties().isHorizontal();
                double start = style.getEdge().getStartSize() * scale;
                if (horizontal) {
                    width += vertexBounds.getWidth();
                    height += start;
                } else {
                    width += start;
                    height += vertexBounds.getHeight();
                }
            } else {
                width += (isEdge) ? 0 : vertexBounds.getWidth();
                height += vertexBounds.getHeight();
            }
        }

        return Utils.getScaledLabelBounds(x, y, size, width, height, style, scale);
    }

    /**
     * Returns the bounds for a label for the given location and size, taking
     * into account the alignment and spacing in the specified style, as well as
     * the width and height of the rectangle that contains the label. (For edge
     * labels this width and height is 0.) The scale is used to scale the given
     * size and the spacings in the specified style.
     */
    public static RectangleDouble getScaledLabelBounds(double x, double y, RectangleDouble size, double outerWidth, double outerHeight, Style style, double scale) {
        double inset = Constants.LABEL_INSET * scale;
        // Scales the size of the label
        // FIXME: Correct rounded font size and not-rounded scale
        double width = size.getWidth() * scale + 2 * inset;
        double height = size.getHeight() * scale + 2 * inset;
        // Gets the global spacing and orientation
        boolean horizontal = style.getCellProperties().isHorizontal();
        // Gets the alignment settings
        HorizontalAlignment align = style.getLabel().getHorizontalAlignment();
        VerticalAlignment valign = style.getLabel().getVerticalAlignment();
        // Gets the vertical spacing
        int top = (int) (style.getLabel().getTopSpacing() * scale);
        int bottom = (int) (style.getLabel().getBottomSpacing() * scale);
        // Gets the horizontal spacing
        int left = (int) (style.getLabel().getLeftSpacing() * scale);
        int right = (int) (style.getLabel().getRightSpacing() * scale);
        // Applies the orientation to the spacings and dimension
        if (!horizontal) {
            int tmp = top;
            top = right;
            right = bottom;
            bottom = left;
            left = tmp;
            double tmp2 = width;
            width = height;
            height = tmp2;
        }

        // Computes the position of the label for the horizontal alignment
        if ((horizontal && align == HorizontalAlignment.CENTER) || (!horizontal && valign == VerticalAlignment.MIDDLE)) {
            x += (outerWidth - width) / 2 + left - right;
        } else if ((horizontal && align == HorizontalAlignment.RIGHT) || (!horizontal && valign == VerticalAlignment.BOTTOM)) {
            x += outerWidth - width - right;
        } else {
            x += left;
        }

        // Computes the position of the label for the vertical alignment
        if ((!horizontal && align == HorizontalAlignment.CENTER) || (horizontal && valign == VerticalAlignment.MIDDLE)) {
            y += (outerHeight - height) / 2 + top - bottom;
        } else if ((!horizontal && align == HorizontalAlignment.LEFT) || (horizontal && valign == VerticalAlignment.BOTTOM)) {
            y += outerHeight - height - bottom;
        } else {
            y += top;
        }
        return new RectangleDouble(x, y, width, height);
    }

    /**
     * Returns the font metrics of the static font graphics instance
     *
     * @param font The font whose metrics are to be returned
     * @return the font metrics of the specified font
     */
    public static FontMetrics getFontMetrics(Font font) {
        if (fontGraphics != null) {
            return fontGraphics.getFontMetrics(font);
        }

        return null;
    }

    /**
     * Returns an <Rectangle> with the size (width and height in pixels) of
     * the given string.
     *
     * @param text String whose size should be returned.
     * @param font Font to be used for the computation.
     */
    public static RectangleDouble getSizeForString(String text, Font font, double scale) {
        FontRenderContext frc = new FontRenderContext(null, false, false);
        font = font.deriveFont((float) (font.getSize2D() * scale));
        FontMetrics metrics = null;
        if (fontGraphics != null) {
            metrics = fontGraphics.getFontMetrics(font);
        }
        double lineHeight = Constants.LINESPACING;

        if (metrics != null) {
            lineHeight += metrics.getHeight();
        } else {
            lineHeight += font.getSize2D() * 1.27;
        }

        String[] lines = text.split("\n");

        Rectangle2D boundingBox = null;

        if (lines.length == 0) {
            boundingBox = font.getStringBounds("", frc);
        } else {
            for (String line : lines) {
                Rectangle2D bounds = font.getStringBounds(line, frc);

                if (boundingBox == null) {
                    boundingBox = bounds;
                } else {
                    boundingBox.setFrame(0, 0, Math.max(boundingBox.getWidth(), bounds.getWidth()), boundingBox.getHeight() + lineHeight);
                }
            }
        }
        return new RectangleDouble(boundingBox);
    }

    /**
     * Returns the specified text in lines that fit within the specified
     * width when the specified font metrics are applied to the text
     *
     * @param text    the text to wrap
     * @param metrics the font metrics to calculate the text size for
     * @param width   the width that the text must fit within
     * @return the input text split in lines that fit the specified width
     */
    public static String[] wordWrap(String text, FontMetrics metrics, double width) {
        List<String> result = new ArrayList<String>();
        // First split the processing into lines already delimited by
        // newlines. We want the result to retain all newlines in position.
        String[] lines = text.split("\n");

        for (String line : lines) {
            int lineWidth = 0; // the display width of the current line
            int charCount = 0; // keeps count of current position in the line
            StringBuilder currentLine = new StringBuilder();

            // Split the words of the current line by spaces and tabs
            // The words are trimmed of tabs, space and newlines, therefore
            String[] words = line.split("\\s+");

            // Need to a form a stack of the words in reverse order
            // This is because if a word is split during the process
            // the remainder of the word is added to the front of the
            // stack and processed next
            Stack<String> wordStack = new Stack<String>();

            for (int j = words.length - 1; j >= 0; j--) {
                wordStack.push(words[j]);
            }

            while (!wordStack.isEmpty()) {
                String word = wordStack.pop();

                // Work out what whitespace exists before this word.
                // and add the width of the whitespace to the calculation
                int whitespaceCount = 0;

                if (word.length() > 0) {
                    // Concatenate any preceding whitespace to the
                    // word and calculate the number of characters of that
                    // whitespace
                    char firstWordLetter = word.charAt(0);
                    int letterIndex = line.indexOf(firstWordLetter, charCount);
                    String whitespace = line.substring(charCount, letterIndex);
                    whitespaceCount = whitespace.length();
                    word = whitespace.concat(word);
                }

                double wordLength;

                // If the line width is zero, we are at the start of a newline
                // We don't proceed preceeding whitespace in the width
                // calculation
                if (lineWidth > 0) {
                    wordLength = metrics.stringWidth(word);
                } else {
                    wordLength = metrics.stringWidth(word.trim());
                }

                // Does the width of line so far plus the width of the
                // current word exceed the allowed width?
                if (lineWidth + wordLength > width) {
                    if (lineWidth > 0) {
                        // There is already at least one word on this line
                        // and the current word takes the overall width over
                        // the allowed width. Because there is something on
                        // the line, complete the current line, reset the width
                        // counter, create a new line and put the current word
                        // back on the stack for processing in the next round
                        result.add(currentLine.toString());
                        currentLine = new StringBuilder();
                        wordStack.push(word.trim());
                        lineWidth = 0;
                    } else if (Constants.SPLIT_WORDS) {
                        // There are no words on the current line and the
                        // current word does not fit on it. Find the maximum
                        // number of characters of this word that just fit
                        // in the available width
                        word = word.trim();

                        for (int j = 1; j <= word.length(); j++) {
                            wordLength = metrics.stringWidth(word.substring(0, j));

                            if (lineWidth + wordLength > width) {
                                // The last character took us over the allowed
                                // width, deducted it unless there is only one
                                // character, in which case we have to use it
                                // since we can't split it...
                                j = j > 1 ? j - 1 : j;
                                String chars = word.substring(0, j);
                                currentLine = currentLine.append(chars);
                                // Return the unprocessed part of the word
                                // to the stack
                                wordStack.push(word.substring(j));
                                result.add(currentLine.toString());
                                currentLine = new StringBuilder();
                                lineWidth = 0;
                                // Increment char counter allowing for white
                                // space in the original word
                                charCount = charCount + chars.length() + whitespaceCount;
                                break;
                            }
                        }
                    } else {
                        // There are no words on the current line, but
                        // we are not splitting.
                        word = word.trim();
                        result.add(word);
                        currentLine = new StringBuilder();
                        lineWidth = 0;
                        // Increment char counter allowing for white
                        // space in the original word
                        charCount = word.length() + whitespaceCount;
                    }
                } else {
                    // The current word does not take the total line width
                    // over the allowed width. Append the word, removing
                    // preceeding whitespace if it is the first word in the
                    // line.
                    if (lineWidth > 0) {
                        currentLine.append(word);
                    } else {
                        currentLine.append(word.trim());
                    }

                    lineWidth += wordLength;
                    charCount += word.length();
                }
            }

            result.add(currentLine.toString());
        }

        return result.toArray(new String[0]);
    }

    /**
     * Function: arcToCurves
     * <p>
     * Converts the given arc to a series of curves.
     */
    public static double[] arcToCurves(double x0, double y0, double r1, double r2, double angle, double largeArcFlag, double sweepFlag, double x, double y) {
        x -= x0;
        y -= y0;

        if (r1 == 0 || r2 == 0) {
            return new double[0];
        }

        r1 = Math.abs(r1);
        r2 = Math.abs(r2);
        double ctx = -x / 2;
        double cty = -y / 2;
        double cpsi = Math.cos(angle * Math.PI / 180);
        double spsi = Math.sin(angle * Math.PI / 180);
        double rxd = cpsi * ctx + spsi * cty;
        double ryd = -1 * spsi * ctx + cpsi * cty;
        double rxdd = rxd * rxd;
        double rydd = ryd * ryd;
        double r1x = r1 * r1;
        double r2y = r2 * r2;
        double lamda = rxdd / r1x + rydd / r2y;
        double sds;

        if (lamda > 1) {
            r1 = Math.sqrt(lamda) * r1;
            r2 = Math.sqrt(lamda) * r2;
            sds = 0;
        } else {
            double seif = 1;

            if (largeArcFlag == sweepFlag) {
                seif = -1;
            }

            sds = seif * Math.sqrt((r1x * r2y - r1x * rydd - r2y * rxdd) / (r1x * rydd + r2y * rxdd));
        }

        double txd = sds * r1 * ryd / r2;
        double tyd = -1 * sds * r2 * rxd / r1;
        double tx = cpsi * txd - spsi * tyd + x / 2;
        double ty = spsi * txd + cpsi * tyd + y / 2;
        double rad = Math.atan2((ryd - tyd) / r2, (rxd - txd) / r1) - Math.atan2(0, 1);
        double s1 = (rad >= 0) ? rad : 2 * Math.PI + rad;
        rad = Math.atan2((-ryd - tyd) / r2, (-rxd - txd) / r1) - Math.atan2((ryd - tyd) / r2, (rxd - txd) / r1);
        double dr = (rad >= 0) ? rad : 2 * Math.PI + rad;

        if (sweepFlag == 0 && dr > 0) {
            dr -= 2 * Math.PI;
        } else if (sweepFlag != 0 && dr < 0) {
            dr += 2 * Math.PI;
        }

        double sse = dr * 2 / Math.PI;
        int seg = (int) Math.ceil(sse < 0 ? -1 * sse : sse);
        double segr = dr / seg;
        double t = 8 / 3 * Math.sin(segr / 4) * Math.sin(segr / 4) / Math.sin(segr / 2);
        double cpsir1 = cpsi * r1;
        double cpsir2 = cpsi * r2;
        double spsir1 = spsi * r1;
        double spsir2 = spsi * r2;
        double mc = Math.cos(s1);
        double ms = Math.sin(s1);
        double x2 = -t * (cpsir1 * ms + spsir2 * mc);
        double y2 = -t * (spsir1 * ms - cpsir2 * mc);
        double x3 = 0;
        double y3 = 0;

        double[] result = new double[seg * 6];

        for (int n = 0; n < seg; ++n) {
            s1 += segr;
            mc = Math.cos(s1);
            ms = Math.sin(s1);

            x3 = cpsir1 * mc - spsir2 * ms + tx;
            y3 = spsir1 * mc + cpsir2 * ms + ty;
            double dx = -t * (cpsir1 * ms + spsir2 * mc);
            double dy = -t * (spsir1 * ms - cpsir2 * mc);

            // CurveTo updates x0, y0 so need to restore it
            int index = n * 6;
            result[index] = x2 + x0;
            result[index + 1] = y2 + y0;
            result[index + 2] = x3 - dx + x0;
            result[index + 3] = y3 - dy + y0;
            result[index + 4] = x3 + x0;
            result[index + 5] = y3 + y0;

            x2 = x3 + dx;
            y2 = y3 + dy;
        }

        return result;
    }

    /**
     * Returns the bounding box for the rotated rectangle.
     */
    public static RectangleDouble getBoundingBox(RectangleDouble rect, double rotation) {
        RectangleDouble result = null;
        if (rect != null && rotation != 0) {
            double rad = Math.toRadians(rotation);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            PointDouble cx = new PointDouble(rect.getX() + rect.getWidth() / 2, rect.getY() + rect.getHeight() / 2);
            PointDouble p1 = new PointDouble(rect.getX(), rect.getY());
            PointDouble p2 = new PointDouble(rect.getX() + rect.getWidth(), rect.getY());
            PointDouble p3 = new PointDouble(p2.getX(), rect.getY() + rect.getHeight());
            PointDouble p4 = new PointDouble(rect.getX(), p3.getY());
            p1 = getRotatedPoint(p1, cos, sin, cx);
            p2 = getRotatedPoint(p2, cos, sin, cx);
            p3 = getRotatedPoint(p3, cos, sin, cx);
            p4 = getRotatedPoint(p4, cos, sin, cx);
            java.awt.Rectangle tmp = new java.awt.Rectangle((int) p1.getX(), (int) p1.getY(), 0, 0);
            tmp.add(p2.toPoint());
            tmp.add(p3.toPoint());
            tmp.add(p4.toPoint());
            result = new RectangleDouble(tmp);
        } else if (rect != null) {
            result = rect.clone();
        }

        return result;
    }

    /**
     * Find the first character matching the input character in the given
     * string where the character has no letter preceding it.
     *
     * @param text      the string to test for the presence of the input character
     * @param inputChar the test character
     * @param fromIndex the index position of the string to start from
     * @return the position of the first character matching the input character
     * in the given string where the character has no letter preceding it.
     */
    public static int firstCharAt(String text, int inputChar, int fromIndex) {
        int result = 0;

        while (result >= 0) {
            result = text.indexOf(inputChar, fromIndex);

            if (result == 0) {
                return result;
            } else if (result > 0) {
                // Check there is a whitespace or symbol before the hit character
                if (Character.isLetter(text.codePointAt(result - 1))) {
                    // The pre-increment is used in if and else branches.
                    if (++fromIndex >= text.length()) {
                        return -1;
                    } else {
                        // Test again from next candidate character
                        // This isn't the first letter of this word
                        result = text.indexOf(inputChar, fromIndex);
                    }
                } else {
                    return result;
                }
            }

        }

        return result;
    }

    /**
     * Rotates the given point by the given cos and sin.
     */
    public static PointDouble getRotatedPoint(PointDouble pt, double cos, double sin) {
        return getRotatedPoint(pt, cos, sin, new PointDouble());
    }

    /**
     * Finds the index of the nearest segment on the given cell state for the
     * specified coordinate pair.
     */
    public static int findNearestSegment(CellState state, double x, double y) {
        int index = -1;

        if (state.getAbsolutePointCount() > 0) {
            PointDouble last = state.getAbsolutePoint(0);
            double min = Double.MAX_VALUE;

            for (int i = 1; i < state.getAbsolutePointCount(); i++) {
                PointDouble current = state.getAbsolutePoint(i);
                double dist = new Line2D.Double(last.x, last.y, current.x, current.y).ptSegDistSq(x, y);

                if (dist < min) {
                    min = dist;
                    index = i - 1;
                }

                last = current;
            }
        }

        return index;
    }

    /**
     * Rotates the given point by the given cos and sin.
     */
    public static PointDouble getRotatedPoint(PointDouble pt, double cos, double sin, PointDouble c) {
        double x = pt.getX() - c.getX();
        double y = pt.getY() - c.getY();
        double x1 = x * cos - y * sin;
        double y1 = y * cos + x * sin;
        return new PointDouble(x1 + c.getX(), y1 + c.getY());
    }

    /**
     * Returns an integer mask of the port constraints of the given map
     *
     * @param terminal the cached cell state of the cell to determine the
     *                 port constraints for
     * @param edge     the edge connected to the constrained terminal
     * @param source   whether or not the edge specified is connected to the
     *                 terminal specified at its source end
     * @return the mask of port constraint directions
     */
    public static int getPortConstraints(CellState terminal, CellState edge, boolean source) {
        return getPortConstraints(terminal, edge, source, Set.of(Direction.values()));
    }

    /**
     * Returns an integer mask of the port constraints of the given map
     *
     * @param terminal     the cached cell state of the cell to determine the
     *                     port constraints for
     * @param edge         the edge connected to the constrained terminal
     * @param source       whether or not the edge specified is connected to the
     *                     terminal specified at its source end
     * @param defaultValue Default value to return if the key is undefined.
     * @return the mask of port constraint directions
     */
    public static int getPortConstraints(CellState terminal, CellState edge, boolean source, Set<Direction> defaultValue) {
        Set<Direction> directions = terminal.getStyle().getEdge().getPortConstraints();
        if (directions == null || directions.isEmpty()) {
            directions = defaultValue;
        }
        int returnValue = 0;
        if (directions.contains(Direction.NORTH)) {
            returnValue |= Direction.NORTH.getMask();
        }
        if (directions.contains(Direction.WEST)) {
            returnValue |= Direction.WEST.getMask();
        }
        if (directions.contains(Direction.SOUTH)) {
            returnValue |= Direction.SOUTH.getMask();
        }
        if (directions.contains(Direction.EAST)) {
            returnValue |= Direction.EAST.getMask();
        }
        return returnValue;
    }

    public static int reversePortConstraints(int constraint) {
        int result = 0;

        result = (constraint & Constants.DIRECTION_MASK_WEST) << 3;
        result |= (constraint & Constants.DIRECTION_MASK_NORTH) << 1;
        result |= (constraint & Constants.DIRECTION_MASK_SOUTH) >> 1;
        result |= (constraint & Constants.DIRECTION_MASK_EAST) >> 3;

        return result;
    }

    /**
     * Draws the image inside the clip bounds to the given graphics object.
     */
    public static void drawImageClip(Graphics g, BufferedImage image, ImageObserver observer) {
        java.awt.Rectangle clip = g.getClipBounds();

        if (clip != null) {
            int w = image.getWidth();
            int h = image.getHeight();

            int x = Math.max(0, Math.min(clip.x, w));
            int y = Math.max(0, Math.min(clip.y, h));

            w = Math.min(clip.width, w - x);
            h = Math.min(clip.height, h - y);

            if (w > 0 && h > 0) {
                // TODO: Support for normal images using fast subimage copies
                g.drawImage(image.getSubimage(x, y, w, h), clip.x, clip.y, observer);
            }
        } else {
            g.drawImage(image, 0, 0, observer);
        }
    }

    public static void fillClippedRect(Graphics g, int x, int y, int width, int height) {
        java.awt.Rectangle bg = new java.awt.Rectangle(x, y, width, height);

        try {
            if (g.getClipBounds() != null) {
                bg = bg.intersection(g.getClipBounds());
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to compute intersection", e);
            // FIXME: Getting clipbounds sometimes throws an NPE
        }

        g.fillRect(bg.x, bg.y, bg.width, bg.height);
    }

    /**
     * Creates a new list of new points obtained by translating the points in
     * the given list by the given vector. Elements that are not Points are
     * added to the result as-is.
     */
    public static List<PointDouble> translatePoints(List<PointDouble> pts, double dx, double dy) {
        List<PointDouble> result = null;
        if (pts != null) {
            result = new ArrayList<>(pts.size());
            for (PointDouble pt : pts) {
                PointDouble point = (PointDouble) pt.clone();
                point.setX(point.getX() + dx);
                point.setY(point.getY() + dy);
                result.add(point);
            }
        }

        return result;
    }

    /**
     * Returns the intersection of two lines as an Point.
     *
     * @param x0 X-coordinate of the first line's startpoint.
     * @param y0 Y-coordinate of the first line's startpoint.
     * @param x1 X-coordinate of the first line's endpoint.
     * @param y1 Y-coordinate of the first line's endpoint.
     * @param x2 X-coordinate of the second line's startpoint.
     * @param y2 Y-coordinate of the second line's startpoint.
     * @param x3 X-coordinate of the second line's endpoint.
     * @param y3 Y-coordinate of the second line's endpoint.
     * @return Returns the intersection between the two lines.
     */
    public static PointDouble intersection(double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3) {
        double denom = ((y3 - y2) * (x1 - x0)) - ((x3 - x2) * (y1 - y0));
        double nume_a = ((x3 - x2) * (y0 - y2)) - ((y3 - y2) * (x0 - x2));
        double nume_b = ((x1 - x0) * (y0 - y2)) - ((y1 - y0) * (x0 - x2));
        double ua = nume_a / denom;
        double ub = nume_b / denom;
        if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
            // Get the intersection point
            double intersectionX = x0 + ua * (x1 - x0);
            double intersectionY = y0 + ua * (y1 - y0);
            return new PointDouble(intersectionX, intersectionY);
        }

        // No intersection
        return null;
    }

    /**
     * Sorts the given cells according to the order in the cell hierarchy.
     */
    public static List<ICell> sortCells(List<ICell> cells, final boolean ascending) {
        return sortCells(cells, ascending);
    }

    /**
     * Sorts the given cells according to the order in the cell hierarchy.
     */
    public static Collection<ICell> sortCells(Collection<ICell> cells, final boolean ascending) {
        SortedSet<ICell> result = new TreeSet<>((o1, o2) -> {
            int comp = CellPath.compare(CellPath.create(o1), CellPath.create(o2));

            return (comp == 0) ? 0 : (((comp > 0) == ascending) ? 1 : -1);
        });

        result.addAll(cells);

        return result;
    }

    /**
     * Returns true if the given array contains the given object.
     */
    public static boolean contains(Object[] array, Object obj) {
        return indexOf(array, obj) >= 0;
    }

    /**
     * Returns the index of the given object in the given array of -1 if the
     * object is not contained in the array.
     */
    public static int indexOf(Object[] array, Object obj) {
        if (obj != null && array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == obj) {
                    return i;
                }
            }
        }

        return -1;
    }

    public static boolean intersectsHotspot(CellState state, int x, int y, double hotspot) {
        return intersectsHotspot(state, x, y, hotspot, 0, 0);
    }

    /**
     * Returns true if the given coordinate pair intersects the hotspot of the
     * given state.
     */
    public static boolean intersectsHotspot(CellState state, int x, int y, double hotspot, int min, int max) {
        if (hotspot > 0) {
            int cx = (int) Math.round(state.getCenterX());
            int cy = (int) Math.round(state.getCenterY());
            int width = (int) Math.round(state.getWidth());
            int height = (int) Math.round(state.getHeight());
            if (state.getStyle().getShape().getShape() instanceof SwimlaneShape) {
                int start = (int) state.getStyle().getEdge().getStartSize();
                if (state.getStyle().getCellProperties().isHorizontal()) {
                    cy = (int) Math.round(state.getY() + start / 2);
                    height = start;
                } else {
                    cx = (int) Math.round(state.getX() + start / 2);
                    width = start;
                }
            }
            int w = (int) Math.max(min, width * hotspot);
            int h = (int) Math.max(min, height * hotspot);

            if (max > 0) {
                w = Math.min(w, max);
                h = Math.min(h, max);
            }

            java.awt.Rectangle rect = new java.awt.Rectangle(Math.round(cx - w / 2), Math.round(cy - h / 2), w, h);

            return rect.contains(x, y);
        }

        return true;
    }

    /**
     * Returns true if the dictionary contains true for the given key or false
     * if no value is defined for the key.
     *
     * @param dict Dictionary that contains the key, value pairs.
     * @param key  Key whose value should be returned.
     * @return Returns the boolean value for key in dict.
     */
    public static boolean isTrue(Map<String, Object> dict, String key) {
        return isTrue(dict, key, false);
    }

    /**
     * Returns true if the dictionary contains true for the given key or the
     * given default value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the boolean value for key in dict.
     */
    public static boolean isTrue(Map<String, Object> dict, String key, boolean defaultValue) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            return value.equals("1") || value.toString().equalsIgnoreCase("true");
        }
    }

    /**
     * Returns the value for key in dictionary as an int or 0 if no value is
     * defined for the key.
     *
     * @param dict Dictionary that contains the key, value pairs.
     * @param key  Key whose value should be returned.
     * @return Returns the integer value for key in dict.
     */
    public static int getInt(Map<String, Object> dict, String key) {
        return getInt(dict, key, 0);
    }

    /**
     * Returns the value for key in dictionary as an int or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the integer value for key in dict.
     */
    public static int getInt(Map<String, Object> dict, String key, int defaultValue) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            // Handles commas by casting them to an int
            return (int) Float.parseFloat(value.toString());
        }
    }

    /**
     * Returns the value for key in dictionary as a float or 0 if no value is
     * defined for the key.
     *
     * @param dict Dictionary that contains the key, value pairs.
     * @param key  Key whose value should be returned.
     * @return Returns the float value for key in dict.
     */
    public static float getFloat(Map<String, Object> dict, String key) {
        return getFloat(dict, key, 0);
    }

    /**
     * Returns the value for key in dictionary as a float or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the float value for key in dict.
     */
    public static float getFloat(Map<String, Object> dict, String key, float defaultValue) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            return Float.parseFloat(value.toString());
        }
    }

    /**
     * Returns the value for key in dictionary as a float array or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the float array value for key in dict.
     */
    public static float[] getFloatArray(Map<String, Object> dict, String key, float[] defaultValue) {
        return getFloatArray(dict, key, defaultValue, ",");
    }

    /**
     * Returns the value for key in dictionary as a float array or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the float array value for key in dict.
     */
    public static float[] getFloatArray(Map<String, Object> dict, String key, float[] defaultValue, String separator) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            String[] floatChars = value.toString().split(separator);
            float[] result = new float[floatChars.length];

            for (int i = 0; i < floatChars.length; i++) {
                result[i] = Float.parseFloat(floatChars[i]);
            }

            return result;
        }
    }

    /**
     * Returns the value for key in dictionary as a double or 0 if no value is
     * defined for the key.
     *
     * @param dict Dictionary that contains the key, value pairs.
     * @param key  Key whose value should be returned.
     * @return Returns the double value for key in dict.
     */
    public static double getDouble(Map<String, Object> dict, String key) {
        return getDouble(dict, key, 0);
    }

    /**
     * Returns the value for key in dictionary as a double or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the double value for key in dict.
     */
    public static double getDouble(Map<String, Object> dict, String key, double defaultValue) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            return Double.parseDouble(value.toString());
        }
    }

    /**
     * Returns the value for key in dictionary as a string or null if no value
     * is defined for the key.
     *
     * @param dict Dictionary that contains the key, value pairs.
     * @param key  Key whose value should be returned.
     * @return Returns the string value for key in dict.
     */
    public static String getString(Map<String, Object> dict, String key) {
        return getString(dict, key, null);
    }

    /**
     * Returns the value for key in dictionary as a string or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the string value for key in dict.
     */
    public static String getString(Map<String, Object> dict, String key, String defaultValue) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            return value.toString();
        }
    }

    /**
     * Returns the value for key in dictionary as a color or null if no value is
     * defined for the key.
     *
     * @param dict Dictionary that contains the key, value pairs.
     * @param key  Key whose value should be returned.
     * @return Returns the color value for key in dict.
     */
    public static Color getColor(Map<String, Object> dict, String key) {
        return getColor(dict, key, null);
    }

    /**
     * Returns the value for key in dictionary as a color or the given default
     * value if no value is defined for the key.
     *
     * @param dict         Dictionary that contains the key, value pairs.
     * @param key          Key whose value should be returned.
     * @param defaultValue Default value to return if the key is undefined.
     * @return Returns the color value for key in dict.
     */
    public static Color getColor(Map<String, Object> dict, String key, Color defaultValue) {
        Object value = dict.get(key);

        if (value == null) {
            return defaultValue;
        } else {
            return parseColor(value.toString());
        }
    }

    public static Font getFont(Style style) {
        return getFont(style, 1);
    }

    public static Font getFont(Style style, double scale) {
        String fontFamily = style.getLabel().getFontFamily();
        int fontSize = style.getLabel().getFontSize();
        Set<FontModifier> fontModifiers = style.getLabel().getFontModifiers();
        int swingFontStyle = fontModifiers.contains(FontModifier.BOLD) ? Font.BOLD : Font.PLAIN;
        swingFontStyle += fontModifiers.contains(FontModifier.ITALIC) ? Font.ITALIC : Font.PLAIN;
        //https://github.com/elonderin/jgraphx/commit/c1c9b0ca7dee2b1e7ace0b0e88c3c06135bf236c
        Map<TextAttribute, Object> fontAttributes = new HashMap<>();
        if (fontModifiers.contains(FontModifier.UNDERLINE)) {
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        }
        if (fontModifiers.contains(FontModifier.STRIKETHROUGH)) {
            fontAttributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        }
        return new Font(fontFamily, swingFontStyle, (int) (fontSize * scale)).deriveFont(fontAttributes);
    }

    public static String hexString(Color color) {
        return HtmlColor.hexString(color);
    }

    /**
     * Shortcut for parseColor with no transparency.
     */
    public static Color parseColor(String colorString) throws NumberFormatException {
        return HtmlColor.parseColor(colorString);
    }

    /**
     * Convert a string representing a 24/32bit hex color value into a Color
     * object. The following color names are also supported: white, black, red,
     * green, blue, orange, yellow, pink, turquoise, gray and none (null).
     * Examples of possible hex color values are: #C3D9FF, #6482B9 and #774400,
     * but note that you do not include the "#" in the string passed in
     *
     * @param colorString the 24/32bit hex string value (ARGB)
     * @return java.awt.Color (24bit RGB on JDK 1.1, 24/32bit ARGB on JDK1.2)
     * @throws NumberFormatException if the specified string cannot be interpreted as a
     *                               hexidecimal integer
     */
    public static Color parseColor(String colorString, double alpha) throws NumberFormatException {
        return HtmlColor.parseColor(colorString, alpha);
    }

    /**
     * Returns a hex representation for the given color.
     *
     * @param color Color to return the hex string for.
     * @return Returns a hex string for the given color.
     */
    public static String getHexColorString(Color color) {
        return HtmlColor.getHexColorString(color);
    }

    /**
     * Convert a string representing a dash pattern into a float array.
     * A valid dash pattern is a string of dash widths (floating point values)
     * separated by space characters.
     *
     * @param dashPatternString the string representing the dash pattern
     * @return float[]
     * @throws NumberFormatException if any of the dash widths cannot be interpreted as a
     *                               floating point number
     */
    public static float[] parseDashPattern(String dashPatternString) throws NumberFormatException {
        if (dashPatternString != null && dashPatternString.length() > 0) {
            String[] tokens = dashPatternString.split(" ");
            float[] dashpattern = new float[tokens.length];
            float dashWidth;

            for (int i = 0; i < tokens.length; i++) {
                dashWidth = Float.parseFloat(tokens[i]);

                if (dashWidth > 0) {
                    dashpattern[i] = dashWidth;
                } else {
                    throw new NumberFormatException("Dash width must be positive");
                }
            }

            return dashpattern;
        }
        return null;
    }

    /**
     * Returns true if the user object is an XML node with the specified type
     * and the optional attribute has the specified value or is not
     * specified.
     *
     * @param value    Object that should be examined as a node.
     * @param nodeName String that specifies the node name.
     * @return Returns true if the node name of the user object is equal to the
     * given type.
     */

    public static boolean isNode(Object value, String nodeName) {
        return isNode(value, nodeName, null, null);
    }

    /**
     * Returns true if the given value is an XML node with the node name and if
     * the optional attribute has the specified value.
     *
     * @param value          Object that should be examined as a node.
     * @param nodeName       String that specifies the node name.
     * @param attributeName  Optional attribute name to check.
     * @param attributeValue Optional attribute value to check.
     * @return Returns true if the value matches the given conditions.
     */
    public static boolean isNode(Object value, String nodeName, String attributeName, String attributeValue) {
        if (value instanceof Element) {
            Element element = (Element) value;

            if (nodeName == null || element.getNodeName().equalsIgnoreCase(nodeName)) {
                String tmp = (attributeName != null) ? element.getAttribute(attributeName) : null;

                return attributeName == null || tmp.equals(attributeValue);
            }
        }

        return false;
    }

    public static void setAntiAlias(Graphics2D g, boolean antiAlias, boolean textAntiAlias) {
        g.setRenderingHint(RenderingHints.KEY_RENDERING, (antiAlias) ? RenderingHints.VALUE_RENDER_QUALITY : RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (antiAlias) ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, (textAntiAlias) ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    /**
     * Clears the given area of the specified graphics object with the given
     * color or makes the region transparent.
     */
    public static void clearRect(Graphics2D g, java.awt.Rectangle rect, Color background) {
        if (background != null) {
            g.setColor(background);
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
        } else {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            g.fillRect(rect.x, rect.y, rect.width, rect.height);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    /**
     * Creates a buffered image for the given parameters. If there is not enough
     * memory to create the image then a OutOfMemoryError is thrown.
     */
    public static BufferedImage createBufferedImage(int w, int h, Color background) {
        return Utils.createBufferedImage(w, h, background, (background != null) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Creates a buffered image for the given parameters. If there is not enough
     * memory to create the image then a OutOfMemoryError is thrown.
     */
    public static BufferedImage createBufferedImage(int w, int h, Color background, int type) {
        BufferedImage result = null;

        if (w > 0 && h > 0) {
            result = new BufferedImage(w, h, type);

            // Clears background
            if (background != null) {
                Graphics2D g2 = result.createGraphics();
                clearRect(g2, new java.awt.Rectangle(w, h), background);
                g2.dispose();
            }
        }

        return result;
    }

    /**
     * Loads an image from the local filesystem, a data URI or any other URL.
     */
    public static BufferedImage loadImage(String url) {
        BufferedImage img = null;

        if (url != null) {
            URL realUrl;
            try {
                realUrl = new URL(url);
            } catch (Exception e) {
                realUrl = Utils.class.getResource(url);
            }
            if (realUrl != null) {
                try {
                    img = ImageIO.read(realUrl);
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "Failed to read the image from " + realUrl, e1);
                }
            } else {
                log.log(Level.SEVERE, "Failed to load image from " + url);
            }
        }

        return img;
    }
}
