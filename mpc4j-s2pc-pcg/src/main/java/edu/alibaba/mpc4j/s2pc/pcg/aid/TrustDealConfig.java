package edu.alibaba.mpc4j.s2pc.pcg.aid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;

/**
 * trust deal config.
 *
 * @author Weiran Liu
 * @date 2023/5/19
 */
public class TrustDealConfig extends AbstractMultiPartyPtoConfig {

    TrustDealConfig(Builder builder) {
        super(SecurityModel.TRUSTED_DEALER);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TrustDealConfig> {

        public Builder() {
            // empty
        }

        @Override
        public TrustDealConfig build() {
            return new TrustDealConfig(this);
        }
    }
}
