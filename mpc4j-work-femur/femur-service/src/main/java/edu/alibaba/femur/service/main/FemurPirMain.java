package edu.alibaba.femur.service.main;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.stream.IntStream;

public class FemurPirMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(FemurPirMain.class);

    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * main.
     *
     * @param args one input: config file name.
     */
    public static void main(String[] args) throws Exception {
        PropertiesUtils.loadLog4jProperties();
        Properties properties = PropertiesUtils.loadProperties(args[0]);
        String ptoType = MainPtoConfigUtils.readPtoType(properties);
        String ownName;
        switch (ptoType) {
            case Benchmark.PTO_TYPE_NAME:
                Benchmark.run(properties);
                break;
            case UpdateValue.PTO_TYPE_NAME:
                ownName = args[1];
                UpdateValue.run(properties, ownName);
                break;
            case UpdateKey.PTO_TYPE_NAME:
                ownName = args[1];
                UpdateKey.run(properties, ownName);
            default:
                throw new IllegalArgumentException("Invalid pto_type: " + ptoType);
        }
        System.exit(0);
    }

    /**
     * generate bytes input files.
     *
     * @param setSize          set size.
     * @param elementBitLength element bit length.
     * @throws IOException create files failed.
     */
    public static void generateBytesInputFiles(int setSize, int elementBitLength) throws IOException {
        MathPreconditions.checkPositive("elementBitLength", elementBitLength);
        File serverInputFile = new File(getServerFileName("BYTES_SERVER", setSize, elementBitLength));
        if (serverInputFile.exists()) {
            return;
        }
        LOGGER.info("Lost some / all files, generate byte[] set files.");
        if (serverInputFile.exists()) {
            LOGGER.info("Delete server byte[] set file.");
            Preconditions.checkArgument(
                serverInputFile.delete(), "Fail to delete file: %s", serverInputFile.getName()
            );
        }
        byte[][] elementArray = generateElementArray(setSize, elementBitLength);
        FileWriter serverFileWriter = new FileWriter(serverInputFile);
        PrintWriter serverPrintWriter = new PrintWriter(serverFileWriter, true);
        IntStream.range(0, setSize)
            .mapToObj(i -> Hex.toHexString(elementArray[i]))
            .forEach(serverPrintWriter::println);
        serverPrintWriter.close();
        serverFileWriter.close();
    }

    /**
     * return server file name.
     *
     * @param prefix           prefix.
     * @param setSize          set size.
     * @param elementBitLength element bit length.
     * @return server file name.
     */
    public static String getServerFileName(String prefix, int setSize, int elementBitLength) {
        return MainPtoConfigUtils.getFileFolderName() + prefix + "_" + prefix + "_" + elementBitLength + "_" + setSize + ".input";
    }

    /**
     * generate random element array.
     *
     * @param elementSize      element size.
     * @param elementBitLength element bit length.
     * @return random element array.
     */
    public static byte[][] generateElementArray(int elementSize, int elementBitLength) {
        int elementByteLength = CommonUtils.getByteLength(elementBitLength);
        return IntStream.range(0, elementSize)
            .mapToObj(i -> BytesUtils.randomByteArray(elementByteLength, elementBitLength, SECURE_RANDOM))
            .toArray(byte[][]::new);
    }

    public static byte[][] readServerElementArray(int elementSize, int elementBitLength) throws IOException {
        LOGGER.info("Server read element array");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(getServerFileName("BYTES_SERVER", elementSize, elementBitLength)),
            StandardCharsets.UTF_8
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        byte[][] elementArray = bufferedReader.lines()
            .map(Hex::decode)
            .toArray(byte[][]::new);
        bufferedReader.close();
        inputStreamReader.close();
        return elementArray;
    }
}
