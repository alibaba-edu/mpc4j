package edu.alibaba.mpc4j.s2pc.aby.basics.zl64;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl64.MpcZl64Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl64.Zl64;

/**
 * single Zl circuit receiver thread for unary operator.
 *
 * @author Li Peng
 * @date 2024/7/25
 */
class SingleUnaryZl64cReceiverThread extends Thread {
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
    private final UnaryAcOperator operator;
    /**
     * x vector
     */
    private final Zl64Vector xVector;
    /**
     * num
     */
    private final int num;
    /**
     * z (plain)
     */
    private Zl64Vector recvPlainVector;
    /**
     * z (secret)
     */
    private Zl64Vector recvSecretVector;

    SingleUnaryZl64cReceiverThread(Zl64cParty receiver, Zl64 zl64, UnaryAcOperator operator, Zl64Vector xVector) {
        this.receiver = receiver;
        this.zl64 = zl64;
        this.operator = operator;
        this.xVector = xVector;
        num = xVector.getNum();
    }

    Zl64Vector getRecvPlainVector() {
        return recvPlainVector;
    }

    Zl64Vector getRecvSecretVector() {
        return recvSecretVector;
    }

    @Override
    public void run() {
        try {
            receiver.init(zl64.getL(), num);
            // set inputs
            MpcZl64Vector xPlainMpcVector = receiver.create(xVector);
            MpcZl64Vector x1SecretMpcVector = receiver.shareOther(zl64, num);
            MpcZl64Vector z1PlainMpcVector, z1SecretMpcVector;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NEG:
                    // (plain, plain)
                    z1PlainMpcVector = receiver.neg(xPlainMpcVector);
                    receiver.revealOther(z1PlainMpcVector);
                    recvSecretVector = receiver.revealOwn(z1PlainMpcVector);
                    // (plain, secret)
                    z1SecretMpcVector = receiver.neg(x1SecretMpcVector);
                    receiver.revealOther(z1SecretMpcVector);
                    recvPlainVector = receiver.revealOwn(z1SecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryAcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
