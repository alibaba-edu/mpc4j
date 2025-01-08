package edu.alibaba.mpc4j.s2pc.opf.shuffle;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.Arrays;

/**
 * abstract shuffle sender
 *
 * @author Feng Han
 * @date 2024/9/27
 */
public abstract class AbstractShuffleSender extends AbstractTwoPartyPto implements ShuffleParty {
    /**
     * the number of data row
     */
    protected int dataNum;
    /**
     * the number of bit number of each data
     */
    protected int dimNum;
    /**
     * transferred data in row form
     */
    protected byte[][] rowData;

    protected AbstractShuffleSender(PtoDesc ptoDesc, Rpc senderRpc, Party receiverParty, ShuffleConfig config) {
        super(ptoDesc, senderRpc, receiverParty, config);
    }

    protected void setInitInput() {
        initState();
    }

    protected void setPtoInput(MpcZ2Vector[] inputVectors, int dataNum, int dimNum) {
        assert inputVectors != null;
        this.dataNum = dataNum;
        this.dimNum = dimNum;
        MathPreconditions.checkEqual("dimNum", "inputVectors.length", dimNum, inputVectors.length);
        for (int i = 1; i < inputVectors.length; i++) {
            MathPreconditions.checkEqual("data num", "inputVectors[i].bitNum()", dataNum, inputVectors[i].bitNum());
        }
        ZlDatabase zlDatabase = ZlDatabase.create(envType, parallel, Arrays.stream(inputVectors).map(MpcZ2Vector::getBitVector).toArray(BitVector[]::new));
        rowData = zlDatabase.getBytesData();
    }

    protected SquareZ2Vector[] getResultVectors() {
        int resBit = dimNum & 7;
        if (resBit > 0) {
            byte andNum = (byte) ((1 << resBit) - 1);
            Arrays.stream(rowData).forEach(row -> row[0] &= andNum);
        }
        ZlDatabase zlDatabase = ZlDatabase.create(dimNum, rowData);
        BitVector[] data = zlDatabase.bitPartition(envType, parallel);
        return Arrays.stream(data).map(each -> SquareZ2Vector.create(each, false)).toArray(SquareZ2Vector[]::new);
    }

}
