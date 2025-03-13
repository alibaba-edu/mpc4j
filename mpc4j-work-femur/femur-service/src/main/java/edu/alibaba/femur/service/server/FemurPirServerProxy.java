package edu.alibaba.femur.service.server;

import com.google.protobuf.ByteString;
import edu.alibaba.work.femur.demo.FemurDemoPirServer;
import edu.alibaba.work.femur.demo.FemurStatus;
import edu.alibaba.work.femur.service.api.FemurServicePirServerOuterClass;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.alibaba.work.femur.service.api.FemurServicePirServerGrpc.FemurServicePirServerImplBase;
import static edu.alibaba.work.femur.service.api.FemurServicePirServerOuterClass.*;

/**
 * Femur PIR server proxy.
 *
 * @author Weiran Liu
 * @date 2024/12/10
 */
public class FemurPirServerProxy extends FemurServicePirServerImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(FemurPirServerProxy.class);
    /**
     * time unit
     */
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    /**
     * demo PIR server
     */
    private final FemurDemoPirServer demoPirServer;
    /**
     * register request size
     */
    private int registerRequestSize;
    /**
     * register response size
     */
    private int registerResponseSize;
    /**
     * register time
     */
    private long registerTime;
    /**
     * get hint request size
     */
    private int getHintRequestSize;
    /**
     * get hint response size
     */
    private int getHintResponseSize;
    /**
     * get hint time
     */
    private long getHintTime;
    /**
     * query request size
     */
    private int queryRequestSize;
    /**
     * query response size
     */
    private int queryResponseSize;
    /**
     * query time
     */
    private long queryTime;

    public FemurPirServerProxy(FemurDemoPirServer demoPirServer) {
        this.demoPirServer = demoPirServer;
        LOGGER.info("TYPE\tSTATUS\tREQ_SIZE(B)\tRES_SIZE(B)\tTIME(ms)");
        registerRequestSize = 0;
        registerResponseSize = 0;
        registerTime = 0;
        getHintRequestSize = 0;
        getHintResponseSize = 0;
        getHintTime = 0;
        queryRequestSize = 0;
        queryResponseSize = 0;
        queryTime = 0;
    }

    @Override
    public void register(
        RegisterRequest request,
        io.grpc.stub.StreamObserver<RegisterResponse> responseObserver) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // handle request
        List<ByteString> registerRequestPayloadList = request.getRegisterRequestPayloadList();
        List<byte[]> registerRequestPayload = registerRequestPayloadList
            .stream()
            .map(ByteString::toByteArray)
            .collect(Collectors.toList());
        // generate response
        Pair<FemurStatus, List<byte[]>> registerResponse = demoPirServer.register(registerRequestPayload);
        List<ByteString> registerResponsePayloadList = registerResponse.getRight()
            .stream()
            .map(ByteString::copyFrom)
            .collect(Collectors.toList());
        RegisterResponse response = RegisterResponse
            .newBuilder()
            .setCode(FemurServicePirServerOuterClass.FemurStatus.valueOf(registerResponse.getLeft().name()))
            .addAllRegisterResponsePayload(registerResponsePayloadList)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        stopWatch.stop();
        int requestSize = request.getSerializedSize();
        int responseSize = response.getSerializedSize();
        long time = stopWatch.getTime(TIME_UNIT);
        printLogger("REGISTER", registerResponse.getLeft().toString(), requestSize, responseSize, time);
        registerRequestSize += requestSize;
        registerResponseSize += responseSize;
        registerTime += time;
    }

    @Override
    public void getHint(
        com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<HintResponse> responseObserver) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // handle request
        Pair<FemurStatus, List<byte[]>> hintsResponse = demoPirServer.getHint();
        List<ByteString> hintsResponsePayloadList = hintsResponse.getRight()
            .stream()
            .map(ByteString::copyFrom)
            .collect(Collectors.toList());
        // generate response
        HintResponse response = HintResponse
            .newBuilder()
            .setCode(FemurServicePirServerOuterClass.FemurStatus.valueOf(hintsResponse.getLeft().name()))
            .addAllHintsResponsePayload(hintsResponsePayloadList)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
        stopWatch.stop();
        int requestSize = request.getSerializedSize();
        int responseSize = response.getSerializedSize();
        long time = stopWatch.getTime(TIME_UNIT);
        printLogger("GET_HINT", hintsResponse.getLeft().toString(), requestSize, responseSize, time);
        getHintRequestSize += requestSize;
        getHintResponseSize += responseSize;
        getHintTime += time;
    }

    @Override
    public void query(
        QueryRequest request,
        io.grpc.stub.StreamObserver<QueryResponse> responseObserver) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // handle request
        List<byte[]> queryRequestPayload = request.getQueryRequestPayloadList()
            .stream()
            .map(ByteString::toByteArray)
            .toList();
        Pair<FemurStatus, List<byte[]>> queryResponse = demoPirServer.response(queryRequestPayload);
        if (queryResponse.getLeft().equals(FemurStatus.HINT_V_MISMATCH)) {
            Pair<FemurStatus, List<byte[]>> hintResponse = demoPirServer.getHint();
            if (hintResponse.getLeft().equals(FemurStatus.SERVER_SUCC_RES)) {
                queryResponse = Pair.of(FemurStatus.HINT_V_MISMATCH, hintResponse.getRight());
            }
        }
        List<byte[]> queryResponsePayload = queryResponse.getRight();
        // generate response
        List<ByteString> queryResponsePayloadList = queryResponsePayload.stream()
            .map(ByteString::copyFrom)
            .collect(Collectors.toList());
        QueryResponse response = QueryResponse
            .newBuilder()
            .setCode(FemurServicePirServerOuterClass.FemurStatus.valueOf(queryResponse.getLeft().name()))
            .addAllQueryResponsePayload(queryResponsePayloadList)
            .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        stopWatch.stop();
        int requestSize = request.getSerializedSize();
        int responseSize = response.getSerializedSize();
        long time = stopWatch.getTime(TIME_UNIT);
        printLogger("  QUERY", queryResponse.getLeft().toString(), requestSize, responseSize, time);
        queryRequestSize += requestSize;
        queryResponseSize += responseSize;
        queryTime += time;
    }

    private void printLogger(String type, String status, int requestSize, int responseSize, long time) {
        LOGGER.info("{}\t{}\t{}\t{}\t{}", type, status, requestSize, responseSize, time);
    }

    public int getRegisterRequestSize() {
        return registerRequestSize;
    }

    public int getRegisterResponseSize() {
        return registerResponseSize;
    }

    public long getRegisterTime() {
        return registerTime;
    }

    public int getGetHintRequestSize() {
        return getHintRequestSize;
    }

    public int getGetHintResponseSize() {
        return getHintResponseSize;
    }

    public long getGetHintTime() {
        return getHintTime;
    }

    public int getQueryRequestSize() {
        return queryRequestSize;
    }

    public int getQueryResponseSize() {
        return queryResponseSize;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public void reset() {
        registerRequestSize = 0;
        registerResponseSize = 0;
        registerTime = 0;
        getHintRequestSize = 0;
        getHintResponseSize = 0;
        getHintTime = 0;
        queryRequestSize = 0;
        queryResponseSize = 0;
        queryTime = 0;
    }
}
