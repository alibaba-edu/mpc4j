package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory;

import java.util.Set;

/**
 * Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HhLdpConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    HhLdpFactory.HhLdpType getType();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    default String getName() {
        return getType().name();
    }

    /**
     * Gets the domain set.
     *
     * @return the domain set.
     */
    Set<String> getDomainSet();

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

    /**
     * Gets the privacy parameter ε / w.
     *
     * @return the privacy parameter ε / w.
     */
    double getWindowEpsilon();

    /**
     * Returns whether the mechanism obtain accurate estimation for extremely large ε.
     *
     * @return whether the mechanism obtain accurate estimation for extremely large ε.
     */
    boolean isConverge();
}
