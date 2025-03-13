package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.mrr20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFactory.EncodingPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;

/**
 * configure for mrr20 randomized encoding protocol
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class Mrr20RandomEncodingConfig extends AbstractMultiPartyPtoConfig implements RandomEncodingConfig {
    /**
     * threshold for reduce the key
     */
    public static final int THRESHOLD_REDUCE = 80;
    /**
     * config of shared oblivious prp
     */
    private final SoprpConfig soprpConfig;

    private Mrr20RandomEncodingConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        soprpConfig = builder.soprpConfig;
    }

    public SoprpConfig getSoprpConfig() {
        return soprpConfig;
    }

    @Override
    public EncodingPtoType getPtoType() {
        return EncodingPtoType.MRR20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mrr20RandomEncodingConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * config of shared oblivious prp
         */
        private SoprpConfig soprpConfig;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            soprpConfig = SoprpFactory.createDefaultConfig(securityModel, THRESHOLD_REDUCE);
        }

        public Builder setSoprpConfig(SoprpConfig soprpConfig) {
            this.soprpConfig = soprpConfig;
            return this;
        }

        @Override
        public Mrr20RandomEncodingConfig build() {
            return new Mrr20RandomEncodingConfig(this);
        }
    }
}
