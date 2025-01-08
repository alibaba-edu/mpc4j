package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.HashBinEntry;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirClient;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleBinCpKsPirDesc.*;

/**
 * Simple bin client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SimpleBinCpKsPirClient<T> extends AbstractCpKsPirClient<T> {
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * σ
     */
    private final double sigma;
    /**
     * hash
     */
    private Hash hash;
    /**
     * secret key s ← Z_q^n, As
     */
    private IntVector as;
    /**
     * hint · s
     */
    private IntVector[] hs;
    /**
     * prf
     */
    private Prf prf;
    /**
     * columns
     */
    private int columns;
    /**
     * rows
     */
    private int rows;
    /**
     * partition count
     */
    private int partition;

    public SimpleBinCpKsPirClient(Rpc clientRpc, Party serverParty, SimpleBinCpKsPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
        sigma = gaussianLweParam.getSigma();
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        List<byte[]> hashBinInfoPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_HASH_BIN_INFO.ordinal());
        MpcAbortPreconditions.checkArgument(hashBinInfoPayload.size() == 2);
        prf = PrfFactory.createInstance(envType, Integer.BYTES);
        prf.setKey(hashBinInfoPayload.get(0));
        partition = byteL + DIGEST_BYTE_L;
        rows = IntUtils.byteArrayToInt(hashBinInfoPayload.get(1));
        columns = (int) Math.ceil(Math.sqrt((long) n * (long) partition));
        stopWatch.stop();
        long fusePosTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, fusePosTime, "Client init parameters");

        stopWatch.start();
        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_SEED.ordinal());
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        IntMatrix matrixA = IntMatrix.createRandom(columns, dimension, seed);
        IntMatrix transposeMatrixA = matrixA.transpose();
        IntVector s = IntVector.createRandom(dimension, secureRandom);
        as = transposeMatrixA.leftMul(s);
        IntMatrix[] transposeHint = new IntMatrix[partition];
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
        // generate s and s · hint
        IntStream intStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
        hs = intStream.mapToObj(p -> transposeHint[p].leftMul(s)).toArray(IntVector[]::new);
        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initSimplePirTime, "Client stores hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            query(keys.get(i));
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[batchNum][];
        for (int i = 0; i < batchNum; i++) {
            entries[i] = decode(keys.get(i));
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }

    private void query(T key) {
        HashBinEntry<T> hashBinEntry = HashBinEntry.fromRealItem(0, key);
        int colIndex = prf.getInteger(hashBinEntry.getItemByteArray(), columns);
        // client generates qu, qu = A * s + e + q/p * u_i_col
        IntVector e = IntVector.createGaussian(columns, sigma, secureRandom);
        IntVector qu = as.add(e);
        qu.addi(colIndex, 1 << (Integer.SIZE - Byte.SIZE));
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal(), queryPayload);
    }

    private byte[] decode(T key) throws MpcAbortException {
        List<byte[]> responsePayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == partition);
        IntVector[] ansArray = responsePayload.stream()
            .map(ans -> IntVector.create(IntUtils.byteArrayToIntArray(ans)))
            .toArray(IntVector[]::new);
        for (IntVector ans : ansArray) {
            MpcAbortPreconditions.checkArgument(ans.getNum() == rows);
        }
        for (int i = 0; i < rows; i++) {
            ByteBuffer entryByteBuffer = ByteBuffer.allocate(partition);
            for (int p = 0; p < partition; p++) {
                IntVector d = ansArray[p].sub(hs[p]);
                byte partitionEntry;
                int element = d.getElement(i);
                if ((element & 0x00800000) > 0) {
                    partitionEntry = (byte) ((element >>> (Integer.SIZE - Byte.SIZE)) + 1);
                } else {
                    partitionEntry = (byte) (element >>> (Integer.SIZE - Byte.SIZE));
                }
                entryByteBuffer.put(partitionEntry);
            }
            byte[] entry = entryByteBuffer.array();
            byte[] actualDigest = BytesUtils.clone(entry, 0, DIGEST_BYTE_L);
            byte[] expectDigest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
            if (BytesUtils.equals(actualDigest, expectDigest)) {
                return BytesUtils.clone(entry, DIGEST_BYTE_L, byteL);
            }
        }
        return null;
    }
}
