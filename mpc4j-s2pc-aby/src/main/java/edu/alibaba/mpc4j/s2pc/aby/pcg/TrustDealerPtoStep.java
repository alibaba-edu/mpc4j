package edu.alibaba.mpc4j.s2pc.aby.pcg;

/**
 * Trust Dealer protocol step.
 *
 * @author Weiran Liu
 * @date 2024/6/28
 */
public enum TrustDealerPtoStep {
    /**
     * register query
     */
    REGISTER_QUERY,
    /**
     * register response
     */
    REGISTER_RESPONSE,
    /**
     * request Z2 triple
     */
    REQUEST_Z2_TRIPLE,
    /**
     * request Zl triple
     */
    REQUEST_ZL_TRIPLE,
    /**
     * request Zl64 triple
     */
    REQUEST_ZL64_TRIPLE,
    /**
     * request response
     */
    REQUEST_RESPONSE,
    /**
     * destroy query
     */
    DESTROY_QUERY,
    /**
     * destroy response
     */
    DESTROY_RESPONSE,
}
