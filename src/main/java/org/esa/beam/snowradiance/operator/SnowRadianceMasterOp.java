package org.esa.beam.snowradiance.operator;

import org.esa.beam.collocation.CollocateOp;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;
import org.esa.beam.synergy.operators.CreateSynergyOp;

import javax.media.jai.JAI;
import java.util.HashMap;
import java.util.Map;

/**
 * Snow radiance 'master operator'
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
@OperatorMetadata(alias = "SnowRadiance.Master",
                  version = "1.0",
                  authors = "Olaf Danne",
                  copyright = "(c) 2010 by Brockmann Consult",
                  description = "The master operator for the SnowRadiance processor.")
public class SnowRadianceMasterOp extends Operator {

    @SourceProduct(alias = "sourceMeris",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product merisSourceProduct;

    @SourceProduct(alias = "sourceAatsr",
                   optional = true,
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product aatsrSourceProduct;

    @Parameter(defaultValue = "true",
               description = "Only Compute Snow Grain Size and Pollution (requires MERIS only)",
               label = "Only Compute Snow Grain Size and Pollution (requires MERIS only)")
    private boolean computeSnowGrainSizePollutionOnly = true;

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

    @Parameter(defaultValue = "true",
               description = "Compute Snow Temperature (FUB)",
               label = "Compute Snow Temperature (FUB)")
    private boolean computeSnowTemperatureFub;

    @Parameter(defaultValue = "true",
               description = "Compute Emissivity (FUB)",
               label = "Compute Emissivity (FUB)")
    private boolean computeEmissivityFub;


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
               description = "Compute AATSR NDSI",
               label = "Compute AATSR NDSI")
    private boolean computeAatsrNdsi;

    @Parameter(defaultValue = "false",
               description = "Compute MERIS MDSI",
               label = "Compute MERIS MDSI")
    private boolean computeMerisMdsi;

    @Parameter(defaultValue = "false",
               description = "Copy AATSR L1 flags",
               label = "Copy AATSR L1 flags")
    private boolean copyAatsrL1Flags;


    // Processing parameters
    @Parameter(defaultValue = "true",
               description = "Apply cloud mask",
               label = "Apply cloud mask")
    private boolean applyCloudMask;

    @Parameter(defaultValue = "false",
               description = "Get cloud mask from feature classification (MERIS/AATSR Synergy)",
               label = "Cloud probability (MERIS/AATSR Synergy)")
    private boolean getCloudMaskFromSynergy;

    @Parameter(defaultValue = "true",
               description = "Apply 100% snow mask",
               label = "Apply 100% snow mask")
    private boolean apply100PercentSnowMask;

    @Parameter(defaultValue = "0.99", interval = "[0.0, 1.0]",
               description = "Assumed emissivity at 11 microns",
               label = "Assumed emissivity at 11 microns")
    private double assumedEmissivityAt11Microns;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "Cloud probability threshold",
               label = "Cloud probability threshold")
    private double cloudProbabilityThreshold;

    @Parameter(defaultValue = "0.96", interval = "[0.0, 1.0]",
               description = "NDSI upper threshold",
               label = "NDSI upper threshold")
    private double ndsiUpperThreshold;

    @Parameter(defaultValue = "0.90", interval = "[0.0, 1.0]",
               description = "NDSI lower threshold",
               label = "NDSI lower threshold")
    private double ndsiLowerThreshold;

    @Parameter(defaultValue = "10.0", interval = "[1.0, 100.0]",
               description = "AATSR 1610nm upper threshold",
               label = "AATSR 1610nm upper threshold")
    private double aatsr1610UpperThreshold;

    @Parameter(defaultValue = "1.0", interval = "[1.0, 100.0]",
               description = "AATSR 1610nm lower threshold",
               label = "AATSR 1610nm lower threshold")
    private double aatsr1610LowerThreshold;

    @Parameter(defaultValue = "10.0", interval = "[1.0, 100.0]",
               description = "AATSR 670nm upper threshold",
               label = "AATSR 670nm upper threshold")
    private double aatsr0670UpperThreshold = Double.parseDouble(SnowRadianceConstants.aatsr0670UpperDefaultValue);

    @Parameter(defaultValue = "1.0", interval = "[1.0, 100.0]",
               description = "AATSR 670nm lower threshold",
               label = "AATSR 670nm lower threshold")
    private double aatsr0670LowerThreshold = Double.parseDouble(SnowRadianceConstants.aatsr0670LowerDefaultValue);


    @TargetProduct(description = "The target product.")
    private Product targetProduct;


    public void initialize() throws OperatorException {

        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose
        SnowRadianceUtils.validateMerisProduct(merisSourceProduct);

        Product colocatedProduct = null;
        Product snowPropertiesProduct = null;
        if (computeSnowTemperatureFub || computeEmissivityFub ||
                computeSnowGrainSize || computeSnowSootContent || computeSnowAlbedo) {

            if (computeSnowGrainSizePollutionOnly) {
                Map<String, Product> snowPropertiesInput = new HashMap<String, Product>(2);
                snowPropertiesInput.put("merisProduct", merisSourceProduct);
                Map<String, Object> snowPropertiesParams = new HashMap<String, Object>(4);
                snowPropertiesParams.put("applyCloudMask", applyCloudMask);
                snowPropertiesParams.put("copyInputBands", copyInputBands);
                snowPropertiesParams.put("computeSnowGrainSize", computeSnowGrainSize);
                snowPropertiesParams.put("computeSnowAlbedo", computeSnowAlbedo);
                snowPropertiesParams.put("computeSnowSootContent", computeSnowSootContent);
                snowPropertiesParams.put("computeMerisWaterVapour", computeMerisWaterVapour);
                snowPropertiesParams.put("computeMerisNdvi", computeMerisNdvi);
                snowPropertiesParams.put("computeMerisMdsi", computeMerisMdsi);
                snowPropertiesParams.put("cloudProbabilityThreshold", cloudProbabilityThreshold);

                snowPropertiesProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SnowGrainSizePollutionOp.class), snowPropertiesParams, snowPropertiesInput);
            } else {
                SnowRadianceUtils.validateAatsrProduct(aatsrSourceProduct);

                Map<String, Product> collocateInput = new HashMap<String, Product>(2);
                Map<String, Object> collocateParams = new HashMap<String, Object>(2);
                if (getCloudMaskFromSynergy) {
                    // get the colocated product from Synergy...
                    JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
                    collocateInput.put("MERIS", merisSourceProduct);
                    collocateInput.put("AATSR", aatsrSourceProduct);
                    collocateParams.put("subsetOvAreas", false);
                    colocatedProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateSynergyOp.class), collocateParams, collocateInput);
                } else {
                    collocateInput.put("masterProduct", merisSourceProduct);
                    collocateInput.put("slaveProduct", aatsrSourceProduct);
                    collocateParams.put("masterComponentPattern", "${ORIGINAL_NAME}_MERIS");
                    collocateParams.put("slaveComponentPattern", "${ORIGINAL_NAME}_AATSR");
                    colocatedProduct =
                            GPF.createProduct(OperatorSpi.getOperatorAlias(CollocateOp.class), collocateParams, collocateInput);
                }
                colocatedProduct.setProductType(merisSourceProduct.getProductType());

                // Fix collocation output (tie point grids lost their units and descriptions)
                for (TiePointGrid tpg : colocatedProduct.getTiePointGrids()) {
                    tpg.setUnit(merisSourceProduct.getTiePointGrid(tpg.getName()).getUnit());
                    tpg.setDescription(merisSourceProduct.getTiePointGrid(tpg.getName()).getDescription());
                }

                Map<String, Product> snowPropertiesInput = new HashMap<String, Product>(2);
                snowPropertiesInput.put("colocatedProduct", colocatedProduct);
                snowPropertiesInput.put("merisProduct", merisSourceProduct);
                Map<String, Object> snowPropertiesParams = new HashMap<String, Object>(4);
                snowPropertiesParams.put("applyCloudMask", applyCloudMask);
                snowPropertiesParams.put("getCloudMaskFromSynergy", getCloudMaskFromSynergy);
                snowPropertiesParams.put("apply100PercentSnowMask", apply100PercentSnowMask);
                snowPropertiesParams.put("copyInputBands", copyInputBands);
                snowPropertiesParams.put("computeSnowGrainSize", computeSnowGrainSize);
                snowPropertiesParams.put("computeSnowGrainSizePollutionOnly", computeSnowGrainSizePollutionOnly);
                snowPropertiesParams.put("computeSnowAlbedo", computeSnowAlbedo);
                snowPropertiesParams.put("computeSnowSootContent", computeSnowSootContent);
                snowPropertiesParams.put("computeEmissivityFub", computeEmissivityFub);
                snowPropertiesParams.put("computeSnowTemperatureFub", computeSnowTemperatureFub);
                snowPropertiesParams.put("computeMerisWaterVapour", computeMerisWaterVapour);
                snowPropertiesParams.put("computeMerisNdvi", computeMerisNdvi);
                snowPropertiesParams.put("computeAatsrNdsi", computeAatsrNdsi);
                snowPropertiesParams.put("computeMerisMdsi", computeMerisMdsi);
                snowPropertiesParams.put("copyAatsrL1Flags", copyAatsrL1Flags);
                snowPropertiesParams.put("assumedEmissivityAt11Microns", assumedEmissivityAt11Microns);
                snowPropertiesParams.put("cloudProbabilityThreshold", cloudProbabilityThreshold);
                snowPropertiesParams.put("ndsiUpperThreshold", ndsiUpperThreshold);
                snowPropertiesParams.put("ndsiLowerThreshold", ndsiLowerThreshold);
                snowPropertiesParams.put("aatsr1610UpperThreshold", aatsr1610UpperThreshold);
                snowPropertiesParams.put("aatsr1610LowerThreshold", aatsr1610LowerThreshold);
                snowPropertiesParams.put("aatsr0670UpperThreshold", aatsr0670UpperThreshold);
                snowPropertiesParams.put("aatsr0670LowerThreshold", aatsr0670LowerThreshold);

                SnowRadianceUtils.validateParameters(snowPropertiesParams);
                snowPropertiesProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SnowAllPropertiesOp.class), snowPropertiesParams, snowPropertiesInput);
            }
        } else {
            String message = "Nothing to do - select at least one snow property to be computed.";
            SnowRadianceUtils.logErrorMessage(message);
        }

        targetProduct = snowPropertiesProduct;
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SnowRadianceMasterOp.class);
        }
    }
}
