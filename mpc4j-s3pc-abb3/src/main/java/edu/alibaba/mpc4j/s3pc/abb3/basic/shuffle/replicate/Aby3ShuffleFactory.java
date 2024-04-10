package edu.alibaba.mpc4j.s3pc.abb3.basic.shuffle.replicate;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;

/**
 * Replicated-sharing Shuffling party factory.
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public class Aby3ShuffleFactory implements PtoFactory {
    /**
     * Creates a type conversion party.
     *
     * @param config   the config.
     * @param z2cParty z2c party
     * @param zl64cParty zl64c party
     * @return a z2c party.
     */
    public static Aby3ShuffleParty createParty(Aby3ShuffleConfig config, TripletZ2cParty z2cParty, TripletLongParty zl64cParty, Cgh18RpLongParty macParty) {
        switch (config.getSecurityModel()) {
            case SEMI_HONEST:
                return new Aby3ShShuffleParty(z2cParty, zl64cParty, config);
            case MALICIOUS:
                return new Aby3MalShuffleParty(z2cParty, zl64cParty, macParty, config);
            default:
                throw new IllegalArgumentException("Invalid config.getSecurityModel() in creating aby3 shuffle party");
        }
    }

    public static Aby3ShuffleConfig createDefaultConfig(SecurityModel securityModel){
        switch (securityModel) {
            case SEMI_HONEST:
                return new Aby3ShuffleConfig.Builder(false).build();
            case MALICIOUS:
                return new Aby3ShuffleConfig.Builder(true).build();
            default:
                throw new IllegalArgumentException("Invalid securityModel in creating aby3 conversion config");
        }
    }
}
