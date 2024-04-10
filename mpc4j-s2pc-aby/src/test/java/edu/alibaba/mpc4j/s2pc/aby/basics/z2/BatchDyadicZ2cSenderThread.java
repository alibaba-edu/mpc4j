package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * batch Boolean circuit sender thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2022/12/27
 */
class BatchDyadicZ2cSenderThread extends Thread {
    /**
     * sender
     */
    private final Z2cParty sender;
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
     * z vectors
     */
    private final BitVector[] zVectors;
    /**
     * total number of bits
     */
    private final int totalBitNum;
    /**
     * zs (plain, plain)
     */
    private BitVector[] sendPlainPlainVectors;
    /**
     * zs (plain, secret)
     */
    private BitVector[] sendPlainSecretVectors;
    /**
     * zs (secret, plain)
     */
    private BitVector[] sendSecretPlainVectors;
    /**
     * zs (secret, secret)
     */
    private BitVector[] sendSecretSecretVectors;

    BatchDyadicZ2cSenderThread(Z2cParty sender, DyadicBcOperator operator, BitVector[] xVectors, BitVector[] yVectors) {
        this.sender = sender;
        this.operator = operator;
        this.xVectors = xVectors;
        this.yVectors = yVectors;
        totalBitNum = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).sum();
        int vectorLength = xVectors.length;
        switch (operator) {
            case XOR:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].xor(yVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            case AND:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].and(yVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            case OR:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].or(yVectors[index]))
                    .toArray(BitVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicBcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    BitVector[] getExpectVectors() {
        return zVectors;
    }

    BitVector[] getSendPlainPlainVectors() {
        return sendPlainPlainVectors;
    }

    BitVector[] getSendPlainSecretVectors() {
        return sendPlainSecretVectors;
    }

    BitVector[] getSendSecretPlainVectors() {
        return sendSecretPlainVectors;
    }

    BitVector[] getSendSecretSecretVectors() {
        return sendSecretSecretVectors;
    }

    @Override
    public void run() {
        try {
            sender.init(totalBitNum);
            // set inputs
            MpcZ2Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(each -> sender.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] yPlainMpcVectors = Arrays.stream(yVectors)
                .map(each -> sender.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            MpcZ2Vector[] x0SecretMpcVectors = sender.shareOwn(xVectors);
            int[] bitNums = Arrays.stream(yVectors).mapToInt(BitVector::bitNum).toArray();
            MpcZ2Vector[] y0SecretMpcVectors = sender.shareOther(bitNums);
            MpcZ2Vector[] z0PlainPlainMpcVectors, z0PlainSecretMpcVectors;
            MpcZ2Vector[] z0SecretPlainMpcVectors, z0SecretSecretMpcVectors;
            switch (operator) {
                case XOR:
                    // (plain, plain)
                    z0PlainPlainMpcVectors = sender.xor(xPlainMpcVectors, yPlainMpcVectors);
                    sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
                    sender.revealOther(z0PlainPlainMpcVectors);
                    // (plain, secret)
                    z0PlainSecretMpcVectors = sender.xor(xPlainMpcVectors, y0SecretMpcVectors);
                    sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
                    sender.revealOther(z0PlainSecretMpcVectors);
                    // (secret, plain)
                    z0SecretPlainMpcVectors = sender.xor(x0SecretMpcVectors, yPlainMpcVectors);
                    sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
                    sender.revealOther(z0SecretPlainMpcVectors);
                    // (secret, secret)
                    z0SecretSecretMpcVectors = sender.xor(x0SecretMpcVectors, y0SecretMpcVectors);
                    sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
                    sender.revealOther(z0SecretSecretMpcVectors);
                    break;
                case AND:
                    // (plain, plain)
                    z0PlainPlainMpcVectors = sender.and(xPlainMpcVectors, yPlainMpcVectors);
                    sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
                    sender.revealOther(z0PlainPlainMpcVectors);
                    // (plain, secret)
                    z0PlainSecretMpcVectors = sender.and(xPlainMpcVectors, y0SecretMpcVectors);
                    sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
                    sender.revealOther(z0PlainSecretMpcVectors);
                    // (secret, plain)
                    z0SecretPlainMpcVectors = sender.and(x0SecretMpcVectors, yPlainMpcVectors);
                    sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
                    sender.revealOther(z0SecretPlainMpcVectors);
                    // (secret, secret)
                    z0SecretSecretMpcVectors = sender.and(x0SecretMpcVectors, y0SecretMpcVectors);
                    sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
                    sender.revealOther(z0SecretSecretMpcVectors);
                    break;
                case OR:
                    // (plain, plain)
                    z0PlainPlainMpcVectors = sender.or(xPlainMpcVectors, yPlainMpcVectors);
                    sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
                    sender.revealOther(z0PlainPlainMpcVectors);
                    // (plain, secret)
                    z0PlainSecretMpcVectors = sender.or(xPlainMpcVectors, y0SecretMpcVectors);
                    sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
                    sender.revealOther(z0PlainSecretMpcVectors);
                    // (secret, plain)
                    z0SecretPlainMpcVectors = sender.or(x0SecretMpcVectors, yPlainMpcVectors);
                    sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
                    sender.revealOther(z0SecretPlainMpcVectors);
                    // (secret, secret)
                    z0SecretSecretMpcVectors = sender.or(x0SecretMpcVectors, y0SecretMpcVectors);
                    sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
                    sender.revealOther(z0SecretSecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
