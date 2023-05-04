package edu.alibaba.mpc4j.s2pc.aby.basics.bc.single;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcOperator;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.BcParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareZ2Vector;

/**
 * single Boolean circuit receiver thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleDyadicBcReceiverThread extends Thread {
    /**
     * receiver
     */
    private final BcParty receiver;
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
     * number of bits
     */
    private final int bitNum;
    /**
     * share x1
     */
    private SquareZ2Vector shareX1;
    /**
     * final x101
     */
    private SquareZ2Vector finalX011;
    /**
     * final x001
     */
    private SquareZ2Vector finalX001;
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

    SingleDyadicBcReceiverThread(BcParty receiver, BcOperator bcOperator, BitVector xBitVector, BitVector yBitVector) {
        this.receiver = receiver;
        this.bcOperator = bcOperator;
        this.xBitVector = xBitVector;
        this.yBitVector = yBitVector;
        bitNum = xBitVector.bitNum();
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

    SquareZ2Vector getShareX1() {
        return shareX1;
    }

    SquareZ2Vector getFinalX011() {
        return finalX011;
    }

    SquareZ2Vector getFinalX001() {
        return finalX001;
    }

    @Override
    public void run() {
        try {
            receiver.init(bitNum, bitNum);
            // generate x and y
            SquareZ2Vector x = SquareZ2Vector.create(xBitVector, true);
            SquareZ2Vector y = SquareZ2Vector.create(yBitVector, true);
            SquareZ2Vector x1 = receiver.shareOther(bitNum);
            shareX1 = x1.copy();
            SquareZ2Vector y1 = receiver.shareOwn(yBitVector);
            SquareZ2Vector z111, z101, z011, z001;
            switch (bcOperator) {
                case XOR:
                    // (plain, plain)
                    z111 = receiver.xor(x, y);
                    receiver.revealOther(z111);
                    z11Vector = receiver.revealOwn(z111);
                    // (plain, secret)
                    z101 = receiver.xor(x, y1);
                    receiver.revealOther(z101);
                    z10Vector = receiver.revealOwn(z101);
                    // (secret, plain)
                    z011 = receiver.xor(x1, y);
                    finalX011 = x1.copy();
                    receiver.revealOther(z011);
                    z01Vector = receiver.revealOwn(z011);
                    // (secret, secret)
                    z001 = receiver.xor(x1, y1);
                    finalX001 = x1.copy();
                    receiver.revealOther(z001);
                    z00Vector = receiver.revealOwn(z001);
                    break;
                case AND:
                    // (plain, plain)
                    z111 = receiver.and(x, y);
                    receiver.revealOther(z111);
                    z11Vector = receiver.revealOwn(z111);
                    // (plain, secret)
                    z101 = receiver.and(x, y1);
                    receiver.revealOther(z101);
                    z10Vector = receiver.revealOwn(z101);
                    // (secret, plain)
                    z011 = receiver.and(x1, y);
                    finalX011 = x1.copy();
                    receiver.revealOther(z011);
                    z01Vector = receiver.revealOwn(z011);
                    // (secret, secret)
                    z001 = receiver.and(x1, y1);
                    finalX001 = x1.copy();
                    receiver.revealOther(z001);
                    z00Vector = receiver.revealOwn(z001);
                    break;
                case OR:
                    // (plain, plain)
                    z111 = receiver.or(x, y);
                    receiver.revealOther(z111);
                    z11Vector = receiver.revealOwn(z111);
                    // (plain, secret)
                    z101 = receiver.or(x, y1);
                    receiver.revealOther(z101);
                    z10Vector = receiver.revealOwn(z101);
                    // (secret, plain)
                    z011 = receiver.or(x1, y);
                    finalX011 = x1.copy();
                    receiver.revealOther(z011);
                    z01Vector = receiver.revealOwn(z011);
                    // (secret, secret)
                    z001 = receiver.or(x1, y1);
                    finalX001 = x1.copy();
                    receiver.revealOther(z001);
                    z00Vector = receiver.revealOwn(z001);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + BcOperator.class.getSimpleName() + ": " + bcOperator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
