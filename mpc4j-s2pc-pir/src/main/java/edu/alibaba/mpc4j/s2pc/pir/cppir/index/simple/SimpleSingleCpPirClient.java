package edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.common.structure.matrix.zl64.Zl64Matrix;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleCpPirClient;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils.INT_MAX_VALUE;
import static edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleSingleCpPirDesc.*;

/**
 * Simple client-specific preprocessing PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/9/18
 */
public class SimpleSingleCpPirClient extends AbstractSingleCpPirClient {
    /**
     * Simple PIR params
     */
    private SimpleSingleCpPirParams params;
    /**
     * number of columns
     */
    private int cols;
    /**
     * number of rows
     */
    private int rows;
    /**
     * hints
     */
    private Zl64Matrix[] hints;
    /**
     * secret key
     */
    private Zl64Vector secretKey;
    /**
     * random matrix A
     */
    private Zl64Matrix matrixA;
    /**
     * Z_p elements num
     */
    private int d;
    /**
     * partition l
     */
    private int partitionL;
    /**
     * partition l (in byte)
     */
    private int partitionByteL;
    /**
     * partition size
     */
    private int partitionSize;

    public SimpleSingleCpPirClient(Rpc clientRpc, Party serverParty, SimpleSingleCpPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        clientSetup(n, l);
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, paramTime, "Client setups params");

        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> seedPayload = rpc.receive(seedPayloadHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        matrixA = Zl64Matrix.createRandom(params.zl64, cols, SimpleSingleCpPirParams.N, seed);
        matrixA.setParallel(parallel);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, seedTime, "Client generates matrix A");

        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hintPayload = rpc.receive(hintPayloadHeader).getPayload();

        stopWatch.start();
        hints = new Zl64Matrix[partitionSize];
        MpcAbortPreconditions.checkArgument(hintPayload.size() == partitionSize);
        for (int i = 0; i < partitionSize; i++) {
            long[] elements = LongUtils.byteArrayToLongArray(hintPayload.get(i));
            hints[i] = Zl64Matrix.create(params.zl64, elements, rows, SimpleSingleCpPirParams.N);
            hints[i].setParallel(parallel);
        }
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Client stores hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    private void clientSetup(int databaseSize, int l) {
        // compute max number of partitions
        int maxPartitionNum = CommonUtils.getUnitNum(l, SimpleSingleCpPirParams.LOG_P - 1);
        int tempPartitionL = 0;
        for (int partitionNum = 1; partitionNum < maxPartitionNum + 1; partitionNum++) {
            tempPartitionL = CommonUtils.getUnitNum(l, partitionNum);
            int tempPartitionByteL = CommonUtils.getByteLength(tempPartitionL);
            // each row would be divided into d rows in this partition
            d = CommonUtils.getUnitNum(tempPartitionByteL * Byte.SIZE, SimpleSingleCpPirParams.LOG_P - 1);
            // now the database is expanded to d * N, and this may exceed Integer.MAX_VALUE
            BigInteger expandN = BigInteger.valueOf(d).multiply(BigInteger.valueOf(databaseSize));
            if (expandN.compareTo(INT_MAX_VALUE.shiftRight(1)) < 0) {
                int[] dimensions = PirUtils.approxSquareDatabaseDims(databaseSize, d);
                rows = dimensions[0];
                cols = dimensions[1];
                params = new SimpleSingleCpPirParams(envType, PirUtils.getBitLength(cols));
                break;
            }
            // since database.rows() must be less than Integer.MAX_VALUE, and we can make as many partitions as possible
            // the loop must break at some point.
        }
        partitionL = Math.min(tempPartitionL, l);
        partitionByteL = CommonUtils.getByteLength(partitionL);
        partitionSize = CommonUtils.getUnitNum(l, partitionL);
        // secret key
        secretKey = Zl64Vector.createRandom(params.zl64, SimpleSingleCpPirParams.N, secureRandom);
    }

    @Override
    public byte[] pir(int x) throws MpcAbortException {
        setPtoInput(x);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // client generates query
        stopWatch.start();
        List<byte[]> clientQueryPayload = generateQuery(x);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        // receive response
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        stopWatch.start();
        byte[] element = decodeResponse(serverResponsePayload, x);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    /**
     * client generates query.
     *
     * @param index retrieval index.
     * @return client query.
     */
    private List<byte[]> generateQuery(int index) {
        int rowElementsNum = rows / d;
        // column index
        int colIndex = index / rowElementsNum;
        // q / p
        long floor = SimpleSingleCpPirParams.Q / params.p;
        // error term, allow correctness error 𝛿 = 2^{−40}.
        Zl64Vector error = Zl64Vector.createGaussianSample(params.zl64, cols, 0, SimpleSingleCpPirParams.SIGMA);
        // query = A * s + e + q/p * u_i_col
        Zl64Vector query = matrixA.matrixMulVector(secretKey);
        query.addi(error);
        // Add q/p * 1 only to the index corresponding to the desired column
        long[] elements = query.getElements();
        elements[colIndex] = params.zl64.add(elements[colIndex], floor);
        return Collections.singletonList(LongUtils.longArrayToByteArray(elements));
    }

    /**
     * client decodes response.
     *
     * @param serverResponse server response.
     * @param index          retrieval index.
     * @return retrieval element.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private byte[] decodeResponse(List<byte[]> serverResponse, int index)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionSize);
        int rowElementsNum = rows / d;
        // row index
        int rowIndex = index % rowElementsNum;
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        double delta = params.p * 1.0 / SimpleSingleCpPirParams.Q;
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            Zl64Vector vector = hints[i].matrixMulVector(secretKey);
            long[] responseElements = LongUtils.byteArrayToLongArray(serverResponse.get(i));
            Zl64Vector response = Zl64Vector.create(params.zl64, responseElements);
            response.subi(vector);
            long[] element = IntStream.range(0, d)
                .mapToLong(j -> Math.round(response.getElement(rowIndex * d + j) * delta) % params.p)
                .toArray();
            byte[] bytes = PirUtils.convertCoeffsToBytes(element, SimpleSingleCpPirParams.LOG_P - 1);
            databases[i] = ZlDatabase.create(partitionL, new byte[][]{BytesUtils.clone(bytes, 0, partitionByteL)});
        });
        return NaiveDatabase.createFromZl(l, databases).getBytesData(0);
    }
}
