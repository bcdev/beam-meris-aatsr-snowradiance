package org.esa.beam.snowradiance.operator;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Test class for snow grain size retrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8312 $ $Date: 2010-02-09 17:54:10 +0100 (Di, 09 Feb 2010) $
 */
public class SnowGrainSizePollutionTest extends TestCase {

    private String lutPath;

    private static double[] merisWavelengths = new double[]{
            0.4125, 0.4425, 0.490, 0.510, 0.560, 0.620, 0.665, 0.6812, 0.7088, 0.7538, 0.7788, 0.8650, 0.8850
    };

    private static double[] merisReflectances = new double[]{
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

    public void testComputeSnowGrainSize() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double pal = snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(reflMeas2, reflMeas12, reflLut, SZA,
                                                                                 VZA);
        double grainSize = snowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
        assertEquals(0.058, grainSize, 1.E-3);
    }

    public void testComputePal() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double pal = snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(reflMeas2, reflMeas12, reflLut, SZA,
                                                                                 VZA);
        assertEquals(0.1526, pal, 1.E-3);
    }


    public void testComputeSootConcentration() {
        double reflMeas2 = merisReflectances[1];
        double reflMeas12 = merisReflectances[11];
        double reflLut = snowGrainSizePollutionRetrieval.computeReflLutApprox(SAA, SZA, VAA, VZA);

        double pal = snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(reflMeas2, reflMeas12, reflLut, SZA,
                                                                                 VZA);
        double grainSize = snowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
        double conc = snowGrainSizePollutionRetrieval.getSootConcentrationInPollutedSnow(reflMeas2, reflLut, SZA, VZA,
                                                                                         grainSize);

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

    public void testComputeArcTheta() throws Exception {
        //To change body of created methods use File | Settings | File Templates.
        double SAA = 52.9;
        double SZA = 71.7;
        double VAA = 296.2;
        double VZA = 26.4;

        assertEquals(118.0944, SnowGrainSizePollutionRetrieval.computeArcTheta(SAA, SZA, VAA, VZA), 1.E-3);
    }
}
