/**
 * Copyright (c) 2008, Gaudenz Alder
 */
package com.faforever.neroxis.ngraph.swing.view;

import com.faforever.neroxis.ngraph.model.Geometry;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.style.util.HorizontalAlignment;
import com.faforever.neroxis.ngraph.style.util.VerticalAlignment;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLWriter;
import javax.swing.text.html.MinimalHTMLWriter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.EventObject;

/**
 * To control this editor, use Graph.invokesStopCellEditing, Graph.
 * enterStopsCellEditing and Graph.escapeEnabled.
 */
public class CellEditor implements ICellEditor {
    private static final String CANCEL_EDITING = "cancel-editing";
    private static final String INSERT_BREAK = "insert-break";
    private static final String SUBMIT_TEXT = "submit-text";
    public static int DEFAULT_MIN_WIDTH = 100;
    public static int DEFAULT_MIN_HEIGHT = 60;
    public static double DEFAULT_MINIMUM_EDITOR_SCALE = 1;
    protected GraphComponent graphComponent;
    /**
     * Defines the minimum scale to be used for the editor. Set this to
     * 0 if the font size in the editor
     */
    protected double minimumEditorScale = DEFAULT_MINIMUM_EDITOR_SCALE;
    protected int minimumWidth = DEFAULT_MIN_WIDTH;
    protected int minimumHeight = DEFAULT_MIN_HEIGHT;
    protected transient ICell editingCell;
    protected transient EventObject trigger;
    protected transient JScrollPane scrollPane;
    /**
     * Holds the editor for plain text editing.
     */
    protected transient JTextArea textArea;
    /**
     * Holds the editor for HTML editing.
     */
    protected transient JEditorPane editorPane;
    /**
     * Specifies if the text content of the HTML body should be extracted
     * before and after editing for HTML markup. Default is true.
     */
    protected boolean extractHtmlBody = true;
    /**
     * Specifies if linefeeds should be replaced with BREAKS before editing,
     * and BREAKS should be replaced with linefeeds after editing. This
     * value is ignored if extractHtmlBody is false. Default is true.
     */
    protected boolean replaceLinefeeds = true;
    /**
     * Specifies if shift ENTER should submit text if enterStopsCellEditing
     * is true. Default is false.
     */
    protected boolean shiftEnterSubmitsText = false;
    protected AbstractAction cancelEditingAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stopEditing(true);
        }
    };
    protected AbstractAction textSubmitAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            stopEditing(false);
        }
    };
    transient Object editorEnterActionMapKey;
    transient Object textEnterActionMapKey;
    transient KeyStroke escapeKeystroke = KeyStroke.getKeyStroke("ESCAPE");
    transient KeyStroke enterKeystroke = KeyStroke.getKeyStroke("ENTER");
    transient KeyStroke shiftEnterKeystroke = KeyStroke.getKeyStroke("shift ENTER");

    public CellEditor(GraphComponent graphComponent) {
        this.graphComponent = graphComponent;

        // Creates the plain text editor
        textArea = new JTextArea();
        textArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        textArea.setOpaque(false);

        // Creates the HTML editor
        editorPane = new JEditorPane();
        editorPane.setOpaque(false);
        editorPane.setBackground(new Color(0, 0, 0, 0));
        editorPane.setContentType("text/html");

        // Workaround for inserted linefeeds in HTML markup with
        // lines that are longar than 80 chars
        editorPane.setEditorKit(new NoLinefeedHtmlEditorKit());

        // Creates the scollpane that contains the editor
        // FIXME: Cursor not visible when scrolling
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVisible(false);
        scrollPane.setOpaque(false);

        // Installs custom actions
        editorPane.getActionMap().put(CANCEL_EDITING, cancelEditingAction);
        textArea.getActionMap().put(CANCEL_EDITING, cancelEditingAction);
        editorPane.getActionMap().put(SUBMIT_TEXT, textSubmitAction);
        textArea.getActionMap().put(SUBMIT_TEXT, textSubmitAction);

        // Remembers the action map key for the enter keystroke
        editorEnterActionMapKey = editorPane.getInputMap().get(enterKeystroke);
        textEnterActionMapKey = editorPane.getInputMap().get(enterKeystroke);
    }

    /**
     * Returns the current editor or null if no editing is in progress.
     */
    public Component getEditor() {
        if (textArea.getParent() != null) {
            return textArea;
        } else if (editingCell != null) {
            return editorPane;
        }

        return null;
    }

    @Override
    public Object getEditingCell() {
        return editingCell;
    }

    @Override
    public void startEditing(ICell cell, EventObject evt) {
        if (editingCell != null) {
            stopEditing(true);
        }

        CellState state = graphComponent.getGraph().getView().getState(cell);

        if (state != null) {
            editingCell = cell;
            trigger = evt;
            double scale = Math.max(minimumEditorScale, graphComponent.getGraph().getView().getScale());
            scrollPane.setBounds(getEditorBounds(state, scale));
            scrollPane.setVisible(true);
            String value = getInitialValue(state, evt);
            JTextComponent currentEditor = null;
            textArea.setFont(Utils.getFont(state.getStyle(), scale));
            Color fontColor = state.getStyle().getLabel().getTextColor();
            textArea.setForeground(fontColor);
            textArea.setText(value);
            scrollPane.setViewportView(textArea);
            currentEditor = textArea;
            graphComponent.getGraphControl().add(scrollPane, 0);
            if (isHideLabel(state)) {
                graphComponent.redraw(state);
            }
            currentEditor.revalidate();
            currentEditor.requestFocusInWindow();
            currentEditor.selectAll();

            configureActionMaps();
        }
    }

    @Override
    public void stopEditing(boolean cancel) {
        if (editingCell != null) {
            scrollPane.transferFocusUpCycle();
            ICell cell = editingCell;
            editingCell = null;

            if (!cancel) {
                EventObject trig = trigger;
                trigger = null;
                graphComponent.labelChanged(cell, getCurrentValue(), trig);
            } else {
                CellState state = graphComponent.getGraph().getView().getState(cell);
                graphComponent.redraw(state);
            }

            if (scrollPane.getParent() != null) {
                scrollPane.setVisible(false);
                scrollPane.getParent().remove(scrollPane);
            }

            graphComponent.requestFocusInWindow();
        }
    }

    /**
     * Returns the bounds to be used for the editor.
     */
    public Rectangle getEditorBounds(CellState state, double scale) {
        IGraphModel model = state.getView().getGraph().getModel();
        Rectangle bounds = null;

        if (useLabelBounds(state)) {
            bounds = state.getLabelBounds().getRectangle();
            bounds.height += 10;
        } else {
            bounds = state.getRectangle();
        }

        // Applies the horizontal and vertical label positions
        if (model.isVertex(state.getCell())) {
            HorizontalAlignment horizontalAlignment = state.getStyle().getLabel().getHorizontalAlignmentPosition();
            if (horizontalAlignment == HorizontalAlignment.LEFT) {
                bounds.x -= state.getWidth();
            } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                bounds.x += state.getWidth();
            }
            VerticalAlignment verticalAlignment = state.getStyle().getLabel().getVerticalAlignment();
            if (verticalAlignment == VerticalAlignment.TOP) {
                bounds.y -= state.getHeight();
            } else if (verticalAlignment == VerticalAlignment.BOTTOM) {
                bounds.y += state.getHeight();
            }
        }

        bounds.setSize((int) Math.max(bounds.getWidth(), Math.round(minimumWidth * scale)),
                       (int) Math.max(bounds.getHeight(), Math.round(minimumHeight * scale)));

        return bounds;
    }

    /**
     * Returns true if the label bounds of the state should be used for the
     * editor.
     */
    protected boolean useLabelBounds(CellState state) {
        IGraphModel model = state.getView().getGraph().getModel();
        Geometry geometry = model.getGeometry(state.getCell());

        return ((geometry != null && geometry.getOffset() != null && !geometry.isRelative() && (geometry.getOffset()
                                                                                                        .getX() != 0
                                                                                                || geometry.getOffset()
                                                                                                           .getY()
                                                                                                   != 0))
                || model.isEdge(state.getCell()));
    }

    /**
     * Gets the initial editing value for the given cell.
     */
    protected String getInitialValue(CellState state, EventObject trigger) {
        return graphComponent.getEditingValue(state.getCell(), trigger);
    }

    protected boolean isHideLabel(CellState state) {
        return true;
    }

    /**
     * Installs the keyListener in the textArea and editorPane
     * for handling the enter keystroke and updating the modified state.
     */
    protected void configureActionMaps() {
        InputMap editorInputMap = editorPane.getInputMap();
        InputMap textInputMap = textArea.getInputMap();

        // Adds handling for the escape key to cancel editing
        editorInputMap.put(escapeKeystroke, cancelEditingAction);
        textInputMap.put(escapeKeystroke, cancelEditingAction);

        // Adds handling for shift-enter and redirects enter to stop editing
        if (graphComponent.isEnterStopsCellEditing()) {
            editorInputMap.put(shiftEnterKeystroke, editorEnterActionMapKey);
            textInputMap.put(shiftEnterKeystroke, textEnterActionMapKey);

            editorInputMap.put(enterKeystroke, SUBMIT_TEXT);
            textInputMap.put(enterKeystroke, SUBMIT_TEXT);
        } else {
            editorInputMap.put(enterKeystroke, editorEnterActionMapKey);
            textInputMap.put(enterKeystroke, textEnterActionMapKey);

            if (isShiftEnterSubmitsText()) {
                editorInputMap.put(shiftEnterKeystroke, SUBMIT_TEXT);
                textInputMap.put(shiftEnterKeystroke, SUBMIT_TEXT);
            } else {
                editorInputMap.remove(shiftEnterKeystroke);
                textInputMap.remove(shiftEnterKeystroke);
            }
        }
    }

    /**
     * Returns shiftEnterSubmitsText
     */
    public boolean isShiftEnterSubmitsText() {
        return shiftEnterSubmitsText;
    }

    /**
     * Sets shiftEnterSubmitsText
     */
    public void setShiftEnterSubmitsText(boolean value) {
        shiftEnterSubmitsText = value;
    }

    /**
     * Returns the current editing value.
     */
    public String getCurrentValue() {
        String result;

        if (textArea.getParent() != null) {
            result = textArea.getText();
        } else {
            result = editorPane.getText();

            if (isExtractHtmlBody()) {
                result = Utils.getBodyMarkup(result, isReplaceHtmlLinefeeds());
            }
        }

        return result;
    }

    /**
     * Returns replaceHtmlLinefeeds
     */
    public boolean isExtractHtmlBody() {
        return extractHtmlBody;
    }

    /**
     * Sets extractHtmlBody
     */
    public void setExtractHtmlBody(boolean value) {
        extractHtmlBody = value;
    }

    /**
     * Returns replaceHtmlLinefeeds
     */
    public boolean isReplaceHtmlLinefeeds() {
        return replaceLinefeeds;
    }

    /**
     * Sets replaceHtmlLinefeeds
     */
    public void setReplaceHtmlLinefeeds(boolean value) {
        replaceLinefeeds = value;
    }

    /**
     * @return the minimumEditorScale
     */
    public double getMinimumEditorScale() {
        return minimumEditorScale;
    }

    /**
     * @param minimumEditorScale the minimumEditorScale to set
     */
    public void setMinimumEditorScale(double minimumEditorScale) {
        this.minimumEditorScale = minimumEditorScale;
    }

    /**
     * @return the minimumWidth
     */
    public int getMinimumWidth() {
        return minimumWidth;
    }

    /**
     * @param minimumWidth the minimumWidth to set
     */
    public void setMinimumWidth(int minimumWidth) {
        this.minimumWidth = minimumWidth;
    }

    /**
     * @return the minimumHeight
     */
    public int getMinimumHeight() {
        return minimumHeight;
    }

    /**
     * @param minimumHeight the minimumHeight to set
     */
    public void setMinimumHeight(int minimumHeight) {
        this.minimumHeight = minimumHeight;
    }

    /**
     * Workaround for inserted linefeeds when getting text from HTML editor.
     */
    class NoLinefeedHtmlEditorKit extends HTMLEditorKit {
        @Override
        public void write(Writer out, Document doc, int pos, int len) throws IOException, BadLocationException {
            if (doc instanceof HTMLDocument) {
                NoLinefeedHtmlWriter w = new NoLinefeedHtmlWriter(out, (HTMLDocument) doc, pos, len);

                // the default behavior of write() was to setLineLength(80) which resulted in
                // the inserting or a CR/LF around the 80ith character in any given
                // line. This was not good because if a merge tag was in that range, it would
                // insert CR/LF in between the merge tag and then the replacement of
                // merge tag with bean values was not working.
                w.setLineLength(Integer.MAX_VALUE);
                w.write();
            } else if (doc instanceof StyledDocument) {
                MinimalHTMLWriter w = new MinimalHTMLWriter(out, (StyledDocument) doc, pos, len);
                w.write();
            } else {
                super.write(out, doc, pos, len);
            }
        }
    }

    /**
     * Subclassed to make setLineLength visible for the custom editor kit.
     */
    class NoLinefeedHtmlWriter extends HTMLWriter {
        public NoLinefeedHtmlWriter(Writer buf, HTMLDocument doc, int pos, int len) {
            super(buf, doc, pos, len);
        }

        @Override
        protected void setLineLength(int l) {
            super.setLineLength(l);
        }
    }
}
