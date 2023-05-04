package edu.alibaba.mpc4j.s2pc.aby.basics.bc.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.util.Arrays;

/**
 * batch Boolean circuit receiver thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchDyadicBcReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BcParty receiver;
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
    private SquareZ2Vector[] shareX1s;
    /**
     * final x101 array
     */
    private SquareZ2Vector[] finalX011s;
    /**
     * final x001 array
     */
    private SquareZ2Vector[] finalX001s;
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

    BatchDyadicBcReceiverThread(BcParty receiver, BcOperator bcOperator,
                                BitVector[] xBitVectors, BitVector[] yBitVectors) {
        this.receiver = receiver;
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

    SquareZ2Vector[] getShareX1s() {
        return shareX1s;
    }

    SquareZ2Vector[] getFinalX011s() {
        return finalX011s;
    }

    SquareZ2Vector[] getFinalX001s() {
        return finalX001s;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalBitNum, totalBitNum);
            // set inputs
            SquareZ2Vector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareZ2Vector.create(xBitVector, true))
                .toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] ys = Arrays.stream(yBitVectors)
                .map(yBitVector -> SquareZ2Vector.create(yBitVector, true))
                .toArray(SquareZ2Vector[]::new);
            int[] vectorBitLengths = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareZ2Vector[] x1s = receiver.shareOther(vectorBitLengths);
            shareX1s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] y1s = receiver.shareOwn(yBitVectors);
            SquareZ2Vector[] z111s, z101s, z011s, z001s;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z111s = receiver.xor(xs, ys);
                    receiver.revealOther(z111s);
                    z11Vectors = receiver.revealOwn(z111s);
                    // (plain, secret)
                    z101s = receiver.xor(xs, y1s);
                    receiver.revealOther(z101s);
                    z10Vectors = receiver.revealOwn(z101s);
                    // (secret, plain)
                    z011s = receiver.xor(x1s, ys);
                    finalX011s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    receiver.revealOther(z011s);
                    z01Vectors = receiver.revealOwn(z011s);
                    // (secret, secret)
                    z001s = receiver.xor(x1s, y1s);
                    finalX001s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    receiver.revealOther(z001s);
                    z00Vectors = receiver.revealOwn(z001s);
                    break;
                case AND:
                    // (plain, plain)
                    z111s = receiver.and(xs, ys);
                    receiver.revealOther(z111s);
                    z11Vectors = receiver.revealOwn(z111s);
                    // (plain, secret)
                    z101s = receiver.and(xs, y1s);
                    receiver.revealOther(z101s);
                    z10Vectors = receiver.revealOwn(z101s);
                    // (secret, plain)
                    z011s = receiver.and(x1s, ys);
                    finalX011s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    receiver.revealOther(z011s);
                    z01Vectors = receiver.revealOwn(z011s);
                    // (secret, secret)
                    z001s = receiver.and(x1s, y1s);
                    finalX001s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    receiver.revealOther(z001s);
                    z00Vectors = receiver.revealOwn(z001s);
                    break;
                case OR:
                    // (plain, plain)
                    z111s = receiver.or(xs, ys);
                    receiver.revealOther(z111s);
                    z11Vectors = receiver.revealOwn(z111s);
                    // (plain, secret)
                    z101s = receiver.or(xs, y1s);
                    receiver.revealOther(z101s);
                    z10Vectors = receiver.revealOwn(z101s);
                    // (secret, plain)
                    z011s = receiver.or(x1s, ys);
                    finalX011s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    receiver.revealOther(z011s);
                    z01Vectors = receiver.revealOwn(z011s);
                    // (secret, secret)
                    z001s = receiver.or(x1s, y1s);
                    finalX001s = Arrays.stream(x1s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    receiver.revealOther(z001s);
                    z00Vectors = receiver.revealOwn(z001s);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
