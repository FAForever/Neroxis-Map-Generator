package com.faforever.neroxis.ngraph.swing.handler;

import com.faforever.neroxis.ngraph.event.AfterPaintEvent;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.swing.GraphComponent;
import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Basic example of implementing a handler for rotation. This can be used as follows:
 * <p>
 * new RotationHandler(graphComponent)
 * <p>
 * Note that the Java core does actually not support rotation for the selection handles,
 * perimeter points etc. Feel free to contribute a fix!
 */
public class RotationHandler extends MouseAdapter {
    private static final double PI4 = Math.PI / 4;
    public static ImageIcon ROTATE_ICON;

    /**
     * Loads the collapse and expand icons.
     */
    static {
        ROTATE_ICON = new ImageIcon(RotationHandler.class.getResource("/images/rotate.gif"));
    }

    /**
     * Reference to the enclosing graph component.
     */
    protected GraphComponent graphComponent;
    /**
     * Specifies if this handler is enabled. Default is true.
     */
    protected boolean enabled = true;
    protected JComponent handle;
    protected CellState currentState;
    protected double initialAngle;
    protected double currentAngle;
    protected Point first;

    /**
     * Constructs a new rotation handler.
     */
    public RotationHandler(GraphComponent graphComponent) {
        this.graphComponent = graphComponent;
        graphComponent.addMouseListener(this);
        handle = createHandle();
        // Installs the paint handler
        graphComponent.addListener(AfterPaintEvent.class, (sender, evt) -> {
            paint(evt.getGraphics());
        });
        // Listens to all mouse events on the rendering control
        graphComponent.getGraphControl().addMouseListener(this);
        graphComponent.getGraphControl().addMouseMotionListener(this);
        // Needs to catch events because these are consumed
        handle.addMouseListener(this);
        handle.addMouseMotionListener(this);
    }

    protected JComponent createHandle() {
        JLabel label = new JLabel(ROTATE_ICON);
        label.setSize(ROTATE_ICON.getIconWidth(), ROTATE_ICON.getIconHeight());
        label.setOpaque(false);

        return label;
    }

    public void paint(Graphics g) {
        if (currentState != null && first != null) {
            java.awt.Rectangle rect = currentState.getRectangle();
            double deg = currentAngle * Constants.DEG_PER_RAD;

            if (deg != 0) {
                ((Graphics2D) g).rotate(Math.toRadians(deg), currentState.getCenterX(), currentState.getCenterY());
            }

            Utils.setAntiAlias((Graphics2D) g, true, false);
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    public GraphComponent getGraphComponent() {
        return graphComponent;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (currentState != null && handle.getParent() != null && e.getSource() == handle /* mouse hits handle */) {
            start(e);
            e.consume();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed() && first != null) {
            double deg = 0;
            ICell cell = null;

            if (currentState != null) {
                cell = currentState.getCell();
				/*deg = Utils.getDouble(currentState.getStyle(),
						Constants.STYLE_ROTATION);*/
            }

            deg += currentAngle * Constants.DEG_PER_RAD;
            boolean willExecute = cell != null && first != null;

            // TODO: Call reset before execute in all handlers that
            // offer an execute method
            reset();

            if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed() && willExecute) {
                graphComponent.getGraph().setCellStyles(Constants.STYLE_ROTATION, String.valueOf(deg), List.of(cell));

                graphComponent.getGraphControl().repaint();

                e.consume();
            }
        }

        currentState = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled() && !e.isConsumed() && first != null) {
            RectangleDouble dirty = Utils.getBoundingBox(currentState, currentAngle * Constants.DEG_PER_RAD);
            Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), graphComponent.getGraphControl());

            double cx = currentState.getCenterX();
            double cy = currentState.getCenterY();
            double dx = pt.getX() - cx;
            double dy = pt.getY() - cy;
            double c = Math.sqrt(dx * dx + dy * dy);

            currentAngle = ((pt.getX() > cx) ? -1 : 1) * Math.acos(dy / c) + PI4 + initialAngle;

            dirty.add(Utils.getBoundingBox(currentState, currentAngle * Constants.DEG_PER_RAD));
            dirty.grow(1);

            // TODO: Compute dirty rectangle and repaint
            graphComponent.getGraphControl().repaint(dirty.getRectangle());
            e.consume();
        } else if (handle.getParent() != null) {
            handle.getParent().remove(handle);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (graphComponent.isEnabled() && isEnabled()) {
            if (handle.getParent() != null && e.getSource() == handle /* mouse hits handle */) {
                graphComponent.getGraphControl().setCursor(new Cursor(Cursor.HAND_CURSOR));
                e.consume();
            } else if (currentState == null || !currentState.getRectangle().contains(e.getPoint())) {
                CellState eventState = graphComponent.getGraph()
                                                     .getView()
                                                     .getState(graphComponent.getCellAt(e.getX(), e.getY(), false));

                CellState state = null;

                if (eventState != null && isStateHandled(eventState)) {
                    state = eventState;
                }

                if (currentState != state) {
                    currentState = state;

                    if (currentState == null && handle.getParent() != null) {
                        handle.setVisible(false);
                        handle.getParent().remove(handle);
                    } else if (currentState != null) {
                        if (handle.getParent() == null) {
                            // Adds component for rendering the handles (preview is separate)
                            graphComponent.getGraphControl().add(handle, 0);
                            handle.setVisible(true);
                        }

                        handle.setLocation(
                                (int) (currentState.getX() + currentState.getWidth() - handle.getWidth() - 4),
                                (int) (currentState.getY() + currentState.getHeight() - handle.getWidth() - 4));
                    }
                }
            }
        }
    }

    public void start(MouseEvent e) {
        initialAngle = currentState.getStyle().getShape().getRotation() * Constants.RAD_PER_DEG;
        currentAngle = initialAngle;
        first = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), graphComponent.getGraphControl());

        if (!graphComponent.getGraph().isCellSelected(currentState.getCell())) {
            graphComponent.selectCellForEvent(currentState.getCell(), e);
        }
    }

    public boolean isStateHandled(CellState state) {
        return graphComponent.getGraph().getModel().isVertex(state.getCell());
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        enabled = value;
    }

    public void reset() {
        if (handle.getParent() != null) {
            handle.getParent().remove(handle);
        }
        RectangleDouble dirty = null;

        if (currentState != null && first != null) {
            dirty = Utils.getBoundingBox(currentState, currentAngle * Constants.DEG_PER_RAD);
            dirty.grow(1);
        }

        currentState = null;
        currentAngle = 0;
        first = null;

        if (dirty != null) {
            graphComponent.getGraphControl().repaint(dirty.getRectangle());
        }
    }
}
