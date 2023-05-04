package edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.cgs22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.urb.UrbopprfFactory;

/**
 * CGS22 unbalanced related-batch OPPRF config.
 *
 * @author Weiran Liu
 * @date 2023/4/18
 */
public class Cgs22UrbopprfConfig implements UrbopprfConfig {
    /**
     * d = 3
     */
    private static final int D = 3;
    /**
     * single-query OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * cuckoo hash bin type
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Cgs22UrbopprfConfig(Builder builder) {
        sqOprfConfig = builder.sqOprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public UrbopprfFactory.UrbopprfType getPtoType() {
        return UrbopprfFactory.UrbopprfType.CGS22;
    }

    @Override
    public void setEnvType(EnvType envType) {
        sqOprfConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return sqOprfConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    @Override
    public int getD() {
        return D;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Cgs22UrbopprfConfig> {
        /**
         * OPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * cuckoo hash bin type
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder() {
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_PSZ18_3_HASH;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            int hashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
            MathPreconditions.checkEqual("hashNum", "D", hashNum, D);
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Cgs22UrbopprfConfig build() {
            return new Cgs22UrbopprfConfig(this);
        }
    }
}
