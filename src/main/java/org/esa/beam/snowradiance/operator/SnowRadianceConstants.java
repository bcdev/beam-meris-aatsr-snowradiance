package org.esa.beam.snowradiance.operator;

/**
 * Snow radiance constants
 *
 * @author Olaf Danne
 * @version $Revision: 8312 $ $Date: 2010-02-09 17:54:10 +0100 (Di, 09 Feb 2010) $
 */
public class SnowRadianceConstants {

    public static final String SNOW_TEMPERATURE_BAND_NAME= "snow_temperature";
    public static final double SNOW_TEMPERATURE_BAND_NODATAVALUE = -1.0;
    public static final boolean SNOW_TEMPERATURE_BAND_NODATAVALUE_USED = true;

    public static final String EMISSIVITY_BAND_NAME= "snow_emissivity";
    public static final double EMISSIVITY_BAND_NODATAVALUE = -1.0;
    public static final boolean EMISSIVITY_BAND_NODATAVALUE_USED = true;

    public static final int NUMBER_ATMOSPHERIC_PROFILES = 4;
    public static final int NUMBER_AATSR_WVL = 2;
    public static final int NUMBER_TSFC_LUT = 25;

    public static final long[] ATMOSPHERIC_PROFILE_INDICES = {3, 5, 6, 7};

    public static final String[] AATSR_WVL = {"10.8", "12.0"};

    public static final float WATER_VAPOUR_STANDARD_VALUE = 2.8f;

    public static final float EMISSIVITY_MIN = 0.95f;
    public static final float EMISSIVITY_MAX = 0.99f;

    public static final float TSFC_MIN = 243.15f;
    public static final float TSFC_MAX = 274.15f;

    public static final double SNOW_GRAIN_SIZE_POLLUTION_NODATAVALUE = -1.0;
    public static final double SNOW_TEMPERATURE_EMISSIVITY_NODATAVALUE = -1.0;

    public static final String UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NAME= "snow_grain_size";
    public static final double UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NODATAVALUE = -1.0;
    public static final boolean UNPOLLUTED_SNOW_GRAIN_SIZE_BAND_NODATAVALUE_USED = true;

    public static final String SOOT_CONCENTRATION_BAND_NAME = "soot_concentration";
    public static final double SOOT_CONCENTRATION_BAND_NODATAVALUE = -1.0;
    public static final boolean SOOT_CONCENTRATION_BAND_NODATAVALUE_USED = true;

    public static final String SNOW_ALBEDO_BAND_NAME= "snow_albedo";
    public static final double SNOW_ALBEDO_BAND_NODATAVALUE = -1.0;
    public static final boolean SNOW_ALBEDO_BAND_NODATAVALUE_USED = true;

    public static final String copyInputBandsLabel = "Copy input bands";
    public static final String computeSnowGrainSizeLabel = "Compute snow grain size";
    public static final String computeSnowGrainSizePollutionOnlyLabel = "Compute snow grain size / pollution only (no AATSR L1b required)";
    public static final String computeSnowAlbedoLabel = "Compute snow albedo";
    public static final String computeSnowSootContentLabel = "Compute snow soot content";
    public static final String computeEmissivityLabel = "Compute emissivity";
    public static final String computeSnowTemperatureLabel = "Compute temperature";

    public static final String snowPropertiesLabel = "Snow properties:";
    public static final String complementaryQuantitiesLabel = "Complementary quantities:";
    public static final String waterVapourMerisLabel = "Water vapour (from MERIS)";
    public static final String ndviMerisLabel = "NDVI (from MERIS)";
    public static final String ndsiMerisLabel = "NDSI (from AATSR)";
    public static final String mdsiMerisLabel = "MDSI (from MERIS)";
    public static final String aatsrL1FlagsLabel = "AATSR L1 flags";

    public static final String applyCloudMaskLabel = "Identify cloud pixels";
    public static final String applyCloudMaskMepixLabel = "Cloud probability (MERIS O2 Project)";
    public static final String applyCloudMaskSynergyLabel = "Feature classification (MERIS/AATSR Synergy Project)";
    public static final String applySnowMaskLabel = "Identify 100% snow pixels";

    public static final String assumedEmissivity11MicronsLabel = "Assumed emissivity at 11 microns [dl]:";
    public static final String snowIceThresholdsLabel = "Thresholds for snow/ice flags:";

    public static final String ndsiLabel = "NDSI [dl]";
    public static final String cloudProbThresholdLabel = "Cloud probability threshold [dl]:";
    public static final String snowBoundariesLabel = "100% snow thresholds:";

    public static final String aatsr1610Label = "AATSR 1610nm [%]";
    public static final String aatsr0670Label = "AATSR  670nm [%]";
    public static final String lowerLabel = "lower:";
    public static final String upperLabel = "upper:";
    public static final String ndsiLowerDefaultValue = "0.75";

    public static final String assumedEmissivity11MicronsDefaultValue = "0.99";
    public static final String ndsiUpperDefaultValue = "0.9";
    public static final String cloudProbThresholdDefaultValue = "0.4";       // tbd
    public static final String aatsr1610LowerDefaultValue = "1.0";
    public static final String aatsr1610UpperDefaultValue = "10.0";
    public static final String aatsr0670LowerDefaultValue = "60.0";
    public static final String aatsr0670UpperDefaultValue = "100.0";

    public static final String SNOWRADIANCE_FLAG_BAND_NAME = "snowradiance_flags";

    public static final int F_NO_AATSR= 0;
    public static final int F_CLOUD = 1;
    public static final int F_ICE = 2;
    public static final int F_SNOW = 3;
    public static final int F_UNSPECIFIED= 4;

    public static final String AATSR_CONFID_NADIR_FLAG_BAND_NAME = "confid_flags_nadir_AATSR";
    public static final String AATSR_CONFID_FWARD_FLAG_BAND_NAME = "confid_flags_fward_AATSR";
    public static final String AATSR_CLOUD_NADIR_FLAG_BAND_NAME = "cloud_flags_nadir_AATSR";
    public static final String AATSR_CLOUD_FWARD_FLAG_BAND_NAME = "cloud_flags_fward_AATSR";

    public static final String SYNERGY_CLOUD_FLAG_BAND_NAME = "cloud_flags_synergy";

}
