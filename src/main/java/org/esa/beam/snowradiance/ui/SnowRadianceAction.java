package org.esa.beam.snowradiance.ui;

import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.snowradiance.operator.SnowRadianceMasterOp;
import org.esa.beam.visat.actions.AbstractVisatAction;

import javax.media.jai.JAI;
import java.awt.Dimension;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SnowRadianceAction extends AbstractVisatAction {
    @Override
    public void actionPerformed(CommandEvent event) {
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1);
        final SnowRadianceDialog productDialog = new SnowRadianceDialog(
                OperatorSpi.getOperatorAlias(SnowRadianceMasterOp.class),
                        getAppContext(), "Snow Properties", "snowRadiance");

        productDialog.getJDialog().setPreferredSize(new Dimension(500, 500));
        productDialog.setTargetProductNameSuffix("_SNOWRAD");
        productDialog.show();
    }
}
