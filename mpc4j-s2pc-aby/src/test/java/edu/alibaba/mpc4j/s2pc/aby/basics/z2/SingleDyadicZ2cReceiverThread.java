package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * single Boolean circuit receiver thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleDyadicZ2cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Z2cParty receiver;
    /**
     * operator
     */
    private final DyadicBcOperator operator;
    /**
     * x vector
     */
    private final BitVector xVector;
    /**
     * y vector
     */
    private final BitVector yVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * z (plain, plain)
     */
    private BitVector recvPlainPlainVector;
    /**
     * z (plain, secret)
     */
    private BitVector recvPlainSecretVector;
    /**
     * z (secret, plain)
     */
    private BitVector recvSecretPlainVector;
    /**
     * z (secret, secret)
     */
    private BitVector recvSecretSecretVector;

    SingleDyadicZ2cReceiverThread(Z2cParty receiver, DyadicBcOperator operator, BitVector xVector, BitVector yVector) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVector = xVector;
        this.yVector = yVector;
        bitNum = xVector.bitNum();
    }

    BitVector getRecvPlainPlainVector() {
        return recvPlainPlainVector;
    }

    BitVector getRecvPlainSecretVector() {
        return recvPlainSecretVector;
    }

    BitVector getRecvSecretPlainVector() {
        return recvSecretPlainVector;
    }

    BitVector getRecvSecretSecretVector() {
        return recvSecretSecretVector;
    }

    @Override
    public void run() {
        try {
            receiver.init(bitNum);
            // generate x and y
            MpcZ2Vector xPlainMpcVector = receiver.create(true, xVector);
            MpcZ2Vector yPlainMpcVector = receiver.create(true, yVector);
            MpcZ2Vector x1SecretMpcVector = receiver.shareOther(bitNum);
            MpcZ2Vector y1SecretMpcVector = receiver.shareOwn(yVector);
            MpcZ2Vector z1PlainPlainMpcVector, z1PlainSecretMpcVector;
            MpcZ2Vector z1SecretPlainMpcVector, z1SecretSecretMpcVector;
            switch (operator) {
                case XOR:
                    // (plain, plain)
                    z1PlainPlainMpcVector = receiver.xor(xPlainMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1PlainPlainMpcVector);
                    recvPlainPlainVector = receiver.revealOwn(z1PlainPlainMpcVector);
                    // (plain, secret)
                    z1PlainSecretMpcVector = receiver.xor(xPlainMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1PlainSecretMpcVector);
                    recvPlainSecretVector = receiver.revealOwn(z1PlainSecretMpcVector);
                    // (secret, plain)
                    z1SecretPlainMpcVector = receiver.xor(x1SecretMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1SecretPlainMpcVector);
                    recvSecretPlainVector = receiver.revealOwn(z1SecretPlainMpcVector);
                    // (secret, secret)
                    z1SecretSecretMpcVector = receiver.xor(x1SecretMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1SecretSecretMpcVector);
                    recvSecretSecretVector = receiver.revealOwn(z1SecretSecretMpcVector);
                    break;
                case AND:
                    // (plain, plain)
                    z1PlainPlainMpcVector = receiver.and(xPlainMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1PlainPlainMpcVector);
                    recvPlainPlainVector = receiver.revealOwn(z1PlainPlainMpcVector);
                    // (plain, secret)
                    z1PlainSecretMpcVector = receiver.and(xPlainMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1PlainSecretMpcVector);
                    recvPlainSecretVector = receiver.revealOwn(z1PlainSecretMpcVector);
                    // (secret, plain)
                    z1SecretPlainMpcVector = receiver.and(x1SecretMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1SecretPlainMpcVector);
                    recvSecretPlainVector = receiver.revealOwn(z1SecretPlainMpcVector);
                    // (secret, secret)
                    z1SecretSecretMpcVector = receiver.and(x1SecretMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1SecretSecretMpcVector);
                    recvSecretSecretVector = receiver.revealOwn(z1SecretSecretMpcVector);
                    break;
                case OR:
                    // (plain, plain)
                    z1PlainPlainMpcVector = receiver.or(xPlainMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1PlainPlainMpcVector);
                    recvPlainPlainVector = receiver.revealOwn(z1PlainPlainMpcVector);
                    // (plain, secret)
                    z1PlainSecretMpcVector = receiver.or(xPlainMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1PlainSecretMpcVector);
                    recvPlainSecretVector = receiver.revealOwn(z1PlainSecretMpcVector);
                    // (secret, plain)
                    z1SecretPlainMpcVector = receiver.or(x1SecretMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1SecretPlainMpcVector);
                    recvSecretPlainVector = receiver.revealOwn(z1SecretPlainMpcVector);
                    // (secret, secret)
                    z1SecretSecretMpcVector = receiver.or(x1SecretMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1SecretSecretMpcVector);
                    recvSecretSecretVector = receiver.revealOwn(z1SecretSecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
