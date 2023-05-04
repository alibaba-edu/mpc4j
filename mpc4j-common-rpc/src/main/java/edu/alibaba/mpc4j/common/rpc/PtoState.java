package edu.alibaba.mpc4j.common.rpc;

/**
 * protocol state.
 *
 * @author Weiran Liu
 * @date 2023/2/9
 */
public enum PtoState {
    /**
     * init begin
     */
    INIT_BEGIN,
    /**
     * init step
     */
    INIT_STEP,
    /**
     * init end
     */
    INIT_END,
    /**
     * protocol begin
     */
    PTO_BEGIN,
    /**
     * protocol step
     */
    PTO_STEP,
    /**
     * protocol end
     */
    PTO_END,
}
