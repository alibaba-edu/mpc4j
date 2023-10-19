package edu.alibaba.mpc4j.s2pc.pso.payablepsi.zlp23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.osn.gmr21.Gmr21OsnConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.impl.direct.DirectCotConfig;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.PayablePsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.payablepsi.PayablePsiFactory;

/**
 * ZLP23 payable PSI config.
 *
 * @author Liqiang Peng
 * @date 2023/9/15
 */
public class Zlp23PayablePsiConfig extends AbstractMultiPartyPtoConfig implements PayablePsiConfig {

    /**
     * OSN config
     */
    private final OsnConfig osnConfig;
    /**
     * sqOPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;

    public Zlp23PayablePsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.osnConfig, builder.sqOprfConfig);
        this.osnConfig = builder.osnConfig;
        this.sqOprfConfig = builder.sqOprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PayablePsiFactory.PayablePsiType getPtoType() {
        return PayablePsiFactory.PayablePsiType.ZLP23;
    }

    public OsnConfig getOsnConfig() {
        return osnConfig;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zlp23PayablePsiConfig> {
        /**
         * OSN config
         */
        private OsnConfig osnConfig;
        /**
         * sqOPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;

        public Builder() {
            osnConfig = new Gmr21OsnConfig.Builder(false)
                .setCotConfig(new DirectCotConfig.Builder(SecurityModel.MALICIOUS).build()).build();
            sqOprfConfig = new Ra17ByteEccSqOprfConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE;
        }

        public Builder setOsnConfig(OsnConfig osnConfig) {
            this.osnConfig = osnConfig;
            return this;
        }

        public Builder setSqOprfConfig(SqOprfConfig sqOprfConfig) {
            this.sqOprfConfig = sqOprfConfig;
            return this;
        }

        public Builder setCuckooHashBinType(CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType) {
            this.cuckooHashBinType = cuckooHashBinType;
            return this;
        }

        @Override
        public Zlp23PayablePsiConfig build() {
            return new Zlp23PayablePsiConfig(this);
        }
    }
}
