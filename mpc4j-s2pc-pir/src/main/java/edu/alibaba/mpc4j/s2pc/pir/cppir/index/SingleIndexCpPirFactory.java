package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPsiClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoSingleIndexCpPsiServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPsiClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.spam.SpamSingleIndexCpPsiServer;

/**
 * Single Index Client-specific Preprocessing PIR factory.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public class SingleIndexCpPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SingleIndexCpPirFactory() {
        // empty
    }

    /**
     * Single Index Client-specific Preprocessing PIR type
     */
    public enum SingleIndexCpPirType {
        /**
         * ZPSZ23 (PIANO)
         */
        ZPSZ23_PIANO,
        /**
         * MIR23 (SPAM)
         */
        MIR23_SPAM,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static SingleIndexCpPirServer createServer(Rpc serverRpc, Party clientParty, SingleIndexCpPirConfig config) {
        SingleIndexCpPirType type = config.getProType();
        switch (type) {
            case ZPSZ23_PIANO:
                return new PianoSingleIndexCpPsiServer(serverRpc, clientParty, (PianoSingleIndexCpPirConfig) config);
            case MIR23_SPAM:
                return new SpamSingleIndexCpPsiServer(serverRpc, clientParty, (SpamSingleIndexCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexCpPirType.class.getSimpleName() + ": " + type.name()
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
    public static SingleIndexCpPirClient createClient(Rpc clientRpc, Party serverParty, SingleIndexCpPirConfig config) {
        SingleIndexCpPirType type = config.getProType();
        switch (type) {
            case ZPSZ23_PIANO:
                return new PianoSingleIndexCpPsiClient(clientRpc, serverParty, (PianoSingleIndexCpPirConfig) config);
            case MIR23_SPAM:
                return new SpamSingleIndexCpPsiClient(clientRpc, serverParty, (SpamSingleIndexCpPirConfig) config);
            default:
                throw new IllegalArgumentException(
                    "Invalid " + SingleIndexCpPirType.class.getSimpleName() + ": " + type.name()
                );
        }
    }
}
