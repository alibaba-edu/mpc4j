package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.mrr20;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20.Mrr20PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinFactory.PkPkSemiJoinPtoType;

/**
 * The config of the PkPk semi-join protocol
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class Mrr20PkPkSemiJoinConfig extends AbstractMultiPartyPtoConfig implements PkPkSemiJoinConfig {
    /**
     * config of pk-pk join protocol
     */
    private final Mrr20PkPkJoinConfig joinConfig;

    private Mrr20PkPkSemiJoinConfig(Builder builder) {
        super(builder.joinConfig.getSecurityModel());
        joinConfig = builder.joinConfig;
    }

    public Mrr20PkPkJoinConfig getJoinConfig() {
        return joinConfig;
    }

    @Override
    public PkPkSemiJoinPtoType getPkPkSemiJoinPtoType() {
        return PkPkSemiJoinPtoType.PK_PK_SEMI_JOIN_MRR20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mrr20PkPkSemiJoinConfig> {
        /**
         * config of pk-pk join protocol
         */
        private final Mrr20PkPkJoinConfig joinConfig;

        public Builder(boolean malicious) {
            this.joinConfig = new Mrr20PkPkJoinConfig.Builder(malicious).build();
        }

        @Override
        public Mrr20PkPkSemiJoinConfig build() {
            return new Mrr20PkPkSemiJoinConfig(this);
        }
    }
}
