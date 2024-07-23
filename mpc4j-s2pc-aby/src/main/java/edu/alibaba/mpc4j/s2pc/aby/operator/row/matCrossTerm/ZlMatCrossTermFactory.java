package edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21.Rrgg21ZlMatCrossTermConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21.Rrgg21ZlMatCrossTermReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.matCrossTerm.rrgg21.Rrgg21ZlMatCrossTermSender;

/**
 * Zl Matrix Cross Term Multiplication Factory
 *
 * @author Liqiang Peng
 * @date 2024/6/7
 */
public class ZlMatCrossTermFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlMatCrossTermFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlMatCrossTermType {
        /**
         * RRGG21 (SIRNN)
         */
        RRGG21,
    }

    /**
     * Creates a sender.
     *
     * @param z2cSender     z2c sender.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlMatCrossTermParty createSender(Z2cParty z2cSender, Party receiverParty, ZlMatCrossTermConfig config) {
        ZlMatCrossTermType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRGG21:
                return new Rrgg21ZlMatCrossTermSender(z2cSender, receiverParty, (Rrgg21ZlMatCrossTermConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMatCrossTermType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a receiver.
     *
     * @param z2cReceiver z2c receiver.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static ZlMatCrossTermParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlMatCrossTermConfig config) {
        ZlMatCrossTermType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRGG21:
                return new Rrgg21ZlMatCrossTermReceiver(z2cReceiver, senderParty, (Rrgg21ZlMatCrossTermConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMatCrossTermType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlMatCrossTermConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrgg21ZlMatCrossTermConfig.Builder(securityModel, silent).build();
    }
}
