package edu.alibaba.mpc4j.s2pc.pir.index.batch.labelpsi;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;

/**
 * CMG21 Batch PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Cmg21BatchIndexPirConfig extends AbstractMultiPartyPtoConfig implements BatchIndexPirConfig {

    public Cmg21BatchIndexPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.LABEL_PSI;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cmg21BatchIndexPirConfig> {

        @Override
        public Cmg21BatchIndexPirConfig build() {
            return new Cmg21BatchIndexPirConfig(this);
        }
    }
}
