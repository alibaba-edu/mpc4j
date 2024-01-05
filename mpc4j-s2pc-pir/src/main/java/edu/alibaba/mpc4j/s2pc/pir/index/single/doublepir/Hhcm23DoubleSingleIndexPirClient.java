package edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.zl64.Zl64Matrix;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirPtoDesc.getInstance;

/**
 * Double PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23DoubleSingleIndexPirClient extends AbstractSingleIndexPirClient {

    /**
     * Double PIR params
     */
    private Hhcm23DoubleSingleIndexPirParams params;
    /**
     * cols
     */
    private int cols;
    /**
     * rows
     */
    private int rows;
    /**
     * hint_c
     */
    private Zl64Matrix[] hintC;
    /**
     * secret key 1
     */
    private Zl64Vector secretKey1;
    /**
     * secret key 2
     */
    private Zl64Vector secretKey2;
    /**
     * random matrix A1
     */
    private Zl64Matrix a1;
    /**
     * random matrix A2
     */
    private Zl64Matrix a2;

    public Hhcm23DoubleSingleIndexPirClient(Rpc clientRpc, Party serverParty, Hhcm23DoubleSingleIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }


    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength)
        throws MpcAbortException {
        setInitInput(serverElementSize, elementBitLength);
        assert indexPirParams instanceof Hhcm23DoubleSingleIndexPirParams;
        params = (Hhcm23DoubleSingleIndexPirParams) indexPirParams;
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
        handledHintPayload(hintPayload);
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
        handledHintPayload(hintPayload);
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
        int[] dims = PirUtils.approxSquareDatabaseDims(serverElementSize, 1);
        rows = dims[0];
        cols = dims[1];
        params.setPlainModulo(PirUtils.getBitLength(cols));
        partitionBitLength = Math.min(params.logP - 1, elementBitLength);
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        partitionSize = CommonUtils.getUnitNum(elementBitLength, partitionBitLength);
        // secret key
        secretKey1 = Zl64Vector.createRandom(params.zl64, params.n, secureRandom);
        secretKey1.setParallel(parallel);
        secretKey2 = Zl64Vector.createRandom(params.zl64, params.n, secureRandom);
        return null;
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        // row index
        int rowIndex = index / rows;
        // column index
        int colIndex = index % rows;
        // q / p
        long floor = params.q / params.p;
        // error vectors
        Zl64Vector e1 = Zl64Vector.createGaussianSample(params.zl64, cols, 0, params.stdDev);
        Zl64Vector e2 = Zl64Vector.createGaussianSample(params.zl64, rows, 0, params.stdDev);
        // Compute c_1 = A_1 * s_1 + e_1 + floor * u_i_row
        Zl64Vector c1 = a1.matrixMulVector(secretKey1);
        c1.setParallel(parallel);
        c1.addi(e1);
        // Add q/p * 1 only to the index corresponding to the desired column
        long[] elements1 = c1.getElements();
        elements1[colIndex] = params.zl64.add(elements1[colIndex], floor);
        // Compute c_2 = A_2 * s_2 + e_2 + floor * u_i_col
        Zl64Vector c2 = a2.matrixMulVector(secretKey2);
        c2.setParallel(parallel);
        c2.addi(e2);
        long[] elements2 = c2.getElements();
        elements2[rowIndex] = params.zl64.add(elements2[rowIndex], floor);
        List<byte[]> query = new ArrayList<>();
        query.add(LongUtils.longArrayToByteArray(elements1));
        query.add(LongUtils.longArrayToByteArray(elements2));
        return query;
    }

    @Override
    public byte[] decodeResponse(List<byte[]> serverResponse, int index) throws MpcAbortException {
        return decodeResponse(serverResponse, index, elementBitLength);
    }

    @Override
    public byte[] decodeResponse(List<byte[]> serverResponse, int index, int elementBitLength) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(serverResponse.size() == partitionSize * 2);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        long q = 1L << (params.zl64.getL() - 1);
        int size = (int) Math.ceil(Math.log(q - 1) / Math.log(params.p));
        double delta = params.p * 1.0 / params.q;
        for (int i = 0; i < partitionSize; i++) {
            Zl64Matrix h = Zl64Matrix.create(
                params.zl64, LongUtils.byteArrayToLongArray(serverResponse.get(i * 2)), size, params.n
            );
            Zl64Vector ans = Zl64Vector.create(
                params.zl64, LongUtils.byteArrayToLongArray(serverResponse.get(i * 2 + 1))
            );
            ans.setParallel(parallel);
            // hint_c_h =  hint_c || h
            Zl64Matrix combine = hintC[i].concat(h);
            combine.setParallel(parallel);
            // h1_hat_a1_hat = (ans_h || ans_2) - (hint_c || h) *  s_2
            ans.subi(combine.matrixMulVector(secretKey2));
            long[] ansElements = ans.getElements();
            for (int j = 0; j < ans.getNum(); j++) {
                ansElements[j] = (int) (Math.round(ansElements[j] * delta) % params.p);
            }
            long[] elements = new long[params.n];
            for (int j = 0; j < params.n; j++) {
                long[] digits = new long[size];
                System.arraycopy(ansElements, j * size, digits, 0, size);
                elements[j] = recomposed(params.p, digits);
            }
            long[] digits = new long[size];
            System.arraycopy(ansElements, params.n * size, digits, 0, size);
            long a1 = recomposed(params.p, digits);
            Zl64Vector h1 = Zl64Vector.create(params.zl64, elements);
            long value = params.zl64.sub(a1, secretKey1.innerProduct(h1));
            value = (int) (Math.round(value * delta) % params.p);
            byte[] bytes = PirUtils.convertCoeffsToBytes(new long[] {value}, partitionByteLength * Byte.SIZE);
            databases[i] = ZlDatabase.create(partitionBitLength, new byte[][]{bytes});
        }
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }

    @Override
    public void setDefaultParams() {
        params = Hhcm23DoubleSingleIndexPirParams.DEFAULT_PARAMS;
    }

    private long recomposed(long p, long[] digits) {
        long element = 0L;
        long r = 1L;
        for (long digit : digits) {
            element = element + r * digit;
            r = r * p;
        }
        return element;
    }

    /**
     * client handles hint payload.
     *
     * @param hintPayload hint payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handledHintPayload(List<byte[]> hintPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(hintPayload.size() == partitionSize);
        hintC = new Zl64Matrix[partitionSize];
        long q = 1L << (params.zl64.getL() - 1);
        int size = (int) Math.ceil(Math.log(q - 1) / Math.log(params.p));
        IntStream.range(0, partitionSize).forEach(i -> {
            long[] elements = LongUtils.byteArrayToLongArray(hintPayload.get(i));
            hintC[i] = Zl64Matrix.create(params.zl64, elements, size * params.n, params.n);
            hintC[i].setParallel(parallel);
        });
    }

    /**
     * client handles seed payload.
     *
     * @param seedPayload seed payload.
     * @throws MpcAbortException the protocol failure aborts.
     */
    private void handledSeedPayload(List<byte[]> seedPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 2);
        byte[] seed1 = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed1.length == CommonConstants.BLOCK_BYTE_LENGTH);
        a1 = Zl64Matrix.createRandom(params.zl64, cols, params.n, seed1);
        a1.setParallel(parallel);
        byte[] seed2 = seedPayload.get(1);
        MpcAbortPreconditions.checkArgument(seed2.length == CommonConstants.BLOCK_BYTE_LENGTH);
        a2 = Zl64Matrix.createRandom(params.zl64, rows, params.n, seed2);
        a2.setParallel(parallel);
    }
}