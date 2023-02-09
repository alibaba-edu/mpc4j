package edu.alibaba.mpc4j.s2pc.aby.basics.bc.vector;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

import java.util.Arrays;

/**
 * Sender test thread for vector Boolean circuit unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BcVectorUnarySenderThread extends Thread {
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
     * expect bit vectors
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
     * final x0 array
     */
    private SquareSbitVector[] finalX0s;
    /**
     * z array (plain)
     */
    private BitVector[] z1Vectors;
    /**
     * z array (secret)
     */
    private BitVector[] z0Vectors;

    BcVectorUnarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector[] xBitVectors) {
        this.bcSender = bcSender;
        this.bcOperator = bcOperator;
        this.xBitVectors = xBitVectors;
        totalBitNum = Arrays.stream(xBitVectors).mapToInt(BitVector::bitNum).sum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (bcOperator) {
            case NOT:
                expectBitVectors = Arrays.stream(xBitVectors).map(BitVector::not).toArray(BitVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid unary boolean operator: " + bcOperator.name());
        }
    }

    BitVector[] getExpectBitVectors() {
        return expectBitVectors;
    }

    BitVector[] getZ1Vectors() {
        return z1Vectors;
    }

    BitVector[] getZ0Vectors() {
        return z0Vectors;
    }

    SquareSbitVector[] getShareX0s() {
        return shareX0s;
    }

    SquareSbitVector[] getFinalX0s() {
        return finalX0s;
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
            SquareSbitVector[] x0s = bcSender.shareOwn(xBitVectors);
            shareX0s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
            SquareSbitVector[] z00s, z10s;
            //noinspection SwitchStatementWithTooFewBranches
            switch (bcOperator) {
                case NOT:
                    // (plain, plain)
                    z00s = bcSender.not(xs);
                    z0Vectors = bcSender.revealOwn(z00s);
                    bcSender.revealOther(z00s);
                    // (plain, secret)
                    z10s = bcSender.not(x0s);
                    finalX0s = Arrays.stream(x0s).map(SquareSbitVector::copy).toArray(SquareSbitVector[]::new);
                    z1Vectors = bcSender.revealOwn(z10s);
                    bcSender.revealOther(z10s);
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
