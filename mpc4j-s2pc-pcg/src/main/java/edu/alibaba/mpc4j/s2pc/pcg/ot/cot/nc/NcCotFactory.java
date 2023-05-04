package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.crr21.Crr21NcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.direct.DirectNcCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.nc.ywl20.Ywl20NcCotSender;

/**
 * no-choice COT factory.
 *
 * @author Weiran Liu
 * @date 2022/01/26
 */
public class NcCotFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private NcCotFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum NcCotType {
        /**
         * directly invoke core COT
         */
        DIRECT,
        /**
         * YWL20
         */
        YWL20,
        /**
         * CRR21
         */
        CRR21,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static NcCotSender createSender(Rpc senderRpc, Party receiverParty, NcCotConfig config) {
        NcCotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectNcCotSender(senderRpc, receiverParty, (DirectNcCotConfig) config);
            case YWL20:
                return new Ywl20NcCotSender(senderRpc, receiverParty, (Ywl20NcCotConfig)config);
            case CRR21:
                return new Crr21NcCotSender(senderRpc, receiverParty, (Crr21NcCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + NcCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc the receiver RPC.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static NcCotReceiver createReceiver(Rpc receiverRpc, Party senderParty, NcCotConfig config) {
        NcCotType type = config.getPtoType();
        switch (type) {
            case DIRECT:
                return new DirectNcCotReceiver(receiverRpc, senderParty, (DirectNcCotConfig) config);
            case YWL20:
                return new Ywl20NcCotReceiver(receiverRpc, senderParty, (Ywl20NcCotConfig)config);
            case CRR21:
                return new Crr21NcCotReceiver(receiverRpc, senderParty, (Crr21NcCotConfig)config);
            default:
                throw new IllegalArgumentException("Invalid " + NcCotType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent if using a silent protocol.
     * @return a default config.
     */
    public static NcCotConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        if (silent) {
            return new Ywl20NcCotConfig.Builder(securityModel).build();
        } else {
            return new DirectNcCotConfig.Builder(securityModel).build();
        }
    }
}
