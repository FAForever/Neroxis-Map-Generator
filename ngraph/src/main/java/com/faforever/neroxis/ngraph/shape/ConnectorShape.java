/**
 * Copyright (c) 2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.LineDouble;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnectorShape extends BasicShape {

    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        if (state.getAbsolutePointCount() > 1 && configureGraphics(canvas, state, false)) {
            List<PointDouble> pts = new ArrayList<>(state.getAbsolutePoints());
            Map<String, Object> style = state.getStyle();

            // Paints the markers and updates the points
            // Switch off any dash pattern for markers
            boolean dashed = Utils.isTrue(style, Constants.STYLE_DASHED);
            Object dashedValue = style.get(Constants.STYLE_DASHED);

            if (dashed) {
                style.remove(Constants.STYLE_DASHED);
                canvas.getGraphics().setStroke(canvas.createStroke(style));
            }

            translatePoint(pts, 0, paintMarker(canvas, state, true));
            translatePoint(pts, pts.size() - 1, paintMarker(canvas, state, false));

            if (dashed) {
                // Replace the dash pattern
                style.put(Constants.STYLE_DASHED, dashedValue);
                canvas.getGraphics().setStroke(canvas.createStroke(style));
            }

            paintPolyline(canvas, pts, state.getStyle());
        }
    }

    protected void paintPolyline(Graphics2DCanvas canvas, List<PointDouble> points, Map<String, Object> style) {
        boolean rounded = isRounded(style) && canvas.getScale() > Constants.MIN_SCALE_FOR_ROUNDED_LINES;
        canvas.paintPolyline(points.toArray(new PointDouble[0]), rounded);
    }

    public boolean isRounded(Map<String, Object> style) {
        return Utils.isTrue(style, Constants.STYLE_ROUNDED, false);
    }

    private void translatePoint(List<PointDouble> points, int index, PointDouble offset) {
        if (offset != null) {
            PointDouble pt = (PointDouble) points.get(index).clone();
            pt.setX(pt.getX() + offset.getX());
            pt.setY(pt.getY() + offset.getY());
            points.set(index, pt);
        }
    }

    /**
     * Draws the marker for the given edge.
     *
     * @return the offset of the marker from the end of the line
     */
    public PointDouble paintMarker(Graphics2DCanvas canvas, CellState state, boolean source) {
        Map<String, Object> style = state.getStyle();
        float strokeWidth = (float) (Utils.getFloat(style, Constants.STYLE_STROKEWIDTH, 1) * canvas.getScale());
        String type = Utils.getString(style, (source) ? Constants.STYLE_STARTARROW : Constants.STYLE_ENDARROW, "");
        float size = (Utils.getFloat(style, (source) ? Constants.STYLE_STARTSIZE : Constants.STYLE_ENDSIZE, Constants.DEFAULT_MARKERSIZE));
        Color color = Utils.getColor(style, Constants.STYLE_STROKECOLOR);
        canvas.getGraphics().setColor(color);
        double absSize = size * canvas.getScale();
        List<PointDouble> points = state.getAbsolutePoints();
        LineDouble markerVector = getMarkerVector(points, source, absSize);
        PointDouble p0 = markerVector.getP1();
        PointDouble pe = markerVector.getP2();
        PointDouble offset;
        // Computes the norm and the inverse norm
        double dx = pe.getX() - p0.getX();
        double dy = pe.getY() - p0.getY();
        double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        double unitX = dx / dist;
        double unitY = dy / dist;
        double nx = unitX * absSize;
        double ny = unitY * absSize;
        // Allow for stroke width in the end point used and the
        // orthogonal vectors describing the direction of the
        // marker
        double strokeX = unitX * strokeWidth;
        double strokeY = unitY * strokeWidth;
        pe = (PointDouble) pe.clone();
        pe.setX(pe.getX() - strokeX / 2.0);
        pe.setY(pe.getY() - strokeY / 2.0);

        IMarker marker = MarkerRegistry.getMarker(type);

        if (marker != null) {
            offset = marker.paintMarker(canvas, state, type, pe, nx, ny, absSize, source);

            if (offset != null) {
                offset.setX(offset.getX() - strokeX / 2.0);
                offset.setY(offset.getY() - strokeY / 2.0);
            }
        } else {
            // Offset for the strokewidth
            nx = dx * strokeWidth / dist;
            ny = dy * strokeWidth / dist;
            offset = new PointDouble(-strokeX / 2.0, -strokeY / 2.0);
        }

        return offset;
    }

    /**
     * Hook to override creation of the vector that the marker is drawn along
     * since it may not be the same as the vector between any two control
     * points
     *
     * @param points     the guide points of the connector
     * @param source     whether the marker is at the source end
     * @param markerSize the scaled maximum length of the marker
     * @return a line describing the vector the marker should be drawn along
     */
    protected LineDouble getMarkerVector(List<PointDouble> points, boolean source, double markerSize) {
        int n = points.size();
        PointDouble p0 = (source) ? points.get(1) : points.get(n - 2);
        PointDouble pe = (source) ? points.get(0) : points.get(n - 1);
        int count = 1;
        // Uses next non-overlapping point
        while (count < n - 1 && Math.round(p0.getX() - pe.getX()) == 0 && Math.round(p0.getY() - pe.getY()) == 0) {
            p0 = (source) ? points.get(1 + count) : points.get(n - 2 - count);
            count++;
        }
        return new LineDouble(p0, pe);
    }

}
