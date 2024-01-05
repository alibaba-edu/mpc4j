package edu.alibaba.mpc4j.work.psipir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;
import edu.alibaba.mpc4j.work.BatchPirConfig;
import edu.alibaba.mpc4j.work.BatchPirFactory;

/**
 * PSI-PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchPirConfig extends AbstractMultiPartyPtoConfig implements BatchPirConfig {
    /**
     * UPSI config
     */
    private final UpsiConfig upsiConfig;

    public Lpzl24BatchPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.upsiConfig);
        upsiConfig = builder.upsiConfig;
    }

    public UpsiConfig getUpsiConfig() {
        return upsiConfig;
    }

    @Override
    public BatchPirFactory.BatchIndexPirType getPtoType() {
        return BatchPirFactory.BatchIndexPirType.PSI_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lpzl24BatchPirConfig> {
        /**
         * UPSI config
         */
        private UpsiConfig upsiConfig;

        public Builder() {
            upsiConfig = new Cmg21UpsiConfig.Builder().build();
        }

        public Builder setUpsiConfig(UpsiConfig upsiConfig) {
            this.upsiConfig = upsiConfig;
            return this;
        }

        @Override
        public Lpzl24BatchPirConfig build() {
            return new Lpzl24BatchPirConfig(this);
        }
    }
}
