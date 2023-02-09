package edu.alibaba.mpc4j.dp.service.fo;

import java.util.Random;

/**
 * Frequency Oracle (FO) LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/10
 */
public interface FoLdpClient {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    FoLdpFactory.FoLdpType getType();

    /**
     * Randomizes an item.
     *
     * @param item an item.
     * @param random the random state.
     * @return the randomized item.
     */
    byte[] randomize(String item, Random random);

    /**
     * Return the privacy parameter ε.
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
}
