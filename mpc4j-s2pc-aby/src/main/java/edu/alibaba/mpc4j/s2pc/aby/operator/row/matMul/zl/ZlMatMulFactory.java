//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matMul.zl;
//
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
//import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
//import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.rrgg21.Rrgg21ZlCrossTermConfig;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.rrgg21.Rrgg21ZlCrossTermReceiver;
//import edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.rrgg21.Rrgg21ZlCrossTermSender;
//
///**
// * Zl Matrix Multiplication Factory
// *
// * @author Liqiang Peng
// * @date 2024/6/7
// */
//public class ZlMatMulFactory implements PtoFactory {
//    /**
//     * private constructor.
//     */
//    private ZlMatMulFactory() {
//        // empty
//    }
//
//    /**
//     * the type.
//     */
//    public enum ZlMatMulType {
//        /**
//         * RRGG21 (SIRNN)
//         */
//        RRGG21,
//    }
//
//    /**
//     * Creates a sender.
//     *
//     * @param z2cSender     z2c sender.
//     * @param receiverParty the receiver party.
//     * @param config        the config.
//     * @return a sender.
//     */
//    public static ZlMatMulParty createSender(Z2cParty z2cSender, Party receiverParty, ZlMatMulConfig config) {
//        ZlMatMulType type = config.getPtoType();
//        //noinspection SwitchStatementWithTooFewBranches
//        switch (type) {
//            case RRGG21:
//                //return new Rrgg21ZlCrossTermSender(z2cSender, receiverParty, (Rrgg21ZlCrossTermConfig) config);
//            default:
//                throw new IllegalArgumentException("Invalid " + ZlMatMulType.class.getSimpleName() + ": " + type.name());
//        }
//    }
//
//    /**
//     * Creates a receiver.
//     *
//     * @param z2cReceiver z2c receiver.
//     * @param senderParty the sender party.
//     * @param config      the config.
//     * @return a receiver.
//     */
//    public static ZlMatMulParty createReceiver(Z2cParty z2cReceiver, Party senderParty, ZlMatMulConfig config) {
//        ZlMatMulType type = config.getPtoType();
//        //noinspection SwitchStatementWithTooFewBranches
//        switch (type) {
//            case RRGG21:
//                return new Rrgg21ZlCrossTermReceiver(z2cReceiver, senderParty, (Rrgg21ZlCrossTermConfig) config);
//            default:
//                throw new IllegalArgumentException("Invalid " + ZlMatMulType.class.getSimpleName() + ": " + type.name());
//        }
//    }
//
//    /**
//     * Creates a default config.
//     *
//     * @param securityModel the security model.
//     * @param silent        if using a silent protocol.
//     * @return a default config.
//     */
//    public static ZlMatMulConfig createDefaultConfig(SecurityModel securityModel, boolean silent) {
//        return new Rrgg21ZlCrossTermConfig.Builder(securityModel, silent).build();
//    }
//}
