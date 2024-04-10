package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;

/**
 * configure of replicated 3p sharing z2 basic environment
 *
 * @author Feng Han
 * @date 2024/01/24
 */
public class RpZ2EnvConfig extends AbstractMultiPartyPtoConfig {
    public RpZ2EnvConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<RpZ2EnvConfig> {

        public Builder() {

        }

        @Override
        public RpZ2EnvConfig build() {
            return new RpZ2EnvConfig(this);
        }
    }
}
