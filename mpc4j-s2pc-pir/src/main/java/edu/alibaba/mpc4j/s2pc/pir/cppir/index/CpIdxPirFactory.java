package edu.alibaba.mpc4j.s2pc.pir.cppir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo.FrodoCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo.FrodoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.frodo.FrodoCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.plinko.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.mir.MirCpIdxPirServer;
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
         * Frodo
         */
        FRODO,
        /**
         * PAI
         */
        PAI,
        /**
         * MIR
         */
        MIR,
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
        /**
         * Piano-based Plinko
         */
        PIANO_PLINKO,
        /**
         * MIR-based Plinko
         */
        MIR_PLINKO,
    }

    /**
     * Gets support query num for each round.
     * <ul>
     * <li>For stream client-preprocessing PIR, this returns Q so that after Q queries the offline phase runs again. </li>
     * <li>For hint client-preprocessing PIR, this return <code>Integer.MAX_VALUE</code>.</li>
     * </ul>
     *
     * @param type type.
     * @param n    database size.
     * @return support query num for each round.
     */
    public static int supportRoundQueryNum(CpIdxPirType type, int n) {
        return switch (type) {
            case FRODO, SIMPLE, DOUBLE, PAI -> Integer.MAX_VALUE;
            case PIANO -> PianoCpIdxPirUtils.getRoundQueryNum(n);
            case MIR -> MirCpIdxPirUtils.getRoundQueryNum(n);
            case PIANO_PLINKO -> PianoPlinkoCpIdxPirUtils.getRoundQueryNum(n);
            case MIR_PLINKO -> MirPlinkoCpIdxPirUtils.getRoundQueryNum(n);
        };
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
            case FRODO -> {
                return new FrodoCpIdxPirServer(serverRpc, clientParty, (FrodoCpIdxPirConfig) config);
            }
            case PAI -> {
                return new PaiCpIdxPirServer(serverRpc, clientParty, (PaiCpIdxPirConfig) config);
            }
            case MIR -> {
                return new MirCpIdxPirServer(serverRpc, clientParty, (MirCpIdxPirConfig) config);
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
            case PIANO_PLINKO -> {
                return new PianoPlinkoCpIdxPirServer(serverRpc, clientParty, (PianoPlinkoCpIdxPirConfig) config);
            }
            case MIR_PLINKO -> {
                return new MirPlinkoCpIdxPirServer(serverRpc, clientParty, (MirPlinkoCpIdxPirConfig) config);
            }
            default ->
                throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
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
            case FRODO -> {
                return new FrodoCpIdxPirClient(clientRpc, serverParty, (FrodoCpIdxPirConfig) config);
            }
            case PAI -> {
                return new PaiCpIdxPirClient(clientRpc, serverParty, (PaiCpIdxPirConfig) config);
            }
            case MIR -> {
                return new MirCpIdxPirClient(clientRpc, serverParty, (MirCpIdxPirConfig) config);
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
            case PIANO_PLINKO -> {
                return new PianoPlinkoCpIdxPirClient(clientRpc, serverParty, (PianoPlinkoCpIdxPirConfig) config);
            }
            case MIR_PLINKO -> {
                return new MirPlinkoCpIdxPirClient(clientRpc, serverParty, (MirPlinkoCpIdxPirConfig) config);
            }
            default ->
                throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create an updatable server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static StreamCpIdxPirServer createUpdatableServer(Rpc serverRpc, Party clientParty, CpIdxPirConfig config) {
        CpIdxPirType type = config.getPtoType();
        switch (type) {
            case PIANO -> {
                return new PianoCpIdxPirServer(serverRpc, clientParty, (PianoCpIdxPirConfig) config);
            }
            case MIR -> {
                return new MirCpIdxPirServer(serverRpc, clientParty, (MirCpIdxPirConfig) config);
            }
            case PIANO_PLINKO -> {
                return new PianoPlinkoCpIdxPirServer(serverRpc, clientParty, (PianoPlinkoCpIdxPirConfig) config);
            }
            case MIR_PLINKO -> {
                return new MirPlinkoCpIdxPirServer(serverRpc, clientParty, (MirPlinkoCpIdxPirConfig) config);
            }
            default ->
                throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create an updatable client.
     *
     * @param clientRpc   client RPC.
     * @param serverParty server party.
     * @param config      config.
     * @return a client.
     */
    public static StreamCpIdxPirClient createStreamClient(Rpc clientRpc, Party serverParty, CpIdxPirConfig config) {
        CpIdxPirType type = config.getPtoType();
        switch (type) {
            case PIANO -> {
                return new PianoCpIdxPirClient(clientRpc, serverParty, (PianoCpIdxPirConfig) config);
            }
            case MIR -> {
                return new MirCpIdxPirClient(clientRpc, serverParty, (MirCpIdxPirConfig) config);
            }
            case PIANO_PLINKO -> {
                return new PianoPlinkoCpIdxPirClient(clientRpc, serverParty, (PianoPlinkoCpIdxPirConfig) config);
            }
            case MIR_PLINKO -> {
                return new MirPlinkoCpIdxPirClient(clientRpc, serverParty, (MirPlinkoCpIdxPirConfig) config);
            }
            default ->
                throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create a default configure based on type
     *
     * @param type type of pir
     */
    public static CpIdxPirConfig createDefaultConfig(CpIdxPirType type) {
        switch (type) {
            case FRODO -> {
                return new FrodoCpIdxPirConfig.Builder().build();
            }
            case PAI -> {
                return new PaiCpIdxPirConfig.Builder().build();
            }
            case MIR -> {
                return new MirCpIdxPirConfig.Builder().build();
            }
            case PIANO -> {
                return new PianoCpIdxPirConfig.Builder().build();
            }
            case SIMPLE -> {
                return new SimpleCpIdxPirConfig.Builder().build();
            }
            case DOUBLE -> {
                return new DoubleCpIdxPirConfig.Builder().build();
            }
            case PIANO_PLINKO -> {
                return new PianoPlinkoCpIdxPirConfig.Builder().build();
            }
            case MIR_PLINKO -> {
                return new MirPlinkoCpIdxPirConfig.Builder().build();
            }
            default ->
                throw new IllegalArgumentException("Invalid " + CpIdxPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
