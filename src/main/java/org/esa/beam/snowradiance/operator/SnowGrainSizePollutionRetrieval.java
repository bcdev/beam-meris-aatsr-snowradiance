package org.esa.beam.snowradiance.operator;

import org.esa.beam.util.math.MathUtils;

/**
 *  Snow grain size retrieval algorithm
 *
 * @author Olaf Danne
 * @version $Revision: 8312 $ $Date: 2010-02-09 17:54:10 +0100 (Di, 09 Feb 2010) $
 */
public class SnowGrainSizePollutionRetrieval {

    public static double refractiveIndexImaginaryPart  = 2.4E-7;
    public static double MERIS_REFL2_WAVELENGTH = 0.4425; // microns
    public static double MERIS_REFL13_WAVELENGTH = 0.865; // microns

    /**
     * This method computes the unpolluted snow grain particle absorption length (AK snow radiance manual, section 2)
     *
     * @param reflMeas2 - meas. reflectance  442nm
     * @param reflMeas13 - meas. reflectance  865nm
     * @param reflLut  - reflectance from LUT or asymtotic theory
     * @param sunZenith - sun zenith
     * @param viewZenith - view zenith
     * @return  pal
     */
    public double getParticleAbsorptionLength(double reflMeas2, double reflMeas13, double reflLut, double sunZenith, double viewZenith) {
        final double mus = Math.cos(sunZenith* MathUtils.DTOR);
        final double muv = Math.cos(viewZenith* MathUtils.DTOR);
        final double uMus = 3.0*(1.0 + 2.0*mus)/7.0;
        final double uMuv = 3.0*(1.0 + 2.0*muv)/7.0;

        final double d5 = Math.log(reflMeas2 / reflLut) * Math.log(reflMeas2 / reflLut);
        final double fff = uMus * uMuv / reflLut;

        final double gas = 2.25726172818651127 / 3.0; // asymmetry parameter

        final double albs = getSnowAlbedo(reflMeas13, reflLut, sunZenith, viewZenith);
        final double aksi = Math.log(albs)*Math.log(albs)/16.0;
        final double omega = (1.0 - 3.0*aksi)/(1.0-3.0*aksi*gas);
        final double b1 = 1.0 - omega;
        final double B = 0.84;
        final double akaps = 0.46;
        final double gamsot = 4.0 * Math.PI * akaps / (MERIS_REFL13_WAVELENGTH*1.E-3);
        final double yy = 4.0 * Math.PI * akaps / (MERIS_REFL2_WAVELENGTH*1.E-3);

        final double abc = 4.0/Math.sqrt(3.0*(1.0-gas));
        final double alf7 = abc*fff*Math.sqrt(2.0*B*yy/3.0);
        final double x4 = d5/(alf7*alf7);

        final double bets = 2.0*x4*B*gamsot/3.0;
        final double xyz = b1 - bets;
        final double omega0 = 1.0 - xyz;
        final double l0 = (MERIS_REFL13_WAVELENGTH*1.E-3)/(4.0*Math.PI*refractiveIndexImaginaryPart);
        final double pal = -l0*Math.log(1.0 - (1.0 -omega0)/0.47);

        return pal;
    }

    /**
     * This method computes the unpolluted snow grain size (AK snow radiance manual, final step of section 2)
     *
     * @param pal - particle absorption length  442nm
     * @return  size
     */
    public double getUnpollutedSnowGrainSize(double pal) {
         return 0.38*pal;
    }

     /**
     * This method computes the soot concentration in polluted snow (AK snow radiance manual, section 3)
     *
     * @param reflMeas2 - meas. reflectance
     * @param reflLut  - reflectance from LUT or asymtotic theory
     * @param sunZenith - sun zenith
     * @param viewZenith - view zenith
     * @param grainSize - snow grain size
     * @return  conc
     */
     public double getSootConcentrationInPollutedSnow(double reflMeas2, double reflLut,
                                                      double sunZenith, double viewZenith,
                                                      double grainSize) {

         // todo: variable names taken from Fortran breadboard - replace by meaningful ones!

         final double mus = Math.cos(sunZenith * MathUtils.DTOR);
         final double muv = Math.cos(viewZenith * MathUtils.DTOR);
         final double uMus = 3.0 * (1.0 + 2.0 * mus) / 7.0;
         final double uMuv = 3.0 * (1.0 + 2.0 * muv) / 7.0;

         final double akaps = 0.46;
         final double gamsot = 4.0 * Math.PI * akaps/(MERIS_REFL2_WAVELENGTH*1.E-3);

         final double d5 = Math.log(reflMeas2 / reflLut) * Math.log(reflMeas2 / reflLut);
         final double fff = uMus * uMuv / reflLut;
         final double gas = 2.25726172818651127 / 3.0; // asymmetry parameter
         final double alpha = 16.0 * fff * fff / (3.0 * (1.0 - gas));
         final double be = d5 / alpha;
         final double B = 0.84;

         final double cSoot = 3.0 * be / (2.0 * B * gamsot * grainSize);
         final double conc = cSoot * 1.E9;

         return conc;
    }

    /**
     * This method computes the snow albedo (AK snow radiance manual, section 4)
     *
     * @param reflMeas - meas. reflectance
     * @param reflLut  - reflectance from LUT or asymtotic theory
     * @param sunZenith - sun zenith
     * @param viewZenith - view zenith
     * @return  size
     */
    public double getSnowAlbedo(double reflMeas, double reflLut, double sunZenith, double viewZenith) {
        final double mus = Math.cos(sunZenith* MathUtils.DTOR);
        final double muv = Math.cos(viewZenith* MathUtils.DTOR);
        final double uMus = 3.0*(1.0 + 2.0*mus)/7.0;
        final double uMuv = 3.0*(1.0 + 2.0*muv)/7.0;
        final double f = uMus*uMuv/reflLut;
        final double albedo = Math.pow(reflMeas/reflLut, 1.0/f);

        return albedo;
    }

    /**
     * This method computes the 'LUT reflectance' from asymptotic approximation.
     * This is also line-by-line copy of the Fortran breadboard implementation, but simple here.
     * TODO: ask AK for a description of the theory behind.
     *
     * @param saa
     * @param sza
     * @param vza
     * @param vza
     * @return
     */
    public double computeReflLutApprox(double saa, double sza, double vaa, double vza) {
        final double a = 1.247;
        final double b = 1.186;
        final double c = 5.157;
        final double mus = Math.cos(sza*MathUtils.DTOR);
        final double muv = Math.cos(vza*MathUtils.DTOR);
        final double nus = Math.sin(sza*MathUtils.DTOR);
        final double nuv = Math.sin(vza*MathUtils.DTOR);
        final double azimDiff = (vaa - saa)*MathUtils.DTOR;

        final double theta = sza*MathUtils.DTOR;
        final double arcTheta = Math.acos(-mus*muv+nus*nuv*azimDiff) * MathUtils.RTOD;

        final double px = 11.1*Math.exp(-0.087*arcTheta) + 1.1*Math.exp(-0.014*arcTheta);

        final double reflLutApprox = (a + b*(mus + muv) + c*mus*muv + px)/(4.0*(mus+muv));

        return reflLutApprox;
    }

}
