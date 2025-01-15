package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc.PtoStep;

import java.nio.ByteBuffer;
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
public class SimpleCpIdxPirClient extends AbstractCpIdxPirClient implements HintCpIdxPirClient {
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * σ
     */
    private final double sigma;
    /**
     * partition
     */
    private int partition;
    /**
     * byteL for each partition
     */
    private int subByteL;
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
     * transpose matrix A
     */
    private IntMatrix transposeMatrixA;
    /**
     * transpose hint
     */
    private IntMatrix[] transposeHint;
    /**
     * secret key s ← Z_q^n, As
     */
    private IntVector[] ass;
    /**
     * hint · s
     */
    private IntVector[][] hss;

    public SimpleCpIdxPirClient(Rpc clientRpc, Party serverParty, SimpleCpIdxPirConfig config) {
        super(SimpleCpIdxPirPtoDesc.getInstance(), clientRpc, serverParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
        sigma = gaussianLweParam.getSigma();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and byteL columns.
        subByteL = Math.min(byteL, SimpleCpIdxPirPtoDesc.getMaxSubByteL(n));
        int[] sizes = SimpleCpIdxPirPtoDesc.getMatrixSize(n, byteL);
        rows = sizes[0];
        rowElementNum = rows / subByteL;
        columns = sizes[1];
        partition = sizes[2];
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, paramTime, "Client setups params");

        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        IntMatrix matrixA = IntMatrix.createRandom(columns, dimension, seed);
        transposeMatrixA = matrixA.transpose();
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, seedTime, "Client generates matrix A");

        stopWatch.start();
        transposeHint = new IntMatrix[partition];
        for (int p = 0; p < partition; p++) {
            List<byte[]> hintPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HINT.ordinal());
            MpcAbortPreconditions.checkArgument(hintPayload.size() == rows);
            byte[][] hintBytes = hintPayload.toArray(new byte[0][]);
            IntVector[] hintVectors = IntStream.range(0, rows)
                .mapToObj(i -> IntUtils.byteArrayToIntArray(hintBytes[i]))
                .map(IntVector::create)
                .toArray(IntVector[]::new);
            IntMatrix hint = IntMatrix.create(hintVectors);
            transposeHint[p] = hint.transpose();
        }
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Client stores hints");

        updateKeys();

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(int[] xs) throws MpcAbortException {
        setPtoInput(xs);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(xs[i], i);
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = recover(xs[i], i);
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        return entries;
    }

    @Override
    public void query(int x, int i) {
        // client generates qu
        int colIndex = x / rowElementNum;
        // qu = A * s + e + q/p * u_i_col
        IntVector e = IntVector.createGaussian(columns, sigma, secureRandom);
        IntVector qu = ass[i].add(e);
        qu.addi(colIndex, 1 << (Integer.SIZE - Byte.SIZE));
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    @Override
    public byte[] recover(int x, int i) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partition);
        IntVector[] ansArray = responsePayload.stream()
            .map(ans -> IntVector.create(IntUtils.byteArrayToIntArray(ans)))
            .toArray(IntVector[]::new);
        for (IntVector ans : ansArray) {
            MpcAbortPreconditions.checkArgument(ans.getNum() == rows);
        }
        int rowIndex = x % rowElementNum;
        ByteBuffer paddingEntryByteBuffer = ByteBuffer.allocate(subByteL * partition);
        for (int p = 0; p < partition; p++) {
            IntVector d = ansArray[p].sub(hss[i][p]);
            byte[] partitionEntry = new byte[subByteL];
            for (int elementIndex = 0; elementIndex < subByteL; elementIndex++) {
                int element = d.getElement(rowIndex * subByteL + elementIndex);
                if ((element & 0x00800000) > 0) {
                    partitionEntry[elementIndex] = (byte) ((element >>> (Integer.SIZE - Byte.SIZE)) + 1);
                } else {
                    partitionEntry[elementIndex] = (byte) (element >>> (Integer.SIZE - Byte.SIZE));
                }
            }
            paddingEntryByteBuffer.put(partitionEntry);
        }
        byte[] paddingEntry = paddingEntryByteBuffer.array();
        byte[] entry = new byte[byteL];
        System.arraycopy(paddingEntry, subByteL * partition - byteL, entry, 0, byteL);
        return entry;
    }

    @Override
    public void updateKeys() {
        stopWatch.start();
        ass = new IntVector[maxBatchNum];
        hss = new IntVector[maxBatchNum][partition];
        IntStream batchIntStream = parallel ? IntStream.range(0, maxBatchNum).parallel() : IntStream.range(0, maxBatchNum);
        batchIntStream.forEach(batchIndex -> {
            IntVector s = IntVector.createRandom(dimension, secureRandom);
            ass[batchIndex] = transposeMatrixA.leftMul(s);
            // generate s and s · hint
            IntStream intStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
            hss[batchIndex] = intStream.mapToObj(p -> transposeHint[p].leftMul(s)).toArray(IntVector[]::new);
        });
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyTime, "Client updates keys");
    }
}
