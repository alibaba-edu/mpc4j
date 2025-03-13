package edu.alibaba.femur.service.server;

import edu.alibaba.work.femur.demo.seal.SealFemurDemoRedisPirConfig;
import edu.alibaba.work.femur.demo.seal.SealFemurDemoRedisPirServer;
import gnu.trove.map.TLongObjectMap;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Femur SEAL Redis PIR server boot.
 *
 * @author Weiran Liu
 * @date 2024/12/10
 */
public class FemurSealRedisPirServerBoot implements FemurPirServerBoot {
    private static final Logger LOGGER = LoggerFactory.getLogger(FemurSealRedisPirServerBoot.class);
    /**
     * time unit
     */
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
    /**
     * GRPC server host
     */
    private final String host;
    /**
     * GRPC server port
     */
    private final int port;
    /**
     * config
     */
    private final SealFemurDemoRedisPirConfig config;
    /**
     * executor
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * demo PIR server, must be static to make is as a global instance
     */
    private static SealFemurDemoRedisPirServer demoPirServer;
    /**
     * GRPC server, must be static to make is as a global instance
     */
    private static Server grpcServer;
    /**
     * PIR server proxy
     */
    private static FemurPirServerProxy serverProxy;

    public static FemurSealRedisPirServerBoot of(String host, int port, SealFemurDemoRedisPirConfig config) {
        return new FemurSealRedisPirServerBoot(host, port, config);
    }

    private FemurSealRedisPirServerBoot(String host, int port, SealFemurDemoRedisPirConfig config) {
        this.host = host;
        this.port = port;
        this.config = config;
    }

    @Override
    public void start() {
        if (grpcServer == null) {
            CountDownLatch latch = new CountDownLatch(1);
            executor.submit(() -> {
                try {
                    // server
                    demoPirServer = new SealFemurDemoRedisPirServer(config);
                    serverProxy = new FemurPirServerProxy(demoPirServer);
                    grpcServer = ServerBuilder.forPort(port)
                        .executor(Executors.newFixedThreadPool(ForkJoinPool.getCommonPoolParallelism()))
                        .addService(serverProxy)
                        .build()
                        .start();
                    LOGGER.info("Server started on {}:{}", host, port);
                    // Signal that the server has started
                    latch.countDown();
                    grpcServer.awaitTermination();
                } catch (Exception e) {
                    LOGGER.error("Error starting the server", e);
                }
            });

            try {
                // Wait for the signal
                latch.await();
            } catch (InterruptedException e) {
                // Restore interrupted status
                Thread.currentThread().interrupt();
                throw new RuntimeException("Server startup interrupted", e);
            }
        }
    }

    @Override
    public void stop() {
        demoPirServer.reset();
        executor.shutdown();
    }

    @Override
    public void init(int n, int l) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        demoPirServer.init(n, l);
        stopWatch.stop();
        LOGGER.info("{}\t{}\t{}\t{}\t{}", "   INIT", "SERVER_LOCAL_DO", "-", "-", stopWatch.getTime(TIME_UNIT));
    }

    @Override
    public void setDatabase(TLongObjectMap<byte[]> keyValueDatabase) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        demoPirServer.setDatabase(keyValueDatabase);
        stopWatch.stop();
        LOGGER.info("{}\t{}\t{}\t{}\t{}", "  SET_DB", "SERVER_LOCAL_DO", "-", "-", stopWatch.getTime(TIME_UNIT));
    }

    @Override
    public void updateValue(Long key, byte[] value) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        demoPirServer.updateValue(key, value);
        stopWatch.stop();
        LOGGER.info("{}\t{}\t{}\t{}\t{}", "UPD_VAL", "SERVER_LOCAL_DO", "-", "-", stopWatch.getTime(TIME_UNIT));
    }

    @Override
    public void reset() {
        demoPirServer.reset();
    }

    @Override
    public FemurPirServerProxy getPirServerProxy() {
        return serverProxy;
    }
}
