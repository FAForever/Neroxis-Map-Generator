package com.faforever.neroxis.ngraph.style.perimeter;

import com.faforever.neroxis.ngraph.style.Direction;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.RectangleDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;

/**
 * Describes a hexagon perimeter. See RectanglePerimeter
 * for a description of the parameters.
 */
public class HexagonPerimeter implements Perimeter {
    @Override
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
        PointDouble result;
        Direction direction = (vertex != null) ? vertex.getStyle().getShape().getDirection() : Direction.EAST;
        boolean vertical = direction == Direction.NORTH || direction == Direction.SOUTH;
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
}
