package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.aided.AidedZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.aided.AidedZ2TripleGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct.DirectZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct.DirectZ2TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.direct.DirectZ2TripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.fake.FakeZ2TripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot.LcotZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot.LcotZ2TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.lcot.LcotZ2TripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent.SilentZ2TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent.SilentZ2TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.z2.silent.SilentZ2TripleGenSender;

/**
 * Z2 triple generation factory.
 *
 * @author Weiran Liu
 * @date 2024/5/26
 */
public class Z2TripleGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private Z2TripleGenFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Z2TripleGenType {
        /**
         * fake
         */
        FAKE,
        /**
         * aided
         */
        AIDED,
        /**
         * direct core COT
         */
        DIRECT_COT,
        /**
         * silent COT
         */
        SILENT_COT,
        /**
         * direct LCOT
         */
        LCOT,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static Z2TripleGenParty createSender(Rpc senderRpc, Party receiverParty, Z2TripleGenConfig config) {
        Z2TripleGenType type = config.getPtoType();
        return switch (type) {
            case FAKE -> new FakeZ2TripleGenSender(senderRpc, receiverParty, (FakeZ2TripleGenConfig) config);
            case DIRECT_COT -> new DirectZ2TripleGenSender(senderRpc, receiverParty, (DirectZ2TripleGenConfig) config);
            case SILENT_COT -> new SilentZ2TripleGenSender(senderRpc, receiverParty, (SilentZ2TripleGenConfig) config);
            case LCOT -> new LcotZ2TripleGenSender(senderRpc, receiverParty, (LcotZ2TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Z2TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static Z2TripleGenParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Z2TripleGenConfig config) {
        Z2TripleGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        return switch (type) {
            case AIDED ->
                new AidedZ2TripleGenParty(senderRpc, receiverParty, aiderParty, (AidedZ2TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Z2TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static Z2TripleGenParty createReceiver(Rpc receiverRpc, Party senderParty, Z2TripleGenConfig config) {
        Z2TripleGenType type = config.getPtoType();
        return switch (type) {
            case FAKE -> new FakeZ2TripleGenReceiver(receiverRpc, senderParty, (FakeZ2TripleGenConfig) config);
            case DIRECT_COT ->
                new DirectZ2TripleGenReceiver(receiverRpc, senderParty, (DirectZ2TripleGenConfig) config);
            case SILENT_COT ->
                new SilentZ2TripleGenReceiver(receiverRpc, senderParty, (SilentZ2TripleGenConfig) config);
            case LCOT ->
                new LcotZ2TripleGenReceiver(receiverRpc, senderParty, (LcotZ2TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Z2TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param config      config.
     * @return a receiver.
     */
    public static Z2TripleGenParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Z2TripleGenConfig config) {
        Z2TripleGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        return switch (type) {
            case AIDED ->
                new AidedZ2TripleGenParty(receiverRpc, senderParty, aiderParty, (AidedZ2TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Z2TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    public static Z2TripleGenConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        switch (securityModel) {
            case IDEAL:
                return new FakeZ2TripleGenConfig.Builder().build();
            case TRUSTED_DEALER:
                return new AidedZ2TripleGenConfig.Builder().build();
            case SEMI_HONEST:
                if (silent) {
                    return new SilentZ2TripleGenConfig.Builder().build();
                } else {
                    return new DirectZ2TripleGenConfig.Builder().build();
                }
            case MALICIOUS:
            default:
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        }
    }
}
