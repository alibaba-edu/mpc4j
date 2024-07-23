package edu.alibaba.mpc4j.work.payable.pir.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.StdKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.ks.labelpsi.LabelpsiStdKsPirConfig;
import edu.alibaba.mpc4j.work.payable.pir.PayablePirConfig;
import edu.alibaba.mpc4j.work.payable.pir.PayablePirFactory;

/**
 * ZLP24 payable PIR config.
 *
 * @author Liqiang Peng
 * @date 2024/7/2
 */
public class Zlp24PayablePirConfig extends AbstractMultiPartyPtoConfig implements PayablePirConfig {

    /**
     * keyword PIR config
     */
    private final StdKsPirConfig ksPirConfig;

    public Zlp24PayablePirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.ksPirConfig);
        this.ksPirConfig = builder.ksPirConfig;
    }

    @Override
    public PayablePirFactory.PayablePirType getProType() {
        return PayablePirFactory.PayablePirType.ZLP24;
    }

    public StdKsPirConfig getKsPirConfig() {
        return ksPirConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zlp24PayablePirConfig> {
        /**
         * keyword PIR config
         */
        private StdKsPirConfig ksPirConfig;

        public Builder() {
            ksPirConfig = new LabelpsiStdKsPirConfig.Builder().build();
        }

        public Builder setKsPirConfig(StdKsPirConfig kwPirConfig) {
            this.ksPirConfig = kwPirConfig;
            return this;
        }

        @Override
        public Zlp24PayablePirConfig build() {
            return new Zlp24PayablePirConfig(this);
        }
    }
}
