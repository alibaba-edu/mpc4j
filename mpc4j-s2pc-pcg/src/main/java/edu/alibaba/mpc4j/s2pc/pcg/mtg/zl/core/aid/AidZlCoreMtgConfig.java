package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.aid;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;

/**
 * aid Zl core multiplication triple generation config.
 *
 * @author Weiran Liu
 * @date 2023/6/14
 */
public class AidZlCoreMtgConfig extends AbstractMultiPartyPtoConfig implements ZlCoreMtgConfig {
    /**
     * the Zl instance
     */
    private final Zl zl;

    private AidZlCoreMtgConfig(Builder builder) {
        super(SecurityModel.TRUSTED_DEALER);
        zl = builder.zl;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.AID;
    }

    @Override
    public Zl getZl() {
        return zl;
    }

    @Override
    public int maxNum() {
        // In theory, aider can support arbitrary num. Here we limit the max num in case of memory exception.
        return 1 << 24;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<AidZlCoreMtgConfig> {
        /**
         * the Zl instance
         */
        private final Zl zl;

        public Builder(Zl zl) {
            this.zl = zl;
        }

        @Override
        public AidZlCoreMtgConfig build() {
            return new AidZlCoreMtgConfig(this);
        }
    }
}
