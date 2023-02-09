package edu.alibaba.mpc4j.s2pc.aby.basics.bc.vector;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Sender test thread for vector Boolean circuit binary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcVectorBinarySenderThread extends Thread {
    /**
     * sender
     */
    private final BcParty bcSender;
    /**
     * operator
     */
    private final BcOperator bcOperator;
    /**
     * x bit vectors
     */
    private final BitVector[] xBitVectors;
    /**
     * y bit vectors
     */
    private final BitVector[] yBitVectors;
    /**
     * expect bit vector
     */
    private final BitVector[] expectBitVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * share x0 array
     */
    private SquareSbitVector[] shareX0s;
    /**
     * final x100 array
     */
    private SquareSbitVector[] finalX010s;
    /**
     * final x000 array
     */
    private SquareSbitVector[] finalX000s;
    /**
     * z array (plain, plain)
     */
    private BitVector[] z11Vectors;
    /**
     * z array (plain, secret)
     */
    private BitVector[] z10Vectors;
    /**
     * z array (secret, plain)
     */
    private BitVector[] z01Vectors;
    /**
     * z array (secret, secret)
     */
    private BitVector[] z00Vectors;

    BcVectorBinarySenderThread(BcParty bcSender, BcOperator bcOperator,
                               BitVector[] xBitVectors, BitVector[] yBitVectors) {
        this.bcSender = bcSender;
        this.bcOperator = bcOperator;
        this.xBitVectors = xBitVectors;
        this.yBitVectors = yBitVectors;
        totalBitNum = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).sum();
        int vectorLength = xBitVectors.length;
        switch (bcOperator) {
            case XOR:
                expectBitVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xBitVectors[index].xor(yBitVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            case AND:
                expectBitVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xBitVectors[index].and(yBitVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            case OR:
                expectBitVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xBitVectors[index].or(yBitVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
        }
    }

    BitVector[] getExpectVectors() {
        return expectBitVectors;
    }

    BitVector[] getZ11Vectors() {
        return z11Vectors;
    }

    BitVector[] getZ10Vectors() {
        return z10Vectors;
    }

    BitVector[] getZ01Vectors() {
        return z01Vectors;
    }

    BitVector[] getZ00Vectors() {
        return z00Vectors;
    }

    SquareSbitVector[] getShareX0s() {
        return shareX0s;
    }

    SquareSbitVector[] getFinalX010s() {
        return finalX010s;
    }

    SquareSbitVector[] getFinalX000s() {
        return finalX000s;
    }

    @Override
    public void run() {
        try {
            bcSender.getRpc().connect();
            bcSender.init(totalBitNum, totalBitNum);
            // set inputs
            SquareSbitVector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareSbitVector.create(xBitVector, true))
                .toArray(SquareSbitVector[]::new);
            SquareSbitVector[] ys = Arrays.stream(yBitVectors)
                .map(yBitVector -> SquareSbitVector.create(yBitVector, true))
                .toArray(SquareSbitVector[]::new);
            SquareSbitVector[] x0s = bcSender.shareOwn(xBitVectors);
            shareX0s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
            int[] vectorBitLengths = Arrays.stream(yBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareSbitVector[] y0s = bcSender.shareOther(vectorBitLengths);
            SquareSbitVector[] z110s, z100s, z010s, z000s;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z110s = bcSender.xor(xs, ys);
                    z11Vectors = bcSender.revealOwn(z110s);
                    bcSender.revealOther(z110s);
                    // (plain, secret)
                    z100s = bcSender.xor(xs, y0s);
                    z10Vectors = bcSender.revealOwn(z100s);
                    bcSender.revealOther(z100s);
                    // (secret, plain)
                    z010s = bcSender.xor(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z01Vectors = bcSender.revealOwn(z010s);
                    bcSender.revealOther(z010s);
                    // (secret, secret)
                    z000s = bcSender.xor(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z00Vectors = bcSender.revealOwn(z000s);
                    bcSender.revealOther(z000s);
                    break;
                case AND:
                    // (plain, plain)
                    z110s = bcSender.and(xs, ys);
                    z11Vectors = bcSender.revealOwn(z110s);
                    bcSender.revealOther(z110s);
                    // (plain, secret)
                    z100s = bcSender.and(xs, y0s);
                    z10Vectors = bcSender.revealOwn(z100s);
                    bcSender.revealOther(z100s);
                    // (secret, plain)
                    z010s = bcSender.and(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z01Vectors = bcSender.revealOwn(z010s);
                    bcSender.revealOther(z010s);
                    // (secret, secret)
                    z000s = bcSender.and(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z00Vectors = bcSender.revealOwn(z000s);
                    bcSender.revealOther(z000s);
                    break;
                case OR:
                    // (plain, plain)
                    z110s = bcSender.or(xs, ys);
                    z11Vectors = bcSender.revealOwn(z110s);
                    bcSender.revealOther(z110s);
                    // (plain, secret)
                    z100s = bcSender.or(xs, y0s);
                    z10Vectors = bcSender.revealOwn(z100s);
                    bcSender.revealOther(z100s);
                    // (secret, plain)
                    z010s = bcSender.or(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z01Vectors = bcSender.revealOwn(z010s);
                    bcSender.revealOther(z010s);
                    // (secret, secret)
                    z000s = bcSender.or(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z00Vectors = bcSender.revealOwn(z000s);
                    bcSender.revealOther(z000s);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
            bcSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
