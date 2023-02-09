package edu.alibaba.mpc4j.dp.service.fo.config;

import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;
import edu.alibaba.mpc4j.dp.service.tool.Domain;

import java.util.Set;

/**
 * Frequency Oracle LDP client config.
 *
 * @author Weiran Liu
 * @date 2023/1/14
 */
public interface FoLdpConfig {
    /**
     * Gets the type.
     *
     * @return the type.
     */
    FoLdpFactory.FoLdpType getType();

    /**
     * Gets the name.
     *
     * @return the name.
     */
    default String getName() {
        return getType().name();
    }

    /**
     * Gets the domain.
     *
     * @return the domain.
     */
    Domain getDomain();

    /**
     * Gets the domain set.
     *
     * @return the domain set.
     */
    default Set<String> getDomainSet() {
        return getDomain().getDomainSet();
    }

    /**
     * Gets the domain size d, i.e., |Ω|.
     *
     * @return the domain size d.
     */
    int getD();

    /**
     * Return the privacy parameter ε.
     *
     * @return the privacy parameter ε.
     */
    double getEpsilon();
}
