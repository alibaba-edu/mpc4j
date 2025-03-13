package edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.hzf22;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.hzf22.Hzf22PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.semijoin.pkpk.PkPkSemiJoinFactory.PkPkSemiJoinPtoType;

/**
 * configure for HZF22 PkPk semi join
 *
 * @author Feng Han
 * @date 2025/2/25
 */
public class Hzf22PkPkSemiJoinConfig extends AbstractMultiPartyPtoConfig implements PkPkSemiJoinConfig {
    /**
     * config of pk-pk join protocol
     */
    private final Hzf22PkPkJoinConfig joinConfig;

    private Hzf22PkPkSemiJoinConfig(Builder builder) {
        super(builder.joinConfig.getSecurityModel());
        joinConfig = builder.joinConfig;
    }

    public Hzf22PkPkJoinConfig getJoinConfig() {
        return joinConfig;
    }

    @Override
    public PkPkSemiJoinPtoType getPkPkSemiJoinPtoType() {
        return PkPkSemiJoinPtoType.PK_PK_SEMI_JOIN_HZF22;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hzf22PkPkSemiJoinConfig> {
        /**
         * config of pk-pk join protocol
         */
        private final Hzf22PkPkJoinConfig joinConfig;

        public Builder(boolean malicious) {
            this.joinConfig = new Hzf22PkPkJoinConfig.Builder(malicious).build();
        }

        @Override
        public Hzf22PkPkSemiJoinConfig build() {
            return new Hzf22PkPkSemiJoinConfig(this);
        }
    }
}
