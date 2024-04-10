package edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.replicate;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.z2.TripletZ2cParty;

/**
 * Replicated z2 sharing party factory.
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class Aby3Z2cFactory implements PtoFactory {
    /**
     * Creates a z2c party.
     *
     * @param rpc      the RPC.
     * @param config   the config.
     * @param provider context provider
     * @return a z2c party.
     */
    public static TripletZ2cParty createParty(Rpc rpc, Aby3Z2cConfig config, TripletProvider provider) {
        switch (config.getSecurityModel()) {
            case SEMI_HONEST:
                return new Aby3SemiHonestZ2cParty(rpc, config, provider);
            case MALICIOUS:
                return new Aby3MaliciousZ2cParty(rpc, config, provider);
            default:
                throw new IllegalArgumentException("Invalid config.getSecurityModel() in creating aby3 z2c party");
        }
    }

    public static Aby3Z2cConfig createDefaultConfig(SecurityModel securityModel){
        switch (securityModel){
            case MALICIOUS:
                return new Aby3Z2cConfig.Builder(true).build();
            case SEMI_HONEST:
                return new Aby3Z2cConfig.Builder(false).build();
            default:
                throw new IllegalArgumentException("Invalid SecurityModel in creating aby3 z2c config");
        }
    }
}
