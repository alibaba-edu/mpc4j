package edu.alibaba.mpc4j.s2pc.aby.basics.bc.vector;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.Arrays;

/**
 * Receiver test thread for vector Boolean circuit unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcVectorUnaryReceiverThread extends Thread {
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
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * share x1 array
     */
    private SquareSbitVector[] shareX1s;
    /**
     * final x1 array
     */
    private SquareSbitVector[] finalX1s;
    /**
     * z (plain)
     */
    private BitVector[] z1Vectors;
    /**
     * z (secret)
     */
    private BitVector[] z0Vectors;

    BcVectorUnaryReceiverThread(BcParty bcSender, BcOperator bcOperator, BitVector[] xBitVectors) {
        this.bcSender = bcSender;
        this.bcOperator = bcOperator;
        this.xBitVectors = xBitVectors;
        totalBitNum = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).sum();
    }

    BitVector[] getZ1Vectors() {
        return z1Vectors;
    }

    BitVector[] getZ0Vectors() {
        return z0Vectors;
    }

    SquareSbitVector[] getShareX1s() {
        return shareX1s;
    }

    SquareSbitVector[] getFinalX1s() {
        return finalX1s;
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
            int[] xLengths = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).toArray();
            SquareSbitVector[] x1s = bcSender.shareOther(xLengths);
            shareX1s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
            SquareSbitVector[] z01s, z11s;
            //noinspection SwitchStatementWithTooFewBranches
            switch (bcOperator) {
                case NOT:
                    // (plain, plain)
                    z01s = bcSender.not(xs);
                    bcSender.revealOther(z01s);
                    z0Vectors = bcSender.revealOwn(z01s);
                    // (plain, secret)
                    z11s = bcSender.not(x1s);
                    finalX1s = Arrays.stream(x1s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    bcSender.revealOther(z11s);
                    z1Vectors = bcSender.revealOwn(z11s);
                    break;
                default:
                    throw new IllegalStateException("Invalid unary boolean operator: " + bcOperator.name());
            }
            bcSender.getRpc().disconnect();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
