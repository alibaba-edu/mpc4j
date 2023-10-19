package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleCpPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.pai.PaiSingleCpPirServer;

/**
 * Single client-specific preprocessing PIR factory.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class SingleCpPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SingleCpPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum SingleCpPirType {
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
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static SingleCpPirServer createServer(Rpc serverRpc, Party clientParty, SingleCpPirConfig config) {
        SingleCpPirType type = config.getPtoType();
        switch (type) {
            case PAI:
                return new PaiSingleCpPirServer(serverRpc, clientParty, (PaiSingleCpPirConfig) config);
            case SPAM:
                return new SpamSingleCpPirServer(serverRpc, clientParty, (SpamSingleCpPirConfig) config);
            case PIANO:
                return new PianoSingleCpPirServer(serverRpc, clientParty, (PianoSingleCpPirConfig) config);
            case SIMPLE:
                return new SimpleSingleCpPirServer(serverRpc, clientParty, (SimpleSingleCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleCpPirType.class.getSimpleName() + ": " + type.name()
                );
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
    public static SingleCpPirClient createClient(Rpc clientRpc, Party serverParty, SingleCpPirConfig config) {
        SingleCpPirType type = config.getPtoType();
        switch (type) {
            case PAI:
                return new PaiSingleCpPirClient(clientRpc, serverParty, (PaiSingleCpPirConfig) config);
            case SPAM:
                return new SpamSingleCpPirClient(clientRpc, serverParty, (SpamSingleCpPirConfig) config);
            case PIANO:
                return new PianoSingleCpPirClient(clientRpc, serverParty, (PianoSingleCpPirConfig) config);
            case SIMPLE:
                return new SimpleSingleCpPirClient(clientRpc, serverParty, (SimpleSingleCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleCpPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }
}
