/*
* Copyright (C) 2002-2007 by ?
*
* This program is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by the
* Free Software Foundation. This program is distributed in the hope it will
* be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
* of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package org.esa.beam.snowradiance.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.LookupTable;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;
import org.esa.beam.meris.cloud.CloudProbabilityOp;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Operator for snow temperature and emissivity retrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
@OperatorMetadata(alias = "SnowRadiance.temperature")
public class SnowTemperatureEmissivityOp extends Operator {

    @SourceProduct(alias = "source",
                   label = "Name (Collocated MERIS AATSR product)",
                   description = "Select a collocated MERIS AATSR product.")
    private Product sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct;

    @Parameter(defaultValue = "true",
               description = "Compute Snow Temperature (FUB)",
               label = "Compute Snow Temperature (FUB)")
    private boolean computeSnowTemperatureFub;

    @Parameter(defaultValue = "true",
               description = "Compute Emissivity (FUB)",
               label = "Compute Emissivity (FUB)")
    private boolean computeEmissivityFub;

    @Parameter(defaultValue = "true",
               description = "Perform cloud/ice/snow detection",
               label = "Perform Cloud/Ice/Snow Detection")
    private boolean computeCloudIceSnow;

    @Parameter(defaultValue = "M",
               description = "Master Product Bands Suffix",
               label = "Master Product Bands Suffix")
    private String masterProductBandsSuffix;

    @Parameter(defaultValue = "S",
               description = "Slave Product Bands Suffix",
               label = "Slave Product Bands Suffix")
    private String slaveProductBandsSuffix;

    @Parameter(defaultValue = "0.90f",
               description = "NDSI lower threshold for snow/ice detection",
               label = "NDSI lower threshold for snow/ice detection")
    private float ndsiSnowLowerThreshold;

    @Parameter(defaultValue = "0.96f",
               description = "NDSI upper threshold for snow/ice detection",
               label = "NDSI upper threshold for snow/ice detection")
    private float ndsiSnowUpperThreshold;

    @Parameter(alias = SnowRadianceConstants.LUT_PATH_PARAM_NAME,
               defaultValue = SnowRadianceConstants.LUT_PATH_PARAM_DEFAULT,
               description = SnowRadianceConstants.LUT_PATH_PARAM_DESCRIPTION,
               label = SnowRadianceConstants.LUT_PATH_PARAM_LABEL)
    private String lutPath;

    private Product cloudProbabilityProduct;

    private Band aatsrBt11NadirBand;
    private Band aatsrBt12NadirBand;

    private Band aatsrReflecNadir0670Band;
    private Band aatsrReflecNadir0870Band;
    private Band aatsrReflecNadir1600Band;

    private Band merisRad13Band;
    private Band merisRad14Band;
    private Band merisRad15Band;

    public static final String CLOUDICESNOW_BAND_NAME = "cloud_ice_snow";
    public static final String NDSI_BAND_NAME = "ndsi";

    public static final int FLAG_UNCERTAIN = 0;
    public static final int FLAG_ICE = 1;
    public static final int FLAG_SNOW = 2;
    public static final int FLAG_CLOUD = 4;

    private LookupTable[][] rtmLookupTables;

    private static String productName = "SNOWRADIANCE PRODUCT";
    private static String productType = "SNOWRADIANCE PRODUCT";

    private double[][][] tsfcLut;
    private double[] tLowestLayer = new double[SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES];

    private static float NDSI_THRESH_CLOUD_LOWER = 0.2f;
    private static float NDSI_THRESH_SNOW_LOWER = 0.6f;
    private static float NDSI_THRESH_SNOW_UPPER = 0.85f;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SnowTemperatureEmissivityOp() {
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

        Map<String, Product> cloudProbabilityInput = new HashMap<String, Product>(1);
        sourceProduct.setProductType("MER_RR__1P");
        cloudProbabilityInput.put("input", sourceProduct);
        Map<String, Object> cloudProbabilityParameters = new HashMap<String, Object>(3);
        cloudProbabilityParameters.put("configFile", "cloud_config.txt");
        cloudProbabilityParameters.put("validLandExpression", "not l1_flags_" + masterProductBandsSuffix + ".INVALID and dem_alt > -50");
        cloudProbabilityParameters.put("validOceanExpression", "not l1_flags_" + masterProductBandsSuffix + ".INVALID and dem_alt <= -50");
        cloudProbabilityParameters.put("productBandsSuffix", masterProductBandsSuffix);
        cloudProbabilityProduct = GPF.createProduct("Meris.CloudProbability", cloudProbabilityParameters, cloudProbabilityInput);

        createTargetProduct();

        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct);

        try {
            rtmLookupTables = SnowRadianceAuxData.createRtmLookupTables(lutPath);
            tsfcLut = SnowRadianceAuxData.getTsfcFromLookupTables(lutPath);
            for (int i=0; i<SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
                tLowestLayer[i] = tsfcLut[i][0][24];
            }
        } catch (IOException e) {
            throw new OperatorException("Failed to read RTM lookup tables:\n" + e.getMessage(), e);
        }

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
        if (computeSnowTemperatureFub) {
            Band snowTempBand = targetProduct.addBand(SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NAME, ProductData.TYPE_FLOAT32);
            snowTempBand.setNoDataValue(SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NODATAVALUE);
            snowTempBand.setNoDataValueUsed(SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NODATAVALUE_USED);
            snowTempBand.setUnit("K");
        }

        if (computeEmissivityFub) {
            Band emissivityBand = targetProduct.addBand(SnowRadianceConstants.EMISSIVITY_BAND_NAME, ProductData.TYPE_FLOAT32);
            emissivityBand.setNoDataValue(SnowRadianceConstants.EMISSIVITY_BAND_NODATAVALUE);
            emissivityBand.setNoDataValueUsed(SnowRadianceConstants.EMISSIVITY_BAND_NODATAVALUE_USED);
            emissivityBand.setUnit("K");
        }

        if (computeCloudIceSnow) {
            // create and add the flags coding
            Band cloudIceSnowBand = targetProduct.addBand(CLOUDICESNOW_BAND_NAME, ProductData.TYPE_INT16);
            cloudIceSnowBand.setDescription("Cloud/Ice/Snow flags");
            cloudIceSnowBand.setNoDataValue(-1);
            cloudIceSnowBand.setNoDataValueUsed(true);

            Band ndsiBand = targetProduct.addBand(NDSI_BAND_NAME, ProductData.TYPE_FLOAT32);
            cloudIceSnowBand.setDescription("NDSI");
            cloudIceSnowBand.setNoDataValue(-1.0f);
            cloudIceSnowBand.setNoDataValueUsed(true);
        }
    }


    public static FlagCoding createCloudIceSnowFlagCoding(Product outputProduct) {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding(CLOUDICESNOW_BAND_NAME);
        flagCoding.setDescription("Cloud Flag Coding");

        cloudAttr = new MetadataAttribute("cloudcovered", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUD);
        cloudAttr.setDescription("is with more than 80% cloudy");
        flagCoding.addAttribute(cloudAttr);
        outputProduct.addBitmaskDef(new BitmaskDef(cloudAttr.getName(),
                                                   cloudAttr.getDescription(),
                                                   flagCoding.getName() + "." + cloudAttr.getName(),
                                                   createBitmaskColor(1, 3),
                                                   0.5F));

        cloudAttr = new MetadataAttribute("icecovered", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_ICE);
        cloudAttr.setDescription("is covered with ice (NDSI criterion)");
        flagCoding.addAttribute(cloudAttr);
        outputProduct.addBitmaskDef(new BitmaskDef(cloudAttr.getName(),
                                                   cloudAttr.getDescription(),
                                                   flagCoding.getName() + "." + cloudAttr.getName(),
                                                   createBitmaskColor(2, 3),
                                                   0.5F));

        cloudAttr = new MetadataAttribute("snowcovered", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_SNOW);
        cloudAttr.setDescription("is covered with snow (AATSR band criterion)");
        flagCoding.addAttribute(cloudAttr);
        outputProduct.addBitmaskDef(new BitmaskDef(cloudAttr.getName(),
                                                   cloudAttr.getDescription(),
                                                   flagCoding.getName() + "." + cloudAttr.getName(),
                                                   createBitmaskColor(3, 3),
                                                   0.5F));

        return flagCoding;
    }

    /**
     * Creates a new color object to be used in the bitmaskDef.
     * The given indices start with 1.
     *
     * @param index
     * @param maxIndex
     * @return the color
     */
    private static Color createBitmaskColor(int index, int maxIndex) {
        final double rf1 = 0.0;
        final double gf1 = 0.5;
        final double bf1 = 1.0;

        final double a = 2 * Math.PI * index / maxIndex;

        return new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
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

        if ((!computeSnowTemperatureFub && !computeEmissivityFub) || targetBand.isFlagBand()) {
            // nothing to do
            return;
        }

        JnnNet neuralNetWv;
        try {
            neuralNetWv = SnowRadianceAuxData.getInstance().loadNeuralNet(SnowRadianceAuxData.NEURAL_NET_WV_OCEAN_MERIS_FILE_NAME);
        } catch (IOException e) {
            throw new OperatorException("Failed to read WV neural net:\n" + e.getMessage(), e);
        } catch (JnnException e) {
            throw new OperatorException("Failed to load WV neural net:\n" + e.getMessage(), e);
        }

        Rectangle rectangle = targetTile.getRectangle();

        Tile zonalWindTile = getSourceTile(sourceProduct.getTiePointGrid("zonal_wind"), rectangle, pm);
        Tile meridWindTile = getSourceTile(sourceProduct.getTiePointGrid("merid_wind"), rectangle, pm);
        Tile saMerisTile = getSourceTile(sourceProduct.getTiePointGrid("sun_azimuth"), rectangle, pm);
        Tile szMerisTile = getSourceTile(sourceProduct.getTiePointGrid("sun_zenith"), rectangle, pm);
        Tile vaMerisTile = getSourceTile(sourceProduct.getTiePointGrid("view_azimuth"), rectangle, pm);
        Tile vzMerisTile = getSourceTile(sourceProduct.getTiePointGrid("view_zenith"), rectangle, pm);

        Tile merisRad14Tile = getSourceTile(sourceProduct.getBand("radiance_14" + "_" + masterProductBandsSuffix + ""), rectangle, pm);
        Tile merisRad15Tile = getSourceTile(sourceProduct.getBand("radiance_15" + "_" + masterProductBandsSuffix + ""), rectangle, pm);

        Tile aatsrBTNadir1100Tile = getSourceTile(sourceProduct.getBand("btemp_nadir_1100" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);
        Tile aatsrBTNadir1200Tile = getSourceTile(sourceProduct.getBand("btemp_nadir_1200" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);

        Tile veAatsrNadirTile = getSourceTile(sourceProduct.getBand("view_elev_nadir" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);

        Tile aatsrReflecNadir550Tile = getSourceTile(sourceProduct.getBand("reflec_nadir_0550" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);
        Tile aatsrReflecNadir670Tile = getSourceTile(sourceProduct.getBand("reflec_nadir_0670" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);
        Tile aatsrReflecNadir870Tile = getSourceTile(sourceProduct.getBand("reflec_nadir_0870" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);
        Tile aatsrReflecNadir1600Tile = getSourceTile(sourceProduct.getBand("reflec_nadir_1600" + "_" + slaveProductBandsSuffix + ""), rectangle, pm);


        Tile cloudFlags = getSourceTile(cloudProbabilityProduct.getBand(CloudProbabilityOp.CLOUD_FLAG_BAND), rectangle, pm);


        int x0 = rectangle.x;
        int y0 = rectangle.y;
        int w = rectangle.width;
        int h = rectangle.height;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {

                if (pm.isCanceled()) {
                    break;
                }

                final float aatsrBt11 = aatsrBTNadir1100Tile.getSampleFloat(x, y);
                final float aatsrBt12 = aatsrBTNadir1200Tile.getSampleFloat(x, y);

//                if (targetBand.getName().equals(CLOUDICESNOW_BAND_NAME) && x == 580 && y == 670) {
//                    System.out.println("halt");
//                }

                if (aatsrBt11 > 0.0 && aatsrBt12 > 0.0 && !(aatsrBt11 == Float.NaN) && !(aatsrBt12 == Float.NaN)) {

                    boolean isCloud = cloudFlags.getSampleBit(x, y, CloudProbabilityOp.FLAG_CLOUDY);
                    if (targetBand.getName().equals(NDSI_BAND_NAME)) {
                        if (!isCloud) {
                            float aatsr865 = aatsrReflecNadir870Tile.getSampleFloat(x, y);
                            float aatsr1610 = aatsrReflecNadir1600Tile.getSampleFloat(x, y);
                            float ndsi = (aatsr865 - aatsr1610) / (aatsr865 + aatsr1610);
                            targetTile.setSample(x, y, ndsi);
                        } else {
                            targetTile.setSample(x, y, -1.0f);
                        }
                    } else if (targetBand.getName().equals(CLOUDICESNOW_BAND_NAME)) {
                        // cloud, ice, snow retrieval using NDSI thresholds
//                         if (!isCloud) {
                             targetTile.setSample(x, y, FLAG_UNCERTAIN);
                             float aatsr865 = aatsrReflecNadir870Tile.getSampleFloat(x, y);
                             float aatsr1610 = aatsrReflecNadir1600Tile.getSampleFloat(x, y);
                             float ndsi = (aatsr865 - aatsr1610) / (aatsr865 + aatsr1610);
                             if (ndsi > ndsiSnowLowerThreshold && ndsi < ndsiSnowUpperThreshold) {
                                 targetTile.setSample(x, y, FLAG_SNOW);
                             } else if (ndsi > ndsiSnowUpperThreshold) {
                                 targetTile.setSample(x, y, FLAG_ICE);
                             }
                             if (!(ndsi > ndsiSnowUpperThreshold)) {
                                 float aatsr550 = aatsrReflecNadir550Tile.getSampleFloat(x, y);
                                 float aatsr670 = aatsrReflecNadir670Tile.getSampleFloat(x, y);
//                             boolean is550InInterval = aatsr550 >= 0.97f && aatsr550 <= 0.99f;
//                             boolean is670InInterval = aatsr670 >= 0.92f && aatsr670 <= 0.97f;
//                             boolean is870InInterval = aatsr865 >= 0.79f && aatsr865 <= 0.93f;
                                 boolean is1600InInterval = aatsr1610 >= 1.0f && aatsr1610 <= 10.0f;
//                             boolean isSnow = is550InInterval && is670InInterval && is870InInterval && is1600InInterval;
                                 boolean isSnow = is1600InInterval;
                                 if (isSnow) {
                                     targetTile.setSample(x, y, FLAG_SNOW);
                                 } else {
                                     targetTile.setSample(x, y, FLAG_UNCERTAIN);
                                 }
                             }
//                         } else {
//                             targetTile.setSample(x, y, FLAG_CLOUD);
//                         }
                    } else {
                        // 3.2.3 Calculation of water vapour
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
//                        float waterVapourColumn = SnowTemperatureEmissivityRetrieval.computeWaterVapour(neuralNetWv, zonalWind, meridWind, merisAzimuthDifference,
//                                                                                              merisViewZenith, merisSunZenith, merisRad14, merisRad15);
                        float waterVapourColumn = 0.3f; // simplification, might be sufficient (RP, 2010/04/14)

                        // 3.2.4 temperature retrieval

                        final float aatsrViewElevationNadir = veAatsrNadirTile.getSampleFloat(x, y);
                        final float viewZenith = 90.0f - aatsrViewElevationNadir;

                        float tempSurface = SnowTemperatureEmissivityRetrieval.
                                minimizeNewtonForTemperature(waterVapourColumn, viewZenith, aatsrBt11, rtmLookupTables, tLowestLayer);

                        if (targetBand.getName().equals(SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NAME)) {
                            targetTile.setSample(x, y, tempSurface);
                        }
                        if (computeEmissivityFub && targetBand.getName().equals(SnowRadianceConstants.EMISSIVITY_BAND_NAME)) {

                            float emissivity =  SnowTemperatureEmissivityRetrieval.
                                    minimizeNewtonForEmissivity(waterVapourColumn, viewZenith, tempSurface, aatsrBt12,
                                                                             rtmLookupTables, tLowestLayer);
                            targetTile.setSample(x, y, emissivity);
                        }
                    }

                } else {
                    if (computeSnowTemperatureFub && targetBand.getName().equals(SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NAME)) {
                        targetTile.setSample(x, y, SnowRadianceConstants.SNOW_TEMPERATURE_BAND_NODATAVALUE);
                    }
                    if (computeEmissivityFub && targetBand.getName().equals(SnowRadianceConstants.EMISSIVITY_BAND_NAME)) {
                        targetTile.setSample(x, y, SnowRadianceConstants.EMISSIVITY_BAND_NODATAVALUE);
                    }
                    if (targetBand.getName().equals(CLOUDICESNOW_BAND_NAME)) {
                        targetTile.setSample(x, y, FLAG_UNCERTAIN);
                    }
                }


            }
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnowTemperatureEmissivityOp.class);
        }
    }
}
