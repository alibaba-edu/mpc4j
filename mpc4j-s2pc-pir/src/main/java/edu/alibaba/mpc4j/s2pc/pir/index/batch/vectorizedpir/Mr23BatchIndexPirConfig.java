package edu.alibaba.mpc4j.s2pc.pir.index.batch.vectorizedpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.BatchIndexPirFactory;

/**
 * Vectorized Batch PIR config.
 *
 * @author Liqiang Peng
 * @date 2023/3/7
 */
public class Mr23BatchIndexPirConfig extends AbstractMultiPartyPtoConfig implements BatchIndexPirConfig {

    public Mr23BatchIndexPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
    }

    @Override
    public BatchIndexPirFactory.BatchIndexPirType getPtoType() {
        return BatchIndexPirFactory.BatchIndexPirType.VECTORIZED_BATCH_PIR;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mr23BatchIndexPirConfig> {

        @Override
        public Mr23BatchIndexPirConfig build() {
            return new Mr23BatchIndexPirConfig(this);
        }
    }
}
