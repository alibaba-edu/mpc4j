package edu.alibaba.mpc4j.s2pc.pso.psi.hfh99;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;

/**
 * HFH99-椭圆曲线PSI协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/9/19
 */
public class Hfh99EccPsiConfig implements PsiConfig {
    /**
     * 环境类型
     */
    private final EnvType envType;
    /**
     * 是否压缩编码
     */
    private final boolean compressEncode;

    private Hfh99EccPsiConfig(Builder builder) {
        envType = builder.envType;
        compressEncode = builder.compressEncode;
    }

    @Override
    public PsiFactory.PsiType getPtoType() {
        return PsiFactory.PsiType.HFH99_ECC;
    }

    @Override
    public EnvType getEnvType() {
        return envType;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    public boolean getCompressEncode() {
        return compressEncode;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Hfh99EccPsiConfig> {
        /**
         * 环境类型
         */
        private EnvType envType;
        /**
         * 是否压缩编码
         */
        private boolean compressEncode;

        public Builder() {
            envType = EnvType.STANDARD;
            compressEncode = true;
        }

        public Builder setEnvType(EnvType envType) {
            this.envType = envType;
            return this;
        }

        public Builder setCompressEncode(boolean compressEncode) {
            this.compressEncode = compressEncode;
            return this;
        }

        @Override
        public Hfh99EccPsiConfig build() {
            return new Hfh99EccPsiConfig(this);
        }
    }
}
