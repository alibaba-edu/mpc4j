package edu.alibaba.mpc4j.s2pc.pir.cppir;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * Gaussian LWE-based PIR config.
 *
 * @author Weiran Liu
 * @date 2024/9/2
 */
public interface GaussianLwePirConfig extends MultiPartyPtoConfig {
    /**
     * Gets Gaussian LWE parameter.
     *
     * @return Gaussian LWE parameter.
     */
    GaussianLweParam getGaussianLweParam();
}
