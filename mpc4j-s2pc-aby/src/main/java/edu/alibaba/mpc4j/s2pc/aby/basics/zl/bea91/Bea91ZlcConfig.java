package edu.alibaba.mpc4j.s2pc.aby.basics.zl.bea91;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.ZlMtgFactory;

/**
 * Bea91 Zl circuit config.
 *
 * @author Weiran Liu
 * @date 2023/5/10
 */
public class Bea91ZlcConfig extends AbstractMultiPartyPtoConfig implements ZlcConfig {
    /**
     * multiplication triple generation config
     */
    private final ZlMtgConfig mtgConfig;

    private Bea91ZlcConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mtgConfig);
        mtgConfig = builder.mtgConfig;
    }

    public ZlMtgConfig getMtgConfig() {
        return mtgConfig;
    }

    @Override
    public ZlcFactory.ZlType getPtoType() {
        return ZlcFactory.ZlType.BEA91;
    }

    @Override
    public Zl getZl() {
        return mtgConfig.getZl();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea91ZlcConfig> {
        /**
         * multiplication triple generation config
         */
        private ZlMtgConfig mtgConfig;

        public Builder(SecurityModel securityModel, Zl zl) {
            mtgConfig = ZlMtgFactory.createDefaultConfig(securityModel, zl);
        }

        public Builder setMtgConfig(ZlMtgConfig mtgConfig) {
            this.mtgConfig = mtgConfig;
            return this;
        }

        @Override
        public Bea91ZlcConfig build() {
            return new Bea91ZlcConfig(this);
        }
    }
}
