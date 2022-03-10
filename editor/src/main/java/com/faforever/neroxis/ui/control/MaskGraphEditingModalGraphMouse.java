package com.faforever.neroxis.ui.control;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import org.jungrapht.visualization.MultiLayerTransformer;
import org.jungrapht.visualization.RenderContext;
import org.jungrapht.visualization.control.AbstractGraphMousePlugin;
import org.jungrapht.visualization.control.CrossoverScalingControl;
import org.jungrapht.visualization.control.DefaultModalGraphMouse;
import org.jungrapht.visualization.control.ModalGraphMouse;
import org.jungrapht.visualization.control.ScalingGraphMousePlugin;
import org.jungrapht.visualization.control.SelectingGraphMousePlugin;

import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MaskGraphEditingModalGraphMouse extends DefaultModalGraphMouse<MaskGraphVertex<?>, MaskMethodEdge>
        implements ModalGraphMouse {

    /**
     * Build an instance of a EditingModalGraphMouse
     */
    public static class Builder
            extends DefaultModalGraphMouse.Builder<MaskGraphVertex<?>, MaskMethodEdge, MaskGraphEditingModalGraphMouse, Builder> {

        protected Supplier<Map<MaskGraphVertex<?>, String>> vertexLabelMapSupplier = HashMap::new;
        protected Supplier<Map<MaskMethodEdge, String>> edgeLabelMapSupplier = HashMap::new;
        protected Supplier<MultiLayerTransformer> multiLayerTransformerSupplier;
        protected Supplier<RenderContext<MaskGraphVertex<?>, MaskMethodEdge>> renderContextSupplier;

        public Builder vertexLabelMapSupplier(Supplier<Map<MaskGraphVertex<?>, String>> vertexLabelMapSupplier) {
            this.vertexLabelMapSupplier = vertexLabelMapSupplier;
            return self();
        }

        public Builder edgeLabelMapSupplier(Supplier<Map<MaskMethodEdge, String>> edgeLabelMapSupplier) {
            this.edgeLabelMapSupplier = edgeLabelMapSupplier;
            return self();
        }

        public Builder multiLayerTransformerSupplier(
                Supplier<MultiLayerTransformer> multiLayerTransformerSupplier) {
            this.multiLayerTransformerSupplier = multiLayerTransformerSupplier;
            return self();
        }

        public Builder renderContextSupplier(Supplier<RenderContext<MaskGraphVertex<?>, MaskMethodEdge>> renderContextSupplier) {
            this.renderContextSupplier = renderContextSupplier;
            return self();
        }

        public MaskGraphEditingModalGraphMouse build() {
            return new MaskGraphEditingModalGraphMouse(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected Supplier<MultiLayerTransformer> multiLayerTransformerSupplier;
    protected Map<MaskGraphVertex<?>, String> vertexLabelMap;
    protected Map<MaskMethodEdge, String> edgeLabelMap;
    protected MaskGraphPopupMousePlugin popupEditingPlugin;
    protected MultiLayerTransformer basicTransformer;
    protected RenderContext<MaskGraphVertex<?>, MaskMethodEdge> rc;

    public MaskGraphEditingModalGraphMouse(Builder builder) {
        super(builder);
        this.multiLayerTransformerSupplier = builder.multiLayerTransformerSupplier;
        this.vertexLabelMap = builder.vertexLabelMapSupplier.get();
        this.edgeLabelMap = builder.edgeLabelMapSupplier.get();
        this.basicTransformer = builder.multiLayerTransformerSupplier.get();
        this.rc = builder.renderContextSupplier.get();
    }

    /**
     * create the plugins, and load the plugins for TRANSFORMING mode
     */
    @Override
    public void loadPlugins() {
        selectingPlugin =
                SelectingGraphMousePlugin.builder()
                        .singleSelectionMask(InputEvent.BUTTON1_DOWN_MASK)
                        .toggleSingleSelectionMask(InputEvent.BUTTON1_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK)
                        .build();
        scalingPlugin =
                ScalingGraphMousePlugin.builder().scalingControl(new CrossoverScalingControl()).build();
        popupEditingPlugin = new MaskGraphPopupMousePlugin();
        setEditingMode();
    }

    /**
     * setter for the Mode.
     */
    @Override
    public void setMode(Mode mode) {

    }

    protected void setEditingMode() {
        clear();
        add(scalingPlugin);
        add(selectingPlugin);
        add(popupEditingPlugin);
    }


    /**
     * @return the editingPlugin
     */
    public AbstractGraphMousePlugin getSelectingPlugin() {
        return selectingPlugin;
    }

    /**
     * @return the popupEditingPlugin
     */
    public MaskGraphPopupMousePlugin getPopupEditingPlugin() {
        return popupEditingPlugin;
    }
}
