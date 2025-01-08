package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

import java.util.Arrays;

/**
 * batch Zl circuit sender thread for unary operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class BatchUnaryZl64cSenderThread extends Thread {
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
    private final UnaryAcOperator operator;
    /**
     * x vectors
     */
    private final Zl64Vector[] xVectors;
    /**
     * z vectors
     */
    private final Zl64Vector[] zVectors;
    /**
     * total num
     */
    private final int totalNum;
    /**
     * zs (plain)
     */
    private Zl64Vector[] sendPlainVectors;
    /**
     * zs (secret)
     */
    private Zl64Vector[] sendSecretVectors;

    BatchUnaryZl64cSenderThread(Zl64cParty sender, Zl64 zl64, UnaryAcOperator operator, Zl64Vector[] xVectors) {
        this.sender = sender;
        this.zl64 = zl64;
        this.operator = operator;
        this.xVectors = xVectors;
        totalNum = Arrays.stream(xVectors).mapToInt(Zl64Vector::getNum).sum();
        //noinspection SwitchStatementWithTooFewBranches
        switch (operator) {
            case NEG:
                zVectors = Arrays.stream(xVectors).map(Zl64Vector::neg).toArray(Zl64Vector[]::new);
                break;
            default:
                throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
        }
    }

    Zl64Vector[] getExpectVectors() {
        return zVectors;
    }

    Zl64Vector[] getSendPlainVectors() {
        return sendPlainVectors;
    }

    Zl64Vector[] getSendSecretVectors() {
        return sendSecretVectors;
    }

    @Override
    public void run() {
        try {
            sender.init(zl64.getL(), totalNum);
            // set inputs
            MpcZl64Vector[] xPlainMpcVectors = Arrays.stream(xVectors)
                .map(sender::create)
                .toArray(MpcZl64Vector[]::new);
            MpcZl64Vector[] x0SecretMpcVectors = sender.shareOwn(xVectors);
            MpcZl64Vector[] z0PlainMpcVectors, z0SecretMpcVectors;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NEG:
                    // (plain, plain)
                    z0PlainMpcVectors = sender.neg(xPlainMpcVectors);
                    sendSecretVectors = sender.revealOwn(z0PlainMpcVectors);
                    sender.revealOther(z0PlainMpcVectors);
                    // (plain, secret)
                    z0SecretMpcVectors = sender.neg(x0SecretMpcVectors);
                    sendPlainVectors = sender.revealOwn(z0SecretMpcVectors);
                    sender.revealOther(z0SecretMpcVectors);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
