package edu.alibaba.femur.service.client;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import edu.alibaba.work.femur.demo.FemurDemoPirClient;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurDemoPirFactory;
import edu.alibaba.work.femur.demo.FemurStatus;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.alibaba.work.femur.service.api.FemurServicePirServerGrpc.FemurServicePirServerBlockingStub;
import static edu.alibaba.work.femur.service.api.FemurServicePirServerGrpc.newBlockingStub;
import static edu.alibaba.work.femur.service.api.FemurServicePirServerOuterClass.*;


/**
 * Femur service PIR client.
 *
 * @author Weiran Liu
 * @date 2024/12/10
 */
public class FemurPirClient {
    /**
     * GRPC server host
     */
    private final String host;
    /**
     * GRPC server port
     */
    private final int port;
    /**
     * channel
     */
    private ManagedChannel channel;
    /**
     * stub
     */
    private FemurServicePirServerBlockingStub stub;
    /**
     * client ID
     */
    private final String clientId;
    /**
     * client
     */
    private final FemurDemoPirClient client;
    /**
     * generate query time
     */
    private long genQueryTime;
    /**
     * decode time
     */
    private long decodeTime;
    /**
     * stopwatch
     */
    StopWatch stopWatch;

    public FemurPirClient(String host, int port, String clientId, FemurDemoPirConfig config) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        client = FemurDemoPirFactory.createClient(config);
        stopWatch = new StopWatch();
        genQueryTime = 0;
        decodeTime = 0;
    }

    public void setUp() {
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            // see https://stackoverflow.com/questions/75211164/grpc-message-exceeds-maximum-size-4194304-5145024-java
            .maxInboundMessageSize(Integer.MAX_VALUE)
            .build();
        stub = newBlockingStub(channel);
    }

    public void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            channel = null;
        }
        if (stub != null) {
            stub = null;
        }
    }

    public FemurStatus register() {
        List<byte[]> registerRequestPayload = client.register(clientId);
        List<ByteString> registerRequestPayloadList = registerRequestPayload
            .stream()
            .map(ByteString::copyFrom)
            .toList();
        RegisterRequest request = RegisterRequest.newBuilder()
            .addAllRegisterRequestPayload(registerRequestPayloadList)
            .build();
        RegisterResponse response = stub.register(request);
        FemurStatus femurStatus = FemurStatus.values()[response.getCode().getNumber()];
        switch (femurStatus) {
            case SERVER_NOT_INIT, SERVER_NOT_KVDB -> {
                return femurStatus;
            }
            case SERVER_SUCC_RES -> {
                List<byte[]> responsePayload = response.getRegisterResponsePayloadList()
                    .stream()
                    .map(ByteString::toByteArray)
                    .collect(Collectors.toList());
                client.setDatabaseParams(responsePayload);
                return femurStatus;
            }
            default -> throw new RuntimeException("Illegal state: " + femurStatus.name());
        }
    }

    public FemurStatus getHint() {
        Empty request = Empty.newBuilder().build();
        HintResponse response = stub.getHint(request);
        FemurStatus femurStatus = FemurStatus.values()[response.getCode().getNumber()];
        switch (femurStatus) {
            case SERVER_NOT_INIT, SERVER_NOT_KVDB -> {
                return femurStatus;
            }
            case SERVER_SUCC_RES -> {
                List<byte[]> responsePayload = response.getHintsResponsePayloadList()
                    .stream()
                    .map(ByteString::toByteArray)
                    .collect(Collectors.toList());
                client.setHint(responsePayload);
                return femurStatus;
            }
            default -> throw new RuntimeException("Illegal state: " + femurStatus.name());
        }
    }

    public Pair<FemurStatus, byte[]> query(long key, int t, double epsilon) {
        stopWatch.start();
        List<byte[]> queryRequestPayload = client.query(key, t, epsilon);
        stopWatch.stop();
        genQueryTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        List<ByteString> queryRequestPayloadList = queryRequestPayload
            .stream()
            .map(ByteString::copyFrom)
            .toList();
        QueryRequest request = QueryRequest.newBuilder()
            .addAllQueryRequestPayload(queryRequestPayloadList)
            .build();
        QueryResponse response = stub.query(request);
        FemurStatus queryStatus = FemurStatus.values()[response.getCode().getNumber()];
        List<byte[]> queryResponsePayload = response.getQueryResponsePayloadList()
            .stream()
            .map(byteString -> byteString.isEmpty() ? null : byteString.toByteArray())
            .collect(Collectors.toList());
        if (queryStatus.equals(FemurStatus.HINT_V_MISMATCH)) {
            client.setHint(queryResponsePayload);
        }
        stopWatch.start();
        Pair<FemurStatus, byte[]> result = client.retrieve(Pair.of(queryStatus, queryResponsePayload));
        stopWatch.stop();
        decodeTime += stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        return result;
    }

    public long getDecodeTime() {
        return decodeTime;
    }

    public long getGenQueryTime() {
        return genQueryTime;
    }
}
