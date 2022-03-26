/**
 * Copyright (c) 2008-2009, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.layout.orthogonal;

import com.faforever.neroxis.ngraph.layout.GraphLayout;
import com.faforever.neroxis.ngraph.layout.orthogonal.model.OrthogonalModel;
import com.faforever.neroxis.ngraph.view.Graph;

/**
 *
 */

/**
 *
 */
public class OrthogonalLayout extends GraphLayout {

    /**
     *
     */
    protected OrthogonalModel orthModel;

    /**
     * Whether or not to route the edges along grid lines only, if the grid
     * is enabled. Default is false
     */
    protected boolean routeToGrid = false;

    /**
     *
     */
    public OrthogonalLayout(Graph graph) {
        super(graph);
        orthModel = new OrthogonalModel(graph);
    }

    /**
     *
     */
    public void execute(Object parent) {
        // Create the rectangulation

    }

}
