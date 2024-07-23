package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpPbcIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc.PtoStep;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Simple client-specific preprocessing index PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleCpIdxPirClient extends AbstractCpIdxPirClient implements CpPbcIdxPirClient {
    /**
     * rows
     */
    private int rows;
    /**
     * columns
     */
    private int columns;
    /**
     * number of elements in each row
     */
    private int rowElementNum;
    /**
     * transposed random matrix A
     */
    private IntMatrix transposeMatrixA;
    /**
     * secret key s ← Z_q^n
     */
    private IntVector s;
    /**
     * hint · s
     */
    private IntVector hs;

    public SimpleCpIdxPirClient(Rpc clientRpc, Party serverParty, SimpleCpIdxPirConfig config) {
        super(SimpleCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and byteL columns.
        long entryNum = (long) byteL * n;
        int sqrt = (int) Math.ceil(Math.sqrt(entryNum));
        // ensure that each row can contain at least one element
        sqrt = Math.max(sqrt, byteL);
        // we make DB rows a little bit larger than rows to ensure each column can contain more data.
        rowElementNum = CommonUtils.getUnitNum(sqrt, byteL);
        rows = rowElementNum * byteL;
        columns = (int) Math.ceil((double) entryNum / rows);
        assert (long) rows * columns >= entryNum;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, paramTime, "Client setups params");

        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        IntMatrix matrixA = IntMatrix.createRandom(columns, SimpleCpIdxPirPtoDesc.N, seed);
        transposeMatrixA = matrixA.transpose();
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, seedTime, "Client generates matrix A");

        List<byte[]> hintPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(hintPayload.size() == rows);
        byte[][] hintBytes = hintPayload.toArray(new byte[0][]);
        IntVector[] hintVectors = IntStream.range(0, rows)
            .mapToObj(i -> IntUtils.byteArrayToIntArray(hintBytes[i]))
            .map(IntVector::create)
            .toArray(IntVector[]::new);
        IntMatrix hint = IntMatrix.create(hintVectors);
        IntMatrix transposeHint = hint.transpose();
        // generate s and s · hint
        s = IntVector.createRandom(SimpleCpIdxPirPtoDesc.N, secureRandom);
        hs = transposeHint.leftMul(s);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Client stores hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(xs[i]);
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(xs[i]);
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        return entries;
    }

    @Override
    public void query(int x) {
        // client generates qu
        int colIndex = x / rowElementNum;
        // qu = A * s + e + q/p * u_i_col
        IntVector e = IntVector.createGaussian(columns, SimpleCpIdxPirPtoDesc.SIGMA);
        IntVector qu = transposeMatrixA.leftMul(s);
        qu.addi(e);
        qu.addi(colIndex, 1 << (Integer.SIZE - Byte.SIZE));
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public byte[] recover(int x) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == 1);
        IntVector ans = IntVector.create(IntUtils.byteArrayToIntArray(responsePayload.get(0)));
        MpcAbortPreconditions.checkArgument(ans.getNum() == rows);

        int rowIndex = x % rowElementNum;
        IntVector d = ans.sub(hs);
        byte[] element = new byte[byteL];
        for (int elementIndex = 0; elementIndex < byteL; elementIndex++) {
            element[elementIndex] = (byte) (d.getElement(rowIndex * byteL + elementIndex) >>> (Integer.SIZE - Byte.SIZE));
        }
        return element;
    }
}
