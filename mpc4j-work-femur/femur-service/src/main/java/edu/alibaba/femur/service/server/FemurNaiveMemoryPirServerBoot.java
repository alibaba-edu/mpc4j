package edu.alibaba.femur.service.server;

import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoMemoryPirConfig;
import edu.alibaba.work.femur.demo.naive.NaiveFemurDemoMemoryPirServer;
import gnu.trove.map.TLongObjectMap;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/**
 * Femur naive memory PIR server boot.
 *
 * @author Weiran Liu
 * @date 2024/12/10
 */
public class FemurNaiveMemoryPirServerBoot implements FemurPirServerBoot {
    private static final Logger LOGGER = LoggerFactory.getLogger(FemurNaiveMemoryPirServerBoot.class);
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
    private final NaiveFemurDemoMemoryPirConfig config;
    /**
     * executor
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * demo PIR server, must be static to make is as a global instance
     */
    private static NaiveFemurDemoMemoryPirServer demoPirServer;
    /**
     * GRPC server, must be static to make is as a global instance
     */
    private static Server grpcServer;
    /**
     * PIR server proxy
     */
    private static FemurPirServerProxy serverProxy;

    public static FemurNaiveMemoryPirServerBoot of(String host, int port, NaiveFemurDemoMemoryPirConfig config) {
        return new FemurNaiveMemoryPirServerBoot(host, port, config);
    }

    private FemurNaiveMemoryPirServerBoot(String host, int port, NaiveFemurDemoMemoryPirConfig config) {
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
                    demoPirServer = new NaiveFemurDemoMemoryPirServer(config);
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
        demoPirServer.init(n, l);
    }

    @Override
    public void setDatabase(TLongObjectMap<byte[]> keyValueDatabase) {
        demoPirServer.setDatabase(keyValueDatabase);
    }

    @Override
    public void updateValue(Long key, byte[] value) {
        demoPirServer.updateValue(key, value);
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
