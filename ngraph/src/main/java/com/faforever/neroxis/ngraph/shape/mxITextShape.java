/**
 * Copyright (c) 2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.shape;

import com.faforever.neroxis.ngraph.canvas.mxGraphics2DCanvas;
import com.faforever.neroxis.ngraph.view.mxCellState;

import java.util.Map;

public interface mxITextShape {
	/**
	 *
	 */
	void paintShape(mxGraphics2DCanvas canvas, String text, mxCellState state, Map<String, Object> style);

}
