package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.util.Arrays;

/**
 * batch Zl circuit receiver thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class BatchUnaryZlcReceiverThread extends Thread {
    /**
     * receiver
     */
    private final ZlcParty receiver;
    /**
     * operator
     */
    private final UnaryAcOperator operator;
    /**
     * x vectors
     */
    private final ZlVector[] xVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * z (plain)
     */
    private ZlVector[] recvPlainVectors;
    /**
     * z (secret)
     */
    private ZlVector[] recvSecretVectors;

    BatchUnaryZlcReceiverThread(ZlcParty receiver, UnaryAcOperator operator, ZlVector[] xVectors) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVectors = xVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(ZlVector::getNum).sum();
    }

    ZlVector[] getRecvPlainVectors() {
        return recvPlainVectors;
    }

    ZlVector[] getRecvSecretVectors() {
        return recvSecretVectors;
    }

    @Override
    public void run() {
        try {
            receiver.init(totalNum);
            // set inputs
            MpcZlVector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(receiver::create)
                .toArray(MpcZlVector[]::new);
            int[] nums = Arrays.stream(xVectors).mapToInt(ZlVector::getNum).toArray();
            MpcZlVector[] x1SecretMpcVectors = receiver.shareOther(nums);
            MpcZlVector[] z1PlainMpcVectors, z1SecretMpcVectors;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NEG:
                    // (plain, plain)
                    z1PlainMpcVectors = receiver.neg(xPlainMpcVectors);
                    receiver.revealOther(z1PlainMpcVectors);
                    recvSecretVectors = receiver.revealOwn(z1PlainMpcVectors);
                    // (plain, secret)
                    z1SecretMpcVectors = receiver.neg(x1SecretMpcVectors);
                    receiver.revealOther(z1SecretMpcVectors);
                    recvPlainVectors = receiver.revealOwn(z1SecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
