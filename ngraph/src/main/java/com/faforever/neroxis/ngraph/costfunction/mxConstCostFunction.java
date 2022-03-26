package com.faforever.neroxis.ngraph.costfunction;

import com.faforever.neroxis.ngraph.view.mxCellState;

/**
 * @author Mate
 * A constant cost function that returns <b>const</b> regardless of edge value
 */
public class mxConstCostFunction extends com.faforever.neroxis.ngraph.costfunction.mxCostFunction {
    private final double cost;

    public mxConstCostFunction(double cost) {
        this.cost = cost;
    }

    public double getCost(mxCellState state) {
        return cost;
    }

}
