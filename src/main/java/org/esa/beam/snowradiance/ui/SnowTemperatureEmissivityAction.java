package org.esa.beam.snowradiance.ui;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.media.jai.JAI;
import java.awt.Dimension;

/**
 * Action for snow temperature and emissivity retrieval
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowTemperatureEmissivityAction extends AbstractVisatAction {
    @Override
    public void actionPerformed(CommandEvent event) {
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
        final DefaultSingleTargetProductDialog productDialog = new DefaultSingleTargetProductDialog(
                "SnowRadiance.temperature", getAppContext(), "Snow Temperature Retrieval", "");

        productDialog.getJDialog().setPreferredSize(new Dimension(500, 500));
        productDialog.setTargetProductNameSuffix("_SNOWRAD");
        productDialog.show();
    }

}
