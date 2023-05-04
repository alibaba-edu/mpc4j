package edu.alibaba.mpc4j.s2pc.aby.basics.bc.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * batch Boolean circuit sender thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchDyadicBcSenderThread extends Thread {
    /**
     * sender
     */
    private final BcParty sender;
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
    private SquareZ2Vector[] shareX0s;
    /**
     * final x100 array
     */
    private SquareZ2Vector[] finalX010s;
    /**
     * final x000 array
     */
    private SquareZ2Vector[] finalX000s;
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

    BatchDyadicBcSenderThread(BcParty sender, BcOperator bcOperator,
                              BitVector[] xBitVectors, BitVector[] yBitVectors) {
        this.sender = sender;
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

    SquareZ2Vector[] getShareX0s() {
        return shareX0s;
    }

    SquareZ2Vector[] getFinalX010s() {
        return finalX010s;
    }

    SquareZ2Vector[] getFinalX000s() {
        return finalX000s;
    }

    @Override
    public void run() {
        try {
            sender.init(totalBitNum, totalBitNum);
            // set inputs
            SquareZ2Vector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareZ2Vector.create(xBitVector, true))
                .toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] ys = Arrays.stream(yBitVectors)
                .map(yBitVector -> SquareZ2Vector.create(yBitVector, true))
                .toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] x0s = sender.shareOwn(xBitVectors);
            shareX0s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
            int[] vectorBitLengths = Arrays.stream(yBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareZ2Vector[] y0s = sender.shareOther(vectorBitLengths);
            SquareZ2Vector[] z110s, z100s, z010s, z000s;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z110s = sender.xor(xs, ys);
                    z11Vectors = sender.revealOwn(z110s);
                    sender.revealOther(z110s);
                    // (plain, secret)
                    z100s = sender.xor(xs, y0s);
                    z10Vectors = sender.revealOwn(z100s);
                    sender.revealOther(z100s);
                    // (secret, plain)
                    z010s = sender.xor(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z01Vectors = sender.revealOwn(z010s);
                    sender.revealOther(z010s);
                    // (secret, secret)
                    z000s = sender.xor(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z00Vectors = sender.revealOwn(z000s);
                    sender.revealOther(z000s);
                    break;
                case AND:
                    // (plain, plain)
                    z110s = sender.and(xs, ys);
                    z11Vectors = sender.revealOwn(z110s);
                    sender.revealOther(z110s);
                    // (plain, secret)
                    z100s = sender.and(xs, y0s);
                    z10Vectors = sender.revealOwn(z100s);
                    sender.revealOther(z100s);
                    // (secret, plain)
                    z010s = sender.and(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z01Vectors = sender.revealOwn(z010s);
                    sender.revealOther(z010s);
                    // (secret, secret)
                    z000s = sender.and(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z00Vectors = sender.revealOwn(z000s);
                    sender.revealOther(z000s);
                    break;
                case OR:
                    // (plain, plain)
                    z110s = sender.or(xs, ys);
                    z11Vectors = sender.revealOwn(z110s);
                    sender.revealOther(z110s);
                    // (plain, secret)
                    z100s = sender.or(xs, y0s);
                    z10Vectors = sender.revealOwn(z100s);
                    sender.revealOther(z100s);
                    // (secret, plain)
                    z010s = sender.or(x0s, ys);
                    finalX010s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z01Vectors = sender.revealOwn(z010s);
                    sender.revealOther(z010s);
                    // (secret, secret)
                    z000s = sender.or(x0s, y0s);
                    finalX000s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z00Vectors = sender.revealOwn(z000s);
                    sender.revealOther(z000s);
                    break;
                default:
                    throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
