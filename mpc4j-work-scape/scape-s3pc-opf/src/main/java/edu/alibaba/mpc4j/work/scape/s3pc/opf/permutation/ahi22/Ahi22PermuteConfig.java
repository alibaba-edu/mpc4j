package edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.ahi22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.permutation.PermuteFactory.PermuteType;

/**
 * Ahi22 oblivious permutation party config.
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class Ahi22PermuteConfig extends AbstractMultiPartyPtoConfig implements PermuteConfig {

    private Ahi22PermuteConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
    }

    @Override
    public PermuteType getPermuteType() {
        return PermuteType.PERMUTE_AHI22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Ahi22PermuteConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;

        public Builder(boolean malicious) {
            this.malicious = malicious;
        }

        @Override
        public Ahi22PermuteConfig build() {
            return new Ahi22PermuteConfig(this);
        }
    }
}
