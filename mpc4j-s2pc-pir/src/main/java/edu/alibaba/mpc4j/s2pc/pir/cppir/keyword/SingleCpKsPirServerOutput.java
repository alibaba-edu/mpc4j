package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword;

/**
 * Single client-preprocessing KSPIR output for each query.
 *
 * @author Weiran Liu
 * @date 2023/9/25
 */
public enum SingleCpKsPirServerOutput {
    /**
     * client's keyword is in server's key-value map
     */
    IN,
    /**
     * client's keyword is not in server's key-value map
     */
    OUT,
    /**
     * server does not know if client's keyword is in server's key-value map
     */
    UNKNOWN,
}
