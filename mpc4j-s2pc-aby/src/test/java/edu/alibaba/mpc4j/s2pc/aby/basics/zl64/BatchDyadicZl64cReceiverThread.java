package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;

/**
 * batch Zl circuit receiver thread for dyadic (binary) operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class BatchDyadicZl64cReceiverThread extends Thread {
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
     * x vectors
     */
    private final Zl64Vector[] xVectors;
    /**
     * y vectors
     */
    private final Zl64Vector[] yVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * zs (plain, plain)
     */
    private Zl64Vector[] recvPlainPlainVectors;
    /**
     * zs (plain, secret)
     */
    private Zl64Vector[] recvPlainSecretVectors;
    /**
     * zs (secret, plain)
     */
    private Zl64Vector[] recvSecretPlainVectors;
    /**
     * zs (secret, secret)
     */
    private Zl64Vector[] recvSecretSecretVectors;

    BatchDyadicZl64cReceiverThread(Zl64cParty receiver, Zl64 zl64, DyadicAcOperator operator, Zl64Vector[] xVectors, Zl64Vector[] yVectors) {
        this.receiver = receiver;
        this.zl64 = zl64;
        this.operator = operator;
        this.xVectors = xVectors;
        this.yVectors = yVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(Zl64Vector::getNum).sum();
    }

    Zl64Vector[] getRecvPlainPlainVectors() {
        return recvPlainPlainVectors;
    }

    Zl64Vector[] getRecvPlainSecretVectors() {
        return recvPlainSecretVectors;
    }

    Zl64Vector[] getRecvSecretPlainVectors() {
        return recvSecretPlainVectors;
    }

    Zl64Vector[] getRecvSecretSecretVectors() {
        return recvSecretSecretVectors;
    }

    @Override
    public void run() {
        try {
            receiver.init(zl64.getL(), totalNum);
            // set inputs
            MpcZl64Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(receiver::create)
                .toArray(MpcZl64Vector[]::new);
            MpcZl64Vector[] yPlainMpcVectors = Arrays.stream(yVectors)
                .map(receiver::create)
                .toArray(MpcZl64Vector[]::new);
            int[] bitNums = Arrays.stream(xVectors).mapToInt(Zl64Vector::getNum).toArray();
            MpcZl64Vector[] x1SecretMpcVectors = receiver.shareOther(zl64, bitNums);
            MpcZl64Vector[] y1SecretMpcVectors = receiver.shareOwn(yVectors);
            MpcZl64Vector[] z1PlainPlainMpcVectors, z1PlainSecretMpcVectors;
            MpcZl64Vector[] z1SecretPlainMpcVectors, z1SecretSecretMpcVectors;
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
