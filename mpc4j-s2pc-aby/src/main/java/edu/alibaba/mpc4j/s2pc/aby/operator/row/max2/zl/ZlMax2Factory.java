package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.rrk20.Rrk20ZlMax2Config;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.rrk20.Rrk20ZlMax2Receiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl.rrk20.Rrk20ZlMax2Sender;

/**
 * Zl Max2 Factory
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class ZlMax2Factory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlMax2Factory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlMax2Type {
        /**
         * RRK+20
         */
        RRK20,
    }

    /**
     * Creates a sender.
     *
     * @param z2cSender     z2 circuit sender.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlMax2Party createSender(Z2cParty z2cSender, Party receiverParty, ZlMax2Config config) {
        ZlMax2Type type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlMax2Sender(z2cSender, receiverParty, (Rrk20ZlMax2Config) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMax2Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param z2cReceiver z2 circuit receiver.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static ZlMax2Party createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlMax2Config config) {
        ZlMax2Type type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlMax2Receiver(z2cReceiver, senderParty, (Rrk20ZlMax2Config) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMax2Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlMax2Config createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrk20ZlMax2Config.Builder(securityModel, silent).build();
    }
}
