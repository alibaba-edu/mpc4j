package edu.alibaba.mpc4j.dp.service.fo;

import java.util.*;

/**
 * Frequency Oracle (FO) LDP server.
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public interface FoLdpServer {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    FoLdpFactory.FoLdpType getType();

    /**
     * Inserts a randomized item.
     *
     * @param itemBytes the randomized item.
     */
    void insert(byte[] itemBytes);

    /**
     * Calculates frequency estimates for all items in the domain.
     *
     * @return the frequency estimates for all items in the domain.
     */
    Map<String, Double> estimate();

    /**
     * Calculates ordered frequency estimates with descending order list.
     *
     * @return ordered frequency estimates.
     */
    default List<Map.Entry<String, Double>> orderedEstimate() {
        Map<String, Double> frequencyEstimates = estimate();
        List<Map.Entry<String, Double>> orderedFrequencyEstimates = new ArrayList<>(frequencyEstimates.entrySet());
        // descending sort
        orderedFrequencyEstimates.sort(Comparator.comparingDouble(Map.Entry::getValue));
        Collections.reverse(orderedFrequencyEstimates);

        return orderedFrequencyEstimates;
    }

    /**
     * Returns the privacy parameter ε.
     *
     * @return the privacy parameter ε.
     */
    double getEpsilon();

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size d.
     */
    int getD();

    /**
     * Returns the total insert item num.
     *
     * @return the total insert item num.
     */
    int getNum();
}
