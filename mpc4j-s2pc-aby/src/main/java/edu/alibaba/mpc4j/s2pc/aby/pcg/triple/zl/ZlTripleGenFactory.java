package edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.aided.AidedZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.aided.AidedZlTripleGenParty;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct.DirectZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct.DirectZlTripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.direct.DirectZlTripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.fake.FakeZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.fake.FakeZlTripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.fake.FakeZlTripleGenSender;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.silent.SilentZlTripleGenConfig;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.silent.SilentZlTripleGenReceiver;
import edu.alibaba.mpc4j.s2pc.aby.pcg.triple.zl.silent.SilentZlTripleGenSender;

/**
 * Zl triple generation factory.
 *
 * @author Weiran Liu
 * @date 2024/5/27
 */
public class ZlTripleGenFactory implements PtoFactory {
    /**
     * private constructor
     */
    private ZlTripleGenFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum ZlTripleGenType {
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
    public static ZlTripleGenParty createSender(Rpc senderRpc, Party receiverParty, ZlTripleGenConfig config) {
        ZlTripleGenType type = config.getPtoType();
        return switch (type) {
            case FAKE -> new FakeZlTripleGenSender(senderRpc, receiverParty, (FakeZlTripleGenConfig) config);
            case DIRECT_COT -> new DirectZlTripleGenSender(senderRpc, receiverParty, (DirectZlTripleGenConfig) config);
            case SILENT_COT -> new SilentZlTripleGenSender(senderRpc, receiverParty, (SilentZlTripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + ZlTripleGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlTripleGenParty createSender(Rpc senderRpc, Party receiverParty, Party aiderParty, ZlTripleGenConfig config) {
        ZlTripleGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        return switch (type) {
            case AIDED ->
                new AidedZlTripleGenParty(senderRpc, receiverParty, aiderParty, (AidedZlTripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + ZlTripleGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlTripleGenParty createReceiver(Rpc receiverRpc, Party senderParty, ZlTripleGenConfig config) {
        ZlTripleGenType type = config.getPtoType();
        return switch (type) {
            case FAKE -> new FakeZlTripleGenReceiver(receiverRpc, senderParty, (FakeZlTripleGenConfig) config);
            case DIRECT_COT ->
                new DirectZlTripleGenReceiver(receiverRpc, senderParty, (DirectZlTripleGenConfig) config);
            case SILENT_COT ->
                new SilentZlTripleGenReceiver(receiverRpc, senderParty, (SilentZlTripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + ZlTripleGenType.class.getSimpleName() + ": " + type.name());
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
    public static ZlTripleGenParty createReceiver(Rpc receiverRpc, Party senderParty, Party aiderParty, ZlTripleGenConfig config) {
        ZlTripleGenType type = config.getPtoType();
        //noinspection SwitchStatementWithTooFewBranches
        return switch (type) {
            case AIDED ->
                new AidedZlTripleGenParty(receiverRpc, senderParty, aiderParty, (AidedZlTripleGenConfig) config);
            default ->
                throw new IllegalArgumentException("Invalid " + ZlTripleGenType.class.getSimpleName() + ": " + type.name());
        };
    }

    public static ZlTripleGenConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
        return switch (securityModel) {
            case IDEAL -> new FakeZlTripleGenConfig.Builder().build();
            case TRUSTED_DEALER -> new AidedZlTripleGenConfig.Builder().build();
            case SEMI_HONEST -> {
                if (silent) {
                    yield new SilentZlTripleGenConfig.Builder().build();
                } else {
                    yield new DirectZlTripleGenConfig.Builder().build();
                }
            }
            default ->
                throw new IllegalArgumentException("Invalid " + SecurityModel.class.getSimpleName() + ": " + securityModel);
        };

    }
}
