package edu.alibaba.mpc4j.s2pc.pir.index.batch.naive;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.s2pc.pir.index.batch.AbstractBatchIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.batch.naive.NaiveBatchIndexPirPtoDesc.*;

/**
 * Naive Batch PIR client.
 *
 * @author Liqiang Peng
 * @date 2023/7/14
 */
public class NaiveBatchIndexPirClient extends AbstractBatchIndexPirClient {

    /**
     * single index PIR client
     */
    private final SingleIndexPirClient client;

    public NaiveBatchIndexPirClient(Rpc clientRpc, Party serverParty, NaiveBatchIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        client = SingleIndexPirFactory.createClient(clientRpc, serverParty, config.getSingleIndexPirConfig());
        addSubPto(client);
    }

    @Override
    public void init(int serverElementSize, int elementBitLength, int maxRetrievalSize) throws MpcAbortException {
        logPhaseInfo(PtoState.INIT_BEGIN);
        setInitInput(serverElementSize, elementBitLength, maxRetrievalSize);

        stopWatch.start();
        // client init single index PIR client
        client.init(serverElementSize, elementBitLength);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Map<Integer, byte[]> pir(List<Integer> indexList) throws MpcAbortException {
        setPtoInput(indexList);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // generate queries
        IntStream queryStream = IntStream.range(0, retrievalSize);
        queryStream = parallel ? queryStream.parallel() : queryStream;
        List<byte[]> clientQueryPayload = queryStream
            .mapToObj(i -> client.generateQuery(indexList.get(i)))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
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
        MpcAbortPreconditions.checkArgument(serverResponsePayload.size() % retrievalSize == 0);

        stopWatch.start();
        int count = serverResponsePayload.size() / retrievalSize;
        Map<Integer, byte[]> result = new ConcurrentHashMap<>(retrievalSize);
        IntStream intStream = IntStream.range(0, retrievalSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(i -> {
            byte[] item;
            try {
                item = client.decodeResponse(
                    serverResponsePayload.subList(i * count, (i + 1) * count), indexList.get(i)
                );
                result.put(indexList.get(i), item);
            } catch (MpcAbortException e) {
                e.printStackTrace();
            }
        });
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return result;
    }
}