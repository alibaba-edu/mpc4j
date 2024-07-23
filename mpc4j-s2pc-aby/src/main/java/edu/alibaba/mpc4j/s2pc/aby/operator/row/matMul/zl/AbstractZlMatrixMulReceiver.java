//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matMul.zl;
//
//import com.google.common.base.Preconditions;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
//import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
//import edu.alibaba.mpc4j.common.tool.MathPreconditions;
//import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
//import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
//import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
//import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
//
//import java.util.stream.IntStream;
//
///**
// * Abstract Zl Matrix Multiplication Receiver.
// *
// * @author Liqiang Peng
// * @date 2024/6/7
// */
//public abstract class AbstractZlMatrixMulReceiver extends AbstractTwoPartyPto implements ZlMatMulParty {
//    /**
//     * max num
//     */
//    protected int maxNum;
//    /**
//     * max m
//     */
//    protected int maxM;
//    /**
//     * max n
//     */
//    protected int maxN;
//    /**
//     * num
//     */
//    protected int num;
//    /**
//     * m
//     */
//    protected int m;
//    /**
//     * n
//     */
//    protected int n;
//    /**
//     * output Zl instance
//     */
//    protected Zl outputZl;
//    /**
//     * ys
//     */
//    protected byte[][] ys;
//
//    public AbstractZlMatrixMulReceiver(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, ZlMatMulConfig config) {
//        super(ptoDesc, ownRpc, otherParty, config);
//    }
//
//    protected void setInitInput(int maxM, int maxN, int maxNum) {
//        MathPreconditions.checkPositive("maxNum", maxNum);
//        MathPreconditions.checkPositive("maxM", maxM);
//        MathPreconditions.checkPositive("maxN", maxN);
//        this.maxNum = maxNum;
//        this.maxM = maxM;
//        this.maxN = maxN;
//        initState();
//    }
//
//    protected void setPtoInput(SquareZlVector y, int m) {
//        checkInitialized();
//        Preconditions.checkArgument(y.isPlain());
//        MathPreconditions.checkLessOrEqual("m <= n", m, y.getZl().getL());
//        MathPreconditions.checkPositiveInRangeClosed("num", y.getNum(), maxNum);
//        MathPreconditions.checkPositiveInRangeClosed("m", m, maxM);
//        MathPreconditions.checkPositiveInRangeClosed("n", y.getZl().getL(), maxN);
//        num = y.getNum();
//        this.m = m;
//        this.n = y.getZl().getL();
//        outputZl = ZlFactory.createInstance(envType, m + n);
//        int byteL = y.getZl().getByteL();
//        ys = new byte[num][byteL];
//        IntStream.range(0, num).forEach(i ->
//            ys[i] = BigIntegerUtils.nonNegBigIntegerToByteArray(y.getZlVector().getElement(i), byteL)
//        );
//    }
//}
