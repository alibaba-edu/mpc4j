package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

/**
 * single Zl circuit sender thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class SingleDyadicZlcSenderThread extends Thread {
    /**
     * sender
     */
    private final ZlcParty sender;
    /**
     * operator
     */
    private final DyadicAcOperator operator;
    /**
     * x vector
     */
    private final ZlVector xVector;
    /**
     * y vector
     */
    private final ZlVector yVector;
    /**
     * z vector
     */
    private final ZlVector zVector;
    /**
     * num
     */
    private final int num;
    /**
     * z (plain, plain)
     */
    private ZlVector senPlainPlainVector;
    /**
     * z (plain, secret)
     */
    private ZlVector sendPlainSecretVector;
    /**
     * z (secret, plain)
     */
    private ZlVector sendSecretPlainVector;
    /**
     * z (secret, secret)
     */
    private ZlVector sendSecretSecretVector;

    SingleDyadicZlcSenderThread(ZlcParty sender, DyadicAcOperator operator, ZlVector xVector, ZlVector yVector) {
        this.sender = sender;
        this.operator = operator;
        this.xVector = xVector;
        this.yVector = yVector;
        num = xVector.getNum();
        switch (operator) {
            case ADD:
                zVector = xVector.add(yVector);
                break;
            case SUB:
                zVector = xVector.sub(yVector);
                break;
            case MUL:
                zVector = xVector.mul(yVector);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    ZlVector getExpectVector() {
        return zVector;
    }

    ZlVector getSenPlainPlainVector() {
        return senPlainPlainVector;
    }

    ZlVector getSendPlainSecretVector() {
        return sendPlainSecretVector;
    }

    ZlVector getSendSecretPlainVector() {
        return sendSecretPlainVector;
    }

    ZlVector getSendSecretSecretVector() {
        return sendSecretSecretVector;
    }

    @Override
    public void run() {
        try {
            sender.init(num);
            // generate x and y
            MpcZlVector xPlainMpcVector = sender.create(xVector);
            MpcZlVector yPlainMpcVector = sender.create(yVector);
            MpcZlVector x0SecretMpcVector = sender.shareOwn(xVector);
            MpcZlVector y0SecretMpcVector = sender.shareOther(num);
            MpcZlVector z0PlainPlainMpcVector, z0PlainSecretMpcVector;
            MpcZlVector z0SecretPlainMpcVector, z0SecretSecretMpcVector;
            switch (operator) {
                case ADD:
                    // (plain, plain)
                    z0PlainPlainMpcVector = sender.add(xPlainMpcVector, yPlainMpcVector);
                    senPlainPlainVector = sender.revealOwn(z0PlainPlainMpcVector);
                    sender.revealOther(z0PlainPlainMpcVector);
                    // (plain, secret)
                    z0PlainSecretMpcVector = sender.add(xPlainMpcVector, y0SecretMpcVector);
                    sendPlainSecretVector = sender.revealOwn(z0PlainSecretMpcVector);
                    sender.revealOther(z0PlainSecretMpcVector);
                    // (secret, plain)
                    z0SecretPlainMpcVector = sender.add(x0SecretMpcVector, yPlainMpcVector);
                    sendSecretPlainVector = sender.revealOwn(z0SecretPlainMpcVector);
                    sender.revealOther(z0SecretPlainMpcVector);
                    // (secret, secret)
                    z0SecretSecretMpcVector = sender.add(x0SecretMpcVector, y0SecretMpcVector);
                    sendSecretSecretVector = sender.revealOwn(z0SecretSecretMpcVector);
                    sender.revealOther(z0SecretSecretMpcVector);
                    break;
                case SUB:
                    // (plain, plain)
                    z0PlainPlainMpcVector = sender.sub(xPlainMpcVector, yPlainMpcVector);
                    senPlainPlainVector = sender.revealOwn(z0PlainPlainMpcVector);
                    sender.revealOther(z0PlainPlainMpcVector);
                    // (plain, secret)
                    z0PlainSecretMpcVector = sender.sub(xPlainMpcVector, y0SecretMpcVector);
                    sendPlainSecretVector = sender.revealOwn(z0PlainSecretMpcVector);
                    sender.revealOther(z0PlainSecretMpcVector);
                    // (secret, plain)
                    z0SecretPlainMpcVector = sender.sub(x0SecretMpcVector, yPlainMpcVector);
                    sendSecretPlainVector = sender.revealOwn(z0SecretPlainMpcVector);
                    sender.revealOther(z0SecretPlainMpcVector);
                    // (secret, secret)
                    z0SecretSecretMpcVector = sender.sub(x0SecretMpcVector, y0SecretMpcVector);
                    sendSecretSecretVector = sender.revealOwn(z0SecretSecretMpcVector);
                    sender.revealOther(z0SecretSecretMpcVector);
                    break;
                case MUL:
                    // (plain, plain)
                    z0PlainPlainMpcVector = sender.mul(xPlainMpcVector, yPlainMpcVector);
                    senPlainPlainVector = sender.revealOwn(z0PlainPlainMpcVector);
                    sender.revealOther(z0PlainPlainMpcVector);
                    // (plain, secret)
                    z0PlainSecretMpcVector = sender.mul(xPlainMpcVector, y0SecretMpcVector);
                    sendPlainSecretVector = sender.revealOwn(z0PlainSecretMpcVector);
                    sender.revealOther(z0PlainSecretMpcVector);
                    // (secret, plain)
                    z0SecretPlainMpcVector = sender.mul(x0SecretMpcVector, yPlainMpcVector);
                    sendSecretPlainVector = sender.revealOwn(z0SecretPlainMpcVector);
                    sender.revealOther(z0SecretPlainMpcVector);
                    // (secret, secret)
                    z0SecretSecretMpcVector = sender.mul(x0SecretMpcVector, y0SecretMpcVector);
                    sendSecretSecretVector = sender.revealOwn(z0SecretSecretMpcVector);
                    sender.revealOther(z0SecretSecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
