package edu.alibaba.mpc4j.s3pc.abb3.basic;

import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * ABB3 party factory.
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Abb3Factory implements PtoFactory {
    /**
     * private constructor
     */
    private Abb3Factory() {
        // empty
    }

    public enum PtoType {
        /**
         * replicated secret sharing
         */
        REPLICATE,
    }

    /**
     * the share type of data, in binary form or in arithmetic form
     */
    public enum ShareType {
        /**
         * ABY3 Binary
         */
        BINARY,
        /**
         * ABY3 ARITHMETIC
         */
        ARITHMETIC,
    }
}
