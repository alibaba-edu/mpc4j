package edu.alibaba.mpc4j.common.rpc.utils;

/**
 * payload type.
 *
 * @author Weiran Liu
 * @date 2024/1/5
 */
public enum PayloadType {
    /**
     * normal payload
     */
    NORMAL,
    /**
     * empty payload
     */
    EMPTY,
    /**
     * singleton payload
     */
    SINGLETON,
    /**
     * equal-size payload
     */
    EQUAL_SIZE,
}
