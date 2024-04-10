package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;

/**
 * configure of replicated 3p sharing zl64 basic environment
 *
 * @author Feng Han
 * @date 2024/01/25
 */
public class RpLongEnvConfig extends AbstractMultiPartyPtoConfig {
    public RpLongEnvConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpLongEnvConfig> {

        public Builder() {

        }

        @Override
        public RpLongEnvConfig build() {
            return new RpLongEnvConfig(this);
        }
    }
}
