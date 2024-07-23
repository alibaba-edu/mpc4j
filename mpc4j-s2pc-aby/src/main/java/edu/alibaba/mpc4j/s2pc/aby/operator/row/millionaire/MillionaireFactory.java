package edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20.Rrk20MillionaireConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20.Rrk20MillionaireReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.millionaire.rrk20.Rrk20MillionaireSender;

/**
 * Millionaire Protocol Factory.
 *
 * @author Li Peng
 * @date 2023/4/24
 */
public class MillionaireFactory {
    /**
     * Private constructor.
     */
    private MillionaireFactory() {
        // empty
    }

    /**
     * Protocol enums.
     */
    public enum MillionaireType {
        /**
         * RRK+20, CHEETAH
         */
        RRK20,
    }

    /**
     * Build Sender.
     *
     * @param z2cSender     z2 circuit party.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return sender.
     */
    public static MillionaireParty createSender(Z2cParty z2cSender, Party receiverParty, MillionaireConfig config) {
        MillionaireType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20MillionaireSender(z2cSender, receiverParty, (Rrk20MillionaireConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + MillionaireType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Build Receiver.
     *
     * @param z2cReceiver z2 circuit receiver.
     * @param senderParty sender party.
     * @param config      config.
     * @return receiver.
     */
    public static MillionaireParty createReceiver(Z2cParty z2cReceiver, Party senderParty, MillionaireConfig config) {
        MillionaireType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20MillionaireReceiver(z2cReceiver, senderParty, (Rrk20MillionaireConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + MillionaireType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static MillionaireConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrk20MillionaireConfig.Builder(securityModel, silent).build();
    }
}
