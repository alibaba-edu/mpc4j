package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory.RpZl64PtoType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongConfig;

/**
 * Replicated zl64 sharing party configure.
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3LongConfig extends AbstractMultiPartyPtoConfig implements TripletRpLongConfig {
    /**
     * whether the required protocol is malicious
     */
    private final boolean malicious;
    /**
     * the directory to buffer the unverified and tuples
     */
    private final String bufferPath;

    /**
     * the maximum number of bytes in each buffer vectors
     */
    private final int maxBufferElementLen;
    /**
     * the maximum number of memoryBuffer vectors
     */
    private final int memoryBufferThreshold;
    /**
     * the maximum number of vectors can be verified at once
     */
    private final int singleVerifyThreshold;

    private Aby3LongConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        malicious = builder.malicious;
        bufferPath = builder.bufferPath;
        maxBufferElementLen = builder.maxBufferElementLen;
        memoryBufferThreshold = builder.memoryBufferThreshold;
        singleVerifyThreshold = builder.singleVerifyThreshold;
    }

    @Override
    public RpZl64PtoType getRpZl64PtoType() {
        return malicious ? RpZl64PtoType.MALICIOUS_TUPLE : RpZl64PtoType.SEMI_HONEST;
    }

    public String getBufferPath() {
        return bufferPath;
    }

    public int getMaxBufferElementLen() {
        return maxBufferElementLen;
    }

    public int getMemoryBufferThreshold() {
        return memoryBufferThreshold;
    }

    public int getSingleVerifyThreshold() {
        return singleVerifyThreshold;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aby3LongConfig> {
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
        private int maxBufferElementLen;
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
            maxBufferElementLen = 1 << 20;
            memoryBufferThreshold = 1 << 2;
            singleVerifyThreshold = 1 << 2;
        }

        public void setBufferPath(String bufferPath) {
            this.bufferPath = bufferPath;
        }

        public void setVerifyParam(int maxBufferElementLen, int memoryBufferThreshold, int singleVerifyThreshold) {
            this.maxBufferElementLen = maxBufferElementLen;
            this.memoryBufferThreshold = memoryBufferThreshold;
            this.singleVerifyThreshold = singleVerifyThreshold;
        }

        @Override
        public Aby3LongConfig build() {
            return new Aby3LongConfig(this);
        }
    }
}
