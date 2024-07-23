package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.rrt23;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.structure.lpn.dual.excoder.ExCoderFactory.ExCoderType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.NcCotFactory.NcCotType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.sp.msp.bcg19.Bcg19RegMspCotConfig;

/**
 * RRT23-NC-COT config.
 *
 * @author Weiran Liu
 * @date 2024/6/17
 */
public class Rrt23NcCotConfig extends AbstractMultiPartyPtoConfig implements NcCotConfig {
    /**
     * MSP-COT
     */
    private final MspCotConfig mspCotConfig;
    /**
     * coder type
     */
    private final ExCoderType exCoderType;

    private Rrt23NcCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.mspCotConfig);
        mspCotConfig = builder.mspCotConfig;
        exCoderType = builder.exCoderType;
    }

    public MspCotConfig getMspCotConfig() {
        return mspCotConfig;
    }

    @Override
    public NcCotType getPtoType() {
        return NcCotType.RRT23;
    }

    @Override
    public int maxNum() {
        return 1 << Rrt23NcCotPtoDesc.MAX_LOG_N;
    }

    public ExCoderType getCodeType() {
        return exCoderType;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rrt23NcCotConfig> {
        /**
         * MSP-COT
         */
        private final MspCotConfig mspCotConfig;
        /**
         * coder type
         */
        private final ExCoderType exCoderType;

        public Builder(SecurityModel securityModel) {
            mspCotConfig = new Bcg19RegMspCotConfig.Builder(securityModel).build();
            exCoderType = ExCoderType.EX_ACC_7;
        }

        @Override
        public Rrt23NcCotConfig build() {
            return new Rrt23NcCotConfig(this);
        }
    }
}
