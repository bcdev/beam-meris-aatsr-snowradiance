package org.esa.beam.snowradiance.ui;

import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

import java.awt.Dimension;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceAction extends AbstractVisatAction {
    @Override
    public void actionPerformed(CommandEvent event) {
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
//        final DefaultSingleTargetProductDialog productDialog = new DefaultSingleTargetProductDialog(
//                "SnowRadiance.temperature", getAppContext(), "Snow Temperature Retrieval", "");
        final SnowRadianceDialog productDialog = new SnowRadianceDialog(getAppContext());

        productDialog.getJDialog().setPreferredSize(new Dimension(500, 500));
        productDialog.setTargetProductNameSuffix("_SNOWRAD");
        productDialog.show();
    }
}
