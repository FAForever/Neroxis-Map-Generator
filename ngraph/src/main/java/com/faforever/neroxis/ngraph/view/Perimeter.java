/**
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Utils;

/**
 * Provides various perimeter functions to be used in a style
 * as the value of Constants.STYLE_PERIMETER. Alternately, the Constants.
 * PERIMETER_* constants can be used to reference a perimeter via the
 * StyleRegistry.
 */
public class Perimeter {

    /**
     * Describes a rectangular perimeter for the given bounds.
     */
    public static PerimeterFunction RectanglePerimeter = new PerimeterFunction() {
        public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            double dx = next.getX() - cx;
            double dy = next.getY() - cy;
            double alpha = Math.atan2(dy, dx);
            PointDouble p = new PointDouble();
            double pi = Math.PI;
            double pi2 = Math.PI / 2;
            double beta = pi2 - alpha;
            double t = Math.atan2(bounds.getHeight(), bounds.getWidth());

            if (alpha < -pi + t || alpha > pi - t) {
                // Left edge
                p.setX(bounds.getX());
                p.setY(cy - bounds.getWidth() * Math.tan(alpha) / 2);
            } else if (alpha < -t) {
                // Top Edge
                p.setY(bounds.getY());
                p.setX(cx - bounds.getHeight() * Math.tan(beta) / 2);
            } else if (alpha < t) {
                // Right Edge
                p.setX(bounds.getX() + bounds.getWidth());
                p.setY(cy + bounds.getWidth() * Math.tan(alpha) / 2);
            } else {
                // Bottom Edge
                p.setY(bounds.getY() + bounds.getHeight());
                p.setX(cx + bounds.getHeight() * Math.tan(beta) / 2);
            }

            if (orthogonal) {
                if (next.getX() >= bounds.getX() && next.getX() <= bounds.getX() + bounds.getWidth()) {
                    p.setX(next.getX());
                } else if (next.getY() >= bounds.getY() && next.getY() <= bounds.getY() + bounds.getHeight()) {
                    p.setY(next.getY());
                }

                if (next.getX() < bounds.getX()) {
                    p.setX(bounds.getX());
                } else if (next.getX() > bounds.getX() + bounds.getWidth()) {
                    p.setX(bounds.getX() + bounds.getWidth());
                }

                if (next.getY() < bounds.getY()) {
                    p.setY(bounds.getY());
                } else if (next.getY() > bounds.getY() + bounds.getHeight()) {
                    p.setY(bounds.getY() + bounds.getHeight());
                }
            }

            return p;
        }

    };
    /**
     * Describes an elliptic perimeter.
     */
    public static PerimeterFunction EllipsePerimeter = new PerimeterFunction() {
        public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
            double x = bounds.getX();
            double y = bounds.getY();
            double a = bounds.getWidth() / 2;
            double b = bounds.getHeight() / 2;
            double cx = x + a;
            double cy = y + b;
            double px = next.getX();
            double py = next.getY();
            // Calculates straight line equation through
            // point and ellipse center y = d * x + h
            double dx = px - cx;
            double dy = py - cy;

            if (dx == 0 && dy != 0) {
                return new PointDouble(cx, cy + b * dy / Math.abs(dy));
            } else if (dx == 0 && dy == 0) {
                return new PointDouble(px, py);
            }

            if (orthogonal) {
                if (py >= y && py <= y + bounds.getHeight()) {
                    double ty = py - cy;
                    double tx = Math.sqrt(a * a * (1 - (ty * ty) / (b * b)));

                    if (Double.isNaN(tx)) {
                        tx = 0;
                    }

                    if (px <= x) {
                        tx = -tx;
                    }
                    return new PointDouble(cx + tx, py);
                }

                if (px >= x && px <= x + bounds.getWidth()) {
                    double tx = px - cx;
                    double ty = Math.sqrt(b * b * (1 - (tx * tx) / (a * a)));

                    if (Double.isNaN(ty)) {
                        ty = 0;
                    }

                    if (py <= y) {
                        ty = -ty;
                    }
                    return new PointDouble(px, cy + ty);
                }
            }

            // Calculates intersection
            double d = dy / dx;
            double h = cy - d * cx;
            double e = a * a * d * d + b * b;
            double f = -2 * cx * e;
            double g = a * a * d * d * cx * cx + b * b * cx * cx - a * a * b * b;
            double det = Math.sqrt(f * f - 4 * e * g);

            // Two solutions (perimeter points)
            double xout1 = (-f + det) / (2 * e);
            double xout2 = (-f - det) / (2 * e);
            double yout1 = d * xout1 + h;
            double yout2 = d * xout2 + h;
            double dist1 = Math.sqrt(Math.pow((xout1 - px), 2) + Math.pow((yout1 - py), 2));
            double dist2 = Math.sqrt(Math.pow((xout2 - px), 2) + Math.pow((yout2 - py), 2));

            // Correct solution
            double xout = 0;
            double yout = 0;

            if (dist1 < dist2) {
                xout = xout1;
                yout = yout1;
            } else {
                xout = xout2;
                yout = yout2;
            }
            return new PointDouble(xout, yout);
        }

    };
    /**
     * Describes a rhombus (aka diamond) perimeter.
     */
    public static PerimeterFunction RhombusPerimeter = new PerimeterFunction() {
        public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
            double x = bounds.getX();
            double y = bounds.getY();
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            double cx = x + w / 2;
            double cy = y + h / 2;
            double px = next.getX();
            double py = next.getY();

            // Special case for intersecting the diamond's corners
            if (cx == px) {
                if (cy > py) {
                    return new PointDouble(cx, y); // top
                } else {
                    return new PointDouble(cx, y + h); // bottom
                }
            } else if (cy == py) {
                if (cx > px) {
                    return new PointDouble(x, cy); // left
                } else {
                    return new PointDouble(x + w, cy); // right
                }
            }

            double tx = cx;
            double ty = cy;

            if (orthogonal) {
                if (px >= x && px <= x + w) {
                    tx = px;
                } else if (py >= y && py <= y + h) {
                    ty = py;
                }
            }

            // In which quadrant will the intersection be?
            // set the slope and offset of the border line accordingly
            if (px < cx) {
                if (py < cy) {
                    return Utils.intersection(px, py, tx, ty, cx, y, x, cy);
                } else {
                    return Utils.intersection(px, py, tx, ty, cx, y + h, x, cy);
                }
            } else if (py < cy) {
                return Utils.intersection(px, py, tx, ty, cx, y, x + w, cy);
            } else {
                return Utils.intersection(px, py, tx, ty, cx, y + h, x + w, cy);
            }
        }

    };
    /**
     * Describes a triangle perimeter. See RectanglePerimeter
     * for a description of the parameters.
     */
    public static PerimeterFunction TrianglePerimeter = new PerimeterFunction() {
        public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
            Object direction = (vertex != null) ? Utils.getString(vertex.style, Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST) : Constants.DIRECTION_EAST;
            boolean vertical = direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH);
            double x = bounds.getX();
            double y = bounds.getY();
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            double cx = x + w / 2;
            double cy = y + h / 2;
            PointDouble start = new PointDouble(x, y);
            PointDouble corner = new PointDouble(x + w, cy);
            PointDouble end = new PointDouble(x, y + h);
            if (direction.equals(Constants.DIRECTION_NORTH)) {
                start = end;
                corner = new PointDouble(cx, y);
                end = new PointDouble(x + w, y + h);
            } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
                corner = new PointDouble(cx, y + h);
                end = new PointDouble(x + w, y);
            } else if (direction.equals(Constants.DIRECTION_WEST)) {
                start = new PointDouble(x + w, y);
                corner = new PointDouble(x, cy);
                end = new PointDouble(x + w, y + h);
            }

            // Compute angle
            double dx = next.getX() - cx;
            double dy = next.getY() - cy;

            double alpha = (vertical) ? Math.atan2(dx, dy) : Math.atan2(dy, dx);
            double t = (vertical) ? Math.atan2(w, h) : Math.atan2(h, w);

            boolean base = false;

            if (direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_WEST)) {
                base = alpha > -t && alpha < t;
            } else {
                base = alpha < -Math.PI + t || alpha > Math.PI - t;
            }
            PointDouble result = null;

            if (base) {
                if (orthogonal && ((vertical && next.getX() >= start.getX() && next.getX() <= end.getX()) || (!vertical && next.getY() >= start.getY() && next.getY() <= end.getY()))) {
                    if (vertical) {
                        result = new PointDouble(next.getX(), start.getY());
                    } else {
                        result = new PointDouble(start.getX(), next.getY());
                    }
                } else {
                    if (direction.equals(Constants.DIRECTION_EAST)) {
                        result = new PointDouble(x, y + h / 2 - w * Math.tan(alpha) / 2);
                    } else if (direction.equals(Constants.DIRECTION_NORTH)) {
                        result = new PointDouble(x + w / 2 + h * Math.tan(alpha) / 2, y + h);
                    } else if (direction.equals(Constants.DIRECTION_SOUTH)) {
                        result = new PointDouble(x + w / 2 - h * Math.tan(alpha) / 2, y);
                    } else if (direction.equals(Constants.DIRECTION_WEST)) {
                        result = new PointDouble(x + w, y + h / 2 + w * Math.tan(alpha) / 2);
                    }
                }
            } else {
                if (orthogonal) {
                    PointDouble pt = new PointDouble(cx, cy);

                    if (next.getY() >= y && next.getY() <= y + h) {
                        pt.setX((vertical) ? cx : ((direction.equals(Constants.DIRECTION_WEST)) ? x + w : x));
                        pt.setY(next.getY());
                    } else if (next.getX() >= x && next.getX() <= x + w) {
                        pt.setX(next.getX());
                        pt.setY((!vertical) ? cy : ((direction.equals(Constants.DIRECTION_NORTH)) ? y + h : y));
                    }

                    // Compute angle
                    dx = next.getX() - pt.getX();
                    dy = next.getY() - pt.getY();

                    cx = pt.getX();
                    cy = pt.getY();
                }

                if ((vertical && next.getX() <= x + w / 2) || (!vertical && next.getY() <= y + h / 2)) {
                    result = Utils.intersection(next.getX(), next.getY(), cx, cy, start.getX(), start.getY(), corner.getX(), corner.getY());
                } else {
                    result = Utils.intersection(next.getX(), next.getY(), cx, cy, corner.getX(), corner.getY(), end.getX(), end.getY());
                }
            }

            if (result == null) {
                result = new PointDouble(cx, cy);
            }

            return result;
        }

    };
    /**
     * Describes a hexagon perimeter. See RectanglePerimeter
     * for a description of the parameters.
     */
    public static PerimeterFunction HexagonPerimeter = new PerimeterFunction() {
        public PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal) {
            double x = bounds.getX();
            double y = bounds.getY();
            double w = bounds.getWidth();
            double h = bounds.getHeight();
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            double px = next.getX();
            double py = next.getY();
            double dx = px - cx;
            double dy = py - cy;
            double alpha = -Math.atan2(dy, dx);
            double pi = Math.PI;
            double pi2 = Math.PI / 2;
            PointDouble result = new PointDouble(cx, cy);
            Object direction = (vertex != null) ? Utils.getString(vertex.style, Constants.STYLE_DIRECTION, Constants.DIRECTION_EAST) : Constants.DIRECTION_EAST;
            boolean vertical = direction.equals(Constants.DIRECTION_NORTH) || direction.equals(Constants.DIRECTION_SOUTH);
            PointDouble a = new PointDouble();
            PointDouble b = new PointDouble();
            //Only consider corrects quadrants for the orthogonal case.
            if ((px < x) && (py < y) || (px < x) && (py > y + h) || (px > x + w) && (py < y) || (px > x + w) && (py > y + h)) {
                orthogonal = false;
            }
            if (orthogonal) {
                if (vertical) {
                    //Special cases where intersects with hexagon corners
                    if (px == cx) {
                        if (py <= y) {
                            return new PointDouble(cx, y);
                        } else if (py >= y + h) {
                            return new PointDouble(cx, y + h);
                        }
                    } else if (px < x) {
                        if (py == y + h / 4) {
                            return new PointDouble(x, y + h / 4);
                        } else if (py == y + 3 * h / 4) {
                            return new PointDouble(x, y + 3 * h / 4);
                        }
                    } else if (px > x + w) {
                        if (py == y + h / 4) {
                            return new PointDouble(x + w, y + h / 4);
                        } else if (py == y + 3 * h / 4) {
                            return new PointDouble(x + w, y + 3 * h / 4);
                        }
                    } else if (px == x) {
                        if (py < cy) {
                            return new PointDouble(x, y + h / 4);
                        } else if (py > cy) {
                            return new PointDouble(x, y + 3 * h / 4);
                        }
                    } else if (px == x + w) {
                        if (py < cy) {
                            return new PointDouble(x + w, y + h / 4);
                        } else if (py > cy) {
                            return new PointDouble(x + w, y + 3 * h / 4);
                        }
                    }
                    if (py == y) {
                        return new PointDouble(cx, y);
                    } else if (py == y + h) {
                        return new PointDouble(cx, y + h);
                    }

                    if (px < cx) {
                        if ((py > y + h / 4) && (py < y + 3 * h / 4)) {
                            a = new PointDouble(x, y);
                            b = new PointDouble(x, y + h);
                        } else if (py < y + h / 4) {
                            a = new PointDouble(x - (int) (0.5 * w), y + (int) (0.5 * h));
                            b = new PointDouble(x + w, y - (int) (0.25 * h));
                        } else if (py > y + 3 * h / 4) {
                            a = new PointDouble(x - (int) (0.5 * w), y + (int) (0.5 * h));
                            b = new PointDouble(x + w, y + (int) (1.25 * h));
                        }
                    } else if (px > cx) {
                        if ((py > y + h / 4) && (py < y + 3 * h / 4)) {
                            a = new PointDouble(x + w, y);
                            b = new PointDouble(x + w, y + h);
                        } else if (py < y + h / 4) {
                            a = new PointDouble(x, y - (int) (0.25 * h));
                            b = new PointDouble(x + (int) (1.5 * w), y + (int) (0.5 * h));
                        } else if (py > y + 3 * h / 4) {
                            a = new PointDouble(x + (int) (1.5 * w), y + (int) (0.5 * h));
                            b = new PointDouble(x, y + (int) (1.25 * h));
                        }
                    }

                } else {
                    //Special cases where intersects with hexagon corners
                    if (py == cy) {
                        if (px <= x) {
                            return new PointDouble(x, y + h / 2);
                        } else if (px >= x + w) {
                            return new PointDouble(x + w, y + h / 2);
                        }
                    } else if (py < y) {
                        if (px == x + w / 4) {
                            return new PointDouble(x + w / 4, y);
                        } else if (px == x + 3 * w / 4) {
                            return new PointDouble(x + 3 * w / 4, y);
                        }
                    } else if (py > y + h) {
                        if (px == x + w / 4) {
                            return new PointDouble(x + w / 4, y + h);
                        } else if (px == x + 3 * w / 4) {
                            return new PointDouble(x + 3 * w / 4, y + h);
                        }
                    } else if (py == y) {
                        if (px < cx) {
                            return new PointDouble(x + w / 4, y);
                        } else if (px > cx) {
                            return new PointDouble(x + 3 * w / 4, y);
                        }
                    } else if (py == y + h) {
                        if (px < cx) {
                            return new PointDouble(x + w / 4, y + h);
                        } else if (py > cy) {
                            return new PointDouble(x + 3 * w / 4, y + h);
                        }
                    }
                    if (px == x) {
                        return new PointDouble(x, cy);
                    } else if (px == x + w) {
                        return new PointDouble(x + w, cy);
                    }

                    if (py < cy) {
                        if ((px > x + w / 4) && (px < x + 3 * w / 4)) {
                            a = new PointDouble(x, y);
                            b = new PointDouble(x + w, y);
                        } else if (px < x + w / 4) {
                            a = new PointDouble(x - (int) (0.25 * w), y + h);
                            b = new PointDouble(x + (int) (0.5 * w), y - (int) (0.5 * h));
                        } else if (px > x + 3 * w / 4) {
                            a = new PointDouble(x + (int) (0.5 * w), y - (int) (0.5 * h));
                            b = new PointDouble(x + (int) (1.25 * w), y + h);
                        }
                    } else if (py > cy) {
                        if ((px > x + w / 4) && (px < x + 3 * w / 4)) {
                            a = new PointDouble(x, y + h);
                            b = new PointDouble(x + w, y + h);
                        } else if (px < x + w / 4) {
                            a = new PointDouble(x - (int) (0.25 * w), y);
                            b = new PointDouble(x + (int) (0.5 * w), y + (int) (1.5 * h));
                        } else if (px > x + 3 * w / 4) {
                            a = new PointDouble(x + (int) (0.5 * w), y + (int) (1.5 * h));
                            b = new PointDouble(x + (int) (1.25 * w), y);
                        }
                    }
                }

                double tx = cx;
                double ty = cy;

                if (px >= x && px <= x + w) {
                    tx = px;
                    if (py < cy) {
                        ty = y + h;
                    } else {
                        ty = y;
                    }
                } else if (py >= y && py <= y + h) {
                    ty = py;
                    if (px < cx) {
                        tx = x + w;
                    } else {
                        tx = x;
                    }
                }

                result = Utils.intersection(tx, ty, next.getX(), next.getY(), a.getX(), a.getY(), b.getX(), b.getY());
            } else {
                if (vertical) {
                    double beta = Math.atan2(h / 4, w / 2);

                    //Special cases where intersects with hexagon corners
                    if (alpha == beta) {
                        return new PointDouble(x + w, y + (int) (0.25 * h));
                    } else if (alpha == pi2) {
                        return new PointDouble(x + (int) (0.5 * w), y);
                    } else if (alpha == (pi - beta)) {
                        return new PointDouble(x, y + (int) (0.25 * h));
                    } else if (alpha == -beta) {
                        return new PointDouble(x + w, y + (int) (0.75 * h));
                    } else if (alpha == (-pi2)) {
                        return new PointDouble(x + (int) (0.5 * w), y + h);
                    } else if (alpha == (-pi + beta)) {
                        return new PointDouble(x, y + (int) (0.75 * h));
                    }

                    if ((alpha < beta) && (alpha > -beta)) {
                        a = new PointDouble(x + w, y);
                        b = new PointDouble(x + w, y + h);
                    } else if ((alpha > beta) && (alpha < pi2)) {
                        a = new PointDouble(x, y - (int) (0.25 * h));
                        b = new PointDouble(x + (int) (1.5 * w), y + (int) (0.5 * h));
                    } else if ((alpha > pi2) && (alpha < (pi - beta))) {
                        a = new PointDouble(x - (int) (0.5 * w), y + (int) (0.5 * h));
                        b = new PointDouble(x + w, y - (int) (0.25 * h));
                    } else if (((alpha > (pi - beta)) && (alpha <= pi)) || ((alpha < (-pi + beta)) && (alpha >= -pi))) {
                        a = new PointDouble(x, y);
                        b = new PointDouble(x, y + h);
                    } else if ((alpha < -beta) && (alpha > -pi2)) {
                        a = new PointDouble(x + (int) (1.5 * w), y + (int) (0.5 * h));
                        b = new PointDouble(x, y + (int) (1.25 * h));
                    } else if ((alpha < -pi2) && (alpha > (-pi + beta))) {
                        a = new PointDouble(x - (int) (0.5 * w), y + (int) (0.5 * h));
                        b = new PointDouble(x + w, y + (int) (1.25 * h));
                    }
                } else {
                    double beta = Math.atan2(h / 2, w / 4);

                    //Special cases where intersects with hexagon corners
                    if (alpha == beta) {
                        return new PointDouble(x + (int) (0.75 * w), y);
                    } else if (alpha == (pi - beta)) {
                        return new PointDouble(x + (int) (0.25 * w), y);
                    } else if ((alpha == pi) || (alpha == -pi)) {
                        return new PointDouble(x, y + (int) (0.5 * h));
                    } else if (alpha == 0) {
                        return new PointDouble(x + w, y + (int) (0.5 * h));
                    } else if (alpha == -beta) {
                        return new PointDouble(x + (int) (0.75 * w), y + h);
                    } else if (alpha == (-pi + beta)) {
                        return new PointDouble(x + (int) (0.25 * w), y + h);
                    }

                    if ((alpha > 0) && (alpha < beta)) {
                        a = new PointDouble(x + (int) (0.5 * w), y - (int) (0.5 * h));
                        b = new PointDouble(x + (int) (1.25 * w), y + h);
                    } else if ((alpha > beta) && (alpha < (pi - beta))) {
                        a = new PointDouble(x, y);
                        b = new PointDouble(x + w, y);
                    } else if ((alpha > (pi - beta)) && (alpha < pi)) {
                        a = new PointDouble(x - (int) (0.25 * w), y + h);
                        b = new PointDouble(x + (int) (0.5 * w), y - (int) (0.5 * h));
                    } else if ((alpha < 0) && (alpha > -beta)) {
                        a = new PointDouble(x + (int) (0.5 * w), y + (int) (1.5 * h));
                        b = new PointDouble(x + (int) (1.25 * w), y);
                    } else if ((alpha < -beta) && (alpha > (-pi + beta))) {
                        a = new PointDouble(x, y + h);
                        b = new PointDouble(x + w, y + h);
                    } else if ((alpha < (-pi + beta)) && (alpha > -pi)) {
                        a = new PointDouble(x - (int) (0.25 * w), y);
                        b = new PointDouble(x + (int) (0.5 * w), y + (int) (1.5 * h));
                    }
                }

                result = Utils.intersection(cx, cy, next.getX(), next.getY(), a.getX(), a.getY(), b.getX(), b.getY());
            }
            if (result == null) {
                return new PointDouble(cx, cy);
            }
            return result;
        }
    };

    /**
     * Defines the requirements for a perimeter function.
     */
    public interface PerimeterFunction {

        /**
         * Implements a perimeter function.
         *
         * @param bounds     Rectangle that represents the absolute bounds of the
         *                   vertex.
         * @param vertex     Cell state that represents the vertex.
         * @param next       Point that represents the nearest neighbour point on the
         *                   given edge.
         * @param orthogonal Boolean that specifies if the orthogonal projection onto
         *                   the perimeter should be returned. If this is false then the intersection
         *                   of the perimeter and the line between the next and the center point is
         *                   returned.
         * @return Returns the perimeter point.
         */
        PointDouble apply(RectangleDouble bounds, CellState vertex, PointDouble next, boolean orthogonal);

    }
}
