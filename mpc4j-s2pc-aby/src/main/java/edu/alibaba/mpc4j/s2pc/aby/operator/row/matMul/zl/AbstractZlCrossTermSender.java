//package edu.alibaba.mpc4j.s2pc.aby.operator.row.matMul.zl;
//
//import com.google.common.base.Preconditions;
//import edu.alibaba.mpc4j.common.rpc.Party;
//import edu.alibaba.mpc4j.common.rpc.Rpc;
//import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
//import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
//import edu.alibaba.mpc4j.common.tool.CommonConstants;
//import edu.alibaba.mpc4j.common.tool.MathPreconditions;
//import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
//import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
//import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
//import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
//import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
//
//import java.util.stream.IntStream;
//
///**
// * Abstract Zl Cross Term Multiplication Sender.
// *
// * @author Liqiang Peng
// * @date 2024/6/5
// */
//public abstract class AbstractZlCrossTermSender extends AbstractTwoPartyPto implements ZlMatMulParty {
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
//     * xs
//     */
//    protected boolean[][] xs;
//
//    public AbstractZlCrossTermSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, ZlMatMulConfig config) {
//        super(ptoDesc, senderRpc, receiverParty, config);
//    }
//
//    protected void setInitInput(int maxM, int maxN, int maxNum) {
//        MathPreconditions.checkLess("maxM", maxM, CommonConstants.BLOCK_BIT_LENGTH);
//        MathPreconditions.checkPositive("maxNum", maxNum);
//        MathPreconditions.checkPositive("maxM", maxM);
//        MathPreconditions.checkPositive("maxN", maxN);
//        this.maxNum = maxNum;
//        this.maxM = maxM;
//        this.maxN = maxN;
//        initState();
//    }
//
//    protected void setPtoInput(SquareZlVector x, int n) {
//        checkInitialized();
//        Preconditions.checkArgument(x.isPlain());
//        MathPreconditions.checkLessOrEqual("m <= n", x.getZl().getL(), n);
//        MathPreconditions.checkPositiveInRangeClosed("num", x.getNum(), maxNum);
//        MathPreconditions.checkPositiveInRangeClosed("m", x.getZl().getL(), maxM);
//        MathPreconditions.checkPositiveInRangeClosed("n", n, maxN);
//        num = x.getNum();
//        this.m = x.getZl().getL();
//        this.n = n;
//        outputZl = ZlFactory.createInstance(envType, m + n);
//        int byteL = x.getZl().getByteL();
//        xs = new boolean[num][m];
//        IntStream.range(0, num).forEach(i -> {
//            byte[] bytes = BigIntegerUtils.nonNegBigIntegerToByteArray(x.getZlVector().getElement(i), byteL);
//            xs[i] = BinaryUtils.byteArrayToBinary(bytes, m);
//        });
//    }
//}
