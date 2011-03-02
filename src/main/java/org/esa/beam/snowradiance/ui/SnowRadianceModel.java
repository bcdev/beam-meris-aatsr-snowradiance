package org.esa.beam.snowradiance.ui;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.snowradiance.operator.SnowRadianceConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Model for Snow Radiance parameters
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceModel {

    @SourceProduct(alias = "sourceMeris",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product merisSourceProduct;

    @SourceProduct(alias = "sourceAatsr",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product aatsrSourceProduct;

    // Target bands
    @Parameter(defaultValue = "false",
               description = "Copy input bands to target product",
               label = "Copy input bands")
    private boolean copyInputBands;

    @Parameter(defaultValue = "true",
               description = "Compute Snow Grain Size",
               label = "Compute snow grain size")
    private boolean computeSnowGrainSize = true;

    @Parameter(defaultValue = "false",
               description = "Only Compute Snow Grain Size and Pollution (requires MERIS only)",
               label = "Only Compute Snow Grain Size and Pollution (requires MERIS only)")
    private boolean computeSnowGrainSizePollutionOnly;

    @Parameter(defaultValue = "true",
               description = "Compute snow albedo",
               label = "Compute snow albedo")
    private boolean computeSnowAlbedo = true;

    @Parameter(defaultValue = "false",
               description = "Compute snow soot content",
               label = "Compute snow soot content")
    private boolean computeSnowSootContent = true;

    @Parameter(defaultValue = "true",
               description = "Compute Snow Temperature (FUB)",
               label = "Compute Snow Temperature (FUB)")
    private boolean computeSnowTemperatureFub = true;

    @Parameter(defaultValue = "true",
               description = "Compute Emissivity (FUB)",
               label = "Compute Emissivity (FUB)")
    private boolean computeEmissivityFub = true;


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
    private boolean applyCloudMask = true;

    @Parameter(defaultValue = "false",
               description = "Get cloud mask from feature classification (MERIS/AATSR Synergy)",
               label = "Cloud probability (MERIS/AATSR Synergy)")
    private boolean getCloudMaskFromSynergy;

    @Parameter(defaultValue = "true",
               description = "Apply 100% snow mask",
               label = "Apply 100% snow mask")
    private boolean apply100PercentSnowMask = true;

    @Parameter(defaultValue = "0.99", interval = "[0.0, 1.0]",
               description = "Assumed emissivity at 11 microns",
               label = "Assumed emissivity at 11 microns")
    private double assumedEmissivityAt11Microns = Double.parseDouble(SnowRadianceConstants.assumedEmissivity11MicronsDefaultValue);

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "Cloud probability threshold",
               label = "Cloud probability threshold")
    private double cloudProbabilityThreshold = Double.parseDouble(SnowRadianceConstants.cloudProbThresholdDefaultValue);

    @Parameter(defaultValue = "0.96", interval = "[0.0, 1.0]",
               description = "NDSI upper threshold",
               label = "NDSI upper threshold")
    private double ndsiUpperThreshold = Double.parseDouble(SnowRadianceConstants.ndsiUpperDefaultValue);

    @Parameter(defaultValue = "0.90", interval = "[0.0, 1.0]",
               description = "NDSI lower threshold",
               label = "NDSI lower threshold")
    private double ndsiLowerThreshold = Double.parseDouble(SnowRadianceConstants.ndsiLowerDefaultValue);

    @Parameter(defaultValue = "10.0", interval = "[1.0, 100.0]",
               description = "AATSR 1610nm upper threshold",
               label = "AATSR 1610nm upper threshold")
    private double aatsr1610UpperThreshold = Double.parseDouble(SnowRadianceConstants.aatsr1610UpperDefaultValue);

    @Parameter(defaultValue = "1.0", interval = "[1.0, 100.0]",
               description = "AATSR 1610nm lower threshold",
               label = "AATSR 1610nm lower threshold")
    private double aatsr1610LowerThreshold = Double.parseDouble(SnowRadianceConstants.aatsr1610LowerDefaultValue);

    @Parameter(defaultValue = "10.0", interval = "[1.0, 100.0]",
               description = "AATSR 670nm upper threshold",
               label = "AATSR 670nm upper threshold")
    private double aatsr0670UpperThreshold = Double.parseDouble(SnowRadianceConstants.aatsr0670UpperDefaultValue);

    @Parameter(defaultValue = "1.0", interval = "[1.0, 100.0]",
               description = "AATSR 670nm lower threshold",
               label = "AATSR 670nm lower threshold")
    private double aatsr0670LowerThreshold = Double.parseDouble(SnowRadianceConstants.aatsr0670LowerDefaultValue);



    private PropertyContainer propertyContainer;


    public SnowRadianceModel() {
        propertyContainer = PropertyContainer.createObjectBacked(this, new ParameterDescriptorFactory());
    }

    public Product getMerisSourceProduct() {
        return merisSourceProduct;
    }

    public Product getAatsrSourceProduct() {
        return aatsrSourceProduct;
    }

    public PropertyContainer getPropertyContainer() {
        return propertyContainer;
    }

    public void setComputeSnowTemperatureFub(boolean computeSnowTemperatureFub) {
        this.computeSnowTemperatureFub = computeSnowTemperatureFub;
    }

    public void setComputeEmissivityFub(boolean computeEmissivityFub) {
        this.computeEmissivityFub = computeEmissivityFub;
    }

    public void setComputeAatsrNdsi(boolean computeAatsrNdsi) {
        this.computeAatsrNdsi = computeAatsrNdsi;
    }

    public void setCopyAatsrL1Flags(boolean copyAatsrL1Flags) {
        this.copyAatsrL1Flags = copyAatsrL1Flags;
    }

    public void setGetCloudMaskFromSynergy(boolean getCloudMaskFromSynergy) {
        this.getCloudMaskFromSynergy = getCloudMaskFromSynergy;
    }

    public void setApply100PercentSnowMask(boolean apply100PercentSnowMask) {
        this.apply100PercentSnowMask = apply100PercentSnowMask;
    }

    public void setComputeSnowGrainSizePollutionOnly(boolean computeSnowGrainSizePollutionOnly) {
        this.computeSnowGrainSizePollutionOnly = computeSnowGrainSizePollutionOnly;
    }

    public Map<String, Object> getSnowRadianceParameters() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        configTargetBands(params);
        configProcessingParameters(params);
        return params;
    }

    private void configProcessingParameters(HashMap<String, Object> params) {
        params.put("applyCloudMask", applyCloudMask);
        params.put("getCloudMaskFromSynergy", getCloudMaskFromSynergy);
        params.put("apply100PercentSnowMask", apply100PercentSnowMask);
        params.put("assumedEmissivityAt11Microns", assumedEmissivityAt11Microns);
        params.put("cloudProbabilityThreshold", cloudProbabilityThreshold);
        params.put("ndsiUpperThreshold", ndsiUpperThreshold);
        params.put("ndsiLowerThreshold", ndsiLowerThreshold);
        params.put("aatsr1610UpperThreshold", aatsr1610UpperThreshold);
        params.put("aatsr1610LowerThreshold", aatsr1610LowerThreshold);
        params.put("aatsr0670UpperThreshold", aatsr0670UpperThreshold);
        params.put("aatsr0670LowerThreshold", aatsr0670LowerThreshold);
    }

    private void configTargetBands(HashMap<String, Object> params) {
        params.put("copyInputBands", copyInputBands);
        params.put("computeSnowGrainSize", computeSnowGrainSize);
        params.put("computeSnowGrainSizePollutionOnly", computeSnowGrainSizePollutionOnly);
        params.put("computeSnowAlbedo", computeSnowAlbedo);
        params.put("computeSnowSootContent", computeSnowSootContent);
        params.put("computeSnowTemperatureFub", computeSnowTemperatureFub);
        params.put("computeEmissivityFub", computeEmissivityFub);
        params.put("computeMerisWaterVapour", computeMerisWaterVapour);
        params.put("computeMerisNdvi", computeMerisNdvi);
        params.put("computeAatsrNdsi", computeAatsrNdsi);
        params.put("computeMerisMdsi", computeMerisMdsi);
        params.put("copyAatsrL1Flags", copyAatsrL1Flags);
    }

}
