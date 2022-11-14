package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.Zp64CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.Zp64CoreMtgFactory;

import java.math.BigInteger;

/**
 * RSS19-核Zp64三元组生成协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgConfig implements Zp64CoreMtgConfig {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * 模多项式阶
     */
    private final int polyModulusDegree;
    /**
     * 明文模数
     */
    private final long p;

    private Rss19Zp64CoreMtgConfig(Builder builder) {
        polyModulusDegree = builder.polyModulusDegree;
        p = Rss19Zp64CoreMtgNativeUtils.checkCreatePlainModulus(polyModulusDegree, builder.plainModulusSize);
        assert (BigInteger.valueOf(p).bitLength() == builder.plainModulusSize);
    }

    @Override
    public long getZp() {
        return p;
    }

    /**
     * 返回模多项式阶。
     *
     * @return 模多项式阶。
     */
    public int getPolyModulusDegree() {
        return polyModulusDegree;
    }

    @Override
    public int maxAllowNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setEnvType(EnvType envType) {
        if (envType.equals(EnvType.STANDARD_JDK) || envType.equals(EnvType.INLAND_JDK)) {
            throw new IllegalArgumentException("Protocol using " + CommonConstants.MPC4J_NATIVE_FHE_NAME
                + " must not be " + EnvType.STANDARD_JDK.name() + " or " + EnvType.INLAND_JDK.name()
                + ": " + envType.name());
        }
    }

    @Override
    public EnvType getEnvType() {
        return EnvType.STANDARD;
    }

    @Override
    public SecurityModel getSecurityModel() {
        return SecurityModel.SEMI_HONEST;
    }

    @Override
    public Zp64CoreMtgFactory.Zp64CoreMtgType getPtoType() {
        return Zp64CoreMtgFactory.Zp64CoreMtgType.RSS19;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rss19Zp64CoreMtgConfig> {
        /**
         * 明文模数比特长度
         */
        private int plainModulusSize;
        /**
         * 模多项式阶
         */
        private int polyModulusDegree;

        public Builder() {
            this.plainModulusSize = CommonConstants.STATS_BIT_LENGTH;
            this.polyModulusDegree = Rss19Zp64CoreMtgPtoDesc.defaultPolyModulusDegree(this.plainModulusSize);
        }

        public Builder setPolyModulusDegree(int polyModulusDegree, int plainModulusSize) {
            this.polyModulusDegree = polyModulusDegree;
            this.plainModulusSize = plainModulusSize;
            return this;
        }

        public Builder setPlainModulusSize(int plainModulusSize) {
            this.plainModulusSize = plainModulusSize;
            this.polyModulusDegree = Rss19Zp64CoreMtgPtoDesc.defaultPolyModulusDegree(plainModulusSize);
            return this;
        }

        @Override
        public Rss19Zp64CoreMtgConfig build() {
            return new Rss19Zp64CoreMtgConfig(this);
        }
    }
}
