package edu.alibaba.work.femur.demo;

/**
 * status in Femur PIR.
 *
 * @author Weiran Liu
 * @date 2024/12/2
 */
public enum FemurStatus {
    /**
     * server has not been initialized
     */
    SERVER_NOT_INIT,
    /**
     * server has not set database
     */
    SERVER_NOT_KVDB,
    /**
     * success
     */
    SERVER_SUCC_RES,
    /**
     * Client not registered
     */
    CLIENT_NOT_REGS,
    /**
     * Hint version mismatch
     */
    HINT_V_MISMATCH,
}
