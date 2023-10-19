package edu.alibaba.mpc4j.s2pc.pir.payable.zlp23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.keyword.cmg21.Cmg21KwPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.payable.PayablePirFactory;

/**
 * ZLP23 payable PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/7
 */
public class Zlp23PayablePirConfig extends AbstractMultiPartyPtoConfig implements PayablePirConfig {

    /**
     * keyword PIR config
     */
    private final KwPirConfig kwPirConfig;

    public Zlp23PayablePirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.kwPirConfig);
        this.kwPirConfig = builder.kwPirConfig;
    }

    @Override
    public PayablePirFactory.PayablePirType getProType() {
        return PayablePirFactory.PayablePirType.ZLP23;
    }

    public KwPirConfig getKwPirConfig() {
        return kwPirConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zlp23PayablePirConfig> {
        /**
         * keyword PIR config
         */
        private KwPirConfig kwPirConfig;

        public Builder() {
            kwPirConfig = new Cmg21KwPirConfig.Builder().build();
        }

        public Builder setKwPirConfig(KwPirConfig kwPirConfig) {
            this.kwPirConfig = kwPirConfig;
            return this;
        }

        @Override
        public Zlp23PayablePirConfig build() {
            return new Zlp23PayablePirConfig(this);
        }
    }
}
