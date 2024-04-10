package edu.alibaba.mpc4j.s2pc.opf.pmpeqt.tcl23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtConfig;
import edu.alibaba.mpc4j.s2pc.opf.pmpeqt.PmPeqtFactory;

/**
 * TCL23 pm-PEQT based on Byte Ecc DDH config.
 *
 * @author Liqiang Peng
 * @date 2024/3/6
 */
public class Tcl23ByteEccDdhPmPeqtConfig extends AbstractMultiPartyPtoConfig implements PmPeqtConfig {

    public Tcl23ByteEccDdhPmPeqtConfig() {
        super(SecurityModel.SEMI_HONEST);
    }

    @Override
    public PmPeqtFactory.PmPeqtType getPtoType() {
        return PmPeqtFactory.PmPeqtType.TCL23_BYTE_ECC_DDH;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Tcl23ByteEccDdhPmPeqtConfig> {

        @Override
        public Tcl23ByteEccDdhPmPeqtConfig build() {
            return new Tcl23ByteEccDdhPmPeqtConfig();
        }
    }
}
