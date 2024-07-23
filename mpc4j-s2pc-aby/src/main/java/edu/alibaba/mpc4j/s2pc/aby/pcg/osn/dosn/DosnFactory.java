package edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.dosn.lll24.Lll24DosnSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.osn.rosn.RosnFactory;

/**
 * Decision OSN factory.
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public class DosnFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private DosnFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum DosnType {
        /**
         * LLL24
         */
        LLL24,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static DosnSender createSender(Rpc senderRpc, Party receiverParty, DosnConfig config) {
        DosnType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LLL24:
                return new Lll24DosnSender(senderRpc, receiverParty, (Lll24DosnConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + DosnType.class.getSimpleName() + ": " + type.name());
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
    public static DosnReceiver createReceiver(Rpc receiverRpc, Party senderParty, DosnConfig config) {
        DosnType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case LLL24:
                return new Lll24DosnReceiver(receiverRpc, senderParty, (Lll24DosnConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + DosnType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates default config.
     *
     * @param silent        using silent OT.
     * @param securityModel the security model.
     * @return a default config.
     */
    public static DosnConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
            case SEMI_HONEST:
                return new Lll24DosnConfig.Builder(silent).build();
            case MALICIOUS:
                return new Lll24DosnConfig.Builder(RosnFactory.createDefaultConfig(SecurityModel.MALICIOUS, silent)).build();
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel.name());
        }
    }
}
