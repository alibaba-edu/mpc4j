package edu.alibaba.mpc4j.s2pc.pir.stdpir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.IdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.cw.CwStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.mul.MulStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.onion.OnionStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.pbc.PbcStdIdxPirServer;
//import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirClient;
//import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirConfig;
//import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal.SealStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.seal4j.SealStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.vectorized.VectorizedStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.xpir.XpirStdIdxPirServer;

/**
 * standard index PIR factory.
 *
 * @author Weiran Liu
 * @date 2024/7/9
 */
public class StdIdxPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private StdIdxPirFactory() {
        // empty
    }

    /**
     * standard index PIR type
     */
    public enum StdIdxPirType {
        /**
         * XPIR
         */
        XPIR,
        /**
         * Seal PIR
         */
        SEAL,
        /**
         * Onion PIR
         */
        ONION,
        /**
         * Fast PIR
         */
        FAST,
        /**
         * Vectorized PIR
         */
        VECTOR,
        /**
         * MulPIR
         */
        MUL,
        /**
         * Constant-Weight PIR
         */
        CW,
        /**
         * probabilistic batch code (PBC) index PIR
         */
        PBC,
    }

    /**
     * create single index PIR server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return single index PIR server.
     */
    public static IdxPirServer createServer(Rpc serverRpc, Party clientParty, StdIdxPirConfig config) {
        StdIdxPirType type = config.getProType();
        switch (type) {
            case XPIR -> {
                return new XpirStdIdxPirServer(serverRpc, clientParty, (XpirStdIdxPirConfig) config);
            }
            case SEAL -> {
                return new SealStdIdxPirServer(serverRpc, clientParty, (SealStdIdxPirConfig) config);
            }
            case MUL -> {
                return new MulStdIdxPirServer(serverRpc, clientParty, (MulStdIdxPirConfig) config);
            }
            case ONION -> {
                return new OnionStdIdxPirServer(serverRpc, clientParty, (OnionStdIdxPirConfig) config);
            }
            case VECTOR -> {
                return new VectorizedStdIdxPirServer(serverRpc, clientParty, (VectorizedStdIdxPirConfig) config);
            }
            case FAST -> {
                return new FastStdIdxPirServer(serverRpc, clientParty, (FastStdIdxPirConfig) config);
            }
            case CW -> {
                return new CwStdIdxPirServer(serverRpc, clientParty, (CwStdIdxPirConfig) config);
            }
            case PBC -> {
                return new PbcStdIdxPirServer(serverRpc, clientParty, (PbcStdIdxPirConfig) config);
            }
            default -> throw new IllegalArgumentException(
                "Invalid " + StdIdxPirType.class.getSimpleName() + ": " + type.name()
            );
        }
    }

    /**
     * create single index PIR client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return single index PIR client.
     */
    public static IdxPirClient createClient(Rpc clientRpc, Party serverParty, StdIdxPirConfig config) {
        StdIdxPirType type = config.getProType();
        switch (type) {
            case XPIR -> {
                return new XpirStdIdxPirClient(clientRpc, serverParty, (XpirStdIdxPirConfig) config);
            }
            case SEAL -> {
                return new SealStdIdxPirClient(clientRpc, serverParty, (SealStdIdxPirConfig) config);
            }
            case MUL -> {
                return new MulStdIdxPirClient(clientRpc, serverParty, (MulStdIdxPirConfig) config);
            }
            case ONION -> {
                return new OnionStdIdxPirClient(clientRpc, serverParty, (OnionStdIdxPirConfig) config);
            }
            case VECTOR -> {
                return new VectorizedStdIdxPirClient(clientRpc, serverParty, (VectorizedStdIdxPirConfig) config);
            }
            case FAST -> {
                return new FastStdIdxPirClient(clientRpc, serverParty, (FastStdIdxPirConfig) config);
            }
            case CW -> {
                return new CwStdIdxPirClient(clientRpc, serverParty, (CwStdIdxPirConfig) config);
            }
            case PBC -> {
                return new PbcStdIdxPirClient(clientRpc, serverParty, (PbcStdIdxPirConfig) config);
            }
            default -> throw new IllegalArgumentException(
                "Invalid " + StdIdxPirType.class.getSimpleName() + ": " + type.name()
            );
        }
    }

    /**
     * create PBC index PIR server.
     *
     * @param serverRpc   server rpc.
     * @param clientParty client party.
     * @param config      config.
     * @return single index PIR server.
     */
    public static PbcableStdIdxPirServer createPbcableServer(Rpc serverRpc, Party clientParty, PbcableStdIdxPirConfig config) {
        StdIdxPirType type = config.getProType();
        switch (type) {
            case XPIR -> {
                return new XpirStdIdxPirServer(serverRpc, clientParty, (XpirStdIdxPirConfig) config);
            }
            case SEAL -> {
                return new SealStdIdxPirServer(serverRpc, clientParty, (SealStdIdxPirConfig) config);
            }
            case MUL -> {
                return new MulStdIdxPirServer(serverRpc, clientParty, (MulStdIdxPirConfig) config);
            }
            case ONION -> {
                return new OnionStdIdxPirServer(serverRpc, clientParty, (OnionStdIdxPirConfig) config);
            }
            case FAST -> {
                return new FastStdIdxPirServer(serverRpc, clientParty, (FastStdIdxPirConfig) config);
            }
            case CW -> {
                return new CwStdIdxPirServer(serverRpc, clientParty, (CwStdIdxPirConfig) config);
            }
            default -> throw new IllegalArgumentException(
                "Invalid " + StdIdxPirType.class.getSimpleName() + ": " + type.name()
            );
        }
    }

    /**
     * create PBC index PIR client.
     *
     * @param clientRpc   client rpc.
     * @param serverParty server party.
     * @param config      config.
     * @return single index PIR client.
     */
    public static PbcableStdIdxPirClient createPbcableClient(Rpc clientRpc, Party serverParty, PbcableStdIdxPirConfig config) {
        StdIdxPirType type = config.getProType();
        switch (type) {
            case XPIR -> {
                return new XpirStdIdxPirClient(clientRpc, serverParty, (XpirStdIdxPirConfig) config);
            }
            case SEAL -> {
                return new SealStdIdxPirClient(clientRpc, serverParty, (SealStdIdxPirConfig) config);
            }
            case MUL -> {
                return new MulStdIdxPirClient(clientRpc, serverParty, (MulStdIdxPirConfig) config);
            }
            case ONION -> {
                return new OnionStdIdxPirClient(clientRpc, serverParty, (OnionStdIdxPirConfig) config);
            }
            case FAST -> {
                return new FastStdIdxPirClient(clientRpc, serverParty, (FastStdIdxPirConfig) config);
            }
            case CW -> {
                return new CwStdIdxPirClient(clientRpc, serverParty, (CwStdIdxPirConfig) config);
            }
            default -> throw new IllegalArgumentException(
                "Invalid " + StdIdxPirType.class.getSimpleName() + ": " + type.name()
            );
        }
    }
}
