package com.faforever.neroxis.debugger;

import com.faforever.neroxis.mask.Mask;
import com.faforever.neroxis.util.MathUtil;
import com.faforever.neroxis.util.vector.Vector2;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputAdapter;

public class EntryPanel extends JPanel {
    private final JLabel titleLabel = new JLabel();
    private final JLabel valueLabel = new JLabel();
    private final ImagePanel imagePanel = new ImagePanel();

    public EntryPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        setupImagePanel();
        setupLabels();
    }

    private void setupImagePanel() {
        add(imagePanel, BorderLayout.CENTER);
    }

    private void setupLabels() {
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(titleLabel, BorderLayout.NORTH);

        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setMinimumSize(new Dimension(100, 25));
        valueLabel.setPreferredSize(new Dimension(100, 25));
        valueLabel.setMaximumSize(new Dimension(100, 25));

        add(valueLabel, BorderLayout.SOUTH);
    }

    public void setMask(Mask<?, ?> mask) {
        imagePanel.setMask(mask);
        repaint();
        setValueLabel();
    }

    private void setValueLabel() {
        if (imagePanel.mask != null) {
            Vector2 maskCoords = imagePanel.getMouseOnMask();
            if (imagePanel.mask.inBounds(maskCoords)) {
                valueLabel.setText(String.format("X: %5.0f, Y: %5.0f Value: %s", maskCoords.getX(), maskCoords.getY(), imagePanel.mask.get(maskCoords).toString()));
            }
        }
    }

    private class ImagePanel extends JPanel {
        private final Vector2 lastMousePosition = new Vector2();
        private final Vector2 fractionalImageOffset = new Vector2();
        private final Vector2 imageZoomFactor = new Vector2();
        private float userZoomLevel = 0;
        private BufferedImage image;
        private Mask<?, ?> mask;

        public ImagePanel() {
            CanvasMouseListener canvasMouseListener = new CanvasMouseListener();
            addMouseListener(canvasMouseListener);
            addMouseMotionListener(canvasMouseListener);
            addMouseWheelListener(canvasMouseListener);
            image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
            paintCheckerboard();
        }

        public void setMask(Mask<?, ?> mask) {
            this.mask = mask;
            if (mask != null) {
                image = new BufferedImage(mask.getSize(), mask.getSize(), BufferedImage.TYPE_INT_RGB);
                paintCheckerboard();

                Graphics g = image.getGraphics();
                g.drawImage(mask.toImage(), 0, 0, this);
            } else {
                image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
            }
        }

        public Vector2 getMouseOnMask() {
            return canvasCoordinatesToMaskCoordinates(lastMousePosition).floor();
        }

        private void paintCheckerboard() {
            for (int x = 0; x < image.getWidth(); ++x) {
                for (int y = 0; y < image.getHeight(); ++y) {
                    if ((x / 4 + y / 4) % 2 == 0) {
                        image.setRGB(x, y, Color.GRAY.getRGB());
                    } else {
                        image.setRGB(x, y, Color.DARK_GRAY.getRGB());
                    }
                }
            }
        }

        private Vector2 getFullScalingVector() {
            return new Vector2().add(getUserScaleFactor()).multiply(imageZoomFactor);
        }

        private float getUserScaleFactor() {
            return (float) StrictMath.pow(2, userZoomLevel);
        }

        private Vector2 canvasCoordinatesToMaskCoordinates(Vector2 canvasCoords) {
            return canvasCoordinatesToFractionalMaskCoordinates(canvasCoords).multiply(mask.getSize());
        }

        private Vector2 canvasCoordinatesToFractionalMaskCoordinates(Vector2 canvasCoords) {
            return canvasCoords.copy().divide(getFullScalingVector().multiply(mask.getSize())).subtract(fractionalImageOffset);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (mask != null) {

                imageZoomFactor.setX((float) getWidth() / image.getWidth());
                imageZoomFactor.setY((float) getHeight() / image.getHeight());
                Vector2 fullScalingVector = getFullScalingVector();
                Vector2 imageOffset = fractionalImageOffset.copy().multiply(mask.getSize());

                AffineTransform at = new AffineTransform();
                at.scale(fullScalingVector.getX(), fullScalingVector.getY());
                at.translate(imageOffset.getX(), imageOffset.getY());
                AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                BufferedImage newImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                op.filter(image, newImage);

                Graphics2D g2d = (Graphics2D) g.create();
                g2d.drawImage(newImage, 0, 0, this);
                g2d.dispose();
            }
        }

        private class CanvasMouseListener extends MouseInputAdapter {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (mask != null) {
                    lastMousePosition.set(e.getPoint());
                    Vector2 oldMousePositionOnMask = canvasCoordinatesToFractionalMaskCoordinates(lastMousePosition);
                    userZoomLevel = (float) StrictMath.min(StrictMath.max(userZoomLevel - e.getWheelRotation() * .25, 0), MathUtil.log2(imagePanel.mask.getSize()));
                    Vector2 newMousePositionOnMask = canvasCoordinatesToFractionalMaskCoordinates(lastMousePosition);
                    fractionalImageOffset.subtract(oldMousePositionOnMask).add(newMousePositionOnMask);
                    boundOffset();
                    repaint();
                }
            }

            private void boundOffset() {
                fractionalImageOffset.min(0).max(1 / getUserScaleFactor() - 1);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mask != null) {
                    Vector2 newMousePosition = new Vector2(e.getPoint());
                    fractionalImageOffset.subtract(lastMousePosition.copy().subtract(newMousePosition).divide(getFullScalingVector().multiply(imagePanel.mask.getSize())));
                    boundOffset();
                    lastMousePosition.set(newMousePosition);
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastMousePosition.set(e.getPoint());
                setValueLabel();
            }
        }
    }
}
