package edu.alibaba.mpc4j.crypto.fhe.rq;

import edu.alibaba.mpc4j.crypto.fhe.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.modulus.Modulus;
import org.junit.Assert;
import org.junit.Test;

/**
 * Polynomial Arithmetic Small Mod unit tests.
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/polyarithsmallmod.cpp
 * </p>
 *
 * @author Anony_Trent, Liqiang Peng
 * @date 2023/8/20
 */
public class PolyArithmeticSmallModTest {

    @Test
    public void testModuloPolyCoeffs() {
        {
            long[] poly = new long[]{2, 15, 77};
            Modulus modulus = new Modulus(15);
            PolyArithmeticSmallMod.moduloPolyCoeff(poly, 3, modulus, poly);
            Assert.assertArrayEquals(new long[]{2, 0, 2}, poly);
        }
        {
            long[][] poly = new long[][]{
                {2, 15, 77},
                {2, 15, 77}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] rns = RnsIterator.createRnsFrom2dArray(poly);
            PolyArithmeticSmallMod.moduloPolyCoeffRns(rns, 3, 2, modulus, rns, 3, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 2},
                    {2, 0, 2}},
                RnsIterator.rnsTo2dArray(rns, 3, 2)
            );
        }
        {
            long[][][] poly = new long[][][]{
                {
                    {2, 15, 77},
                    {2, 15, 77},
                },
                {
                    {2, 15, 77},
                    {2, 15, 77},
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] result = PolyIterator.createPolyFrom3dArray(poly);
            PolyArithmeticSmallMod.moduloPolyCoeffPoly(result, 3, 2, 2, modulus, result, 3, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 0, 2},
                        {2, 0, 2},
                    },
                    {
                        {2, 0, 2},
                        {2, 0, 2},
                    }
                },
                PolyIterator.polyTo3dArray(result, 2, 3, 2)
            );
        }
    }

    @Test
    public void testNegatePolyCoeffs() {
        {
            Modulus modulus = new Modulus(15);
            long[] poly = new long[]{2, 3, 4};
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly, 3, modulus, poly);
            Assert.assertArrayEquals(new long[]{13, 12, 11}, poly);

            poly = new long[]{2, 3, 4};
            modulus = new Modulus(0xFFFFFFFFFFFFFFL);
            PolyArithmeticSmallMod.negatePolyCoeffMod(poly, 3, modulus, poly);
            Assert.assertArrayEquals(new long[]{0xFFFFFFFFFFFFFDL, 0xFFFFFFFFFFFFFCL, 0xFFFFFFFFFFFFFBL}, poly);
        }
        {
            long[][] poly = new long[][]{
                {2, 3, 4},
                {2, 0, 1}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] rns = RnsIterator.createRnsFrom2dArray(poly);
            PolyArithmeticSmallMod.negatePolyCoeffModRns(rns, 3, 2, modulus, rns, 3, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {13, 12, 11},
                    {1, 0, 2}},
                RnsIterator.rnsTo2dArray(rns, 3, 2)
            );
        }
        {
            long[][][] poly = new long[][][]{
                {
                    {2, 3, 4},
                    {2, 0, 1}
                },
                {
                    {2, 3, 4},
                    {2, 0, 1}
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] result = PolyIterator.createPolyFrom3dArray(poly);
            PolyArithmeticSmallMod.negatePolyCoeffModPoly(result, 3, 2, 2, modulus, result, 3, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {13, 12, 11},
                        {1, 0, 2}
                    },
                    {
                        {13, 12, 11},
                        {1, 0, 2}
                    }
                },
                PolyIterator.polyTo3dArray(result, 2, 3, 2)
            );
        }
    }

    @Test
    public void testAddPolyCoeffMod() {
        {
            long[] poly1 = new long[]{1, 3, 4};
            long[] poly2 = new long[]{1, 2, 4};
            Modulus modulus = new Modulus(5);
            PolyArithmeticSmallMod.addPolyCoeffMod(poly1, poly2, 3, modulus, poly1);
            Assert.assertArrayEquals(new long[]{2, 0, 3}, poly1);
        }
        {
            long[][] poly1 = new long[][]{
                {1, 3, 4},
                {0, 1, 2}
            };
            long[][] poly2 = new long[][]{
                {1, 2, 4},
                {2, 1, 0}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns1 = RnsIterator.createRnsFrom2dArray(poly1);
            long[] rns2 = RnsIterator.createRnsFrom2dArray(poly2);
            PolyArithmeticSmallMod.addPolyCoeffModRns(rns1, 3, 2, rns2, 3, 2, modulus, rns1, 3, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 3},
                    {2, 2, 2}
                },
                RnsIterator.rnsTo2dArray(rns1, 3, 2)
            );
        }
        {
            long[][][] poly1 = new long[][][]{
                {
                    {1, 3, 4},
                    {0, 1, 2}
                },
                {
                    {2, 4, 0},
                    {1, 2, 0}
                }
            };
            long[][][] poly2 = new long[][][]{
                {
                    {1, 2, 4},
                    {2, 1, 0}
                },
                {
                    {2, 4, 0},
                    {0, 2, 1}
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] result1 = PolyIterator.createPolyFrom3dArray(poly1);
            long[] result2 = PolyIterator.createPolyFrom3dArray(poly2);
            PolyArithmeticSmallMod.addPolyCoeffModPoly(result1, 3, 2, result2, 3, 2, 2, modulus, result1, 3, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 0, 3},
                        {2, 2, 2}
                    },
                    {
                        {4, 3, 0},
                        {1, 1, 1}
                    }
                },
                PolyIterator.polyTo3dArray(result1, 2, 3, 2)
            );
        }
    }

    @Test
    public void testSubPolyCoeffMod() {
        {
            long[] poly1 = new long[]{4, 3, 2};
            long[] poly2 = new long[]{2, 3, 4};
            Modulus modulus = new Modulus(5);
            PolyArithmeticSmallMod.subPolyCoeffMod(poly1, poly2, 3, modulus, poly1);
            Assert.assertArrayEquals(new long[]{2, 0, 3}, poly1);
        }
        {
            long[][] poly1 = new long[][]{
                {1, 3, 4},
                {0, 1, 2}
            };
            long[][] poly2 = new long[][]{
                {1, 2, 4},
                {2, 1, 0}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns1 = RnsIterator.createRnsFrom2dArray(poly1);
            long[] rns2 = RnsIterator.createRnsFrom2dArray(poly2);
            PolyArithmeticSmallMod.subPolyCoeffModRns(rns1, 3, 2, rns2, 3, 2, modulus, rns1, 3, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {0, 1, 0},
                    {1, 0, 2}
                },
                RnsIterator.rnsTo2dArray(rns1, 3, 2)
            );
        }
        {
            long[][][] data1 = new long[][][]{
                {
                    {1, 3, 4},
                    {0, 1, 2}
                },
                {
                    {2, 4, 0},
                    {1, 2, 0}
                }
            };
            long[][][] data2 = new long[][][]{
                {
                    {1, 2, 4},
                    {2, 1, 0}
                },
                {
                    {2, 4, 0},
                    {0, 2, 1}
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] poly1 = PolyIterator.createPolyFrom3dArray(data1);
            long[] poly2 = PolyIterator.createPolyFrom3dArray(data2);
            PolyArithmeticSmallMod.subPolyCoeffModPoly(poly1, 3, 2, poly2, 3, 2, 2, modulus, poly1, 3, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {0, 1, 0},
                        {1, 0, 2}
                    },
                    {
                        {0, 0, 0},
                        {1, 0, 2}
                    }
                },
                PolyIterator.polyTo3dArray(poly1, 2, 3, 2)
            );
        }
    }

    @Test
    public void testMultiplyPolyScalarCoeffMod() {
        {
            long[] coeff = new long[]{1, 3, 4};
            Modulus modulus = new Modulus(5);
            long scalar = 3;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(coeff, 3, scalar, modulus, coeff);
            Assert.assertArrayEquals(new long[]{3, 4, 2}, coeff);
        }
        {
            long[][] data = new long[][]{
                {1, 3, 4},
                {1, 0, 2}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            long scalar = 2;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModRns(rns, 3, 2, scalar, modulus, rns, 3, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 1, 3},
                    {2, 0, 1}
                },
                RnsIterator.rnsTo2dArray(rns, 3, 2)
            );
        }
        {
            long[][][] data = new long[][][]{
                {
                    {1, 3, 4},
                    {1, 0, 2}
                },
                {
                    {1, 3, 4},
                    {1, 0, 2}
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            long scalar = 2;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffModPoly(poly, 3, 2, 2, scalar, modulus, poly, 3, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 1, 3},
                        {2, 0, 1}
                    },
                    {
                        {2, 1, 3},
                        {2, 0, 1}
                    }
                },
                PolyIterator.polyTo3dArray(poly, 2, 3, 2)
            );
        }
    }

    @Test
    public void testMultiplyPolyMonoCoeffMod() {
        {
            long[] coeff = new long[]{1, 3, 4, 2};
            Modulus modulus = new Modulus(5);
            long[] result = new long[4];

            long monoCoeff = 3;
            int monoExponent = 0;
            // n = 1, 1 * 3 mod 5 = 3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 1, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 0, 0, 0}, result);

            // n = 2, (1 + 3x) * 3 mod 5 = 3 + 4x
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 2, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 4, 0, 0}, result);

            monoExponent = 1;
            // n = 2, (1 + 3x) * 3x mod 5 = (3x - 9) mod 5 = 1 + 3x
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 2, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{1, 3, 0, 0}, result);

            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (3x) = (3x + 9x^2 + 12x^3 - 6) mod 5 = 4 + 3x + 4x^2 + 2x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{4, 3, 4, 2}, result);

            monoCoeff = 1;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (x) mod 5 = (x + 3x^2 + 4x^3 - 2) mod 5 = 3 + x + 3x^2 + 4x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 1, 3, 4}, result);

            monoCoeff = 4;
            monoExponent = 3;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (4x^3) mod 5 = (4x^3 - 12 - 16x - 8x^2) mod 5 = 3 + 4x + 2x^2 + 4x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{3, 4, 2, 4}, result);

            monoCoeff = 1;
            monoExponent = 0;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (1) mod 5 = 1 + 3x + 4x^2 + 2x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeff, 4, monoCoeff, monoExponent, modulus, result);
            Assert.assertArrayEquals(new long[]{1, 3, 4, 2}, result);
        }
        {
            long[][] data = new long[][]{
                {1, 3, 4, 2},
                {1, 3, 4, 2}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 7});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            long[] rnsR = RnsIterator.allocateZeroRns(4, 2);

            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModRns(
                rns, 4, 2, monoCoeff, monoExponent, modulus, rnsR, 4, 2
            );
            Assert.assertArrayEquals(
                new long[][]{
                    {4, 2, 4, 2},
                    {5, 6, 4, 5}
                },
                RnsIterator.rnsTo2dArray(rnsR, 4, 2)
            );
        }
        {
            long[][][] data = new long[][][]{
                {
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
                },
                {
                    {1, 3, 4, 2},
                    {1, 3, 4, 2},
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 7});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            long[] polyR = PolyIterator.allocateZeroPoly(2, 4, 2);
            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                poly, 4, 2, 2, monoCoeff, monoExponent, modulus, polyR, 4, 2
            );
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {4, 2, 4, 2},
                        {5, 6, 4, 5}
                    },
                    {
                        {4, 2, 4, 2},
                        {5, 6, 4, 5}
                    }
                },
                PolyIterator.polyTo3dArray(polyR, 2, 4, 2)
            );
        }
    }

    @Test
    public void testDyadicProductCoeffMod() {
        {
            long[] coeff1 = new long[]{1, 1, 1};
            long[] coeff2 = new long[]{2, 3, 4};
            long[] coeffR = new long[3];
            Modulus modulus = new Modulus(13);
            PolyArithmeticSmallMod.dyadicProductCoeffMod(coeff1, coeff2, 3, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{2, 3, 4}, coeffR);
        }
        {
            long[][] data1 = new long[][]{
                {1, 2, 1},
                {2, 1, 2}
            };
            long[][] data2 = new long[][]{
                {2, 3, 4},
                {2, 3, 4}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{13, 7});
            long[] rns1 = RnsIterator.createRnsFrom2dArray(data1);
            long[] rns2 = RnsIterator.createRnsFrom2dArray(data2);
            long[] rnsR = RnsIterator.allocateZeroRns(3, 2);
            PolyArithmeticSmallMod.dyadicProductCoeffModRns(rns1, 3, 2, rns2, 3, 2, modulus, rnsR, 3, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 6, 4},
                    {4, 3, 1}
                },
                RnsIterator.rnsTo2dArray(rnsR, 3, 2)
            );
        }
        {
            long[][][] data1 = new long[][][]{
                {
                    {1, 2, 1},
                    {2, 1, 2}
                },
                {
                    {1, 2, 1},
                    {2, 1, 2}
                }
            };
            long[][][] data2 = new long[][][]{
                {
                    {2, 3, 4},
                    {2, 3, 4}
                },
                {
                    {2, 3, 4},
                    {2, 3, 4}
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{13, 7});
            long[] poly1 = PolyIterator.createPolyFrom3dArray(data1);
            long[] poly2 = PolyIterator.createPolyFrom3dArray(data2);
            long[] polyR = PolyIterator.allocateZeroPoly(2, 3, 2);
            PolyArithmeticSmallMod.dyadicProductCoeffModPoly(poly1, 3, 2, poly2, 3, 2, 2, modulus, polyR, 3, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {2, 6, 4},
                        {4, 3, 1}
                    },
                    {
                        {2, 6, 4},
                        {4, 3, 1}
                    }
                },
                PolyIterator.polyTo3dArray(polyR, 2, 3, 2)
            );
        }
    }


    @Test
    public void testPolyInftyNormCoeffMod() {
        Modulus mod = new Modulus(10);

        long[] poly = new long[]{0, 1, 2, 3};
        Assert.assertEquals(3, PolyArithmeticSmallMod.polyInftyNormCoeffMod(poly, 4, mod));

        poly = new long[]{0, 1, 2, 8};
        Assert.assertEquals(2, PolyArithmeticSmallMod.polyInftyNormCoeffMod(poly, 4, mod));
    }

    @Test
    public void testNegacyclicShiftPolyCoeffMod() {
        {
            long[] coeff = new long[4];
            long[] coeffR = new long[4];
            Modulus modulus = new Modulus(10);

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 0, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 2, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 3, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffR);

            coeff = new long[]{1, 2, 3, 4};
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 0, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{1, 2, 3, 4}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{6, 1, 2, 3}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 2, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{7, 6, 1, 2}, coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff, 4, 3, modulus, coeffR);
            Assert.assertArrayEquals(new long[]{8, 7, 6, 1}, coeffR);

            coeff = new long[]{1, 2, 3, 4};
            int n1 = 2;
            long[] coeff1 = new long[n1];
            coeffR = new long[n1];
            System.arraycopy(coeff, 0, coeff1, 0, n1);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff1, n1, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[] {8, 1}, coeffR);
            int n2 = 2;
            long[] coeff2 = new long[n2];
            coeffR = new long[n2];
            System.arraycopy(coeff, 2, coeff2, 0, n2);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeff2, n2, 1, modulus, coeffR);
            Assert.assertArrayEquals(new long[] {6, 3}, coeffR);
        }
        {
            long[][] data = new long[][]{
                {1, 2, 3, 4},
                {1, 2, 3, 4}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{10, 11});
            long[] rns = RnsIterator.createRnsFrom2dArray(data);
            long[] rnsR = RnsIterator.allocateZeroRns(4, 2);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, 4, 2, 0, modulus, rnsR, 4, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                },
                RnsIterator.rnsTo2dArray(rnsR, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, 4, 2, 1, modulus, rnsR, 4, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {6, 1, 2, 3},
                    {7, 1, 2, 3}
                },
                RnsIterator.rnsTo2dArray(rnsR, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, 4, 2, 2, modulus, rnsR, 4, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {7, 6, 1, 2},
                    {8, 7, 1, 2}
                },
                RnsIterator.rnsTo2dArray(rnsR, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rns, 4, 2, 3, modulus, rnsR, 4, 2);
            Assert.assertArrayEquals(
                new long[][]{
                    {8, 7, 6, 1},
                    {9, 8, 7, 1}
                },
                RnsIterator.rnsTo2dArray(rnsR, 4, 2)
            );
        }
        {
            long[][][] data = new long[][][]{
                {
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                },
                {
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                }
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{10, 11});
            long[] poly = PolyIterator.createPolyFrom3dArray(data);
            long[] polyR = PolyIterator.allocateZeroPoly(2, 4, 2);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, 4, 2, 2, 0, modulus, polyR, 4, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {1, 2, 3, 4},
                        {1, 2, 3, 4}
                    },
                    {
                        {1, 2, 3, 4},
                        {1, 2, 3, 4}
                    }
                },
                PolyIterator.polyTo3dArray(polyR, 2, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, 4, 2, 2, 1, modulus, polyR, 4, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {6, 1, 2, 3},
                        {7, 1, 2, 3}
                    },
                    {
                        {6, 1, 2, 3},
                        {7, 1, 2, 3}
                    }
                },
                PolyIterator.polyTo3dArray(polyR, 2, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, 4, 2, 2, 2, modulus, polyR, 4, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {7, 6, 1, 2},
                        {8, 7, 1, 2}
                    },
                    {
                        {7, 6, 1, 2},
                        {8, 7, 1, 2}
                    }
                },
                PolyIterator.polyTo3dArray(polyR, 2, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(poly, 4, 2, 2, 3, modulus, polyR, 4, 2);
            Assert.assertArrayEquals(
                new long[][][]{
                    {
                        {8, 7, 6, 1},
                        {9, 8, 7, 1}
                    },
                    {
                        {8, 7, 6, 1},
                        {9, 8, 7, 1}
                    }
                },
                PolyIterator.polyTo3dArray(polyR, 2, 4, 2)
            );
        }
    }
}
