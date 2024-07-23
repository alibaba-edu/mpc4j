package edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20.Rrkc20ZlWrapConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20.Rrkc20ZlWrapReceiver;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.wrap.zl.rrkc20.Rrkc20ZlWrapSender;

/**
 * Zl wrap protocol factory.
 *
 * @author Liqiang Peng
 * @date 2024/5/30
 */
public class ZlWrapFactory {
    /**
     * private constructor
     */
    private ZlWrapFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlWrapType {
        /**
         * RRKC20 (CryptFlow2)
         */
        RRKC20,
    }

    /**
     * Build Sender.
     *
     * @param z2cSender     z2c sender.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return sender.
     */
    public static ZlWrapParty createSender(Z2cParty z2cSender, Party receiverParty, ZlWrapConfig config) {
        ZlWrapType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRKC20:
                return new Rrkc20ZlWrapSender(z2cSender, receiverParty, (Rrkc20ZlWrapConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlWrapType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Build Receiver.
     *
     * @param z2cReceiver z2c receiver.
     * @param senderParty sender party.
     * @param config      config.
     * @return receiver.
     */
    public static ZlWrapParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlWrapConfig config) {
        ZlWrapType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case RRKC20:
                return new Rrkc20ZlWrapReceiver(z2cReceiver, senderParty, (Rrkc20ZlWrapConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + ZlWrapType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel the security model.
     * @param silent        if using a silent protocol.
     * @return a default config.
     */
    public static ZlWrapConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return new Rrkc20ZlWrapConfig.Builder(securityModel, silent).build();
    }
}
