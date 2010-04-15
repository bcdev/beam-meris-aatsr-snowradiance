package org.esa.beam.snowradiance.operator;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

import org.esa.beam.util.math.LookupTable;
import org.esa.beam.snowradiance.operator.SnowRadianceAuxData;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;

/**
 * Test class for snow temperature and emissivity retrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowTemperatureFubOpTest extends TestCase {

    private String lutPath;

    protected void setUp() {

        try {
            final URL url = getClass().getResource("");
            lutPath = URLDecoder.decode(url.getPath(), "UTF-8");
        } catch (IOException e) {
            fail("Auxdata cloud not be loaded: " + e.getMessage());
        }
    }


    public void testCreateRtmLutsFromNetcdf() throws IOException {

        LookupTable[][] rtmLookupTables;
        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmLookupTables(lutPath);
            assertNotNull(rtmLookupTables);
            assertEquals(4, rtmLookupTables.length);
            assertNotNull(rtmLookupTables[0][0]);
            assertNotNull(rtmLookupTables[0][0].getDimensions());
            assertEquals(4,rtmLookupTables[0][0].getDimensions().length);
            assertEquals(9,rtmLookupTables[0][0].getDimensions()[0].getSequence().length);
            assertEquals(33,rtmLookupTables[0][0].getDimensions()[1].getSequence().length);
            assertEquals(11,rtmLookupTables[0][0].getDimensions()[2].getSequence().length);
            assertEquals(21,rtmLookupTables[0][0].getDimensions()[3].getSequence().length);
//            for (int i=0; i<rtmLookupTables[0][0][0].getDimensions()[2].getSequence().length; i++) {
//                System.out.println("SEQUENCE: " + i + "," +
//                rtmLookupTables[0][0][0].getDimensions()[2].getSequence()[i]);
//            }
            assertEquals(243.15,rtmLookupTables[0][0].getDimensions()[1].getSequence()[0], 1.E-3);
            assertEquals(244.1187,rtmLookupTables[0][0].getDimensions()[1].getSequence()[1], 1.E-3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testGetNearestValueIndices() {
        double[] array = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};

        double x = 2.5;
        int indexHigh = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(x, array);
        assertEquals(2, indexHigh);
        int indexLow = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(x, array);
        assertEquals(1, indexLow);

        x = 3.0;
        indexHigh = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(x, array);
        assertEquals(3, indexHigh);
        indexLow = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(x, array);
        assertEquals(1, indexLow);

        x = 1.0;
        indexHigh = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(x, array);
        assertEquals(1, indexHigh);
        indexLow = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(x, array);
        assertEquals(0, indexLow);

        x = 5.0;
        indexHigh = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(x, array);
        assertEquals(4, indexHigh);
        indexLow = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(x, array);
        assertEquals(3, indexLow);

        x = 6.0;
        indexHigh = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(x, array);
        assertEquals(4, indexHigh);
        indexLow = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(x, array);
        assertEquals(4, indexLow);

        x = -1.0;
        indexHigh = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(x, array);
        assertEquals(0, indexHigh);
        indexLow = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(x, array);
        assertEquals(0, indexLow);
    }

    public void testGetTsfcFromLut() {
        double[][][] tsfcLut;

        try {
            tsfcLut = SnowRadianceAuxData.getTsfcFromLookupTables(lutPath);
            assertEquals(272.2, tsfcLut[0][0][24], 1.E-3);
            assertEquals(257.1, tsfcLut[1][0][24], 1.E-3);
            assertEquals(272.39, tsfcLut[2][0][24], 1.E-3);
            assertEquals(241.95, tsfcLut[3][0][24], 1.E-3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testGetUpperLowerIndices() {
        double[][][] tsfcLut;
        double[] tLowestLayer = new double[4];

        try {
            tsfcLut = SnowRadianceAuxData.getTsfcFromLookupTables(lutPath);
            for (int i=0; i<SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
                tLowestLayer[i] = tsfcLut[i][0][24];
            }

            final int tsfcUpperIndex = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(250.92f, tLowestLayer);
            assertEquals(1,tsfcUpperIndex);
            final int tsfcLowerIndex = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(250.92f, tLowestLayer);
            assertEquals(3,tsfcLowerIndex);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testGetRtmSingle() {

        LookupTable[][] rtmLookupTables;
        final float tSfc = 260.0f;
        final float viewAngle = 40.0f;
        final float esissivity = 0.98f;
        final float waterVapour = 14.0f;

        float[][] btToa = new float[SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES][SnowRadianceConstants.NUMBER_AATSR_WVL];

        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmLookupTables(lutPath);
            for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
                for (int j = 0; j < SnowRadianceConstants.NUMBER_AATSR_WVL; j++) {
                    btToa[i][j] = SnowTemperatureEmissivityRetrieval.getRtmSingle(waterVapour, esissivity, tSfc, viewAngle, rtmLookupTables[i][j]);
                }
            }
            assertEquals(259.587f, btToa[0][0], 1.E-3);
            assertEquals(258.498f, btToa[1][0], 1.E-3);
            assertEquals(260.241f, btToa[2][0], 1.E-3);
            assertEquals(258.124f, btToa[3][0], 1.E-3);
            assertEquals(259.761f, btToa[0][1], 1.E-3);
            assertEquals(257.757f, btToa[1][1], 1.E-3);
            assertEquals(260.681f, btToa[2][1], 1.E-3);
            assertEquals(256.787f, btToa[3][1], 1.E-3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    public void testGetRtmValue() {
        LookupTable[][] rtmLookupTables;
        double[] tLowestLayer = new double[4];
        double[][][] tsfcLut;
        final float tSfc = 260.0f;
        final float viewAngle = 40.0f;
        final float esissivity = 0.98f;
        final float waterVapour = 14.0f;

        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmLookupTables(lutPath);
            tsfcLut = SnowRadianceAuxData.getTsfcFromLookupTables(lutPath);
            for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
                tLowestLayer[i] = tsfcLut[i][0][24];
            }
            float btToa = SnowTemperatureEmissivityRetrieval.getToaBTFromRtm(waterVapour, esissivity, tSfc, viewAngle, 0,
                                                                             rtmLookupTables, tLowestLayer);
            assertEquals(258.707f, btToa, 1.E-3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

     public void testMinimizeNewtonTemperature() {
        LookupTable[][] rtmLookupTables;
        double[] tLowestLayer = new double[4];
        double[][][] tsfcLut;
        final float viewAngle = 20.9755f;
        final float waterVapour = 3.6498f;
        final float bt11 = 250.42f;

        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmLookupTables(lutPath);
            tsfcLut = SnowRadianceAuxData.getTsfcFromLookupTables(lutPath);
            for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
                tLowestLayer[i] = tsfcLut[i][0][24];
            }
            float temp = SnowTemperatureEmissivityRetrieval.minimizeNewtonForTemperature(waterVapour, viewAngle, bt11,
                                                                             rtmLookupTables, tLowestLayer);
            assertEquals(250.945f, temp, 1.E-3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

     public void testMinimizeNewtonEmissivity() {
        LookupTable[][] rtmLookupTables;
        double[] tLowestLayer = new double[4];
        double[][][] tsfcLut;
        final float viewAngle = 20.9755f;
        final float waterVapour = 3.6498f;
        final float tsfc = 250.945f;
        final float bt12 = 250.010f;

        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmLookupTables(lutPath);
            tsfcLut = SnowRadianceAuxData.getTsfcFromLookupTables(lutPath);
            for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
                tLowestLayer[i] = tsfcLut[i][0][24];
            }
            float emis = SnowTemperatureEmissivityRetrieval.minimizeNewtonForEmissivity(waterVapour, viewAngle, tsfc, bt12,
                                                                             rtmLookupTables, tLowestLayer);
            assertEquals(0.9837, emis, 1.E-3);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
