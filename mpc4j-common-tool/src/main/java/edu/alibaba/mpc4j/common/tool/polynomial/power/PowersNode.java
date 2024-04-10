package edu.alibaba.mpc4j.common.tool.polynomial.power;

/**
 * PowersNode represents an individual node in the PowersDag. The node holds the power it represents, and depth in the
 * DAG. Source nodes (i.e., powers of a query that are given directly and do not need to be computed) have depth zero.
 * The node also holds the powers of its parents; parent values both 0 denotes that this is a source node. If only one
 * of the parent values is zero, this node is invalid and the PowersDag is in an invalid state. For the DAG to be in a
 * valid state, for each non-source node, the sum of the powers of the parent nodes of a given must equal the power of
 * that node.
 * <p></p>
 * The implementation comes from the following source code:
 * <p>https://github.com/microsoft/APSI/blob/main/common/apsi/powers.h</p>
 *
 * @author Liqiang Peng
 * @date 2022/8/3
 */
public class PowersNode {
    /**
     * The power represented by this node. In a valid PowersDag this can never be zero.
     */
    private final int power;
    /**
     * The depth of this node in the DAG.
     */
    private final int depth;
    /**
     * Holds the powers of the two parents of this node. Both values must be either zero indicating that this is a
     * source node, or non-zero.
     */
    private final int[] parents;

    /**
     * Creates a new PowerNode. The powers of two parents are both set to zero.
     *
     * @param power the power.
     * @param depth the depth.
     */
    public PowersNode(int power, int depth) {
        this.power = power;
        this.depth = depth;
        parents = new int[] {0, 0};
    }

    /**
     * Creates a new PowerNode.
     *
     * @param power      the power.
     * @param depth      the depth.
     * @param leftPower  the power of the left parent.
     * @param rightPower the power of the right parent.
     */
    public PowersNode(int power, int depth, int leftPower, int rightPower) {
        this.power = power;
        this.depth = depth;
        parents = new int[] {leftPower, rightPower};
    }

    /**
     * Gets the depth.
     *
     * @return the depth.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Gets the power.
     *
     * @return the power.
     */
    public int getPower() {
        return power;
    }

    /**
     * Gets the powers of the two parents.
     *
     * @return the powers of the two parents.
     */
    public int[] getParents() {
        return parents;
    }
}
