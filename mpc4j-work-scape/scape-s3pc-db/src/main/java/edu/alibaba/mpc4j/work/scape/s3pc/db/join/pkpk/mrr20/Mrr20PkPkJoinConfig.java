package edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.mrr20;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.join.pkpk.PkPkJoinFactory.PkPkJoinPtoType;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingConfig;
import edu.alibaba.mpc4j.work.scape.s3pc.db.tools.randenc.RandomEncodingFactory;

/**
 * configure of MRR20 PkPk join protocol
 *
 * @author Feng Han
 * @date 2025/2/19
 */
public class Mrr20PkPkJoinConfig extends AbstractMultiPartyPtoConfig implements PkPkJoinConfig {
    /**
     * how many hash functions should be used in join
     */
    private final int hashNum;
    /**
     * config of random encoding
     */
    private final RandomEncodingConfig encodingConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType hashBinType;

    private Mrr20PkPkJoinConfig(Builder builder) {
        super(builder.malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST);
        hashNum = builder.hashNum;
        encodingConfig = builder.encodingConfig;
        hashBinType = builder.hashBinType;
    }

    public int getHashNum() {
        return hashNum;
    }

    public RandomEncodingConfig getEncodingConfig() {
        return encodingConfig;
    }

    public CuckooHashBinType getHashBinType() {
        return hashBinType;
    }

    @Override
    public PkPkJoinPtoType getPkPkJoinPtoType() {
        return PkPkJoinPtoType.PK_PK_JOIN_MRR20;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Mrr20PkPkJoinConfig> {
        /**
         * whether malicious secure or not
         */
        private final boolean malicious;
        /**
         * how many hash functions should be used in join
         */
        private final int hashNum;
        /**
         * config of random encoding
         */
        private final RandomEncodingConfig encodingConfig;
        /**
         * cuckoo hash type
         */
        private CuckooHashBinType hashBinType;

        public Builder(boolean malicious) {
            this.malicious = malicious;
            hashNum = 3;
            SecurityModel securityModel = malicious ? SecurityModel.MALICIOUS : SecurityModel.SEMI_HONEST;
            encodingConfig = RandomEncodingFactory.createDefaultConfig(securityModel);
            hashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setCuckooHashType(CuckooHashBinType hashBinType){
            this.hashBinType = hashBinType;
            return this;
        }

        @Override
        public Mrr20PkPkJoinConfig build() {
            return new Mrr20PkPkJoinConfig(this);
        }
    }
}
