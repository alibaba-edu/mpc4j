package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.flnw17;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.RpLongMtgFactory.Zl64MtgType;

/**
 * configure of FLNW17 replicated 3p sharing zl64 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Flnw17RpLongMtgConfig extends AbstractMultiPartyPtoConfig implements RpLongMtgConfig {
    /**
     * the maximum size of one generation for tuples
     */
    private final int numOfResultBalls;
    /**
     * the maximum size of one generation for tuples
     */
    private final int elementInEachBall;

    public Flnw17RpLongMtgConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        numOfResultBalls = builder.numOfResultBalls;
        elementInEachBall = builder.elementInEachBall;
    }

    @Override
    public Zl64MtgType getMtgType() {
        return Zl64MtgType.FLNW17;
    }

    @Override
    public int getNumOfResultBalls() {
        return numOfResultBalls;
    }

    public int getElementInEachBall() {
        return elementInEachBall;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Flnw17RpLongMtgConfig> {
        /**
         * the maximum size of one generation for tuples
         */
        private int numOfResultBalls;
        /**
         * the maximum size of one generation for tuples
         */
        private int elementInEachBall;

        public Builder() {
            numOfResultBalls = 1 << 15;
            elementInEachBall = 32;
        }

        public void setParam(int numOfResultBalls, int elementInEachBall) {
            MathPreconditions.checkGreaterOrEqual("numOfResultBalls >= 1", numOfResultBalls, 1);
            MathPreconditions.checkGreaterOrEqual("elementInEachBall >= 1", elementInEachBall, 1);
            MathPreconditions.checkEqual("1<<LongUtils.ceilLog2(elementInEachBall)", "elementInEachBall",
                1 << LongUtils.ceilLog2(elementInEachBall), elementInEachBall);
            this.numOfResultBalls = numOfResultBalls;
            this.elementInEachBall = elementInEachBall;
        }

        @Override
        public Flnw17RpLongMtgConfig build() {
            return new Flnw17RpLongMtgConfig(this);
        }
    }
}
