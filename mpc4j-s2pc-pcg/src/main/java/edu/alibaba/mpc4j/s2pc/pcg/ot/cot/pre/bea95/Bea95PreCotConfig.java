package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.bea95;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.pre.PreCotFactory;

/**
 * Bea95-预计算COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/14
 */
public class Bea95PreCotConfig implements PreCotConfig {
    /**
     * 环境类型
     */
    private EnvType envType;

    private Bea95PreCotConfig(Builder builder) {
        envType = EnvType.STANDARD;
    }

    @Override
    public PreCotFactory.PreCotType getPtoType() {
        return PreCotFactory.PreCotType.Bea95;
    }

    @Override
    public void setEnvType(EnvType envType) {
        this.envType = envType;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.MALICIOUS;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bea95PreCotConfig> {

        public Builder() {

        }

        @Override
        public Bea95PreCotConfig build() {
            return new Bea95PreCotConfig(this);
        }
    }
}
