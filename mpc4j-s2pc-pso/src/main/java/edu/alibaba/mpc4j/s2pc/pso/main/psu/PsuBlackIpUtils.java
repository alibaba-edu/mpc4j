package edu.alibaba.mpc4j.s2pc.pso.main.psu;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PSU_BLACK_IP工具类。
 *
 * @author Weiran Liu
 * @date 2022/9/23
 */
public class PsuBlackIpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsuBlackIpMain.class);

    /**
     * 私有构造函数
     */
    private PsuBlackIpUtils() {
        // empty
    }

    /**
     * IP元素字节长度
     */
    static final int IP_BYTE_LENGTH = 4;

    /**
     * 读取黑IP集合。
     *
     * @param file 文件路径。
     * @return 黑IP集合。
     */
    static Set<ByteBuffer> readBlackIpSet(String file) throws IOException {
        Path filePath = Paths.get(file);
        LOGGER.info("The given black IP file path = {}", filePath.toAbsolutePath());
        return Files.lines(filePath)
            .map(ip -> {
                String[] splitIp = ip.split("\\.");
                Preconditions.checkArgument(splitIp.length == 4, "IP must be in format XXX.XXX.XXX.XXX: {}" + ip);
                ByteBuffer ipByteBuffer = ByteBuffer.allocate(IP_BYTE_LENGTH);
                for (String subIp : splitIp) {
                    // 直接用Byte.parseByte智能读取有符号数，这里需要先通过int按照无符号数读取
                    int intValue = Integer.parseInt(subIp);
                    ipByteBuffer.put((byte)(intValue & 0xFF));
                }
                return ipByteBuffer.array();
            })
            .map(ByteBuffer::wrap)
            .collect(Collectors.toSet());
    }
}
