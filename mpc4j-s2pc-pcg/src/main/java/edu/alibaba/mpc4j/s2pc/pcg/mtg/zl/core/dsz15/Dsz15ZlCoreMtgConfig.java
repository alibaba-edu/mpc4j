package edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.dsz15;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zl.core.ZlCoreMtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotFactory;

/**
 * DSZ15核l比特三元组生成协议配置项。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/9/8
 */
public class Dsz15ZlCoreMtgConfig implements ZlCoreMtgConfig {
    /**
     * 乘法三元组比特长度
     */
    private final int l;
    /**
     * COT协议配置项
     */
    private final CotConfig cotConfig;

    private Dsz15ZlCoreMtgConfig(Builder builder) {
        l = builder.l;
        cotConfig = builder.cotConfig;
    }

    @Override
    public ZlCoreMtgFactory.ZlCoreMtgType getPtoType() {
        return ZlCoreMtgFactory.ZlCoreMtgType.DSZ15;
    }

    @Override
    public int getL() {
        return l;
    }

    @Override
    public int maxAllowNum() {
        return (int) Math.floor((double) cotConfig.maxBaseNum() / l);
    }

    @Override
    public void setEnvType(EnvType envType) {
        cotConfig.setEnvType(envType);
    }

    @Override
    public EnvType getEnvType() {
        return cotConfig.getEnvType();
    }

    @Override
    public SecurityModel getSecurityModel() {
        SecurityModel securityModel = SecurityModel.SEMI_HONEST;
        if (cotConfig.getSecurityModel().compareTo(securityModel) < 0) {
            securityModel = cotConfig.getSecurityModel();
        }
        return securityModel;
    }

    public CotConfig getCotConfig() {
        return cotConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dsz15ZlCoreMtgConfig> {
        /**
         * 乘法三元组比特长度
         */
        private final int l;
        /**
         * COT协议配置项
         */
        private CotConfig cotConfig;

        public Builder(int l) {
            super();
            assert l > 0 : "l must be greater than 0: " + l;
            this.l = l;
            cotConfig = CotFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        public Builder setCotConfig(CotConfig cotConfig) {
            this.cotConfig = cotConfig;
            return this;
        }

        @Override
        public Dsz15ZlCoreMtgConfig build() {
            return new Dsz15ZlCoreMtgConfig(this);
        }
    }
}
