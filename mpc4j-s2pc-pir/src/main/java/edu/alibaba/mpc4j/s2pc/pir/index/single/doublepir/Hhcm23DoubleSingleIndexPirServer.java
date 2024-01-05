package edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.pir.index.single.doublepir.Hhcm23DoubleSingleIndexPirPtoDesc.getInstance;

/**
 * Double PIR server.
 *
 * @author Liqiang Peng
 * @date 2023/5/30
 */
public class Hhcm23DoubleSingleIndexPirServer extends AbstractSingleIndexPirServer {

    /**
     * Double PIR params
     */
    private Hhcm23DoubleSingleIndexPirParams params;
    /**
     * hint_s
     */
    private Zl64Matrix[] hintS;
    /**
     * hint_c
     */
    private Zl64Matrix[] hintC;
    /**
     * database
     */
    private Zl64Matrix[] db;
    /**
     * random seed
     */
    private byte[][] seed;
    /**
     * cols
     */
    private int cols;
    /**
     * public matrix A1
     */
    private Zl64Matrix a2;

    public Hhcm23DoubleSingleIndexPirServer(Rpc serverRpc, Party clientParty, Hhcm23DoubleSingleIndexPirConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        setInitInput(database);
        assert indexPirParams instanceof Hhcm23DoubleSingleIndexPirParams;
        params = (Hhcm23DoubleSingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        serverSetup(database);
        DataPacketHeader seedPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_SEED.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Arrays.asList(seed)));
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hintC[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        hintC = null;
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
        rpc.send(DataPacket.fromByteArrayList(seedPayloadHeader, Arrays.asList(seed)));
        List<byte[]> hintPayload = IntStream.range(0, partitionSize)
            .mapToObj(i -> LongUtils.longArrayToByteArray(hintC[i].elements))
            .collect(Collectors.toList());
        DataPacketHeader hintPayloadHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HINT.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(hintPayloadHeader, hintPayload));
        hintC = null;
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
        int[] dims = PirUtils.approxSquareDatabaseDims(database.rows(), 1);
        int rows = dims[0];
        cols = dims[1];
        params.setPlainModulo(PirUtils.getBitLength(cols));
        partitionBitLength = Math.min(params.logP - 1, database.getL());
        partitionByteLength = CommonUtils.getByteLength(partitionBitLength);
        databases = database.partitionZl(partitionBitLength);
        partitionSize = databases.length;
        // public matrix A
        seed = new byte[2][CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(seed[0]);
        secureRandom.nextBytes(seed[1]);
        Zl64Matrix a1 = Zl64Matrix.createRandom(params.zl64, cols, params.n, seed[0]);
        a2 = Zl64Matrix.createRandom(params.zl64, rows, params.n, seed[1]);
        a1.setParallel(parallel);
        a2.setParallel(parallel);
        // generate the client's hint, which is the database multiplied by A. Also known as the setup.
        db = new Zl64Matrix[partitionSize];
        hintS = new Zl64Matrix[partitionSize];
        hintC = new Zl64Matrix[partitionSize];
        for (int i = 0; i < partitionSize; i++) {
            db[i] = Zl64Matrix.createZeros(params.zl64, rows, cols);
            db[i].setParallel(parallel);
        }
        for (int i = 0; i < partitionSize; i++) {
            for (int j = 0; j < rows; j++) {
                for (int l = 0; l < cols; l++) {
                    if (j * rows + l < num) {
                        byte[] element = databases[i].getBytesData(j * rows + l);
                        long[] coeffs = PirUtils.convertBytesToCoeffs(
                            partitionByteLength * Byte.SIZE, 0, element.length, element
                        );
                        assert coeffs.length == 1;
                        // values mod the plaintext modulus p
                        db[i].set(j, l, coeffs[0]);
                    }
                }
            }
            // hint_s = A transposed * db transposed
            Zl64Matrix a1Trans = a1.transpose();
            a1Trans.setParallel(parallel);
            Zl64Matrix s = a1Trans.matrixMul(db[i].transpose());
            // decomposed(hint_s);
            hintS[i] = s.decompose(params.p);
            hintS[i].setParallel(parallel);
            // hint_c = hint_s * A_2
            hintC[i] = hintS[i].matrixMul(a2);
        }
        return null;
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery, List<byte[][]> empty) {
        long[] c1Elements = LongUtils.byteArrayToLongArray(clientQuery.get(0));
        Zl64Matrix c1 = Zl64Matrix.create(params.zl64, c1Elements, 1, cols);
        c1.setParallel(parallel);
        long[] c2Elements = LongUtils.byteArrayToLongArray(clientQuery.get(1));
        Zl64Vector c2 = Zl64Vector.create(params.zl64, c2Elements);
        List<byte[]> response = new ArrayList<>();
        for (int i = 0; i < partitionSize; i++) {
            Zl64Matrix t = c1.matrixMul(db[i].transpose());
            Zl64Matrix answer1 = t.decompose(params.p);
            answer1.setParallel(parallel);
            Zl64Matrix h = answer1.matrixMul(a2);
            Zl64Matrix combine = hintS[i].concat(answer1);
            combine.setParallel(parallel);
            Zl64Vector ans = combine.matrixMulVector(c2);
            response.add(LongUtils.longArrayToByteArray(h.elements));
            response.add(LongUtils.longArrayToByteArray(ans.getElements()));
        }
        return response;
    }

    @Override
    public List<byte[]> generateResponse(List<byte[]> clientQuery) throws MpcAbortException {
        return generateResponse(clientQuery, null);
    }

    @Override
    public void setDefaultParams() {
        params = Hhcm23DoubleSingleIndexPirParams.DEFAULT_PARAMS;
    }

    @Override
    public int getQuerySize() {
        return 2;
    }
}
