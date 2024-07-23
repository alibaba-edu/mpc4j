package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24.G24ZlExtensionConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24.G24ZlExtensionReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24.G24ZlExtensionSender;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21.Rrgg21ZlExtensionConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21.Rrgg21ZlExtensionReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.rrgg21.Rrgg21ZlExtensionSender;

/**
 * Zl Value Extension Factory.
 *
 * @author Liqiang Peng
 * @date 2024/5/29
 */
public class ZlExtensionFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private ZlExtensionFactory() {
        // empty
    }

    /**
     * the type.
     */
    public enum ZlExtensionType {
        /**
         * RRGG21 (SIRNN)
         */
        RRGG21,
        /**
         * Algorithm from Hao Guo.
         */
        G24
    }

    /**
     * Creates a sender.
     *
     * @param z2cSender     z2c sender.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static ZlExtensionParty createSender(Z2cParty z2cSender, Party receiverParty, ZlExtensionConfig config) {
        ZlExtensionType type = config.getPtoType();
        switch (type) {
            case RRGG21:
                return new Rrgg21ZlExtensionSender(z2cSender, receiverParty, (Rrgg21ZlExtensionConfig) config);
            case G24:
                return new G24ZlExtensionSender(z2cSender, receiverParty, (G24ZlExtensionConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlExtensionType.class.getSimpleName() + ": " + type.name());
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
    public static ZlExtensionParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlExtensionConfig config) {
        ZlExtensionType type = config.getPtoType();
        switch (type) {
            case RRGG21:
                return new Rrgg21ZlExtensionReceiver(z2cReceiver, senderParty, (Rrgg21ZlExtensionConfig) config);
            case G24:
                return new G24ZlExtensionReceiver(z2cReceiver, senderParty, (G24ZlExtensionConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlExtensionType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlExtensionConfig createDefaultConfig(SecurityModel securityModel, boolean silent, boolean signed) {
        return signed ?
            new G24ZlExtensionConfig.Builder(securityModel, silent).build() :
            new Rrgg21ZlExtensionConfig.Builder(securityModel, silent).build();
    }
}
