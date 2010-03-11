package org.esa.beam.snowradiance.operator;

import junit.framework.TestCase;

import java.net.URL;
import java.net.URLDecoder;
import java.io.IOException;

/**
 * Test class for snow grain size retrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8312 $ $Date: 2010-02-09 17:54:10 +0100 (Di, 09 Feb 2010) $
 */
public class SnowGrainSizePollutionTest extends TestCase {
    private String lutPath;

    private static double[] merisWavelengths = new double[] {
        0.4125, 0.4425, 0.490, 0.510, 0.560, 0.620, 0.665, 0.6812, 0.7088, 0.7538, 0.7788, 0.8650, 0.8850
    };

    private static double[] merisReflectances = new double[] {
        0.958, 0.9597, 0.9611, 0.9614, 0.9615, 0.9597, 0.9562, 0.9552, 0.9507, 0.9419, 0.9336, 0.9113, 0.8957
    };

    private static double SZA = 54.0;
    private static double SAA = 0.0;
    private static double VZA = 0.0;
    private static double VAA = 0.0;

    private SnowGrainSizePollutionRetrieval snowGrainSizePollutionRetrieval;

    protected void setUp() {

        try {
            final URL url = getClass().getResource("");
            lutPath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (IOException e) {
            fail("Auxdata cloud not be loaded: " + e.getMessage());
        }
        snowGrainSizePollutionRetrieval = new SnowGrainSizePollutionRetrieval();
    }

    public void testGetJavaFormattedDecimalString() {
        String testString = "0.12345678D+02";
        String javaString = SnowRadianceAuxData.getInstance().getJavaFormattedDecimalString(testString);
        assertNotNull(javaString);
        assertEquals("0.12345678E02", javaString);

        testString = "0.12345678D-02";
        javaString = SnowRadianceAuxData.getInstance().getJavaFormattedDecimalString(testString);
        assertNotNull(javaString);
        assertEquals("0.12345678E-02", javaString);

        testString = "0.12345678";
        javaString = SnowRadianceAuxData.getInstance().getJavaFormattedDecimalString(testString);
        assertNotNull(javaString);
        assertEquals("0.12345678", javaString);
    }

     public void testGetDoubleNumbersFromJavaFormattedDecimalString() {
        String testString = " 0.12345678D+02";
        double[] testResult = SnowRadianceAuxData.getInstance().getDoubleNumbersFromJavaFormattedDecimalString(testString);
        assertEquals(1, testResult.length);
        assertEquals(12.345678, testResult[0], 1.E-4);

        testString = "-0.12345678D+02";
        testResult = SnowRadianceAuxData.getInstance().getDoubleNumbersFromJavaFormattedDecimalString(testString);
        assertEquals(1, testResult.length);
        assertEquals(-12.345678, testResult[0], 1.E-4);

        testString = " 0.22425798D-03 0.32414135D-03 0.87896246D-04-0.19350035D-03-0.48647492D-03-0.10500075D-02";
        testResult = SnowRadianceAuxData.getInstance().getDoubleNumbersFromJavaFormattedDecimalString(testString);
        assertEquals(6, testResult.length);
        assertEquals(0.22425798E-03, testResult[0], 1.E-4);
        assertEquals(0.32414135E-03, testResult[1], 1.E-4);
        assertEquals(0.87896246E-04, testResult[2], 1.E-4);
        assertEquals(-0.19350035E-03, testResult[3], 1.E-4);
        assertEquals(-0.48647492E-03, testResult[4], 1.E-4);
        assertEquals(-0.10500075E-02, testResult[5], 1.E-4);

//         0.19325034E-03-0.97732627E-04
         testString = " 0.19325034D-03-0.97732627D-04";
        testResult = SnowRadianceAuxData.getInstance().getDoubleNumbersFromJavaFormattedDecimalString(testString);
        assertEquals(2, testResult.length);
        assertEquals(0.00019325, testResult[0], 1.E-6);
        assertEquals(-0.0000977326, testResult[1], 1.E-6);

        testString = "0.19325034D-03-0.97732627D-04";
        testResult = SnowRadianceAuxData.getInstance().getDoubleNumbersFromJavaFormattedDecimalString(testString);
        assertEquals(2, testResult.length);
        assertEquals(0.19325034E-03, testResult[0], 1.E-6);
        assertEquals(-0.97732627E-04, testResult[1], 1.E-6);

        testString = " 0.95602055D-03 0.67729125D-03 0.19325034D-03-0.97732627D-04";
        testResult = SnowRadianceAuxData.getInstance().getDoubleNumbersFromJavaFormattedDecimalString(testString);
        assertEquals(4, testResult.length);
        assertEquals(0.00095602, testResult[0], 1.E-6);
        assertEquals(0.00067729, testResult[1], 1.E-6);
        assertEquals(0.0001932, testResult[2], 1.E-6);
        assertEquals(-0.000097732, testResult[3], 1.E-6);

     }

    public void testGetNumberBlockToUseFromReflectanceLut() {
        try {
            int numberBlocksToUse = SnowRadianceAuxData.getInstance().getReflectionLookupTableSize(lutPath);
            assertEquals(2, numberBlocksToUse);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

     public void testReadReflectanceLut() {
        try {
            int numberBlocksToUse = SnowRadianceAuxData.getInstance().getReflectionLookupTableSize(lutPath);
            assertEquals(2, numberBlocksToUse);

            SnowRadianceAuxData.ReflectionLookupTable lut =
                    SnowRadianceAuxData.getInstance().createReflectionLookupTable(SnowRadianceConstants.REFLECTION_LUT_TESTBLOCKS_TO_USE, lutPath);
            assertEquals(2000, lut.getAl1().length);
            assertEquals(1.0, lut.getAlb());
            assertEquals(2, lut.getIflag().length);
            assertEquals(2000, lut.getmMax1());
            assertEquals(100, lut.getnGs());
            assertEquals(1, lut.getnSep());
            assertEquals(100, lut.getX().length);
            assertEquals(2, lut.getR0().length);
            assertEquals(100, lut.getR0()[0].length);
            assertEquals(100, lut.getR0()[0][0].length);
            assertEquals(5146.5698, lut.getR0()[0][0][0], 1.E-4);
            assertEquals(-1.0, lut.getR0()[0][0][1]);
            assertEquals(2499.3018, lut.getR0()[0][1][0], 1.E-4);
            assertEquals(2123.6472, lut.getR0()[0][1][1], 1.E-4);
            assertEquals(1.102361, lut.getR0()[0][99][98], 1.E-4);
            assertEquals(1.1023579, lut.getR0()[0][99][99], 1.E-4);
            assertEquals(0.0, lut.getR0()[1][1][0], 1.E-4);
            assertEquals(0.0, lut.getR0()[1][1][1], 1.E-4);
            assertEquals(0.0, lut.getR0()[1][99][98], 1.E-4);
            assertEquals(0.0, lut.getR0()[1][99][99], 1.E-4);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testComputeReflLut() {
        try {
            int numberBlocksToUse = SnowRadianceAuxData.getInstance().getReflectionLookupTableSize(lutPath);
            assertEquals(2, numberBlocksToUse);

            float sza = 74.5f;
            float vza = 295.5f;
            
            SnowRadianceAuxData.ReflectionLookupTable lut =
                    SnowRadianceAuxData.getInstance().createReflectionLookupTable(SnowRadianceConstants.REFLECTION_LUT_TESTBLOCKS_TO_USE, lutPath);

            long t1 = System.currentTimeMillis();
            // todo:
            // this would be just 100x100 pixels, takes more than 20 seconds :-(
            // we probably need something more efficient than a just 'copy' of the Fortran code...
//            for (int i=0; i<10000; i++) {
//                double reflLut = snowGrainSizePollutionRetrieval.computeReflLut(lut, sza, vza);
//            }
            long t2 = System.currentTimeMillis();
            System.out.println("computation time: " + (t2-t1));

            // todo: set up test once we know some input/output numbers...
            assertEquals(true, true);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testComputeSnowGrainSize() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double pal = snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(reflMeas2, reflMeas12, reflLut, SZA, VZA);
        double grainSize = snowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
        assertEquals(0.058, grainSize, 1.E-3);
    }

    public void testComputePal() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double pal = snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(reflMeas2, reflMeas12, reflLut, SZA, VZA);
        assertEquals(0.1526, pal, 1.E-3);
    }


    public void testComputeSootConcentration() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double pal = snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(reflMeas2, reflMeas12, reflLut, SZA, VZA);
        double grainSize = snowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
        double conc = snowGrainSizePollutionRetrieval.getSootConcentrationInPollutedSnow(reflMeas2, reflLut, SZA, VZA, pal, grainSize);
        
        assertEquals(127.665, conc, 1.E-3);
    }

    public void testComputeSnowAlbedo() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas7 = merisReflectances[6];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double albedo = snowGrainSizePollutionRetrieval.getSnowAlbedo(reflMeas2, reflLut, SZA, VZA);
        assertEquals(0.9664, albedo, 1.E-4);

        albedo = snowGrainSizePollutionRetrieval.getSnowAlbedo(reflMeas7, reflLut, SZA, VZA);
        assertEquals(0.9635, albedo, 1.E-4);

        albedo = snowGrainSizePollutionRetrieval.getSnowAlbedo(reflMeas12, reflLut, SZA, VZA);
        assertEquals(0.9256, albedo, 1.E-4);
    }
}
