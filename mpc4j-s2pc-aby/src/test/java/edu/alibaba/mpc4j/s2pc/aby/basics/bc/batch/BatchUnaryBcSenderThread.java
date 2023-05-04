package edu.alibaba.mpc4j.s2pc.aby.basics.bc.batch;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

import java.util.Arrays;

/**
 * batch Boolean circuit sender thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchUnaryBcSenderThread extends Thread {
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
    private SquareZ2Vector[] shareX0s;
    /**
     * final x0 array
     */
    private SquareZ2Vector[] finalX0s;
    /**
     * z array (plain)
     */
    private BitVector[] z1Vectors;
    /**
     * z array (secret)
     */
    private BitVector[] z0Vectors;

    BatchUnaryBcSenderThread(BcParty sender, BcOperator bcOperator, BitVector[] xBitVectors) {
        this.sender = sender;
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

    SquareZ2Vector[] getShareX0s() {
        return shareX0s;
    }

    SquareZ2Vector[] getFinalX0s() {
        return finalX0s;
    }

    @Override
    public void run() {
        try {
            sender.init(totalBitNum, totalBitNum);
            // set inputs
            SquareZ2Vector[] xs = Arrays.stream(xBitVectors)
                .map(xBitVector -> SquareZ2Vector.create(xBitVector, true))
                .toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] x0s = sender.shareOwn(xBitVectors);
            shareX0s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
            SquareZ2Vector[] z00s, z10s;
            //noinspection SwitchStatementWithTooFewBranches
            switch (bcOperator) {
                case NOT:
                    // (plain, plain)
                    z00s = sender.not(xs);
                    z0Vectors = sender.revealOwn(z00s);
                    sender.revealOther(z00s);
                    // (plain, secret)
                    z10s = sender.not(x0s);
                    finalX0s = Arrays.stream(x0s).map(SquareZ2Vector::copy).toArray(SquareZ2Vector[]::new);
                    z1Vectors = sender.revealOwn(z10s);
                    sender.revealOther(z10s);
                    break;
                default:
                    throw new IllegalStateException("Invalid unary boolean operator: " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
