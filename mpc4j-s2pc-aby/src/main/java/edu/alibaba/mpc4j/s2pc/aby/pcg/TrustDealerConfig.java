package edu.alibaba.mpc4j.s2pc.aby.pcg;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;

/**
 * Trust Dealer config.
 *
 * @author Weiran Liu
 * @date 2024/6/28
 */
public class TrustDealerConfig extends AbstractMultiPartyPtoConfig {

    private TrustDealerConfig() {
        super(SecurityModel.TRUSTED_DEALER);
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<TrustDealerConfig> {

        public Builder() {
            // empty
        }

        @Override
        public TrustDealerConfig build() {
            return new TrustDealerConfig();
        }
    }
}
