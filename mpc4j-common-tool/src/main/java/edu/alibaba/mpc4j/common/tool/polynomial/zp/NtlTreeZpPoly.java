package edu.alibaba.mpc4j.common.tool.polynomial.zp;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 用NTL实现的二叉树快速插值。方案描述参见下述论文完整版的附录C：Fast Interpolation and Multi-point Evaluation
 * <p>
 * Pinkas, Benny, Mike Rosulek, Ni Trieu, and Avishay Yanai. Spot-light: Lightweight private set intersection from
 * sparse OT extension. CRYPTO 2019, pp. 401-431. Springer, Cham, 2019.
 * </p>
 *
 * @author Weiran Liu
 * @date 2022/11/2
 */
public class NtlTreeZpPoly extends AbstractZpTreePoly {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_TOOL_NAME);
    }

    /**
     * 有限域质数p的字节数组
     */
    private final byte[] primeByteArray;
    /**
     * 有限域质数p的字节长度，可能会大于byteL
     */
    private final int primeByteLength;
    /**
     * 插值点数量
     */
    private int interpolatePointNum;
    /**
     * 插值二叉树
     */
    private ByteBuffer interpolateBinaryTreeHandler;
    /**
     * 倒数的逆
     */
    private ByteBuffer derivativeInversesHandler;
    /**
     * 求值多项式插值点数量
     */
    private int evaluatePolynomialPointNum;
    /**
     * 求指点数量
     */
    private int evaluatePointNum;
    /**
     * 单一求值点
     */
    private byte[] singleEvaluatePoint;
    /**
     * 求值间隔点数量
     */
    private int intervalPointNum;
    /**
     * 求值二叉树
     */
    private ArrayList<ByteBuffer> evaluateTreeHandlerArrayList;

    public NtlTreeZpPoly(int l) {
        super(l);
        primeByteArray = BigIntegerUtils.bigIntegerToByteArray(p);
        primeByteLength = primeByteArray.length;
    }

    @Override
    public ZpPolyFactory.ZpTreePolyType getType() {
        return ZpPolyFactory.ZpTreePolyType.NTL_TREE;
    }

    @Override
    public void prepareInterpolateBinaryTree(BigInteger[] xArray) {
        assert xArray.length > 0 : "x.length must be greater than 0:" + xArray.length;
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }
        interpolatePointNum = xArray.length;
        byte[][] xByteArrays = BigIntegerUtils.nonNegBigIntegersToByteArrays(xArray, primeByteLength);
        // 构造满二叉树
        interpolateBinaryTreeHandler = nativeBuildBinaryTree(primeByteArray, xByteArrays);
        // 计算导数的逆
        derivativeInversesHandler = nativeBuildDerivativeInverses(interpolateBinaryTreeHandler, xArray.length);
    }

    @Override
    public void destroyInterpolateBinaryTree() {
        interpolatePointNum = 0;
        if (interpolateBinaryTreeHandler != null) {
            nativeDestroyBinaryTree(interpolateBinaryTreeHandler);
            interpolateBinaryTreeHandler = null;
        }
        if (derivativeInversesHandler != null) {
            nativeDestroyDerivativeInverses(derivativeInversesHandler);
            derivativeInversesHandler = null;
        }
    }

    @Override
    public BigInteger[] interpolate(BigInteger[] yArray) {
        assert yArray.length == interpolatePointNum
            : "y.length must be equal to x.length = " + interpolatePointNum + ": " + yArray.length;
        for (BigInteger y : yArray) {
            assert validPoint(y);
        }
        byte[][] yByteArray = BigIntegerUtils.nonNegBigIntegersToByteArrays(yArray, primeByteLength);
        // 调用本地函数完成插值
        byte[][] polynomial
            = nativeInterpolate(primeByteArray, interpolateBinaryTreeHandler, derivativeInversesHandler, yByteArray);
        // 转换为大整数
        return BigIntegerUtils.byteArraysToNonNegBigIntegers(polynomial);
    }

    private static native ByteBuffer nativeBuildBinaryTree(byte[] primeByteArray, byte[][] xByteArrays);

    private static native ByteBuffer nativeBuildDerivativeInverses(ByteBuffer binaryTreeHandler, int pointNum);

    private static native void nativeDestroyBinaryTree(ByteBuffer binaryTreeHandler);

    private static native void nativeDestroyDerivativeInverses(ByteBuffer derivativeInversesHandler);

    private static native byte[][] nativeInterpolate(
        byte[] primeByteArray, ByteBuffer interpolateTreeHandler, ByteBuffer derivativeInversesHandler, byte[][] yByteArrays
    );

    @Override
    public void prepareEvaluateBinaryTrees(int evaluatePolynomialPointNum, BigInteger[] xArray) {
        assert evaluatePolynomialPointNum > 0
            : "evaluate polynomial point num must be greater than 0: " + evaluatePolynomialPointNum;
        assert xArray.length > 0 : "x.length must be greater than 0:" + xArray.length;
        for (BigInteger x : xArray) {
            assert validPoint(x);
        }
        evaluatePointNum = xArray.length;
        this.evaluatePolynomialPointNum = evaluatePolynomialPointNum;
        byte[][] xByteArrays = BigIntegerUtils.nonNegBigIntegersToByteArrays(xArray, primeByteLength);
        if (xArray.length == 1) {
            singleEvaluatePoint = xByteArrays[0];
        } else {
            // 一次可以并行计算的阶数要求是离polynomial.degree()最近的n = 2^k
            intervalPointNum = 1 << (LongUtils.ceilLog2(evaluatePolynomialPointNum, 1) - 1);
            int intervalNum = CommonUtils.getUnitNum(evaluatePointNum, intervalPointNum);
            evaluateTreeHandlerArrayList = new ArrayList<>(intervalNum);
            for (int pointIndex = 0; pointIndex < evaluatePointNum; pointIndex += intervalPointNum) {
                // 一次取出maxNum个点，如果不足则后面补0
                byte[][] intervalPoints = new byte[intervalPointNum][primeByteLength];
                int minCopy = Math.min(intervalPointNum, xByteArrays.length - pointIndex);
                System.arraycopy(xByteArrays, pointIndex, intervalPoints, 0, minCopy);
                evaluateTreeHandlerArrayList.add(nativeBuildBinaryTree(primeByteArray, intervalPoints));
            }
        }
    }

    @Override
    public void destroyEvaluateBinaryTree() {
        evaluatePointNum = 0;
        evaluatePolynomialPointNum = 0;
        singleEvaluatePoint = null;
        intervalPointNum = 0;
        if (evaluateTreeHandlerArrayList != null && evaluateTreeHandlerArrayList.size() > 0) {
            for (ByteBuffer evaluateTreeHandler : evaluateTreeHandlerArrayList) {
                nativeDestroyBinaryTree(evaluateTreeHandler);
            }
            evaluateTreeHandlerArrayList = null;
        }
    }

    @Override
    public BigInteger[] evaluate(BigInteger[] coefficients) {
        assert coefficients.length - 1 == evaluatePolynomialPointNum
            : "coefficient.length must be equal to " + (evaluatePolynomialPointNum + 1) + ": " + coefficients.length;
        for (BigInteger coefficient : coefficients) {
            assert validPoint(coefficient);
        }
        byte[][] coefficientByteArrays = BigIntegerUtils.nonNegBigIntegersToByteArrays(coefficients, primeByteLength);
        if (evaluatePointNum == 1) {
            // 如果只对一个点求值，则直接返回结果
            byte[] yByteArray = nativeSingleEvaluate(primeByteArray, coefficientByteArrays, singleEvaluatePoint);
            return new BigInteger[]{BigIntegerUtils.byteArrayToNonNegBigInteger(yByteArray)};
        }
        byte[][] yByteArrays = new byte[evaluatePointNum][primeByteLength];
        int evaluateBinaryTreeIndex = 0;
        for (int pointIndex = 0; pointIndex < evaluatePointNum; pointIndex += intervalPointNum) {
            int minCopy = Math.min(intervalPointNum, evaluatePointNum - pointIndex);
            byte[][] intervalValues
                = nativeTreeEvaluate(primeByteArray, coefficientByteArrays, evaluateTreeHandlerArrayList.get(evaluateBinaryTreeIndex), intervalPointNum);
            evaluateBinaryTreeIndex++;
            System.arraycopy(intervalValues, 0, yByteArrays, pointIndex, minCopy);
        }
        return BigIntegerUtils.byteArraysToNonNegBigIntegers(yByteArrays);
    }

    private static native byte[] nativeSingleEvaluate(byte[] primeByteArray, byte[][] coefficients, byte[] xByteArray);

    private static native byte[][] nativeTreeEvaluate(byte[] primeByteArray, byte[][] coefficients, ByteBuffer binaryTreeHandler, int pointNum);
}
