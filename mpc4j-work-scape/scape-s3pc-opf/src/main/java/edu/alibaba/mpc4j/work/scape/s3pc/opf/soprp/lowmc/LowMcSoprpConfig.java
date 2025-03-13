package edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory.SoprpType;

/**
 * lowmc party config
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class LowMcSoprpConfig extends AbstractMultiPartyPtoConfig implements SoprpConfig {
    /**
     * the file directory storing parameters
     */
    private final LowMcParam param;
    private LowMcSoprpConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        param = builder.param;
    }

    @Override
    public SoprpType getPrpType() {
        return SoprpType.LOWMC_SOPRP;
    }

    public LowMcParam getParam() {
        return param;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<LowMcSoprpConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * the file directory storing parameters
         */
        private final LowMcParam param;

        public Builder(boolean malicious, LowMcParam param) {
            this.malicious = malicious;
            this.param = param;
        }

        @Override
        public LowMcSoprpConfig build() {
            return new LowMcSoprpConfig(this);
        }
    }
}
