package edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.fillper.FillPermutationFactory.FillType;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.SoprpFactory;

/**
 * HZF22 fill permutation config
 *
 * @author Feng Han
 * @date 2025/2/26
 */
public class Hzf22FillPermutationConfig extends AbstractMultiPartyPtoConfig implements FillPermutationConfig {
    /**
     * threshold for reduce the key
     */
    public static final int TARGET_DIM = 64;
    /**
     * config of shared oblivious prp
     */
    private final SoprpConfig soprpConfig;

    private Hzf22FillPermutationConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        soprpConfig = builder.soprpConfig;
    }

    public SoprpConfig getSoprpConfig() {
        return soprpConfig;
    }

    @Override
    public FillType getFillType() {
        return FillType.SOPRP_BASED_HZF22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22FillPermutationConfig> {
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
            soprpConfig = SoprpFactory.createDefaultConfig(securityModel, TARGET_DIM);
        }

        @Override
        public Hzf22FillPermutationConfig build() {
            return new Hzf22FillPermutationConfig(this);
        }
    }
}
