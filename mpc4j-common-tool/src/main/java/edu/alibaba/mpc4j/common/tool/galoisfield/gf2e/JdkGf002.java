package edu.alibaba.mpc4j.common.tool.galoisfield.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2e.Gf2eFactory.Gf2eType;

/**
 * GF(2^2) using pure Java.
 *
 * @author Weiran Liu
 * @date 2024/6/3
 */
class JdkGf002 extends AbstractGf2e {
    /**
     * mul lookup table
     */
    private static final byte[] MUL_LOOKUP_TABLE = new byte[]{
        // (  0) * (  0) = (  0)
        0b00000000,
        // (  0) * (  1) = (  0)
        0b00000000,
        // (  0) * (x  ) = (  0)
        0b00000000,
        // (  0) * (x+1) = (  0)
        0b00000000,
        // (  1) * (  0) = (  0)
        0b00000000,
        // (  1) * (  1) = (  1)
        0b00000001,
        // (  1) * (x  ) = (x  )
        0b00000010,
        // (  1) * (x+1) = (x+1)
        0b00000011,
        // (x  ) * (  0) = (  0)
        0b00000000,
        // (x  ) * (  1) = (x  )
        0b00000010,
        // (x  ) * (x  ) = (x+1)
        0b00000011,
        // (x  ) * (x+1) = (  1)
        0b00000001,
        // (x+1) * (  0) = (  0)
        0b00000000,
        // (x+1) * (  1) = (x+1)
        0b00000011,
        // (x+1) * (x  ) = (  1)
        0b00000001,
        // (x+1) * (x+1) = (x  )
        0b00000010,
    };

    /**
     * inv lookup table
     */
    private static final byte[] INV_LOOKUP_TABLE = new byte[]{
        // (  0)^-1 = undefined
        0b00000000,
        // (  1)^-1 = (  1)
        0b00000001,
        // (x  )^-1 = (x+1)
        0b00000011,
        // (x+1)^-1 = (x  )
        0b00000010,
    };

    /**
     * div lookup table
     */
    private static final byte[] DIV_LOOKUP_TABLE = new byte[]{
        // (  0) / (  0) = undefined
        0b00000000,
        // (  0) / (  1) = (  0)
        0b00000000,
        // (  0) / (x  ) = (  0)
        0b00000000,
        // (  0) / (x+1) = (  0)
        0b00000000,
        // (  1) / (  0) = undefined
        0b00000000,
        // (  1) / (  1) = (  1)
        0b00000001,
        // (  1) / (x  ) = (x+1)
        0b00000011,
        // (  1) * (x+1) = (x  )
        0b00000010,
        // (x  ) / (  0) = undefined
        0b00000000,
        // (x  ) / (  1) = (x  )
        0b00000010,
        // (x  ) / (x  ) = (  1)
        0b00000001,
        // (x  ) / (x+1) = (x+1)
        0b00000011,
        // (x+1) / (  0) = undefined
        0b00000000,
        // (x+1) / (  1) = (x+1)
        0b00000011,
        // (x+1) / (x  ) = (x  )
        0b00000010,
        // (x+1) / (x+1) = (  1)
        0b00000001,
    };

    public JdkGf002(EnvType envType) {
        super(envType, 2);
    }

    @Override
    public Gf2eType getGf2eType() {
        return Gf2eType.JDK;
    }

    @Override
    public boolean validateElement(byte[] p) {
        return p.length == 1 && ((p[0] & 0b00000011) == p[0]);
    }

    @Override
    public byte[] mul(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[] r = new byte[byteL];
        r[0] = MUL_LOOKUP_TABLE[(p[0] << 2) | q[0]];
        return r;
    }

    @Override
    public void muli(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        p[0] = MUL_LOOKUP_TABLE[(p[0] << 2) | q[0]];
    }

    @Override
    public byte[] inv(byte[] p) {
        assert validateNonZeroElement(p);
        return new byte[]{INV_LOOKUP_TABLE[p[0]]};
    }

    @Override
    public void invi(byte[] p) {
        assert validateNonZeroElement(p);
        p[0] = INV_LOOKUP_TABLE[p[0]];
    }

    @Override
    public byte[] div(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        byte[] r = new byte[byteL];
        r[0] = DIV_LOOKUP_TABLE[(p[0] << 2) | q[0]];
        return r;
    }

    @Override
    public void divi(byte[] p, byte[] q) {
        assert validateElement(p);
        assert validateElement(q);
        p[0] = DIV_LOOKUP_TABLE[(p[0] << 2) | q[0]];
    }
}
