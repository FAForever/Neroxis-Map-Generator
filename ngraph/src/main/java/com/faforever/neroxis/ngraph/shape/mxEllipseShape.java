package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.mxGraphics2DCanvas;
import com.faforever.neroxis.ngraph.view.mxCellState;

import java.awt.*;
import java.awt.geom.Ellipse2D;

public class mxEllipseShape extends mxBasicShape {

	/**
	 *
	 */
	public Shape createShape(mxGraphics2DCanvas canvas, mxCellState state) {
		Rectangle temp = state.getRectangle();

		return new Ellipse2D.Float(temp.x, temp.y, temp.width, temp.height);
	}

}
