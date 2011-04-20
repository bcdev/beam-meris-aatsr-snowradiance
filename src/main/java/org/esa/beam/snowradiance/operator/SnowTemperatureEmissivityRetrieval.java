package org.esa.beam.snowradiance.operator;

import com.bc.jnn.JnnNet;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowTemperatureEmissivityRetrieval {

    public static float getRtmSingle(float waterVapourColumn, float emissivity, float tSfc, float viewZenith,
                                     LookupTable lut) {

        double[] rtmInput = new double[]{-viewZenith, tSfc, emissivity, waterVapourColumn};
        float btToa = (float) (lut.getValue(rtmInput));

        return btToa;
    }

    public static float getToaBTFromRtm(float waterVapourColumn, float emissivity, float tSfc, float viewZenith,
                                        int iwvl,
                                        LookupTable[][] rtmLookupTables, double[] tLowestLayer) {

        final int tsfcUpperIndex = SnowRadianceUtils.getNearestHigherValueIndexInDoubleArray(tSfc, tLowestLayer);
        final int tsfcLowerIndex = SnowRadianceUtils.getNearestLowerValueIndexInDoubleArray(tSfc, tLowestLayer);

        if (tsfcUpperIndex == tsfcLowerIndex) {
            return getRtmSingle(waterVapourColumn, emissivity, tSfc, viewZenith, rtmLookupTables[tsfcUpperIndex][iwvl]);
        }

        final double btToaUpper = SnowTemperatureEmissivityRetrieval.
                getRtmSingle(waterVapourColumn, emissivity, tSfc, viewZenith, rtmLookupTables[tsfcUpperIndex][iwvl]);
        final double btToaLower = SnowTemperatureEmissivityRetrieval.
                getRtmSingle(waterVapourColumn, emissivity, tSfc, viewZenith, rtmLookupTables[tsfcLowerIndex][iwvl]);

        final double wLower = (tLowestLayer[tsfcUpperIndex] - tSfc) / (tLowestLayer[tsfcUpperIndex] - tLowestLayer[tsfcLowerIndex]);
        final double wUpper = 1.0 - wLower;

        float rtm = (float) (wLower * btToaLower + wUpper * btToaUpper);

        return rtm;
    }

    public static float minimizeNewtonForTemperature(double assumedEmissivityAt11Microns, float waterVapourColumn,
                                                     float viewZenith, float aatsrBt11,
                                                     LookupTable[][] rtmLookupTables, double[] tLowestLayer) {
        final float emissivity = (float) assumedEmissivityAt11Microns;
        float tSfcStart = aatsrBt11 + 0.5f;
        if (tSfcStart < SnowRadianceConstants.TSFC_MIN) {
            tSfcStart = SnowRadianceConstants.TSFC_MIN;
        }
        if (tSfcStart > SnowRadianceConstants.TSFC_MAX) {
            tSfcStart = SnowRadianceConstants.TSFC_MAX;
        }
        final float deltaTsfc = 0.1f;  // as in breadboard: inv_aatsr.pro, l.48
        final float thresh = 0.1f;  // as in breadboard: inv_aatsr.pro, l.2
        final double eps = 0.001;
        float tsfc = tSfcStart;

        final int itermax = 5;
        int iter = 0;
        float btToa11 = 100.0f;
        while (Math.abs(
                btToa11 - aatsrBt11) > thresh && aatsrBt11 != SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NODATAVALUE
               && iter < itermax) {
            btToa11 = getToaBTFromRtm(waterVapourColumn, emissivity, tsfc, viewZenith, 0,
                                      rtmLookupTables, tLowestLayer);
            final float tSfcUpper = Math.min(tsfc + deltaTsfc, SnowRadianceConstants.TSFC_MAX);
            final float tSfcLower = Math.max(tsfc - deltaTsfc, SnowRadianceConstants.TSFC_MIN);
            final float btToa11Upper = getToaBTFromRtm(waterVapourColumn, emissivity, tSfcUpper, viewZenith, 0,
                                                       rtmLookupTables, tLowestLayer);
            final float btToa11Lower = getToaBTFromRtm(waterVapourColumn, emissivity, tSfcLower, viewZenith, 0,
                                                       rtmLookupTables, tLowestLayer);
            float derivative = ((btToa11Upper - btToa11Lower) / (tSfcUpper - tSfcLower));
            if (derivative < 0.0) {
                derivative = (float) Math.min(derivative, -eps);
            } else {
                derivative = (float) Math.max(derivative, eps);
            }

            tsfc -= (btToa11 - aatsrBt11) / derivative;
            if (tsfc < SnowRadianceConstants.TSFC_MIN) {
                tsfc = SnowRadianceConstants.TSFC_MIN;
            }
            if (tsfc > SnowRadianceConstants.TSFC_MAX) {
                tsfc = SnowRadianceConstants.TSFC_MAX;
            }
            iter++;
        }

        if (btToa11 < 110.0f || iter == itermax) {
            tsfc = (float) SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NODATAVALUE;
        }
        return tsfc;
    }

    public static float minimizeNewtonForEmissivity(float waterVapourColumn, float viewZenith, float tSfc,
                                                    float aatsrBt12,
                                                    LookupTable[][] rtmLookupTables, double[] tLowestLayer) {
        final float emissivityStart = 0.96f; // as in breadboard: inv_aatsr.pro, l.114
        final float deltaEmi = 0.01f;  // as in breadboard: inv_aatsr.pro, l.11
        final float thresh = 0.01f;    // as in breadboard: inv_aatsr.pro, l.48
        float emissivity = emissivityStart;
        final double eps = 0.001;

        final int itermax = 5;
        int iter = 0;
        float btToa12 = 100.0f;
        while (Math.abs(
                btToa12 - aatsrBt12) > thresh && aatsrBt12 != SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NODATAVALUE
               && iter < itermax) {
            btToa12 = SnowTemperatureEmissivityRetrieval.getToaBTFromRtm(waterVapourColumn, emissivity, tSfc,
                                                                         viewZenith, 1,
                                                                         rtmLookupTables, tLowestLayer);
            final float emisUpper = Math.min(emissivity + deltaEmi, SnowRadianceConstants.EMISSIVITY_MAX);
            final float emisLower = Math.max(emissivity - deltaEmi, SnowRadianceConstants.EMISSIVITY_MIN);
            float btToa12Upper = SnowTemperatureEmissivityRetrieval.getToaBTFromRtm(waterVapourColumn, emisUpper, tSfc,
                                                                                    viewZenith, 1,
                                                                                    rtmLookupTables, tLowestLayer);
            float btToa12Lower = SnowTemperatureEmissivityRetrieval.getToaBTFromRtm(waterVapourColumn, emisLower, tSfc,
                                                                                    viewZenith, 1,
                                                                                    rtmLookupTables, tLowestLayer);
            float derivative = (btToa12Upper - btToa12Lower) / (emisUpper - emisLower);
            if (derivative < 0.0) {
                derivative = (float) Math.min(derivative, -eps);
            } else {
                derivative = (float) Math.max(derivative, eps);
            }

            emissivity -= (btToa12 - aatsrBt12) / derivative;
            if (emissivity < SnowRadianceConstants.EMISSIVITY_MIN) {
                emissivity = SnowRadianceConstants.EMISSIVITY_MIN;
            }
            if (emissivity > SnowRadianceConstants.EMISSIVITY_MAX) {
                emissivity = SnowRadianceConstants.EMISSIVITY_MAX;
            }
            iter++;
        }

        if (btToa12 < 110.0f || iter == itermax) {
            emissivity = (float) SnowRadianceConstants.EMISSIVITY_BAND_NODATAVALUE;
        }
        return emissivity;
    }


    /**
     * This method computes the water vapour column to correct for transmission in 3.7um (and 1.6um) channel..
     * Computation by FUB neural net (IDL breadboard step 1.b.1)
     *
     * @param neuralNetWv            - water vapour neural net
     * @param zonalWind              - zonalWind
     * @param meridionalWind         - meridionalWind
     * @param merisAzimuthDifference - MERIS azimuth difference
     * @param merisViewZenith        - MERIS view zenith angle (degree)
     * @param merisSunZenith         - MERIS sun zenith angle (degree)
     * @param merisRadiance14        - MERIS radiance band14 angle (degree)
     * @param merisRadiance15        - MERIS radiance band15 angle (degree)
     *
     * @return float
     */
    public static float computeWaterVapour(JnnNet neuralNetWv, float zonalWind, float meridionalWind,
                                           float merisAzimuthDifference,
                                           float merisViewZenith, float merisSunZenith,
                                           float merisRadiance14, float merisRadiance15) {

        float waterVapour = SnowRadianceConstants.WATER_VAPOUR_STANDARD_VALUE;   // standard value

        final double[] nnIn = new double[5];
        final double[] nnOut = new double[1];

        final double windSpeed = Math.sqrt(zonalWind * zonalWind + meridionalWind * meridionalWind);

        // apply FUB NN...
        nnIn[0] = windSpeed;
        nnIn[1] = Math.cos(Math.toRadians(merisAzimuthDifference)) *
                  Math.sin(Math.toRadians(merisViewZenith));  // angles in degree!
        nnIn[2] = Math.cos(Math.toRadians(merisViewZenith));  // angle in degree!
        nnIn[3] = Math.cos(Math.toRadians(merisSunZenith));  // angle in degree!
        nnIn[4] = Math.log(Math.max(merisRadiance15, 1.0E-4) / Math.max(merisRadiance14, 1.0E-4));

        final float[][] nnLimits = new float[][]{
                {3.75e-02f, 1.84e+01f},
                {-6.33e-01f, 6.31e-01f},
                {7.73e-01f, 1.00e+00f},
                {1.60e-01f, 9.26e-01f},
                {-6.98e-01f, 7.62e+00f}
        };

        boolean applyNetWv = true;
        for (int i = 0; i < nnIn.length; i++) {
            if (nnIn[i] < nnLimits[i][0] || nnIn[i] > nnLimits[i][1]) {
                // if any input is out of NN range, do not apply NN, keep WV to standard value
                applyNetWv = false;
                break;
            }
        }
        if (applyNetWv) {
            neuralNetWv.process(nnIn, nnOut);
            waterVapour = (float) nnOut[0];
        }

        return waterVapour;
    }

    /**
     * This method removes ambiguities in azimuth differences.
     *
     * @param viewAzimuth - view azimuth angle (degree)
     * @param sunAzimuth  - sun azimuth angle (degree)
     *
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
            correctedAzimuthDifference = -1.0f * correctedAzimuthDifference;
        }
        return correctedAzimuthDifference;
    }

}
