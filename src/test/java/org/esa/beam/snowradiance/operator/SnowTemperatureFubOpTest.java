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

        LookupTable[][][] rtmLookupTables;
        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmOceanLookupTables(lutPath);
            assertNotNull(rtmLookupTables);
            assertEquals(2, rtmLookupTables.length);
            assertEquals(4, rtmLookupTables[0].length);
            assertNotNull(rtmLookupTables[0][0][0]);
            assertNotNull(rtmLookupTables[0][0][0].getDimensions());
            assertEquals(4,rtmLookupTables[0][0][0].getDimensions().length);
            assertEquals(9,rtmLookupTables[0][0][0].getDimensions()[0].getSequence().length);
            assertEquals(33,rtmLookupTables[0][0][0].getDimensions()[1].getSequence().length);
            assertEquals(11,rtmLookupTables[0][0][0].getDimensions()[2].getSequence().length);
            assertEquals(21,rtmLookupTables[0][0][0].getDimensions()[3].getSequence().length);
//            for (int i=0; i<rtmLookupTables[0][0][0].getDimensions()[2].getSequence().length; i++) {
//                System.out.println("SEQUENCE: " + i + "," +
//                rtmLookupTables[0][0][0].getDimensions()[2].getSequence()[i]);
//            }
            assertEquals(243.15,rtmLookupTables[0][0][0].getDimensions()[1].getSequence()[0], 1.E-3);
            assertEquals(244.1187,rtmLookupTables[0][0][0].getDimensions()[1].getSequence()[1], 1.E-3);
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
    }
}
