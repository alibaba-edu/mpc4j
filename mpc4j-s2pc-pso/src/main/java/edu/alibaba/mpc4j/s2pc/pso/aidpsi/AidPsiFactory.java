package edu.alibaba.mpc4j.s2pc.pso.aidpsi;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiAider;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiClient;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.aidpsi.passive.Kmrs14ShAidPsiServer;

/**
 * aid PSI factory.
 *
 * @author Weiran Liu
 * @date 2023/5/4
 */
public class AidPsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private AidPsiFactory() {
        // empty
    }

    /**
     * aid PSI type.
     */
    public enum AidPsiType {
        /**
         * KMRS14 (semi-honest aider)
         */
        KMRS14_SH_AIDER,
        /**
         * KMRS14 (malicious aider)
         */
        KMRS14_MA_AIDER,
        /**
         * KMRS14 (intersection-size hiding malicious aider)
         */
        KMRS14_SIZE_HIDING_MA_AIDER,
    }

    /**
     * Creates a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param aiderParty  aider party.
     * @param config      config.
     * @return a server.
     */
    public static <X> AidPsiParty<X> createServer(Rpc serverRpc, Party clientParty, Party aiderParty, AidPsiConfig config) {
        AidPsiType type = config.getPtoType();
        switch (type) {
            case KMRS14_SH_AIDER:
                return new Kmrs14ShAidPsiServer<>(serverRpc, clientParty, aiderParty, (Kmrs14ShAidPsiConfig) config);
            case KMRS14_MA_AIDER:
            case KMRS14_SIZE_HIDING_MA_AIDER:
            default:
                throw new IllegalArgumentException("Invalid " + AidPsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param aiderParty  aider party.
     * @param config      config.
     * @return a client.
     */
    public static <X> AidPsiParty<X> createClient(Rpc clientRpc, Party serverParty, Party aiderParty, AidPsiConfig config) {
        AidPsiType type = config.getPtoType();
        switch (type) {
            case KMRS14_SH_AIDER:
                return new Kmrs14ShAidPsiClient<>(clientRpc, serverParty, aiderParty, (Kmrs14ShAidPsiConfig) config);
            case KMRS14_MA_AIDER:
            case KMRS14_SIZE_HIDING_MA_AIDER:
            default:
                throw new IllegalArgumentException("Invalid " + AidPsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates an aider.
     *
     * @param aiderRpc    aider RPC.
     * @param serverParty server party.
     * @param clientParty client party.
     * @param config      config.
     * @return an aider.
     */
    public static AidPsiAider createAider(Rpc aiderRpc, Party serverParty, Party clientParty, AidPsiConfig config) {
        AidPsiType type = config.getPtoType();
        switch (type) {
            case KMRS14_SH_AIDER:
                return new Kmrs14ShAidPsiAider(aiderRpc, serverParty, clientParty, (Kmrs14ShAidPsiConfig) config);
            case KMRS14_MA_AIDER:
            case KMRS14_SIZE_HIDING_MA_AIDER:
            default:
                throw new IllegalArgumentException("Invalid " + AidPsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
