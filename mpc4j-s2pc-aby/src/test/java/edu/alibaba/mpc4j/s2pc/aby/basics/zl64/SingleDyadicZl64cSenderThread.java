package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * single Zl circuit sender thread for dyadic (binary) operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class SingleDyadicZl64cSenderThread extends Thread {
    /**
     * sender
     */
    private final Zl64cParty sender;
    /**
     * Zl
     */
    private final Zl64 zl64;
    /**
     * operator
     */
    private final DyadicAcOperator operator;
    /**
     * x vector
     */
    private final Zl64Vector xVector;
    /**
     * y vector
     */
    private final Zl64Vector yVector;
    /**
     * z vector
     */
    private final Zl64Vector zVector;
    /**
     * num
     */
    private final int num;
    /**
     * z (plain, plain)
     */
    private Zl64Vector senPlainPlainVector;
    /**
     * z (plain, secret)
     */
    private Zl64Vector sendPlainSecretVector;
    /**
     * z (secret, plain)
     */
    private Zl64Vector sendSecretPlainVector;
    /**
     * z (secret, secret)
     */
    private Zl64Vector sendSecretSecretVector;

    SingleDyadicZl64cSenderThread(Zl64cParty sender, Zl64 zl64, DyadicAcOperator operator, Zl64Vector xVector, Zl64Vector yVector) {
        this.sender = sender;
        this.zl64 = zl64;
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

    Zl64Vector getExpectVector() {
        return zVector;
    }

    Zl64Vector getSenPlainPlainVector() {
        return senPlainPlainVector;
    }

    Zl64Vector getSendPlainSecretVector() {
        return sendPlainSecretVector;
    }

    Zl64Vector getSendSecretPlainVector() {
        return sendSecretPlainVector;
    }

    Zl64Vector getSendSecretSecretVector() {
        return sendSecretSecretVector;
    }

    @Override
    public void run() {
        try {
            sender.init(zl64.getL(), num);
            // generate x and y
            MpcZl64Vector xPlainMpcVector = sender.create(xVector);
            MpcZl64Vector yPlainMpcVector = sender.create(yVector);
            MpcZl64Vector x0SecretMpcVector = sender.shareOwn(xVector);
            MpcZl64Vector y0SecretMpcVector = sender.shareOther(zl64, num);
            MpcZl64Vector z0PlainPlainMpcVector, z0PlainSecretMpcVector;
            MpcZl64Vector z0SecretPlainMpcVector, z0SecretSecretMpcVector;
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
