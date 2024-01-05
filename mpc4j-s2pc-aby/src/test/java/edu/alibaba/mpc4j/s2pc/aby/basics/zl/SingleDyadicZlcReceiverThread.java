package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

/**
 * single Zl circuit receiver thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class SingleDyadicZlcReceiverThread extends Thread {
    /**
     * receiver
     */
    private final ZlcParty receiver;
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
     * num
     */
    private final int num;
    /**
     * z (plain, plain)
     */
    private ZlVector recvPlainPlainVector;
    /**
     * z (plain, secret)
     */
    private ZlVector recvPlainSecretVector;
    /**
     * z (secret, plain)
     */
    private ZlVector recvSecretPlainVector;
    /**
     * z (secret, secret)
     */
    private ZlVector recvSecretSecretVector;

    SingleDyadicZlcReceiverThread(ZlcParty receiver, DyadicAcOperator operator, ZlVector xVector, ZlVector yVector) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVector = xVector;
        this.yVector = yVector;
        num = xVector.getNum();
    }

    ZlVector getRecvPlainPlainVector() {
        return recvPlainPlainVector;
    }

    ZlVector getRecvPlainSecretVector() {
        return recvPlainSecretVector;
    }

    ZlVector getRecvSecretPlainVector() {
        return recvSecretPlainVector;
    }

    ZlVector getRecvSecretSecretVector() {
        return recvSecretSecretVector;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            // generate x and y
            MpcZlVector xPlainMpcVector = receiver.create(xVector);
            MpcZlVector yPlainMpcVector = receiver.create(yVector);
            MpcZlVector x1SecretMpcVector = receiver.shareOther(num);
            MpcZlVector y1SecretMpcVector = receiver.shareOwn(yVector);
            MpcZlVector z1PlainPlainMpcVector, z1PlainSecretMpcVector;
            MpcZlVector z1SecretPlainMpcVector, z1SecretSecretMpcVector;
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
