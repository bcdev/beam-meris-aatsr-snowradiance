package org.esa.beam.snowradiance.ui;

import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceModel {

    @SourceProduct(alias = "sourceMeris",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product merisProduct;

    @SourceProduct(alias = "sourceAatsr",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product aatsrProduct;

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

    @Parameter(defaultValue = "true",
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
               description = "Compute MERIS NDSI",
               label = "Compute MERIS NDSI")
    private boolean computeMerisNdsi;

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

//    @Parameter(defaultValue = "false",
//               description = "Get cloud mask from cloud probability (MEPIX)",
//               label = "Cloud probability (MEPIX)")
//    private boolean getCloudMaskFromMepix;

    @Parameter(defaultValue = "true",
               description = "Get cloud mask from feature classification (MERIS/AATSR Synergy)",
               label = "Cloud probability (MERIS/AATSR Synergy)")
    private boolean getCloudMaskFromSynergy;

    @Parameter(defaultValue = "true",
               description = "Apply 100% snow mask",
               label = "Apply 100% snow mask")
    private boolean apply100PercentSnowMask;

    @Parameter(defaultValue = "false",
               description = "Apply 100% snow mask with AATSR as master",
               label = "AATSR as master")
    private boolean use100PercentSnowMaskWithAatsrMaster;

//    @Parameter(defaultValue = "false",
//               description = "Apply 100% snow mask with MERIS as master",
//               label = "MERIS as master")
//    private boolean use100PercentSnowMaskWithMerisMaster;

    @Parameter(defaultValue = "0.99", interval = "[0.0, 1.0]",
               description = "Assumed emissivity at 11 microns",
               label = "Assumed emissivity at 11 microns")
    private double assumedEmissivityAt11Microns;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "Cloud probability threshold",
               label = "Cloud probability threshold")
    private double cloudProbabilityThreshold;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "NDSI upper threshold",
               label = "NDSI upper threshold")
    private double ndsiUpperThreshold;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "NDSI lower threshold",
               label = "NDSI lower threshold")
    private double ndsiLowerThreshold;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "AATSR 1610um upper threshold",
               label = "AATSR 1610um upper threshold")
    private double aatsr1610UpperThreshold;

    @Parameter(defaultValue = "0.8", interval = "[0.0, 1.0]",
               description = "AATSR 1610um lower threshold",
               label = "AATSR 1610um lower threshold")
    private double aatsr1610LowerThreshold;


    private Product merisSourceProduct;
    private Product aatsrSourceProduct;
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

    public Map<String, Object> getSnowRadianceParameters() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        configTargetBands(params);
        configProcessingParameters(params);
        return params;
    }

    private void configProcessingParameters(HashMap<String, Object> params) {
        params.put("applyCloudMask", applyCloudMask);
//        params.put("getCloudMaskFromMepix", getCloudMaskFromMepix);
        params.put("getCloudMaskFromSynergy", getCloudMaskFromSynergy);
        params.put("apply100PercentSnowMask", apply100PercentSnowMask);
        params.put("use100PercentSnowMaskWithAatsrMaster", use100PercentSnowMaskWithAatsrMaster);
//        params.put("use100PercentSnowMaskWithMerisMaster", use100PercentSnowMaskWithMerisMaster);
        params.put("assumedEmissivityAt11Microns", assumedEmissivityAt11Microns);
        params.put("cloudProbabilityThreshold", cloudProbabilityThreshold);
        params.put("ndsiUpperThreshold", ndsiUpperThreshold);
        params.put("ndsiLowerThreshold", ndsiLowerThreshold);
        params.put("pureSnowAatsr1610UpperThreshold", aatsr1610UpperThreshold);
        params.put("pureSnowAatsr1610LowerThreshold", aatsr1610LowerThreshold);
    }

    private void configTargetBands(HashMap<String, Object> params) {
        params.put("copyInputBands", copyInputBands);
        params.put("computeSnowGrainSize", computeSnowGrainSize);
        params.put("computeSnowAlbedo", computeSnowAlbedo);
        params.put("computeSnowSootContent", computeSnowSootContent);
        params.put("computeSnowTemperatureFub", computeSnowTemperatureFub);
        params.put("computeEmissivityFub", computeEmissivityFub);
        params.put("computeMerisWaterVapour", computeMerisWaterVapour);
        params.put("computeMerisNdvi", computeMerisNdvi);
        params.put("computeMerisNdsi", computeMerisNdsi);
        params.put("computeMerisMdsi", computeMerisMdsi);
        params.put("copyAatsrL1Flags", copyAatsrL1Flags);
    }

}
