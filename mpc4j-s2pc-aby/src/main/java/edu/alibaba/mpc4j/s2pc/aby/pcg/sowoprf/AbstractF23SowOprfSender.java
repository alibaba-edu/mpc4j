package edu.alibaba.mpc4j.s2pc.aby.pcg.sowoprf;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitmatrix.dense.DenseBitMatrix;
import edu.alibaba.mpc4j.common.tool.galoisfield.Z3ByteField;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;

import java.util.Arrays;

/**
 * abstract (F2, F3)-sowOPRF sender.
 *
 * @author Weiran Liu
 * @date 2024/10/22
 */
public abstract class AbstractF23SowOprfSender extends AbstractTwoPartyPto implements F23SowOprfSender {
    /**
     * Z3 field
     */
    protected final Z3ByteField z3Field;
    /**
     * (F2, F3)-wPRF
     */
    private final F23Wprf f23Wprf;
    /**
     * key
     */
    protected final byte[] key;
    /**
     * matrix A
     */
    protected DenseBitMatrix matrixA;
    /**
     * matrix B
     */
    protected F23WprfMatrix matrixB;
    /**
     * expect batch size
     */
    protected int expectBatchSize;
    /**
     * batch size
     */
    protected int batchSize;
    /**
     * pre-computed COT sender output
     */
    protected CotSenderOutput preCotSenderOutput;

    protected AbstractF23SowOprfSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, F23SowOprfConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
        z3Field = new Z3ByteField();
        byte[] seedA = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        byte[] seedB = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        Arrays.fill(seedB, (byte) 0xFF);
        f23Wprf = new F23Wprf(z3Field, seedA, seedB, config.getMatrixType());
        matrixA = f23Wprf.getMatrixA();
        matrixB = f23Wprf.getMatrixB();
        key = f23Wprf.keyGen(secureRandom);
        f23Wprf.init(key);
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

    protected void setPtoInput(int batchSize) {
        checkInitialized();
        MathPreconditions.checkPositive("batchSize", batchSize);
        this.batchSize = batchSize;
    }

    protected void setPtoInput(int batchSize, CotSenderOutput preCotSenderOutput) {
        setPtoInput(batchSize);
        if (preCotSenderOutput != null) {
            MathPreconditions.checkEqual("pre-computed COT num", "COT num", batchSize * F23Wprf.M, preCotSenderOutput.getNum());
        }
        this.preCotSenderOutput = preCotSenderOutput;
    }

    @Override
    public byte[] prf(byte[] x) {
        return f23Wprf.prf(x);
    }
}
