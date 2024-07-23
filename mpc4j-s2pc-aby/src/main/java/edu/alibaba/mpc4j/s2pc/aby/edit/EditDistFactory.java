package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistConfig;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistReceiver;
import edu.alibaba.mpc4j.s2pc.aby.edit.s2pc.diag.S2pcDiagEditDistSender;

/**
 * Edit distance factory.
 *
 * @author Li Peng
 * @date 2024/4/12
 */
public class EditDistFactory {

    public enum EditDistType {
        /**
         * secure 2-party edit distance in diagnose style
         */
        S2PC_DIAG_EDIT_DISTANCE,
    }

    /**
     * Build a receiver.
     *
     * @param z2cReceiver z2 circuit receiver.
     * @param senderParty the sender party.
     * @param config      the config.
     * @return a receiver.
     */
    public static DistCmpReceiver createReceiver(Z2cParty z2cReceiver, Party senderParty, DistCmpConfig config) {
        EditDistType type = config.getEditDistType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case S2PC_DIAG_EDIT_DISTANCE:
                return new S2pcDiagEditDistReceiver(z2cReceiver, senderParty, (S2pcDiagEditDistConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + EditDistType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Build a sender.
     *
     * @param z2cSender     z2 circuit sender.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static DistCmpSender createSender(Z2cParty z2cSender, Party receiverParty, DistCmpConfig config) {
        EditDistType type = config.getEditDistType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case S2PC_DIAG_EDIT_DISTANCE:
                return new S2pcDiagEditDistSender(z2cSender, receiverParty, (S2pcDiagEditDistConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + EditDistType.class.getSimpleName() + ": " + type.name());
        }
    }
}