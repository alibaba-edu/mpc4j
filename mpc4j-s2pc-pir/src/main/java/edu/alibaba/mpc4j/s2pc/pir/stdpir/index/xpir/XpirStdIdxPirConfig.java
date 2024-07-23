package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.StdIdxPirFactory.StdIdxPirType;

/**
 * XPIR config.
 *
 * @author Liqiang Peng
 * @date 2022/8/25
 */
public class XpirStdIdxPirConfig extends AbstractMultiPartyPtoConfig implements PbcableStdIdxPirConfig {
    /**
     * XPIR params
     */
    private final XpirStdIdxPirParams params;

    public XpirStdIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        this.params = builder.params;
    }

    @Override
    public StdIdxPirType getProType() {
        return StdIdxPirType.XPIR;
    }

    @Override
    public XpirStdIdxPirParams getStdIdxPirParams() {
        return params;
    }


    public static class Builder implements org.apache.commons.lang3.builder.Builder<XpirStdIdxPirConfig> {
        /**
         * XPIR params
         */
        private XpirStdIdxPirParams params;

        public Builder() {
            params = XpirStdIdxPirParams.DEFAULT_PARAMS;
        }

        public Builder setParams(XpirStdIdxPirParams params) {
            this.params = params;
            return this;
        }

        @Override
        public XpirStdIdxPirConfig build() {
            return new XpirStdIdxPirConfig(this);
        }
    }
}
