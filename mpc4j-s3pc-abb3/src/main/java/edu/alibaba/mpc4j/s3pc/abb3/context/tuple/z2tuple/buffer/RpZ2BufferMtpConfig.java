package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.buffer;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.RpMtProviderFactory.MtProviderType;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.RpZ2MtpConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvConfig;

/**
 * configure of replicated 3p sharing z2 mt provider in buffer mode
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpZ2BufferMtpConfig extends AbstractMultiPartyPtoConfig implements RpZ2MtpConfig {
    /**
     * mtg config
     */
    private final RpZ2EnvConfig rpZ2EnvConfig;
    /**
     * mtg config
     */
    private final RpZ2MtgConfig rpZ2MtgConfig;

    public RpZ2BufferMtpConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        rpZ2EnvConfig = builder.rpZ2EnvConfig;
        rpZ2MtgConfig = builder.rpZ2MtgConfig;
    }

    @Override
    public MtProviderType getProviderType() {
        return MtProviderType.BUFFER;
    }

    public RpZ2EnvConfig getRpZ2EnvConfig() {
        return rpZ2EnvConfig;
    }

    public RpZ2MtgConfig getRpZ2MtgConfig() {
        return rpZ2MtgConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpZ2BufferMtpConfig> {
        /**
         * mtg config
         */
        private final RpZ2EnvConfig rpZ2EnvConfig;
        /**
         * mtg config
         */
        private RpZ2MtgConfig rpZ2MtgConfig;

        public Builder() {
            rpZ2EnvConfig = new RpZ2EnvConfig.Builder().build();
            rpZ2MtgConfig = RpZ2MtgFactory.createDefaultConfig();
        }

        public void setRpZ2MtgConfig(RpZ2MtgConfig rpZ2MtgConfig) {
            this.rpZ2MtgConfig = rpZ2MtgConfig;
        }

        @Override
        public RpZ2BufferMtpConfig build() {
            return new RpZ2BufferMtpConfig(this);
        }
    }
}
