/**
 * Copyright (c) 2010-2012, JGraph Ltd
 */
package com.faforever.neroxis.ngraph.io;

import com.faforever.neroxis.ngraph.model.GraphModel;
import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.view.Graph;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses a GD .txt file and imports it in the given graph.<br/>
 * This class depends from the classes contained in
 * com.faforever.neroxis.ngraph.io.gd.
 */
public class GdCodec {
    private static final Logger log = Logger.getLogger(GdCodec.class.getName());
    /**
     * Map with the vertex cells added in the addNode method.
     */
    protected static HashMap<String, ICell> cellsMap = new HashMap<>();

    /**
     * Parses simple GD format and populate the specified graph
     *
     * @param input GD file to be parsed
     * @param graph Graph where the parsed graph is included.
     */
    public static void decode(String input, Graph graph) {
        BufferedReader br = new BufferedReader(new StringReader(input));
        GDParseState state = GDParseState.START;
        ICell parent = graph.getDefaultParent();

        graph.getModel().beginUpdate();

        try {
            String line = br.readLine().trim();
            while (line != null) {
                switch (state) {
                    case START: {
                        if (!line.startsWith("#")) {
                            state = GDParseState.NUM_NODES;
                        } else {
                            break;
                        }
                    }
                    case NUM_NODES: {
                        if (!line.startsWith("#")) {
                            int numVertices = Integer.valueOf(line);

                            for (int i = 0; i < numVertices; i++) {
                                String label = String.valueOf(i);
                                ICell vertex = graph.insertVertex(parent, label, label, 0, 0, 10, 10);

                                cellsMap.put(label, vertex);
                            }
                        } else {
                            state = GDParseState.PARSING_EDGES;
                        }

                        break;
                    }
                    case PARSING_NODES: {
                        if (line.startsWith("# Edges")) {
                            state = GDParseState.PARSING_EDGES;
                        } else if (!line.equals("")) {
                            String[] items = line.split(",");
                            if (items.length != 5) {
                                throw new Exception("Error in parsing");
                            } else {
                                double x = Double.valueOf(items[1]);
                                double y = Double.valueOf(items[2]);
                                double width = Double.valueOf(items[3]);
                                double height = Double.valueOf(items[4]);


                                //Set the node name as label.
                                String label = items[0];

                                //Insert a new vertex in the graph
                                ICell vertex = graph.insertVertex(parent, label, label, x - width / 2.0, y - height / 2.0, width, height);

                                cellsMap.put(label, vertex);
                            }
                        }
                        break;
                    }
                    case PARSING_EDGES: {
                        if (!line.equals("")) {
                            String[] items = line.split(" ");
                            if (items.length != 2) {
                                throw new Exception("Error in parsing");
                            } else {
                                ICell source = cellsMap.get(items[0]);
                                ICell target = cellsMap.get(items[1]);

                                graph.insertEdge(parent, null, "", source, target);
                            }
                        }
                        break;
                    }
                }

                line = br.readLine();
            }
        } catch (Exception e) {
            log.log(Level.FINEST, "Failed to decode", e);
        } finally {
            graph.getModel().endUpdate();
        }
    }

    /**
     * Generates a GD text output with the cells in the graph.
     * The implementation only uses the cells located in the default parent.
     *
     * @param graph Graph with the cells.
     * @return The GD document generated.
     */
    public static String encode(Graph graph) {
        StringBuilder builder = new StringBuilder();

        ICell parent = graph.getDefaultParent();
        List<ICell> vertices = GraphModel.getChildCells(graph.getModel(), parent, true, false);

        builder.append("# Number of Nodes (0-").append(vertices.size() - 1).append(")");
        builder.append(vertices.size());

        // TODO

        return builder.toString();
    }

    /**
     * Represents the different states in the parse of a file.
     */
    public enum GDParseState {
        START, NUM_NODES, PARSING_NODES, PARSING_EDGES
    }
}
