package edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.MpOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiFactory.UpsiType;

/**
 * CMG21 config.
 *
 * @author Liqiang Peng
 * @date 2022/6/13
 */
public class Cmg21UpsiConfig extends AbstractMultiPartyPtoConfig implements UpsiConfig {
    /**
     * MP-OPRF
     */
    private final MpOprfConfig mpOprfConfig;

    public Cmg21UpsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.mpOprfConfig);
        mpOprfConfig = builder.mpOprfConfig;
    }

    @Override
    public UpsiType getPtoType() {
        return UpsiType.CMG21;
    }

    public MpOprfConfig getMpOprfConfig() {
        return mpOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21UpsiConfig> {
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
        public Cmg21UpsiConfig build() {
            return new Cmg21UpsiConfig(this);
        }
    }
}
