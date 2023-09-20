package edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.cgs22.Cgs22ScpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.psty19.Psty19ScpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21.Rs21ScpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21.Rs21ScpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.scpsi.rs21.Rs21ScpsiServer;

/**
 * server-payload circuit PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public class ScpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ScpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum ScpsiType {
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
    public static <X> ScpsiServer<X> createServer(Rpc serverRpc, Party clientParty, ScpsiConfig config) {
        ScpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19ScpsiServer<>(serverRpc, clientParty, (Psty19ScpsiConfig) config);
            case CGS22:
                return new Cgs22ScpsiServer<>(serverRpc, clientParty, (Cgs22ScpsiConfig) config);
            case RS21:
                return new Rs21ScpsiServer<>(serverRpc, clientParty, (Rs21ScpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ScpsiType.class.getSimpleName() + ": " + type.name());
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
    public static <X> ScpsiClient<X> createClient(Rpc clientRpc, Party serverParty, ScpsiConfig config) {
        ScpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19ScpsiClient<>(clientRpc, serverParty, (Psty19ScpsiConfig) config);
            case CGS22:
                return new Cgs22ScpsiClient<>(clientRpc, serverParty, (Cgs22ScpsiConfig) config);
            case RS21:
                return new Rs21ScpsiClient<>(clientRpc, serverParty, (Rs21ScpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ScpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
