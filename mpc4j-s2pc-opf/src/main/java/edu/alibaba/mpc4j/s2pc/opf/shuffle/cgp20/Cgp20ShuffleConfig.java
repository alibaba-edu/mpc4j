package edu.alibaba.mpc4j.s2pc.opf.shuffle.cgp20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleConfig;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory.ShuffleType;

/**
 * CGP20 shuffle protocol config
 *
 * @author Feng Han
 * @date 2024/9/26
 */
public class Cgp20ShuffleConfig extends AbstractMultiPartyPtoConfig implements ShuffleConfig {
    /**
     * Dosn config
     */
    private final DosnConfig dosnConfig;

    public Cgp20ShuffleConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.dosnConfig);
        this.dosnConfig = builder.dosnConfig;
    }

    @Override
    public ShuffleType getPtoType() {
        return ShuffleType.CGP20;
    }

    @Override
    public RosnType getRosnType() {
        return dosnConfig.getRosnType();
    }

    public DosnConfig getDosnConfig() {
        return dosnConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgp20ShuffleConfig> {
        /**
         * Dosn config
         */
        private DosnConfig dosnConfig;

        public Builder(boolean silent) {
            dosnConfig = DosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
        }

        public Builder setRosnConfig(RosnConfig rosnConfig) {
            this.dosnConfig = new Lll24DosnConfig.Builder(rosnConfig).build();
            return this;
        }

        @Override
        public Cgp20ShuffleConfig build() {
            return new Cgp20ShuffleConfig(this);
        }
    }

}
