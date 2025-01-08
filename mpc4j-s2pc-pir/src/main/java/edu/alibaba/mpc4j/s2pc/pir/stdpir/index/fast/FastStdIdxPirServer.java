package edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.AbstractStdIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.stdpir.index.PbcableStdIdxPirServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.stdpir.index.fast.FastStdIdxPirPtoDesc.getInstance;

/**
 * FastPIR server.
 *
 * @author Liqiang Peng
 * @date 2023/1/18
 */
public class FastStdIdxPirServer extends AbstractStdIdxPirServer implements PbcableStdIdxPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * Fast PIR params
     */
    private final FastStdIdxPirParams params;
    /**
     * partition size
     */
    protected int partitionSize;
    /**
     * partition bit-length
     */
    protected int partitionBitLength;
    /**
     * partition byte length
     */
    protected int partitionByteLength;
    /**
     * Galois keys
     */
    private byte[] galoisKeys;
    /**
     * BFV plaintext in NTT form
     */
    private List<byte[][]> encodedDatabase;
    /**
     * query payload size
     */
    private int queryPayloadSize;
    /**
     * element column size
     */
    private int elementColumnLength;

    public FastStdIdxPirServer(Rpc serverRpc, Party clientParty, FastStdIdxPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        params = config.getStdIdxPirParams();
    }

    @Override
    public void init(NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        // receive Galois keys
        List<byte[]> serverKeysPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal());
        init(serverKeysPayload, database, maxBatchNum);
    }

    @Override
    public void init(List<byte[]> serverKeys, NaiveDatabase database, int maxBatchNum) throws MpcAbortException {
        if (serverKeys == null) {
            init(database, maxBatchNum);
        } else {
            setInitInput(database, maxBatchNum);
            logPhaseInfo(PtoState.INIT_BEGIN);

            stopWatch.start();
            MpcAbortPreconditions.checkArgument(serverKeys.size() == 1);
            this.galoisKeys = serverKeys.get(0);
            int byteLength = database.getByteL();
            if (byteLength % 2 == 1) {
                byteLength++;
            }
            int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
            partitionBitLength = Math.min(maxPartitionBitLength, byteLength * Byte.SIZE);
            partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
            ZlDatabase[] databases = database.partitionZl(partitionBitLength);
            partitionSize = databases.length;
            queryPayloadSize = CommonUtils.getUnitNum(database.rows(), params.getPolyModulusDegree() / 2);
            elementColumnLength = CommonUtils.getUnitNum(
                (partitionByteLength / 2) * Byte.SIZE, params.getPlainModulusBitLength()
            );
            IntStream intStream = parallel ? IntStream.range(0, partitionSize).parallel() : IntStream.range(0, partitionSize);
            encodedDatabase = intStream
                .mapToObj(partitionIndex -> preprocessDatabase(databases, partitionIndex))
                .collect(Collectors.toList());
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

            logPhaseInfo(PtoState.INIT_END);
        }
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    private byte[][] preprocessDatabase(ZlDatabase[] databases, int partitionIndex) {
        int coeffCount = params.getPolyModulusDegree();
        long[][] encodedDatabase = new long[queryPayloadSize * elementColumnLength][coeffCount];
        int rowSize = params.getPolyModulusDegree() / 2;
        for (int i = 0; i < databases[partitionIndex].rows(); i++) {
            int rowIndex = i / rowSize;
            int colIndex = i % rowSize;
            byte[] element = databases[partitionIndex].getBytesData(i);
            int length = element.length / 2;
            byte[] upperBytes = new byte[length];
            System.arraycopy(element, 0, upperBytes, 0, length);
            byte[] lowerBytes = new byte[length];
            System.arraycopy(element, length, lowerBytes, 0, length);
            long[] upperCoeffs = PirUtils.convertBytesToCoeffs(params.getPlainModulusBitLength(), 0, length, upperBytes);
            long[] lowerCoeffs = PirUtils.convertBytesToCoeffs(params.getPlainModulusBitLength(), 0, length, lowerBytes);
            for (int j = 0; j < elementColumnLength; j++) {
                encodedDatabase[rowIndex][colIndex] = upperCoeffs[j];
                encodedDatabase[rowIndex][colIndex + rowSize] = lowerCoeffs[j];
                rowIndex += queryPayloadSize;
            }
        }
        return FastStdIdxPirNativeUtils.nttTransform(params.getEncryptionParams(), encodedDatabase).toArray(new byte[0][]);
    }

    @Override
    public void answer() throws MpcAbortException {
        List<byte[]> queryPayload = receiveOtherPartyPayload(PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(queryPayload.size() == queryPayloadSize);
        List<byte[]> clientQuery = IntStream.range(0, queryPayloadSize)
            .mapToObj(queryPayload::get)
            .collect(Collectors.toCollection(ArrayList::new));
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        List<byte[]> responsePayload = intStream
            .mapToObj(i -> FastStdIdxPirNativeUtils.generateResponse(
                params.getEncryptionParams(),
                galoisKeys,
                clientQuery,
                encodedDatabase.get(i),
                elementColumnLength))
            .collect(Collectors.toCollection(ArrayList::new));
        sendOtherPartyPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
