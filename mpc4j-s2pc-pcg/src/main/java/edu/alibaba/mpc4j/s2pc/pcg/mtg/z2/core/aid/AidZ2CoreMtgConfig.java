package edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.aid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.core.Z2CoreMtgFactory;

/**
 * aid Z2 core multiplication triple generation config.
 *
 * @author Weiran Liu
 * @date 2023/5/20
 */
public class AidZ2CoreMtgConfig extends AbstractMultiPartyPtoConfig implements Z2CoreMtgConfig {

    private AidZ2CoreMtgConfig(Builder builder) {
        super(SecurityModel.TRUSTED_DEALER);
    }

    @Override
    public Z2CoreMtgFactory.Z2CoreMtgType getPtoType() {
        return Z2CoreMtgFactory.Z2CoreMtgType.AID;
    }

    @Override
    public int maxNum() {
        // In theory, aider can support arbitrary num. Here we limit the max num in case of memory exception.
        return 1 << 24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AidZ2CoreMtgConfig> {

        public Builder() {
            // empty
        }

        @Override
        public AidZ2CoreMtgConfig build() {
            return new AidZ2CoreMtgConfig(this);
        }
    }
}
