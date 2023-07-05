package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * DSZ15 OT-based Zl core multiplication triple generation protocol configuration.
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/2/20
 */
public class Dsz15OtZlCoreMtgConfig extends AbstractMultiPartyPtoConfig implements ZlCoreMtgConfig {
    /**
     * the Zl instance
     */
    private final Zl zl;
    /**
     * the COT configuration
     */
    private final CotConfig cotConfig;

    private Dsz15OtZlCoreMtgConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cotConfig);
        zl = builder.zl;
        cotConfig = builder.cotConfig;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.DSZ15_OT;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public int maxNum() {
        return (int) Math.floor((double) (1 << 24) / zl.getL());
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15OtZlCoreMtgConfig> {
        /**
         * the Zl instance
         */
        private final Zl zl;
        /**
         * the COT configuration
         */
        private CotConfig cotConfig;

        public Builder(Zl zl) {
            super();
            this.zl = zl;
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, true);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Dsz15OtZlCoreMtgConfig build() {
            return new Dsz15OtZlCoreMtgConfig(this);
        }
    }
}
