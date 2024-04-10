package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * single Boolean circuit sender thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleDyadicZ2cSenderThread extends Thread {
    /**
     * sender
     */
    private final Z2cParty sender;
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
     * z vector
     */
    private final BitVector zVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * z (plain, plain)
     */
    private BitVector senPlainPlainVector;
    /**
     * z (plain, secret)
     */
    private BitVector sendPlainSecretVector;
    /**
     * z (secret, plain)
     */
    private BitVector sendSecretPlainVector;
    /**
     * z (secret, secret)
     */
    private BitVector sendSecretSecretVector;

    SingleDyadicZ2cSenderThread(Z2cParty sender, DyadicBcOperator operator, BitVector xVector, BitVector yVector) {
        this.sender = sender;
        this.operator = operator;
        this.xVector = xVector;
        this.yVector = yVector;
        bitNum = xVector.bitNum();
        switch (operator) {
            case XOR:
                zVector = xVector.xor(yVector);
                break;
            case AND:
                zVector = xVector.and(yVector);
                break;
            case OR:
                zVector = xVector.or(yVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicBcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    BitVector getExpectVector() {
        return zVector;
    }

    BitVector getSenPlainPlainVector() {
        return senPlainPlainVector;
    }

    BitVector getSendPlainSecretVector() {
        return sendPlainSecretVector;
    }

    BitVector getSendSecretPlainVector() {
        return sendSecretPlainVector;
    }

    BitVector getSendSecretSecretVector() {
        return sendSecretSecretVector;
    }

    @Override
    public void run() {
        try {
            sender.init(bitNum);
            // generate x and y
            MpcZ2Vector xPlainMpcVector = sender.create(true, xVector);
            MpcZ2Vector yPlainMpcVector = sender.create(true, yVector);
            MpcZ2Vector x0SecretMpcVector = sender.shareOwn(xVector);
            MpcZ2Vector y0SecretMpcVector = sender.shareOther(bitNum);
            MpcZ2Vector z0PlainPlainMpcVector, z0PlainSecretMpcVector;
            MpcZ2Vector z0SecretPlainMpcVector, z0SecretSecretMpcVector;
            switch (operator) {
                case XOR:
                    // (plain, plain)
                    z0PlainPlainMpcVector = sender.xor(xPlainMpcVector, yPlainMpcVector);
                    senPlainPlainVector = sender.revealOwn(z0PlainPlainMpcVector);
                    sender.revealOther(z0PlainPlainMpcVector);
                    // (plain, secret)
                    z0PlainSecretMpcVector = sender.xor(xPlainMpcVector, y0SecretMpcVector);
                    sendPlainSecretVector = sender.revealOwn(z0PlainSecretMpcVector);
                    sender.revealOther(z0PlainSecretMpcVector);
                    // (secret, plain)
                    z0SecretPlainMpcVector = sender.xor(x0SecretMpcVector, yPlainMpcVector);
                    sendSecretPlainVector = sender.revealOwn(z0SecretPlainMpcVector);
                    sender.revealOther(z0SecretPlainMpcVector);
                    // (secret, secret)
                    z0SecretSecretMpcVector = sender.xor(x0SecretMpcVector, y0SecretMpcVector);
                    sendSecretSecretVector = sender.revealOwn(z0SecretSecretMpcVector);
                    sender.revealOther(z0SecretSecretMpcVector);
                    break;
                case AND:
                    // (plain, plain)
                    z0PlainPlainMpcVector = sender.and(xPlainMpcVector, yPlainMpcVector);
                    senPlainPlainVector = sender.revealOwn(z0PlainPlainMpcVector);
                    sender.revealOther(z0PlainPlainMpcVector);
                    // (plain, secret)
                    z0PlainSecretMpcVector = sender.and(xPlainMpcVector, y0SecretMpcVector);
                    sendPlainSecretVector = sender.revealOwn(z0PlainSecretMpcVector);
                    sender.revealOther(z0PlainSecretMpcVector);
                    // (secret, plain)
                    z0SecretPlainMpcVector = sender.and(x0SecretMpcVector, yPlainMpcVector);
                    sendSecretPlainVector = sender.revealOwn(z0SecretPlainMpcVector);
                    sender.revealOther(z0SecretPlainMpcVector);
                    // (secret, secret)
                    z0SecretSecretMpcVector = sender.and(x0SecretMpcVector, y0SecretMpcVector);
                    sendSecretSecretVector = sender.revealOwn(z0SecretSecretMpcVector);
                    sender.revealOther(z0SecretSecretMpcVector);
                    break;
                case OR:
                    // (plain, plain)
                    z0PlainPlainMpcVector = sender.or(xPlainMpcVector, yPlainMpcVector);
                    senPlainPlainVector = sender.revealOwn(z0PlainPlainMpcVector);
                    sender.revealOther(z0PlainPlainMpcVector);
                    // (plain, secret)
                    z0PlainSecretMpcVector = sender.or(xPlainMpcVector, y0SecretMpcVector);
                    sendPlainSecretVector = sender.revealOwn(z0PlainSecretMpcVector);
                    sender.revealOther(z0PlainSecretMpcVector);
                    // (secret, plain)
                    z0SecretPlainMpcVector = sender.or(x0SecretMpcVector, yPlainMpcVector);
                    sendSecretPlainVector = sender.revealOwn(z0SecretPlainMpcVector);
                    sender.revealOther(z0SecretPlainMpcVector);
                    // (secret, secret)
                    z0SecretSecretMpcVector = sender.or(x0SecretMpcVector, y0SecretMpcVector);
                    sendSecretSecretVector = sender.revealOwn(z0SecretSecretMpcVector);
                    sender.revealOther(z0SecretSecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
