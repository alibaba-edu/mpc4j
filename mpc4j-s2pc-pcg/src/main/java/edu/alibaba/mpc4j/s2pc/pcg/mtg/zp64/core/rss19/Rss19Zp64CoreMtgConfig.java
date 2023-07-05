package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp64.Zp64Factory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.Zp64CoreMtgConfig;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.Zp64CoreMtgFactory;

/**
 * RSS19-核Zp64三元组生成协议配置项。
 *
 * @author Liqiang Peng
 * @date 2022/9/5
 */
public class Rss19Zp64CoreMtgConfig extends AbstractMultiPartyPtoConfig implements Zp64CoreMtgConfig {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * polynomial modulus degree
     */
    private final int polyModulusDegree;
    /**
     * the prime
     */
    private final long p;

    private Rss19Zp64CoreMtgConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        polyModulusDegree = builder.polyModulusDegree;
        p = Rss19Zp64CoreMtgNativeUtils.checkCreatePlainModulus(polyModulusDegree, builder.primeBitLength);
    }

    @Override
    public Zp64 getZp64() {
        return Zp64Factory.createInstance(getEnvType(), p);
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
    public Zp64CoreMtgFactory.Zp64CoreMtgType getPtoType() {
        return Zp64CoreMtgFactory.Zp64CoreMtgType.RSS19;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Rss19Zp64CoreMtgConfig> {
        /**
         * 明文模数比特长度
         */
        private final int primeBitLength;
        /**
         * 模多项式阶
         */
        private int polyModulusDegree;

        public Builder(int l) {
            MathPreconditions.checkPositive("l", 1);
            primeBitLength = l + 1;
            polyModulusDegree = Rss19Zp64CoreMtgPtoDesc.defaultPolyModulusDegree(primeBitLength);
        }

        public Builder setPolyModulusDegree(int polyModulusDegree) {
            this.polyModulusDegree = polyModulusDegree;
            return this;
        }

        @Override
        public Rss19Zp64CoreMtgConfig build() {
            return new Rss19Zp64CoreMtgConfig(this);
        }
    }
}
