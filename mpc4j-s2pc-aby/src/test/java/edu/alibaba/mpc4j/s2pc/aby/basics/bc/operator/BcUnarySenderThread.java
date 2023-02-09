package edu.alibaba.mpc4j.s2pc.aby.basics.bc.operator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * Sender test thread for Boolean circuit unary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class BcUnarySenderThread extends Thread {
    /**
     * sender
     */
    private final BcParty bcSender;
    /**
     * operator
     */
    private final BcOperator bcOperator;
    /**
     * x bit vector
     */
    private final BitVector xBitVector;
    /**
     * expect bit vector
     */
    private final BitVector expectBitVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * share x0
     */
    private SquareSbitVector shareX0;
    /**
     * final x0
     */
    private SquareSbitVector finalX0;
    /**
     * z (plain)
     */
    private BitVector z1Vector;
    /**
     * z (secret)
     */
    private BitVector z0Vector;

    BcUnarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector xBitVector) {
        this.bcSender = bcSender;
        this.bcOperator = bcOperator;
        this.xBitVector = xBitVector;
        bitNum = xBitVector.bitNum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (bcOperator) {
            case NOT:
                expectBitVector = xBitVector.not();
                break;
            default:
                throw new IllegalStateException("Invalid unary boolean operator: " + bcOperator.name());
        }
    }

    BitVector getExpectVector() {
        return expectBitVector;
    }

    BitVector getZ1Vector() {
        return z1Vector;
    }

    BitVector getZ0Vector() {
        return z0Vector;
    }

    SquareSbitVector getShareX0() {
        return shareX0;
    }

    SquareSbitVector getFinalX0() {
        return finalX0;
    }

    @Override
    public void run() {
        try {
            bcSender.getRpc().connect();
            bcSender.init(bitNum, bitNum);
            // set inputs
            SquareSbitVector x = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector x0 = bcSender.shareOwn(xBitVector);
            shareX0 = x0.copy();
            SquareSbitVector z00, z10;
            //noinspection SwitchStatementWithTooFewBranches
            switch (bcOperator) {
                case NOT:
                    // (plain, plain)
                    z00 = bcSender.not(x);
                    z0Vector = bcSender.revealOwn(z00);
                    bcSender.revealOther(z00);
                    // (plain, secret)
                    z10 = bcSender.not(x0);
                    finalX0 = x0.copy();
                    z1Vector = bcSender.revealOwn(z10);
                    bcSender.revealOther(z10);
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
