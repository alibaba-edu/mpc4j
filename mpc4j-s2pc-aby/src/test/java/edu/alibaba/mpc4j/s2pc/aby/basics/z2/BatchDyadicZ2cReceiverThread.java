package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;

/**
 * batch Boolean circuit receiver thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchDyadicZ2cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Z2cParty receiver;
    /**
     * operator
     */
    private final DyadicBcOperator operator;
    /**
     * x vectors
     */
    private final BitVector[] xVectors;
    /**
     * y vectors
     */
    private final BitVector[] yVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * zs (plain, plain)
     */
    private BitVector[] recvPlainPlainVectors;
    /**
     * zs (plain, secret)
     */
    private BitVector[] recvPlainSecretVectors;
    /**
     * zs (secret, plain)
     */
    private BitVector[] recvSecretPlainVectors;
    /**
     * zs (secret, secret)
     */
    private BitVector[] recvSecretSecretVectors;

    BatchDyadicZ2cReceiverThread(Z2cParty receiver, DyadicBcOperator operator, BitVector[] xVectors, BitVector[] yVectors) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVectors = xVectors;
        this.yVectors = yVectors;
        totalBitNum = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).sum();
    }

    BitVector[] getRecvPlainPlainVectors() {
        return recvPlainPlainVectors;
    }

    BitVector[] getRecvPlainSecretVectors() {
        return recvPlainSecretVectors;
    }

    BitVector[] getRecvSecretPlainVectors() {
        return recvSecretPlainVectors;
    }

    BitVector[] getRecvSecretSecretVectors() {
        return recvSecretSecretVectors;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalBitNum);
            // set inputs
            MpcZ2Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(each -> receiver.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] yPlainMpcVectors = Arrays.stream(yVectors)
                .map(each -> receiver.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            int[] bitNums = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).toArray();
            MpcZ2Vector[] x1SecretMpcVectors = receiver.shareOther(bitNums);
            MpcZ2Vector[] y1SecretMpcVectors = receiver.shareOwn(yVectors);
            MpcZ2Vector[] z1PlainPlainMpcVectors, z1PlainSecretMpcVectors;
            MpcZ2Vector[] z1SecretPlainMpcVectors, z1SecretSecretMpcVectors;
            switch (operator) {
                case XOR:
                    // (plain, plain)
                    z1PlainPlainMpcVectors = receiver.xor(xPlainMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1PlainPlainMpcVectors);
                    recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
                    // (plain, secret)
                    z1PlainSecretMpcVectors = receiver.xor(xPlainMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1PlainSecretMpcVectors);
                    recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
                    // (secret, plain)
                    z1SecretPlainMpcVectors = receiver.xor(x1SecretMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1SecretPlainMpcVectors);
                    recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
                    // (secret, secret)
                    z1SecretSecretMpcVectors = receiver.xor(x1SecretMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1SecretSecretMpcVectors);
                    recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);
                    break;
                case AND:
                    // (plain, plain)
                    z1PlainPlainMpcVectors = receiver.and(xPlainMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1PlainPlainMpcVectors);
                    recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
                    // (plain, secret)
                    z1PlainSecretMpcVectors = receiver.and(xPlainMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1PlainSecretMpcVectors);
                    recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
                    // (secret, plain)
                    z1SecretPlainMpcVectors = receiver.and(x1SecretMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1SecretPlainMpcVectors);
                    recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
                    // (secret, secret)
                    z1SecretSecretMpcVectors = receiver.and(x1SecretMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1SecretSecretMpcVectors);
                    recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);
                    break;
                case OR:
                    // (plain, plain)
                    z1PlainPlainMpcVectors = receiver.or(xPlainMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1PlainPlainMpcVectors);
                    recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
                    // (plain, secret)
                    z1PlainSecretMpcVectors = receiver.or(xPlainMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1PlainSecretMpcVectors);
                    recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
                    // (secret, plain)
                    z1SecretPlainMpcVectors = receiver.or(x1SecretMpcVectors, yPlainMpcVectors);
                    receiver.revealOther(z1SecretPlainMpcVectors);
                    recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
                    // (secret, secret)
                    z1SecretSecretMpcVectors = receiver.or(x1SecretMpcVectors, y1SecretMpcVectors);
                    receiver.revealOther(z1SecretSecretMpcVectors);
                    recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
