package edu.alibaba.mpc4j.s2pc.aby.operator.row.extension.zl.g24;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

import java.io.Serializable;

/**
 * G24 zl value signed extension protocol description. The protocol comes from the idea of Hao Guo.
 *
 * @author Li Peng
 * @date 2024/6/20
 */
public class G24ZlExtensionPtoDesc implements PtoDesc, Serializable {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 3924345120538641962L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "G24_ZL_EXTENSION";

    /**
     * singleton mode
     */
    private static final G24ZlExtensionPtoDesc INSTANCE = new G24ZlExtensionPtoDesc();

    /**
     * private constructor.
     */
    private G24ZlExtensionPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
