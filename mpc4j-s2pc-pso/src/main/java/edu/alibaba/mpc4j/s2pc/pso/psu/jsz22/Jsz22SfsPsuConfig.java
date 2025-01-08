package edu.alibaba.mpc4j.s2pc.pso.psu.jsz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.oprf.OprfFactory;
import edu.alibaba.mpc4j.s2pc.pso.psu.OoPsuConfig;
import edu.alibaba.mpc4j.s2pc.pso.psu.PsuFactory;

/**
 * JSZ22-SFS-PSU协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/03/18
 */
public class Jsz22SfsPsuConfig extends AbstractMultiPartyPtoConfig implements OoPsuConfig {
    /**
     * OPRF协议配置项
     */
    private final OprfConfig oprfConfig;
    /**
     * OSN协议配置项
     */
    private final DosnConfig dosnConfig;
    /**
     * random-OSN
     */
    private final RosnConfig rosnConfig;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;

    private Jsz22SfsPsuConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.oprfConfig, builder.dosnConfig);
        oprfConfig = builder.oprfConfig;
        dosnConfig = builder.dosnConfig;
        rosnConfig = builder.rosnConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PsuFactory.PsuType getPtoType() {
        return PsuFactory.PsuType.JSZ22_SFS;
    }

    public OprfConfig getOprfConfig() {
        return oprfConfig;
    }

    public DosnConfig getOsnConfig() {
        return dosnConfig;
    }

    public RosnConfig getRosnConfig() {
        return rosnConfig;
    }

    public CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Jsz22SfsPsuConfig> {
        /**
         * OPRF协议配置项
         */
        private final OprfConfig oprfConfig;
        /**
         * OSN协议配置项
         */
        private DosnConfig dosnConfig;
        /**
         * random-OSN
         */
        private RosnConfig rosnConfig;
        /**
         * 布谷鸟哈希类型
         */
        private CuckooHashBinType cuckooHashBinType;

        public Builder(boolean silent) {
            oprfConfig = OprfFactory.createOprfDefaultConfig(SecurityModel.SEMI_HONEST);
            dosnConfig = DosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            rosnConfig = RosnFactory.createDefaultConfig(SecurityModel.SEMI_HONEST, silent);
            // 论文建议平衡场景下使用PSZ18的3哈希协议，非平衡场景下使用PSZ18的4哈希协议
            cuckooHashBinType = CuckooHashBinType.NAIVE_3_HASH;
        }

        public Builder setCuckooHashBinType(CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        public Builder setRosnConfig(RosnConfig rosnConfig) {
            this.rosnConfig = rosnConfig;
            this.dosnConfig = new Lll24DosnConfig.Builder(rosnConfig).build();
            return this;
        }

        @Override
        public Jsz22SfsPsuConfig build() {
            return new Jsz22SfsPsuConfig(this);
        }
    }
}
