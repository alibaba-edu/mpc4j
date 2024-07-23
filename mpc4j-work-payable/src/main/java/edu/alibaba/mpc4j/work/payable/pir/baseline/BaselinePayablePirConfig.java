package edu.alibaba.mpc4j.work.payable.pir.baseline;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.zcl23.Zcl23PkeMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirConfig;
import edu.alibaba.mpc4j.work.payable.pir.PayablePirConfig;
import edu.alibaba.mpc4j.work.payable.pir.PayablePirFactory;

/**
 * Baseline payable PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class BaselinePayablePirConfig extends AbstractMultiPartyPtoConfig implements PayablePirConfig {

    /**
     * keyword PIR config
     */
    private final StdKsPirConfig ksPirConfig;
    /**
     * mqRPMT config
     */
    private final MqRpmtConfig mqRpmtConfig;

    public BaselinePayablePirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ksPirConfig, builder.mqRpmtConfig);
        this.ksPirConfig = builder.ksPirConfig;
        this.mqRpmtConfig = builder.mqRpmtConfig;
    }

    @Override
    public PayablePirFactory.PayablePirType getProType() {
        return PayablePirFactory.PayablePirType.BASELINE;
    }

    public StdKsPirConfig getKsPirConfig() {
        return ksPirConfig;
    }

    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BaselinePayablePirConfig> {
        /**
         * keyword PIR config
         */
        private StdKsPirConfig ksPirConfig;
        /**
         * mqRPMT config
         */
        private MqRpmtConfig mqRpmtConfig;

        public Builder() {
            ksPirConfig = new LabelpsiStdKsPirConfig.Builder().build();
            mqRpmtConfig = new Zcl23PkeMqRpmtConfig.Builder().build();
        }

        public Builder setKsPirConfig(StdKsPirConfig ksPirConfig) {
            this.ksPirConfig = ksPirConfig;
            return this;
        }

        public Builder setMqRpmtConfig(MqRpmtConfig mqRpmtConfig) {
            this.mqRpmtConfig = mqRpmtConfig;
            return this;
        }

        @Override
        public BaselinePayablePirConfig build() {
            return new BaselinePayablePirConfig(this);
        }
    }
}
