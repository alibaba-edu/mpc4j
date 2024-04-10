package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.ShuffleConfig;

/**
 * Replicated-sharing shuffling party config.
 *
 * @author Feng Han
 * @date 2024/01/18
 */
public class Aby3ShuffleConfig extends AbstractMultiPartyPtoConfig implements ShuffleConfig {
    /**
     * whether malicious secure or not
     */
    private final boolean malicious;
    private Aby3ShuffleConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        malicious = builder.malicious;
    }

    public boolean isMalicious() {
        return malicious;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Aby3ShuffleConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;

        public Builder(boolean malicious) {
            this.malicious = malicious;
        }

        @Override
        public Aby3ShuffleConfig build() {
            return new Aby3ShuffleConfig(this);
        }
    }
}
