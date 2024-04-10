package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21j;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiFactory.UpsiType;

/**
 * CMG21J config.
 *
 * @author Liqiang Peng
 * @date 2024/2/23
 */
public class Cmg21jUpsiConfig extends AbstractMultiPartyPtoConfig implements UpsiConfig {
    /**
     * MP-OPRF
     */
    private final MpOprfConfig mpOprfConfig;

    public Cmg21jUpsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
    }

    @Override
    public UpsiType getPtoType() {
        return UpsiType.CMG21J;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21jUpsiConfig> {
        /**
         * MP-OPRF
         */
        private MpOprfConfig mpOprfConfig;

        public Builder() {
            mpOprfConfig = OprfFactory.createMpOprfDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setMpOprfConfig(MpOprfConfig mpOprfConfig) {
            this.mpOprfConfig = mpOprfConfig;
            return this;
        }

        @Override
        public Cmg21jUpsiConfig build() {
            return new Cmg21jUpsiConfig(this);
        }
    }
}
