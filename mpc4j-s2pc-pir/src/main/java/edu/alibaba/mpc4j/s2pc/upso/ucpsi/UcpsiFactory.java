package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.cgs22.Cgs22UcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiServer;

/**
 * Unbalanced Circuit PSI factory.
 *
 * @author Liqiang Peng
 * @date 2023/4/17
 */
public class UcpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private UcpsiFactory() {
        // empty
    }

    /**
     * the type
     */
    public enum UcpsiType {
        /**
         * PSTY19 circuit PSI
         */
        PSTY19,
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
    public static <X> UcpsiServer<X> createServer(Rpc serverRpc, Party clientParty, UcpsiConfig config) {
        UcpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19UcpsiServer<>(serverRpc, clientParty, (Psty19UcpsiConfig) config);
            case CGS22:
                return new Cgs22UcpsiServer<>(serverRpc, clientParty, (Cgs22UcpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + type.name());
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
    public static <X> UcpsiClient<X> createClient(Rpc clientRpc, Party serverParty, UcpsiConfig config) {
        UcpsiType type = config.getPtoType();
        switch (type) {
            case PSTY19:
                return new Psty19UcpsiClient<>(clientRpc, serverParty, (Psty19UcpsiConfig) config);
            case CGS22:
                return new Cgs22UcpsiClient<>(clientRpc, serverParty, (Cgs22UcpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
