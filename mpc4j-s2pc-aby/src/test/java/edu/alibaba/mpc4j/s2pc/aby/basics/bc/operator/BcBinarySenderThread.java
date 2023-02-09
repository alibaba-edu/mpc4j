package edu.alibaba.mpc4j.s2pc.aby.basics.bc.operator;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;

/**
 * Sender test thread for Boolean circuit binary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class BcBinarySenderThread extends Thread {
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
     * y bit vector
     */
    private final BitVector yBitVector;
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
     * final x100
     */
    private SquareSbitVector finalX010;
    /**
     * final x000
     */
    private SquareSbitVector finalX000;
    /**
     * z (plain, plain)
     */
    private BitVector z11Vector;
    /**
     * z (plain, secret)
     */
    private BitVector z10Vector;
    /**
     * z (secret, plain)
     */
    private BitVector z01Vector;
    /**
     * z (secret, secret)
     */
    private BitVector z00Vector;

    BcBinarySenderThread(BcParty bcSender, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
        this.bcSender = bcSender;
        this.bcOperator = bcOperator;
        this.xBitVector = xBitVector;
        this.yBitVector = yBitVector;
        bitNum = xBitVector.bitNum();
        switch (bcOperator) {
            case XOR:
                expectBitVector = xBitVector.xor(yBitVector);
                break;
            case AND:
                expectBitVector = xBitVector.and(yBitVector);
                break;
            case OR:
                expectBitVector = xBitVector.or(yBitVector);
                break;
            default:
                throw new IllegalStateException("Invalid binary boolean operator: " + bcOperator.name());
        }
    }

    BitVector getExpectVector() {
        return expectBitVector;
    }

    BitVector getZ11Vector() {
        return z11Vector;
    }

    BitVector getZ10Vector() {
        return z10Vector;
    }

    BitVector getZ01Vector() {
        return z01Vector;
    }

    BitVector getZ00Vector() {
        return z00Vector;
    }

    SquareSbitVector getShareX0() {
        return shareX0;
    }

    SquareSbitVector getFinalX010() {
        return finalX010;
    }

    SquareSbitVector getFinalX000() {
        return finalX000;
    }

    @Override
    public void run() {
        try {
            bcSender.getRpc().connect();
            bcSender.init(bitNum, bitNum);
            // generate x and y
            SquareSbitVector x = SquareSbitVector.create(xBitVector, true);
            SquareSbitVector y = SquareSbitVector.create(yBitVector, true);
            SquareSbitVector x0 = bcSender.shareOwn(xBitVector);
            shareX0 = x0.copy();
            SquareSbitVector y0 = bcSender.shareOther(bitNum);
            SquareSbitVector z110, z100, z010, z000;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z110 = bcSender.xor(x, y);
                    z11Vector = bcSender.revealOwn(z110);
                    bcSender.revealOther(z110);
                    // (plain, secret)
                    z100 = bcSender.xor(x, y0);
                    z10Vector = bcSender.revealOwn(z100);
                    bcSender.revealOther(z100);
                    // (secret, plain)
                    z010 = bcSender.xor(x0, y);
                    finalX010 = x0.copy();
                    z01Vector = bcSender.revealOwn(z010);
                    bcSender.revealOther(z010);
                    // (secret, secret)
                    z000 = bcSender.xor(x0, y0);
                    finalX000 = x0.copy();
                    z00Vector = bcSender.revealOwn(z000);
                    bcSender.revealOther(z000);
                    break;
                case AND:
                    // (plain, plain)
                    z110 = bcSender.and(x, y);
                    z11Vector = bcSender.revealOwn(z110);
                    bcSender.revealOther(z110);
                    // (plain, secret)
                    z100 = bcSender.and(x, y0);
                    z10Vector = bcSender.revealOwn(z100);
                    bcSender.revealOther(z100);
                    // (secret, plain)
                    z010 = bcSender.and(x0, y);
                    finalX010 = x0.copy();
                    z01Vector = bcSender.revealOwn(z010);
                    bcSender.revealOther(z010);
                    // (secret, secret)
                    z000 = bcSender.and(x0, y0);
                    finalX000 = x0.copy();
                    z00Vector = bcSender.revealOwn(z000);
                    bcSender.revealOther(z000);
                    break;
                case OR:
                    // (plain, plain)
                    z110 = bcSender.or(x, y);
                    z11Vector = bcSender.revealOwn(z110);
                    bcSender.revealOther(z110);
                    // (plain, secret)
                    z100 = bcSender.or(x, y0);
                    z10Vector = bcSender.revealOwn(z100);
                    bcSender.revealOther(z100);
                    // (secret, plain)
                    z010 = bcSender.or(x0, y);
                    finalX010 = x0.copy();
                    z01Vector = bcSender.revealOwn(z010);
                    bcSender.revealOther(z010);
                    // (secret, secret)
                    z000 = bcSender.or(x0, y0);
                    finalX000 = x0.copy();
                    z00Vector = bcSender.revealOwn(z000);
                    bcSender.revealOther(z000);
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
