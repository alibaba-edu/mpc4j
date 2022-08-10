package edu.alibaba.mpc4j.s2pc.pcg.ot.no.lh2n;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.hot.LhotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.no.NotFactory.NotType;

/**
 * 2^l选1-HOT转换为n选1-OT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/5/27
 */
public class Lh2nNotConfig implements NotConfig {
    /**
     * LHOT协议配置项
     */
    private final LhotConfig lhotConfig;

    private Lh2nNotConfig(Builder builder) {
        lhotConfig = builder.lhotConfig;
    }

    public LhotConfig getLhotConfig() {
        return lhotConfig;
    }

    @Override
    public NotType getPtoType() {
        return NotType.LH2N;
    }

    @Override
    public EnvType getEnvType() {
        return lhotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        return lhotConfig.getSecurityModel();
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Lh2nNotConfig> {
        /**
         * LHOT协议配置项
         */
        private LhotConfig lhotConfig;

        public Builder() {
            lhotConfig = LhotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setLhotConfig(LhotConfig lhotConfig) {
            this.lhotConfig = lhotConfig;
            return this;
        }

        @Override
        public Lh2nNotConfig build() {
            return new Lh2nNotConfig(this);
        }
    }
}
