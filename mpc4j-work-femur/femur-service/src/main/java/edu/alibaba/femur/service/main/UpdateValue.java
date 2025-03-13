package edu.alibaba.femur.service.main;

import edu.alibaba.femur.service.client.FemurPirClient;
import edu.alibaba.femur.service.server.FemurPirServerBoot;
import edu.alibaba.femur.service.server.FemurPirServerBootFactory;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
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


public class UpdateValue {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateValue.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME_KEY = "femur_pir_pto_name";
    /**
     * type name
     */
    public static final String PTO_TYPE_NAME = "UPDATE_VALUE";
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
     * interval time
     */
    private static int intervalTime;
    /**
     * append string
     */
    protected static String appendString;
    /**
     * client id
     */
    private static String clientId;

    public static void run(Properties properties, String ownName) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        stopWatch = new StopWatch();
        // read common config
        LOGGER.info("read common config");
        entryBitLength = PropertiesUtils.readInt(properties, "entry_bit_length");
        serverSetSizes = PropertiesUtils.readIntArray(properties, "server_set_size");
        MathPreconditions.checkEqual("server set sizes", "1", serverSetSizes.length, 1);
        queryNum = PropertiesUtils.readInt(properties, "query_num");
        rangeBound = PropertiesUtils.readInt(properties, "range_bound");
        epsilon = PropertiesUtils.readDouble(properties, "epsilon");
        host = PropertiesUtils.readString(properties, "host");
        port = PropertiesUtils.readInt(properties, "port");
        intervalTime = PropertiesUtils.readInt(properties, "interval_time");
        appendString = PropertiesUtils.readString(properties, "append_string");
        clientId = PropertiesUtils.readString(properties, "client_id");
        // create PTO config
        LOGGER.info("read PTO config");
        config = FemurPirConfigUtils.createConfig(properties, PTO_NAME_KEY);
        // generate database
        LOGGER.info("server generate element files");
        for (int setSize : serverSetSizes) {
            generateBytesInputFiles(setSize, entryBitLength);
        }
        // test query
        if (ownName.equals("server")) {
            runServer();
        } else if (ownName.equals("client") ){
            runClient();
        } else {
            throw new IllegalArgumentException("Invalid own name: " + ownName);
        }
        System.exit(0);
    }

    private static void runServer() throws IOException, InterruptedException {
        // create server
        FemurPirServerBoot serverBoot = FemurPirServerBootFactory.getInstance(host, port, config);
        serverBoot.start();
        LOGGER.info(
            "server: serverSetSize = {}, entryBitLength = {}, queryNum = {}",
            serverSetSizes[0], entryBitLength, queryNum
        );
        byte[][] elementArray = readServerElementArray(serverSetSizes[0], entryBitLength);
        TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
        for (int j = 0; j < serverSetSizes[0]; j++) {
            keyValueMap.put(j, elementArray[j]);
        }
        serverBoot.init(serverSetSizes[0], entryBitLength);
        serverBoot.setDatabase(keyValueMap);
        LOGGER.info("Server Ready");
        do {
            Thread.sleep(intervalTime);
            long[] keys = keyValueMap.keys();
            int byteL = CommonUtils.getByteLength(entryBitLength);
            long key = keys[SECURE_RANDOM.nextInt(serverSetSizes[0])];
            serverBoot.updateValue(key, BytesUtils.randomByteArray(byteL, entryBitLength, SECURE_RANDOM));
        } while (true);
    }

    private static void runClient() throws IOException, InterruptedException {
        LOGGER.info("create query test result file");
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + "UPDATE_VALUE_" + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + entryBitLength
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Server Set Size\tQuery Number\tInit  Time(ms)\tPto  Time(ms)";
        printWriter.println(tab);
        LOGGER.info(
            "server: serverSetSize = {}, entryBitLength = {}, queryNum = {}",
            serverSetSizes[0], entryBitLength, queryNum
        );
        byte[][] elementArray = readServerElementArray(serverSetSizes[0], entryBitLength);
        TLongObjectMap<byte[]> keyValueMap = new TLongObjectHashMap<>();
        for (int j = 0; j < serverSetSizes[0]; j++) {
            keyValueMap.put(j, elementArray[j]);
        }
        FemurPirClient client = new FemurPirClient(host, port, clientId, config);
        client.setUp();
        // client register and hint
        stopWatch.start();
        LOGGER.info("client register");
        FemurStatus registerStatus = client.register();
        Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, registerStatus);
        LOGGER.info("client get hint");
        FemurStatus hintStatus = client.getHint();
        Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, hintStatus);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        // queries
        stopWatch.start();
        long[] keys = keyValueMap.keys();
        for (int i = 0; i < queryNum; i++) {
            long key = keys[SECURE_RANDOM.nextInt(keyValueMap.size())];
            Pair<FemurStatus, byte[]> response = client.query(key, rangeBound, epsilon);
            Assert.assertEquals(FemurStatus.SERVER_SUCC_RES, response.getLeft());
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        String info = serverSetSizes[0] + "\t" + queryNum + "\t" + initTime + "\t" + ptoTime;
        printWriter.println(info);
        client.tearDown();
    }
}
