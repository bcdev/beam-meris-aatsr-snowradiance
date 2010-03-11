package org.esa.beam.snowradiance.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.meris.brr.Rad2ReflOp;
import org.esa.beam.meris.cloud.CloudProbabilityOp;

import java.awt.Rectangle;
import java.awt.Color;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 *  Operator for snow grain size retrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8313 $ $Date: 2010-02-09 17:57:24 +0100 (Di, 09 Feb 2010) $
 */
@OperatorMetadata(alias="SnowRadiance.snowgrains")
public class SnowGrainSizePollutionOp extends Operator {
    @SourceProduct(alias = "source",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product.")
    private Product sourceProduct;

//    @SourceProduct(alias = "merisAatsr",
//                   label = "Name (Collocated MERIS AATSR product)",
//                   description = "Select a collocated MERIS AATSR product.")
//    private Product merisAatsrProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    @Parameter(defaultValue = "true",
               description = "Compute Unpolluted Snow Grain Size",
               label = "Compute Unpolluted Snow Grain Size")
    private boolean computeUnpollutedSnowGrainSize;

    @Parameter(defaultValue = "true",
               description = "Compute Soot Concentration",
               label = "Compute Soot Concentration")
    private boolean computeSootConcentration;

    @Parameter(defaultValue = "true",
               description = "Compute Snow Albedo",
               label = "Compute Snow Albedo")
    private boolean computeSnowAlbedo;

     @Parameter(alias = SnowRadianceConstants.LUT_PATH_PARAM_NAME,
               defaultValue = SnowRadianceConstants.LUT_PATH_PARAM_DEFAULT,
               description = SnowRadianceConstants.LUT_PATH_PARAM_DESCRIPTION,
               label = SnowRadianceConstants.LUT_PATH_PARAM_LABEL)
    private String lutPath;

//    @Parameter(defaultValue = "true",
//               description = "Get reflectances from approximative formula (instead of LUT)",
//               label = "Get reflectances from approximative formula (instead of LUT)")
//    private boolean getReflFromApprox;
    private boolean getReflFromApprox = true;

    private SnowRadianceAuxData.ReflectionLookupTable reflectionLookupTable;

    private static String productName = "SNOWRADIANCE PRODUCT";
    private static String productType = "SNOWRADIANCE PRODUCT";

    private int nWvLut;
    private int nEmiLut;
    private int nTsfcLut;
    private int nViewLut;

    private float[][] rtmUpperSingle;    // NUMBER_ATMOSPHERIC_PROFILES,  NUMBER_AATSR_WVL (4, 2)
    private float[][] rtmLowerSingle;
    private float[][] rtm;

    private double[] tsfcLut;

    private SnowGrainSizePollutionRetrieval snowGrainSizePollutionRetrieval;
    private Band[] merisReflectanceBands;
    private Product rad2reflProduct;

    public static final String CLOUDICESNOW_FLAG_BAND = "cloud_ice_snow";

    public static final int FLAG_UNCERTAIN = 0;
    public static final int FLAG_CLOUD = 1;
    public static final int FLAG_ICE = 2;
    public static final int FLAG_SNOW = 4;

     private Band cloudIceSnowFlagBand;

    private static float NDSI_THRESH_SNOW_LOWER = 1.0f;
    private static float NDSI_THRESH_SNOW_UPPER = 1.0f;

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

        Map<String, Object> emptyParams = new HashMap<String, Object>();
        rad2reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(Rad2ReflOp.class), emptyParams, sourceProduct);

        createTargetProduct();

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);
//        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        merisReflectanceBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i=0; i< EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            merisReflectanceBands[i] = rad2reflProduct.getBand("rho_toa_" + (i+1));
        }

        snowGrainSizePollutionRetrieval = new SnowGrainSizePollutionRetrieval();



        if (!getReflFromApprox) {
            try {
                reflectionLookupTable = SnowRadianceAuxData.getInstance().createReflectionLookupTable(SnowRadianceConstants.REFLECTION_LUT_BLOCKS_TO_USE, lutPath);
            } catch (IOException e) {
                throw new OperatorException("Failed to read RTM lookup tables:\n" + e.getMessage(), e);
            }
        }
        System.out.println("Done reading RTM LUT.");
    }


    private void createTargetProduct() {
        targetProduct = new Product(productName,
                                    productType,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        createTargetProductBands();
    }

     private void createTargetProductBands() {
        // the result bands:
        if (computeUnpollutedSnowGrainSize) {
            Band unpollutedSnowGrainSizeBand = targetProduct.addBand(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NAME, ProductData.TYPE_FLOAT32);
            unpollutedSnowGrainSizeBand.setNoDataValue(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NODATAVALUE);
            unpollutedSnowGrainSizeBand.setNoDataValueUsed(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NODATAVALUE_USED);
            unpollutedSnowGrainSizeBand.setUnit("mm");
        }

         if (computeSootConcentration) {
             Band pollutedSnowGrainSizeBand = targetProduct.addBand(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NAME, ProductData.TYPE_FLOAT32);
             pollutedSnowGrainSizeBand.setNoDataValue(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE);
             pollutedSnowGrainSizeBand.setNoDataValueUsed(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NODATAVALUE_USED);
             pollutedSnowGrainSizeBand.setUnit("ng/g");
         }

         if (computeSnowAlbedo) {
             Band[] snowAlbedoBand = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
             for (int i=0; i< EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
                 snowAlbedoBand[i] = targetProduct.addBand(SnowRadianceConstants.SNOW_ALBEDO_BAND_NAME + "_" + i, ProductData.TYPE_FLOAT32);
                 snowAlbedoBand[i].setNoDataValue(SnowRadianceConstants.SNOW_ALBEDO_BAND_NODATAVALUE);
                 snowAlbedoBand[i].setNoDataValueUsed(SnowRadianceConstants.SNOW_ALBEDO_BAND_NODATAVALUE_USED);
                 snowAlbedoBand[i].setUnit("dl");
             }
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

        Rectangle rectangle = targetTile.getRectangle();

        Tile[] merisSpectralBandTiles = new Tile[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i=0; i< EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
            merisSpectralBandTiles[i] = getSourceTile(merisReflectanceBands[i], rectangle, pm);
        }

        Tile saMerisTile = getSourceTile(sourceProduct.getTiePointGrid("sun_azimuth"), rectangle, pm);
        Tile szMerisTile = getSourceTile(sourceProduct.getTiePointGrid("sun_zenith"), rectangle, pm);
        Tile vaMerisTile = getSourceTile(sourceProduct.getTiePointGrid("view_azimuth"), rectangle, pm);
        Tile vzMerisTile = getSourceTile(sourceProduct.getTiePointGrid("view_zenith"), rectangle, pm);

        Tile merisRefl2Tile = merisSpectralBandTiles[1];
        Tile merisRefl13Tile = merisSpectralBandTiles[12];

        int x0 = rectangle.x;
        int y0 = rectangle.y;
        int w = rectangle.width;
        int h = rectangle.height;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {

                if (pm.isCanceled()) {
                    break;
                }

                double saa = saMerisTile.getSampleDouble(x, y);
                double sza = szMerisTile.getSampleDouble(x, y);
                double vaa = vaMerisTile.getSampleDouble(x, y);
                double vza = vzMerisTile.getSampleDouble(x, y);
                double reflFunction = 0.0;

                if (!getReflFromApprox) {
                    reflFunction = snowGrainSizePollutionRetrieval.computeReflLut(reflectionLookupTable, sza, vza);
                    System.out.println("reflFunction: " + reflFunction);
                }  else {
                    reflFunction = snowGrainSizePollutionRetrieval.computeReflLutApprox(saa, sza, vaa, vza);
                }

                double merisRefl2 = merisRefl2Tile.getSampleDouble(x, y);
                double merisRefl13 = merisRefl13Tile.getSampleDouble(x, y);

                if (targetBand.getName().equals(SnowRadianceConstants.UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NAME)) {
                    double unpollutedSnowGrainSize =
                        snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(merisRefl2, merisRefl13, reflFunction, sza, vza);
                    targetTile.setSample(x, y, unpollutedSnowGrainSize);
                }

                if (targetBand.getName().equals(SnowRadianceConstants.SOOT_CONCENTRATION_BAND_NAME)) {
                    double pal =
                        snowGrainSizePollutionRetrieval.getParticleAbsorptionLength(merisRefl2, merisRefl13, reflFunction, sza, vza);
                     double unpollutedSnowGrainSize =
                        snowGrainSizePollutionRetrieval.getUnpollutedSnowGrainSize(pal);
                    double sootConcentration =
                        snowGrainSizePollutionRetrieval.getSootConcentrationInPollutedSnow(merisRefl13, reflFunction, sza, vza,
                                                                                           pal, unpollutedSnowGrainSize);
                    targetTile.setSample(x, y, sootConcentration);
                }

                if (targetBand.getName().startsWith(SnowRadianceConstants.SNOW_ALBEDO_BAND_NAME)) {
                    for (int i=0; i< EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS; i++) {
                        double merisRefl = merisSpectralBandTiles[i].getSampleDouble(x, y);
                        double snowAlbedo =
                            snowGrainSizePollutionRetrieval.getSnowAlbedo(merisRefl, reflFunction, sza, vza);
                        targetTile.setSample(x, y, snowAlbedo);
                    }
                }

                pm.worked(1);
            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnowGrainSizePollutionOp.class);
        }
    }
}
