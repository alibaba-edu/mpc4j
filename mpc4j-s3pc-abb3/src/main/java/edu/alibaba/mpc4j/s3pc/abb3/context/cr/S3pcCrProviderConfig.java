package edu.alibaba.mpc4j.s3pc.abb3.context.cr;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;

/**
 * Correlated randomness provider configure for 3PC
 *
 * @author Feng Han
 * @date 2023/12/25
 */
public class S3pcCrProviderConfig extends AbstractMultiPartyPtoConfig {
    /**
     * the size of byte array for buffering prg
     */
    private final int bufferByteSize;
    public S3pcCrProviderConfig(Builder builder) {
        super(SecurityModel.IDEAL);
        bufferByteSize = builder.bufferByteSize;
    }

    public int getBufferByteSize() {
        return bufferByteSize;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<S3pcCrProviderConfig> {
        /**
         * the size of byte array for buffering prg
         */
        private int bufferByteSize;

        public Builder() {
            this.bufferByteSize = 1<<24;
        }

        public void setBufferByteSize(int bufferByteSize){
            this.bufferByteSize = bufferByteSize;
        }

        @Override
        public S3pcCrProviderConfig build() {
            return new S3pcCrProviderConfig(this);
        }
    }
}
