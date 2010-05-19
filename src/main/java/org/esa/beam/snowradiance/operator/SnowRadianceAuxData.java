package org.esa.beam.snowradiance.operator;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.snowradiance.util.SnowRadianceUtils;
import org.esa.beam.util.math.IntervalPartition;
import org.esa.beam.util.math.LookupTable;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.List;

import com.bc.jnn.JnnNet;
import com.bc.jnn.JnnException;
import com.bc.jnn.Jnn;

/**
 * Snow radiance aux data class
 *
 * @author Olaf Danne
 * @version $Revision: 8267 $ $Date: 2010-02-05 16:39:24 +0100 (Fr, 05 Feb 2010) $
 */
public class SnowRadianceAuxData {

    public static final String NEURAL_NET_WV_OCEAN_MERIS_FILE_NAME = "wv_ocean_meris.nna";

    private static SnowRadianceAuxData instance;

    public static SnowRadianceAuxData getInstance() {
        if (instance == null) {
            instance = new SnowRadianceAuxData();
        }

        return instance;
    }


    /**
     * This method reads a neural net file.
     *
     * @param filename - NN file
     * @return JnnNet - the NN object (see {@link com.bc.jnn.JnnNet})
     * @throws IOException
     * @throws com.bc.jnn.JnnException
     */
    public JnnNet loadNeuralNet(String filename) throws IOException, JnnException {
        InputStream inputStream = getClass().getResourceAsStream(filename);
        final InputStreamReader reader = new InputStreamReader(inputStream);

        JnnNet neuralNet = null;

        try {
            Jnn.setOptimizing(true);
            neuralNet = Jnn.readNna(reader);
        } finally {
            reader.close();
        }

        return neuralNet;
    }


    /**
     * This method reads all LUT files for snow temperature retrieval and creates
     * corresponding {@link org.esa.beam.util.math.LookupTable} objects.
     *
     * @return LookupTable[][]
     * @throws java.io.IOException
     */
    public static LookupTable[][] createRtmLookupTables() throws IOException {

        final DecimalFormat df1 = new DecimalFormat("0");

        LookupTable[][] rtmLookupTables = new LookupTable
                [SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES]
                [SnowRadianceConstants.NUMBER_AATSR_WVL];

        for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
            for (int j = 0; j < SnowRadianceConstants.NUMBER_AATSR_WVL; j++) {
                final String sb2 = (df1.format(SnowRadianceConstants.ATMOSPHERIC_PROFILE_INDICES[i]));
                //  e.g.,  pr3_10.8.nc
                final String inputFileString = "pr" + sb2 + "_" + SnowRadianceConstants.AATSR_WVL[j] + ".nc";

                try {
                    final URL url = SnowPropertiesOp.class.getResource(inputFileString);
                    final String path = URLDecoder.decode(url.getPath(), "UTF-8");
                    final File file = new File(path);
                    final String inputPath = file.getAbsolutePath();

//                    final String netcdfPath = "C:" + File.separator + "temp" + File.separator + inputFileString;
//                    final String netcdfPath = "C:" + File.separator + "temp" + File.separator + inputFileString;
                    final String netcdfPath = System.getProperty("java.io.tmpdir") + 
                            File.separator + inputFileString;
                    InputStream inputStream = SnowPropertiesOp.class.getResourceAsStream(inputFileString);
                    SnowRadianceUtils.copyStreamToFile(inputStream, netcdfPath);

//                    final NetcdfFile netcdfFile = NetcdfFile.open(inputPath + File.separator + inputFileString);
//                    final NetcdfFile netcdfFile = NetcdfFile.open(inputPath);
                    final NetcdfFile netcdfFile = NetcdfFile.open(netcdfPath);

                    List variables = netcdfFile.getVariables();

                    // the variables in the netcdf file are defined like this (as obtained from an ncdump):
                    //       float WVA(WVA_dimension_1=21);
                    //       float EMI(EMI_dimension_1=11);
                    //       float TEM(TEM_dimension_1=33);
                    //       float VIE(VIE_dimension_1=9);
                    //       float MT(WVA_dimension_1=21, EMI_dimension_2=11, TEM_dimension_3=33, VIW_dimension_4=9);
                    //       float ST(WVA_dimension_1=21, EMI_dimension_2=11, TEM_dimension_3=33, VIW_dimension_4=9);

                    final Variable wva = netcdfFile.findVariable("WVA");
                    final Variable emi = netcdfFile.findVariable("EMI");
                    final Variable tem = netcdfFile.findVariable("TEM");
                    final Variable tsfc = netcdfFile.findVariable("TMP");
                    final Variable vie = netcdfFile.findVariable("VIE");
                    final Variable mt = netcdfFile.findVariable("MT");
                    final Variable st = netcdfFile.findVariable("ST");

                    final float[] wvaArray = getJavaFloat1DFromNetcdfVariable(wva);
                    final float[] emiArray = getJavaFloat1DFromNetcdfVariable(emi);
                    final float[] temArray = getJavaFloat1DFromNetcdfVariable(tem);
                    final float[] vieArray = getJavaFloat1DFromNetcdfVariable(vie);
                    for (int k = 0; k < vieArray.length; k++) {
                        // take negative value to get increasing sequence for LUT creation
                        vieArray[k] = -vieArray[k];
                    }

                    final Array mtArrayNc = mt.read();
                    int mtDataSize = 1;
                    for (int k = 0; k < 4; k++) {
                        mtDataSize *= mt.getDimension(k).getLength();
                    }
                    Object mtStorage = mtArrayNc.getStorage();
                    float[] mtArray = new float[mtDataSize];
                    System.arraycopy(mtStorage, 0, mtArray, 0, mtDataSize);

                    final IntervalPartition[] mtDimensions = IntervalPartition.createArray(
                            vieArray, temArray, emiArray, wvaArray);

                    rtmLookupTables[i][j] = new LookupTable(mtArray, mtDimensions);

                } catch (UnsupportedEncodingException e) {
                    throw new OperatorException("Failed to read RTM LUT from netcdf file.\n");
                }
            }
        }

        return rtmLookupTables;
    }

    public static double[][][] getTsfcFromLookupTables() throws IOException {

        final DecimalFormat df1 = new DecimalFormat("0");

        double[][][] tSfcLut = new double
                [SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES]
                [SnowRadianceConstants.NUMBER_AATSR_WVL]
                [SnowRadianceConstants.NUMBER_TSFC_LUT];

        for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
            for (int j = 0; j < SnowRadianceConstants.NUMBER_AATSR_WVL; j++) {
                final String sb2 = (df1.format(SnowRadianceConstants.ATMOSPHERIC_PROFILE_INDICES[i]));
                //  e.g.,  pr3_10.8.nc
                final String inputFileString = "pr" + sb2 + "_" + SnowRadianceConstants.AATSR_WVL[j] + ".nc";

                try {
                    final URL url = SnowPropertiesOp.class.getResource(inputFileString);
                    final String path = URLDecoder.decode(url.getPath(), "UTF-8");
                    final File file = new File(path);
                    final String inputPath = file.getAbsolutePath();

//                    final NetcdfFile netcdfFile = NetcdfFile.open(inputPath + File.separator + inputFileString);
//                    final String netcdfPath = "C:" + File.separator + "temp" + File.separator + inputFileString;
                    final String netcdfPath = System.getProperty("java.io.tmpdir") +
                            File.separator + inputFileString;
                    InputStream inputStream = SnowPropertiesOp.class.getResourceAsStream(inputFileString);
                    SnowRadianceUtils.copyStreamToFile(inputStream, netcdfPath);
//                    final NetcdfFile netcdfFile = NetcdfFile.open(inputPath);
                    final NetcdfFile netcdfFile = NetcdfFile.open(netcdfPath);

                    final Variable tsfc = netcdfFile.findVariable("TMP");

                    float[] tsfcArray = getJavaFloat1DFromNetcdfVariable(tsfc);
                    for (int k = 0; k < tsfcArray.length; k++) {
                        tSfcLut[i][j][k] = tsfcArray[k];
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new OperatorException("Failed to read RTM LUT from netcdf file.\n");
                }
            }
        }

        return tSfcLut;
    }

    private static float[] getJavaFloat1DFromNetcdfVariable(Variable f) throws IOException {
        Array fArrayNc = f.read();
        int fSize = (int) fArrayNc.getSize();
        Object storage = fArrayNc.getStorage();
        float[] fArray = new float[fSize];
        System.arraycopy(storage, 0, fArray, 0, fSize);

        return fArray;
    }
}
