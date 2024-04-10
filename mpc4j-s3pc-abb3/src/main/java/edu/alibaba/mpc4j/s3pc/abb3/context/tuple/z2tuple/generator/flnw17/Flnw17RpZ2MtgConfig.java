package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.flnw17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.RpZ2MtgFactory.Z2MtgType;

/**
 * configure of FLNW17 replicated 3p sharing z2 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Flnw17RpZ2MtgConfig extends AbstractMultiPartyPtoConfig implements RpZ2MtgConfig {
    /**
     * the maximum size of one generation for tuples
     */
    private final int numOfResultBalls;
    /**
     * the maximum size of one generation for tuples
     */
    private final int bitInEachBall;

    public Flnw17RpZ2MtgConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        numOfResultBalls = builder.numOfResultBalls;
        bitInEachBall = builder.bitInEachBall;
    }

    @Override
    public Z2MtgType getMtgType() {
        return Z2MtgType.FLNW17;
    }

    @Override
    public int getNumOfResultBalls() {
        return numOfResultBalls;
    }

    public int getBitInEachBall() {
        return bitInEachBall;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Flnw17RpZ2MtgConfig> {
        /**
         * the maximum size of one generation for tuples
         */
        private int numOfResultBalls;
        /**
         * the maximum size of one generation for tuples
         */
        private int bitInEachBall;

        public Builder() {
            numOfResultBalls = 1 << 18;
            bitInEachBall = 256;
        }

        public void setParam(int numOfResultBalls, int bitInEachBall) {
            MathPreconditions.checkGreaterOrEqual("numOfResultBalls >= 1", numOfResultBalls, 1);
            MathPreconditions.checkGreaterOrEqual("bitInEachBall >= 1", bitInEachBall, 1);
            MathPreconditions.checkEqual("1<<LongUtils.ceilLog2(bitInEachBall)", "bitInEachBall",
                1 << LongUtils.ceilLog2(bitInEachBall), bitInEachBall);
            this.numOfResultBalls = numOfResultBalls;
            this.bitInEachBall = bitInEachBall;
        }

        @Override
        public Flnw17RpZ2MtgConfig build() {
            return new Flnw17RpZ2MtgConfig(this);
        }
    }
}
