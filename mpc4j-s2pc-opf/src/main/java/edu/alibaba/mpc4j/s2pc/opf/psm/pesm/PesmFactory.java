package edu.alibaba.mpc4j.s2pc.opf.psm.pesm;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22.Cgs22LnotPesmConfig;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22.Cgs22LnotPesmReceiver;
import edu.alibaba.mpc4j.s2pc.opf.psm.pesm.cgs22.Cgs22LnotPesmSender;

/**
 * private (equal) set membership factory.
 *
 * @author Weiran Liu
 * @date 2023/7/22
 */
public class PesmFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PesmFactory() {
        // empty
    }

    /**
     * type
     */
    public enum PesmType {
        /**
         * CGS22 based on LNOT
         */
        CGS22_LNOT,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static PesmSender createSender(Rpc senderRpc, Party receiverParty, PesmConfig config) {
        PesmType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGS22_LNOT:
                return new Cgs22LnotPesmSender(senderRpc, receiverParty, (Cgs22LnotPesmConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PesmType.class.getSimpleName() + ": " + type.name());
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
    public static PesmReceiver createReceiver(Rpc receiverRpc, Party senderParty, PesmConfig config) {
        PesmType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case CGS22_LNOT:
                return new Cgs22LnotPesmReceiver(receiverRpc, senderParty, (Cgs22LnotPesmConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PesmType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static PesmConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Cgs22LnotPesmConfig.Builder(securityModel, silent).build();
    }
}
