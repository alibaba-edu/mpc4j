package edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.crypto.prp.DefaultFixedKeyPrp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

import java.util.Objects;

/**
 * PIANO client-specific preprocessing index PIR config.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class PianoCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig {
    /**
     * fixed key PRP
     */
    private final FixedKeyPrp fixedKeyPrp;

    public PianoCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        fixedKeyPrp = Objects.requireNonNullElseGet(builder.fixedKeyPrp, () -> new DefaultFixedKeyPrp(getEnvType()));
    }

    public FixedKeyPrp getFixedKeyPrp() {
        return fixedKeyPrp;
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.PIANO;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<PianoCpIdxPirConfig> {
        /**
         * fixed key PRP
         */
        private FixedKeyPrp fixedKeyPrp;

        public Builder() {
            fixedKeyPrp = null;
        }

        public Builder setFixedKeyPrp(FixedKeyPrp fixedKeyPrp) {
            this.fixedKeyPrp = fixedKeyPrp;
            return this;
        }

        @Override
        public PianoCpIdxPirConfig build() {
            return new PianoCpIdxPirConfig(this);
        }
    }
}
