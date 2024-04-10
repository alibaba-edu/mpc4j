package edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.env.RpZ2EnvParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.flnw17.Flnw17RpZ2MtgConfig;
import edu.alibaba.mpc4j.s3pc.abb3.context.tuple.z2tuple.generator.flnw17.Flnw17RpZ2Mtg;

/**
 * factory of replicated 3p sharing z2 mt generator
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class RpZ2MtgFactory {
    /**
     * Z2 multiplication tuple generator type
     */
    public enum Z2MtgType {
        /**
         * FLNW+17
         */
        FLNW17,
        /**
         * ABF+17
         */
        ABF17,
    }
    public static RpZ2Mtg createParty(Rpc rpc, RpZ2MtgConfig config, RpZ2EnvParty rpZ2EnvParty){
        switch (config.getMtgType()){
            case FLNW17:
                return new Flnw17RpZ2Mtg(rpc, (Flnw17RpZ2MtgConfig) config, rpZ2EnvParty);
            case ABF17:
            default:
                throw new IllegalArgumentException("Invalid " + Z2MtgType.class.getSimpleName() + ": " + config.getMtgType().name());
        }
    }

    public static RpZ2MtgConfig createDefaultConfig(){
        return new Flnw17RpZ2MtgConfig.Builder().build();
    }
}
