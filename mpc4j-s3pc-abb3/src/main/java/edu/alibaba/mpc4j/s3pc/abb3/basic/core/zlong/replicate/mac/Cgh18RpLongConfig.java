package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongCpFactory.RpZl64PtoType;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.TripletRpLongConfig;

/**
 * Replicated zl64 sharing party configure for Cgh18 protocol
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Cgh18RpLongConfig extends AbstractMultiPartyPtoConfig implements TripletRpLongConfig {
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

    private Cgh18RpLongConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        bufferPath = builder.bufferPath;
        maxBufferElementLen = builder.maxBufferElementLen;
        memoryBufferThreshold = builder.memoryBufferThreshold;
        singleVerifyThreshold = builder.singleVerifyThreshold;
    }

    @Override
    public RpZl64PtoType getRpZl64PtoType() {
        return RpZl64PtoType.MALICIOUS_MAC;
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgh18RpLongConfig> {
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

        public Builder() {
            this.bufferPath = ".";
            maxBufferElementLen = 1 << 20;
            memoryBufferThreshold = 1 << 6;
            singleVerifyThreshold = 1 << 6;
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
        public Cgh18RpLongConfig build() {
            return new Cgh18RpLongConfig(this);
        }
    }
}
