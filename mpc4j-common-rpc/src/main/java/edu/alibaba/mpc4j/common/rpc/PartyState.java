package edu.alibaba.mpc4j.common.rpc;

/**
 * Party State.
 *
 * @author Weiran Liu
 * @date 2023/2/9
 */
public enum PartyState {
    /**
     * newly created party without initialized
     */
    NON_INITIALIZED,
    /**
     * initialized
     */
    INITIALIZED,
    /**
     * destroyed
     */
    DESTROYED,
}
