package com.faforever.neroxis.ui.panel;

import com.faforever.neroxis.graph.domain.MaskConstructorVertex;
import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.graph.domain.MaskMethodEdge;
import com.faforever.neroxis.graph.domain.MaskMethodVertex;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.listener.BoundsPopupMenuListener;
import com.faforever.neroxis.ui.model.GraphVertexParameterTableModel;
import com.faforever.neroxis.ui.renderer.StringListCellRenderer;
import com.faforever.neroxis.ui.renderer.StringTableCellRenderer;
import com.faforever.neroxis.util.MaskReflectUtil;
import lombok.Setter;
import org.jgrapht.Graph;
import org.jungrapht.visualization.VisualizationViewer;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static javax.swing.SwingConstants.CENTER;

public class MaskGraphVertexEditPanel extends JPanel {
    private final JComboBox<Class<? extends Mask<?, ?>>> maskClassComboBox = new JComboBox<>();
    private final JComboBox<Executable> maskExecutableComboBox = new JComboBox<>();
    private final GraphVertexParameterTableModel parameterTableModel = new GraphVertexParameterTableModel();
    private final JTable parametersTable = new JTable(parameterTableModel);
    private final JLabel returnLabel = new JLabel();
    private MaskGraphVertex<?> vertex;
    private boolean ready;

    @Setter
    private VisualizationViewer<MaskGraphVertex<?>, MaskMethodEdge> visualizationViewer;

    public MaskGraphVertexEditPanel() {
        setLayout(new GridBagLayout());
        setupClassComboBox();
        setupExecutableComboBox();
        setupReturnClassLabel();
        setupParametersTable();
    }

    private void setupClassComboBox() {
        maskClassComboBox.setEnabled(false);
        maskClassComboBox.setEditable(false);
        MaskReflectUtil.getMaskClasses().forEach(maskClassComboBox::addItem);
        maskClassComboBox.setSelectedItem(null);
        maskClassComboBox.setPreferredSize(new Dimension(150, 25));

        maskClassComboBox.setRenderer(new StringListCellRenderer<Class<?>>(Class::getSimpleName));
        maskClassComboBox.addActionListener(e -> {
            maskExecutableComboBox.removeAllItems();
            Class<? extends Mask<?, ?>> maskClass = (Class<? extends Mask<?, ?>>) maskClassComboBox.getSelectedItem();
            if (maskClass != null) {
                vertex.setExecutorClass(maskClass);
                if (vertex instanceof MaskMethodVertex) {
                    MaskReflectUtil.getMaskMethods(maskClass).forEach(maskExecutableComboBox::addItem);
                } else if (vertex instanceof MaskConstructorVertex) {
                    MaskReflectUtil.getMaskConstructors(maskClass).forEach(maskExecutableComboBox::addItem);
                }
            }
            visualizationViewer.repaint();
        });

        GridBagConstraints comboBoxConstraints = new GridBagConstraints();
        comboBoxConstraints.fill = GridBagConstraints.HORIZONTAL;
        comboBoxConstraints.gridx = 1;
        comboBoxConstraints.weightx = 1;
        comboBoxConstraints.gridy = 0;
        comboBoxConstraints.weighty = 0;

        add(maskClassComboBox, comboBoxConstraints);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.gridx = 0;
        labelConstraints.weightx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.weighty = 0;

        JLabel maskClassLabel = new JLabel("Mask Class");
        maskClassLabel.setHorizontalAlignment(CENTER);
        add(maskClassLabel, labelConstraints);
    }

    private void setupExecutableComboBox() {
        maskExecutableComboBox.setEnabled(false);
        maskExecutableComboBox.setEditable(false);
        maskExecutableComboBox.setPreferredSize(new Dimension(150, 25));
        maskExecutableComboBox.addPopupMenuListener(new BoundsPopupMenuListener(true, false));

        maskExecutableComboBox.addActionListener(e -> {
            if (!ready) {
                return;
            }

            if (vertex.getExecutorClass() == null) {
                return;
            }

            Executable selectedExecutable = (Executable) maskExecutableComboBox.getSelectedItem();
            if (vertex instanceof MaskMethodVertex) {
                ((MaskMethodVertex) vertex).setExecutable((Method) selectedExecutable);
            } else if (vertex instanceof MaskConstructorVertex) {
                ((MaskConstructorVertex<?>) vertex).setExecutable((Constructor) selectedExecutable);
            }

            parameterTableModel.updateTableModel();
            visualizationViewer.repaint();
        });

        maskExecutableComboBox.setRenderer(
                new StringListCellRenderer<Executable>(executable -> {
                    String parametersString = Arrays.stream(executable.getParameters())
                            .limit(4)
                            .map(Parameter::getName)
                            .collect(Collectors.joining(", "));
                    String parametersEllipsis = executable.getParameters().length > 4 ? "..." : "";
                    if (executable instanceof Constructor) {
                        return String.format("%s(%s%s)",
                                executable.getDeclaringClass().getSimpleName(),
                                parametersString,
                                parametersEllipsis);
                    } else {
                        return String.format("%s(%s%s)",
                                executable.getName(),
                                parametersString,
                                parametersEllipsis);
                    }
                })
        );

        GridBagConstraints comboBoxConstraints = new GridBagConstraints();
        comboBoxConstraints.fill = GridBagConstraints.HORIZONTAL;
        comboBoxConstraints.gridx = 1;
        comboBoxConstraints.weightx = 1;
        comboBoxConstraints.gridy = 1;
        comboBoxConstraints.weighty = 0;

        add(maskExecutableComboBox, comboBoxConstraints);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.NONE;
        labelConstraints.gridx = 0;
        labelConstraints.weightx = 0;
        labelConstraints.gridy = 1;
        labelConstraints.weighty = 0;

        JLabel methodLabel = new JLabel("Method");
        methodLabel.setHorizontalAlignment(CENTER);
        add(methodLabel, labelConstraints);
    }

    private void setupReturnClassLabel() {
        JLabel label1 = new JLabel("Return Class");

        GridBagConstraints constraints1 = new GridBagConstraints();
        constraints1.fill = GridBagConstraints.NONE;
        constraints1.gridx = 0;
        constraints1.weightx = 1;
        constraints1.gridy = 2;
        constraints1.weighty = 0;

        add(label1, constraints1);

        GridBagConstraints constraints2 = new GridBagConstraints();
        constraints2.fill = GridBagConstraints.NONE;
        constraints2.gridx = 1;
        constraints2.weightx = 1;
        constraints2.gridy = 2;
        constraints2.weighty = 0;

        add(returnLabel, constraints2);
    }

    private void setupParametersTable() {
        parametersTable.setDefaultRenderer(Class.class, new StringTableCellRenderer<Class<?>>(Class::getSimpleName));
        parametersTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        parametersTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        parametersTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 2;
        constraints.gridy = 3;
        constraints.weighty = 1;

        add(parametersTable, constraints);
    }

    public void setVertex(MaskGraphVertex<?> vertex) {
        this.vertex = vertex;
        updatePanel();
    }

    public void updatePanel() {
        Graph<MaskGraphVertex<?>, MaskMethodEdge> graph = visualizationViewer.getVisualizationModel().getGraph();
        if (vertex != null && !graph.containsVertex(vertex)) {
            setVertex(null);
            return;
        }

        ready = false;
        boolean hasNoEdges = vertex != null && graph.incomingEdgesOf(vertex).isEmpty();
        maskExecutableComboBox.setEnabled(hasNoEdges);
        maskClassComboBox.setEnabled(hasNoEdges);
        maskClassComboBox.setSelectedItem(vertex == null ? null : vertex.getExecutorClass());
        maskExecutableComboBox.setSelectedItem(vertex == null ? null : vertex.getExecutable());
        parameterTableModel.setVertex(vertex);

        String resultClassName = "";
        if (vertex != null && vertex.getResultClass(MaskGraphVertex.SELF) != null) {
            resultClassName = vertex.getResultClass(MaskGraphVertex.SELF).getSimpleName();
        }
        returnLabel.setText(resultClassName);

        ready = true;
    }

}
