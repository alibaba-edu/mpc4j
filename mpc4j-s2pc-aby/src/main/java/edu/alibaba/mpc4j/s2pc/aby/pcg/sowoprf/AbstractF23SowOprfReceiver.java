package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.common.tool.utils.BlockUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotReceiverOutput;

import java.util.Arrays;

/**
 * abstract (F2, F3)-sowOPRF receiver.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public abstract class AbstractF23SowOprfReceiver extends AbstractTwoPartyPto implements F23SowOprfReceiver {
    /**
     * Z3 field
     */
    protected final Z3ByteField z3Field;
    /**
     * (F2, F3)-wPRF
     */
    protected final F23Wprf f23Wprf;
    /**
     * matrix A
     */
    protected DenseBitMatrix matrixA;
    /**
     * matrix B
     */
    protected F23WprfMatrix matrixB;
    /**
     * max batch size
     */
    protected int expectBatchSize;
    /**
     * inputs
     */
    protected byte[][] inputs;
    /**
     * batch size
     */
    protected int batchSize;
    /**
     * pre-computed COT receiver output
     */
    protected CotReceiverOutput preCotReceiverOutput;

    protected AbstractF23SowOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, F23SowOprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        z3Field = new Z3ByteField();
        byte[] seedA = BlockUtils.zeroBlock();
        byte[] seedB = BlockUtils.zeroBlock();
        Arrays.fill(seedB, (byte) 0xFF);
        f23Wprf = new F23Wprf(z3Field, seedA, seedB, config.getMatrixType());
        matrixA = f23Wprf.getMatrixA();
        matrixB = f23Wprf.getMatrixB();
    }

    protected void setInitInput(int expectBatchSize) {
        MathPreconditions.checkPositive("expectBatchSize", expectBatchSize);
        this.expectBatchSize = expectBatchSize;
        initState();
    }

    protected void setInitInput() {
        expectBatchSize = -1;
        initState();
    }

    protected void setPtoInput(byte[][] inputs) {
        checkInitialized();
        MathPreconditions.checkPositive("batchSize", inputs.length);
        batchSize = inputs.length;
        this.inputs = Arrays.stream(inputs)
            .peek(input -> MathPreconditions.checkEqual("n (byte)", "input.length", F23Wprf.getInputByteLength(), input.length))
            .toArray(byte[][]::new);
    }

    protected void setPtoInput(byte[][] inputs, CotReceiverOutput preCotReceiverOutput) {
        setPtoInput(inputs);
        if (preCotReceiverOutput != null) {
            MathPreconditions.checkEqual("pre-computed COT num", "COT num", batchSize * F23Wprf.M, preCotReceiverOutput.getNum());
        }
        this.preCotReceiverOutput = preCotReceiverOutput;
    }
}
