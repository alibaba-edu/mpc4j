package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;

/**
 * TCL23 pm-PEQT based on Ecc DDH config.
 *
 * @author Liqiang Peng
 * @date 2024/4/1
 */
public class Tcl23EccDdhPmPeqtConfig extends AbstractMultiPartyPtoConfig implements PmPeqtConfig {

    /**
     * compress encode
     */
    private final boolean compressEncode;

    public Tcl23EccDdhPmPeqtConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        this.compressEncode = builder.compressEncode;
    }

    public boolean isCompressEncode() {
        return compressEncode;
    }

    @Override
    public PmPeqtFactory.PmPeqtType getPtoType() {
        return PmPeqtFactory.PmPeqtType.TCL23_ECC_DDH;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Tcl23EccDdhPmPeqtConfig> {

        /**
         * compress encode
         */
        private boolean compressEncode;

        public Builder() {
            compressEncode = true;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Tcl23EccDdhPmPeqtConfig build() {
            return new Tcl23EccDdhPmPeqtConfig(this);
        }
    }
}
