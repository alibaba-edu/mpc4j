package edu.alibaba.mpc4j.s2pc.pir.index;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.fastpir.Ayaa21IndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.onionpir.Mcr21IndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.sealpir.Acls18IndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.vectorizedpir.Mr23IndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.index.xpir.Mbfk16IndexPirServer;

/**
 * 索引PIR协议工厂。
 *
 * @author Liqiang Peng
 * @date 2022/8/24
 */
public class IndexPirFactory implements PtoFactory {
    /**
     * 私有构造函数
     */
    private IndexPirFactory() {
        // empty
    }

    /**
     * 索引PIR协议类型。
     */
    public enum IndexPirType {
        /**
         * XPIR
         */
        XPIR,
        /**
         * SealPIR
         */
        SEAL_PIR,
        /**
         * OnionPIR
         */
        ONION_PIR,
        /**
         * FastPIR
         */
        FAST_PIR,
        /**
         * Vectorized PIR
         */
        VECTORIZED_PIR,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static IndexPirServer createServer(Rpc serverRpc, Party clientParty, IndexPirConfig config) {
        IndexPirType type = config.getProType();
        switch (type) {
            case XPIR:
                return new Mbfk16IndexPirServer(serverRpc, clientParty, (Mbfk16IndexPirConfig) config);
            case SEAL_PIR:
                return new Acls18IndexPirServer(serverRpc, clientParty, (Acls18IndexPirConfig) config);
            case ONION_PIR:
                return new Mcr21IndexPirServer(serverRpc, clientParty, (Mcr21IndexPirConfig) config);
            case FAST_PIR:
                return new Ayaa21IndexPirServer(serverRpc, clientParty, (Ayaa21IndexPirConfig) config);
            case VECTORIZED_PIR:
                return new Mr23IndexPirServer(serverRpc, clientParty, (Mr23IndexPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + IndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc   客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @return 客户端。
     */
    public static IndexPirClient createClient(Rpc clientRpc, Party serverParty, IndexPirConfig config) {
        IndexPirType type = config.getProType();
        switch (type) {
            case XPIR:
                return new Mbfk16IndexPirClient(clientRpc, serverParty, (Mbfk16IndexPirConfig) config);
            case SEAL_PIR:
                return new Acls18IndexPirClient(clientRpc, serverParty, (Acls18IndexPirConfig) config);
            case ONION_PIR:
                return new Mcr21IndexPirClient(clientRpc, serverParty, (Mcr21IndexPirConfig) config);
            case FAST_PIR:
                return new Ayaa21IndexPirClient(clientRpc, serverParty, (Ayaa21IndexPirConfig) config);
            case VECTORIZED_PIR:
                return new Mr23IndexPirClient(clientRpc, serverParty, (Mr23IndexPirConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + IndexPirType.class.getSimpleName() + ": " + type.name());
        }
    }
}
