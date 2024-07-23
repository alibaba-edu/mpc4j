package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.prrs24;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory.RosnType;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf.F32SowOprfFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.Conv32Factory.Conv32Type;

/**
 * PRRS24 OPRF Random OSN config.
 *
 * @author Weiran Liu
 * @date 2024/6/7
 */
public class Prrs24OprfRosnConfig extends AbstractMultiPartyPtoConfig implements RosnConfig {
    /**
     * (F3, F2)-sowOPRF config
     */
    private final F32SowOprfConfig f32SowOprfConfig;

    private Prrs24OprfRosnConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.f32SowOprfConfig);
        f32SowOprfConfig = builder.f32SowOprfConfig;
    }

    @Override
    public RosnType getPtoType() {
        return RosnType.PRRS24_OPRF;
    }

    public F32SowOprfConfig getF32SowOprfConfig() {
        return f32SowOprfConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Prrs24OprfRosnConfig> {
        /**
         * (F3, F2)-sowOPRF config
         */
        private final F32SowOprfConfig f32SowOprfConfig;

        public Builder(Conv32Type conv32Type) {
            f32SowOprfConfig = F32SowOprfFactory.createDefaultConfig(conv32Type);
        }

        @Override
        public Prrs24OprfRosnConfig build() {
            return new Prrs24OprfRosnConfig(this);
        }
    }
}
