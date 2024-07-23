package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.ywl20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotFactory.MspCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.bsp.BspCotConfig;

/**
 * YWL20-UNI-MSP-COT config.
 *
 * @author Weiran Liu
 * @date 2022/01/22
 */
public class Ywl20UniMspCotConfig extends AbstractMultiPartyPtoConfig implements MspCotConfig {
    /**
     * BSP-COT config
     */
    private final BspCotConfig bspCotConfig;

    private Ywl20UniMspCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.bspcotConfig);
        bspCotConfig = builder.bspcotConfig;
    }

    @Override
    public BspCotConfig getBspCotConfig() {
        return bspCotConfig;
    }

    @Override
    public MspCotType getPtoType() {
        return MspCotType.YWL20_UNI;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ywl20UniMspCotConfig> {
        /**
         * BSP-COT config
         */
        private BspCotConfig bspcotConfig;

        public Builder(SecurityModel securityModel) {
            this.bspcotConfig = BspCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setBspcotConfig(BspCotConfig bspcotConfig) {
            this.bspcotConfig = bspcotConfig;
            return this;
        }

        @Override
        public Ywl20UniMspCotConfig build() {
            return new Ywl20UniMspCotConfig(this);
        }
    }
}
