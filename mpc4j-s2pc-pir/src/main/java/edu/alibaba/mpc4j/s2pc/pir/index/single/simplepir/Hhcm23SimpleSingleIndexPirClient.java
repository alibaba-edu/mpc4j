package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.zl64.Zl64Matrix;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils.INT_MAX_VALUE;
import static edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirPtoDesc.*;

/**
 * Simple PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23SimpleSingleIndexPirClient extends AbstractSingleIndexPirClient {

    /**
     * Simple PIR params
     */
    private Hhcm23SimpleSingleIndexPirParams params;
    /**
     * cols
     */
    private int cols;
    /**
     * rows
     */
    private int rows;
    /**
     * hint
     */
    private Zl64Matrix[] hint;
    /**
     * secret key
     */
    private Zl64Vector secretKey;
    /**
     * random matrix A
     */
    private Zl64Matrix a;
    /**
     * Z_p elements num
     */
    private int d;

    public Hhcm23SimpleSingleIndexPirClient(Rpc clientRpc, Party serverParty, Hhcm23SimpleSingleIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }


    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength)
        throws MpcAbortException {
        setInitInput(serverElementSize, elementBitLength);
        assert indexPirParams instanceof Hhcm23SimpleSingleIndexPirParams;
        params = (Hhcm23SimpleSingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> seedPayload = rpc.receive(seedPayloadHeader).getPayload();
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hintPayload = rpc.receive(hintPayloadHeader).getPayload();

        stopWatch.start();
        clientSetup(serverElementSize, elementBitLength);
        handledSeedPayload(seedPayload);
        hint = new Zl64Matrix[partitionSize];
        hint = handledHintPayload(hintPayload);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength) throws MpcAbortException {
        setInitInput(serverElementSize, elementBitLength);
        setDefaultParams();
        logPhaseInfo(PtoState.INIT_BEGIN);

        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> seedPayload = rpc.receive(seedPayloadHeader).getPayload();
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> hintPayload = rpc.receive(hintPayloadHeader).getPayload();

        stopWatch.start();
        clientSetup(serverElementSize, elementBitLength);
        handledSeedPayload(seedPayload);
        hint = new Zl64Matrix[partitionSize];
        hint = handledHintPayload(hintPayload);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // client generates query
        stopWatch.start();
        List<byte[]> clientQueryPayload = generateQuery(index);
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
        byte[] element = decodeResponse(serverResponsePayload, index);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    @Override
    public List<byte[]> clientSetup(int serverElementSize, int elementBitLength) {
        int maxPartitionBitLength = 0;
        int upperBound = CommonUtils.getUnitNum(elementBitLength, params.logP - 1);
        for (int count = 1; count < upperBound + 1; count++) {
            maxPartitionBitLength = CommonUtils.getUnitNum(elementBitLength, count);
            int maxPartitionByteLength = CommonUtils.getByteLength(maxPartitionBitLength);
            d = CommonUtils.getUnitNum(maxPartitionByteLength * Byte.SIZE, params.logP - 1);
            if ((BigInteger.valueOf(d).multiply(BigInteger.valueOf(serverElementSize)))
                .compareTo(INT_MAX_VALUE.shiftRight(1)) < 0) {
                int[] dims = PirUtils.approxSquareDatabaseDims(serverElementSize, d);
                rows = dims[0];
                cols = dims[1];
                params.setPlainModulo(PirUtils.getBitLength(cols));
                break;
            }
        }
        partitionBitLength = Math.min(maxPartitionBitLength, elementBitLength);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        // secret key
        secretKey = Zl64Vector.createRandom(params.zl64, params.n, secureRandom);
        return null;
    }

    /**
     * client setup.
     *
     * @param binSize                bin size.
     * @param binNum                 bin num.
     * @param elementBitLength       element bit length.
     * @param isCommunicationOptimal is communication optimal.
     */
    public void clientBatchSetup(int binSize, int binNum, int elementBitLength, boolean isCommunicationOptimal) {
        int maxPartitionBitLength = 0;
        int upperBound = CommonUtils.getUnitNum(elementBitLength, params.logP - 1);
        for (int count = 1; count < upperBound + 1; count++) {
            maxPartitionBitLength = CommonUtils.getUnitNum(elementBitLength, count);
            int maxPartitionByteLength = CommonUtils.getByteLength(maxPartitionBitLength);
            d = CommonUtils.getUnitNum(maxPartitionByteLength * Byte.SIZE, params.logP - 1);
            if ((BigInteger.valueOf(d).multiply(BigInteger.valueOf(binSize)))
                .compareTo(INT_MAX_VALUE.shiftRight(1)) < 0) {
                if (isCommunicationOptimal) {
                    rows = (int) Math.max(2, Math.ceil(Math.sqrt(d * binSize * binNum)));
                    rows = CommonUtils.getUnitNum(rows, binNum);
                    long rem = rows % d;
                    if (rem != 0) {
                        rows += d - rem;
                    }
                    cols = (int) Math.ceil((double) d * binSize / rows);
                } else {
                    int[] dims = PirUtils.approxSquareDatabaseDims(binSize, d);
                    rows = dims[0];
                    cols = dims[1];
                }
                params.setPlainModulo(PirUtils.getBitLength(cols));
                break;
            }
        }
        partitionBitLength = Math.min(maxPartitionBitLength, elementBitLength);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        // secret key
        secretKey = Zl64Vector.createRandom(params.zl64, params.n, secureRandom);
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        int rowElementsNum = rows / d;
        // column index
        int colIndex = index / rowElementsNum;
        // q / p
        long floor = params.q / params.p;
        // error term, allow correctness error 𝛿 = 2^{−40}.
        Zl64Vector error = Zl64Vector.createGaussianSample(params.zl64, cols, 0, params.stdDev);
        // query = A * s + e + q/p * u_i_col
        Zl64Vector query = a.matrixMulVector(secretKey);
        query.addi(error);
        // Add q/p * 1 only to the index corresponding to the desired column
        long[] elements = query.getElements();
        elements[colIndex] = params.zl64.add(elements[colIndex], floor);
        return Collections.singletonList(LongUtils.longArrayToByteArray(elements));
    }

    @Override
    public byte[] decodeResponse(List<byte[]> serverResponse, int index) throws MpcAbortException {
        return decodeResponse(serverResponse, index, elementBitLength);
    }

    @Override
    public byte[] decodeResponse(List<byte[]> serverResponse, int index, int elementBitLength)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionSize);
        int rowElementsNum = rows / d;
        // row index
        int rowIndex = index % rowElementsNum;
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        double delta = params.p * 1.0 / params.q;
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            Zl64Vector vector = hint[i].matrixMulVector(secretKey);
            long[] responseElements = LongUtils.byteArrayToLongArray(serverResponse.get(i));
            Zl64Vector response = Zl64Vector.create(params.zl64, responseElements);
            response.subi(vector);
            long[] element = IntStream.range(0, d)
                .mapToLong(j ->
                    (int) (Math.round(response.getElement(rowIndex * d + j) * delta) % params.p))
                .toArray();
            byte[] bytes = PirUtils.convertCoeffsToBytes(element, params.logP - 1);
            databases[i] = ZlDatabase.create(
                partitionBitLength, new byte[][]{BytesUtils.clone(bytes, 0, partitionByteLength)}
            );
        });
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }

    @Override
    public void setDefaultParams() {
        params = Hhcm23SimpleSingleIndexPirParams.DEFAULT_PARAMS;
    }

    /**
     * client handles hint payload.
     *
     * @param hintPayload hint payload.
     * @return hint.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public Zl64Matrix[] handledHintPayload(List<byte[]> hintPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hintPayload.size() == partitionSize);
        Zl64Matrix[] hint = new Zl64Matrix[partitionSize];
        for (int i = 0; i < partitionSize; i++) {
            long[] elements = LongUtils.byteArrayToLongArray(hintPayload.get(i));
            hint[i] = Zl64Matrix.create(params.zl64, elements, rows, params.n);
            hint[i].setParallel(parallel);
        }
        return hint;
    }

    /**
     * client handles seed payload.
     *
     * @param seedPayload seed payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void handledSeedPayload(List<byte[]> seedPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        a = Zl64Matrix.createRandom(params.zl64, cols, params.n, seed);
        a.setParallel(parallel);
    }

    /**
     * return simple PIR params.
     *
     * @return simple PIR params.
     */
    public Hhcm23SimpleSingleIndexPirParams getParams() {
        return params;
    }

    public byte[] decodeResponse(List<byte[]> serverResponse, int index, Zl64Matrix[] hint,
                                 int elementBitLength) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionSize);
        int rowElementsNum = rows / d;
        // row index
        int rowIndex = index % rowElementsNum;
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        double delta = params.p * 1.0 / params.q;
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            Zl64Vector vector = hint[i].matrixMulVector(secretKey);
            long[] responseElements = LongUtils.byteArrayToLongArray(serverResponse.get(i));
            Zl64Vector response = Zl64Vector.create(params.zl64, responseElements);
            response.subi(vector);
            long[] element = IntStream.range(0, d)
                .mapToLong(j ->
                    (int) (Math.round(response.getElement(rowIndex * d + j) * delta) % params.p))
                .toArray();
            byte[] bytes = PirUtils.convertCoeffsToBytes(element, params.logP - 1);
            databases[i] = ZlDatabase.create(
                partitionBitLength, new byte[][]{BytesUtils.clone(bytes, 0, partitionByteLength)}
            );
        });
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }
}
