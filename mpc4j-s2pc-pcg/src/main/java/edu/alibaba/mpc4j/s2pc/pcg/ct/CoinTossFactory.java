package edu.alibaba.mpc4j.s2pc.pcg.ct;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ct.blum82.Blum82CoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.blum82.Blum82CoinTossReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ct.blum82.Blum82CoinTossSender;
import edu.alibaba.mpc4j.s2pc.pcg.ct.direct.DirectCoinTossConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ct.direct.DirectCoinTossReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ct.direct.DirectCoinTossSender;

/**
 * coin-tossing protocol factory.
 *
 * @author Weiran Liu
 * @date 2023/5/6
 */
public class CoinTossFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private CoinTossFactory() {
        // empty
    }

    /**
     * protocol type.
     */
    public enum CoinTossType {
        /**
         * direct (semi-honest security)
         */
        DIRECT,
        /**
         * BLUM82 (malicious security)
         */
        BLUM82,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static CoinTossParty createSender(Rpc senderRpc, Party receiverParty, CoinTossConfig config) {
        CoinTossType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectCoinTossSender(senderRpc, receiverParty, (DirectCoinTossConfig) config);
            case BLUM82:
                return new Blum82CoinTossSender(senderRpc, receiverParty, (Blum82CoinTossConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CoinTossType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static CoinTossParty createReceiver(Rpc receiverRpc, Party senderParty, CoinTossConfig config) {
        CoinTossType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectCoinTossReceiver(receiverRpc, senderParty, (DirectCoinTossConfig) config);
            case BLUM82:
                return new Blum82CoinTossReceiver(receiverRpc, senderParty, (Blum82CoinTossConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + CoinTossType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @return a default config.
     */
    public static CoinTossConfig createDefaultConfig(SecurityModel securityModel) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new DirectCoinTossConfig.Builder().build();
            case COVERT:
            case MALICIOUS:
                return new Blum82CoinTossConfig.Builder().build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
