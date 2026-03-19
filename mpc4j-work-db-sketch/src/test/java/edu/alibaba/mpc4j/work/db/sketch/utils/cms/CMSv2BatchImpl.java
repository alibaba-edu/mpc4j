package edu.alibaba.mpc4j.work.db.sketch.utils.cms;

import edu.alibaba.mpc4j.common.circuit.z2.MpcZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2Vector;
import edu.alibaba.mpc4j.common.circuit.z2.PlainZ2cParty;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.work.db.sketch.utils.LowMcCircuit;
import edu.alibaba.mpc4j.work.db.sketch.utils.Utils;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParam;
import edu.alibaba.mpc4j.work.scape.s3pc.opf.soprp.lowmc.LowMcParamUtils;

import java.math.BigInteger;
import java.util.Arrays;

public class CMSv2BatchImpl extends AbstractCMSBatchImpl implements CMS {
    private final PlainZ2cParty party;
    private final LowMcCircuit circuit;
    private PlainZ2Vector[] keys;

    public CMSv2BatchImpl(int d, int t, PlainZ2Vector[] hashKeys, int elementBitLen) {
        super(d, t, elementBitLen);
        assert (d == hashKeys.length) : "row size must be equal to hash parameter length";
        this.keys = hashKeys;
        this.party = new PlainZ2cParty();
        LowMcParam lowMcParam = LowMcParamUtils.getParam(64, 23131, 40);
        this.circuit = new LowMcCircuit(party, null, lowMcParam);
        try {
            circuit.init();
        } catch (MpcAbortException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void merge() {
        PlainZ2Vector[] plainBuffer;
        BitVector[] bitVectors = new BitVector[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            bitVectors[i] = BitVectorFactory.create(32, buffer[i]);
        }
        plainBuffer = party.setPublicValues(bitVectors);
        plainBuffer = Utils.matrixTranspose(party, plainBuffer);
        for (int i = 0; i < rowNum; i++) {
            int[] indexes = hash(plainBuffer, i);
            int finalI = i;
            assert indexes != null;
            Arrays.stream(indexes).forEach(index -> {
                data[finalI][index] += 1;
            });
        }
        bufferSize = 0;
    }

    private int[] hash(PlainZ2Vector[] bufferData, int index) {
        MpcZ2Vector[] hashValue;
        try {
            circuit.setKey(keys[index]);
            hashValue = circuit.enc(bufferData);
        } catch (MpcAbortException e) {
            throw new RuntimeException(e);
        }
        if (hashValue != null) {
            hashValue = Utils.matrixTranspose(party, hashValue);
            Arrays.stream(hashValue).forEach(ele -> ele.split(ele.bitNum() - logSize));
            return Arrays.stream(hashValue).mapToInt(ele -> ele.getBitVector().getBigInteger().intValueExact()
            ).toArray();
        }
        return null;
    }

    @Override
    protected int hash(BigInteger element, int index) {
        PlainZ2Vector[] plainBuffer;
        BitVector[] bitVector = new BitVector[]{BitVectorFactory.create(32, element)};
        plainBuffer = party.setPublicValues(bitVector);
        plainBuffer = Utils.matrixTranspose(party, plainBuffer);
        int[] hashRes = hash(plainBuffer, index);
        assert hashRes != null;
        return hashRes[0];
    }
}
