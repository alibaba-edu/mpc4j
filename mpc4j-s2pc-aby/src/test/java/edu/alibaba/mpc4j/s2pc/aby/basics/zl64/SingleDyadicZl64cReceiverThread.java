package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * single Zl circuit receiver thread for dyadic (binary) operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class SingleDyadicZl64cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Zl64cParty receiver;
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
     * num
     */
    private final int num;
    /**
     * z (plain, plain)
     */
    private Zl64Vector recvPlainPlainVector;
    /**
     * z (plain, secret)
     */
    private Zl64Vector recvPlainSecretVector;
    /**
     * z (secret, plain)
     */
    private Zl64Vector recvSecretPlainVector;
    /**
     * z (secret, secret)
     */
    private Zl64Vector recvSecretSecretVector;

    SingleDyadicZl64cReceiverThread(Zl64cParty receiver, Zl64 zl64, DyadicAcOperator operator, Zl64Vector xVector, Zl64Vector yVector) {
        this.receiver = receiver;
        this.zl64 = zl64;
        this.operator = operator;
        this.xVector = xVector;
        this.yVector = yVector;
        num = xVector.getNum();
    }

    Zl64Vector getRecvPlainPlainVector() {
        return recvPlainPlainVector;
    }

    Zl64Vector getRecvPlainSecretVector() {
        return recvPlainSecretVector;
    }

    Zl64Vector getRecvSecretPlainVector() {
        return recvSecretPlainVector;
    }

    Zl64Vector getRecvSecretSecretVector() {
        return recvSecretSecretVector;
    }

    @Override
    public void run() {
        try {
            receiver.init(zl64.getL(), num);
            // generate x and y
            MpcZl64Vector xPlainMpcVector = receiver.create(xVector);
            MpcZl64Vector yPlainMpcVector = receiver.create(yVector);
            MpcZl64Vector x1SecretMpcVector = receiver.shareOther(zl64, num);
            MpcZl64Vector y1SecretMpcVector = receiver.shareOwn(yVector);
            MpcZl64Vector z1PlainPlainMpcVector, z1PlainSecretMpcVector;
            MpcZl64Vector z1SecretPlainMpcVector, z1SecretSecretMpcVector;
            switch (operator) {
                case ADD:
                    // (plain, plain)
                    z1PlainPlainMpcVector = receiver.add(xPlainMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1PlainPlainMpcVector);
                    recvPlainPlainVector = receiver.revealOwn(z1PlainPlainMpcVector);
                    // (plain, secret)
                    z1PlainSecretMpcVector = receiver.add(xPlainMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1PlainSecretMpcVector);
                    recvPlainSecretVector = receiver.revealOwn(z1PlainSecretMpcVector);
                    // (secret, plain)
                    z1SecretPlainMpcVector = receiver.add(x1SecretMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1SecretPlainMpcVector);
                    recvSecretPlainVector = receiver.revealOwn(z1SecretPlainMpcVector);
                    // (secret, secret)
                    z1SecretSecretMpcVector = receiver.add(x1SecretMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1SecretSecretMpcVector);
                    recvSecretSecretVector = receiver.revealOwn(z1SecretSecretMpcVector);
                    break;
                case SUB:
                    // (plain, plain)
                    z1PlainPlainMpcVector = receiver.sub(xPlainMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1PlainPlainMpcVector);
                    recvPlainPlainVector = receiver.revealOwn(z1PlainPlainMpcVector);
                    // (plain, secret)
                    z1PlainSecretMpcVector = receiver.sub(xPlainMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1PlainSecretMpcVector);
                    recvPlainSecretVector = receiver.revealOwn(z1PlainSecretMpcVector);
                    // (secret, plain)
                    z1SecretPlainMpcVector = receiver.sub(x1SecretMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1SecretPlainMpcVector);
                    recvSecretPlainVector = receiver.revealOwn(z1SecretPlainMpcVector);
                    // (secret, secret)
                    z1SecretSecretMpcVector = receiver.sub(x1SecretMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1SecretSecretMpcVector);
                    recvSecretSecretVector = receiver.revealOwn(z1SecretSecretMpcVector);
                    break;
                case MUL:
                    // (plain, plain)
                    z1PlainPlainMpcVector = receiver.mul(xPlainMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1PlainPlainMpcVector);
                    recvPlainPlainVector = receiver.revealOwn(z1PlainPlainMpcVector);
                    // (plain, secret)
                    z1PlainSecretMpcVector = receiver.mul(xPlainMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1PlainSecretMpcVector);
                    recvPlainSecretVector = receiver.revealOwn(z1PlainSecretMpcVector);
                    // (secret, plain)
                    z1SecretPlainMpcVector = receiver.mul(x1SecretMpcVector, yPlainMpcVector);
                    receiver.revealOther(z1SecretPlainMpcVector);
                    recvSecretPlainVector = receiver.revealOwn(z1SecretPlainMpcVector);
                    // (secret, secret)
                    z1SecretSecretMpcVector = receiver.mul(x1SecretMpcVector, y1SecretMpcVector);
                    receiver.revealOther(z1SecretSecretMpcVector);
                    recvSecretSecretVector = receiver.revealOwn(z1SecretSecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
