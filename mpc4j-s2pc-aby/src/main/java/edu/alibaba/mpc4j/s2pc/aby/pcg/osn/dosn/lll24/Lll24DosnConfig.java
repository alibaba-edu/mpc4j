package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory.DosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;

/**
 * Decision OSN config.
 *
 * @author Feng Han
 * @date 2024/6/20
 */
public class Lll24DosnConfig extends AbstractMultiPartyPtoConfig implements DosnConfig {
    /**
     * Random OSN
     */
    private final RosnConfig rosnConfig;

    private Lll24DosnConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.rosnConfig);
        rosnConfig = builder.rosnConfig;
    }

    public RosnConfig getRosnConfig() {
        return rosnConfig;
    }

    @Override
    public DosnType getPtoType() {
        return DosnType.LLL24;
    }

    @Override
    public RosnType getRosnType() {
        return rosnConfig.getPtoType();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lll24DosnConfig> {
        /**
         * Random OSN
         */
        private final RosnConfig rosnConfig;

        public Builder(boolean silent) {
            rosnConfig = RosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder(RosnConfig rosnConfig) {
            this.rosnConfig = rosnConfig;
        }

        @Override
        public Lll24DosnConfig build() {
            return new Lll24DosnConfig(this);
        }
    }
}
