package edu.alibaba.mpc4j.s2pc.pir.cppir.ks;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.alpr21.Alpr21CpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.chalamet.ChalametCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.pai.PaiCpCksPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimplePgmCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirServer;

/**
 * client-specific preprocessing KSPIR factory.
 *
 * @author Liqiang Peng
 * @date 2023/9/14
 */
public class CpKsPirFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private CpKsPirFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum CpKsPirType {
        /**
         * PAI (CKS)
         */
        PAI_CKS,
        /**
         * ALPR21
         */
        ALPR21,
        /**
         * Chalamet
         */
        CHALAMET,
        /**
         * simple naive
         */
        SIMPLE_NAIVE,
        /**
         * simple bin
         */
        SIMPLE_BIN,
        /**
         * PGM-index
         */
        PGM_INDEX,
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param clientParty client party.
     * @param config      config.
     * @return a server.
     */
    public static <T> CpKsPirServer<T> createServer(Rpc serverRpc, Party clientParty, CpKsPirConfig config) {
        CpKsPirType type = config.getPtoType();
        switch (type) {
            case PAI_CKS -> {
                return new PaiCpCksPirServer<>(serverRpc, clientParty, (PaiCpCksPirConfig) config);
            }
            case ALPR21 -> {
                return new Alpr21CpKsPirServer<>(serverRpc, clientParty, (Alpr21CpKsPirConfig) config);
            }
            case CHALAMET -> {
                return new ChalametCpKsPirServer<>(serverRpc, clientParty, (ChalametCpKsPirConfig) config);
            }
            case SIMPLE_NAIVE -> {
                return new SimpleNaiveCpKsPirServer<>(serverRpc, clientParty, (SimpleNaiveCpKsPirConfig) config);
            }
            case SIMPLE_BIN -> {
                return new SimpleBinCpKsPirServer<>(serverRpc, clientParty, (SimpleBinCpKsPirConfig) config);
            }
            case PGM_INDEX -> {
                return new SimplePgmCpKsPirServer<>(serverRpc, clientParty, (SimplePgmCpKsPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + CpKsPirType.class.getSimpleName() + ": " + type.name());
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
    public static <T> CpKsPirClient<T> createClient(Rpc clientRpc, Party serverParty, CpKsPirConfig config) {
        CpKsPirType type = config.getPtoType();
        switch (type) {
            case PAI_CKS -> {
                return new PaiCpCksPirClient<>(clientRpc, serverParty, (PaiCpCksPirConfig) config);
            }
            case ALPR21 -> {
                return new Alpr21CpKsPirClient<>(clientRpc, serverParty, (Alpr21CpKsPirConfig) config);
            }
            case CHALAMET -> {
                return new ChalametCpKsPirClient<>(clientRpc, serverParty, (ChalametCpKsPirConfig) config);
            }
            case SIMPLE_NAIVE -> {
                return new SimpleNaiveCpKsPirClient<>(clientRpc, serverParty, (SimpleNaiveCpKsPirConfig) config);
            }
            case SIMPLE_BIN -> {
                return new SimpleBinCpKsPirClient<>(clientRpc, serverParty, (SimpleBinCpKsPirConfig) config);
            }
            case PGM_INDEX -> {
                return new SimplePgmCpKsPirClient<>(clientRpc, serverParty, (SimplePgmCpKsPirConfig) config);
            }
            default -> throw new IllegalArgumentException("Invalid " + CpKsPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
