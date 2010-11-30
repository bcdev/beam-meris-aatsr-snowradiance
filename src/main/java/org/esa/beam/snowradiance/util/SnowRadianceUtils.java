package org.esa.beam.snowradiance.util;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.snowradiance.operator.SnowRadianceConstants;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

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


    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("snowradiance");

    private static String DESCRIPTION_FLAG_NO_AATSR = "Pixel is outside AATSR coverage (in case of colocated product)";
    private static String DESCRIPTION_FLAG_CLOUD = "Pixel classified as cloudy";
    private static String DESCRIPTION_FLAG_ICE  = "Pixel classified as ice-covered";
    private static String DESCRIPTION_FLAG_SNOW = "Pixel classified as snow_covered";
    private static String DESCRIPTION_FLAG_UNSPECIFIED = "Pixel neither classified as cloudy nor ice or snow-covered";

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
        if (merisProduct != null) {
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
        } else {
            String message = "Missing MERIS L1b input product";
            throw new OperatorException(message);
        }
    }

    public static void validateAatsrProduct(final Product aatsrProduct) {
        if (aatsrProduct != null) {
            final String missedBand = validateAatsrProductBands(aatsrProduct);
            if (!missedBand.isEmpty()) {
                String message = MessageFormat.format("Missing required band in AATSR input product: {0} . Is this really an AATSR L1b product?",
                                                      missedBand);
                throw new OperatorException(message);
            }
            final String missedTPG = validateAatsrProductTpgs(aatsrProduct);
            if (!missedTPG.isEmpty()) {
                String message = MessageFormat.format("Missing required tie-point grid in AATSR input product: {0} . Is this really an AATSR L1b product?",
                                                      missedTPG);
                throw new OperatorException(message);
            }
        } else {
            String message = "Missing AATSR L1b input product";
            throw new OperatorException(message);
        }
    }

    public static void validateParameters(Map<String, Object> parameterMap) {

        double ndsiUpperThreshold = ((Double) parameterMap.get("ndsiUpperThreshold")).doubleValue();
        double ndsiLowerThreshold = ((Double) parameterMap.get("ndsiLowerThreshold")).doubleValue();
        double aatsr1610UpperThreshold = ((Double) parameterMap.get("aatsr1610UpperThreshold")).doubleValue();
        double aatsr1610LowerThreshold = ((Double) parameterMap.get("aatsr1610LowerThreshold")).doubleValue();
         double aatsr0670UpperThreshold = ((Double) parameterMap.get("aatsr0670UpperThreshold")).doubleValue();
        double aatsr0670LowerThreshold = ((Double) parameterMap.get("aatsr0670LowerThreshold")).doubleValue();

        if (ndsiUpperThreshold < ndsiLowerThreshold) {
            String message = "NDSI: lower threshold must be less than upper threshold";
            throw new OperatorException(message);
        }
        if (aatsr1610UpperThreshold < aatsr1610LowerThreshold) {
            String message = "AATSR 1610nm: lower threshold must be less than upper threshold";
            throw new OperatorException(message);
        }

        if (aatsr0670UpperThreshold < aatsr0670LowerThreshold) {
            String message = "AATSR 670nm: lower threshold must be less than upper threshold";
            throw new OperatorException(message);
        }
    }

    public static void copyStreamToFile(InputStream inFile, String to) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(inFile);
            OutputStream outFile = new FileOutputStream(to);
            out = new BufferedOutputStream(outFile);
            while (true) {
                int data = in.read();
                if (data == -1) {
                    break;
                }
                out.write(data);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public static FlagCoding createSnowRadianceFlagCoding() {
        final FlagCoding flagCoding = new FlagCoding(SnowRadianceConstants.SNOWRADIANCE_FLAG_BAND_NAME);
        flagCoding.setDescription("Snow Radiance Flag Coding");

//        flagCoding.addFlag("F_UNCERTAIN", BitSetter.setFlag(0, SnowRadianceConstants.F_UNCERTAIN), "coverage is uncertain");
        flagCoding.addFlag("F_NO_AATSR", BitSetter.setFlag(0, SnowRadianceConstants.F_NO_AATSR), DESCRIPTION_FLAG_NO_AATSR);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, SnowRadianceConstants.F_CLOUD), DESCRIPTION_FLAG_CLOUD);
        flagCoding.addFlag("F_ICE", BitSetter.setFlag(0, SnowRadianceConstants.F_ICE), DESCRIPTION_FLAG_ICE);
        flagCoding.addFlag("F_SNOW", BitSetter.setFlag(0, SnowRadianceConstants.F_SNOW), DESCRIPTION_FLAG_SNOW);
        flagCoding.addFlag("F_UNSPECIFIED", BitSetter.setFlag(0, SnowRadianceConstants.F_UNSPECIFIED), DESCRIPTION_FLAG_UNSPECIFIED);
        return flagCoding;
    }

    public static int setupGlobAlbedoCloudscreeningBitmasks(Product sourceProduct, Product targetProduct) {

        int index = 0;
        int w = sourceProduct.getSceneRasterWidth();
        int h = sourceProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("NO_AATSR", DESCRIPTION_FLAG_NO_AATSR, w, h, "snowradiance_flags.F_NO_AATSR",
                                         Color.RED, 0.5f);
        targetProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("CLOUD", DESCRIPTION_FLAG_CLOUD, w, h, "snowradiance_flags.F_CLOUD",
                                         Color.YELLOW, 0.5f);
        targetProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("ICE", DESCRIPTION_FLAG_ICE, w, h,
                                         "snowradiance_flags.F_ICE", Color.CYAN, 0.5f);
        targetProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("SNOW", DESCRIPTION_FLAG_SNOW, w, h,
                                         "snowradiance_flags.F_SNOW", Color.BLUE, 0.5f);
        targetProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("UNSPECIFIED", DESCRIPTION_FLAG_UNSPECIFIED, w, h,
                                         "snowradiance_flags.F_UNSPECIFIED", Color.GREEN, 0.5f);
        targetProduct.getMaskGroup().add(index++, mask);

        return index;
    }

    public static boolean snowGrainSizePollutionAlgoFailed(double result) {
        return (result < 0.0 || result == Double.NaN || Math.abs(result) == Double.POSITIVE_INFINITY);
    }

    public static boolean temperatureAlgoFailed(double result) {
        return (result < 200.0 || result > 350 || result == Double.NaN || Math.abs(result) == Double.POSITIVE_INFINITY);
    }

    public static boolean emissivityAlgoFailed(double result) {
        return (result < 0.0 || result > 1.0 || result == Double.NaN || Math.abs(result) == Double.POSITIVE_INFINITY);
    }

    public static void logInfoMessage(String msg) {
        if (System.getProperty("snowradianceMode") != null && System.getProperty("snowradianceMode").equals("GUI")) {
            JOptionPane.showOptionDialog(null, msg, "Snow Properties - Info Message", JOptionPane.DEFAULT_OPTION,
                                         JOptionPane.INFORMATION_MESSAGE, null, null, null);
        } else {
            info(msg);
        }
    }

    public static void logErrorMessage(String msg) {
        if (System.getProperty("snowradianceMode") != null && System.getProperty("snowradianceMode").equals("GUI")) {
            JOptionPane.showOptionDialog(null, msg, "Snow Properties - Error Message", JOptionPane.DEFAULT_OPTION,
                                         JOptionPane.ERROR_MESSAGE, null, null, null);
        } else {
            info(msg);
        }
    }

    public static void info(final String msg) {
        logger.info(msg);
        System.out.println(msg);
    }

    /**
     * Copies all bands which contain a flagcoding from the source product to the target product.
     *
     * @param sourceProduct the source product
     * @param targetProduct the target product
     */
    public static void copySingleFlagBand(Product sourceProduct, Product targetProduct, String bandName) {
        Guardian.assertNotNull("source", sourceProduct);
        Guardian.assertNotNull("target", targetProduct);
        if (sourceProduct.getFlagCodingGroup().getNodeCount() > 0) {
            for (int i = 0; i < sourceProduct.getNumBands(); i++) {
                Band sourceBand = sourceProduct.getBandAt(i);
                String sourceBandName = sourceBand.getName();
                FlagCoding coding = sourceBand.getFlagCoding();
                if (coding != null && sourceBandName.equals(bandName)) {
                    Band targetBand = ProductUtils.copyBand(bandName, sourceProduct, targetProduct);
                    ProductUtils.copyFlagCoding(coding, targetProduct);
                    targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(coding.getName()));
                }
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


    private static Color getRandomColour(Random random) {
        int rColor = random.nextInt(256);
        int gColor = random.nextInt(256);
        int bColor = random.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }

}
