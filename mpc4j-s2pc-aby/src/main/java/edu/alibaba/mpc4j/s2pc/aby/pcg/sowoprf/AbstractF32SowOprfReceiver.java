package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.matrix.Z3ByteMatrix;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;

import java.util.Arrays;

/**
 * abstract
 *
 * @author Weiran Liu
 * @date 2024/6/6
 */
public abstract class AbstractF32SowOprfReceiver extends AbstractTwoPartyPto implements F32SowOprfReceiver {
    /**
     * Z3 field
     */
    protected final Z3ByteField z3Field;
    /**
     * (F3, F2)-wPRF
     */
    protected final F32Wprf f32Wprf;
    /**
     * matrix A
     */
    protected Z3ByteMatrix matrixA;
    /**
     * matrix B
     */
    protected DenseBitMatrix matrixB;
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

    protected AbstractF32SowOprfReceiver(PtoDesc ptoDesc, Rpc receiverRpc, Party senderParty, F32SowOprfConfig config) {
        super(ptoDesc, receiverRpc, senderParty, config);
        z3Field = new Z3ByteField();
        f32Wprf = new F32Wprf(z3Field, new byte[CommonConstants.BLOCK_BYTE_LENGTH], new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
        matrixA = f32Wprf.getMatrixA();
        matrixB = f32Wprf.getMatrixB();
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

    protected void setPtoInput(byte[][] inputs) throws MpcAbortException {
        checkInitialized();
        MathPreconditions.checkPositive("batchSize", inputs.length);
        batchSize = inputs.length;
        this.inputs = Arrays.stream(inputs)
            .peek(input -> {
                MathPreconditions.checkEqual("n", "input.length", F32Wprf.getInputLength(), input.length);
                for (byte b : input) {
                    Preconditions.checkArgument(z3Field.validateElement(b));
                }
            })
            .toArray(byte[][]::new);
        extraInfo++;
    }
}
