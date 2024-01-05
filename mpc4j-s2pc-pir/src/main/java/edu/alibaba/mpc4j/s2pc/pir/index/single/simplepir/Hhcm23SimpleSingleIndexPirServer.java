package edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.zl64.Zl64Matrix;
import edu.alibaba.mpc4j.common.structure.vector.Zl64Vector;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils.*;
import static edu.alibaba.mpc4j.s2pc.pir.index.single.simplepir.Hhcm23SimpleSingleIndexPirPtoDesc.*;

/**
 * Simple PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23SimpleSingleIndexPirServer extends AbstractSingleIndexPirServer {

    /**
     * Simple PIR params
     */
    public Hhcm23SimpleSingleIndexPirParams params;
    /**
     * hint
     */
    private Zl64Matrix[] hint;
    /**
     * database
     */
    private Zl64Matrix[] db;
    /**
     * random seed
     */
    private byte[] seed;
    /**
     * cols
     */
    private int cols;

    public Hhcm23SimpleSingleIndexPirServer(Rpc serverRpc, Party clientParty, Hhcm23SimpleSingleIndexPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        assert indexPirParams instanceof Hhcm23SimpleSingleIndexPirParams;
        params = (Hhcm23SimpleSingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        serverSetup(database);
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Collections.singletonList(seed)));
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hint[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        hint = null;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        setDefaultParams();
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        serverSetup(database);
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Collections.singletonList(seed)));
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hint[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        hint = null;
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        // receive query
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());

        // generate response
        stopWatch.start();
        List<byte[]> serverResponsePayload = generateResponse(clientQueryPayload, null);
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public void setPublicKey(List<byte[]> clientPublicKeysPayload) {
        // empty
    }

    @Override
    public List<byte[][]> serverSetup(NaiveDatabase database) {
        int maxPartitionBitLength = 0, rows = 0, d = 0;
        int upperBound = CommonUtils.getUnitNum(database.getL(), params.logP - 1);
        for (int count = 1; count < upperBound + 1; count++) {
            maxPartitionBitLength = CommonUtils.getUnitNum(database.getL(), count);
            int maxPartitionByteLength = CommonUtils.getByteLength(maxPartitionBitLength);
            d = CommonUtils.getUnitNum(maxPartitionByteLength * Byte.SIZE, params.logP - 1);
            if ((BigInteger.valueOf(d).multiply(BigInteger.valueOf(database.rows())))
                .compareTo(INT_MAX_VALUE.shiftRight(1)) < 0) {
                int[] dims = PirUtils.approxSquareDatabaseDims(database.rows(), d);
                rows = dims[0];
                cols = dims[1];
                params.setPlainModulo(PirUtils.getBitLength(cols));
                break;
            }
        }
        partitionBitLength = Math.min(maxPartitionBitLength, database.getL());
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        databases = database.partitionZl(partitionBitLength);
        partitionSize = databases.length;
        // public matrix A
        seed = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed);
        Zl64Matrix a = Zl64Matrix.createRandom(params.zl64, cols, params.n, seed);
        // generate the client's hint, which is the database multiplied by A. Also known as the setup.
        db = new Zl64Matrix[partitionSize];
        for (int i = 0; i < partitionSize; i++) {
            db[i] = Zl64Matrix.createZeros(params.zl64, rows, cols);
            db[i].setParallel(parallel);
        }
        hint = new Zl64Matrix[partitionSize];
        int rowElementsNum = rows / d;
        for (int i = 0; i < partitionSize; i++) {
            for (int j = 0; j < cols; j++) {
                for (int l = 0; l < rowElementsNum; l++) {
                    if (j * rowElementsNum + l < num) {
                        byte[] element = databases[i].getBytesData(j * rowElementsNum + l);
                        long[] coeffs = PirUtils.convertBytesToCoeffs(params.logP - 1, 0, element.length, element);
                        // values mod the plaintext modulus p
                        for (int k = 0; k < d; k++) {
                            db[i].set(l * d + k, j, coeffs[k]);
                        }
                    }
                }
            }
            hint[i] = db[i].matrixMul(a);
        }
        return null;
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery, List<byte[][]> empty) throws MpcAbortException {
        long[] queryElements = LongUtils.byteArrayToLongArray(clientQuery.get(0));
        MpcAbortPreconditions.checkArgument(queryElements.length == cols);
        Zl64Vector query = Zl64Vector.create(params.zl64, queryElements);
        return IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(db[i].matrixMulVector(query).getElements()))
            .collect(Collectors.toList());
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        return generateResponse(clientQuery, null);
    }

    @Override
    public void setDefaultParams() {
        params = Hhcm23SimpleSingleIndexPirParams.DEFAULT_PARAMS;
    }

    @Override
    public int getQuerySize() {
        return 1;
    }
}
