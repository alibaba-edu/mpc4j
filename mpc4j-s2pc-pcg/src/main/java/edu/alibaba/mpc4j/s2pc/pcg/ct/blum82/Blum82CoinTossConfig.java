package edu.alibaba.mpc4j.s2pc.pcg.ct.blum82;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.CoinTossFactory;

/**
 * Blum82 coin-tossing protocol config.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class Blum82CoinTossConfig extends AbstractMultiPartyPtoConfig implements CoinTossConfig {

    private Blum82CoinTossConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public CoinTossFactory.CoinTossType getPtoType() {
        return CoinTossFactory.CoinTossType.BLUM82;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Blum82CoinTossConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Blum82CoinTossConfig build() {
            return new Blum82CoinTossConfig(this);
        }
    }
}
