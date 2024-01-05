package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.common.structure.okve.dokvs.gf2e.Gf2eDokvsFactory.Gf2eDokvsType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;

/**
 * GMR21-mqRPMT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtConfig extends AbstractMultiPartyPtoConfig implements MqRpmtConfig {
    /**
     * 布谷鸟哈希所用OPRF协议配置项
     */
    private final OprfConfig cuckooHashOprfConfig;
    /**
     * PEQT所用OPRF协议配置项
     */
    private final OprfConfig peqtOprfConfig;
    /**
     * OSN协议配置项
     */
    private final OsnConfig osnConfig;
    /**
     * OKVS type
     */
    private final Gf2eDokvsType okvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Gmr21MqRpmtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.cuckooHashOprfConfig, builder.peqtOprfConfig, builder.osnConfig);
        cuckooHashOprfConfig = builder.cuckooHashOprfConfig;
        peqtOprfConfig = builder.peqtOprfConfig;
        osnConfig = builder.osnConfig;
        okvsType = builder.okvsType;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public MqRpmtFactory.MqRpmtType getPtoType() {
        return MqRpmtFactory.MqRpmtType.GMR21;
    }

    @Override
    public int getVectorLength(int serverElementSize, int clientElementSize) {
        MathPreconditions.checkGreater("server_element_size", serverElementSize, 1);
        MathPreconditions.checkGreater("client_element_size", clientElementSize, 1);
        return CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
    }

    public OprfConfig getCuckooHashOprfConfig() {
        return cuckooHashOprfConfig;
    }

    public OprfConfig getPeqtOprfConfig() {
        return peqtOprfConfig;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public Gf2eDokvsType getOkvsType() {
        return okvsType;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Gmr21MqRpmtConfig> {
        /**
         * 布谷鸟哈希所用OPRF协议配置项
         */
        private OprfConfig cuckooHashOprfConfig;
        /**
         * PEQT所用OPRF协议配置项
         */
        private OprfConfig peqtOprfConfig;
        /**
         * OSN协议配置项
         */
        private OsnConfig osnConfig;
        /**
         * OKVS type
         */
        private Gf2eDokvsType okvsType;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            cuckooHashOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            peqtOprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            osnConfig = OsnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            okvsType = Gf2eDokvsType.MEGA_BIN;
            // GMR21源代码使用普通布谷鸟哈希实现无贮存区布谷鸟哈希的功能，这样通信量可以更小一点
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setCuckooHashOprfConfig(OprfConfig cuckooHashOprfConfig) {
            this.cuckooHashOprfConfig = cuckooHashOprfConfig;
            return this;
        }

        public Builder setPeqtOprfConfig(OprfConfig peqtOprfConfig) {
            this.peqtOprfConfig = peqtOprfConfig;
            return this;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        public Builder setOkvsType(Gf2eDokvsType okvsType) {
            this.okvsType = okvsType;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Gmr21MqRpmtConfig build() {
            return new Gmr21MqRpmtConfig(this);
        }
    }
}
