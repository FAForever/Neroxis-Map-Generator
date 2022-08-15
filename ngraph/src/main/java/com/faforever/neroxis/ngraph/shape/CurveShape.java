/**
 * Copyright (c) 2009-2010, David Benson, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.Graphics2DCanvas;
import com.faforever.neroxis.ngraph.style.Style;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.Curve;
import com.faforever.neroxis.ngraph.util.LineDouble;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.RenderingHints;
import java.util.List;

public class CurveShape extends ConnectorShape {
    /**
     * Cache of the points between which drawing straight lines views as a
     * curve
     */
    protected Curve curve;

    public CurveShape() {
        this(new Curve());
    }

    public CurveShape(Curve curve) {
        this.curve = curve;
    }

    public Curve getCurve() {
        return curve;
    }

    @Override
    public void paintShape(Graphics2DCanvas canvas, CellState state) {
        Object keyStrokeHint = canvas.getGraphics().getRenderingHint(RenderingHints.KEY_STROKE_CONTROL);
        canvas.getGraphics().setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        super.paintShape(canvas, state);

        canvas.getGraphics().setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, keyStrokeHint);
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
    @Override
    protected LineDouble getMarkerVector(List<PointDouble> points, boolean source, double markerSize) {
        double curveLength = curve.getCurveLength(Curve.CORE_CURVE);
        double markerRatio = markerSize / curveLength;
        if (markerRatio >= 1.0) {
            markerRatio = 1.0;
        }
        if (source) {
            LineDouble sourceVector = curve.getCurveParallel(Curve.CORE_CURVE, markerRatio);
            return new LineDouble(sourceVector.getP1(), points.get(0));
        } else {
            LineDouble targetVector = curve.getCurveParallel(Curve.CORE_CURVE, 1.0 - markerRatio);
            int pointCount = points.size();
            return new LineDouble(targetVector.getP1(), points.get(pointCount - 1));
        }
    }

    @Override
    protected void paintPolyline(Graphics2DCanvas canvas, List<PointDouble> points, Style style) {
        double scale = canvas.getScale();
        validateCurve(points, scale, style);
        canvas.paintPolyline(curve.getCurvePoints(Curve.CORE_CURVE), false);
    }

    /**
     * Forces underlying curve to a valid state
     */
    public void validateCurve(List<PointDouble> points, double scale, Style style) {
        if (curve == null) {
            curve = new Curve(points);
        } else {
            curve.updateCurve(points);
        }
        curve.setLabelBuffer(scale * Constants.DEFAULT_LABEL_BUFFER);
    }
}
