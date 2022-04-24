package com.faforever.neroxis.ngraph.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GeometryChange extends AtomicGraphModelChange {

    protected ICell cell;
    protected Geometry geometry, previous;

    public GeometryChange(GraphModel model, ICell cell, Geometry geometry) {
        super(model);
        this.cell = cell;
        this.geometry = geometry;
        this.previous = this.geometry;
    }

    /**
     * Changes the root of the model.
     */
    @Override
    public void execute() {
        geometry = previous;
        previous = ((GraphModel) model).geometryForCellChanged(cell, previous);
    }
}
