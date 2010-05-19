package org.esa.beam.snowradiance.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.snowradiance.operator.SnowRadianceMasterOp;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceDialog extends SingleTargetProductDialog {
    public static final String TITLE = "Snow Radiance Processor - v1.0";
    private SnowRadianceForm form;
    private SnowRadianceModel model;

    private String targetProductNameSuffix;

    private Map<String, Object> parameterMap;

    public SnowRadianceDialog(AppContext appContext)  {
        super(appContext, TITLE, "snowPropertiesProcessor");
        targetProductNameSuffix = "";

        parameterMap = new HashMap<String, Object>(17);

        final String operatorName = OperatorSpi.getOperatorAlias(SnowRadianceMasterOp.class);
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName");
        }

        model = new SnowRadianceModel();
        form = new SnowRadianceForm(appContext, model, getTargetProductSelector());
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

    @Override
    protected boolean verifyUserInput() {
        SnowRadianceUtils.validateMerisProduct(model.getMerisSourceProduct());
        SnowRadianceUtils.validateAatsrProduct(model.getAatsrSourceProduct());

        final Map<String, Object> parameterMap = model.getSnowRadianceParameters();
        SnowRadianceUtils.validateParameters(parameterMap);

        // if all validations passed:
        return true;
    }

    protected Product createTargetProduct() throws Exception {
        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
        Product merisSourceProduct = model.getMerisSourceProduct();
        Product aatsrSourceProduct = model.getAatsrSourceProduct();
        sourceProducts.put("merisSourceProduct", merisSourceProduct);
        sourceProducts.put("aatsrSourceProduct", aatsrSourceProduct);
        Product targetProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SnowRadianceMasterOp.class)
              , model.getSnowRadianceParameters(), sourceProducts);
        return targetProduct;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

}
