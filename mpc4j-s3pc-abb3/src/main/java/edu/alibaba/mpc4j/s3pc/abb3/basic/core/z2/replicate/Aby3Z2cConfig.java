package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.Abb3Factory.PtoType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cConfig;

/**
 * Replicated z2 sharing party configure.
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3Z2cConfig extends AbstractMultiPartyPtoConfig implements TripletZ2cConfig {
    /**
     * the directory to buffer the unverified and tuples
     */
    private final String bufferPath;
    /**
     * the maximum number of bytes in each buffer vectors
     */
    private final int bufferMaxByteLen;
    /**
     * the maximum number of memoryBuffer vectors
     */
    private final int memoryBufferThreshold;
    /**
     * the maximum number of vectors can be verified at once
     */
    private final int singleVerifyThreshold;

    private Aby3Z2cConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        bufferPath = builder.bufferPath;
        bufferMaxByteLen = builder.bufferMaxByteLen;
        memoryBufferThreshold = builder.memoryBufferThreshold;
        singleVerifyThreshold = builder.singleVerifyThreshold;
    }

    @Override
    public PtoType getPtoType() {
        return PtoType.REPLICATE;
    }

    public String getBufferPath() {
        return bufferPath;
    }

    public int getBufferMaxByteLen() {
        return bufferMaxByteLen;
    }

    public int getMemoryBufferThreshold() {
        return memoryBufferThreshold;
    }

    public int getSingleVerifyThreshold() {
        return singleVerifyThreshold;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aby3Z2cConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * the directory to buffer the unverified and tuples
         */
        private String bufferPath;
        /**
         * the maximum number of bytes in each buffer vectors
         */
        private int bufferMaxByteLen;
        /**
         * the maximum number of memoryBuffer vectors
         */
        private int memoryBufferThreshold;
        /**
         * the maximum number of vectors can be verified at once
         */
        private int singleVerifyThreshold;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            this.bufferPath = ".";
            bufferMaxByteLen = 1 << 20;
            memoryBufferThreshold = 1 << 4;
            singleVerifyThreshold = 1 << 4;
        }

        public void setBufferPath(String bufferPath) {
            this.bufferPath = bufferPath;
        }

        public void setVerifyParam(int bufferMaxByteLen, int memoryBufferThreshold, int singleVerifyThreshold) {
            this.bufferMaxByteLen = bufferMaxByteLen;
            this.memoryBufferThreshold = memoryBufferThreshold;
            this.singleVerifyThreshold = singleVerifyThreshold;
        }

        @Override
        public Aby3Z2cConfig build() {
            return new Aby3Z2cConfig(this);
        }
    }
}
