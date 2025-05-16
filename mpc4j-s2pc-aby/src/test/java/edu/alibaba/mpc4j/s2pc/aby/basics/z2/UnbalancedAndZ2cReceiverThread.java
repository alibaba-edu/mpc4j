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
class UnbalancedAndZ2cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Z2cParty receiver;
    /**
     * x vectors
     */
    private final BitVector f;
    /**
     * x vectors
     */
    private final BitVector[] xVectors;
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

    UnbalancedAndZ2cReceiverThread(Z2cParty receiver, BitVector f, BitVector[] xVectors) {
        this.receiver = receiver;
        this.f = f;
        this.xVectors = xVectors;
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
            MpcZ2Vector fPlainMpcVector = receiver.create(true, f);
            MpcZ2Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(each -> receiver.create(true, each))
                .toArray(MpcZ2Vector[]::new);
            MpcZ2Vector f1SecretMpcVector = receiver.shareOther(new int[]{f.bitNum()})[0];
            MpcZ2Vector[] x1SecretMpcVectors = receiver.shareOwn(xVectors);
            MpcZ2Vector[] z1PlainPlainMpcVectors, z1PlainSecretMpcVectors;
            MpcZ2Vector[] z1SecretPlainMpcVectors, z1SecretSecretMpcVectors;

            // (plain, plain)
            z1PlainPlainMpcVectors = receiver.and(fPlainMpcVector, xPlainMpcVectors);
            receiver.revealOther(z1PlainPlainMpcVectors);
            recvPlainPlainVectors = receiver.revealOwn(z1PlainPlainMpcVectors);
            // (plain, secret)
            z1PlainSecretMpcVectors = receiver.and(fPlainMpcVector, x1SecretMpcVectors);
            receiver.revealOther(z1PlainSecretMpcVectors);
            recvPlainSecretVectors = receiver.revealOwn(z1PlainSecretMpcVectors);
            // (secret, plain)
            z1SecretPlainMpcVectors = receiver.and(f1SecretMpcVector, xPlainMpcVectors);
            receiver.revealOther(z1SecretPlainMpcVectors);
            recvSecretPlainVectors = receiver.revealOwn(z1SecretPlainMpcVectors);
            // (secret, secret)
            z1SecretSecretMpcVectors = receiver.and(f1SecretMpcVector, x1SecretMpcVectors);
            receiver.revealOther(z1SecretSecretMpcVectors);
            recvSecretSecretVectors = receiver.revealOwn(z1SecretSecretMpcVectors);

        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
