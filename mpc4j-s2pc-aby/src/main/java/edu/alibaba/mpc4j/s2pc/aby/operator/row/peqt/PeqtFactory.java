package edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.cgs22.Cgs22PeqtSender;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.naive.NaivePeqtSender;

/**
 * private equality test factory.
 *
 * @author Weiran Liu
 * @date 2023/4/14
 */
public class PeqtFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PeqtFactory() {
        // empty
    }

    /**
     * type
     */
    public enum PeqtType {
        /**
         * naive implementation, bit-wise operations.
         */
        NAIVE,
        /**
         * CGS22
         */
        CGS22,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PeqtParty createSender(Rpc senderRpc, Party receiverParty, PeqtConfig config) {
        PeqtType type = config.getPtoType();
        switch (type) {
            case NAIVE:
                return new NaivePeqtSender(senderRpc, receiverParty, (NaivePeqtConfig) config);
            case CGS22:
                return new Cgs22PeqtSender(senderRpc, receiverParty, (Cgs22PeqtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PeqtType.class.getSimpleName() + ": " + type.name());
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
    public static PeqtParty createReceiver(Rpc receiverRpc, Party senderParty, PeqtConfig config) {
        PeqtType type = config.getPtoType();
        switch (type) {
            case NAIVE:
                return new NaivePeqtReceiver(receiverRpc, senderParty, (NaivePeqtConfig) config);
            case CGS22:
                return new Cgs22PeqtReceiver(receiverRpc, senderParty, (Cgs22PeqtConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PeqtType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PeqtConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Cgs22PeqtConfig.Builder(securityModel, silent).build();
    }
}
