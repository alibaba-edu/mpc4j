package edu.alibaba.mpc4j.common.sampler.binary;

import edu.alibaba.mpc4j.common.sampler.Sampler;

/**
 * Binary sampler interface.
 *
 * @author Weiran Liu
 * @date 2021/07/27
 */
public interface BinarySampler extends Sampler {
    /**
     * Sample.
     *
     * @return sample result.
     */
    boolean sample();
}
