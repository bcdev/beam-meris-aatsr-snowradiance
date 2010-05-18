package org.esa.beam.snowradiance.util;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Utility class for snow radiance algorithms
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowRadianceUtils {

    private static final String[] REQUIRED_MERIS_TPG_NAMES = {
            EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME,
            EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME,
            EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME,
            EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME,
            EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME,
            "atm_press",
            "ozone",
    };

    private static final String[] REQUIRED_AATSR_TPG_NAMES =
            EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES;

    /**
     * This method computed the index of the nearest higher value in a float array
     * compared to a given input float value
     *
     * @param x     - input value
     * @param array - the float array
     * @return int
     */
    public static int getNearestHigherValueIndexInDoubleArray(double x, double[] array) {
        int nearestValueIndex = -1;
        double big = Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (x < array[i]) {
                if (array[i] - x < big) {
                    big = array[i] - x;
                    nearestValueIndex = i;
                }
            }
        }
        // special boundary case:
        if (x >= array[array.length - 1] && nearestValueIndex == -1) {
            nearestValueIndex = array.length - 1;
        }

        return nearestValueIndex;
    }

    public static int getNearestLowerValueIndexInDoubleArray(double x, double[] array) {
        int nearestValueIndex = -1;
        double big = Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (x > array[i]) {
                if (x - array[i] < big) {
                    big = x - array[i];
                    nearestValueIndex = i;
                }
            }
        }
        // special boundary case:
        if (x <= array[0] && nearestValueIndex == -1) {
            nearestValueIndex = 0;
        }

        return nearestValueIndex;
    }

    public static void validateMerisProduct(final Product merisProduct) {
        final String missedBand = validateMerisProductBands(merisProduct);
        if (!missedBand.isEmpty()) {
            String message = MessageFormat.format("Missing required band in MERIS input product: {0} . Not a L1b product?",
                                                  missedBand);
            throw new OperatorException(message);
        }
        final String missedTPG = validateMerisProductTpgs(merisProduct);
        if (!missedTPG.isEmpty()) {
            String message = MessageFormat.format("Missing required tie-point grid in MERIS input product: {0} . Not a L1b product?",
                                                  missedTPG);
            throw new OperatorException(message);
        }
    }

    public static void validateAatsrProduct(final Product aatsrProduct) {
        if (aatsrProduct != null) {
            final String missedBand = validateAatsrProductBands(aatsrProduct);
            if (!missedBand.isEmpty()) {
                String message = MessageFormat.format("Missing required band in AATSR input product: {0} . Not a L1b product?",
                                                      missedBand);
                throw new OperatorException(message);
            }
            final String missedTPG = validateAatsrProductTpgs(aatsrProduct);
            if (!missedTPG.isEmpty()) {
                String message = MessageFormat.format("Missing required tie-point grid in AATSR input product: {0} . Not a L1b product?",
                                                      missedTPG);
                throw new OperatorException(message);
            }
        }
    }

    private static String validateMerisProductBands(Product product) {
        List<String> sourceBandNameList = Arrays.asList(product.getBandNames());
        for (String bandName : EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES) {
            if (!sourceBandNameList.contains(bandName)) {
                return bandName;
            }
        }
        if (!sourceBandNameList.contains(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME)) {
            return EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME;
        }

        return "";
    }

    private static String validateAatsrProductBands(Product product) {
        List<String> sourceBandNameList = Arrays.asList(product.getBandNames());
        for (String bandName : EnvisatConstants.AATSR_L1B_BAND_NAMES) {
            if (!sourceBandNameList.contains(bandName)) {
                return bandName;
            }
        }

        return "";
    }

    private static String validateMerisProductTpgs(Product product) {
        List<String> sourceTpgNameList = Arrays.asList(product.getTiePointGridNames());
        for (String tpgName : REQUIRED_MERIS_TPG_NAMES) {
            if (!sourceTpgNameList.contains(tpgName)) {
                return tpgName;
            }
        }

        return "";
    }

    private static String validateAatsrProductTpgs(Product product) {
        List<String> sourceTpgNameList = Arrays.asList(product.getTiePointGridNames());
        for (String tpgName : REQUIRED_AATSR_TPG_NAMES) {
            if (!sourceTpgNameList.contains(tpgName)) {
                return tpgName;
            }
        }

        return "";
    }

    public static void validateParameters(Map<String, Object> parameterMap) {

        double ndsiUpperThreshold = ((Double) parameterMap.get("ndsiUpperThreshold")).doubleValue();
        double ndsiLowerThreshold = ((Double) parameterMap.get("ndsiLowerThreshold")).doubleValue();
        double aatsr1610UpperThreshold = ((Double) parameterMap.get("aatsr1610UpperThreshold")).doubleValue();
        double aatsr1610LowerThreshold = ((Double) parameterMap.get("aatsr1610LowerThreshold")).doubleValue();

        if (ndsiUpperThreshold < ndsiLowerThreshold) {
            String message = MessageFormat.format("NDSI: lower threshold {0} must be less than upper threshold {1}",
                                                  ndsiLowerThreshold, ndsiUpperThreshold);
            throw new OperatorException(message);
        }
        if (aatsr1610UpperThreshold < aatsr1610LowerThreshold) {
            String message = MessageFormat.format("AATSR 1610nm: lower threshold {0} must be less than upper threshold {1}",
                                                  aatsr1610LowerThreshold, aatsr1610UpperThreshold);
            throw new OperatorException(message);
        }
    }
}
