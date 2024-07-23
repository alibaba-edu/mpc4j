package edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.rrk20.Rrk20ZlMaxSender;

/**
 * Zl Max Factory
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public class ZlMaxFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlMaxFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlMaxType {
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
    public static ZlMaxParty createSender(Z2cParty z2cSender, Party receiverParty, ZlMaxConfig config) {
        ZlMaxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlMaxSender(z2cSender, receiverParty, (Rrk20ZlMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMaxType.class.getSimpleName() + ": " + type.name());
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
    public static ZlMaxParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlMaxConfig config) {
        ZlMaxType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRK20:
                return new Rrk20ZlMaxReceiver(z2cReceiver, senderParty, (Rrk20ZlMaxConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlMaxType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlMaxConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrk20ZlMaxConfig.Builder(securityModel, silent).build();
    }
}
