package edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.hzf22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.traversal.TraversalFactory.TraversalType;

/**
 * HZF22 traversal party config
 *
 * @author Feng Han
 * @date 2024/02/26
 */
public class Hzf22TraversalConfig extends AbstractMultiPartyPtoConfig implements TraversalConfig {
    private Hzf22TraversalConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
    }

    @Override
    public TraversalType getTraversalType() {
        return TraversalType.TRAVERSAL_HZF22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22TraversalConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;

        public Builder(boolean malicious) {
            this.malicious = malicious;
        }

        @Override
        public Hzf22TraversalConfig build() {
            return new Hzf22TraversalConfig(this);
        }
    }
}

