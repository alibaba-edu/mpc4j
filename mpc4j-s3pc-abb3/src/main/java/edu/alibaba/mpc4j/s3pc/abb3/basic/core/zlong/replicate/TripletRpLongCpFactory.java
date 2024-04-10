package edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3LongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongConfig;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.mac.Cgh18RpLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.context.TripletProvider;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.TripletLongParty;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3MaliciousZl64Party;
import edu.alibaba.mpc4j.s3pc.abb3.basic.core.zlong.replicate.aby3.Aby3SemiHonestLongParty;

/**
 * Factory for three-party replicate zl64c party
 *
 * @author Feng Han
 * @date 2024/01/26
 */
public class TripletRpLongCpFactory {
    public enum RpZl64PtoType {
        /**
         * semi-honest version
         */
        SEMI_HONEST,
        /**
         * malicious version with mac
         */
        MALICIOUS_MAC,
        /**
         * malicious version with multiplication tuples
         */
        MALICIOUS_TUPLE
    }
    /**
     * Creates a zl64 party.
     *
     * @param rpc      the RPC.
     * @param config   the config.
     * @param provider context provider
     * @return a zl64 party.
     */
    public static TripletLongParty createParty(Rpc rpc, TripletRpLongConfig config, TripletProvider provider) {
        switch (config.getRpZl64PtoType()) {
            case SEMI_HONEST:
                return new Aby3SemiHonestLongParty(rpc, (Aby3LongConfig) config, provider);
            case MALICIOUS_TUPLE:
                return new Aby3MaliciousZl64Party(rpc, (Aby3LongConfig) config, provider);
            case MALICIOUS_MAC:
                return new Cgh18RpLongParty(rpc, (Cgh18RpLongConfig) config, provider);
            default:
                throw new IllegalArgumentException("Invalid config.getSecurityModel() in creating zl64c party");
        }
    }

    public static TripletRpLongConfig createDefaultConfig(SecurityModel securityModel){
        switch (securityModel){
            case MALICIOUS:
                return new Aby3LongConfig.Builder(true).build();
            case SEMI_HONEST:
                return new Aby3LongConfig.Builder(false).build();
            default:
                throw new IllegalArgumentException("Invalid SecurityModel in creating zl64c config");
        }
    }

    public static TripletRpLongConfig createDefaultMacConfig(SecurityModel securityModel){
        //noinspection SwitchStatementWithTooFewBranches
        switch (securityModel){
            case MALICIOUS:
                return new Cgh18RpLongConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid SecurityModel in creating zl64c config");
        }
    }

}
