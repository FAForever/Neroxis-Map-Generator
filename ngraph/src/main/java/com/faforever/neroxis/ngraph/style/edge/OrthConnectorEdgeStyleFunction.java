package com.faforever.neroxis.ngraph.style.edge;

import com.faforever.neroxis.ngraph.util.Constants;
import com.faforever.neroxis.ngraph.util.PointDouble;
import com.faforever.neroxis.ngraph.util.Utils;
import com.faforever.neroxis.ngraph.view.CellState;
import com.faforever.neroxis.ngraph.view.Graph;
import java.util.List;

/**
 * An orthogonal connector that avoids connecting vertices and
 * respects port constraints
 */
public class OrthConnectorEdgeStyleFunction implements EdgeStyleFunction {
    private static final SegmentConnectorEdgeStyleFunction SEGMENT_CONNECTOR_EDGE_STYLE = new SegmentConnectorEdgeStyleFunction();
    private static final double ORTH_BUFFER = 10;
    private static final double[][] DIR_VECTORS = new double[][]{{-1, 0}, {0, -1}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 0}};
    private static final double[][] WAY_POINTS_1 = new double[128][2];
    /**
     * The default routing patterns for orthogonal connections
     */
    private static final int[][][] ROUTE_PATTERNS = new int[][][]{{{513, 2308, 2081, 2562}, {513, 1090, 514, 2184, 2114, 2561}, {513, 1090, 514, 2564, 2184, 2562}, {513, 2308, 2561, 1090, 514, 2568, 2308}}, {{514, 1057, 513, 2308, 2081, 2562}, {514, 2184, 2114, 2561}, {514, 2184, 2562, 1057, 513, 2564, 2184}, {514, 1057, 513, 2568, 2308, 2561}}, {{1090, 514, 1057, 513, 2308, 2081, 2562}, {2114, 2561}, {1090, 2562, 1057, 513, 2564, 2184}, {1090, 514, 1057, 513, 2308, 2561, 2568}}, {{2081, 2562}, {1057, 513, 1090, 514, 2184, 2114, 2561}, {1057, 513, 1090, 514, 2184, 2562, 2564}, {1057, 2561, 1090, 514, 2568, 2308}}};
    /**
     * Overriden routing patterns for orthogonal connections
     * where the vertices have
     */
    private static final int[][][] INLINE_ROUTE_PATTERNS = new int[][][]{{null, {2114, 2568}, null, null}, {null, {514, 2081, 2114, 2568}, null, null}, {null, {2114, 2561}, null, null}, {{2081, 2562}, {1057, 2114, 2568}, {2184, 2562}, null}};
    private static final double[] VERTEX_SEPERATIONS = new double[5];
    private static final double[][] LIMITS = new double[2][9];
    private static final int LEFT_MASK = 32;
    private static final int TOP_MASK = 64;
    private static final int RIGHT_MASK = 128;
    private static final int BOTTOM_MASK = 256;
    private static final int LEFT = 1;
    private static final int TOP = 2;
    private static final int RIGHT = 4;
    private static final int BOTTOM = 8;
    private static final int SIDE_MASK = LEFT_MASK | TOP_MASK | RIGHT_MASK | BOTTOM_MASK;
    private static final int CENTER_MASK = 512;
    private static final int SOURCE_MASK = 1024;
    private static final int TARGET_MASK = 2048;

    @Override
    public void apply(CellState state, CellState source, CellState target, List<PointDouble> points, List<PointDouble> result) {
        Graph graph = state.getView().getGraph();
        boolean sourceEdge = source != null && graph.getModel().isEdge(source.getCell());
        boolean targetEdge = target != null && graph.getModel().isEdge(target.getCell());
        if ((points != null && points.size() > 0) || (sourceEdge) || (targetEdge)) {
            SEGMENT_CONNECTOR_EDGE_STYLE.apply(state, source, target, points, result);
            return;
        }
        if (source != null && target != null) {
            double scaledOrthBuffer = ORTH_BUFFER * state.getView().getScale();
            // Determine the side(s) of the source and target vertices
            // that the edge may connect to
            // portConstraint -> [source, target];
            int[] portConstraint = new int[2];
            portConstraint[0] = Utils.getPortConstraints(source, state, true);
            portConstraint[1] = Utils.getPortConstraints(target, state, false);
            // dir -> [source, target] initial direction leaving vertices
            int[] dir = new int[2];
            // Work out which faces of the vertices present against each other
            // in a way that would allow a 3-segment connection if port constraints
            // permitted.
            // geo -> [source, target] [x, y, width, height]
            double[][] geo = new double[2][4];
            geo[0][0] = source.getX();
            geo[0][1] = source.getY();
            geo[0][2] = source.getWidth();
            geo[0][3] = source.getHeight();
            geo[1][0] = target.getX();
            geo[1][1] = target.getY();
            geo[1][2] = target.getWidth();
            geo[1][3] = target.getHeight();
            for (int i = 0; i < 2; i++) {
                LIMITS[i][1] = geo[i][0] - scaledOrthBuffer;
                LIMITS[i][2] = geo[i][1] - scaledOrthBuffer;
                LIMITS[i][4] = geo[i][0] + geo[i][2] + scaledOrthBuffer;
                LIMITS[i][8] = geo[i][1] + geo[i][3] + scaledOrthBuffer;
            }
            // Work out which quad the target is in
            double sourceCenX = geo[0][0] + geo[0][2] / 2.0;
            double sourceCenY = geo[0][1] + geo[0][3] / 2.0;
            double targetCenX = geo[1][0] + geo[1][2] / 2.0;
            double targetCenY = geo[1][1] + geo[1][3] / 2.0;
            double dx = sourceCenX - targetCenX;
            double dy = sourceCenY - targetCenY;
            int quad = 0;
            if (dx < 0) {
                if (dy < 0) {
                    quad = 2;
                } else {
                    quad = 1;
                }
            } else {
                if (dy <= 0) {
                    quad = 3;
                    // Special case on x = 0 and negative y
                    if (dx == 0) {
                        quad = 2;
                    }
                }
            }
            // Check for connection constraints
            PointDouble p0 = state.getAbsolutePoint(0);
            PointDouble pe = state.getAbsolutePoint(state.getAbsolutePointCount() - 1);
            PointDouble currentTerm = p0;
            // constraint[source, target] [x, y]
            double[][] constraint = new double[][]{{0.5, 0.5}, {0.5, 0.5}};
            for (int i = 0; i < 2; i++) {
                if (currentTerm != null) {
                    constraint[i][0] = (currentTerm.getX() - geo[i][0]) / geo[i][2];
                    if (constraint[i][0] < 0.01) {
                        dir[i] = Constants.DIRECTION_MASK_WEST;
                    } else if (constraint[i][0] > 0.99) {
                        dir[i] = Constants.DIRECTION_MASK_EAST;
                    }
                    constraint[i][1] = (currentTerm.getY() - geo[i][1]) / geo[i][3];
                    if (constraint[i][1] < 0.01) {
                        dir[i] = Constants.DIRECTION_MASK_NORTH;
                    } else if (constraint[i][1] > 0.99) {
                        dir[i] = Constants.DIRECTION_MASK_SOUTH;
                    }
                }
                currentTerm = pe;
            }
            double sourceTopDist = geo[0][1] - (geo[1][1] + geo[1][3]);
            double sourceLeftDist = geo[0][0] - (geo[1][0] + geo[1][2]);
            double sourceBottomDist = geo[1][1] - (geo[0][1] + geo[0][3]);
            double sourceRightDist = geo[1][0] - (geo[0][0] + geo[0][2]);
            VERTEX_SEPERATIONS[1] = Math.max(sourceLeftDist - 2 * scaledOrthBuffer, 0);
            VERTEX_SEPERATIONS[2] = Math.max(sourceTopDist - 2 * scaledOrthBuffer, 0);
            VERTEX_SEPERATIONS[4] = Math.max(sourceBottomDist - 2 * scaledOrthBuffer, 0);
            VERTEX_SEPERATIONS[3] = Math.max(sourceRightDist - 2 * scaledOrthBuffer, 0);
            //==============================================================
            // Start of source and target direction determination
            // Work through the preferred orientations by relative positioning
            // of the vertices and list them in preferred and available order
            int[] dirPref = new int[2];
            int[] horPref = new int[2];
            int[] vertPref = new int[2];
            horPref[0] = sourceLeftDist >= sourceRightDist ? Constants.DIRECTION_MASK_WEST : Constants.DIRECTION_MASK_EAST;
            vertPref[0] = sourceTopDist >= sourceBottomDist ? Constants.DIRECTION_MASK_NORTH : Constants.DIRECTION_MASK_SOUTH;
            horPref[1] = Utils.reversePortConstraints(horPref[0]);
            vertPref[1] = Utils.reversePortConstraints(vertPref[0]);
            double preferredHorizDist = Math.max(sourceLeftDist, sourceRightDist);
            double preferredVertDist = Math.max(sourceTopDist, sourceBottomDist);
            int[][] prefOrdering = new int[2][2];
            boolean preferredOrderSet = false;
            // If the preferred port isn't available, switch it
            for (int i = 0; i < 2; i++) {
                if (dir[i] != 0x0) {
                    continue;
                }
                if ((horPref[i] & portConstraint[i]) == 0) {
                    horPref[i] = Utils.reversePortConstraints(horPref[i]);
                }
                if ((vertPref[i] & portConstraint[i]) == 0) {
                    vertPref[i] = Utils.reversePortConstraints(vertPref[i]);
                }
                prefOrdering[i][0] = vertPref[i];
                prefOrdering[i][1] = horPref[i];
            }
            if (preferredVertDist > scaledOrthBuffer * 2 && preferredHorizDist > scaledOrthBuffer * 2) {
                // Possibility of two segment edge connection
                if (((horPref[0] & portConstraint[0]) > 0) && ((vertPref[1] & portConstraint[1]) > 0)) {
                    prefOrdering[0][0] = horPref[0];
                    prefOrdering[0][1] = vertPref[0];
                    prefOrdering[1][0] = vertPref[1];
                    prefOrdering[1][1] = horPref[1];
                    preferredOrderSet = true;
                } else if (((vertPref[0] & portConstraint[0]) > 0) && ((horPref[1] & portConstraint[1]) > 0)) {
                    prefOrdering[0][0] = vertPref[0];
                    prefOrdering[0][1] = horPref[0];
                    prefOrdering[1][0] = horPref[1];
                    prefOrdering[1][1] = vertPref[1];
                    preferredOrderSet = true;
                }
            }
            if (preferredVertDist > scaledOrthBuffer * 2 && !preferredOrderSet) {
                prefOrdering[0][0] = vertPref[0];
                prefOrdering[0][1] = horPref[0];
                prefOrdering[1][0] = vertPref[1];
                prefOrdering[1][1] = horPref[1];
                preferredOrderSet = true;
            }
            if (preferredHorizDist > scaledOrthBuffer * 2 && !preferredOrderSet) {
                prefOrdering[0][0] = horPref[0];
                prefOrdering[0][1] = vertPref[0];
                prefOrdering[1][0] = horPref[1];
                prefOrdering[1][1] = vertPref[1];
                preferredOrderSet = true;
            }
            // The source and target prefs are now an ordered list of
            // the preferred port selections
            // It the list can contain gaps, compact it
            for (int i = 0; i < 2; i++) {
                if (dir[i] != 0x0) {
                    continue;
                }
                if ((prefOrdering[i][0] & portConstraint[i]) == 0) {
                    prefOrdering[i][0] = prefOrdering[i][1];
                }
                dirPref[i] = prefOrdering[i][0] & portConstraint[i];
                dirPref[i] |= (prefOrdering[i][1] & portConstraint[i]) << 8;
                dirPref[i] |= (prefOrdering[1 - i][i] & portConstraint[i]) << 16;
                dirPref[i] |= (prefOrdering[1 - i][1 - i] & portConstraint[i]) << 24;
                if ((dirPref[i] & 0xF) == 0) {
                    dirPref[i] = dirPref[i] << 8;
                }
                if ((dirPref[i] & 0xF00) == 0) {
                    dirPref[i] = (dirPref[i] & 0xF) | dirPref[i] >> 8;
                }
                if ((dirPref[i] & 0xF0000) == 0) {
                    dirPref[i] = (dirPref[i] & 0xFFFF) | ((dirPref[i] & 0xF000000) >> 8);
                }
                dir[i] = dirPref[i] & 0xF;
                if (portConstraint[i] == Constants.DIRECTION_MASK_WEST || portConstraint[i] == Constants.DIRECTION_MASK_NORTH || portConstraint[i] == Constants.DIRECTION_MASK_EAST || portConstraint[i] == Constants.DIRECTION_MASK_SOUTH) {
                    dir[i] = portConstraint[i];
                }
            }
            //==============================================================
            // End of source and target direction determination
            int[] routePattern = getRoutePattern(dir, quad, dx, dy);
            WAY_POINTS_1[0][0] = geo[0][0];
            WAY_POINTS_1[0][1] = geo[0][1];
            switch (dir[0]) {
                case Constants.DIRECTION_MASK_WEST -> {
                    WAY_POINTS_1[0][0] -= scaledOrthBuffer;
                    WAY_POINTS_1[0][1] += constraint[0][1] * geo[0][3];
                }
                case Constants.DIRECTION_MASK_SOUTH -> {
                    WAY_POINTS_1[0][0] += constraint[0][0] * geo[0][2];
                    WAY_POINTS_1[0][1] += geo[0][3] + scaledOrthBuffer;
                }
                case Constants.DIRECTION_MASK_EAST -> {
                    WAY_POINTS_1[0][0] += geo[0][2] + scaledOrthBuffer;
                    WAY_POINTS_1[0][1] += constraint[0][1] * geo[0][3];
                }
                case Constants.DIRECTION_MASK_NORTH -> {
                    WAY_POINTS_1[0][0] += constraint[0][0] * geo[0][2];
                    WAY_POINTS_1[0][1] -= scaledOrthBuffer;
                }
            }
            int currentIndex = 0;
            int lastOrientation = (dir[0] & (Constants.DIRECTION_MASK_EAST | Constants.DIRECTION_MASK_WEST)) > 0 ? 0 : 1;
            int currentOrientation;
            for (int j : routePattern) {
                int nextDirection = j & 0xF;
                // Rotate the index of this direction by the quad
                // to get the real direction
                int directionIndex = nextDirection == Constants.DIRECTION_MASK_EAST ? 3 : nextDirection;
                directionIndex += quad;
                if (directionIndex > 4) {
                    directionIndex -= 4;
                }
                double[] direction = DIR_VECTORS[directionIndex - 1];
                currentOrientation = (directionIndex % 2 > 0) ? 0 : 1;
                // Only update the current index if the point moved
                // in the direction of the current segment move,
                // otherwise the same point is moved until there is
                // a segment direction change
                if (currentOrientation != lastOrientation) {
                    currentIndex++;
                    // Copy the previous way point into the new one
                    // We can't base the new position on index - 1
                    // because sometime elbows turn out not to exist,
                    // then we'd have to rewind.
                    WAY_POINTS_1[currentIndex][0] = WAY_POINTS_1[currentIndex - 1][0];
                    WAY_POINTS_1[currentIndex][1] = WAY_POINTS_1[currentIndex - 1][1];
                }
                boolean tar = (j & TARGET_MASK) > 0;
                boolean sou = (j & SOURCE_MASK) > 0;
                int side = (j & SIDE_MASK) >> 5;
                side = side << quad;
                if (side > 0xF) {
                    side = side >> 4;
                }
                boolean center = (j & CENTER_MASK) > 0;
                if ((sou || tar) && side < 9) {
                    double limit;
                    int souTar = sou ? 0 : 1;
                    if (center && currentOrientation == 0) {
                        limit = geo[souTar][0] + constraint[souTar][0] * geo[souTar][2];
                    } else if (center) {
                        limit = geo[souTar][1] + constraint[souTar][1] * geo[souTar][3];
                    } else {
                        limit = LIMITS[souTar][side];
                    }
                    if (currentOrientation == 0) {
                        double lastX = WAY_POINTS_1[currentIndex][0];
                        double deltaX = (limit - lastX) * direction[0];
                        if (deltaX > 0) {
                            WAY_POINTS_1[currentIndex][0] += direction[0] * deltaX;
                        }
                    } else {
                        double lastY = WAY_POINTS_1[currentIndex][1];
                        double deltaY = (limit - lastY) * direction[1];
                        if (deltaY > 0) {
                            WAY_POINTS_1[currentIndex][1] += direction[1] * deltaY;
                        }
                    }
                } else if (center) {
                    // Which center we're travelling to depend on the current direction
                    WAY_POINTS_1[currentIndex][0] += direction[0] * Math.abs(VERTEX_SEPERATIONS[directionIndex] / 2);
                    WAY_POINTS_1[currentIndex][1] += direction[1] * Math.abs(VERTEX_SEPERATIONS[directionIndex] / 2);
                }
                if (currentIndex > 0 && WAY_POINTS_1[currentIndex][currentOrientation] == WAY_POINTS_1[currentIndex - 1][currentOrientation]) {
                    currentIndex--;
                } else {
                    lastOrientation = currentOrientation;
                }
            }
            for (int i = 0; i <= currentIndex; i++) {
                result.add(new PointDouble(WAY_POINTS_1[i][0], WAY_POINTS_1[i][1]));
            }
        }
    }

    /**
     * Hook method to return the routing pattern for the given state
     */
    private int[] getRoutePattern(int[] dir, double quad, double dx, double dy) {
        int sourceIndex = dir[0] == Constants.DIRECTION_MASK_EAST ? 3 : dir[0];
        int targetIndex = dir[1] == Constants.DIRECTION_MASK_EAST ? 3 : dir[1];
        sourceIndex -= quad;
        targetIndex -= quad;
        if (sourceIndex < 1) {
            sourceIndex += 4;
        }
        if (targetIndex < 1) {
            targetIndex += 4;
        }
        int[] result = ROUTE_PATTERNS[sourceIndex - 1][targetIndex - 1];
        if (dx == 0 || dy == 0) {
            if (INLINE_ROUTE_PATTERNS[sourceIndex - 1][targetIndex - 1] != null) {
                result = INLINE_ROUTE_PATTERNS[sourceIndex - 1][targetIndex - 1];
            }
        }
        return result;
    }
}
