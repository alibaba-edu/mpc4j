package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91.Bea91Zl64cConfig;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91.Bea91Zl64cReceiver;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl64.bea91.Bea91Zl64cSender;

/**
 * Zl circuit party factory.
 *
 * @author Li Peng
 * @date 2024/7/23
 */
public class Zl64cFactory implements PtoFactory {
    /**
     * private constructor
     */
    private Zl64cFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Zl64cType {
        /**
         * Bea91
         */
        BEA91,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static Zl64cParty createSender(Rpc senderRpc, Party receiverParty, Zl64cConfig config) {
        Zl64cType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BEA91:
                return new Bea91Zl64cSender(senderRpc, receiverParty, (Bea91Zl64cConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Zl64cType.class.getSimpleName() + ": " + type.name());
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
    public static Zl64cParty createReceiver(Rpc receiverRpc, Party senderParty, Zl64cConfig config) {
        Zl64cType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case BEA91:
                return new Bea91Zl64cReceiver(receiverRpc, senderParty, (Bea91Zl64cConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + Zl64cType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        silent.
     * @return a default config.
     */
    public static Zl64cConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Bea91Zl64cConfig.Builder(securityModel, silent).build();
    }
}
