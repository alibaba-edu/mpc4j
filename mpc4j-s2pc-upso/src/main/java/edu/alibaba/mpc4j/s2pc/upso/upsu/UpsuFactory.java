package edu.alibaba.mpc4j.s2pc.upso.upsu;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuConfig;
import edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuReceiver;
import edu.alibaba.mpc4j.s2pc.upso.upsu.tcl23.Tcl23UpsuSender;
import edu.alibaba.mpc4j.s2pc.upso.upsu.zlp24.*;

/**
 * UPSU factory.
 *
 * @author Liqiang Peng
 * @date 2024/3/7
 */
public class UpsuFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private UpsuFactory() {
        // empty
    }

    /**
     * UPSU type
     */
    public enum UpsuType {
        /**
         * TCL23
         */
        TCL23,
        /**
         * ZLP24 + PKE
         */
        ZLP24_PKE,
        /**
         * ZLP24 + PEQT
         */
        ZLP24_PEQT,
    }

    /**
     * create a sender.
     *
     * @param senderRpc     sender rpc.
     * @param receiverParty receiver party.
     * @param config      config.
     * @return a sender.
     */
    public static UpsuSender createSender(Rpc senderRpc, Party receiverParty, UpsuConfig config) {
        UpsuType type = config.getPtoType();
        switch (type) {
            case TCL23:
                return new Tcl23UpsuSender(senderRpc, receiverParty, (Tcl23UpsuConfig) config);
            case ZLP24_PKE:
                return new Zlp24PkeUpsuSender(senderRpc, receiverParty, (Zlp24PkeUpsuConfig) config);
            case ZLP24_PEQT:
                return new Zlp24PeqtUpsuSender(senderRpc, receiverParty, (Zlp24PeqtUpsuConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UpsuType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create a receiver.
     *
     * @param receiverRpc receiver rpc.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static UpsuReceiver createReceiver(Rpc receiverRpc, Party senderParty, UpsuConfig config) {
        UpsuType type = config.getPtoType();
        switch (type) {
            case TCL23:
                return new Tcl23UpsuReceiver(receiverRpc, senderParty, (Tcl23UpsuConfig) config);
            case ZLP24_PKE:
                return new Zlp24PkeUpsuReceiver(receiverRpc, senderParty, (Zlp24PkeUpsuConfig) config);
            case ZLP24_PEQT:
                return new Zlp24PeqtUpsuReceiver(receiverRpc, senderParty, (Zlp24PeqtUpsuConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + UpsuType.class.getSimpleName() + ": " + type.name());
        }
    }
}
