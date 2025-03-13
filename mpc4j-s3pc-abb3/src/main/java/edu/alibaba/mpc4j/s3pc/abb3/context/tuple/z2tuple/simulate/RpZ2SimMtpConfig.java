package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.simulate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2MtpConfig;

/**
 * simulate z2 mt provider config
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class RpZ2SimMtpConfig extends AbstractMultiPartyPtoConfig implements RpZ2MtpConfig {
    /**
     * the max buffer size
     */
    private final int maxBufferSize;

    public RpZ2SimMtpConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        maxBufferSize = builder.maxBufferSize;
    }

    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    @Override
    public MtProviderType getProviderType() {
        return MtProviderType.SIMULATE;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpZ2SimMtpConfig> {
        /**
         * the max buffer size
         */
        private final int maxBufferSize;

        public Builder() {
            maxBufferSize = 1 << 20;
        }

        @Override
        public RpZ2SimMtpConfig build() {
            return new RpZ2SimMtpConfig(this);
        }
    }
}
