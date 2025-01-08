package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * batch Zl circuit sender thread for dyadic (binary) operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class BatchDyadicZl64cSenderThread extends Thread {
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
     * x vectors
     */
    private final Zl64Vector[] xVectors;
    /**
     * y vectors
     */
    private final Zl64Vector[] yVectors;
    /**
     * z vectors
     */
    private final Zl64Vector[] zVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * zs (plain, plain)
     */
    private Zl64Vector[] sendPlainPlainVectors;
    /**
     * zs (plain, secret)
     */
    private Zl64Vector[] sendPlainSecretVectors;
    /**
     * zs (secret, plain)
     */
    private Zl64Vector[] sendSecretPlainVectors;
    /**
     * zs (secret, secret)
     */
    private Zl64Vector[] sendSecretSecretVectors;

    BatchDyadicZl64cSenderThread(Zl64cParty sender, Zl64 zl64, DyadicAcOperator operator, Zl64Vector[] xVectors, Zl64Vector[] yVectors) {
        this.sender = sender;
        this.zl64 = zl64;
        this.operator = operator;
        this.xVectors = xVectors;
        this.yVectors = yVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(Zl64Vector::getNum).sum();
        int vectorLength = xVectors.length;
        switch (operator) {
            case ADD:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].add(yVectors[index]))
                    .toArray(Zl64Vector[]::new);
                break;
            case SUB:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].sub(yVectors[index]))
                    .toArray(Zl64Vector[]::new);
                break;
            case MUL:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].mul(yVectors[index]))
                    .toArray(Zl64Vector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    Zl64Vector[] getExpectVectors() {
        return zVectors;
    }

    Zl64Vector[] getSendPlainPlainVectors() {
        return sendPlainPlainVectors;
    }

    Zl64Vector[] getSendPlainSecretVectors() {
        return sendPlainSecretVectors;
    }

    Zl64Vector[] getSendSecretPlainVectors() {
        return sendSecretPlainVectors;
    }

    Zl64Vector[] getSendSecretSecretVectors() {
        return sendSecretSecretVectors;
    }

    @Override
    public void run() {
        try {
            sender.init(zl64.getL(), totalNum);
            // set inputs
            MpcZl64Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(sender::create)
                .toArray(MpcZl64Vector[]::new);
            MpcZl64Vector[] yPlainMpcVectors = Arrays.stream(yVectors)
                .map(sender::create)
                .toArray(MpcZl64Vector[]::new);
            MpcZl64Vector[] x0SecretMpcVectors = sender.shareOwn(xVectors);
            int[] nums = Arrays.stream(yVectors).mapToInt(Zl64Vector::getNum).toArray();
            MpcZl64Vector[] y0SecretMpcVectors = sender.shareOther(zl64, nums);
            MpcZl64Vector[] z0PlainPlainMpcVectors, z0PlainSecretMpcVectors;
            MpcZl64Vector[] z0SecretPlainMpcVectors, z0SecretSecretMpcVectors;
            switch (operator) {
                case ADD:
                    // (plain, plain)
                    z0PlainPlainMpcVectors = sender.add(xPlainMpcVectors, yPlainMpcVectors);
                    sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
                    sender.revealOther(z0PlainPlainMpcVectors);
                    // (plain, secret)
                    z0PlainSecretMpcVectors = sender.add(xPlainMpcVectors, y0SecretMpcVectors);
                    sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
                    sender.revealOther(z0PlainSecretMpcVectors);
                    // (secret, plain)
                    z0SecretPlainMpcVectors = sender.add(x0SecretMpcVectors, yPlainMpcVectors);
                    sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
                    sender.revealOther(z0SecretPlainMpcVectors);
                    // (secret, secret)
                    z0SecretSecretMpcVectors = sender.add(x0SecretMpcVectors, y0SecretMpcVectors);
                    sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
                    sender.revealOther(z0SecretSecretMpcVectors);
                    break;
                case SUB:
                    // (plain, plain)
                    z0PlainPlainMpcVectors = sender.sub(xPlainMpcVectors, yPlainMpcVectors);
                    sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
                    sender.revealOther(z0PlainPlainMpcVectors);
                    // (plain, secret)
                    z0PlainSecretMpcVectors = sender.sub(xPlainMpcVectors, y0SecretMpcVectors);
                    sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
                    sender.revealOther(z0PlainSecretMpcVectors);
                    // (secret, plain)
                    z0SecretPlainMpcVectors = sender.sub(x0SecretMpcVectors, yPlainMpcVectors);
                    sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
                    sender.revealOther(z0SecretPlainMpcVectors);
                    // (secret, secret)
                    z0SecretSecretMpcVectors = sender.sub(x0SecretMpcVectors, y0SecretMpcVectors);
                    sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
                    sender.revealOther(z0SecretSecretMpcVectors);
                    break;
                case MUL:
                    // (plain, plain)
                    z0PlainPlainMpcVectors = sender.mul(xPlainMpcVectors, yPlainMpcVectors);
                    sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
                    sender.revealOther(z0PlainPlainMpcVectors);
                    // (plain, secret)
                    z0PlainSecretMpcVectors = sender.mul(xPlainMpcVectors, y0SecretMpcVectors);
                    sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
                    sender.revealOther(z0PlainSecretMpcVectors);
                    // (secret, plain)
                    z0SecretPlainMpcVectors = sender.mul(x0SecretMpcVectors, yPlainMpcVectors);
                    sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
                    sender.revealOther(z0SecretPlainMpcVectors);
                    // (secret, secret)
                    z0SecretSecretMpcVectors = sender.mul(x0SecretMpcVectors, y0SecretMpcVectors);
                    sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
                    sender.revealOther(z0SecretSecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
