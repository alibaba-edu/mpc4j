package edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20.Rrkc20ZlB2aConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20.Rrkc20ZlB2aReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.b2a.zl.rrkc20.Rrkc20ZlB2aSender;

/**
 * Zl boolean to arithmetic protocol factory.
 *
 * @author Liqiang Peng
 * @date 2024/6/4
 */
public class ZlB2aFactory {
    /**
     * private constructor
     */
    private ZlB2aFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlB2aType {
        /**
         * RRKC20 (CryptFlow2)
         */
        RRKC20,
    }

    /**
     * Build Sender.
     *
     * @param senderRpc     sender rpc.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return sender.
     */
    public static ZlB2aParty createSender(Rpc senderRpc, Party receiverParty, ZlB2aConfig config) {
        ZlB2aType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRKC20:
                return new Rrkc20ZlB2aSender(senderRpc, receiverParty, (Rrkc20ZlB2aConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlB2aType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Build Receiver.
     *
     * @param receiverRpc receiver rpc.
     * @param senderParty sender party.
     * @param config      config.
     * @return receiver.
     */
    public static ZlB2aParty createReceiver(Rpc receiverRpc, Party senderParty, ZlB2aConfig config) {
        ZlB2aType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRKC20:
                return new Rrkc20ZlB2aReceiver(receiverRpc, senderParty, (Rrkc20ZlB2aConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlB2aType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlB2aConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrkc20ZlB2aConfig.Builder(securityModel, silent).build();
    }
}
