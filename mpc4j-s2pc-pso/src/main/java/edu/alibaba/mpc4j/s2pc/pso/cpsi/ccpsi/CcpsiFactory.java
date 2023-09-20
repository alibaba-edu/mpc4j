package edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiServer;

/**
 * client-payload circuit PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/4/19
 */
public class CcpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private CcpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum CcpsiType {
        /**
         * PSTY19 circuit PSI
         */
        PSTY19,
        /**
         * RS21 circuit PSI
         */
        RS21,
        /**
         * CGS22 circuit PSI
         */
        CGS22,
    }

    /**
     * Creates a server.
     *
     * @param serverRpc   the server RPC.
     * @param clientParty the client party.
     * @param config      the config.
     * @return a server.
     */
    public static <X> CcpsiServer<X> createServer(Rpc serverRpc, Party clientParty, CcpsiConfig config) {
        CcpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19CcpsiServer<>(serverRpc, clientParty, (Psty19CcpsiConfig) config);
            case CGS22:
                return new Cgs22CcpsiServer<>(serverRpc, clientParty, (Cgs22CcpsiConfig) config);
            case RS21:
                return new Rs21CcpsiServer<>(serverRpc, clientParty, (Rs21CcpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CcpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a client.
     *
     * @param clientRpc   the client RPC.
     * @param serverParty the server party.
     * @param config      the config.
     * @return a client.
     */
    public static <X> CcpsiClient<X> createClient(Rpc clientRpc, Party serverParty, CcpsiConfig config) {
        CcpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19CcpsiClient<>(clientRpc, serverParty, (Psty19CcpsiConfig) config);
            case CGS22:
                return new Cgs22CcpsiClient<>(clientRpc, serverParty, (Cgs22CcpsiConfig) config);
            case RS21:
                return new Rs21CcpsiClient<>(clientRpc, serverParty, (Rs21CcpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CcpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static CcpsiConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case TRUSTED_DEALER:
            case SEMI_HONEST:
                return new Psty19CcpsiConfig.Builder(silent).build();
            case COVERT:
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
