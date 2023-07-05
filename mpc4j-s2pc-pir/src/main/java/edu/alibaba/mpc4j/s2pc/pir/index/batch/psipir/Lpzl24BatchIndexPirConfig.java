package edu.alibaba.mpc4j.s2pc.pir.index.batch.psipir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsi.UpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsi.cmg21.Cmg21UpsiConfig;

/**
 * PSI-PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Lpzl24BatchIndexPirConfig extends AbstractMultiPartyPtoConfig implements BatchIndexPirConfig {
    /**
     * UPSI config
     */
    private final UpsiConfig upsiConfig;

    public Lpzl24BatchIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.upsiConfig);
        upsiConfig = builder.upsiConfig;
    }

    public UpsiConfig getUpsiConfig() {
        return upsiConfig;
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.PSI_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lpzl24BatchIndexPirConfig> {
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
        public Lpzl24BatchIndexPirConfig build() {
            return new Lpzl24BatchIndexPirConfig(this);
        }
    }
}
