package edu.alibaba.mpc4j.s2pc.upso.ucpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.psty19.Psty19UcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.pdsm.Sj23PdsmUcpsiServer;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiClient;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiConfig;
import edu.alibaba.mpc4j.s2pc.upso.ucpsi.sj23.peqt.Sj23PeqtUcpsiServer;

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
         * SJ23 circuit PSI construction 1
         */
        SJ23_PEQT,
        /**
         * SJ23 circuit PSI construction 2
         */
        SJ23_PDSM,
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
            case SJ23_PEQT:
                return new Sj23PeqtUcpsiServer<>(serverRpc, clientParty, (Sj23PeqtUcpsiConfig) config);
            case SJ23_PDSM:
                return new Sj23PdsmUcpsiServer<>(serverRpc, clientParty, (Sj23PdsmUcpsiConfig) config);
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
            case SJ23_PEQT:
                return new Sj23PeqtUcpsiClient<>(clientRpc, serverParty, (Sj23PeqtUcpsiConfig) config);
            case SJ23_PDSM:
                return new Sj23PdsmUcpsiClient<>(clientRpc, serverParty, (Sj23PdsmUcpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UcpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
