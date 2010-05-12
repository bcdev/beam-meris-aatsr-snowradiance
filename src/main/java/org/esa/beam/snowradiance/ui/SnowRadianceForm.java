package org.esa.beam.snowradiance.ui;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.dataio.envisat.EnvisatProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.snowradiance.operator.SnowRadianceConstants;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceForm extends JTabbedPane {

    private TargetProductSelector targetProductSelector;
    private SourceProductSelector merisSourceProductSelector;
    private SourceProductSelector aatsrSourceProductSelector;

    private JCheckBox copyInputBandsCheckBox;
    private JCheckBox computeSnowGrainSizeCheckBox;
    private JCheckBox computeSnowAlbedoCheckBox;
    private JCheckBox computeSnowSootContentCheckBox;
    private JCheckBox computeSnowTemperatureFubCheckBox;
    private JCheckBox computeEmissivityFubCheckBox;

    private JCheckBox computeMerisWaterVapourCheckBox;
    private JCheckBox computeMerisNdviCheckBox;
    private JCheckBox computeMerisNdsiCheckBox;
    private JCheckBox computeMerisMdsiCheckBox;
    private JCheckBox copyAatsrL1FlagsCheckBox;


    private JCheckBox applyCloudMaskCheckBox;
    private ButtonGroup cloudMaskGroup;
    private JRadioButton getCloudMaskFromMepixRadioButton;
    private JRadioButton getCloudMaskFromSynergyRadioButton;
    private ButtonGroup snowMaskGroup;
    private JCheckBox apply100PercentSnowMaskCheckBox;
    private JRadioButton use100PercentSnowMaskWithAatsrMasterRadioButton;
    private JRadioButton use100PercentSnowMaskWithMerisMasterRadioButton;
    private JFormattedTextField assumedEmissivityAt11MicronsTextField;
    private JFormattedTextField cloudProbabilityThresholdTextField;
    private JFormattedTextField ndsiUpperThresholdTextField;
    private JFormattedTextField ndsiLowerThresholdTextField;
    private JFormattedTextField aatsr1610UpperThresholdTextField;
    private JFormattedTextField aatsr1610LowerThresholdTextField;


    private SnowRadianceModel snowRadianceModel;

    public SnowRadianceForm(AppContext appContext, SnowRadianceModel snowRadianceModel, TargetProductSelector targetProductSelector) {
       this.snowRadianceModel = snowRadianceModel;
       this.targetProductSelector = targetProductSelector;

       merisSourceProductSelector = new SourceProductSelector(appContext, "MERIS L1b:");
       aatsrSourceProductSelector = new SourceProductSelector(appContext, "AATSR L1b:");

       JComboBox sourceComboBox = merisSourceProductSelector.getProductNameComboBox();
       sourceComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setTargetProductName();
            }
       });

       initComponents();
       bindComponents();
       updateUIState();
    }

    private void bindComponents() {
        final BindingContext bc = new BindingContext(snowRadianceModel.getPropertyContainer());
        bc.bind("merisProduct", merisSourceProductSelector.getProductNameComboBox());
        bc.bind("aatsrProduct", aatsrSourceProductSelector.getProductNameComboBox());

        bc.bind("copyInputBands", copyInputBandsCheckBox);
        bc.bind("computeSnowGrainSize", computeSnowGrainSizeCheckBox);
        bc.bind("computeSnowAlbedo", computeSnowAlbedoCheckBox);
        bc.bind("computeSnowSootContent", computeSnowSootContentCheckBox);
        bc.bind("computeSnowTemperatureFub", computeSnowTemperatureFubCheckBox);
        bc.bind("computeEmissivityFub", computeEmissivityFubCheckBox);
        bc.bind("computeMerisWaterVapour", computeMerisWaterVapourCheckBox);
        bc.bind("computeMerisNdvi", computeMerisNdviCheckBox);
        bc.bind("computeMerisNdsi", computeMerisNdsiCheckBox);
        bc.bind("computeMerisMdsi", computeMerisMdsiCheckBox);
        bc.bind("copyAatsrL1Flags", copyAatsrL1FlagsCheckBox);
        
        bc.bind("applyCloudMask", applyCloudMaskCheckBox);
        bc.bind("apply100PercentSnowMask", apply100PercentSnowMaskCheckBox);
        bc.bind("assumedEmissivityAt11Microns", assumedEmissivityAt11MicronsTextField);
        bc.bind("cloudProbabilityThreshold", cloudProbabilityThresholdTextField);
        bc.bind("ndsiUpperThreshold", ndsiUpperThresholdTextField);
        bc.bind("ndsiLowerThreshold", ndsiLowerThresholdTextField);
        bc.bind("aatsr1610UpperThreshold", aatsr1610UpperThresholdTextField);
        bc.bind("aatsr1610LowerThreshold", aatsr1610LowerThresholdTextField);

        Map<AbstractButton, Object> cloudMaskGroupValueSet = new HashMap<AbstractButton, Object>(4);
        cloudMaskGroupValueSet.put(getCloudMaskFromMepixRadioButton, false);
        cloudMaskGroupValueSet.put(getCloudMaskFromSynergyRadioButton, true);
        bc.bind("getCloudMaskFromSynergy", cloudMaskGroup, cloudMaskGroupValueSet);

        Map<AbstractButton, Object> snowMaskGroupValueSet = new HashMap<AbstractButton, Object>(4);
        snowMaskGroupValueSet.put(use100PercentSnowMaskWithAatsrMasterRadioButton, true);
        snowMaskGroupValueSet.put(use100PercentSnowMaskWithMerisMasterRadioButton, false);
        bc.bind("use100PercentSnowMaskWithAatsrMaster", snowMaskGroup, snowMaskGroupValueSet);

    }

    private void initComponents() {
        setPreferredSize(new Dimension(600, 600));

        copyInputBandsCheckBox = new JCheckBox(SnowRadianceConstants.copyInputBandsLabel);
        computeSnowGrainSizeCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowGrainSizeLabel, true);
        computeSnowAlbedoCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowAlbedoLabel, true);
        computeSnowSootContentCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowSootContentLabel, true);
        computeEmissivityFubCheckBox = new JCheckBox(SnowRadianceConstants.computeEmissivityLabel, true);
        computeSnowTemperatureFubCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowTemperatureLabel, true);

        computeMerisWaterVapourCheckBox = new JCheckBox(SnowRadianceConstants.waterVapourMerisLabel);
        computeMerisNdviCheckBox = new JCheckBox(SnowRadianceConstants.ndviMerisLabel);
        computeMerisNdsiCheckBox = new JCheckBox(SnowRadianceConstants.ndsiMerisLabel);
        computeMerisMdsiCheckBox = new JCheckBox(SnowRadianceConstants.mdsiMerisLabel);
        copyAatsrL1FlagsCheckBox = new JCheckBox(SnowRadianceConstants.aatsrL1FlagsLabel);

        applyCloudMaskCheckBox = new JCheckBox(SnowRadianceConstants.applyCloudMaskLabel, true);
        cloudMaskGroup = new ButtonGroup();
        getCloudMaskFromMepixRadioButton = new JRadioButton(SnowRadianceConstants.applyCloudMaskMepixLabel);
        getCloudMaskFromMepixRadioButton.setSelected(false);
        getCloudMaskFromSynergyRadioButton = new JRadioButton(SnowRadianceConstants.applyCloudMaskSynergyLabel);
        getCloudMaskFromSynergyRadioButton.setSelected(true);
        cloudMaskGroup.add(getCloudMaskFromMepixRadioButton);
        cloudMaskGroup.add(getCloudMaskFromSynergyRadioButton);

        apply100PercentSnowMaskCheckBox = new JCheckBox(SnowRadianceConstants.applySnowMaskLabel);
        ActionListener snowMaskActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
		};
        apply100PercentSnowMaskCheckBox.addActionListener(snowMaskActionListener);
        snowMaskGroup = new ButtonGroup();
        use100PercentSnowMaskWithAatsrMasterRadioButton = new JRadioButton(SnowRadianceConstants.applySnowMaskWithAatsrMasterLabel);
        use100PercentSnowMaskWithAatsrMasterRadioButton.setSelected(true);
        use100PercentSnowMaskWithMerisMasterRadioButton = new JRadioButton(SnowRadianceConstants.applySnowMaskWithMerisMasterLabel);
        use100PercentSnowMaskWithMerisMasterRadioButton.setSelected(false);
        snowMaskGroup.add(use100PercentSnowMaskWithAatsrMasterRadioButton);
        snowMaskGroup.add(use100PercentSnowMaskWithMerisMasterRadioButton);

        assumedEmissivityAt11MicronsTextField = new JFormattedTextField(SnowRadianceConstants.assumedEmissivity11MicronsDefaultValue);
        cloudProbabilityThresholdTextField = new JFormattedTextField(SnowRadianceConstants.cloudProbThresholdLabel);
        ndsiLowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiLowerDefaultValue);
        ndsiUpperThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiUpperDefaultValue);
        aatsr1610LowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.aatsr1610LowerDefaultValue);
        aatsr1610UpperThresholdTextField = new JFormattedTextField(SnowRadianceConstants.aatsr1610UpperDefaultValue);
        ndsiLowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiLowerDefaultValue);
        ndsiLowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiLowerDefaultValue);

        TableLayout layoutIO = new TableLayout(1);
        layoutIO.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layoutIO.setTableFill(TableLayout.Fill.HORIZONTAL);
        layoutIO.setTableWeightX(1);
        layoutIO.setCellWeightY(2, 0, 1);
        layoutIO.setTablePadding(2, 2);

        TableLayout targetBandsParam = new TableLayout(1);
        targetBandsParam.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        targetBandsParam.setTableFill(TableLayout.Fill.HORIZONTAL);
        targetBandsParam.setTableWeightX(1);
        targetBandsParam.setCellWeightY(0, 0, 1);
        targetBandsParam.setTablePadding(2, 2);

        TableLayout processingParam = new TableLayout(1);
        processingParam.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        processingParam.setTableFill(TableLayout.Fill.HORIZONTAL);
        processingParam.setTableWeightX(1);
        processingParam.setCellWeightY(3, 0, 1);
        processingParam.setTablePadding(2, 2);

        JPanel ioTab = new JPanel(layoutIO);
        JPanel targetBandsParamTab = new JPanel(targetBandsParam);
        JPanel processingParamTab = new JPanel(processingParam);
        addTab("I/O Parameters", ioTab);
        addTab("Target Bands", targetBandsParamTab);
        addTab("Processing Parameters", processingParamTab);

        JPanel merisInputPanel = merisSourceProductSelector.createDefaultPanel();
        ioTab.add(merisInputPanel);
        JPanel aatsrInputPanel = aatsrSourceProductSelector.createDefaultPanel();
        ioTab.add(aatsrInputPanel);
		ioTab.add(targetProductSelector.createDefaultPanel());
		ioTab.add(new JLabel(""));

        JPanel targetBandsPanel = createTargetBandSelectionPanel();
        targetBandsParamTab.add(targetBandsPanel);

        JPanel processingParamsPanel = createProcessingParamsPanel();
        processingParamTab.add(processingParamsPanel);
    }

    private JPanel createTargetBandSelectionPanel() {
		TableLayout layout = new TableLayout(1);
		layout.setTableAnchor(TableLayout.Anchor.WEST);
		layout.setTableFill(TableLayout.Fill.HORIZONTAL);
		layout.setTableWeightX(1);
		layout.setTablePadding(2, 2);

		JPanel panel = new JPanel(layout);
//		panel.setBorder(BorderFactory.createTitledBorder(null,
//				"", TitledBorder.DEFAULT_JUSTIFICATION,
//				TitledBorder.DEFAULT_POSITION, new Font("Tahoma", 0, 11),
//				new Color(0, 70, 213)));

        panel.add(copyInputBandsCheckBox);
        panel.add(computeSnowGrainSizeCheckBox);
        panel.add(computeSnowAlbedoCheckBox);
        panel.add(computeSnowSootContentCheckBox);
        panel.add(computeEmissivityFubCheckBox);
        panel.add(computeSnowTemperatureFubCheckBox);

        panel.add(new JLabel(" "));
        panel.add(new JLabel(" "));
        panel.add(new JLabel(SnowRadianceConstants.complementaryQuantitiesLabel));
        panel.add(new JLabel(" "));
        panel.add(computeMerisWaterVapourCheckBox);
        panel.add(computeMerisNdviCheckBox);
        panel.add(computeMerisNdsiCheckBox);
        panel.add(computeMerisMdsiCheckBox);
        panel.add(copyAatsrL1FlagsCheckBox);

		return panel;
	}

    public JPanel createProcessingParamsPanel() {

        TableLayout layout = new TableLayout(5);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(10, 5);     // space between columns/rows
        final JPanel panel = new JPanel(layout);

        int rowIndex = 0;

        layout.setCellColspan(rowIndex, 0, 4);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(new JLabel(SnowRadianceConstants.parametersForMaskingLabel), new TableLayout.Cell(rowIndex, 0));
        rowIndex++;

        layout.setCellPadding(rowIndex, 0, new Insets(0, 12, 0, 0));
        layout.setCellPadding(rowIndex, 1, new Insets(0, 60, 0, 0));
        layout.setCellPadding(rowIndex, 2, new Insets(0, 120, 0, 0));
        layout.setCellPadding(rowIndex, 3, new Insets(0, 180, 0, 0));
//        layout.setCellWeightX(rowIndex, 0, 1.0);
//        layout.setCellWeightX(rowIndex, 1, 0.2);
//        layout.setCellWeightX(rowIndex, 2, 0.2);
//        layout.setCellWeightX(rowIndex, 3, 0.2);
//        layout.setCellWeightX(rowIndex, 4, 0.2);
        panel.add(new JLabel(SnowRadianceConstants.ndsiLowerLabel), new TableLayout.Cell(rowIndex, 0));
        panel.add(ndsiLowerThresholdTextField, new TableLayout.Cell(rowIndex, 1));
        panel.add(new JLabel(SnowRadianceConstants.ndsiUpperLabel), new TableLayout.Cell(rowIndex, 2));
        panel.add(ndsiUpperThresholdTextField, new TableLayout.Cell(rowIndex, 3));
        panel.add(new JLabel("bla"), new TableLayout.Cell(rowIndex, 4));
        rowIndex++;

        rowIndex = addCloudMaskParameters(layout, panel, rowIndex);
        rowIndex = addEmptyLine(panel, rowIndex);
        rowIndex = addSnowMaskParameters(layout, panel, rowIndex);

        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        layout.setCellWeightY(rowIndex, 0, 1.0);
        panel.add(new JPanel());

        return panel;
    }

    private int addCloudMaskParameters(TableLayout layout, JPanel panel, int rowIndex) {

        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(applyCloudMaskCheckBox, new TableLayout.Cell(rowIndex, 0));
        rowIndex++;

        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellPadding(rowIndex, 0, new Insets(0, 24, 0, 0));
        layout.setCellPadding(rowIndex, 1, new Insets(0, 24, 0, 0));
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(getCloudMaskFromMepixRadioButton, new TableLayout.Cell(rowIndex, 0));

        rowIndex++;

        layout.setCellPadding(rowIndex, 0, new Insets(0, 24, 0, 0));
        layout.setCellPadding(rowIndex, 1, new Insets(0, 24, 0, 0));
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(getCloudMaskFromSynergyRadioButton, new TableLayout.Cell(rowIndex, 0));
        rowIndex++;

        return rowIndex;
    }



    private int addSnowMaskParameters(TableLayout layout, JPanel panel, int rowIndex) {

        rowIndex++;
        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(apply100PercentSnowMaskCheckBox, new TableLayout.Cell(rowIndex, 0));
        rowIndex++;

        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellPadding(rowIndex, 0, new Insets(0, 24, 0, 0));
        layout.setCellPadding(rowIndex, 1, new Insets(0, 24, 0, 0));
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(use100PercentSnowMaskWithAatsrMasterRadioButton, new TableLayout.Cell(rowIndex, 0));

        rowIndex++;

        layout.setCellPadding(rowIndex, 0, new Insets(0, 24, 0, 0));
        layout.setCellPadding(rowIndex, 1, new Insets(0, 24, 0, 0));
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(use100PercentSnowMaskWithMerisMasterRadioButton, new TableLayout.Cell(rowIndex, 0));

        rowIndex++;

        return rowIndex;
    }

    private int addEmptyLine(JPanel panel, int rowIndex) {
        panel.add(new JLabel(" "), new TableLayout.Cell(rowIndex, 0));
        rowIndex++;
        return rowIndex;
    }

    private void updateUIState() {
        updateSnowMaskUIstate();
    }

    private void updateSnowMaskUIstate() {
        boolean maskSnowSelected = apply100PercentSnowMaskCheckBox.isSelected();
        use100PercentSnowMaskWithAatsrMasterRadioButton.setEnabled(maskSnowSelected);
        use100PercentSnowMaskWithMerisMasterRadioButton.setEnabled(maskSnowSelected);
    }

    public void prepareShow() {
	    merisSourceProductSelector.initProducts();
	    aatsrSourceProductSelector.initProducts();
    }

	public void prepareHide() {
	    merisSourceProductSelector.releaseProducts();
	    aatsrSourceProductSelector.releaseProducts();
    }

    private void setTargetProductName() {
        final Product sourceProduct = merisSourceProductSelector.getSelectedProduct();
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (sourceProduct != null) {
            String sourceProductName = sourceProduct.getName();
            String targetProductName = sourceProductName.substring(0,4) + "ATS_2SNOWRAD" +
                       sourceProductName.substring(14,sourceProductName.length()-3);
            selectorModel.setProductName(targetProductName);
        }
    }
}
