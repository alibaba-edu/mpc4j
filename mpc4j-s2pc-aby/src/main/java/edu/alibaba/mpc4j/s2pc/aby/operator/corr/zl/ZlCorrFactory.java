package edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.gp23.Gp23ZlCorrSender;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.corr.zl.rrk20.Rrk20ZlCorrSender;

/**
 * Zl Corr Factory
 *
 * @author Liqiang Peng
 * @date 2023/10/1
 */
public class ZlCorrFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlCorrFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlCorrType {
        /**
         * RRK+20
         */
        RRK20,
        /**
         * GP23
         */
        GP23,
    }

    /**
     * Creates a sender.
     *
     * @param z2cSender     z2 circuit sender.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlCorrParty createSender(Z2cParty z2cSender, Party receiverParty, ZlCorrConfig config) {
        ZlCorrType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlCorrSender(z2cSender, receiverParty, (Rrk20ZlCorrConfig) config);
            case GP23:
                return new Gp23ZlCorrSender(z2cSender.getRpc(), receiverParty, (Gp23ZlCorrConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCorrType.class.getSimpleName() + ": " + type.name());
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
    public static ZlCorrParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlCorrConfig config) {
        ZlCorrType type = config.getPtoType();
        switch (type) {
            case RRK20:
                return new Rrk20ZlCorrReceiver(z2cReceiver, senderParty, (Rrk20ZlCorrConfig) config);
            case GP23:
                return new Gp23ZlCorrReceiver(z2cReceiver.getRpc(), senderParty, (Gp23ZlCorrConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlCorrType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlCorrConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrk20ZlCorrConfig.Builder(securityModel, silent).build();
    }
}
