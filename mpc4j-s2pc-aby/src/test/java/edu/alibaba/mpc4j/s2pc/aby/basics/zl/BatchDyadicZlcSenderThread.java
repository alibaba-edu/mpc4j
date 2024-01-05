package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.DyadicAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * batch Zl circuit sender thread for dyadic (binary) operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class BatchDyadicZlcSenderThread extends Thread {
    /**
     * sender
     */
    private final ZlcParty sender;
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
     * z vectors
     */
    private final ZlVector[] zVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * zs (plain, plain)
     */
    private ZlVector[] sendPlainPlainVectors;
    /**
     * zs (plain, secret)
     */
    private ZlVector[] sendPlainSecretVectors;
    /**
     * zs (secret, plain)
     */
    private ZlVector[] sendSecretPlainVectors;
    /**
     * zs (secret, secret)
     */
    private ZlVector[] sendSecretSecretVectors;

    BatchDyadicZlcSenderThread(ZlcParty sender, DyadicAcOperator operator, ZlVector[] xVectors, ZlVector[] yVectors) {
        this.sender = sender;
        this.operator = operator;
        this.xVectors = xVectors;
        this.yVectors = yVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(ZlVector::getNum).sum();
        int vectorLength = xVectors.length;
        switch (operator) {
            case ADD:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].add(yVectors[index]))
                    .toArray(ZlVector[]::new);
                break;
            case SUB:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].sub(yVectors[index]))
                    .toArray(ZlVector[]::new);
                break;
            case MUL:
                zVectors = IntStream.range(0, vectorLength)
                    .mapToObj(index -> xVectors[index].mul(yVectors[index]))
                    .toArray(ZlVector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid " + DyadicAcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    ZlVector[] getExpectVectors() {
        return zVectors;
    }

    ZlVector[] getSendPlainPlainVectors() {
        return sendPlainPlainVectors;
    }

    ZlVector[] getSendPlainSecretVectors() {
        return sendPlainSecretVectors;
    }

    ZlVector[] getSendSecretPlainVectors() {
        return sendSecretPlainVectors;
    }

    ZlVector[] getSendSecretSecretVectors() {
        return sendSecretSecretVectors;
    }

    @Override
    public void run() {
        try {
            sender.init(totalNum);
            // set inputs
            MpcZlVector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(sender::create)
                .toArray(MpcZlVector[]::new);
            MpcZlVector[] yPlainMpcVectors = Arrays.stream(yVectors)
                .map(sender::create)
                .toArray(MpcZlVector[]::new);
            MpcZlVector[] x0SecretMpcVectors = sender.shareOwn(xVectors);
            int[] nums = Arrays.stream(yVectors).mapToInt(ZlVector::getNum).toArray();
            MpcZlVector[] y0SecretMpcVectors = sender.shareOther(nums);
            MpcZlVector[] z0PlainPlainMpcVectors, z0PlainSecretMpcVectors;
            MpcZlVector[] z0SecretPlainMpcVectors, z0SecretSecretMpcVectors;
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
