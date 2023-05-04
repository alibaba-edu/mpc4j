package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import java.util.Random;

/**
 * HeavyGuardian-based Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public interface HgHhLdpConfig extends HhLdpConfig {
    /**
     * Gets the bucket num.
     *
     * @return the bucket num.
     */
    int getW();

    /**
     * Gets λ_h, i.e., the cell num in each bucket.
     *
     * @return λ_h.
     */
    int getLambdaH();

    /**
     * Gets the random state used in HeavyGuardian.
     *
     * @return the random state used in HeavyGuardian.
     */
    Random getHgRandom();
}
