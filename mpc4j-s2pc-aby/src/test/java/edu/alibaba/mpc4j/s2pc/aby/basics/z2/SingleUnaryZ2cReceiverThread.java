package edu.alibaba.mpc4j.s2pc.aby.basics.z2;

import edu.alibaba.mpc4j.common.circuit.operator.UnaryBcOperator;
import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;

/**
 * single Boolean circuit receiver thread for unary operator.
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
class SingleUnaryZ2cReceiverThread extends Thread {
    /**
     * receiver
     */
    private final Z2cParty receiver;
    /**
     * operator
     */
    private final UnaryBcOperator operator;
    /**
     * x vector
     */
    private final BitVector xVector;
    /**
     * number of bits
     */
    private final int bitNum;
    /**
     * z (plain)
     */
    private BitVector recvPlainVector;
    /**
     * z (secret)
     */
    private BitVector recvSecretVector;

    SingleUnaryZ2cReceiverThread(Z2cParty receiver, UnaryBcOperator operator, BitVector xVector) {
        this.receiver = receiver;
        this.operator = operator;
        this.xVector = xVector;
        bitNum = xVector.bitNum();
    }

    BitVector getRecvPlainVector() {
        return recvPlainVector;
    }

    BitVector getRecvSecretVector() {
        return recvSecretVector;
    }

    @Override
    public void run() {
        try {
            receiver.init(bitNum);
            // set inputs
            MpcZ2Vector xPlainMpcVector = receiver.create(true, xVector);
            MpcZ2Vector x1SecretMpcVector = receiver.shareOther(bitNum);
            MpcZ2Vector z1PlainMpcVector, z1SecretMpcVector;
            //noinspection SwitchStatementWithTooFewBranches
            switch (operator) {
                case NOT:
                    // (plain, plain)
                    z1PlainMpcVector = receiver.not(xPlainMpcVector);
                    receiver.revealOther(z1PlainMpcVector);
                    recvSecretVector = receiver.revealOwn(z1PlainMpcVector);
                    // (plain, secret)
                    z1SecretMpcVector = receiver.not(x1SecretMpcVector);
                    receiver.revealOther(z1SecretMpcVector);
                    recvPlainVector = receiver.revealOwn(z1SecretMpcVector);
                    break;
                default:
                    throw new IllegalStateException("Invalid " + UnaryBcOperator.class.getSimpleName() + ": " + operator.name());
            }
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
