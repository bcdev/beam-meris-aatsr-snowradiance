package org.esa.beam.snowradiance.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.snowradiance.operator.SnowRadianceMasterOp;

import javax.swing.JPanel;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

//    private final List<SourceProductSelector> sourceProductSelectorList;
//    private final Map<Field, SourceProductSelector> sourceProductSelectorMap;

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

//        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
//        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);
        // Fetch source products
//        initSourceProductSelectors(operatorSpi);
//        if (!sourceProductSelectorList.isEmpty()) {
//            setSourceProductSelectorLabels();
//            setSourceProductSelectorToolTipTexts();
//        }

//        final TableLayout tableLayout = new TableLayout(1);
//        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
//        tableLayout.setTableWeightX(1.0);
//        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
//        tableLayout.setTablePadding(3, 3);
//
//        JPanel ioParametersPanel = new JPanel(tableLayout);
//        for (SourceProductSelector selector : sourceProductSelectorList) {
//            ioParametersPanel.add(selector.createDefaultPanel());
//        }
//        ioParametersPanel.add(getTargetProductSelector().createDefaultPanel());
        


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
        // todo: verify
        return true;
    }

    protected Product createTargetProduct() throws Exception {
        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
        Product merisProduct = model.getMerisSourceProduct();
        Product aatsrProduct = model.getAatsrSourceProduct();
        sourceProducts.put("sourceMeris", merisProduct);
        sourceProducts.put("sourceAatsr", aatsrProduct);
        Product targetProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SnowRadianceMasterOp.class)
              , model.getSnowRadianceParameters(), sourceProducts);
        return targetProduct;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }


//    @Override
//    protected Product createTargetProduct() throws Exception {
//        final HashMap<String, Product> sourceProducts = createSourceProductsMap();
//        return GPF.createProduct(OperatorSpi.getOperatorAlias(SnowRadianceMasterOp.class), parameterMap, sourceProducts);
//    }
//
//    private HashMap<String, Product> createSourceProductsMap() {
//        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
//        for (Field field : sourceProductSelectorMap.keySet()) {
//            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
//            String key = field.getName();
//            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
//            if (!annot.alias().isEmpty()) {
//                key = annot.alias();
//            }
//            sourceProducts.put(key, selector.getSelectedProduct());
//        }
//        return sourceProducts;
//    }
//
//    private void initSourceProductSelectors(OperatorSpi operatorSpi) {
//        final Field[] fields = operatorSpi.getOperatorClass().getDeclaredFields();
//        for (Field field : fields) {
//            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
//            if (annot != null) {
//                final ProductFilter productFilter = new AnnotatedSourceProductFilter(annot);
//                SourceProductSelector sourceProductSelector = new SourceProductSelector(getAppContext());
//                sourceProductSelector.setProductFilter(productFilter);
//                sourceProductSelectorList.add(sourceProductSelector);
//                sourceProductSelectorMap.put(field, sourceProductSelector);
//            }
//        }
//    }
//
//    private void setSourceProductSelectorLabels() {
//        for (Field field : sourceProductSelectorMap.keySet()) {
//            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
//            String label = null;
//            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
//            if (!annot.label().isEmpty()) {
//                label = annot.label();
//            }
//            if (label == null && !annot.alias().isEmpty()) {
//                label = annot.alias();
//            }
//            if (label == null) {
//                String name = field.getName();
//                if (!annot.alias().isEmpty()) {
//                    name = annot.alias();
//                }
//                label = PropertyDescriptor.createDisplayName(name);
//            }
//            if (!label.endsWith(":")) {
//                label += ":";
//            }
//            selector.getProductNameLabel().setText(label);
//        }
//    }
//
//    private void setSourceProductSelectorToolTipTexts() {
//        for (Field field : sourceProductSelectorMap.keySet()) {
//            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
//
//            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
//            final String description = annot.description();
//            if (!description.isEmpty()) {
//                selector.getProductNameComboBox().setToolTipText(description);
//            }
//        }
//    }
//
//    private static class AnnotatedSourceProductFilter implements ProductFilter {
//
//        private final SourceProduct annot;
//
//        private AnnotatedSourceProductFilter(SourceProduct annot) {
//            this.annot = annot;
//        }
//
//        @Override
//        public boolean accept(Product product) {
//
//            if (!annot.type().isEmpty() && !product.getProductType().matches(annot.type())) {
//                return false;
//            }
//
//            for (String bandName : annot.bands()) {
//                if (!product.containsBand(bandName)) {
//                    return false;
//                }
//            }
//
//            return true;
//        }
//    }

}
