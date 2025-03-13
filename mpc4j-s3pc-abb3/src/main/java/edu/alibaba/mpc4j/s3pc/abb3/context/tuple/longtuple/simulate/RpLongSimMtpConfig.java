package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.simulate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtpConfig;

/**
 * simulate long mt provider config
 *
 * @author Feng Han
 * @date 2025/2/17
 */
public class RpLongSimMtpConfig extends AbstractMultiPartyPtoConfig implements RpLongMtpConfig {
    /**
     * the max buffer size
     */
    private final int maxBufferSize;

    public RpLongSimMtpConfig(Builder builder) {
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

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpLongSimMtpConfig> {
        /**
         * the max buffer size
         */
        private final int maxBufferSize;

        public Builder() {
            maxBufferSize = 1 << 20;
        }

        @Override
        public RpLongSimMtpConfig build() {
            return new RpLongSimMtpConfig(this);
        }
    }
}
