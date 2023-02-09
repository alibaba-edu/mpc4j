package edu.alibaba.mpc4j.dp.service.heavyhitter;

import edu.alibaba.mpc4j.dp.service.heavyhitter.utils.HhLdpServerContext;

import java.util.Random;

/**
 * Heavy Hitter LDP client.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HhLdpClient {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    HhLdpFactory.HhLdpType getType();

    /**
     * Encodes the item in the warm-up state.
     *
     * @param item the item.
     * @return the encoded item in the warm-up state.
     */
    byte[] warmup(String item);


    /**
     * Randomizes the item based on the current data structure.
     *
     * @param serverContext the server context.
     * @param item          the item.
     * @param random        the random state.
     * @return the randomized item.
     */
    byte[] randomize(HhLdpServerContext serverContext, String item, Random random);

    /**
     * Randomizes the item based on the current data structure.
     *
     * @param serverContext the server context.
     * @param item          the item.
     * @return the randomized item.
     */
    default byte[] randomize(HhLdpServerContext serverContext, String item) {
        return randomize(serverContext, item, new Random());
    }

    /**
     * Returns the privacy parameter ε / w.
     *
     * @return the privacy parameter ε / w.
     */
    double getWindowEpsilon();

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size d.
     */
    int getD();

    /**
     * Gets the number of Heavy Hitters k.
     *
     * @return the number of Heavy Hitters.
     */
    int getK();
}
