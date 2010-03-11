package org.esa.beam.snowradiance.operator;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.math.IntervalPartition;
import org.esa.beam.util.math.LookupTable;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.StringTokenizer;

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
     * This method reads all LUT files for ocean aerosol retrieval and creates
     * corresponding {@link org.esa.beam.util.math.LookupTable} objects.
     *
     * @param inputPath - file input path
     * @return LookupTable[][]
     * @throws java.io.IOException
     */
    public static LookupTable[][][] createRtmOceanLookupTables(String inputPath) throws IOException {

        DecimalFormat df1 = new DecimalFormat("0");
        DecimalFormat df5 = new DecimalFormat("00000");

        LookupTable[][][] rtmLookupTables = new LookupTable[SnowRadianceConstants.NUMBER_RTM]
                [SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES]
                [SnowRadianceConstants.NUMBER_AATSR_WVL];

        for (int i = 0; i < SnowRadianceConstants.NUMBER_ATMOSPHERIC_PROFILES; i++) {
            for (int j = 0; j < SnowRadianceConstants.NUMBER_AATSR_WVL; j++) {
                String sb2 = (df1.format(SnowRadianceConstants.ATMOSPHERIC_PROFILE_INDICES[i]));
                //  e.g.,  pr3_10.8.nc
                String inputFileString = "pr" + sb2 + "_" + SnowRadianceConstants.AATSR_WVL[j] + ".nc";

                try {
                    final NetcdfFile netcdfFile = NetcdfFile.open(inputPath + File.separator + inputFileString);

                    List variables = netcdfFile.getVariables();

                    // the variables in the netcdf file are defined like this (as obtained from an ncdump):
                    //       float WVA(WVA_dimension_1=21);
                    //       float EMI(EMI_dimension_1=11);
                    //       float TEM(TEM_dimension_1=33);
                    //       float VIE(VIE_dimension_1=9);
                    //       float MT(WVA_dimension_1=21, EMI_dimension_2=11, TEM_dimension_3=33, VIW_dimension_4=9);
                    //       float ST(WVA_dimension_1=21, EMI_dimension_2=11, TEM_dimension_3=33, VIW_dimension_4=9);

                    Variable wva = netcdfFile.findVariable("WVA");
                    Variable emi = netcdfFile.findVariable("EMI");
                    Variable tem = netcdfFile.findVariable("TEM");
                    Variable vie = netcdfFile.findVariable("VIE");
                    Variable mt = netcdfFile.findVariable("MT");
                    Variable st = netcdfFile.findVariable("ST");

                    float[] wvaArray = getJavaFloat1DFromNetcdfVariable(wva);
                    float[] emiArray = getJavaFloat1DFromNetcdfVariable(emi);
                    float[] temArray = getJavaFloat1DFromNetcdfVariable(tem);
                    float[] vieArray = getJavaFloat1DFromNetcdfVariable(vie);
                    for (int k = 0; k < vieArray.length; k++) {
                        // take negative value to get increasing sequence for LUT creation
                        vieArray[k] = -vieArray[k];
                    }

                    // set up LUT for first RTM
                    Array mtArrayNc = mt.read();
                    int mtDataSize = 1;
                    for (int k = 0; k < 4; k++) {
                        mtDataSize *= mt.getDimension(k).getLength();
                    }
                    Object mtStorage = mtArrayNc.getStorage();
                    float[] mtArray = new float[mtDataSize];
                    System.arraycopy(mtStorage, 0, mtArray, 0, mtDataSize);

                    final IntervalPartition[] mtDimensions = IntervalPartition.createArray(
                            vieArray, temArray, emiArray, wvaArray);

                    rtmLookupTables[0][i][j] = new LookupTable(mtArray, mtDimensions);

                    // set up LUT for second RTM
                    Array stArrayNc = st.read();
                    int stDataSize = 1;
                    for (int k = 0; k < 4; k++) {
                        stDataSize *= st.getDimension(k).getLength();
                    }
                    Object stStorage = stArrayNc.getStorage();
                    float[] stArray = new float[stDataSize];
                    System.arraycopy(stStorage, 0, stArray, 0, stDataSize);

                    final IntervalPartition[] stDimensions = IntervalPartition.createArray(
                            vieArray, temArray, emiArray, wvaArray);

                    rtmLookupTables[1][i][j] = new LookupTable(stArray, stDimensions);

                } catch (UnsupportedEncodingException e) {
                    throw new OperatorException("Failed to read RTM LUT from netcdf file.\n");
                }
            }
        }

        return rtmLookupTables;
    }

    public int getReflectionLookupTableSize(String inputPath) throws IOException {

        ReflectionLookupTable reflectionLookupTable = null;

        File inputFile = new File(inputPath + File.separator + SnowRadianceConstants.REFLECTION_LUT_FILENAME);
        final InputStream inputStream = new FileInputStream(inputFile);
        ReflectionLookupTable reflLut = new ReflectionLookupTable();

        int numberOfBlocksToUse = 0;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringTokenizer st;
        try {
            readReflectionLutFixPart(reflLut, bufferedReader);
            String line;
            int i;

            // now read MMax1 times the following block:
            reflLut.iflag = new int[reflLut.getmMax1()];
            for (int k=0; k<reflLut.getmMax1(); k++) {
                // read IFLAG:
                if ((line = bufferedReader.readLine()) != null) {
                    line = line.trim();
                    st = new StringTokenizer(line, " ", false);
                    reflLut.setIflag(k, Integer.parseInt(st.nextToken()));
                    if (reflLut.getIflag()[k] == 0) {
                        break;
                    }
                    numberOfBlocksToUse++;
                }

                // now read one value of R for I=1, two values for I=2, and so on...
                int j = 0;
                while (line != null && j<reflLut.getnGs()) {
                    int rLineIndex = 0;
                    int numberRLines = j/6 + 1;
                    i = 0;
                    while (rLineIndex < numberRLines && (line = bufferedReader.readLine()) != null) {
                       st = new StringTokenizer(line, " ", false);

                        while (st.hasMoreTokens()) {
                            final String s = st.nextToken();
                            double[] numbers = getDoubleNumbersFromJavaFormattedDecimalString(s);
                            for (double d:numbers) {
                                i++;
                            }
                        }
                       rLineIndex++;
                   }
                   j++;
                }
            }

        } catch (IOException e) {
            throw new OperatorException("Failed to read Reflection Lookup  Table: \n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to read Reflection Lookup  Table: \n" + e.getMessage(), e);
        } finally {
            inputStream.close();
        }

        return numberOfBlocksToUse;
    }

    private void readReflectionLutFixPart(ReflectionLookupTable reflLut, BufferedReader bufferedReader) throws IOException {
        StringTokenizer st;
        String line;
        // first line: read NSEP
        if ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            st = new StringTokenizer(line, " ", false);
            reflLut.setnSep(Integer.parseInt(st.nextToken()));
        }
        // second line: read NGS
        if ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            st = new StringTokenizer(line, " ", false);
            reflLut.setnGs(Integer.parseInt(st.nextToken()));
        }

        // now read NGS values of X:
        int i = 0;
        int xLineIndex = 0;
        int numberXLines = reflLut.getnGs()/6 + 1;
        reflLut.x = new double[reflLut.getnGs()];
        while (xLineIndex < numberXLines && (line = bufferedReader.readLine()) != null) {
            st = new StringTokenizer(line, " ", false);

            while (st.hasMoreTokens()) {
                double[] numbers = getDoubleNumbersFromJavaFormattedDecimalString(st.nextToken());
                for (double d:numbers) {
                    reflLut.setX(i, d);
                    i++;
                }
            }
            xLineIndex++;
        }

        // now read MMax1 and ALB:
        if ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            st = new StringTokenizer(line, " ", false);
            reflLut.setmMax1(Integer.parseInt(st.nextToken()));
            String s = getJavaFormattedDecimalString(st.nextToken());
            reflLut.setAlb(Double.parseDouble(s));
        }

        // now read MMax1 values of AL1:
        i = 0;
        int al1LineIndex = 0;
        int numberAl1Lines = reflLut.getmMax1()/6 + 1;
        reflLut.al1 = new double[reflLut.getmMax1()];
        while (al1LineIndex < numberAl1Lines && (line = bufferedReader.readLine()) != null) {
            st = new StringTokenizer(line, " ", false);

            while (st.hasMoreTokens()) {
                double[] numbers = getDoubleNumbersFromJavaFormattedDecimalString(st.nextToken());
                for (double d:numbers) {
                    reflLut.setAl1(i, d);
                    i++;
                }
            }
            al1LineIndex++;
        }
    }

    public ReflectionLookupTable createReflectionLookupTable(int numberOfVariableBlocksToUse, String inputPath) throws IOException {

        File inputFile = new File(inputPath + File.separator + SnowRadianceConstants.REFLECTION_LUT_FILENAME);
        final InputStream inputStream = new FileInputStream(inputFile);
        ReflectionLookupTable reflLut = new ReflectionLookupTable();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringTokenizer st;
        try {
            readReflectionLutFixPart(reflLut, bufferedReader);
            String line;
            int i;

            // now read numberOfVariableBlocksToUse times the following block:
            reflLut.iflag = new int[numberOfVariableBlocksToUse];
            reflLut.r0 = new double[numberOfVariableBlocksToUse][reflLut.getnGs()][reflLut.getnGs()];
            initR0Lut(reflLut, numberOfVariableBlocksToUse, reflLut.getnGs(), reflLut.getnGs());
            for (int k=0; k<numberOfVariableBlocksToUse; k++) {
                // read IFLAG:
                if ((line = bufferedReader.readLine()) != null) {
                    line = line.trim();
                    st = new StringTokenizer(line, " ", false);
                    reflLut.setIflag(k, Integer.parseInt(st.nextToken()));
                }

                // now read one value of R for I=1, two values for I=2, and so on...
                int j = 0;
                while (line != null && j<reflLut.getnGs()) {
                    int rLineIndex = 0;
                    int numberRLines = j/6 + 1;
                    i = 0;
                    while (rLineIndex < numberRLines && (line = bufferedReader.readLine()) != null) {
                       st = new StringTokenizer(line, " ", false);

                        while (st.hasMoreTokens()) {
                            double[] numbers = getDoubleNumbersFromJavaFormattedDecimalString(st.nextToken());
                            for (double d:numbers) {
                                reflLut.setR0(k, j, i, d);
                                i++;
                            }
                        }
                       rLineIndex++;
                   }
                   j++;
                }
            }

        } catch (IOException e) {
            throw new OperatorException("Failed to load Reflection Lookup Table: \n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load Reflection Lookup Table: \n" + e.getMessage(), e);
        } finally {
            inputStream.close();
        }

        return reflLut;
    }

    /**
     * This method converts the Fortran-like number formats '...D+...' and '...D-...'
     * to the Java-parseable format '...E...' and '...E-...'.
     *
     * @param fortranString - input, e.g. 0.13747170D+00
     * @return javaString
     */
    public String getJavaFormattedDecimalString(String fortranString) {
        String javaString = new String(fortranString);
        for (int i = 0; i < fortranString.length() - 1; i++) {
            final char c1 = fortranString.charAt(i);
            if (c1 == 'D') {
                javaString = fortranString.replace(c1, 'E');
            }
            final char c2 = fortranString.charAt(i + 1);
            if (c1 == '+') {
                javaString = javaString.substring(0, i) + javaString.substring(i + 1, javaString.length());
            }
        }
        return javaString;
    }

    public double[] getDoubleNumbersFromJavaFormattedDecimalString(String fortranString) {
        // we must also consider stuff like this:
        // 0.22425798D-03 0.32414135D-03 0.87896246D-04-0.19350035D-03-0.48647492D-03-0.10500075D-02

        int tokenLength = 15;
        int numberOfTokens;

        numberOfTokens = (fortranString.length() - 14)/tokenLength + 1;

//        if (fortranString.length() == 14) {
//            numberOfTokens = 1;
//        } else {
//            numberOfTokens = fortranString.length()/tokenLength;
//        }

        double[] result = new double[numberOfTokens];

        String thisToken = new String("");
        int posInString = 0;
        for (int i=0; i<numberOfTokens; i++) {
            if (numberOfTokens == 1) {
                thisToken = fortranString;
            } else {
                if (fortranString.charAt(posInString) == ' ' || fortranString.charAt(posInString) == '-') {
                    thisToken = fortranString.substring(posInString, posInString+tokenLength);
                    posInString += tokenLength;
                } else {
                    thisToken = fortranString.substring(posInString, posInString+tokenLength-1);
                    posInString += (tokenLength-1);
                }
            }
            String javaString = new String(thisToken);
            int thisTokenLength = thisToken.length();
            for (int j = 0; j < thisToken.length() - 1; j++) {
                final char c1 = thisToken.charAt(j);
                if (c1 == 'D') {
                    javaString = thisToken.replace(c1, 'E');
                }
                final char c2 = thisToken.charAt(j + 1);
                if (c2 == '+') {
                    javaString = javaString.substring(0, j) + javaString.substring(j, javaString.length());
                }
            }
            result[i] = Double.parseDouble(javaString);
        }


        return result;
    }



    private void initR0Lut(ReflectionLookupTable lut, int iDim, int jDim, int kDim) {
        for (int i = 0; i < iDim; i++) {
            for (int j = 0; j < jDim; j++) {
                for (int k = 0; k < kDim; k++) {
                    lut.setR0(i, j, k, SnowRadianceConstants.REFLECTION_LUT_NODATAVALUE);
                }
            }
        }

    }

    private static float[] getJavaFloat1DFromNetcdfVariable(Variable f) throws IOException {
        Array fArrayNc = f.read();
        int fSize = (int) fArrayNc.getSize();
        Object storage = fArrayNc.getStorage();
        float[] fArray = new float[fSize];
        System.arraycopy(storage, 0, fArray, 0, fSize);

        return fArray;
    }

    public class ReflectionLookupTable {
        // variable names just as in Fortran breadboard (whatever they mean...)
        // todo: clarify and introduce meaningful names!
        int nSep;
        int nGs;
        int mMax1;
        double[] x;
        double alb;
        double[] al1;
        int[] iflag;
        double[][][] r0;

        public int getnSep() {
            return nSep;
        }

        public void setnSep(int nSep) {
            this.nSep = nSep;
        }

        public int getnGs() {
            return nGs;
        }

        public void setnGs(int nGs) {
            this.nGs = nGs;
        }

        public int getmMax1() {
            return mMax1;
        }

        public void setmMax1(int mMax1) {
            this.mMax1 = mMax1;
        }

        public double[] getX() {
            return x;
        }

        public void setX(int index, double value) {
            x[index] = value;
        }

        public double getAlb() {
            return alb;
        }

        public void setAlb(double alb) {
            this.alb = alb;
        }

        public double[] getAl1() {
            return al1;
        }

        public void setAl1(int index, double value) {
            al1[index] = value;
        }

        public int[] getIflag() {
            return iflag;
        }

        public void setIflag(int index, int value) {
            iflag[index] = value;
        }

        public double[][][] getR0() {
            return r0;
        }

        public void setR0(int indexI, int indexJ, int indexK, double value) {
            r0[indexI][indexJ][indexK] = value;
        }
    }
}
