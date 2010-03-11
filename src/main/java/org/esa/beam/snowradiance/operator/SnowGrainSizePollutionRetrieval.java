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
        double mus = Math.cos(sunZenith* MathUtils.DTOR);
        double muv = Math.cos(viewZenith* MathUtils.DTOR);
        double uMus = 3.0*(1.0 + 2.0*mus)/7.0;
        double uMuv = 3.0*(1.0 + 2.0*muv)/7.0;

        double d5 = Math.log(reflMeas2 / reflLut) * Math.log(reflMeas2 / reflLut);
        double fff = uMus * uMuv / reflLut;

        double gas = 2.25726172818651127 / 3.0; // asymmetry parameter

        double albs = getSnowAlbedo(reflMeas13, reflLut, sunZenith, viewZenith);
        double aksi = Math.log(albs)*Math.log(albs)/16.0;
        double omega = (1.0 - 3.0*aksi)/(1.0-3.0*aksi*gas);
        double b1 = 1.0 - omega;
        double B = 0.84;
        double akaps = 0.46;
        double gamsot = 4.0 * Math.PI * akaps / (MERIS_REFL13_WAVELENGTH*1.E-3);
        double yy = 4.0 * Math.PI * akaps / (MERIS_REFL2_WAVELENGTH*1.E-3);

        double abc = 4.0/Math.sqrt(3.0*(1.0-gas));
        double alf7 = abc*fff*Math.sqrt(2.0*B*yy/3.0);
        double x4 = d5/(alf7*alf7);

        double bets = 2.0*x4*B*gamsot/3.0;
        double xyz = b1 - bets;
        double omega0 = 1.0 - xyz;
        double l0 = (MERIS_REFL13_WAVELENGTH*1.E-3)/(4.0*Math.PI*refractiveIndexImaginaryPart);
        double pal = -l0*Math.log(1.0 - (1.0 -omega0)/0.47);

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
                                                      double pal, double grainSize) {

         // todo: variable names taken from Fortran breadboard - replace by meaningful ones!

         double mus = Math.cos(sunZenith * MathUtils.DTOR);
         double muv = Math.cos(viewZenith * MathUtils.DTOR);
         double uMus = 3.0 * (1.0 + 2.0 * mus) / 7.0;
         double uMuv = 3.0 * (1.0 + 2.0 * muv) / 7.0;

         double akaps = 0.46;
         double akap2 = 6.3E-11;
         double gamma = 4.0 * Math.PI * akap2/(MERIS_REFL2_WAVELENGTH*1.E-3);
         double gamsot = 4.0 * Math.PI * akaps/(MERIS_REFL2_WAVELENGTH*1.E-3);

         double d5 = Math.log(reflMeas2 / reflLut) * Math.log(reflMeas2 / reflLut);
         double fff = uMus * uMuv / reflLut;
         double gas = 2.25726172818651127 / 3.0; // asymmetry parameter
         double alpha = 16.0 * fff * fff / (3.0 * (1.0 - gas));
         double be = d5 / alpha;
         double B = 0.84;

         double cSoot = 3.0 * be / (2.0 * B * gamsot * grainSize);
         double conc = cSoot * 1.E9;

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
        double mus = Math.cos(sunZenith* MathUtils.DTOR);
        double muv = Math.cos(viewZenith* MathUtils.DTOR);
        double uMus = 3.0*(1.0 + 2.0*mus)/7.0;
        double uMuv = 3.0*(1.0 + 2.0*muv)/7.0;
        double f = uMus*uMuv/reflLut;
        double albedo = Math.pow(reflMeas/reflLut, 1.0/f);

        return albedo;
    }

    /**
     * This method computes the 'LUT reflectance' from the RF lookup table.
     * Currently, this is just a stupid line-by-line copy of the uncommented Fortran breadboard implementation.
     * TODO: ask AK for a description of the LUT, its contents, and more info on the Fortran code
     *
     * @param lut
     * @param sza
     * @param vza
     * @return
     */
    public double computeReflLut(SnowRadianceAuxData.ReflectionLookupTable lut, double sza, double vza) {

        double mus = Math.cos(sza*MathUtils.DTOR);
        double muv = Math.cos(vza*MathUtils.DTOR);

        double[][] splineResult;
        double[] ri = new double[lut.getnGs()];
        double[] rr = new double[lut.getIflag().length];
        for (int i=0; i<lut.getIflag().length; i++) {
            for (int j=0; j<lut.getnGs(); j++) {
                for (int k=0; k<j; k++) {
                    //               R(J,I)=R(I,J)
                    lut.setR0(i, k, j, lut.getR0()[i][j][k]);
                }
            }

            for (int j=0; j<lut.getnGs(); j++) {
                double[] y = new double[lut.getnGs()];
                for (int k=0; k<lut.getnGs(); k++) {
                    //               Y(J)=R(I,J)*X(J)*X(I)
                    y[k] = lut.getR0()[i][j][k] * lut.getX()[k] * lut.getX()[j];
                }
                //            CALL SPLINE (NG,X,Y,BB,CC,DD)
                splineResult = spline(lut.getnGs(), lut.getX(), y);
                //            RI(I)=SEVAL(NG,MU0,X,Y,BB,CC,DD)
                ri[j] = seval(lut.getnGs(), mus, lut.getX(), y, splineResult);
            }
            //         CALL SPLINE (NG,X,RI,BB,CC,DD)
            splineResult = spline(lut.getnGs(), lut.getX(), ri);
            //         DO K=1,NMU
            //            RR(K,M1)=SEVAL(NG,MU(K),X,RI,BB,CC,DD)/MU(K)
            //         ENDDO
            rr[i] = seval(lut.getnGs(), muv, lut.getX(), ri, splineResult)/muv;
        }
        double roi = 0.0;
        //            DO M1=1,MMAX2
        //               DL=DFLOAT(M1-1)*PF
        //               CO=DCOS(DL)
        //               A=2D0
        //               IF(M1.EQ.1) A=1D0
        //               ROI=ROI+A*RR(I,M1)*CO
        //            ENDDO
        for (int j=1; j<lut.getIflag().length; j++) {
            double co = 1.0;
            double a;
            if (j == 1) {
                a = 1.0;
            } else {
                a = 2.0;
            }
            roi += a * rr[j] * co;
        }
        //               CALL MATR (MU(I),MU0,PF,P11)
        //               ROI=ROI+0.25D0*ALB*P11*MU0/(MU(I)+MU0)
        double p11 = matr(muv, mus, lut.getIflag().length, lut.getAl1());
        roi += 0.25*lut.getAlb()*p11*mus/(mus + muv);

        return roi;
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
        double a = 1.247;
        double b = 1.186;
        double c = 5.157;
        double mus = Math.cos(sza*MathUtils.DTOR);
        double muv = Math.cos(vza*MathUtils.DTOR);
        double nus = Math.sin(sza*MathUtils.DTOR);
        double nuv = Math.sin(vza*MathUtils.DTOR);
        double azimDiff = (vaa - saa)*MathUtils.DTOR;

        double theta = sza*MathUtils.DTOR;
        double cosTheta = Math.cos(theta);
        double arcTheta = Math.acos(-mus*muv+nus*nuv*azimDiff) * MathUtils.RTOD;

        double px = 11.1*Math.exp(-0.087*arcTheta) + 1.1*Math.exp(-0.014*arcTheta);

        double reflLutApprox = (a + b*(mus + muv) + c*mus*muv + px)/(4.0*(mus+muv));

        return reflLutApprox;
    }

    // just a line-by-line implementation of AK Fortran method
    // meaning of input/output unclear  TODO: clarify
    private double[][] spline(int n, double[] x, double[] y) {
//        REAL*8 X(N),Y(N),B(N),C(N),D(N)                                   SPL00510
        double[][] result = new double[3][n]; // B, C, D in breadboard
//      NM1=N-1                                                           SPL00520
        int nm1 = n - 1;
//      IF (N.LT.2) RETURN                                                SPL00530
        
//      IF (N.LT.3) GO TO 50                                              SPL00540
        if (n >= 3) {
//           D(1)=X(2)-X(1)                                                    SPL00570
            result[2][0] = x[1] - x[0];
//          C(2)=(Y(2)-Y(1))/D(1)                                             SPL00580
            result[1][1] = (y[1] - y[0])/result[2][0];
//          DO 10 I=2,NM1                                                     SPL00590
//              D(I)=X(I+1)-X(I)                                              SPL00600
//              DI=D(I)                                                       SPL00610
//              B(I)=2D0*(D(I-1)+DI)                                          SPL00620
//              C(I+1)=(Y(I+1)-Y(I))/DI                                       SPL00630
//              C(I)=C(I+1)-C(I)                                              SPL00640
//          10 CONTINUE                                                          SPL00650
            for (int i=1; i<nm1; i++) {
                result[2][i] = x[i+1] - x[i];
                result[0][i] = 2.0f*(result[2][i-1] + result[2][i]);
                result[1][i+1] = (y[i+1] - y[i])/result[2][i];
                result[1][i] = result[1][i+1] - result[1][i];
            }
//          B(1)=-D(1)                                                        SPL00700
            result[0][0] = -result[2][0];
//          NN1=N-1                                                           SPL00710
            int nn1 = n - 1;
//          B(N)=-D(NN1)                                                      SPL00720
            result[0][n-1] = result[2][nn1-1];
//          C(1)=0D0                                                          SPL00730
            result[1][0] = 0.0f;
//          C(N)=0D0                                                          SPL00740
            result[1][n-1] = 0.0f;
//          IF (N.EQ.3) GO TO 15                                              SPL00750
            if (n > 3) {
//              C(1)= C(3)/(X(4)-X(2)) - C(2)/(X(3)-X(1))                         SPL00760
                result[1][0] = result[1][2]/(result[0][3] - result[0][1]) -
                               result[1][1]/(result[0][2] - result[0][0]);
//              NN2=N-2                                                           SPL00770
                int nn2 = n - 2;
//              NN3=N-3                                                           SPL00780
                int nn3 = n - 3;
//              C(N)= C(NN1)/(X(N)-X(NN2)) - C(NN2)/(X(NN1)-X(NN3))               SPL00790
                result[1][n-1] = result[1][n-2]/(result[0][n-1] - result[0][n-3]) -
                               result[1][n-3]/(result[0][n-2] - result[0][n-4]);
//              DD1=D(1)                                                          SPL00800
//              C(1)=C(1)*DD1*DD1/(X(4)-X(1))                                     SPL00810
                result[1][0] *= (result[2][0]*result[2][0]/(result[0][3] - result[0][0]));
//              DD1=D(NN1)                                                        SPL00820
//              C(N) = -C(N)*DD1*DD1/(X(N)-X(NN3))                                SPL00830
                result[1][n-1] *= (-result[2][0]*result[2][0]/(result[0][n-1] - result[0][n-4]));
            }
            //   15 DO 20 I=2,N                                                       SPL00870
//                  II=I-1                                                        SPL00880
//                  T=D(II)/B(II)                                                 SPL00890
//                  B(I)=B(I)-T*D(II)                                             SPL00900
//                  C(I)=C(I)-T*C(II)                                             SPL00910
//              20 CONTINUE                                                          SPL00920
            for (int i=1; i<n; i++) {
                double t = result[2][i-1]/result[0][i-1];
                result[0][i] -= t*result[2][i-1];
                result[1][i] -= t*result[1][i-1];
            }
//          C(N)=C(N)/B(N)                                                    SPL00960
            result[1][n-1] /= result[0][n-1];
//          DO 30 IB=1,NM1                                                    SPL00970
//              I=N-IB                                                        SPL00980
//              C(I)=(C(I)-D(I)*C(I+1))/B(I)                                  SPL00990
//          30 CONTINUE                                                          SPL01000
            for (int i=0; i<n-1-1; i++) {
                int j = n - 2 - i;
                result[1][j] = (result[1][j] - result[2][j]*result[1][j+1])/result[0][j];
            }
//          DDN=D(NM1)                                                        SPL01050
//          B(N)= (Y(N)-Y(NM1))/DDN + DDN*(C(NM1)+2D0*C(N))                   SPL01060
            result[0][n-1] = (y[n-1] - y[n-2])/result[2][n-2] +
                             result[2][n-2]*(result[1][n-2] + 2.0f*result[1][n-1]);
//          DO 40 I=1,NM1                                                     SPL01070
//              II=I+1                                                        SPL01080
//              CI=C(I)                                                       SPL01090
//              CCI=C(II)                                                     SPL01100
//              DI=D(I)                                                       SPL01110
//              DDI=1D0/DI                                                    SPL01120
//              B(I)= (Y(II)-Y(I))*DDI - DI*(CCI+2D0*CI)                      SPL01130
//              D(I)=(CCI-CI)*DDI                                             SPL01140
//              C(I)=3D0*CI                                                   SPL01150
//          40 CONTINUE                                                          SPL01160
            for (int i=0; i<n-1-1; i++) {
                result[0][i] = (y[i+1] - y[i])/result[2][i] -
                               result[2][i]*(result[1][i+1] + 2.0f*result[1][i]);
                result[2][i] = (result[1][i+1] - result[1][i])/result[2][i];
                result[1][i] *= 3.0;
            }
//          C(N)=3D0*C(N)                                                     SPL01170
            result[1][n-1] *= 3.0;
//          D(N)=D(NN1)                                                       SPL01180
            result[2][n-1] = result[2][n-2];
        } else {
//          50 B(1)=(Y(2)-Y(1))/(X(2)-X(1))                                      SPL01200
//          C(1)=0D0                                                          SPL01210
//          D(1)=0D0                                                          SPL01220
//          B(2)=B(1)                                                         SPL01230
//          C(2)=0D0                                                          SPL01240
//          D(2)=0D0                                                          SPL01250
            result[0][0] = (y[2] - y[1])/((x[2] - x[1]));
            result[1][0] = 0.0f;
            result[2][0] = 0.0f;
            result[0][1] = result[0][0];
            result[1][1] = 0.0f;
            result[2][1] = 0.0f;
        }

        return result;
    }

    // just a line-by-line implementation of AK Fortran method
    // meaning of this method and input/output totally unclear TODO: clarify
    private double seval(int n, double u, double[] x, double[] y, double[][] splineResult) {
        double seval = 0.0;
        double[] b = splineResult[0];
        double[] c = splineResult[1];
        double[] d = splineResult[2];

//        IMPLICIT REAL*8 (A-H,O-Z)                                         SEV00450
//      REAL*8 X(N),Y(N),B(N),C(N),D(N)                                   SEV00460
//      DATA I/1/                                                         SEV00470
//      IF (I.GE.N) I=1                                                   SEV00480
        // todo: clarify - what does this mean???
//      IF (U.LT.X(I)) GO TO 10                                           SEV00490
//      IF (U.LE.X(I+1)) GO TO 30                                         SEV00500
//   10 I=1                                                               SEV00540
        int i = 0;
//      J=N+1                                                             SEV00550
        int j = n;
//   20 K=(I+J)*0.5D0                                                     SEV00560
        do {
            int k = (i+j)/2;
    //      XK=X(K)                                                           SEV00570
    //      IF (U.LT.XK) J=K                                                  SEV00580
            if (u < x[k]) {
                j = k;
            }
    //      IF (U.GE.XK) I=K                                                  SEV00590
            if (u >= x[k]) {
                i = k;
            }
    //      IF (J.GT.I+1) GO TO 20                                            SEV00600
        } while(j > i+1);
//   30 DX=U-X(I)                                                         SEV00620
        double dx = u - x[i];
//      SEVAL=Y(I)+DX*(B(I)+DX*(C(I)+DX*D(I)))                            SEV00630
        seval = y[i] + dx*(b[i] + dx*(c[i] + dx*d[i]));
//      RETURN                                                            SEV00640

        return seval;
    }

    // just a line-by-line implementation of AK Fortran method
    // meaning of this method and input/output totally unclear TODO: clarify
    double matr(double mu, double mu0, int mmax1, double[] al1) {
        double p11 = 0.0f;
//      PARAMETER (LMAX1=22700)
        int lmax1 = 22700;

//      IF(DABS(PHI).LT.1D-7.OR.DABS(PHI-PI).LT.1D-7)
//     &        PHI=PHI+1D-7
        float phi = 1.E-7f;      // make sure phi is in radians!!
//      IF (DABS(MU-MU0).LT.1D-10) MU0=MU0*0.999999999d0       ?????
//      IF (DABS(MU+MU0).LT.1D-10) MU0=MU0*0.999999999d0
//      SI=DSQRT(1D0-MU*MU)
        double si = Math.sqrt(1.0 - mu*mu);
//      SI0=DSQRT(1D0-MU0*MU0)
        double si0 = Math.sqrt(1.0 - mu0*mu0);
//      U=-MU*MU0+SI*SI0*DCOS(PHI)
        double u = -mu*mu0 + si*si0*Math.cos(phi);
//      D6=DSQRT(6D0)*0.25D0
        double d6 = Math.sqrt(6*0.25); // ????????
//      F11=0D0
        double f11 = 0.0;
//      P1=0D0
        double p1 = 0.0;
//      PP1=1D0
        double pp1 = 1.0;
//      LMAX=MMAX1-1
        int lmax = mmax1 - 1;

//      DO L1=1,MMAX1
//         L=L1-1
//         DL=DFLOAT(L)
//         DL1=DFLOAT(L1)
//         F11=F11+AL1(L1)*PP1
//         IF(L.EQ.LMAX) GO TO 1
//         PL1=DFLOAT(2*L+1)
//         P=(PL1*U*PP1-DL*P1)/DL1
//         P1=PP1
//         PP1=P
//      ENDDO
        int i = 1;
        int l = i-1;
        while (i<mmax1-1 && l != lmax) {
            l = i-1;
            float dl = l*1.0f;
            float dl1 = i*1.0f;
            f11 += al1[i]*pp1;
            double pl1 = 2.0*l + 1.0;
            double p = (pl1*u*pp1 - dl*p1)/dl1;
            p1 = pp1;
            pp1 = p;
            i++;
        }
        p11 = f11;
//    1 P11=F11
//      RETURN
//      END

        return p11;
    }
}
