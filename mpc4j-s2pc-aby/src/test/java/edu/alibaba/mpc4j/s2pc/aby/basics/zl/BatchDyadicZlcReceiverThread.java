package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;

/**
 * batch Zl circuit receiver thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class BatchDyadicZlcReceiverThread extends Thread {
    /**
     * receiver
     */
    private final ZlcParty receiver;
    /**
     * operator
     */
    private final DyadicAcOperator operator;
    /**
     * x vectors
     */
    private final ZlVector[] xVectors;
    /**
     * y vectors
     */
    private final ZlVector[] yVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * zs (plain, plain)
     */
    private ZlVector[] recvPlainPlainVectors;
    /**
     * zs (plain, secret)
     */
    private ZlVector[] recvPlainSecretVectors;
    /**
     * zs (secret, plain)
     */
    private ZlVector[] recvSecretPlainVectors;
    /**
     * zs (secret, secret)
     */
    private ZlVector[] recvSecretSecretVectors;

    BatchDyadicZlcReceiverThread(ZlcParty receiver, DyadicAcOperator operator, ZlVector[] xVectors, ZlVector[] yVectors) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVectors = xVectors;
        this.yVectors = yVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(ZlVector::getNum).sum();
    }

    ZlVector[] getRecvPlainPlainVectors() {
        return recvPlainPlainVectors;
    }

    ZlVector[] getRecvPlainSecretVectors() {
        return recvPlainSecretVectors;
    }

    ZlVector[] getRecvSecretPlainVectors() {
        return recvSecretPlainVectors;
    }

    ZlVector[] getRecvSecretSecretVectors() {
        return recvSecretSecretVectors;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalNum);
            // set inputs
            MpcZlVector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(receiver::create)
                .toArray(MpcZlVector[]::new);
            MpcZlVector[] yPlainMpcVectors = Arrays.stream(yVectors)
                .map(receiver::create)
                .toArray(MpcZlVector[]::new);
            int[] bitNums = Arrays.stream(xVectors).mapToInt(ZlVector::getNum).toArray();
            MpcZlVector[] x1SecretMpcVectors = receiver.shareOther(bitNums);
            MpcZlVector[] y1SecretMpcVectors = receiver.shareOwn(yVectors);
            MpcZlVector[] z1PlainPlainMpcVectors, z1PlainSecretMpcVectors;
            MpcZlVector[] z1SecretPlainMpcVectors, z1SecretSecretMpcVectors;
            switch (operator) {
                case ADD:
                    // (plain, plain)
                    z1PlainPlainMpcVectors = receiver.add(xPlainMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1PlainPlainMpcVectors);
                    recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
                    // (plain, secret)
                    z1PlainSecretMpcVectors = receiver.add(xPlainMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1PlainSecretMpcVectors);
                    recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
                    // (secret, plain)
                    z1SecretPlainMpcVectors = receiver.add(x1SecretMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1SecretPlainMpcVectors);
                    recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
                    // (secret, secret)
                    z1SecretSecretMpcVectors = receiver.add(x1SecretMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1SecretSecretMpcVectors);
                    recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);
                    break;
                case SUB:
                    // (plain, plain)
                    z1PlainPlainMpcVectors = receiver.sub(xPlainMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1PlainPlainMpcVectors);
                    recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
                    // (plain, secret)
                    z1PlainSecretMpcVectors = receiver.sub(xPlainMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1PlainSecretMpcVectors);
                    recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
                    // (secret, plain)
                    z1SecretPlainMpcVectors = receiver.sub(x1SecretMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1SecretPlainMpcVectors);
                    recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
                    // (secret, secret)
                    z1SecretSecretMpcVectors = receiver.sub(x1SecretMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1SecretSecretMpcVectors);
                    recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);
                    break;
                case MUL:
                    // (plain, plain)
                    z1PlainPlainMpcVectors = receiver.mul(xPlainMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1PlainPlainMpcVectors);
                    recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
                    // (plain, secret)
                    z1PlainSecretMpcVectors = receiver.mul(xPlainMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1PlainSecretMpcVectors);
                    recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
                    // (secret, plain)
                    z1SecretPlainMpcVectors = receiver.mul(x1SecretMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1SecretPlainMpcVectors);
                    recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
                    // (secret, secret)
                    z1SecretSecretMpcVectors = receiver.mul(x1SecretMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1SecretSecretMpcVectors);
                    recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
