package edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.drelu.zl.rrk20.Rrk20ZlDreluSender;

/**
 * Zl DReLU Factory
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class ZlDreluFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlDreluFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlDreluType {
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
    public static ZlDreluParty createSender(Z2cParty z2cSender, Party receiverParty, ZlDreluConfig config) {
        ZlDreluFactory.ZlDreluType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlDreluSender(z2cSender, receiverParty, (Rrk20ZlDreluConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDreluType.class.getSimpleName() + ": " + type.name());
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
    public static ZlDreluParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlDreluConfig config) {
        ZlDreluFactory.ZlDreluType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlDreluReceiver(z2cReceiver, senderParty, (Rrk20ZlDreluConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlDreluType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlDreluConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrk20ZlDreluConfig.Builder(securityModel, silent).build();
    }
}
