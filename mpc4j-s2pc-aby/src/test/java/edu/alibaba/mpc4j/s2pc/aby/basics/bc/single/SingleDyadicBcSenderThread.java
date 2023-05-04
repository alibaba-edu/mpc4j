package edu.alibaba.mpc4j.s2pc.aby.basics.bc.single;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

/**
 * single Boolean circuit sender thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleDyadicBcSenderThread extends Thread {
    /**
     * sender
     */
    private final BcParty sender;
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
    private SquareZ2Vector shareX0;
    /**
     * final x100
     */
    private SquareZ2Vector finalX010;
    /**
     * final x000
     */
    private SquareZ2Vector finalX000;
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

    SingleDyadicBcSenderThread(BcParty sender, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
        this.sender = sender;
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

    SquareZ2Vector getShareX0() {
        return shareX0;
    }

    SquareZ2Vector getFinalX010() {
        return finalX010;
    }

    SquareZ2Vector getFinalX000() {
        return finalX000;
    }

    @Override
    public void run() {
        try {
            sender.init(bitNum, bitNum);
            // generate x and y
            SquareZ2Vector x = SquareZ2Vector.create(xBitVector, true);
            SquareZ2Vector y = SquareZ2Vector.create(yBitVector, true);
            SquareZ2Vector x0 = sender.shareOwn(xBitVector);
            shareX0 = x0.copy();
            SquareZ2Vector y0 = sender.shareOther(bitNum);
            SquareZ2Vector z110, z100, z010, z000;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z110 = sender.xor(x, y);
                    z11Vector = sender.revealOwn(z110);
                    sender.revealOther(z110);
                    // (plain, secret)
                    z100 = sender.xor(x, y0);
                    z10Vector = sender.revealOwn(z100);
                    sender.revealOther(z100);
                    // (secret, plain)
                    z010 = sender.xor(x0, y);
                    finalX010 = x0.copy();
                    z01Vector = sender.revealOwn(z010);
                    sender.revealOther(z010);
                    // (secret, secret)
                    z000 = sender.xor(x0, y0);
                    finalX000 = x0.copy();
                    z00Vector = sender.revealOwn(z000);
                    sender.revealOther(z000);
                    break;
                case AND:
                    // (plain, plain)
                    z110 = sender.and(x, y);
                    z11Vector = sender.revealOwn(z110);
                    sender.revealOther(z110);
                    // (plain, secret)
                    z100 = sender.and(x, y0);
                    z10Vector = sender.revealOwn(z100);
                    sender.revealOther(z100);
                    // (secret, plain)
                    z010 = sender.and(x0, y);
                    finalX010 = x0.copy();
                    z01Vector = sender.revealOwn(z010);
                    sender.revealOther(z010);
                    // (secret, secret)
                    z000 = sender.and(x0, y0);
                    finalX000 = x0.copy();
                    z00Vector = sender.revealOwn(z000);
                    sender.revealOther(z000);
                    break;
                case OR:
                    // (plain, plain)
                    z110 = sender.or(x, y);
                    z11Vector = sender.revealOwn(z110);
                    sender.revealOther(z110);
                    // (plain, secret)
                    z100 = sender.or(x, y0);
                    z10Vector = sender.revealOwn(z100);
                    sender.revealOther(z100);
                    // (secret, plain)
                    z010 = sender.or(x0, y);
                    finalX010 = x0.copy();
                    z01Vector = sender.revealOwn(z010);
                    sender.revealOther(z010);
                    // (secret, secret)
                    z000 = sender.or(x0, y0);
                    finalX000 = x0.copy();
                    z00Vector = sender.revealOwn(z000);
                    sender.revealOther(z000);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + BcOperator.class.getSimpleName() + ": " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
