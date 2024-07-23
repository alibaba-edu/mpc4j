package edu.alibaba.mpc4j.s2pc.pcg.ot.conv32;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot.ScotConv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot.ScotConv32Receiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.scot.ScotConv32Sender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode.SvodeConv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode.SvodeConv32Receiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svode.SvodeConv32Sender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole.SvoleConv32Config;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole.SvoleConv32Receiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.conv32.svole.SvoleConv32Sender;

/**
 * F_3 -> F_2 modulus conversion factory.
 *
 * @author Weiran Liu
 * @date 2024/6/5
 */
public class Conv32Factory implements PtoFactory {
    /**
     * private constructor
     */
    private Conv32Factory() {
        // empty
    }

    /**
     * F_3 -> F_2 modulus conversion type.
     */
    public enum Conv32Type {
        /**
         * silent COT
         */
        SCOT,
        /**
         * Subfield VOLE
         */
        SVOLE,
        /**
         * Subfield VODE
         */
        SVODE,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     the sender RPC.
     * @param receiverParty the receiver party.
     * @param config        the config.
     * @return a sender.
     */
    public static Conv32Party createSender(Rpc senderRpc, Party receiverParty, Conv32Config config) {
        Conv32Type type = config.getPtoType();
        switch (type) {
            case SCOT:
                return new ScotConv32Sender(senderRpc, receiverParty, (ScotConv32Config) config);
            case SVOLE:
                return new SvoleConv32Sender(senderRpc, receiverParty, (SvoleConv32Config) config);
            case SVODE:
                return new SvodeConv32Sender(senderRpc, receiverParty, (SvodeConv32Config) config);
            default:
                throw new IllegalArgumentException("Invalid " + Conv32Type.class.getSimpleName() + ": " + type.name());
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
    public static Conv32Party createReceiver(Rpc receiverRpc, Party senderParty, Conv32Config config) {
        Conv32Type type = config.getPtoType();
        switch (type) {
            case SCOT:
                return new ScotConv32Receiver(receiverRpc, senderParty, (ScotConv32Config) config);
            case SVOLE:
                return new SvoleConv32Receiver(receiverRpc, senderParty, (SvoleConv32Config) config);
            case SVODE:
                return new SvodeConv32Receiver(receiverRpc, senderParty, (SvodeConv32Config) config);
            default:
                throw new IllegalArgumentException("Invalid " + Conv32Type.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a default config.
     *
     * @param securityModel security model.
     * @param conv32Type    Conv32 type.
     * @return a default config.
     */
    public static Conv32Config createDefaultConfig(SecurityModel securityModel, Conv32Type conv32Type) {
        switch (conv32Type) {
            case SCOT:
                return new ScotConv32Config.Builder(securityModel).build();
            case SVOLE:
                return new SvoleConv32Config.Builder(securityModel).build();
            case SVODE:
                return new SvodeConv32Config.Builder(securityModel).build();
            default:
                throw new IllegalArgumentException("Invalid " + Conv32Type.class.getSimpleName() + ": " + conv32Type);
        }
    }
}
