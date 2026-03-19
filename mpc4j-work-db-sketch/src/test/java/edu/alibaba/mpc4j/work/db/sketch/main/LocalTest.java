package edu.alibaba.mpc4j.work.db.sketch.main;

import org.junit.Test;

import java.util.Objects;

public class LocalTest {
    @Test
    public void run0() throws Exception {
        String path = "conf_mg_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        StreamingMain.main(new String[]{configPath, "first"});
    }

    @Test
    public void run1() throws Exception {
        String path = "conf_mg_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        StreamingMain.main(new String[]{configPath, "second"});
    }

    @Test
    public void run2() throws Exception {
        String path = "conf_mg_example.conf";
        String configPath = Objects.requireNonNull(getClass().getClassLoader().getResource(path)).getPath();
        StreamingMain.main(new String[]{configPath, "third"});
    }
}
