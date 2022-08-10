package edu.alibaba.mpc4j.s2pc.pso;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 字节数组元素集合生成测试。
 *
 * @author Weiran Liu
 * @date 2022/02/16
 */
@RunWith(Parameterized.class)
public class BytesElementGenTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurationParams = new ArrayList<>();
        // n = 2^12
        configurationParams.add(new Object[] {"n = 2^12", 1 << 12,});
        // n = 2^16
        configurationParams.add(new Object[] {"n = 2^16", 1 << 16,});
        // n = 2^20
        configurationParams.add(new Object[] {"n = 2^20", 1 << 20,});

        return configurationParams;
    }

    /**
     * 集合大小
     */
    private final int setSize;

    public BytesElementGenTest(String name, int setSize) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name));
        this.setSize = setSize;
    }

    @Test
    public void testGen8ByteSets() {
        testGenSets(Long.BYTES);
    }

    @Test
    public void testGen16ByteSets() {
        testGenSets(CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Test
    public void testGen32ByteSets() {
        testGenSets(CommonConstants.BLOCK_BYTE_LENGTH * 2);
    }

    private void testGenSets(int elementByteLength) {
        ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(setSize, elementByteLength);
        Set<ByteBuffer> serverSet = sets.get(0);
        Set<ByteBuffer> clientSet = sets.get(1);
        // 验证集合大小
        Assert.assertEquals(setSize, serverSet.size());
        Assert.assertEquals(setSize, clientSet.size());
        // 验证字节长度
        serverSet.forEach(element -> Assert.assertEquals(elementByteLength, element.array().length));
        clientSet.forEach(element -> Assert.assertEquals(elementByteLength, element.array().length));
        // 验证并集
        Set<ByteBuffer> unionSet = new HashSet<>(serverSet);
        unionSet.addAll(clientSet);
        Assert.assertTrue(unionSet.size() > 0);
    }

    @Test
    public void testGen8BytesFiles() throws IOException {
        testGenFiles(Long.BYTES);
    }

    @Test
    public void testGen16BytesFiles() throws IOException {
        testGenFiles(CommonConstants.BLOCK_BYTE_LENGTH);
    }

    @Test
    public void testGen32BytesFiles() throws IOException {
        testGenFiles(CommonConstants.BLOCK_BYTE_LENGTH * 2);
    }

    private void testGenFiles(int elementByteLength) throws IOException {
        PsoUtils.generateBytesInputFiles(setSize, elementByteLength);
        // 读取发送方文件
        InputStreamReader senderFileInputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, elementByteLength)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader senderFileBufferedReader = new BufferedReader(senderFileInputStreamReader);
        Set<ByteBuffer> senderSet = senderFileBufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        senderFileBufferedReader.close();
        senderFileInputStreamReader.close();
        // 读取接收方文件
        InputStreamReader receiverFileInputStreamReader = new InputStreamReader(
            new FileInputStream(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, elementByteLength)),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader receiverFileBufferedReader = new BufferedReader(receiverFileInputStreamReader);
        Set<ByteBuffer> clientSet = receiverFileBufferedReader.lines()
            .map(Hex::decode)
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
        receiverFileBufferedReader.close();
        receiverFileInputStreamReader.close();
        // 验证集合数量
        Assert.assertEquals(setSize, senderSet.size());
        Assert.assertEquals(setSize, clientSet.size());
        // 验证集合元素字节长度
        senderSet.forEach(element -> Assert.assertEquals(elementByteLength, element.array().length));
        clientSet.forEach(element -> Assert.assertEquals(elementByteLength, element.array().length));
    }
}
