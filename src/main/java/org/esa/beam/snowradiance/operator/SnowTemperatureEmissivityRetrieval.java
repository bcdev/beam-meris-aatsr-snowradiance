package org.esa.beam.snowradiance.operator;

import com.bc.jnn.JnnNet;
import com.bc.jnn.JnnException;

import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowTemperatureEmissivityRetrieval {

    private JnnNet neuralNetWv;

    /**
         *  This method loads required SnowRadiance Auxdata
         *
         * @throws java.io.IOException
         * @throws com.bc.jnn.JnnException
         */
        protected void loadSnowRadianceAuxData() throws IOException, JnnException {
            neuralNetWv = SnowRadianceAuxData.getInstance().loadNeuralNet(SnowRadianceAuxData.NEURAL_NET_WV_OCEAN_MERIS_FILE_NAME);
        }


    /**
     * This method computes the water vapour column to correct for transmission in 3.7um (and 1.6um) channel..
     * Computation by FUB neural net (IDL breadboard step 1.b.1)
     *
     * @param zonalWind - zonalWind
     * @param meridionalWind - meridionalWind
     * @param merisAzimuthDifference - MERIS azimuth difference
     * @param merisViewZenith - MERIS view zenith angle (degree)
     * @param merisSunZenith - MERIS sun zenith angle (degree)
     * @param merisRadiance14 - MERIS radiance band14 angle (degree)
     * @param merisRadiance15 - MERIS radiance band15 angle (degree)
     * @return float
     */
    public float computeWaterVapour(float zonalWind, float meridionalWind,
                                       float merisAzimuthDifference,
                                     float merisViewZenith, float merisSunZenith,
                                     float merisRadiance14, float merisRadiance15) {
        
        float waterVapour = SnowRadianceConstants.WATER_VAPOUR_STANDARD_VALUE;   // standard value

        final double[] nnIn = new double[5];
        final double[] nnOut = new double[1];

        double windSpeed = Math.sqrt(zonalWind*zonalWind + meridionalWind*meridionalWind);

        // apply FUB NN...
        nnIn[0] = windSpeed;
        nnIn[1] = Math.cos(Math.toRadians(merisAzimuthDifference))*
                  Math.sin(Math.toRadians(merisViewZenith));  // angles in degree!
        nnIn[2] = Math.cos(Math.toRadians(merisViewZenith));  // angle in degree!
        nnIn[3] = Math.cos(Math.toRadians(merisSunZenith));  // angle in degree!
        nnIn[4] = Math.log(Math.max(merisRadiance15, 1.0E-4)/Math.max(merisRadiance14, 1.0E-4));

        float[][] nnLimits = new float[][]{{3.75e-02f, 1.84e+01f},
                                           {-6.33e-01f, 6.31e-01f},
                                           {7.73e-01f, 1.00e+00f},
                                           {1.60e-01f, 9.26e-01f},
                                           {-6.98e-01f, 7.62e+00f}};

        for (int i=0; i<nnIn.length; i++) {
            if (nnIn[i] >= nnLimits[i][0] && nnIn[i] >= nnLimits[i][1]) {
                // otherwise do not apply NN, keep WV to standard value
                neuralNetWv.process(nnIn, nnOut);
                waterVapour = (float) nnOut[0];
            }
        }

        return waterVapour;
    }

    /**
         * This method removes ambiguities in azimuth differences.
         *
         * @param viewAzimuth - view azimuth angle (degree)
         * @param sunAzimuth - sun azimuth angle (degree)
         * @return float
         */
        protected static float removeAzimuthDifferenceAmbiguity(float viewAzimuth, float sunAzimuth) {
            float correctedViewAzimuth = viewAzimuth;
            float correctedSunAzimuth = sunAzimuth;

            // first correct for angles < 0.0
            if (correctedViewAzimuth < 0.0) {
                correctedViewAzimuth += 360.0;
            }
            if (correctedSunAzimuth < 0.0) {
                correctedSunAzimuth += 360.0;
            }

            // now correct difference ambiguities
            float correctedAzimuthDifference = correctedViewAzimuth - correctedSunAzimuth;
            if (correctedAzimuthDifference > 180.0) {
                correctedAzimuthDifference = 360.0f - correctedAzimuthDifference;
            }
            if (correctedAzimuthDifference < 0.0) {
                correctedAzimuthDifference = -1.0f* correctedAzimuthDifference;
            }
            return correctedAzimuthDifference;
        }

}
