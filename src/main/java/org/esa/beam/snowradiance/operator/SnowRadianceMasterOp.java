package org.esa.beam.snowradiance.operator;

import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.datamodel.Product;

import java.util.Map;
import java.util.HashMap;

/**
 * Snow radiance 'master operator' (CURRENTLY NOT USED)
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
@OperatorMetadata(alias="SnowRadiance.Master")
public class SnowRadianceMasterOp extends Operator {

    @SourceProduct(alias = "source",
                   label = "Name (MERIS product)",
                   description = "Select a MERIS product for snow grains retrieval.")
    private Product merisProduct;

    @SourceProduct(alias = "source",
                   label = "Name (MERIS/AATSR colocated product)",
                   description = "Select a MERIS/AATSR colocated product for snow temperature retrieval.")
    private Product colocatedProduct;

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
               description = "Compute Unpolluted Snow Grain Size",
               label = "Compute Unpolluted Snow Grain Size")
    private boolean computeUnpollutedSnowGrainSize;

    @Parameter(defaultValue = "false",
               description = "Compute Polluted Snow Grain Size",
               label = "Compute Polluted Snow Grain Size")
    private boolean computePollutedSnowGrainSize;

    @Parameter(defaultValue = "true",
               description = "Compute Snow Albedo",
               label = "Compute Snow Albedo")
    private boolean computeSnowAlbedo;

    @Parameter(defaultValue = "M",
               description = "Master Product Bands Suffix",
               label = "Master Product Bands Suffix")
    private String masterProductBandsSuffix;

    @Parameter(defaultValue = "S",
               description = "Slave Product Bands Suffix",
               label = "Slave Product Bands Suffix")
    private String slaveProductBandsSuffix;

    @Parameter(alias = SnowRadianceConstants.LUT_PATH_PARAM_NAME,
               defaultValue = SnowRadianceConstants.LUT_PATH_PARAM_DEFAULT,
               description = SnowRadianceConstants.LUT_PATH_PARAM_DESCRIPTION,
               label = SnowRadianceConstants.LUT_PATH_PARAM_LABEL)
    private String lutPath;

//    @Parameter(alias = SnowRadianceConstants.LUT_PATH_PARAM_NAME,
//               defaultValue = SnowRadianceConstants.LUT_PATH_PARAM_DEFAULT,
//               description = SnowRadianceConstants.LUT_PATH_PARAM_DESCRIPTION,
//               label = SnowRadianceConstants.LUT_PATH_PARAM_LABEL)
//    private String fubLutPath;


    public void initialize() throws OperatorException {

        // compute the  FUB product...
        Product fubSnowTempProduct = null;
        if (computeSnowTemperatureFub || computeEmissivityFub) {
            Map<String, Product> fubSnowTempInput = new HashMap<String, Product>(1);
            fubSnowTempInput.put("source", colocatedProduct);
            Map<String, Object> fubSnowTempParams = new HashMap<String, Object>(4);
            fubSnowTempParams.put("computeSnowTemperatureFub", computeSnowTemperatureFub);
            fubSnowTempParams.put("masterProductBandsSuffix", masterProductBandsSuffix);
            fubSnowTempParams.put("slaveProductBandsSuffix", slaveProductBandsSuffix);
            fubSnowTempProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SnowTemperatureEmissivityOp.class), fubSnowTempParams, fubSnowTempInput);
        }

        // compute the  snow grains product...
        Product snowGrainsProduct = null;
        if (computeUnpollutedSnowGrainSize || computePollutedSnowGrainSize || computeSnowAlbedo) {
            Map<String, Product> snowGrainsInput = new HashMap<String, Product>(1);
            snowGrainsInput.put("source", merisProduct);
            Map<String, Object> snowGrainsParams = new HashMap<String, Object>(4);
            snowGrainsParams.put("computeUnpollutedSnowGrainSize", computeUnpollutedSnowGrainSize);
            snowGrainsParams.put("computePollutedSnowGrainSize", computePollutedSnowGrainSize);
            snowGrainsParams.put("computeSnowAlbedo", computeSnowAlbedo);
            fubSnowTempProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SnowGrainSizePollutionOp.class), snowGrainsParams, snowGrainsInput);
        }

//        targetProduct = fubSnowTempProduct;
        targetProduct = snowGrainsProduct;
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
