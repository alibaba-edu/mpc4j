package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory.PsuType;

/**
 * JSZ22-SFC-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/03/14
 */
public class Jsz22SfcPsuConfig extends AbstractMultiPartyPtoConfig implements PsuConfig {
    /**
     * OPRF协议配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * OSN协议配置项
     */
    private final DosnConfig dosnConfig;
    /**
     * 核COT协议配置项
     */
    private final CoreCotConfig coreCotConfig;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Jsz22SfcPsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.oprfConfig, builder.dosnConfig, builder.coreCotConfig);
        oprfConfig = builder.oprfConfig;
        dosnConfig = builder.dosnConfig;
        coreCotConfig = builder.coreCotConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PsuType getPtoType() {
        return PsuType.JSZ22_SFC;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public DosnConfig getOsnConfig() {
        return dosnConfig;
    }

    public CoreCotConfig getCoreCotConfig() {
        return coreCotConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Jsz22SfcPsuConfig> {
        /**
         * OPRF协议配置项
         */
        private final OprfConfig oprfConfig;
        /**
         * OSN协议配置项
         */
        private final DosnConfig dosnConfig;
        /**
         * 核COT协议配置项
         */
        private final CoreCotConfig coreCotConfig;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            dosnConfig = DosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            coreCotConfig = CoreCotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Jsz22SfcPsuConfig build() {
            return new Jsz22SfcPsuConfig(this);
        }
    }
}
