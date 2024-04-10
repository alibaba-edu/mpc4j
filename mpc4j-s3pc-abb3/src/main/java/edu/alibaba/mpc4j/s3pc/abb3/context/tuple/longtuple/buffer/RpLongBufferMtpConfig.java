package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.buffer;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.RpLongMtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgFactory;

/**
 * configure of replicated 3p sharing zl64 mt provider in buffer mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpLongBufferMtpConfig extends AbstractMultiPartyPtoConfig implements RpLongMtpConfig {
    /**
     * mtg config
     */
    private final RpLongEnvConfig rpLongEnvConfig;
    /**
     * mtg config
     */
    private final RpLongMtgConfig rpLongMtgConfig;

    public RpLongBufferMtpConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        rpLongEnvConfig = builder.rpLongEnvConfig;
        rpLongMtgConfig = builder.rpLongMtgConfig;
    }

    @Override
    public MtProviderType getProviderType() {
        return MtProviderType.BUFFER;
    }

    public RpLongEnvConfig getRpZl64EnvConfig() {
        return rpLongEnvConfig;
    }

    public RpLongMtgConfig getRpZl64MtgConfig() {
        return rpLongMtgConfig;
    }


    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpLongBufferMtpConfig> {
        /**
         * mtg config
         */
        private final RpLongEnvConfig rpLongEnvConfig;
        /**
         * mtg config
         */
        private RpLongMtgConfig rpLongMtgConfig;

        public Builder() {
            rpLongEnvConfig = new RpLongEnvConfig.Builder().build();
            rpLongMtgConfig = RpLongMtgFactory.createDefaultConfig();
        }

        public void setRpZl64MtgConfig(RpLongMtgConfig rpLongMtgConfig) {
            this.rpLongMtgConfig = rpLongMtgConfig;
        }

        @Override
        public RpLongBufferMtpConfig build() {
            return new RpLongBufferMtpConfig(this);
        }
    }
}
