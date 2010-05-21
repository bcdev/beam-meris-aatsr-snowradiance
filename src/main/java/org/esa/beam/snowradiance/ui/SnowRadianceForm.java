package org.esa.beam.snowradiance.ui;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.snowradiance.operator.SnowRadianceConstants;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceForm extends JTabbedPane {

    private TargetProductSelector targetProductSelector;
    private SourceProductSelector merisSourceProductSelector;

    private JCheckBox computeSnowGrainSizePollutionOnlyCheckBox;

    private SourceProductSelector aatsrSourceProductSelector;
    private JCheckBox copyInputBandsCheckBox;
    private JCheckBox computeSnowGrainSizeCheckBox;
    private JCheckBox computeSnowAlbedoCheckBox;
    private JCheckBox computeSnowSootContentCheckBox;
    private JCheckBox computeSnowTemperatureFubCheckBox;
    private JCheckBox computeEmissivityFubCheckBox;

    private JCheckBox computeMerisWaterVapourCheckBox;
    private JCheckBox computeMerisNdviCheckBox;
    private JCheckBox computeAatsrNdsiCheckBox;
    private JCheckBox computeMerisMdsiCheckBox;
    private JCheckBox copyAatsrL1FlagsCheckBox;


    private JCheckBox applyCloudMaskCheckBox;
    private ButtonGroup cloudMaskGroup;
    private JRadioButton getCloudMaskFromMepixRadioButton;
    private JRadioButton getCloudMaskFromSynergyRadioButton;
    private JCheckBox apply100PercentSnowMaskCheckBox;
    private JFormattedTextField assumedEmissivityAt11MicronsTextField;
    private JFormattedTextField cloudProbabilityThresholdTextField;
    private JFormattedTextField ndsiUpperThresholdTextField;
    private JFormattedTextField ndsiLowerThresholdTextField;
    private JFormattedTextField aatsr1610UpperThresholdTextField;
    private JFormattedTextField aatsr1610LowerThresholdTextField;
    private JFormattedTextField aatsr0670UpperThresholdTextField;
    private JFormattedTextField aatsr0670LowerThresholdTextField;


    private SnowRadianceModel snowRadianceModel;
    private JPanel aatsrInputPanel;

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
        bc.bind("merisSourceProduct", merisSourceProductSelector.getProductNameComboBox());
        bc.bind("aatsrSourceProduct", aatsrSourceProductSelector.getProductNameComboBox());

        bc.bind("computeSnowGrainSizePollutionOnly", computeSnowGrainSizePollutionOnlyCheckBox);

        bc.bind("copyInputBands", copyInputBandsCheckBox);
        bc.bind("computeSnowGrainSize", computeSnowGrainSizeCheckBox);
        bc.bind("computeSnowAlbedo", computeSnowAlbedoCheckBox);
        bc.bind("computeSnowSootContent", computeSnowSootContentCheckBox);
        bc.bind("computeSnowTemperatureFub", computeSnowTemperatureFubCheckBox);
        bc.bind("computeEmissivityFub", computeEmissivityFubCheckBox);
        bc.bind("computeMerisWaterVapour", computeMerisWaterVapourCheckBox);
        bc.bind("computeMerisNdvi", computeMerisNdviCheckBox);
        bc.bind("computeAatsrNdsi", computeAatsrNdsiCheckBox);
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
        bc.bind("aatsr0670UpperThreshold", aatsr0670UpperThresholdTextField);
        bc.bind("aatsr0670LowerThreshold", aatsr0670LowerThresholdTextField);

        Map<AbstractButton, Object> cloudMaskGroupValueSet = new HashMap<AbstractButton, Object>(4);
        cloudMaskGroupValueSet.put(getCloudMaskFromMepixRadioButton, false);
        cloudMaskGroupValueSet.put(getCloudMaskFromSynergyRadioButton, true);
        bc.bind("getCloudMaskFromSynergy", cloudMaskGroup, cloudMaskGroupValueSet);

    }

    private void initComponents() {
        setPreferredSize(new Dimension(600, 600));

        copyInputBandsCheckBox = new JCheckBox(SnowRadianceConstants.copyInputBandsLabel);
        computeSnowGrainSizePollutionOnlyCheckBox= new JCheckBox(SnowRadianceConstants.computeSnowGrainSizePollutionOnlyLabel);
        computeSnowGrainSizeCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowGrainSizeLabel, true);
        computeSnowAlbedoCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowAlbedoLabel, true);
        computeSnowSootContentCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowSootContentLabel, true);
        computeEmissivityFubCheckBox = new JCheckBox(SnowRadianceConstants.computeEmissivityLabel, true);
        computeSnowTemperatureFubCheckBox = new JCheckBox(SnowRadianceConstants.computeSnowTemperatureLabel, true);

        computeMerisWaterVapourCheckBox = new JCheckBox(SnowRadianceConstants.waterVapourMerisLabel);
        computeMerisNdviCheckBox = new JCheckBox(SnowRadianceConstants.ndviMerisLabel);
        computeAatsrNdsiCheckBox = new JCheckBox(SnowRadianceConstants.ndsiMerisLabel);
        computeMerisMdsiCheckBox = new JCheckBox(SnowRadianceConstants.mdsiMerisLabel);
        copyAatsrL1FlagsCheckBox = new JCheckBox(SnowRadianceConstants.aatsrL1FlagsLabel);

        applyCloudMaskCheckBox = new JCheckBox(SnowRadianceConstants.applyCloudMaskLabel, true);
        cloudMaskGroup = new ButtonGroup();
        getCloudMaskFromMepixRadioButton = new JRadioButton(SnowRadianceConstants.applyCloudMaskMepixLabel);
        getCloudMaskFromMepixRadioButton.setSelected(true);
        getCloudMaskFromSynergyRadioButton = new JRadioButton(SnowRadianceConstants.applyCloudMaskSynergyLabel);
        getCloudMaskFromSynergyRadioButton.setSelected(false);
        cloudMaskGroup.add(getCloudMaskFromMepixRadioButton);
        cloudMaskGroup.add(getCloudMaskFromSynergyRadioButton);

        apply100PercentSnowMaskCheckBox = new JCheckBox(SnowRadianceConstants.applySnowMaskLabel);
        ActionListener updateUIActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
		};
        applyCloudMaskCheckBox.addActionListener(updateUIActionListener);
        apply100PercentSnowMaskCheckBox.addActionListener(updateUIActionListener);
        computeSnowGrainSizePollutionOnlyCheckBox.addActionListener(updateUIActionListener);
        getCloudMaskFromMepixRadioButton.addActionListener(updateUIActionListener);
        getCloudMaskFromSynergyRadioButton.addActionListener(updateUIActionListener);

        assumedEmissivityAt11MicronsTextField = new JFormattedTextField(SnowRadianceConstants.assumedEmissivity11MicronsDefaultValue);
        cloudProbabilityThresholdTextField = new JFormattedTextField(SnowRadianceConstants.cloudProbThresholdLabel);
        ndsiLowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiLowerDefaultValue);
        ndsiUpperThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiUpperDefaultValue);
        aatsr1610LowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.aatsr1610LowerDefaultValue);
        aatsr1610UpperThresholdTextField = new JFormattedTextField(SnowRadianceConstants.aatsr1610UpperDefaultValue);
        aatsr0670LowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.aatsr0670LowerDefaultValue);
        aatsr0670UpperThresholdTextField = new JFormattedTextField(SnowRadianceConstants.aatsr0670UpperDefaultValue);
        ndsiLowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiLowerDefaultValue);
        ndsiLowerThresholdTextField = new JFormattedTextField(SnowRadianceConstants.ndsiLowerDefaultValue);

        TableLayout layoutIO = new TableLayout(1);
        layoutIO.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layoutIO.setTableFill(TableLayout.Fill.HORIZONTAL);
        layoutIO.setTableWeightX(1);
        layoutIO.setCellWeightY(2, 0, 1);
        layoutIO.setTablePadding(2, 2);

        TableLayout layoutTargetBands = new TableLayout(1);
        layoutTargetBands.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layoutTargetBands.setTableFill(TableLayout.Fill.HORIZONTAL);
        layoutTargetBands.setTableWeightX(1);
        layoutTargetBands.setCellWeightY(0, 0, 1);
        layoutTargetBands.setTablePadding(2, 2);

        JPanel ioTab = new JPanel(layoutIO);
        JPanel targetBandsParamTab = new JPanel(layoutTargetBands);
        JPanel processingParamTab = new JPanel(new BorderLayout(10,10));
        addTab("I/O Parameters", ioTab);
        addTab("Target Bands", targetBandsParamTab);
        addTab("Processing Parameters", processingParamTab);

        JPanel merisInputPanel = merisSourceProductSelector.createDefaultPanel();
        ioTab.add(merisInputPanel);
        ioTab.add(computeSnowGrainSizePollutionOnlyCheckBox);
        aatsrInputPanel = aatsrSourceProductSelector.createDefaultPanel();
        ioTab.add(aatsrInputPanel);
		ioTab.add(targetProductSelector.createDefaultPanel());
		ioTab.add(new JLabel(""));

        JPanel targetBandsPanel = createTargetBandSelectionPanel();
        targetBandsParamTab.add(targetBandsPanel);

        JPanel processingParamsPanel = createProcessingParamsPanel();
        processingParamTab.add(processingParamsPanel, BorderLayout.NORTH);
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

//        panel.add(copyInputBandsCheckBox);
        panel.add(new JLabel(" "));
        panel.add(new JLabel(SnowRadianceConstants.snowPropertiesLabel));
        panel.add(new JLabel(" "));
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
        panel.add(computeAatsrNdsiCheckBox);
        panel.add(computeMerisMdsiCheckBox);
        panel.add(copyAatsrL1FlagsCheckBox);

		return panel;
	}

    private JPanel createCloudMaskParametersPanel() {

        TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(10, 5);     // space between columns/rows
        final JPanel panel = new JPanel(layout);

        int rowIndex = 0;

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

        return panel;
    }

    private JPanel createSnowMaskParametersPanel() {

        TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(10, 5);     // space between columns/rows
        final JPanel panel = new JPanel(layout);

        int rowIndex = 0;

        layout.setCellColspan(rowIndex, 0, 2);
        layout.setCellWeightX(rowIndex, 0, 1.0);
        panel.add(apply100PercentSnowMaskCheckBox, new TableLayout.Cell(rowIndex, 0));

        return panel;
    }

    private JPanel createProcessingParamsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel cloudMaskParametersPanel = createCloudMaskParametersPanel();
        JPanel snowMaskParametersPanel = createSnowMaskParametersPanel();

        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        panel.add(cloudMaskParametersPanel, c);

         // Cloud prob. threshold:
        c.insets = new Insets(0, 30, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.cloudProbThresholdLabel), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 2;
        panel.add(cloudProbabilityThresholdTextField, c);

        // Label: Thresholds for snow/ice flags
        c.insets = new Insets(10, 10, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.snowIceThresholdsLabel), c);

        // NDSI upper/lower:
        c.insets = new Insets(0, 50, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.ndsiLabel), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        panel.add(new JLabel(SnowRadianceConstants.lowerLabel), c);
        c.gridx = 2;
        panel.add(ndsiLowerThresholdTextField, c);
        c.insets = new Insets(0, 10, 0, 0);
        c.gridx = 3;
        panel.add(new JLabel(SnowRadianceConstants.upperLabel), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 4;
        panel.add(ndsiUpperThresholdTextField, c);

        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(snowMaskParametersPanel, c);

       // 100% snow boundaries:
        c.insets = new Insets(10, 30, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.snowBoundariesLabel), c);
        c.insets = new Insets(0, 50, 0, 0);
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.aatsr1610Label), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        panel.add(new JLabel(SnowRadianceConstants.lowerLabel), c);
        c.gridx = 2;
        panel.add(aatsr1610LowerThresholdTextField, c);
        c.insets = new Insets(0, 10, 0, 0);
        c.gridx = 3;
        panel.add(new JLabel(SnowRadianceConstants.upperLabel), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 4;
        panel.add(aatsr1610UpperThresholdTextField, c);

        c.insets = new Insets(0, 50, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.aatsr0670Label), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 1;
        panel.add(new JLabel(SnowRadianceConstants.lowerLabel), c);
        c.gridx = 2;
        panel.add(aatsr0670LowerThresholdTextField, c);
        c.insets = new Insets(0, 10, 0, 0);
        c.gridx = 3;
        panel.add(new JLabel(SnowRadianceConstants.upperLabel), c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 4;
        panel.add(aatsr0670UpperThresholdTextField, c);

        // Text field for assumed emissivity at 11 um
        c.insets = new Insets(10, 10, 0, 0);
        c.gridx = 0;
        c.gridy++;
        panel.add(new JLabel(SnowRadianceConstants.assumedEmissivity11MicronsLabel), c);
        c.insets = new Insets(10, 0, 0, 0);
        c.gridx = 2;
        panel.add(assumedEmissivityAt11MicronsTextField, c);


        return panel;
    }

    private int addEmptyLine(JPanel panel, int rowIndex) {
        panel.add(new JLabel(" "), new TableLayout.Cell(rowIndex, 0));
        rowIndex++;
        return rowIndex;
    }

    private void updateUIState() {
        cloudProbabilityThresholdTextField.setEnabled(applyCloudMaskCheckBox.isSelected() && getCloudMaskFromMepixRadioButton.isSelected());
        aatsr1610UpperThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected());
        aatsr1610LowerThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected());
        aatsr0670UpperThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected());
        aatsr0670LowerThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected());

        aatsrInputPanel.setVisible(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        computeEmissivityFubCheckBox.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        computeSnowTemperatureFubCheckBox.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        computeAatsrNdsiCheckBox.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        copyAatsrL1FlagsCheckBox.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());

        boolean maskCloudSelected = applyCloudMaskCheckBox.isSelected();
        getCloudMaskFromMepixRadioButton.setEnabled(maskCloudSelected && !computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        getCloudMaskFromSynergyRadioButton.setEnabled(maskCloudSelected && !computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        apply100PercentSnowMaskCheckBox.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        aatsr1610UpperThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected() && !computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        aatsr1610LowerThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected() && !computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        aatsr0670UpperThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected() && !computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        aatsr0670LowerThresholdTextField.setEnabled(apply100PercentSnowMaskCheckBox.isSelected() && !computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        ndsiUpperThresholdTextField.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        ndsiLowerThresholdTextField.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        assumedEmissivityAt11MicronsTextField.setEnabled(!computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        snowRadianceModel.setComputeSnowGrainSizePollutionOnly(computeSnowGrainSizePollutionOnlyCheckBox.isSelected());
        if (computeSnowGrainSizePollutionOnlyCheckBox.isSelected()) {
            computeEmissivityFubCheckBox.setSelected(false);
            computeSnowTemperatureFubCheckBox.setSelected(false);
            computeAatsrNdsiCheckBox.setSelected(false);
            copyAatsrL1FlagsCheckBox.setSelected(false);

            getCloudMaskFromMepixRadioButton.setSelected(true);
            getCloudMaskFromSynergyRadioButton.setSelected(false);
            cloudProbabilityThresholdTextField.setEnabled(applyCloudMaskCheckBox.isSelected());
            apply100PercentSnowMaskCheckBox.setSelected(false);

            snowRadianceModel.setComputeEmissivityFub(false);
            snowRadianceModel.setComputeSnowTemperatureFub(false);
            snowRadianceModel.setApply100PercentSnowMask(false);
            snowRadianceModel.setComputeAatsrNdsi(false);
            snowRadianceModel.setCopyAatsrL1Flags(false);
            snowRadianceModel.setGetCloudMaskFromSynergy(false);
        }
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
            String sourceProductType = sourceProduct.getProductType();
            String targetProductName = sourceProductType.substring(0,8) + "2SNOWRAD_";
            selectorModel.setProductName(targetProductName);
        }
    }
}
