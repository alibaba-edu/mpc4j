package edu.alibaba.mpc4j.dp.service.heavyhitter.config;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.heavyhitter.HhLdpFactory.HhLdpType;

import java.util.Set;

/**
 * Basic Heavy Hitter LDP config.
 *
 * @author Weiran Liu
 * @date 2023/1/5
 */
public class BasicHhLdpConfig implements HhLdpConfig {
    /**
     * the type
     */
    private final HhLdpType type;
    /**
     * the domain set
     */
    private final Set<String> domainSet;
    /**
     * the domain size d
     */
    private final int d;
    /**
     * the number of Heavy Hitters k
     */
    private final int k;
    /**
     * the privacy parameter ε / w
     */
    private final double windowEpsilon;

    protected BasicHhLdpConfig(Builder builder) {
        type = builder.type;
        domainSet = builder.domainSet;
        d = builder.d;
        k = builder.k;
        windowEpsilon = builder.windowEpsilon;
    }

    @Override
    public HhLdpType getType() {
        return type;
    }

    @Override
    public Set<String> getDomainSet() {
        return domainSet;
    }

    @Override
    public int getD() {
        return d;
    }

    @Override
    public int getK() {
        return k;
    }

    @Override
    public double getWindowEpsilon() {
        return windowEpsilon;
    }

    @Override
    public boolean isConverge() {
        return true;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<BasicHhLdpConfig> {
        /**
         * the type
         */
        protected final HhLdpType type;
        /**
         * the domain set
         */
        protected final Set<String> domainSet;
        /**
         * the domain size d
         */
        private final int d;
        /**
         * the number of Heavy Hitters k
         */
        protected final int k;
        /**
         * the privacy parameter ε / w
         */
        protected final double windowEpsilon;

        public Builder(HhLdpType type, Set<String> domainSet, int k, double windowEpsilon) {
            this.type = type;
            d = domainSet.size();
            MathPreconditions.checkGreater("|Ω|", d, 1);
            this.domainSet = domainSet;
            MathPreconditions.checkPositiveInRangeClosed("k", k, d);
            this.k = k;
            MathPreconditions.checkPositive("ε / w", windowEpsilon);
            this.windowEpsilon = windowEpsilon;
        }

        @Override
        public BasicHhLdpConfig build() {
            return new BasicHhLdpConfig(this);
        }
    }
}
