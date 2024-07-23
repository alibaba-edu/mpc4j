package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.aided.AidedZl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.aided.AidedZl64TripleGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct.DirectZl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct.DirectZl64TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.direct.DirectZl64TripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.fake.FakeZl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.fake.FakeZl64TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.fake.FakeZl64TripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent.SilentZl64TripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent.SilentZl64TripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl64.silent.SilentZl64TripleGenSender;

/**
 * Zl64 triple generation factory.
 *
 * @author Weiran Liu
 * @date 2024/6/29
 */
public class Zl64TripleGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private Zl64TripleGenFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum Zl64TripleGenType {
        /**
         * fake
         */
        FAKE,
        /**
         * aided
         */
        AIDED,
        /**
         * direct COT
         */
        DIRECT_COT,
        /**
         * silent COT
         */
        SILENT_COT,
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param config        config.
     * @return a sender.
     */
    public static Zl64TripleGenParty createSender(Rpc senderRpc, Party receiverParty, Zl64TripleGenConfig config) {
        Zl64TripleGenType type = config.getPtoType();
        return switch (type) {
            case FAKE -> new FakeZl64TripleGenSender(senderRpc, receiverParty, (FakeZl64TripleGenConfig) config);
            case DIRECT_COT -> new DirectZl64TripleGenSender(senderRpc, receiverParty, (DirectZl64TripleGenConfig) config);
            case SILENT_COT -> new SilentZl64TripleGenSender(senderRpc, receiverParty, (SilentZl64TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Zl64TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    /**
     * Creates a sender.
     *
     * @param senderRpc     sender RPC.
     * @param receiverParty receiver party.
     * @param aiderParty    aider party.
     * @param config        config.
     * @return a sender.
     */
    public static Zl64TripleGenParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, Zl64TripleGenConfig config) {
        Zl64TripleGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        return switch (type) {
            case AIDED ->
                new AidedZl64TripleGenParty(senderRpc, receiverParty, aiderParty, (AidedZl64TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Zl64TripleGenType.class.getSimpleName() + ": " + type.name());
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
    public static Zl64TripleGenParty createReceiver(Rpc receiverRpc, Party senderParty, Zl64TripleGenConfig config) {
        Zl64TripleGenType type = config.getPtoType();
        return switch (type) {
            case FAKE -> new FakeZl64TripleGenReceiver(receiverRpc, senderParty, (FakeZl64TripleGenConfig) config);
            case DIRECT_COT ->
                new DirectZl64TripleGenReceiver(receiverRpc, senderParty, (DirectZl64TripleGenConfig) config);
            case SILENT_COT ->
                new SilentZl64TripleGenReceiver(receiverRpc, senderParty, (SilentZl64TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Zl64TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    /**
     * Creates a receiver.
     *
     * @param receiverRpc receiver RPC.
     * @param senderParty sender party.
     * @param aiderParty  aider party.
     * @param config      config.
     * @return a receiver.
     */
    public static Zl64TripleGenParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, Zl64TripleGenConfig config) {
        Zl64TripleGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        return switch (type) {
            case AIDED ->
                new AidedZl64TripleGenParty(receiverRpc, senderParty, aiderParty, (AidedZl64TripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + Zl64TripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    public static Zl64TripleGenConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return switch (securityModel) {
            case IDEAL -> new FakeZl64TripleGenConfig.Builder().build();
            case TRUSTED_DEALER -> new AidedZl64TripleGenConfig.Builder().build();
            case SEMI_HONEST -> {
                if (silent) {
                    yield new SilentZl64TripleGenConfig.Builder().build();
                } else {
                    yield new DirectZl64TripleGenConfig.Builder().build();
                }
            }
            default ->
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        };

    }
}
