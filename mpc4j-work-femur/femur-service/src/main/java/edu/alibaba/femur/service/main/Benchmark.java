package edu.alibaba.femur.service.main;

import edu.alibaba.femur.service.client.FemurPirClient;
import edu.alibaba.femur.service.server.FemurPirServerBoot;
import edu.alibaba.femur.service.server.FemurPirServerBootFactory;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.work.femur.demo.FemurDemoPirConfig;
import edu.alibaba.work.femur.demo.FemurStatus;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.femur.service.main.FemurPirMain.generateBytesInputFiles;
import static edu.alibaba.femur.service.main.FemurPirMain.readServerElementArray;

public class Benchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(Benchmark.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME_KEY = "femur_pir_pto_name";
    /**
     * type name
     */
    public static final String PTO_TYPE_NAME = "BENCHMARK";
    /**
     * entry bit length
     */
    private static int entryBitLength;
    /**
     * server set sizes
     */
    private static int[] serverSetSizes;
    /**
     * query num
     */
    private static int queryNum;
    /**
     * range bound
     */
    private static int rangeBound;
    /**
     * epsilon
     */
    private static double epsilon;
    /**
     * host
     */
    private static String host;
    /**
     * port
     */
    private static int port;
    /**
     * config
     */
    private static FemurDemoPirConfig config;
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * stopwatch
     */
    private static StopWatch stopWatch;
    /**
     * update value num
     */
    private static int updateValueNum;
    /**
     * update key num
     */
    private static int updateKeyNum;
    /**
     * append string
     */
    protected static String appendString;

    public static void run(Properties properties) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        stopWatch = new StopWatch();
        // read common config
        LOGGER.info("read common config");
        entryBitLength = PropertiesUtils.readInt(properties, "entry_bit_length");
        serverSetSizes = PropertiesUtils.readIntArray(properties, "server_set_size");
        queryNum = PropertiesUtils.readInt(properties, "query_num");
        rangeBound = PropertiesUtils.readInt(properties, "range_bound");
        epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        host = PropertiesUtils.readString(properties, "host");
        port = PropertiesUtils.readInt(properties, "port");
        updateValueNum = PropertiesUtils.readInt(properties, "update_value_num");
        updateKeyNum = PropertiesUtils.readInt(properties, "update_key_num");
        appendString = PropertiesUtils.readString(properties, "append_string");
        // create PTO config
        LOGGER.info("read PTO config");
        config = FemurPirConfigUtils.createConfig(properties, PTO_NAME_KEY);
        // generate database
        LOGGER.info("server generate element files");
        for (int setSize : serverSetSizes) {
            generateBytesInputFiles(setSize, entryBitLength);
        }
        // test query
        query();
        // test update value
        updateValue();
        // test update key
        updateKey();
        System.exit(0);
    }

    private static void query() throws IOException, InterruptedException {
        // create server
        FemurPirServerBoot serverBoot = FemurPirServerBootFactory.getInstance(host, port, config);
        LOGGER.info("create query test result file");
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + "QUERY_" + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Server Set Size\tQuery Number\tInit Time(ms)\tPto  Time(ms)\t" +
            "Register Request Size\tRegister Response Size\tRegister Time\t" +
            "Get Hint Request Size\tGet Hint Response Size\tGet Hint Time\t" +
            "Query Request Size\tQuery Response Size\tQuery Time\t" +
            "Client Gen Query Time\tClient Decode Time";
        printWriter.println(tab);
        for (int serverSetSize : serverSetSizes) {
            serverBoot.start();
            LOGGER.info(
                "server: serverSetSize = {}, entryBitLength = {}, queryNum = {}",
                serverSetSize, entryBitLength, queryNum
            );
            byte[][] elementArray = readServerElementArray(serverSetSize, entryBitLength);
            TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
            for (int j = 0; j < serverSetSize; j++) {
                keyValueMap.put(j, elementArray[j]);
            }
            stopWatch.start();
            serverBoot.init(serverSetSize, entryBitLength);
            serverBoot.setDatabase(keyValueMap);
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            stopWatch.start();
            FemurPirClient client = new FemurPirClient(host, port, "Alice", config);
            client.setUp();
            // client register and hint
            LOGGER.info("client register");
            FemurStatus registerStatus = client.register();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
            LOGGER.info("client get hint");
            FemurStatus hintStatus = client.getHint();
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, hintStatus);
            // queries
            long[] keys = keyValueMap.keys();
            for (int i = 0; i < queryNum; i++) {
                long key = keys[SECURE_RANDOM.nextInt(keyValueMap.size())];
                Pair<FemurStatus, byte[]> response = client.query(key, rangeBound, epsilon);
                Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, response.getLeft());
                Assert.assertArrayEquals(keyValueMap.get(key), response.getRight());
            }
            stopWatch.stop();
            long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            serverBoot.reset();
            String info = serverSetSize + "\t" + queryNum + "\t" + initTime + "\t" + ptoTime + "\t" +
                serverBoot.getPirServerProxy().getRegisterRequestSize() + "\t" +
                serverBoot.getPirServerProxy().getRegisterResponseSize() + "\t" +
                serverBoot.getPirServerProxy().getRegisterTime() + "\t" +
                serverBoot.getPirServerProxy().getGetHintRequestSize() + "\t" +
                serverBoot.getPirServerProxy().getGetHintResponseSize() + "\t" +
                serverBoot.getPirServerProxy().getGetHintTime() + "\t" +
                serverBoot.getPirServerProxy().getQueryRequestSize() + "\t" +
                serverBoot.getPirServerProxy().getQueryResponseSize() + "\t" +
                serverBoot.getPirServerProxy().getQueryTime() + "\t" +
                client.getGenQueryTime() + "\t" + client.getDecodeTime();
            printWriter.println(info);
            client.tearDown();
            serverBoot.stop();
            serverBoot.reset();
        }
    }

    private static void updateValue() throws IOException {
        // create server
        FemurPirServerBoot serverBoot = FemurPirServerBootFactory.getInstance(host, port, config);
        LOGGER.info("create update value result file");
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + "UPDATE_VALUE_" + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Server Set Size\tUpdate Number\tInit Time(ms)\tPto  Time(ms)";
        printWriter.println(tab);
        for (int serverSetSize : serverSetSizes) {
            serverBoot.start();
            LOGGER.info(
                "server: serverSetSize = {}, entryBitLength = {}, queryNum = {}",
                serverSetSize, entryBitLength, queryNum
            );
            byte[][] elementArray = readServerElementArray(serverSetSize, entryBitLength);
            TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
            for (int j = 0; j < serverSetSize; j++) {
                keyValueMap.put(j, elementArray[j]);
            }
            stopWatch.start();
            serverBoot.init(serverSetSize, entryBitLength);
            serverBoot.setDatabase(keyValueMap);
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            stopWatch.start();
            // update
            long[] keys = keyValueMap.keys();
            int byteL = CommonUtils.getByteLength(entryBitLength);
            for (int j = 0; j < updateValueNum; j++) {
                long key = keys[SECURE_RANDOM.nextInt(serverSetSize)];
                serverBoot.updateValue(key, BytesUtils.randomByteArray(byteL, entryBitLength, SECURE_RANDOM));
            }
            stopWatch.stop();
            long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            serverBoot.reset();
            String info = serverSetSize + "\t" + updateValueNum + "\t" + initTime + "\t" + ptoTime;
            printWriter.println(info);
            serverBoot.stop();
            serverBoot.reset();
        }
    }

    private static void updateKey() throws IOException {
        // create server
        FemurPirServerBoot serverBoot = FemurPirServerBootFactory.getInstance(host, port, config);
        LOGGER.info("create update key result file");
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + "UPDATE_KEY_" + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Server Set Size\tUpdate Number\tInit Time(ms)\tPto  Time(ms)";
        printWriter.println(tab);
        for (int serverSetSize : serverSetSizes) {
            serverBoot.start();
            LOGGER.info(
                "server: serverSetSize = {}, entryBitLength = {}, queryNum = {}",
                serverSetSize, entryBitLength, queryNum
            );
            byte[][] elementArray = readServerElementArray(serverSetSize, entryBitLength);
            TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
            for (int j = 0; j < serverSetSize; j++) {
                keyValueMap.put(j, elementArray[j]);
            }
            stopWatch.start();
            serverBoot.init(serverSetSize, entryBitLength);
            serverBoot.setDatabase(keyValueMap);
            stopWatch.stop();
            long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            stopWatch.start();
            // update
            int byteL = CommonUtils.getByteLength(entryBitLength);
            for (int j = 0; j < updateKeyNum; j++) {
                serverBoot.reset();
                keyValueMap.remove(j);
                long updateKey = serverSetSize + j;
                keyValueMap.put(updateKey, BytesUtils.randomByteArray(byteL, entryBitLength, SECURE_RANDOM));
                serverBoot.init(serverSetSize, entryBitLength);
                serverBoot.setDatabase(keyValueMap);
            }
            stopWatch.stop();
            long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            serverBoot.reset();
            String info = serverSetSize + "\t" + updateKeyNum + "\t" + initTime + "\t" + ptoTime;
            printWriter.println(info);
            serverBoot.stop();
            serverBoot.reset();
        }
    }
}
