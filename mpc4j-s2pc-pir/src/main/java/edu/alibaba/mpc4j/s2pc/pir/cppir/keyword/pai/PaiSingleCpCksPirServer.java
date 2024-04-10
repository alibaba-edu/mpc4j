package edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteEccFactory;
import edu.alibaba.mpc4j.common.tool.crypto.ecc.ByteFullEcc;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfFactory;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfKey;
import edu.alibaba.mpc4j.s2pc.opf.sqoprf.SqOprfSender;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.AbstractSingleCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.SingleCpKsPirServerOutput;
import edu.alibaba.mpc4j.s2pc.pir.cppir.keyword.pai.PaiSingleCpCksPirDesc.PtoStep;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Pai client-specific preprocessing CKSPIR server.
 *
 * @author Liqiang Peng
 * @date 2023/9/20
 */
public class PaiSingleCpCksPirServer<T> extends AbstractSingleCpKsPirServer<T> {
    /**
     * sq-OPRF sender
     */
    private final SqOprfSender sqOprfSender;
    /**
     * byte full ecc
     */
    private final ByteFullEcc byteFullEcc;
    /**
     * PRG used for encrypt concat value
     */
    private Prg prg;
    /**
     * sq-OPRF key
     */
    private SqOprfKey sqOprfKey;
    /**
     * row num
     */
    private int rowNum;
    /**
     * column num
     */
    private int columnNum;
    /**
     * final database
     */
    private Map<ByteBuffer, byte[]> finalDatabase;

    public PaiSingleCpCksPirServer(Rpc serverRpc, Party clientParty, PaiSingleCpCksPirConfig config) {
        super(PaiSingleCpCksPirDesc.getInstance(), serverRpc, clientParty, config);
        sqOprfSender = SqOprfFactory.createSender(serverRpc, clientParty, config.getSqOprfConfig());
        addSubPto(sqOprfSender);
        byteFullEcc = ByteEccFactory.createFullInstance(envType);
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int labelBitLength) throws MpcAbortException {
        setInitInput(keyValueMap, labelBitLength);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        prg = PrgFactory.createInstance(envType, byteL);
        sqOprfKey = sqOprfSender.keyGen();
        sqOprfSender.init(1, sqOprfKey);
        stopWatch.stop();
        long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 4, sqOprfTime, "Server inits sq-OPRF");

        stopWatch.start();
        rowNum = PaiSingleCpCksPirUtils.getRowNum(n);
        columnNum = PaiSingleCpCksPirUtils.getColumnNum(n);
        assert rowNum * columnNum >= n
            : "rowNum * columnNum must be greater than or equal to n (" + n + "): " + rowNum * columnNum;
        // pad the database
        ArrayList<T> keyArrayList = new ArrayList<>(keyValueMap.keySet());
        // for the security proof reason, we need to shuffle the inputs
        Collections.shuffle(keyArrayList, secureRandom);
        byte[][] initHashes = new byte[rowNum * columnNum][byteFullEcc.pointByteLength()];
        byte[][] initValues = new byte[rowNum * columnNum][byteL];
        IntStream indexIntStream = IntStream.range(0, rowNum * columnNum);
        indexIntStream = parallel ? indexIntStream.parallel() : indexIntStream;
        indexIntStream.forEach(index -> {
            if (index < n) {
                T x = keyArrayList.get(index);
                byte[] xBytes = ObjectUtils.objectToByteArray(x);
                initHashes[index] = byteFullEcc.hashToCurve(ObjectUtils.objectToByteArray(x));
                byte[] key = prg.extendToBytes(sqOprfKey.getPrf(xBytes));
                initValues[index] = BytesUtils.xor(keyValueMap.get(x), key);
            } else {
                initHashes[index] = byteFullEcc.randomPoint(secureRandom);
                initValues[index] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        });
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 2, 4, paddingTime,
            String.format(
                "Server sets params: n = %d, rowNum = %d, colNum = %d, n (pad) = %d",
                n, rowNum, columnNum, rowNum * columnNum
            )
        );

        preprocessing(initHashes, initValues);

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing(byte[][] initKeys, byte[][] initValues) throws MpcAbortException {
        stopWatch.start();

        // init the final database
        finalDatabase = new HashMap<>(rowNum * columnNum);
        // init a template key matrix and a value matrix
        byte[][][] medKeyMatrix = new byte[rowNum][columnNum][byteFullEcc.pointByteLength()];
        byte[][][] medValueMatrix = new byte[rowNum][columnNum][CommonConstants.BLOCK_BYTE_LENGTH + byteL];
        // stream handling rows
        for (int iRow = 0; iRow < rowNum; iRow++) {
            int iFinalRow = iRow;
            // here we must use same β, otherwise server cannot de-blind since client returns the shuffled med key
            BigInteger beta = byteFullEcc.randomZn(secureRandom);
            BigInteger inverseBeta = BigIntegerUtils.modInverse(beta, byteFullEcc.getN());
            byte[][] blindKeys = new byte[columnNum][];
            IntStream columnIndexIntStream = IntStream.range(0, columnNum);
            columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
            columnIndexIntStream.forEach(iColumn ->
                blindKeys[iColumn] = byteFullEcc.mul(initKeys[iFinalRow * columnNum + iColumn], beta)
            );
            // concatenate each column into a whole column
            ByteBuffer rowByteBuffer = ByteBuffer.allocate((byteFullEcc.pointByteLength() + byteL) * columnNum);
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                rowByteBuffer.put(blindKeys[iColumn]);
                rowByteBuffer.put(initValues[iFinalRow * columnNum + iColumn]);
            }
            List<byte[]> rowRequestPayload = Collections.singletonList(rowByteBuffer.array());
            DataPacketHeader rowRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(rowRequestHeader, rowRequestPayload));

            // receive response
            DataPacketHeader medResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> medResponsePayload = rpc.receive(medResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(medResponsePayload.size() == 1);
            byte[] medDataByteArray = medResponsePayload.get(0);
            // each med contains encrypted key + (random IV + encrypted value)
            MpcAbortPreconditions.checkArgument(
                medDataByteArray.length == (byteFullEcc.pointByteLength() + CommonConstants.BLOCK_BYTE_LENGTH + byteL) * columnNum
            );
            // split the stream database
            ByteBuffer medByteBuffer = ByteBuffer.wrap(medDataByteArray);
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                // read encrypted key
                medByteBuffer.get(medKeyMatrix[iRow][iColumn]);
                // read encrypted value
                medByteBuffer.get(medValueMatrix[iRow][iColumn]);
            }
            // de-blind
            columnIndexIntStream = IntStream.range(0, columnNum);
            columnIndexIntStream = parallel ? columnIndexIntStream.parallel() : columnIndexIntStream;
            columnIndexIntStream.forEach(iColumn ->
                medKeyMatrix[iFinalRow][iColumn] = byteFullEcc.mul(medKeyMatrix[iFinalRow][iColumn], inverseBeta)
            );
            extraInfo++;
        }
        stopWatch.stop();
        long streamRowTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 4, streamRowTime, "Server handles " + rowNum + " rows");

        stopWatch.start();
        // stream handling columns
        for (int iColumn = 0; iColumn < columnNum; iColumn++) {
            // concatenate each row into a whole row
            ByteBuffer columnByteBuffer = ByteBuffer.allocate((byteFullEcc.pointByteLength() + CommonConstants.BLOCK_BYTE_LENGTH + byteL) * rowNum);
            for (int iRow = 0; iRow < rowNum; iRow++) {
                columnByteBuffer.put(medKeyMatrix[iRow][iColumn]);
                columnByteBuffer.put(medValueMatrix[iRow][iColumn]);
            }
            List<byte[]> columnRequestPayload = Collections.singletonList(columnByteBuffer.array());
            DataPacketHeader columnRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(columnRequestHeader, columnRequestPayload));

            // receive response
            DataPacketHeader finalResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> finalResponsePayload = rpc.receive(finalResponseHeader).getPayload();
            MpcAbortPreconditions.checkArgument(finalResponsePayload.size() == 1);
            byte[] finalDataByteArray = finalResponsePayload.get(0);
            // each final contains encrypted key + (random IV + encrypted value)
            MpcAbortPreconditions.checkArgument(
                finalDataByteArray.length == (byteFullEcc.pointByteLength() + CommonConstants.BLOCK_BYTE_LENGTH + byteL) * rowNum
            );
            ByteBuffer finalByteBuffer = ByteBuffer.wrap(finalDataByteArray);
            for (int iRow = 0; iRow < rowNum; iRow++) {
                // final key
                byte[] finalKey = new byte[byteFullEcc.pointByteLength()];
                finalByteBuffer.get(finalKey);
                // final value
                byte[] finalValue = new byte[CommonConstants.BLOCK_BYTE_LENGTH + byteL];
                finalByteBuffer.get(finalValue);
                ByteBuffer finalKeyByteBuffer = ByteBuffer.wrap(finalKey);
                assert !finalDatabase.containsKey(finalKeyByteBuffer);
                finalDatabase.put(finalKeyByteBuffer, finalValue);
            }
            extraInfo++;
        }
        stopWatch.stop();
        long streamColumnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 4, 4, streamColumnTime, "Server handles " + columnNum + " columns");
    }

    @Override
    public SingleCpKsPirServerOutput pir() throws MpcAbortException {
        setPtoInput();

        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryRequestPayload = rpc.receive(queryRequestHeader).getPayload();
        int queryRequestSize = queryRequestPayload.size();
        MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 1);

        if (queryRequestSize == 0) {
            // response empty query
            responseEmptyQuery();
            return SingleCpKsPirServerOutput.UNKNOWN;
        } else {
            // response actual query
            return respondActualQuery(queryRequestPayload);
        }
    }

    private void responseEmptyQuery() {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, new LinkedList<>()));
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses empty query");
    }

    private SingleCpKsPirServerOutput respondActualQuery(List<byte[]> queryRequestPayload) throws MpcAbortException {
        byte[] key = queryRequestPayload.get(0);
        // response the value
        ByteBuffer keyByteBuffer = ByteBuffer.wrap(key);
        if (finalDatabase.containsKey(keyByteBuffer)) {
            logPhaseInfo(PtoState.PTO_BEGIN);
            // server contains the key
            stopWatch.start();
            DataPacketHeader queryResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            byte[] value = finalDatabase.get(keyByteBuffer);
            List<byte[]> queryResponsePayload = Collections.singletonList(value);
            rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, queryResponsePayload));
            stopWatch.stop();
            long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 2, responseTime, "Server responses value");

            stopWatch.start();
            sqOprfSender.oprf(1);
            stopWatch.stop();
            long sqOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 2, 2, sqOprfTime, "Server runs sq-OPRF");

            logPhaseInfo(PtoState.PTO_END);
            return SingleCpKsPirServerOutput.IN;
        } else {
            logPhaseInfo(PtoState.PTO_BEGIN);
            // server does not contain the key
            stopWatch.start();
            DataPacketHeader queryResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(queryResponseHeader, new LinkedList<>()));
            stopWatch.stop();
            long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses ⊥");

            logPhaseInfo(PtoState.PTO_END);
            return SingleCpKsPirServerOutput.OUT;
        }
    }
}