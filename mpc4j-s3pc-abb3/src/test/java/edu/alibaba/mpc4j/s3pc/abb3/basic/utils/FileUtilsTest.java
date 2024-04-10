package edu.alibaba.mpc4j.s3pc.abb3.basic.utils;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * file utils test.
 *
 * @author Feng Han
 * @date 2024/01/08
 */
public class FileUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtilsTest.class);

    @Test
    public void test() throws IOException {
        // 1. 使用byte文件读写bitVector的速度
        // 2. 使用fileRpc相同方式读写bitVector的速度

        SecureRandom secureRandom = new SecureRandom();
        BitVector[] data = IntStream.range(0, 128).mapToObj(i ->
            BitVectorFactory.createRandom(12800000, secureRandom)).toArray(BitVector[]::new);
        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        FileUtils.writeFile(data, "1.txt");
        stopWatch.stop();
        long time00 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();


        stopWatch.start();
        BitVector[] read = FileUtils.readFileIntoBitVectors("1.txt", true);
        stopWatch.stop();
        long time01 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        Assert.assertEquals(read.length, data.length);
        for(int i = 0; i < read.length; i++){
            Assert.assertEquals(read[i], data[i]);
        }
        stopWatch.reset();

        stopWatch.start();
        byte[][] writeData = FileUtils.bitVectorToFileMsg(data);
        write("2.txt", writeData);
        stopWatch.stop();
        long time10 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();


        stopWatch.start();
        byte[][] readData = read("2.txt");
        stopWatch.stop();
        long time11 = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        LOGGER.info(time00 + "_" + time01 + "_" + time10 + "_" + time11);
    }


    private void write(String fileName, byte[][] data) throws IOException {
        File payloadFile = new File(fileName);
        // 写入数据包并统计发送数据量
        FileWriter payloadFileWriter = new FileWriter(payloadFile);
        PrintWriter payloadPrintWriter = new PrintWriter(payloadFileWriter, true);
        for(byte[] x : data){
            String payloadString = Base64.getEncoder().encodeToString(x);
            payloadPrintWriter.println(payloadString);
        }
        payloadPrintWriter.close();
    }

    private byte[][] read(String fileName) throws IOException {
        File payloadFile = new File(fileName);
        List<byte[]> byteArrayData = new LinkedList<>();
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(payloadFile), StandardCharsets.UTF_8
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String payloadString;
        while ((payloadString = bufferedReader.readLine()) != null) {
            // 包含该行内容的字符串，不包含任何行终止符，如果已到达流末尾，则返回null
            byte[] byteArray = Base64.getDecoder().decode(payloadString);
            byteArrayData.add(byteArray);
        }
        bufferedReader.close();
        // 删除负载文件
        payloadFile.delete();
        return byteArrayData.toArray(new byte[0][]);
    }

}
