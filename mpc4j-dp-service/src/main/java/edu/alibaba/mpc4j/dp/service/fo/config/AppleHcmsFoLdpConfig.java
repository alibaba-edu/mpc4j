package edu.alibaba.mpc4j.dp.service.fo.config;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.service.fo.FoLdpFactory;

import java.util.Random;
import java.util.Set;

/**
 * Apple's Hadamard Count Mean Sketch (HCMS) Frequency Oracle LDP config.
 * The only difference between Apple's CMS and Apple's HCMS config is that we require m = 2^k.
 *
 * @author Weiran Liu
 * @date 2023/2/1
 */
public class AppleHcmsFoLdpConfig extends AppleCmsFoLdpConfig {

    private AppleHcmsFoLdpConfig(Builder builder) {
        super(builder);
    }

    public static class Builder extends AppleCmsFoLdpConfig.Builder {

        public Builder(FoLdpFactory.FoLdpType type, Set<String> domainSet, double epsilon) {
            super(type, domainSet, epsilon);
        }

        @Override
        public Builder setHashes(int k, int m, Random random) {
            MathPreconditions.checkGreaterOrEqual("output bound of hash functions", m, 2);
            Preconditions.checkArgument((m & (m - 1)) == 0, "m must be a power of 2: %s", m);
            super.setHashes(k, m, random);
            return this;
        }

        @Override
        public AppleHcmsFoLdpConfig build() {
            return new AppleHcmsFoLdpConfig(this);
        }
    }
}
