package edu.alibaba.mpc4j.dp.service.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * streaming data (.dat)
 *
 * @author Weiran Liu
 * @date 2022/11/17
 */
public class StreamDataUtils {

    private StreamDataUtils() {
        // empty
    }

    /**
     * Obtain item stream from the .dat file. Note that you need to invoke {@code close()} to close the stream.
     *
     * @param path file path.
     * @return item string.
     */
    public static Stream<String> obtainItemStream(String path) throws IOException {
        try {
            // directly running return Files.lines(Paths.get(path)) may have problems on Windows due to wrong path
            return Files.lines(Paths.get(new File(path).getAbsolutePath()))
                // each line contains more items, split by " "
                .map(line -> line.split(" "))
                .flatMap(Arrays::stream);
        } catch (NoSuchFileException e) {
            File file = new File(path);
            throw new NoSuchFileException("Cannot find data from " + file.getAbsolutePath());
        }

    }
}
