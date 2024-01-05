package edu.alibaba.mpc4j.s2pc.aby.basics.zl;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryAcOperator;
import edu.alibaba.mpc4j.common.circuit.zl.MpcZlVector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

/**
 * single Zl circuit receiver thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2023/5/11
 */
class SingleUnaryZlcReceiverThread extends Thread {
    /**
     * receiver
     */
    private final ZlcParty receiver;
    /**
     * operator
     */
    private final UnaryAcOperator operator;
    /**
     * x vector
     */
    private final ZlVector xVector;
    /**
     * num
     */
    private final int num;
    /**
     * z (plain)
     */
    private ZlVector recvPlainVector;
    /**
     * z (secret)
     */
    private ZlVector recvSecretVector;

    SingleUnaryZlcReceiverThread(ZlcParty receiver, UnaryAcOperator operator, ZlVector xVector) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVector = xVector;
        num = xVector.getNum();
    }

    ZlVector getRecvPlainVector() {
        return recvPlainVector;
    }

    ZlVector getRecvSecretVector() {
        return recvSecretVector;
    }

    @Override
    public void run() {
        try {
            receiver.init(num);
            // set inputs
            MpcZlVector xPlainMpcVector = receiver.create(xVector);
            MpcZlVector x1SecretMpcVector = receiver.shareOther(num);
            MpcZlVector z1PlainMpcVector, z1SecretMpcVector;
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
