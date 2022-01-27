package com.faforever.neroxis.ui.panel;

import com.faforever.neroxis.graph.domain.MaskGraphVertex;
import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.ui.listener.BoundsPopupMenuListener;
import com.faforever.neroxis.ui.model.GraphVertexParameterTableModel;
import com.faforever.neroxis.ui.renderer.StringListCellRenderer;
import com.faforever.neroxis.ui.renderer.StringTableCellRenderer;
import com.faforever.neroxis.util.MaskReflectUtil;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

import static javax.swing.SwingConstants.CENTER;

public class MaskGraphVertexEditPanel extends JPanel {
    private final JComboBox<Class<? extends Mask<?, ?>>> maskClassComboBox = new JComboBox<>();
    private final JComboBox<Executable> maskExecutableComboBox = new JComboBox<>();
    private final GraphVertexParameterTableModel parameterTableModel = new GraphVertexParameterTableModel();
    private final JTable parametersTable = new JTable(parameterTableModel);
    private MaskGraphVertex vertex;

    public MaskGraphVertexEditPanel() {
        setLayout(new GridBagLayout());
        setupClassComboBox();
        setupExecutableComboBox();
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
                vertex.setMaskClass(maskClass);
                MaskReflectUtil.getMaskMethods(maskClass).forEach(maskExecutableComboBox::addItem);
            }
        });

        GridBagConstraints comboBoxConstraints = new GridBagConstraints();
        comboBoxConstraints.fill = GridBagConstraints.BOTH;
        comboBoxConstraints.gridx = 1;
        comboBoxConstraints.weightx = 0;
        comboBoxConstraints.gridy = 0;
        comboBoxConstraints.weighty = 0;

        add(maskClassComboBox, comboBoxConstraints);

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.fill = GridBagConstraints.BOTH;
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
            if (vertex.getMaskClass() == null) {
                return;
            }

            Executable selectedExecutable = (Executable) maskExecutableComboBox.getSelectedItem();
            vertex.setExecutable(selectedExecutable);
            if (selectedExecutable != null) {
                parameterTableModel.setParameters(selectedExecutable.getParameters());
            } else {
                parameterTableModel.setParameters();
            }
        });

        maskExecutableComboBox.setRenderer(
                new StringListCellRenderer<Executable>(executable -> String.format("%s(%s%s)",
                        executable.getName(),
                        Arrays.stream(executable.getParameters())
                                .limit(4)
                                .map(Parameter::getName)
                                .collect(Collectors.joining(", ")),
                        executable.getParameters().length > 4 ? "..." : "")
                )
        );

        GridBagConstraints comboBoxConstraints = new GridBagConstraints();
        comboBoxConstraints.fill = GridBagConstraints.NONE;
        comboBoxConstraints.gridx = 1;
        comboBoxConstraints.weightx = 0;
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

    private void setupParametersTable() {
        parametersTable.setDefaultRenderer(Class.class, new StringTableCellRenderer<Class<?>>(Class::getSimpleName));
        parametersTable.getColumnModel().getColumn(0).setPreferredWidth(75);
        parametersTable.getColumnModel().getColumn(1).setPreferredWidth(175);
        parametersTable.getColumnModel().getColumn(2).setPreferredWidth(75);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.gridwidth = 2;
        constraints.gridy = 2;
        constraints.weighty = 1;

        add(parametersTable, constraints);
    }

    public void setVertex(MaskGraphVertex vertex) {
        this.vertex = vertex;
        maskExecutableComboBox.setEnabled(true);
        maskClassComboBox.setEnabled(true);
        maskClassComboBox.setSelectedItem(vertex.getMaskClass());
        maskExecutableComboBox.setSelectedItem(vertex.getExecutable());
        parameterTableModel.setVertex(vertex);
    }

}
