package edu.alibaba.mpc4j.s2pc.pcg.ct.direct;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;

/**
 * direct coin-tossing protocol config.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class DirectCoinTossConfig extends AbstractMultiPartyPtoConfig implements CoinTossConfig {

    private DirectCoinTossConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public CoinTossFactory.CoinTossType getPtoType() {
        return CoinTossFactory.CoinTossType.DIRECT;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<DirectCoinTossConfig> {

        public Builder() {
            // empty
        }

        @Override
        public DirectCoinTossConfig build() {
            return new DirectCoinTossConfig(this);
        }
    }
}
