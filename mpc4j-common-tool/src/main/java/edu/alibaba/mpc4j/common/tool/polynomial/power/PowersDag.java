package edu.alibaba.mpc4j.common.tool.polynomial.power;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * PowersDag represents a DAG for computing all powers of a given query ciphertext in a depth-optimal manner given a
 * certain "base" (sources) of powers of the query.
 * <p></p>
 * For example, the computation up to power 7 with sources 1, 2, 5 one can represent as the DAG with nodes 1..7 and edges
 * <p>1 --> 3 <-- 2 (q^3 = q^1 * q^2)</p>
 * <p>2 --> 4 <-- 2 (q^4 = q^2 * q^2; repeated edge)</p>
 * <p>1 --> 6 <-- 5 (q^6 = q^1 * q^5)</p>
 * <p>2 --> 7 <-- 5 (q^7 = q^2 * q^5)</p>
 * <p></p>
 * The graph above describes how q^1...q^7 can be computed from q^1, q^2, and q^5 with a depth 1 circuit. A PowersDag is
 * configured from a given set of source powers ({ 1, 2, 5 } in the example above).
 *
 * @author Weiran Liu
 * @date 2024/2/18
 */
public class PowersDag {
    /**
     * depth of the PowersDag
     */
    private final int depth;
    /**
     * PowersNodes containing upperBound Nodes, and the 0-th PowersNode is for power 1, i.e., all are shifted 1 left.
     */
    private final PowersNode[] powersNodes;

    /**
     * Creates a PowersDag.
     *
     * @param sourcePowers source powers.
     * @param upperBound   upper bound, meaning the target powers are in range [1, upperBound].
     */
    public PowersDag(TIntSet sourcePowers, int upperBound) {
        // Source powers cannot contain 0 and must contain 1
        assert !sourcePowers.contains(0) && sourcePowers.contains(1);
        // Target powers cannot contain 0 and must contain 1
        assert upperBound >= 1 : "upper bound must be greater than or equal to 1 : " + upperBound;

        // create target powers
        TIntSet targetPowers = new TIntHashSet(upperBound);
        for (int targetPower = 1; targetPower <= upperBound; targetPower++) {
            targetPowers.add(targetPower);
        }
        // sort source powers
        int[] sortSourcePowers = sourcePowers.toArray();
        Arrays.sort(sortSourcePowers);
        // Source powers must be a subset of target powers
        assert sortSourcePowers[sortSourcePowers.length - 1] <= upperBound
            : "Source powers must be a subset of target powers";

        powersNodes = new PowersNode[upperBound];
        // Insert all source nodes, all positions are shift one left.
        IntStream.range(0, sortSourcePowers.length).forEach(i ->
            powersNodes[sortSourcePowers[i] - 1] = new PowersNode(sortSourcePowers[i], 0)
        );
        // Keep track of the largest encountered depth
        int currDepth = 0;
        // Now compute the non-source powers
        for (int currPower = 1; currPower <= upperBound; currPower++) {
            // Do nothing if this is a source power
            if (powersNodes[currPower - 1] != null) {
                continue;
            }
            // The current power should be written as a sum of two lower powers in a depth-optimal way
            int optimalDepth = currPower - 1;
            int optimalS1 = currPower - 1;
            int optimalS2 = 1;
            // Loop over possible values for the first parent
            for (int s1 = 1; s1 <= targetPowers.size(); s1++) {
                // Only go up to the current target power for the first parent
                if (s1 >= currPower) {
                    break;
                }
                // Second parent is fully determined and must be a target power as well
                int s2 = currPower - s1;
                if (!targetPowers.contains(s2)) {
                    continue;
                }

                // Compute the depth for this choice of parents for the current power
                int depth = Math.max(powersNodes[s1 - 1].getDepth(), powersNodes[s2 - 1].getDepth()) + 1;

                // If we find a better parents, replace the current one.
                if (depth < optimalDepth) {
                    optimalDepth = depth;
                    optimalS1 = s1;
                    optimalS2 = s2;
                }
            }

            // Found an optimal way to obtain the current power from two lower powers. Now add data for the new node.
            powersNodes[currPower - 1] = new PowersNode(currPower, optimalDepth, optimalS1, optimalS2);
            currDepth = Math.max(currDepth, optimalDepth);
        }
        // it must be success since even the source power only contains 1, we can find a way to get all target powers.
        depth = currDepth;
    }

    /**
     * Gets depth of DAG.
     *
     * @return depth.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Gets the upper bound, i.e., target powers from 1 to the upper bound.
     *
     * @return the upper bound.
     */
    public int upperBound() {
        return powersNodes.length;
    }

    /**
     * Gets DAG. The DAG is represented by an 2D-array, each entry in the 2D-array is the parents for the node.
     *
     * @return the DAG.
     */
    public int[][] getDag() {
        return Arrays.stream(powersNodes).map(PowersNode::getParents).toArray(int[][]::new);
    }

    @Override
    public String toString() {
        return null;
    }
}
