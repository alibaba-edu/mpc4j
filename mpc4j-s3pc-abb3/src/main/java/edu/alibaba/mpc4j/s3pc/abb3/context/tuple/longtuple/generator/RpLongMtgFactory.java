package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.env.RpLongEnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.flnw17.Flnw17RpLongMtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.longtuple.generator.flnw17.Flnw17RpLongMtg;

/**
 * factory of replicated 3p sharing zl64 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class RpLongMtgFactory {
    /**
     * Z2 multiplication tuple generator type
     */
    public enum Zl64MtgType {
        /**
         * FLNW+17
         */
        FLNW17,
        /**
         * ABF+17
         */
        ABF17,
    }
    public static RpLongMtg createParty(Rpc rpc, RpLongMtgConfig config, RpLongEnvParty rpLongEnvParty){
        switch (config.getMtgType()){
            case FLNW17:
                return new Flnw17RpLongMtg(rpc, (Flnw17RpLongMtgConfig) config, rpLongEnvParty);
            case ABF17:

            default:
                throw new IllegalArgumentException("Invalid " + Zl64MtgType.class.getSimpleName() + ": " + config.getMtgType().name());
        }
    }

    public static RpLongMtgConfig createDefaultConfig(){
        return new Flnw17RpLongMtgConfig.Builder().build();
    }
}
