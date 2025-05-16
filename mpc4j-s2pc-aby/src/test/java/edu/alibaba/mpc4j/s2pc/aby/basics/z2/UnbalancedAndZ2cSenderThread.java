package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

import java.util.Arrays;

/**
 * Boolean circuit receiver thread for unbalanced and operator.
 *
 * @author Feng Han
 * @date 2025/04/16
 */
class UnbalancedAndZ2cSenderThread extends Thread {
    /**
     * sender
     */
    private final Z2cParty sender;
    /**
     * y vectors
     */
    private final BitVector f;
    /**
     * x vectors
     */
    private final BitVector[] xVectors;
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

    UnbalancedAndZ2cSenderThread(Z2cParty sender, BitVector f, BitVector[] xVectors) {
        this.sender = sender;
        this.f = f;
        this.xVectors = xVectors;
        totalBitNum = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).sum();
        zVectors = Arrays.stream(xVectors).map(xVector -> xVector.and(f))
            .toArray(BitVector[]::new);
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
            MpcZ2Vector fPlainMpcVector = sender.create(true, f);
            MpcZ2Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(each -> sender.create(true, each))
                .toArray(MpcZ2Vector[]::new);

            MpcZ2Vector f0SecretMpcVectors = sender.shareOwn(f);
            int[] bitNums = Arrays.stream(xVectors).mapToInt(BitVector::bitNum).toArray();
            MpcZ2Vector[] x0SecretMpcVectors = sender.shareOther(bitNums);
            MpcZ2Vector[] z0PlainPlainMpcVectors, z0PlainSecretMpcVectors;
            MpcZ2Vector[] z0SecretPlainMpcVectors, z0SecretSecretMpcVectors;

            // (plain, plain)
            z0PlainPlainMpcVectors = sender.and(fPlainMpcVector, xPlainMpcVectors);
            sendPlainPlainVectors = sender.revealOwn(z0PlainPlainMpcVectors);
            sender.revealOther(z0PlainPlainMpcVectors);
            // (plain, secret)
            z0PlainSecretMpcVectors = sender.and(fPlainMpcVector, x0SecretMpcVectors);
            sendPlainSecretVectors = sender.revealOwn(z0PlainSecretMpcVectors);
            sender.revealOther(z0PlainSecretMpcVectors);
            // (secret, plain)
            z0SecretPlainMpcVectors = sender.and(f0SecretMpcVectors, xPlainMpcVectors);
            sendSecretPlainVectors = sender.revealOwn(z0SecretPlainMpcVectors);
            sender.revealOther(z0SecretPlainMpcVectors);
            // (secret, secret)
            z0SecretSecretMpcVectors = sender.and(f0SecretMpcVectors, x0SecretMpcVectors);
            sendSecretSecretVectors = sender.revealOwn(z0SecretSecretMpcVectors);
            sender.revealOther(z0SecretSecretMpcVectors);

        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
