package edu.alibaba.mpc4j.s2pc.aby.basics.bc.vector;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.Arrays;

/**
 * Receiver test thread for vector Boolean circuit binary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcVectorBinaryReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BcParty bcReceiver;
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
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * share x1 array
     */
    private SquareSbitVector[] shareX1s;
    /**
     * final x101 array
     */
    private SquareSbitVector[] finalX011s;
    /**
     * final x001 array
     */
    private SquareSbitVector[] finalX001s;
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
     * zi array (secret, secret)
     */
    private BitVector[] z00Vectors;

    BcVectorBinaryReceiverThread(BcParty bcReceiver, BcOperator bcOperator,
                                 BitVector[] xBitVectors, BitVector[] yBitVectors) {
        this.bcReceiver = bcReceiver;
        this.bcOperator = bcOperator;
        this.xBitVectors = xBitVectors;
        this.yBitVectors = yBitVectors;
        totalBitNum = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).sum();
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

    SquareSbitVector[] getShareX1s() {
        return shareX1s;
    }

    SquareSbitVector[] getFinalX011s() {
        return finalX011s;
    }

    SquareSbitVector[] getFinalX001s() {
        return finalX001s;
    }

    @Override
    public void run() {
        try {
            bcReceiver.getRpc().connect();
            bcReceiver.init(totalBitNum, totalBitNum);
            // set inputs
            SquareSbitVector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareSbitVector.create(xBitVector, true))
                .toArray(SquareSbitVector[]::new);
            SquareSbitVector[] ys = Arrays.stream(yBitVectors)
                .map(yBitVector -> SquareSbitVector.create(yBitVector, true))
                .toArray(SquareSbitVector[]::new);
            int[] vectorBitLengths = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareSbitVector[] x1s = bcReceiver.shareOther(vectorBitLengths);
            shareX1s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
            SquareSbitVector[] y1s = bcReceiver.shareOwn(yBitVectors);
            SquareSbitVector[] z111s, z101s, z011s, z001s;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z111s = bcReceiver.xor(xs, ys);
                    bcReceiver.revealOther(z111s);
                    z11Vectors = bcReceiver.revealOwn(z111s);
                    // (plain, secret)
                    z101s = bcReceiver.xor(xs, y1s);
                    bcReceiver.revealOther(z101s);
                    z10Vectors = bcReceiver.revealOwn(z101s);
                    // (secret, plain)
                    z011s = bcReceiver.xor(x1s, ys);
                    finalX011s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcReceiver.revealOther(z011s);
                    z01Vectors = bcReceiver.revealOwn(z011s);
                    // (secret, secret)
                    z001s = bcReceiver.xor(x1s, y1s);
                    finalX001s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcReceiver.revealOther(z001s);
                    z00Vectors = bcReceiver.revealOwn(z001s);
                    break;
                case AND:
                    // (plain, plain)
                    z111s = bcReceiver.and(xs, ys);
                    bcReceiver.revealOther(z111s);
                    z11Vectors = bcReceiver.revealOwn(z111s);
                    // (plain, secret)
                    z101s = bcReceiver.and(xs, y1s);
                    bcReceiver.revealOther(z101s);
                    z10Vectors = bcReceiver.revealOwn(z101s);
                    // (secret, plain)
                    z011s = bcReceiver.and(x1s, ys);
                    finalX011s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcReceiver.revealOther(z011s);
                    z01Vectors = bcReceiver.revealOwn(z011s);
                    // (secret, secret)
                    z001s = bcReceiver.and(x1s, y1s);
                    finalX001s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcReceiver.revealOther(z001s);
                    z00Vectors = bcReceiver.revealOwn(z001s);
                    break;
                case OR:
                    // (plain, plain)
                    z111s = bcReceiver.or(xs, ys);
                    bcReceiver.revealOther(z111s);
                    z11Vectors = bcReceiver.revealOwn(z111s);
                    // (plain, secret)
                    z101s = bcReceiver.or(xs, y1s);
                    bcReceiver.revealOther(z101s);
                    z10Vectors = bcReceiver.revealOwn(z101s);
                    // (secret, plain)
                    z011s = bcReceiver.or(x1s, ys);
                    finalX011s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcReceiver.revealOther(z011s);
                    z01Vectors = bcReceiver.revealOwn(z011s);
                    // (secret, secret)
                    z001s = bcReceiver.or(x1s, y1s);
                    finalX001s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcReceiver.revealOther(z001s);
                    z00Vectors = bcReceiver.revealOwn(z001s);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
            bcReceiver.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
