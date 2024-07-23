package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory.CpKsPirType;

import static edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.*;

/**
 * ALPR21 client-specific preprocessing KSPIR config.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class Alpr21CpKsPirConfig extends AbstractMultiPartyPtoConfig implements CpKsPirConfig {
    /**
     * client-specific preprocessing PIR config
     */
    private final CpIdxPirConfig indexCpPirConfig;
    /**
     * sq-OPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * hash byte length (for keyword)
     */
    private final int hashByteLength;

    public Alpr21CpKsPirConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cpIdxPirConfig);
        indexCpPirConfig = builder.cpIdxPirConfig;
        sqOprfConfig = builder.sqOprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
        hashByteLength = builder.hashByteLength;
    }

    @Override
    public CpKsPirType getPtoType() {
        return CpKsPirType.ALPR21;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public CpIdxPirConfig getIndexCpPirConfig() {
        return indexCpPirConfig;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public int hashByteLength() {
        return hashByteLength;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Alpr21CpKsPirConfig> {
        /**
         * single index client-specific preprocessing PIR config
         */
        private CpIdxPirConfig cpIdxPirConfig;
        /**
         * sq-OPRF config
         */
        private final SqOprfConfig sqOprfConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinType cuckooHashBinType;
        /**
         * hash byte length (for keyword)
         */
        private final int hashByteLength;

        public Builder() {
            cpIdxPirConfig = new PianoCpIdxPirConfig.Builder().build();
            sqOprfConfig = SqOprfFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NO_STASH_NAIVE;
            hashByteLength = CommonConstants.BLOCK_BYTE_LENGTH;
        }

        public Builder setCpIdxPirConfig(CpIdxPirConfig cpIdxPirConfig) {
            this.cpIdxPirConfig = cpIdxPirConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Alpr21CpKsPirConfig build() {
            return new Alpr21CpKsPirConfig(this);
        }
    }
}
