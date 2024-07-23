package edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.lut.zl.rrgg21.Rrgg21ZlLutSender;

/**
 * Zl lookup table protocol factory.
 *
 * @author Liqiang Peng
 * @date 2024/6/3
 */
public class ZlLutFactory {
    /**
     * private constructor
     */
    private ZlLutFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlLutType {
        /**
         * RRRR21 (SIRNN)
         */
        RRGG21,
    }

    /**
     * Build Sender.
     *
     * @param senderRpc     sender rpc.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return sender.
     */
    public static ZlLutSender createSender(Rpc senderRpc, Party receiverParty, ZlLutConfig config) {
        ZlLutType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRGG21:
                return new Rrgg21ZlLutSender(senderRpc, receiverParty, (Rrgg21ZlLutConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlLutConfig.class.getSimpleName() + ": " + type.name());
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
    public static ZlLutReceiver createReceiver(Rpc receiverRpc, Party senderParty, ZlLutConfig config) {
        ZlLutType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRGG21:
                return new Rrgg21ZlLutReceiver(receiverRpc, senderParty, (Rrgg21ZlLutConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlLutType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        silent.
     * @return a default config.
     */
    public static ZlLutConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrgg21ZlLutConfig.Builder(securityModel, silent).build();
    }
}
