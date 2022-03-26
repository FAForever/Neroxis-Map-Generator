package com.faforever.neroxis.ngraph.view;

import com.faforever.neroxis.ngraph.model.ICell;
import com.faforever.neroxis.ngraph.model.IGraphModel;
import com.faforever.neroxis.ngraph.util.Utils;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Multiplicity {

    private static final Logger log = Logger.getLogger(Multiplicity.class.getName());

    /**
     * Defines the type of the source or target terminal. The type is a string
     * passed to Utils.isNode together with the source or target vertex
     * value as the first argument.
     */
    protected String type;

    /**
     * Optional string that specifies the attributename to be passed to
     * Cell.is to check if the rule applies to a cell.
     */
    protected String attr;

    /**
     * Optional string that specifies the value of the attribute to be passed
     * to Cell.is to check if the rule applies to a cell.
     */
    protected String value;

    /**
     * Boolean that specifies if the rule is applied to the source or target
     * terminal of an edge.
     */
    protected boolean source;

    /**
     * Defines the minimum number of connections for which this rule applies.
     * Default is 0.
     */
    protected int min = 0;

    /**
     * Defines the maximum number of connections for which this rule applies.
     * A value of 'n' means unlimited times. Default is 'n'.
     */
    protected String max = "n";

    /**
     * Holds an array of strings that specify the type of neighbor for which
     * this rule applies. The strings are used in Cell.is on the opposite
     * terminal to check if the rule applies to the connection.
     */
    protected Collection<String> validNeighbors;

    /**
     * Boolean indicating if the list of validNeighbors are those that are allowed
     * for this rule or those that are not allowed for this rule.
     */
    protected boolean validNeighborsAllowed = true;

    /**
     * Holds the localized error message to be displayed if the number of
     * connections for which the rule applies is smaller than min or greater
     * than max.
     */
    protected String countError;

    /**
     * Holds the localized error message to be displayed if the type of the
     * neighbor for a connection does not match the rule.
     */
    protected String typeError;

    /**
     *
     */
    public Multiplicity(boolean source, String type, String attr, String value, int min, String max, Collection<String> validNeighbors, String countError, String typeError, boolean validNeighborsAllowed) {
        this.source = source;
        this.type = type;
        this.attr = attr;
        this.value = value;
        this.min = min;
        this.max = max;
        this.validNeighbors = validNeighbors;
        this.countError = countError;
        this.typeError = typeError;
        this.validNeighborsAllowed = validNeighborsAllowed;
    }

    /**
     * Function: check
     * <p>
     * Checks the multiplicity for the given arguments and returns the error
     * for the given connection or null if the multiplicity does not apply.
     * <p>
     * Parameters:
     * <p>
     * graph - Reference to the enclosing graph instance.
     * edge - Cell that represents the edge to validate.
     * source - Cell that represents the source terminal.
     * target - Cell that represents the target terminal.
     * sourceOut - Number of outgoing edges from the source terminal.
     * targetIn - Number of incoming edges for the target terminal.
     */
    public String check(Graph graph, ICell edge, ICell source, ICell target, int sourceOut, int targetIn) {
        StringBuilder error = new StringBuilder();

        if ((this.source && checkTerminal(graph, source, edge)) || (!this.source && checkTerminal(graph, target, edge))) {
            if (!isUnlimited()) {
                int m = getMaxValue();

                if (m == 0 || (this.source && sourceOut >= m) || (!this.source && targetIn >= m)) {
                    error.append(countError).append("\n");
                }
            }

            if (validNeighbors != null && typeError != null && validNeighbors.size() > 0) {
                boolean isValid = checkNeighbors(graph, edge, source, target);

                if (!isValid) {
                    error.append(typeError).append("\n");
                }
            }
        }

        return (error.length() > 0) ? error.toString() : null;
    }

    /**
     * Checks the type of the given value.
     */
    public boolean checkNeighbors(Graph graph, ICell edge, ICell source, ICell target) {
        IGraphModel model = graph.getModel();
        Object sourceValue = model.getValue(source);
        Object targetValue = model.getValue(target);
        boolean isValid = !validNeighborsAllowed;

        for (String tmp : validNeighbors) {
            if (this.source && checkType(graph, targetValue, tmp)) {
                isValid = validNeighborsAllowed;
                break;
            } else if (!this.source && checkType(graph, sourceValue, tmp)) {
                isValid = validNeighborsAllowed;
                break;
            }
        }

        return isValid;
    }

    /**
     * Checks the type of the given value.
     */
    public boolean checkTerminal(Graph graph, ICell terminal, ICell edge) {
        Object userObject = graph.getModel().getValue(terminal);

        return checkType(graph, userObject, type, attr, value);
    }

    /**
     * Checks the type of the given value.
     */
    public boolean checkType(Graph graph, Object value, String type) {
        return checkType(graph, value, type, null, null);
    }

    /**
     * Checks the type of the given value.
     */
    public boolean checkType(Graph graph, Object value, String type, String attr, String attrValue) {
        if (value != null) {
            if (value instanceof Element) {
                return Utils.isNode(value, type, attr, attrValue);
            } else {
                return value.equals(type);
            }
        }

        return false;
    }

    /**
     * Returns true if max is "n" (unlimited).
     */
    public boolean isUnlimited() {
        return max == null || max == "n";
    }

    /**
     * Returns the numeric value of max.
     */
    public int getMaxValue() {
        try {
            return Integer.parseInt(max);
        } catch (NumberFormatException e) {
            log.log(Level.SEVERE, "Failed to parse max value " + max, e);
        }

        return 0;
    }

}
