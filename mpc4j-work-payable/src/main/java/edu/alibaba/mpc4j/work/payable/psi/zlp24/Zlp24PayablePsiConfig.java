package edu.alibaba.mpc4j.work.payable.psi.zlp24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.gmr21.Gmr21NetRosnConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfConfig;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.ra17.Ra17ByteEccSqOprfConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiConfig;
import edu.alibaba.mpc4j.work.payable.psi.PayablePsiFactory;

/**
 * ZLP24 payable PSI config.
 *
 * @author Liqiang Peng
 * @date 2024/7/1
 */
public class Zlp24PayablePsiConfig extends AbstractMultiPartyPtoConfig implements PayablePsiConfig {

    /**
     * DOSN config
     */
    private final DosnConfig dosnConfig;
    /**
     * sqOPRF config
     */
    private final SqOprfConfig sqOprfConfig;
    /**
     * cuckoo hash type
     */
    private final CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;

    public Zlp24PayablePsiConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.dosnConfig, builder.sqOprfConfig);
        this.dosnConfig = builder.dosnConfig;
        this.sqOprfConfig = builder.sqOprfConfig;
        cuckooHashBinType = builder.cuckooHashBinType;
    }

    @Override
    public PayablePsiFactory.PayablePsiType getPtoType() {
        return PayablePsiFactory.PayablePsiType.ZLP24;
    }

    public DosnConfig getDosnConfig() {
        return dosnConfig;
    }

    public SqOprfConfig getSqOprfConfig() {
        return sqOprfConfig;
    }

    public CuckooHashBinFactory.CuckooHashBinType getCuckooHashBinType() {
        return cuckooHashBinType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Zlp24PayablePsiConfig> {
        /**
         * DOSN config
         */
        private DosnConfig dosnConfig;
        /**
         * sqOPRF config
         */
        private SqOprfConfig sqOprfConfig;
        /**
         * cuckoo hash
         */
        private CuckooHashBinFactory.CuckooHashBinType cuckooHashBinType;

        public Builder() {
            dosnConfig = new Lll24DosnConfig.Builder(
                new Gmr21NetRosnConfig.Builder(false)
                    .setCotConfig(CotFactory.createDefaultConfig(SecurityModel.MALICIOUS, false))
                    .build()
            ).build();
            sqOprfConfig = new Ra17ByteEccSqOprfConfig.Builder().build();
            cuckooHashBinType = CuckooHashBinFactory.CuckooHashBinType.NO_STASH_NAIVE;
        }

        public Builder setDsnConfig(DosnConfig dosnConfig) {
            this.dosnConfig = dosnConfig;
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
        public Zlp24PayablePsiConfig build() {
            return new Zlp24PayablePsiConfig(this);
        }
    }
}