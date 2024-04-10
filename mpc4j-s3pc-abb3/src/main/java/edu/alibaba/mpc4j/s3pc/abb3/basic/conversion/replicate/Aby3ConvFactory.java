package edu.alibaba.mpc4j.s3pc.abb3.basic.conversion.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;

/**
 * Replicated-sharing type conversion party factory.
 *
 * @author Feng Han
 * @date 2024/01/17
 */
public class Aby3ConvFactory implements PtoFactory {
    /**
     * Creates a type conversion party.
     *
     * @param config   the config.
     * @param z2cParty z2c party
     * @param zl64cParty zl64c party
     * @return a z2c party.
     */
    public static Aby3ConvParty createParty(Aby3ConvConfig config, TripletZ2cParty z2cParty, TripletLongParty zl64cParty) {
        switch (config.getSecurityModel()) {
            case SEMI_HONEST:
                return new Aby3ShConvParty(z2cParty, zl64cParty, config);
            case MALICIOUS:
                return new Aby3MalConvParty(z2cParty, zl64cParty, config);
            default:
                throw new IllegalArgumentException("Invalid config.getSecurityModel() in creating aby3 conversion party");
        }
    }

    public static Aby3ConvConfig createDefaultConfig(SecurityModel securityModel){
        switch (securityModel) {
            case SEMI_HONEST:
                return new Aby3ConvConfig.Builder(false).build();
            case MALICIOUS:
                return new Aby3ConvConfig.Builder(true).build();
            default:
                throw new IllegalArgumentException("Invalid securityModel in creating aby3 conversion config");
        }
    }
}
