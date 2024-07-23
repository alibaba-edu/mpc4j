//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matrixMul.zl.rrgg21;
//
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
//
///**
// * RRGG21 Zl Cross Term Multiplication protocol description. The protocol comes from the following paper:
// * <p>
// * Deevashwer Rathee, Mayank Rathee, Rahul Kranti Kiran Goli, Divya Gupta, Rahul Sharma, Nishanth Chandran and
// * Aseem Rastogi.
// * SIRNN: A Math Library for Secure RNN Inference. IEEE S&P 2021, pp. 1003-1020. 2021.
// * </p>
// *
// * @author Liqiang Peng
// * @date 2024/6/5
// */
//public class Rrgg21ZlCrossTermPtoDesc implements PtoDesc {
//    /**
//     * protocol ID
//     */
//    private static final int PTO_ID = Math.abs((int) 5852988718449630307L);
//    /**
//     * protocol name
//     */
//    private static final String PTO_NAME = "RRGG21_ZL_CROSS_TERM_MULTIPLICATION";
//
//    /**
//     * protocol step
//     */
//    enum PtoStep {
//        /**
//         * sender sends encrypted elements
//         */
//        SENDER_SENDS_ENC_ELEMENTS,
//    }
//
//    /**
//     * singleton mode
//     */
//    private static final Rrgg21ZlCrossTermPtoDesc INSTANCE = new Rrgg21ZlCrossTermPtoDesc();
//
//    /**
//     * private constructor.
//     */
//    private Rrgg21ZlCrossTermPtoDesc() {
//        // empty
//    }
//
//    public static PtoDesc getInstance() {
//        return INSTANCE;
//    }
//
//    static {
//        PtoDescManager.registerPtoDesc(getInstance());
//    }
//
//    @Override
//    public int getPtoId() {
//        return PTO_ID;
//    }
//
//    @Override
//    public String getPtoName() {
//        return PTO_NAME;
//    }
//}
