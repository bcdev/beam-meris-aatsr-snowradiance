package org.esa.beam.snowradiance.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.meris.brr.Rad2ReflOp;
import org.esa.beam.meris.cloud.CloudProbabilityOp;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;
import org.esa.beam.util.ProductUtils;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for snow grain size and pollutionretrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8313 $ $Date: 2010-02-09 17:57:24 +0100 (Di, 09 Feb 2010) $
 */
@OperatorMetadata(alias = "SnowRadiance.snowgrains")
public class SnowGrainSizePollutionOp extends Operator {

    public static final String WV_BAND_NAME = "water_vapour";
    public static final String NDVI_BAND_NAME = "ndvi";
    public static final String MDSI_BAND_NAME = "mdsi";

    private static final String PRODUCT_NAME = "SNOWRADIANCE PRODUCT";
    private static final String PRODUCT_TYPE = "SNOWRADIANCE PRODUCT";

    @SourceProduct(alias = "source",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product.")
    private Product merisProduct;

    // Target bands
    @Parameter(defaultValue = "false",
               description = "Copy input bands to target product",
               label = "Copy input bands")
    private boolean copyInputBands;

    @Parameter(defaultValue = "true",
               description = "Compute Snow Grain Size",
               label = "Compute snow grain size")
    private boolean computeSnowGrainSize;

    @Parameter(defaultValue = "true",
               description = "Compute snow albedo",
               label = "Compute snow albedo")
    private boolean computeSnowAlbedo;

    @Parameter(defaultValue = "false",
               description = "Compute snow soot content",
               label = "Compute snow soot content")
    private boolean computeSnowSootContent;

    // complementary quantities:
    @Parameter(defaultValue = "false",
               description = "Compute MERIS water vapour",
               label = "Compute MERIS water vapour")
    private boolean computeMerisWaterVapour;

    @Parameter(defaultValue = "false",
               description = "Compute MERIS NDVI",
               label = "Compute MERIS NDVI")
    private boolean computeMerisNdvi;

    @Parameter(defaultValue = "false",
               description = "Compute MERIS MDSI",
               label = "Compute MERIS MDSI")
    private boolean computeMerisMdsi;

    // Processing parameters
    @Parameter(defaultValue = "true",
               description = "Apply cloud mask",
               label = "Apply cloud mask")
    private boolean applyCloudMask;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "Cloud probability threshold",
               label = "Cloud probability threshold")
    private double cloudProbabilityThreshold;


    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    private Product cloudProbabilityProduct;
    private Band[] merisReflectanceBands;


    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SnowGrainSizePollutionOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        if (applyCloudMask) {
            Map<String, Product> cloudProbabilityInput = new HashMap<String, Product>(1);
            cloudProbabilityInput.put("input", merisProduct);
            Map<String, Object> cloudProbabilityParameters = new HashMap<String, Object>(3);
            cloudProbabilityParameters.put("configFile", "cloud_config.txt");
            cloudProbabilityParameters.put("validLandExpression", "not l1_flags" + ".INVALID and dem_alt > -50");
            cloudProbabilityParameters.put("validOceanExpression", "not l1_flags" + ".INVALID and dem_alt <= -50");
            cloudProbabilityProduct = GPF.createProduct("Meris.CloudProbability", cloudProbabilityParameters, cloudProbabilityInput);
        }

        createTargetProduct();

        ProductUtils.copyTiePointGrids(merisProduct, targetProduct);
        ProductUtils.copyGeoCoding(merisProduct, targetProduct);
        ProductUtils.copyMetadata(merisProduct, targetProduct);

        // snow grain size / pollution retrieval...
        Map<String, Object> emptyParams = new HashMap<String, Object>();
        Product rad2reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(Rad2ReflOp.class), emptyParams, merisProduct);

        merisReflectanceBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            merisReflectanceBands[i] = rad2reflProduct.getBand("rho_toa_" + (i + 1));
        }

    }

    private void createTargetProduct() {
        targetProduct = new Product(PRODUCT_NAME,
                                    PRODUCT_TYPE,
                                    merisProduct.getSceneRasterWidth(),
                                    merisProduct.getSceneRasterHeight());

        targetProduct.setPreferredTileSize(new Dimension(256, 256));
        createTargetProductBands();

        SnowRadianceUtils.setupGlobAlbedoCloudscreeningBitmasks(merisProduct, targetProduct);
    }

    private void createTargetProductBands() {

        if (copyInputBands) {
            for (Band band : merisProduct.getBands()) {
                if (!band.isFlagBand()) {
                    ProductUtils.copyBand(band.getName(), merisProduct, targetProduct);
                }
            }
        }

        Band cloudIceSnowBand = targetProduct.addBand(SnowRadianceConstants.SNOWRADIANCE_FLAG_BAND_NAME, ProductData.TYPE_INT16);
        cloudIceSnowBand.setDescription("Snowradiance flags");
        cloudIceSnowBand.setNoDataValue(-1);
        cloudIceSnowBand.setNoDataValueUsed(true);

        FlagCoding flagCoding = SnowRadianceUtils.createSnowRadianceFlagCoding();
        cloudIceSnowBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);

        // snow grain size / pollution bands...:
        if (computeSnowGrainSize) {
            Band unpollutedSnowGrainSizeBand = targetProduct.addBand(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NAME, ProductData.TYPE_FLOAT32);
            unpollutedSnowGrainSizeBand.setNoDataValue(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NODATAVALUE);
            unpollutedSnowGrainSizeBand.setNoDataValueUsed(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NODATAVALUE_USED);
            unpollutedSnowGrainSizeBand.setUnit("mm");
        }

        if (computeSnowSootContent) {
            Band pollutedSnowGrainSizeBand = targetProduct.addBand(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NAME, ProductData.TYPE_FLOAT32);
            pollutedSnowGrainSizeBand.setNoDataValue(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE);
            pollutedSnowGrainSizeBand.setNoDataValueUsed(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE_USED);
            pollutedSnowGrainSizeBand.setUnit("ng/g");
        }

        if (computeSnowAlbedo) {
            Band[] snowAlbedoBand = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
            for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
                snowAlbedoBand[i] = targetProduct.addBand(SnowRadianceConstants.SNOW_ALBEDO_BAND_NAME + "_" + i, ProductData.TYPE_FLOAT32);
                snowAlbedoBand[i].setNoDataValue(SnowRadianceConstants.SNOW_ALBEDO_BAND_NODATAVALUE);
                snowAlbedoBand[i].setNoDataValueUsed(SnowRadianceConstants.SNOW_ALBEDO_BAND_NODATAVALUE_USED);
                snowAlbedoBand[i].setUnit("dl");
            }
        }

        // complementary quantities
        if (computeMerisWaterVapour) {
            Band wvBand = targetProduct.addBand(WV_BAND_NAME, ProductData.TYPE_FLOAT32);
            wvBand.setDescription("NDSI");
            wvBand.setNoDataValue(-1.0f);
            wvBand.setNoDataValueUsed(true);
            wvBand.setUnit("kg/m^2");
        }

        if (computeMerisNdvi) {
            Band ndviBand = targetProduct.addBand(NDVI_BAND_NAME, ProductData.TYPE_FLOAT32);
            ndviBand.setDescription("NDVI");
            ndviBand.setNoDataValue(-1.0f);
            ndviBand.setNoDataValueUsed(true);
            ndviBand.setUnit("dl");
        }

        if (computeMerisMdsi) {
            Band mdsiBand = targetProduct.addBand(MDSI_BAND_NAME, ProductData.TYPE_FLOAT32);
            mdsiBand.setDescription("MDSI");
            mdsiBand.setNoDataValue(-1.0f);
            mdsiBand.setNoDataValueUsed(true);
            mdsiBand.setUnit("dl");
        }

        ProductUtils.copyFlagBands(merisProduct, targetProduct);

        if (applyCloudMask) {
            Band cloudProbBand = targetProduct.addBand("cloud_probability", ProductData.TYPE_FLOAT32);
            cloudProbBand.setDescription("cloud_probability");
            cloudProbBand.setNoDataValue(-1.0f);
            cloudProbBand.setNoDataValueUsed(true);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        JnnNet neuralNetWv;
        try {
            neuralNetWv = SnowRadianceAuxData.getInstance().loadNeuralNet(SnowRadianceAuxData.NEURAL_NET_WV_OCEAN_MERIS_FILE_NAME);
        } catch (IOException e) {
            throw new OperatorException("Failed to read WV neural net:\n" + e.getMessage(), e);
        } catch (JnnException e) {
            throw new OperatorException("Failed to load WV neural net:\n" + e.getMessage(), e);
        }

        Rectangle rectangle = targetTile.getRectangle();

        Tile zonalWindTile = getSourceTile(merisProduct.getTiePointGrid("zonal_wind"), rectangle);
        Tile meridWindTile = getSourceTile(merisProduct.getTiePointGrid("merid_wind"), rectangle);
        Tile saMerisTile = getSourceTile(merisProduct.getTiePointGrid("sun_azimuth"), rectangle);
        Tile szMerisTile = getSourceTile(merisProduct.getTiePointGrid("sun_zenith"), rectangle);
        Tile vaMerisTile = getSourceTile(merisProduct.getTiePointGrid("view_azimuth"), rectangle);
        Tile vzMerisTile = getSourceTile(merisProduct.getTiePointGrid("view_zenith"), rectangle);

        Tile merisRad14Tile = getSourceTile(merisProduct.getBand("radiance_14"), rectangle);
        Tile merisRad15Tile = getSourceTile(merisProduct.getBand("radiance_15"), rectangle);

        Tile[] merisSpectralBandTiles = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i = 0; i < EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            merisSpectralBandTiles[i] = getSourceTile(merisReflectanceBands[i], rectangle);
        }

        Tile merisRefl2Tile = merisSpectralBandTiles[1];
        Tile merisRefl12Tile = merisSpectralBandTiles[11];
        Tile merisRefl13Tile = merisSpectralBandTiles[12];
        Tile merisRefl14Tile = merisSpectralBandTiles[13];

        Tile merisL1FlagsTile = getSourceTile(merisProduct.getBand(("l1_flags")), rectangle);

        Tile cloudProbTile = null;
        if (applyCloudMask) {
            cloudProbTile = getSourceTile(cloudProbabilityProduct.getBand(CloudProbabilityOp.CLOUD_PROP_BAND), rectangle);
        }

        int x0 = rectangle.x;
        int y0 = rectangle.y;
        int w = rectangle.width;
        int h = rectangle.height;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {

                if (pm.isCanceled()) {
                    break;
                }

                if (targetBand.isFlagBand() && targetBand.getName().equals("l1_flags")) {
                    targetTile.setSample(x, y, merisL1FlagsTile.getSampleInt(x, y));
                } else {

                    // first determine cloud mask...
                    boolean considerPixelAsCloudy = applyCloudMask && isCloud(cloudProbTile, x, y);

                    if (!considerPixelAsCloudy) {

                        // snow grain size / pollution retrieval...
                        if (doSnowGrainSizePollutionRetrieval()) {
                            double saa = saMerisTile.getSampleDouble(x, y);
                            double sza = szMerisTile.getSampleDouble(x, y);
                            double vaa = vaMerisTile.getSampleDouble(x, y);
                            double vza = vzMerisTile.getSampleDouble(x, y);
                            double reflFunction;

                            if (x == 800 && y == 151) {
                                System.out.println();
                            }
                            reflFunction = SnowGrainSizePollutionRetrieval.computeReflLutApprox(saa, sza, vaa, vza);

                            double merisRefl2 = merisRefl2Tile.getSampleDouble(x, y);
                            double merisRefl13 = merisRefl13Tile.getSampleDouble(x, y);
                            double unpollutedSnowGrainSize;

                            if (computeSnowGrainSize && targetBand.getName().equals(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NAME)) {
                                double pal =
                                        SnowGrainSizePollutionRetrieval.getParticleAbsorptionLength(merisRefl2, merisRefl13, reflFunction, sza, vza);
                                if (!SnowRadianceUtils.snowGrainSizePollutionAlgoFailed(pal)) {
                                    unpollutedSnowGrainSize =
                                            SnowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
                                    if (SnowRadianceUtils.snowGrainSizePollutionAlgoFailed(unpollutedSnowGrainSize)) {
                                        targetTile.setSample(x, y, SnowRadianceConstants.SNOW_GRAIN_SIZE_POLLUTION_NODATAVALUE);
                                    } else {
                                        targetTile.setSample(x, y, unpollutedSnowGrainSize);
                                    }
                                } else {
                                    targetTile.setSample(x, y, SnowRadianceConstants.SNOW_GRAIN_SIZE_POLLUTION_NODATAVALUE);
                                }
                            }

                            if (computeSnowSootContent && targetBand.getName().equals(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NAME)) {
                                double pal =
                                        SnowGrainSizePollutionRetrieval.getParticleAbsorptionLength(merisRefl2, merisRefl13, reflFunction, sza, vza);
                                if (!SnowRadianceUtils.snowGrainSizePollutionAlgoFailed(pal)) {
                                    unpollutedSnowGrainSize =
                                            SnowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
                                    if (!SnowRadianceUtils.snowGrainSizePollutionAlgoFailed(unpollutedSnowGrainSize)) {
                                        double sootConcentration = SnowGrainSizePollutionRetrieval.getSootConcentrationInPollutedSnow(
                                                merisRefl13, reflFunction, sza, vza, unpollutedSnowGrainSize);
                                        if (SnowRadianceUtils.snowGrainSizePollutionAlgoFailed(sootConcentration)) {
                                            targetTile.setSample(x, y, SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE);
                                        } else {
                                            targetTile.setSample(x, y, sootConcentration);
                                        }
                                    } else {
                                        targetTile.setSample(x, y, SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE);
                                    }
                                } else {
                                    targetTile.setSample(x, y, SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE);
                                }
                            }

                            if (computeSnowAlbedo &&
                                    targetBand.getName().startsWith(SnowRadianceConstants.SNOW_ALBEDO_BAND_NAME)) {
                                int snowAlbedoBandPrefixLength = SnowRadianceConstants.SNOW_ALBEDO_BAND_NAME.length();
                                String snowAlbedoBandIndexString = targetBand.getName().
                                        substring(snowAlbedoBandPrefixLength + 1, targetBand.getName().length());
                                int snowAlbedoBandIndex = Integer.parseInt(snowAlbedoBandIndexString);
                                double merisRefl = merisSpectralBandTiles[snowAlbedoBandIndex].getSampleDouble(x, y);
                                double snowAlbedo =
                                        SnowGrainSizePollutionRetrieval.getSnowAlbedo(merisRefl, reflFunction, sza, vza);
                                targetTile.setSample(x, y, snowAlbedo);
                            }

                            if (targetBand.getName().equals(SnowRadianceConstants.SNOWRADIANCE_FLAG_BAND_NAME)) {
                                targetTile.setSample(x, y, SnowRadianceConstants.F_NO_AATSR, true);
                                targetTile.setSample(x, y, SnowRadianceConstants.F_UNSPECIFIED, true);
                            }

                        }

                    } else {
                        if (targetBand.getName().equals(SnowRadianceConstants.SNOWRADIANCE_FLAG_BAND_NAME)) {
                            targetTile.setSample(x, y, SnowRadianceConstants.F_CLOUD, true);
                            targetTile.setSample(x, y, SnowRadianceConstants.F_SNOW, false);
                            targetTile.setSample(x, y, SnowRadianceConstants.F_ICE, false);
                            targetTile.setSample(x, y, SnowRadianceConstants.F_UNSPECIFIED, false);
                            targetTile.setSample(x, y, SnowRadianceConstants.F_NO_AATSR, true);
                        } else {
                            targetTile.setSample(x, y, (SnowRadianceConstants.SNOW_GRAIN_SIZE_POLLUTION_NODATAVALUE));
                        }
                    }


                    // complementary quantities...
                    float merisViewAzimuth = vaMerisTile.getSampleFloat(x, y);
                    float merisSunAzimuth = saMerisTile.getSampleFloat(x, y);
                    final float zonalWind = zonalWindTile.getSampleFloat(x, y);
                    final float meridWind = meridWindTile.getSampleFloat(x, y);
                    float merisAzimuthDifference = SnowTemperatureEmissivityRetrieval.removeAzimuthDifferenceAmbiguity(merisViewAzimuth,
                                                                                                                       merisSunAzimuth);
                    final float merisViewZenith = vzMerisTile.getSampleFloat(x, y);
                    final float merisSunZenith = szMerisTile.getSampleFloat(x, y);
                    final float merisRad14 = merisRad14Tile.getSampleFloat(x, y);
                    final float merisRad15 = merisRad15Tile.getSampleFloat(x, y);

                    if (computeMerisWaterVapour && targetBand.getName().equals(WV_BAND_NAME)) {
                        final float merisWaterVapourColumn = SnowTemperatureEmissivityRetrieval.computeWaterVapour(neuralNetWv, zonalWind, meridWind, merisAzimuthDifference,
                                                                                                                   merisViewZenith, merisSunZenith, merisRad14, merisRad15);
                        targetTile.setSample(x, y, merisWaterVapourColumn);
                    }

                    if (computeMerisNdvi && targetBand.getName().equals(NDVI_BAND_NAME)) {
                        final float merisRefl12 = merisRefl12Tile.getSampleFloat(x, y);
                        final float merisRefl13 = merisRefl13Tile.getSampleFloat(x, y);
                        final double ndvi = (merisRefl12 - merisRefl13) / (merisRefl12 + merisRefl13);
                        targetTile.setSample(x, y, ndvi);
                    }

                    if (computeMerisMdsi && targetBand.getName().equals(MDSI_BAND_NAME)) {
                        final float merisRefl13 = merisRefl13Tile.getSampleFloat(x, y);
                        final float merisRefl14 = merisRefl14Tile.getSampleFloat(x, y);
                        final double mdsi = (merisRefl13 - merisRefl14) / (merisRefl13 + merisRefl14);
                        targetTile.setSample(x, y, mdsi);
                    }
                }
                if (applyCloudMask && targetBand.getName().equals("cloud_probability")) {
                    targetTile.setSample(x, y, cloudProbTile.getSampleFloat(x, y));
                }
            }
        }
    }

    private boolean isCloud(Tile cloudProbTile, int x, int y) {
        boolean isCloud;
        if (!applyCloudMask) {
            return false;
        }

        float cloudProb = cloudProbTile.getSampleFloat(x, y);
        isCloud = (cloudProb > cloudProbabilityThreshold);

        return isCloud;
    }

    private boolean doSnowGrainSizePollutionRetrieval() {
        return (computeSnowGrainSize || computeSnowSootContent || computeSnowAlbedo);
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnowGrainSizePollutionOp.class);
        }
    }
}
