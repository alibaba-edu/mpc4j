package edu.alibaba.mpc4j.crypto.fhe.seal.rq;

import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.CoeffIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.PolyIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.iterator.RnsIterator;
import edu.alibaba.mpc4j.crypto.fhe.seal.modulus.Modulus;
import org.junit.Assert;
import org.junit.Test;

/**
 * Polynomial Arithmetic Small Mod unit tests.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/main/native/tests/seal/util/polyarithsmallmod.cpp">
 * polyarithsmallmod.cpp
 * </a>.
 *
 * @author Anony_Trent, Liqiang Peng
 * @date 2023/8/20
 */
public class PolyArithmeticSmallModTest {

    @Test
    public void testModuloPolyCoeffs() {
        {
            long[] poly = new long[]{2, 15, 77};
            CoeffIterator coeffIterator = CoeffIterator.wrap(poly);
            Modulus modulus = new Modulus(15);
            PolyArithmeticSmallMod.moduloPolyCoeff(coeffIterator, 3, modulus, coeffIterator);
            Assert.assertArrayEquals(new long[]{2, 0, 2}, coeffIterator.coeff());
        }
        {
            long[][] poly = new long[][]{
                {2, 15, 77},
                {2, 15, 77}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            RnsIterator rnsIterator = RnsIterator.wrap(
                RnsIterator.from1dArray(poly),
                3,
                2
            );

            PolyArithmeticSmallMod.moduloPolyCoeffRns(rnsIterator, 2, modulus, rnsIterator);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 2},
                    {2, 0, 2}},
                rnsIterator.to2dArray()
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
            long[] result = PolyIterator.createFrom3dArray(poly);

            PolyIterator polyIterator = PolyIterator.wrap(result, 2, 3, 2);

            PolyArithmeticSmallMod.moduloPolyCoeffPoly(polyIterator, 2, modulus, polyIterator);
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
                PolyIterator.to3dArray(polyIterator.coeff(), 2, 3, 2)
            );
        }
    }

    @Test
    public void testNegatePolyCoeffs() {
        {
            Modulus modulus = new Modulus(15);
            long[] poly = new long[]{2, 3, 4};

            CoeffIterator coeffIterator = CoeffIterator.wrap(poly);

            PolyArithmeticSmallMod.negatePolyCoeffMod(coeffIterator, 3, modulus, coeffIterator);
            Assert.assertArrayEquals(new long[]{13, 12, 11}, poly);

            poly = new long[]{2, 3, 4};
            coeffIterator = CoeffIterator.wrap(poly);
            modulus = new Modulus(0xFFFFFFFFFFFFFFL);
            PolyArithmeticSmallMod.negatePolyCoeffMod(coeffIterator, 3, modulus, coeffIterator);
            Assert.assertArrayEquals(new long[]{0xFFFFFFFFFFFFFDL, 0xFFFFFFFFFFFFFCL, 0xFFFFFFFFFFFFFBL}, coeffIterator.coeff());
        }
        {
            long[][] poly = new long[][]{
                {2, 3, 4},
                {2, 0, 1}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{15, 3});
            long[] rns = RnsIterator.from1dArray(poly);

            RnsIterator rnsIterator = RnsIterator.wrap(rns, 3, 2);

            PolyArithmeticSmallMod.negatePolyCoeffModRns(rnsIterator, 2, modulus, rnsIterator);
            Assert.assertArrayEquals(
                new long[][]{
                    {13, 12, 11},
                    {1, 0, 2}},
                rnsIterator.to2dArray()
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
            long[] result = PolyIterator.createFrom3dArray(poly);

            PolyIterator polyIterator = PolyIterator.wrap(result, 2, 3, 2);


            PolyArithmeticSmallMod.negatePolyCoeffModPoly(polyIterator, 2, modulus, polyIterator);
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
                PolyIterator.to3dArray(polyIterator.coeff(), 2, 3, 2)
            );
        }
    }

    @Test
    public void testAddPolyCoeffMod() {
        {
            long[] poly1 = new long[]{1, 3, 4};
            long[] poly2 = new long[]{1, 2, 4};
            Modulus modulus = new Modulus(5);

            CoeffIterator poly1Iterator = CoeffIterator.wrap(poly1);
            CoeffIterator poly2Iterator = CoeffIterator.wrap(poly2);

            PolyArithmeticSmallMod.addPolyCoeffMod(poly1Iterator, poly2Iterator, 3, modulus, poly1Iterator);
            Assert.assertArrayEquals(new long[]{2, 0, 3}, poly1Iterator.coeff());
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
            long[] rns1 = RnsIterator.from1dArray(poly1);
            long[] rns2 = RnsIterator.from1dArray(poly2);

            RnsIterator rnsIterator1 = RnsIterator.wrap(rns1, 3, 2);
            RnsIterator rnsIterator2 = RnsIterator.wrap(rns2, 3, 2);


            PolyArithmeticSmallMod.addPolyCoeffMod(rnsIterator1, rnsIterator2, 2, modulus, rnsIterator1);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 0, 3},
                    {2, 2, 2}
                },
                rnsIterator1.to2dArray()
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
            long[] result1 = PolyIterator.createFrom3dArray(poly1);
            long[] result2 = PolyIterator.createFrom3dArray(poly2);


            PolyIterator polyIterator1 = PolyIterator.wrap(result1, 2, 3, 2);
            PolyIterator polyIterator2 = PolyIterator.wrap(result2, 2, 3, 2);


            PolyArithmeticSmallMod.addPolyCoeffModPoly(polyIterator1, polyIterator2, 2, modulus, polyIterator1);
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
                PolyIterator.to3dArray(polyIterator1.coeff(), 2, 3, 2)
            );
        }
    }

    @Test
    public void testSubPolyCoeffMod() {
        {
            long[] poly1 = new long[]{4, 3, 2};
            long[] poly2 = new long[]{2, 3, 4};

            CoeffIterator coeffIterator1 = CoeffIterator.wrap(poly1);
            CoeffIterator coeffIterator2 = CoeffIterator.wrap(poly2);

            Modulus modulus = new Modulus(5);
            PolyArithmeticSmallMod.subPolyCoeffMod(coeffIterator1, coeffIterator2, 3, modulus, coeffIterator1);
            Assert.assertArrayEquals(new long[]{2, 0, 3}, coeffIterator1.coeff());
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
            long[] rns1 = RnsIterator.from1dArray(poly1);
            long[] rns2 = RnsIterator.from1dArray(poly2);

            RnsIterator rnsIterator1 = RnsIterator.wrap(rns1, 3, 2);
            RnsIterator rnsIterator2 = RnsIterator.wrap(rns2, 3, 2);

            PolyArithmeticSmallMod.subPolyCoeffMod(rnsIterator1, rnsIterator2, 2, modulus, rnsIterator1);
            Assert.assertArrayEquals(
                new long[][]{
                    {0, 1, 0},
                    {1, 0, 2}
                },
                rnsIterator1.to2dArray()
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
            long[] poly1 = PolyIterator.createFrom3dArray(data1);
            long[] poly2 = PolyIterator.createFrom3dArray(data2);

            PolyIterator polyIterator1 = PolyIterator.wrap(poly1, 2, 3, 2);
            PolyIterator polyIterator2 = PolyIterator.wrap(poly2, 2, 3, 2);

            PolyArithmeticSmallMod.subPolyCoeffModPoly(polyIterator1, polyIterator2, 2, modulus, polyIterator1);
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
                PolyIterator.to3dArray(polyIterator1.coeff(), 2, 3, 2)
            );
        }
    }

    @Test
    public void testMultiplyPolyScalarCoeffMod() {
        {
            long[] coeff = new long[]{1, 3, 4};
            Modulus modulus = new Modulus(5);

            CoeffIterator coeffIterator = CoeffIterator.wrap(coeff);
            long scalar = 3;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(coeffIterator, 3, scalar, modulus, coeffIterator);
            Assert.assertArrayEquals(new long[]{3, 4, 2}, coeffIterator.coeff());
        }
        {
            long[][] data = new long[][]{
                {1, 3, 4},
                {1, 0, 2}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 3});
            long[] rns = RnsIterator.from1dArray(data);

            RnsIterator rnsIterator = RnsIterator.wrap(rns, 3, 2);


            long scalar = 2;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(rnsIterator, 2, scalar, modulus, rnsIterator);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 1, 3},
                    {2, 0, 1}
                },
                rnsIterator.to2dArray()
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
            long[] poly = PolyIterator.createFrom3dArray(data);

            PolyIterator polyIterator = PolyIterator.wrap(poly, 2, 3, 2);


            long scalar = 2;
            PolyArithmeticSmallMod.multiplyPolyScalarCoeffMod(polyIterator, 2, scalar, modulus, polyIterator);
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
                PolyIterator.to3dArray(polyIterator.coeff(), 2, 3, 2)
            );
        }
    }


    @Test
    public void testMultiplyPolyMonoCoeffMod() {
        {
            long[] coeff = new long[]{1, 3, 4, 2};
            Modulus modulus = new Modulus(5);
            long[] result = new long[4];


            CoeffIterator coeffIterator = CoeffIterator.wrap(coeff);
            CoeffIterator resultIterator = CoeffIterator.wrap(result);

            long monoCoeff = 3;
            int monoExponent = 0;
            // n = 1, 1 * 3 mod 5 = 3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 1, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{3, 0, 0, 0}, resultIterator.coeff());

            // n = 2, (1 + 3x) * 3 mod 5 = 3 + 4x
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 2, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{3, 4, 0, 0}, resultIterator.coeff());

            monoExponent = 1;
            // n = 2, (1 + 3x) * 3x mod 5 = (3x - 9) mod 5 = 1 + 3x
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 2, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{1, 3, 0, 0}, resultIterator.coeff());

            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (3x) = (3x + 9x^2 + 12x^3 - 6) mod 5 = 4 + 3x + 4x^2 + 2x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 4, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{4, 3, 4, 2}, resultIterator.coeff());

            monoCoeff = 1;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (x) mod 5 = (x + 3x^2 + 4x^3 - 2) mod 5 = 3 + x + 3x^2 + 4x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 4, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{3, 1, 3, 4}, resultIterator.coeff());

            monoCoeff = 4;
            monoExponent = 3;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (4x^3) mod 5 = (4x^3 - 12 - 16x - 8x^2) mod 5 = 3 + 4x + 2x^2 + 4x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 4, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{3, 4, 2, 4}, resultIterator.coeff());

            monoCoeff = 1;
            monoExponent = 0;
            // n = 4, (1 + 3x + 4x^2 + 2x^3) * (1) mod 5 = 1 + 3x + 4x^2 + 2x^3
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffMod(coeffIterator, 4, monoCoeff, monoExponent, modulus, resultIterator);
            Assert.assertArrayEquals(new long[]{1, 3, 4, 2}, resultIterator.coeff());
        }
        {
            long[][] data = new long[][]{
                {1, 3, 4, 2},
                {1, 3, 4, 2}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{5, 7});
            long[] rns = RnsIterator.from1dArray(data);
            long[] rnsR = RnsIterator.allocateArray(4, 2);

            RnsIterator rnsIterator = RnsIterator.wrap(rns, 4, 2);
            RnsIterator rnsIteratorR = RnsIterator.wrap(rnsR, 4, 2);

            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModRns(
                rnsIterator, 2, monoCoeff, monoExponent, modulus, rnsIteratorR
            );
            Assert.assertArrayEquals(
                new long[][]{
                    {4, 2, 4, 2},
                    {5, 6, 4, 5}
                },
                rnsIteratorR.to2dArray()
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
            long[] poly = PolyIterator.createFrom3dArray(data);
            long[] polyR = PolyIterator.allocateArray(2, 4, 2);

            PolyIterator polyIterator = PolyIterator.wrap(poly, 2, 4, 2);
            PolyIterator polyIteratorR = PolyIterator.wrap(polyR, 2, 4, 2);


            long monoCoeff = 4;
            int monoExponent = 2;
            PolyArithmeticSmallMod.negacyclicMultiplyPolyMonoCoeffModPoly(
                polyIterator, 2, monoCoeff, monoExponent, modulus, polyIteratorR
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
                PolyIterator.to3dArray(polyIteratorR.coeff(), 2, 4, 2)
            );
        }
    }

    @Test
    public void testDyadicProductCoeffMod() {
        {
            long[] coeff1 = new long[]{1, 1, 1};
            long[] coeff2 = new long[]{2, 3, 4};
            long[] coeffR = new long[3];

            CoeffIterator coeffIterator1 = CoeffIterator.wrap(coeff1);
            CoeffIterator coeffIterator2 = CoeffIterator.wrap(coeff2);
            CoeffIterator coeffIterator3 = CoeffIterator.wrap(coeffR);

            Modulus modulus = new Modulus(13);
            PolyArithmeticSmallMod.dyadicProductCoeffMod(coeffIterator1, coeffIterator2, 3, modulus, coeffIterator3);
            Assert.assertArrayEquals(new long[]{2, 3, 4}, coeffIterator3.coeff());
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
            long[] rns1 = RnsIterator.from1dArray(data1);
            long[] rns2 = RnsIterator.from1dArray(data2);
            long[] rnsR = RnsIterator.allocateArray(3, 2);

            RnsIterator rnsIterator1 = RnsIterator.wrap(rns1, 3, 2);
            RnsIterator rnsIterator2 = RnsIterator.wrap(rns2, 3, 2);
            RnsIterator rnsIterator3 = RnsIterator.wrap(rnsR, 3, 2);

            PolyArithmeticSmallMod.dyadicProductCoeffMod(rnsIterator1, rnsIterator2, 2, modulus, rnsIterator3);
            Assert.assertArrayEquals(
                new long[][]{
                    {2, 6, 4},
                    {4, 3, 1}
                },
                rnsIterator3.to2dArray()
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
            long[] poly1 = PolyIterator.createFrom3dArray(data1);
            long[] poly2 = PolyIterator.createFrom3dArray(data2);
            long[] polyR = PolyIterator.allocateArray(2, 3, 2);

            PolyIterator polyIterator1 = PolyIterator.wrap(poly1, 2, 3, 2);
            PolyIterator polyIterator2 = PolyIterator.wrap(poly2, 2, 3, 2);
            PolyIterator polyIterator3 = PolyIterator.wrap(polyR, 2, 3, 2);


            PolyArithmeticSmallMod.dyadicProductCoeffModPoly(polyIterator1, polyIterator2, 2, modulus, polyIterator3);
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
                PolyIterator.to3dArray(polyIterator3.coeff(), 2, 3, 2)
            );
        }
    }

    @Test
    public void testPolyInftyNormCoeffMod() {
        Modulus mod = new Modulus(10);

        long[] poly = new long[]{0, 1, 2, 3};
        CoeffIterator coeffIterator = CoeffIterator.wrap(poly);
        Assert.assertEquals(3, PolyArithmeticSmallMod.polyInftyNormCoeffMod(coeffIterator, 4, mod));

        poly = new long[]{0, 1, 2, 8};
        coeffIterator = CoeffIterator.wrap(poly);
        Assert.assertEquals(2, PolyArithmeticSmallMod.polyInftyNormCoeffMod(coeffIterator, 4, mod));
    }

    @Test
    public void testNegacyclicShiftPolyCoeffMod() {
        {
            long[] coeff = new long[4];
            long[] coeffR = new long[4];
            Modulus modulus = new Modulus(10);

            CoeffIterator coeffIterator = CoeffIterator.wrap(coeff);
            CoeffIterator coeffIteratorR = CoeffIterator.wrap(coeffR);


            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 0, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffIteratorR.coeff());
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 1, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffIteratorR.coeff());
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 2, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffIteratorR.coeff());
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 3, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{0, 0, 0, 0}, coeffIteratorR.coeff());

            coeff = new long[]{1, 2, 3, 4};
            coeffIterator = CoeffIterator.wrap(coeff);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 0, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{1, 2, 3, 4}, coeffIteratorR.coeff());
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 1, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{6, 1, 2, 3}, coeffIteratorR.coeff());
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 2, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{7, 6, 1, 2}, coeffIteratorR.coeff());
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator, 4, 3, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{8, 7, 6, 1}, coeffIteratorR.coeff());

            coeff = new long[]{1, 2, 3, 4};
            int n1 = 2;
            long[] coeff1 = new long[n1];
            coeffR = new long[n1];
            System.arraycopy(coeff, 0, coeff1, 0, n1);
            CoeffIterator coeffIterator1 = CoeffIterator.wrap(coeff1);
            coeffIteratorR = CoeffIterator.wrap(coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator1, n1, 1, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{8, 1}, coeffIteratorR.coeff());

            int n2 = 2;
            long[] coeff2 = new long[n2];
            coeffR = new long[n2];
            System.arraycopy(coeff, 2, coeff2, 0, n2);
            CoeffIterator coeffIterator2 = CoeffIterator.wrap(coeff2);
            coeffIteratorR = CoeffIterator.wrap(coeffR);
            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffMod(coeffIterator2, n2, 1, modulus, coeffIteratorR);
            Assert.assertArrayEquals(new long[]{6, 3}, coeffIteratorR.coeff());
        }
        {
            long[][] data = new long[][]{
                {1, 2, 3, 4},
                {1, 2, 3, 4}
            };
            Modulus[] modulus = Modulus.createModulus(new long[]{10, 11});
            long[] rns = RnsIterator.from1dArray(data);
            long[] rnsR = RnsIterator.allocateArray(4, 2);

            RnsIterator rnsIterator = RnsIterator.wrap(rns, 4, 2);
            RnsIterator rnsIteratorR = RnsIterator.wrap(rnsR, 4, 2);

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rnsIterator, 2, 0, modulus, rnsIteratorR);
            Assert.assertArrayEquals(
                new long[][]{
                    {1, 2, 3, 4},
                    {1, 2, 3, 4}
                },
                rnsIteratorR.to2dArray()
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rnsIterator, 2, 1, modulus, rnsIteratorR);
            Assert.assertArrayEquals(
                new long[][]{
                    {6, 1, 2, 3},
                    {7, 1, 2, 3}
                },
                rnsIteratorR.to2dArray()
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rnsIterator, 2, 2, modulus, rnsIteratorR);
            Assert.assertArrayEquals(
                new long[][]{
                    {7, 6, 1, 2},
                    {8, 7, 1, 2}
                },
                rnsIteratorR.to2dArray()
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModRns(rnsIterator, 2, 3, modulus, rnsIteratorR);
            Assert.assertArrayEquals(
                new long[][]{
                    {8, 7, 6, 1},
                    {9, 8, 7, 1}
                },
                rnsIteratorR.to2dArray()
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
            long[] poly = PolyIterator.createFrom3dArray(data);
            long[] polyR = PolyIterator.allocateArray(2, 4, 2);

            PolyIterator polyIterator = PolyIterator.wrap(poly, 2, 4, 2);
            PolyIterator polyIteratorR = PolyIterator.wrap(polyR, 2, 4, 2);


            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(polyIterator, 2, 0, modulus, polyIteratorR);


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
                PolyIterator.to3dArray(polyIteratorR.coeff(), 2, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(polyIterator, 2, 1, modulus, polyIteratorR);
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
                PolyIterator.to3dArray(polyIteratorR.coeff(), 2, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(polyIterator, 2, 2, modulus, polyIteratorR);
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
                PolyIterator.to3dArray(polyIteratorR.coeff(), 2, 4, 2)
            );

            PolyArithmeticSmallMod.negacyclicShiftPolyCoeffModPoly(polyIterator, 2, 3, modulus, polyIteratorR);
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
                PolyIterator.to3dArray(polyIteratorR.coeff(), 2, 4, 2)
            );
        }
    }
}
