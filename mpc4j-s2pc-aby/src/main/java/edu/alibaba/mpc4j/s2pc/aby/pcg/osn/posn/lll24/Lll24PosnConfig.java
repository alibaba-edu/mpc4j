package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.PosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.posn.PosnFactory.PosnType;

/**
 * LLL24 Precomputed-OSN config.
 *
 * @author Feng Han
 * @date 2024/5/7
 */
public class Lll24PosnConfig extends AbstractMultiPartyPtoConfig implements PosnConfig {

    private Lll24PosnConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public PosnType getPtoType() {
        return PosnType.LLL24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lll24PosnConfig> {

        public Builder() {
            // empty
        }

        @Override
        public Lll24PosnConfig build() {
            return new Lll24PosnConfig(this);
        }
    }
}
