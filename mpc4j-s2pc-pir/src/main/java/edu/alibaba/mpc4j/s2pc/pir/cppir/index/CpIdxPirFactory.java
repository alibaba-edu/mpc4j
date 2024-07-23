package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiCpIdxPirServer;

/**
 * client-specific preprocessing index PIR factory.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class CpIdxPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private CpIdxPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum CpIdxPirType {
        /**
         * PAI
         */
        PAI,
        /**
         * SPAM
         */
        SPAM,
        /**
         * PIANO
         */
        PIANO,
        /**
         * SIMPLE
         */
        SIMPLE,
        /**
         * DOUBLE
         */
        DOUBLE,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static CpIdxPirServer createServer(Rpc serverRpc, Party clientParty, CpIdxPirConfig config) {
        CpIdxPirType type = config.getPtoType();
        switch (type) {
            case PAI -> {
                return new PaiCpIdxPirServer(serverRpc, clientParty, (PaiCpIdxPirConfig) config);
            }
            case SPAM -> {
                return new SpamCpIdxPirServer(serverRpc, clientParty, (SpamCpIdxPirConfig) config);
            }
            case PIANO -> {
                return new PianoCpIdxPirServer(serverRpc, clientParty, (PianoCpIdxPirConfig) config);
            }
            case SIMPLE -> {
                return new SimpleCpIdxPirServer(serverRpc, clientParty, (SimpleCpIdxPirConfig) config);
            }
            case DOUBLE -> {
                return new DoubleCpIdxPirServer(serverRpc, clientParty, (DoubleCpIdxPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create a client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static CpIdxPirClient createClient(Rpc clientRpc, Party serverParty, CpIdxPirConfig config) {
        CpIdxPirType type = config.getPtoType();
        switch (type) {
            case PAI -> {
                return new PaiCpIdxPirClient(clientRpc, serverParty, (PaiCpIdxPirConfig) config);
            }
            case SPAM -> {
                return new SpamCpIdxPirClient(clientRpc, serverParty, (SpamCpIdxPirConfig) config);
            }
            case PIANO -> {
                return new PianoCpIdxPirClient(clientRpc, serverParty, (PianoCpIdxPirConfig) config);
            }
            case SIMPLE -> {
                return new SimpleCpIdxPirClient(clientRpc, serverParty, (SimpleCpIdxPirConfig) config);
            }
            case DOUBLE -> {
                return new DoubleCpIdxPirClient(clientRpc, serverParty, (DoubleCpIdxPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
