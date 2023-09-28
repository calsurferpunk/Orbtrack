package com.nikolaiapps.orbtrack;


import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import org.json.JSONObject;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


public abstract class Calculations
{
    //TLE line 1 indexes
    public static abstract class TLE1Index
    {
        static final byte SatNum = 2;
        static final byte Class = 7;
        static final byte LaunchYear = 9;
        static final byte LaunchNum = 11;
        static final byte LaunchPiece = 14;
        static final byte EpochYear = 18;
        static final byte EpochDay = 20;
        static final byte MeanMotion1 = 33;
        static final byte MeanMotion2 = 44;
        static final byte MeanMotion2Div = 50;
        static final byte Drag = 53;
        static final byte DragDiv = 59;
        static final byte Ephem = 62;
        static final byte Enum = 64;
    }

    //TLE line 2 indexes
    @SuppressWarnings("SpellCheckingInspection")
    public static abstract class TLE2Index
    {
        static final byte Inclin = 8;
        static final byte RightAscn = 17;
        static final byte Eccen = 26;
        static final byte ArgPeri = 34;
        static final byte MeanAnom = 43;
        static final byte Revs = 52;
        static final byte RevEp = 63;
    }

    //GP Params
    @SuppressWarnings("SpellCheckingInspection")
    public static abstract class GPParams
    {
        static final String Name = "OBJECT_NAME";
        static final String SatNum = "NORAD_CAT_ID";
        static final String Class = "CLASSIFICATION_TYPE";
        static final String Owner = "COUNTRY_CODE";
        static final String LaunchDate = "LAUNCH_DATE";
        static final String ObjectId = "OBJECT_ID";
        static final String Epoch = "EPOCH";
        static final String MeanMotion1 = "MEAN_MOTION_DOT";
        static final String MeanMotion2 = "MEAN_MOTION_DDOT";
        static final String Drag = "BSTAR";
        static final String Ephem = "EPHEMERIS_TYPE";
        static final String Enum = "ELEMENT_SET_NO";
        static final String Inclin = "INCLINATION";
        static final String RightAscn = "RA_OF_ASC_NODE";
        static final String Eccen = "ECCENTRICITY";
        static final String ArgPeri = "ARG_OF_PERICENTER";
        static final String MeanAnom = "MEAN_ANOMALY";
        static final String Revs = "MEAN_MOTION";
        static final String RevsEp = "REV_AT_EPOCH";
    }

    //Misc
    static final double KmPerLightYear          = 9.460730472581e+12;      //km per light year
    static final double SecondsPerHour          = 3600.0;
    static final double SecondsPerDay           = 86400.0;
    static final double MsPerDay                = SecondsPerDay * 1000;
    static final double MsPerWeek               = MsPerDay * 7;
    static final double HoursPerDay             = 24.0;
    static final double MinutesPerDay           = 1440.0;
    static final double AU                      = 149597870.691;          //convert between km and astronomical units
    static final double EarthRadiusKM           = 6378.135;				  //earth equatorial radius in km (WGS '72)
    private static final double DaysPerYear     = 365.25;
    private static final double EarthRotPerSD   = 1.00273790934;		  //earth rotations per sidereal day
    private static final double HalfPI          = (Math.PI / 2.0);
    private static final double TwoPI           = (2 * Math.PI);
    private static final double JanFirst1900JD  = 2415020.0;			  //Julian date for January 1, 1900 at 12:00 noon (UTC)
    private static final double JanFirst2000JD  = 2451545.0;			  //Julian date for January 1, 2000 at 12:00 noon (UTC)
    private static final double JulianCentury   = 36525.0;                //Julian days in a century
    private static final double J2				= 1.0826158E-3;			  //J2 harmonic (WGS '72)
    private static final double J3				= -2.53881E-6	;		  //J3 harmonic (WGS '72)
    private static final double XJ3				= J3;
    private static final double J4				= -1.65597E-6;			  //J4 harmonic (WGS '72)
    private static final double EarthFlattening = (1.0 / 298.26);		  //earth flattening (WGS '72)
    private static final double EarthGravity    = 398600.8;				  //earth gravitational constant (WGS '72)
    private static final double AE				= 1.0;
    private static final double E6A				= 1.0e-06;
    private static final double TwoThirds       = (2.0 / 3.0);
    private static final double CK2				= (J2 / 2.0);
    private static final double CK4				= (-3.0 * J4 / 8.0);
    private static final double QO				= (AE + 120.0 / EarthRadiusKM);
    private static final double S				= (AE + 78.0  / EarthRadiusKM);
    private static final double ZCosGS          = 0.1945905;
    private static final double ZSinGS          = -0.98088458;
    private static final double ZCosIS          = 0.91744867;
    private static final double ZSinIS          = 0.39785416;
    private static final double C1SS			= 2.9864797E-6;
    private static final double ZNS				= 1.19459E-5;
    private static final double ZES				= 0.01675;
    private static final double ZNL				= 1.5835218E-4;
    private static final double C1L				= 4.7968065E-7;
    private static final double ZEL				= 0.05490;
    private static final double Root22          = 1.7891679E-6;
    private static final double Root32          = 3.7393792E-7;
    private static final double Root44          = 7.3636953E-9;
    private static final double Root52          = 1.1428639E-7;
    private static final double Root54          = 2.1765803E-9;
    private static final double THDT			= 4.3752691E-3;
    private static final double Q22				= 1.7891679E-6;
    private static final double Q31				= 2.1460748E-6;
    private static final double Q33				= 2.2123015E-7;
    private static final double G22				= 5.7686396;
    private static final double G32				= 0.95240898;
    private static final double G44				= 1.8014998;
    private static final double G52				= 1.0508330;
    private static final double G54				= 4.4108898;
    private static final double DegToLatKm      = 110.574;                //convert between degrees and latitude in km
    private static final double DegToLonKm      = 111.320;                //convert between degrees and longitude in km (111.320 * cos(latitude))
    private static final SimpleDateFormat epochParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US);

    //Functions/conversions
    private static double MinutesToDays(double minutes)		            { return((minutes) / MinutesPerDay); }								//converts minutes to days
    private static double DaysPassed(double julStart, double julEnd)    { return ((julEnd) - (julStart)) * MinutesPerDay; }			            //number of days passed from Julian start and end dates
    private static double Square(double num)					        { return(Math.pow(num, 2)); }										//number squared
    private static final double XKE							            = Math.sqrt(3600.0 * EarthGravity / Math.pow(EarthRadiusKM, 3));
    private static final double QOMS2T						            = Math.pow((QO - S), 4);											//(QO - S)^4 ER^4
    private static boolean IsLeapYear(int year)			                { return(((year) % 4 == 0 && (year) % 100 != 0) || ((year) % 400 == 0)); }
    static double ReduceRadians(double rad)                             { rad = rad - TwoPI * Math.floor(rad / TwoPI); if(rad < 0.0){ rad += TwoPI; } return(rad); }
    static double ReduceAngle(double angle)                             { angle = angle - 360 * Math.floor(angle / 360); if(angle < 0.0){ angle += 360; } return(angle); }
    static double ReduceToRadians(double angle)                         { return(Math.toRadians(ReduceAngle(angle))); }

    //
    //Types
    //

    //Prediction model
    public static abstract class SgpModelType
    {
        static final byte Sgp4Model = 0;
        static final byte Sdp4Model = 1;
    }

    //ECI data
    public static class EciDataType implements Parcelable
    {
        public double positionX;
        public double positionY;
        public double positionZ;
        public double julianDate;						//Julian date of last position update
        public static final Creator<EciDataType> CREATOR = new Parcelable.Creator<EciDataType>()
        {
            @Override
            public EciDataType createFromParcel(Parcel source)
            {
                return(new EciDataType(source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble()));
            }

            @Override
            public EciDataType[] newArray(int size)
            {
                return(new EciDataType[size]);
            }
        };

        public EciDataType()
        {
            positionX = positionY = positionZ = julianDate = 0;
        }
        public EciDataType(double x, double y, double z, double jd)
        {
            positionX = x;
            positionY = y;
            positionZ = z;
            julianDate = jd;
        }
        public EciDataType(EciDataType copyFrom)
        {
            positionX = copyFrom.positionX;
            positionY = copyFrom.positionY;
            positionZ = copyFrom.positionZ;
            julianDate = copyFrom.julianDate;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeDouble(positionX);
            dest.writeDouble(positionY);
            dest.writeDouble(positionZ);
            dest.writeDouble(julianDate);
        }
    }

    //Geodetic data
    public static class GeodeticDataType implements Parcelable
    {
        public double latitude;						    //+ north, - south
        public double longitude;						//+ east, - west
        public double altitudeKm;
        public double speedKmS;                         //speed in km/s
        public double radius;
        public static final Creator<GeodeticDataType> CREATOR = new Parcelable.Creator<GeodeticDataType>()
        {
            @Override
            public GeodeticDataType createFromParcel(Parcel source)
            {
                return(new GeodeticDataType(source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble()));
            }

            @Override
            public GeodeticDataType[] newArray(int size)
            {
                return(new GeodeticDataType[size]);
            }
        };

        public GeodeticDataType()
        {
            latitude = longitude = altitudeKm = speedKmS = radius = 0;
        }
        public GeodeticDataType(double lat, double lon, double altKm, double spdKms, double rad)
        {
            latitude = lat;
            longitude = lon;
            altitudeKm = altKm;
            speedKmS = spdKms;
            radius = rad;
        }
        public GeodeticDataType(GeodeticDataType copyFrom)
        {
            latitude = copyFrom.latitude;
            longitude = copyFrom.longitude;
            altitudeKm = copyFrom.altitudeKm;
            speedKmS = copyFrom.speedKmS;
            radius = copyFrom.radius;
        }

        public boolean isSet()
        {
            return(latitude != 0 || longitude != 0 || altitudeKm != 0);
        }

        public boolean equals(GeodeticDataType other)
        {
            return(other != null && other.latitude == this.latitude && other.longitude == this.longitude && other.altitudeKm == this.altitudeKm);
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
            dest.writeDouble(altitudeKm);
            dest.writeDouble(speedKmS);
            dest.writeDouble(radius);
        }
    }

    //Geodetic area type
    public static class GeodeticAreaType
    {
        public double latitudeWidth;
        public double longitudeWidth;

        public GeodeticAreaType()
        {
            latitudeWidth = longitudeWidth = 0;
        }
    }

    //Topographic data
    public static class TopographicDataType
    {
        public double azimuth;
        public double elevation;
        public double rangeKm;

        public TopographicDataType()
        {
            azimuth = elevation = rangeKm = Double.MAX_VALUE;
        }
        public TopographicDataType(double az, double el, double rkm)
        {
            azimuth = Globals.normalizePositiveAngle(az);
            elevation = el;
            rangeKm = rkm;
        }
    }

    //TLE data
    @SuppressWarnings("SpellCheckingInspection")
    public static class TLEDataType implements Parcelable, Serializable
    {
        //line 1
        public int satelliteNum;
        public char classification;
        public int launchYear;
        public int launchNum;
        public String launchPiece;
        public int epochYear;
        public double epochDay;
        public double meanMotionDeriv1;
        public double meanMotionDeriv2;
        public double drag;
        public int ephemeris;
        public int elementNum;

        //line 2
        public double inclinationDeg;
        public double rightAscnAscNodeDeg;
        public double eccentricity;
        public double argPerigreeDeg;
        public double meanAnomalyDeg;
        public double revsPerDay;
        public int revAtEpoch;

        //calculated
        public double epochJulian;					//Julian date of epoch
        public String internationalCode;            //International code

        //creator
        public static final Creator<TLEDataType> CREATOR = new Parcelable.Creator<TLEDataType>()
        {
            @Override
            public TLEDataType createFromParcel(Parcel source)
            {
                return(new TLEDataType(source.readInt(), (char)source.readInt(), source.readInt(), source.readInt(), source.readString(), source.readInt(), source.readDouble(),
                                       source.readDouble(), source.readDouble(), source.readDouble(), source.readInt(), source.readInt(), source.readDouble(), source.readDouble(),
                                       source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readInt(), source.readDouble(), source.readString()));
            }

            @Override
            public TLEDataType[] newArray(int size)
            {
                return(new TLEDataType[size]);
            }
        };

        public TLEDataType()
        {
            satelliteNum = Universe.IDs.Invalid;
            launchYear = 0;
            epochDay = 0;
        }
        public TLEDataType(int satNum, char cf, int lYr, int lNum, String lP, int eYr, double eDay, double mm1, double mm2, double dg, int eph, int el, double inc, double right, double ecc, double arg, double mean, double revs, int revEp, double epJ, String intCd)
        {
            satelliteNum = satNum;  classification = cf;    launchYear = lYr;   launchNum = lNum; launchPiece = lP; epochYear = eYr;    epochDay = eDay;
            meanMotionDeriv1 = mm1; meanMotionDeriv2 = mm2; drag = dg;  ephemeris = eph;    elementNum = el;    inclinationDeg = inc;   rightAscnAscNodeDeg = right;
            eccentricity = ecc; argPerigreeDeg = arg;   meanAnomalyDeg = mean;  revsPerDay = revs;  revAtEpoch = revEp; epochJulian = epJ;  internationalCode = intCd;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeInt(satelliteNum); dest.writeInt(classification); dest.writeInt(launchYear); dest.writeInt(launchNum); dest.writeString(launchPiece); dest.writeInt(epochYear); dest.writeDouble(epochDay);
            dest.writeDouble(meanMotionDeriv1); dest.writeDouble(meanMotionDeriv2); dest.writeDouble(drag); dest.writeInt(ephemeris); dest.writeInt(elementNum); dest.writeDouble(inclinationDeg); dest.writeDouble(rightAscnAscNodeDeg);
            dest.writeDouble(eccentricity); dest.writeDouble(argPerigreeDeg); dest.writeDouble(meanAnomalyDeg); dest.writeDouble(revsPerDay); dest.writeInt(revAtEpoch); dest.writeDouble(epochJulian); dest.writeString(internationalCode);
        }
    }

    //Orbit data
    public static class OrbitDataType implements Parcelable
    {
        public double meanMotion;						//radians per minute
        public double semiMinorAxis;					//ae units
        public double semiMajorAxis;					//ae units
        public double semiMajorAxisKm;                  //km
        public double perigee;							//perigee in km
        public double apogee;							//apogee in km
        public double periodMinutes;
        public byte predictionModel;                   //SgpModelType
        public static final Creator<OrbitDataType> CREATOR = new Parcelable.Creator<OrbitDataType>()
        {
            @Override
            public OrbitDataType createFromParcel(Parcel source)
            {
                return(new OrbitDataType(source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble(), source.readByte()));
            }

            @Override
            public OrbitDataType[] newArray(int size)
            {
                return(new OrbitDataType[size]);
            }
        };

        public OrbitDataType()
        {
            meanMotion = semiMinorAxis = semiMajorAxis = perigee = apogee = periodMinutes = 0;
            predictionModel = SgpModelType.Sdp4Model;
        }
        public OrbitDataType(double mm, double sMnAx, double sMjAx, double sMjAxKm, double pg, double apg, double pMin, byte pM)
        {
            meanMotion = mm;
            semiMinorAxis = sMnAx;
            semiMajorAxis = sMjAx;
            semiMajorAxisKm = sMjAxKm;
            perigee = pg;
            apogee = apg;
            periodMinutes = pMin;
            predictionModel = pM;
        }
        public OrbitDataType(OrbitDataType copyFrom)
        {
            meanMotion = copyFrom.meanMotion;
            semiMinorAxis = copyFrom.semiMinorAxis;
            semiMajorAxis = copyFrom.semiMajorAxis;
            semiMajorAxisKm = copyFrom.semiMajorAxisKm;
            perigee = copyFrom.perigee;
            apogee = copyFrom.apogee;
            periodMinutes = copyFrom.periodMinutes;
            predictionModel = copyFrom.predictionModel;
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeDouble(meanMotion);
            dest.writeDouble(semiMinorAxis);
            dest.writeDouble(semiMajorAxis);
            dest.writeDouble(semiMajorAxisKm);
            dest.writeDouble(perigee);
            dest.writeDouble(apogee);
            dest.writeDouble(periodMinutes);
            dest.writeByte(predictionModel);
        }
    }

    //Norad data
    @SuppressWarnings("SpellCheckingInspection")
    static class NoradDataType
    {
        //All prediction vars
        double m_satInc;	double m_satEcc;	double m_cosio;		double m_theta2; 	double m_x3thm1;
        double m_eosq;		double m_betao2;	double m_betao;		double m_aodp;		double mean_motion;
        double m_s4;		double m_qoms24;	double m_tsi;		double m_eta;
        double m_etasq;		double m_eeta;		double m_coef;		double m_coef1;		double m_c1;
        double m_c3;		double m_c4;		double m_sinio;
        double m_x1mth2; 	double m_xmdot;		double m_omgdot;	double m_xnodot;
        double m_xnodcf;	double m_t2cof;		double m_xlcof;		double m_aycof;		double m_x7thm1;

        //SGP4 prediction vars
        double m_c5;		double m_omgcof;	double m_xmcof;		double m_delmo;		double m_sinmo;

        //SDP4 prediction vars
        double m_sing;		double m_cosg;		double eqsq;		double siniq;		double cosiq;
        double rteqsq;		double ao;			double cosq2;		double sinomo;		double cosomo;
        double bsq;			double xlldot;		double omgdt;		double xnodot;		double xll;
        double omgasm;		double xnodes;		double _em;			double xinc;		double xn;
        double timeSince;	double dp_e3;		double dp_ee2;		double dp_se2;
        double dp_se3;		double dp_sgh2;		double dp_sgh3;		double dp_sgh4;		double dp_sghs;
        double dp_sh2;		double dp_sh3;		double dp_si2;		double dp_si3;		double dp_sl2;
        double dp_sl3;		double dp_sl4;		double dp_xgh2;		double dp_xgh3;		double dp_xgh4;
        double dp_xh2;		double dp_xh3;		double dp_xi2;		double dp_xi3;		double dp_xl2;
        double dp_xl3;		double dp_xl4;		double dp_xqncl;	double dp_zmol;		double dp_zmos;
        double dp_atime; 	double dp_d2201;	double dp_d2211; 	double dp_d3210;	double dp_d3222;
        double dp_d4410;	double dp_d4422;	double dp_d5220;	double dp_d5232;	double dp_d5421;
        double dp_d5433; 	double dp_del1;		double dp_del2;		double dp_del3;		double dp_fasx2;
        double dp_fasx4;	double dp_fasx6;	double dp_omegaq;	double dp_sse;		double dp_ssg;
        double dp_ssh;		double dp_ssi;		double dp_ssl;		double dp_step2;	double dp_stepn;
        double dp_stepp;	double dp_thgr;		double dp_xfact;	double dp_xlamo;	double dp_xli;
        double dp_xni;		boolean gp_reso;	boolean gp_sync;	double dpi_c;		double dpi_ctem;
        double dpi_day;		double dpi_gam;		double dpi_stem;	double dpi_xnodce;	double dpi_zcosgl;
        double dpi_zcoshl;	double dpi_zcosil;	double dpi_zsingl;	double dpi_zsinhl;	double dpi_zsinil;
        double dpi_zx;		double dpi_zy;

        public NoradDataType() {}

        public NoradDataType(NoradDataType copyFrom)
        {
            if(copyFrom != null)
            {
                this.m_satInc = copyFrom.m_satInc;	    this.m_satEcc = copyFrom.m_satEcc;	    this.m_cosio = copyFrom.m_cosio;		this.m_theta2 = copyFrom.m_theta2; 	    this.m_x3thm1 = copyFrom.m_x3thm1;
                this.m_eosq = copyFrom.m_eosq;		    this.m_betao2 = copyFrom.m_betao2;	    this.m_betao = copyFrom.m_betao;		this.m_aodp = copyFrom.m_aodp;		    this.mean_motion = copyFrom.mean_motion;
                this.m_s4 = copyFrom.m_s4;		        this.m_qoms24 = copyFrom.m_qoms24;	    this.m_tsi = copyFrom.m_tsi;		    this.m_eta = copyFrom.m_eta;
                this.m_etasq = copyFrom.m_etasq;	    this.m_eeta = copyFrom.m_eeta;		    this.m_coef = copyFrom.m_coef;		    this.m_coef1 = copyFrom.m_coef1;		this.m_c1 = copyFrom.m_c1;
                this.m_c3 = copyFrom.m_c3;		        this.m_c4 = copyFrom.m_c4;		        this.m_sinio = copyFrom.m_sinio;
                this.m_x1mth2 = copyFrom.m_x1mth2; 	    this.m_xmdot = copyFrom.m_xmdot;		this.m_omgdot = copyFrom.m_omgdot;	    this.m_xnodot = copyFrom.m_xnodot;
                this.m_xnodcf = copyFrom.m_xnodcf;	    this.m_t2cof = copyFrom.m_t2cof;		this.m_xlcof = copyFrom.m_xlcof;		this.m_aycof = copyFrom.m_aycof;		this.m_x7thm1 = copyFrom.m_x7thm1;

                //SGP4 prediction vars
                this.m_c5 = copyFrom.m_c5;		        this.m_omgcof = copyFrom.m_omgcof;	    this.m_xmcof = copyFrom.m_xmcof;		this.m_delmo = copyFrom.m_delmo;		this.m_sinmo = copyFrom.m_sinmo;

                //SDP4 prediction vars
                this.m_sing = copyFrom.m_sing;		    this.m_cosg = copyFrom.m_cosg;		    this.eqsq = copyFrom.eqsq;		        this.siniq = copyFrom.siniq;		    this.cosiq = copyFrom.cosiq;
                this.rteqsq = copyFrom.rteqsq;		    this.ao = copyFrom.ao;			        this.cosq2 = copyFrom.cosq2;		    this.sinomo = copyFrom.sinomo;		    this.cosomo = copyFrom.cosomo;
                this.bsq = copyFrom.bsq;			    this.xlldot = copyFrom.xlldot;		    this.omgdt = copyFrom.omgdt;		    this.xnodot = copyFrom.xnodot;		    this.xll = copyFrom.xll;
                this.omgasm = copyFrom.omgasm;		    this.xnodes = copyFrom.xnodes;		    this._em = copyFrom._em;			    this.xinc = copyFrom.xinc;		        this.xn = copyFrom.xn;
                this.timeSince = copyFrom.timeSince;			        this.dp_e3 = copyFrom.dp_e3;		    this.dp_ee2 = copyFrom.dp_ee2;		    this.dp_se2 = copyFrom.dp_se2;
                this.dp_se3 = copyFrom.dp_se3;		    this.dp_sgh2 = copyFrom.dp_sgh2;		this.dp_sgh3 = copyFrom.dp_sgh3;		this.dp_sgh4 = copyFrom.dp_sgh4;		this.dp_sghs = copyFrom.dp_sghs;
                this.dp_sh2 = copyFrom.dp_sh2;		    this.dp_sh3 = copyFrom.dp_sh3;		    this.dp_si2 = copyFrom.dp_si2;		    this.dp_si3 = copyFrom.dp_si3;		    this.dp_sl2 = copyFrom.dp_sl2;
                this.dp_sl3 = copyFrom.dp_sl3;		    this.dp_sl4 = copyFrom.dp_sl4;		    this.dp_xgh2 = copyFrom.dp_xgh2;		this.dp_xgh3 = copyFrom.dp_xgh3;		this.dp_xgh4 = copyFrom.dp_xgh4;
                this.dp_xh2 = copyFrom.dp_xh2;		    this.dp_xh3 = copyFrom.dp_xh3;		    this.dp_xi2 = copyFrom.dp_xi2;		    this.dp_xi3 = copyFrom.dp_xi3;		    this.dp_xl2 = copyFrom.dp_xl2;
                this.dp_xl3 = copyFrom.dp_xl3;		    this.dp_xl4 = copyFrom.dp_xl4;		    this.dp_xqncl = copyFrom.dp_xqncl;	    this.dp_zmol = copyFrom.dp_zmol;		this.dp_zmos = copyFrom.dp_zmos;
                this.dp_atime = copyFrom.dp_atime; 	    this.dp_d2201 = copyFrom.dp_d2201;	    this.dp_d2211 = copyFrom.dp_d2211; 	    this.dp_d3210 = copyFrom.dp_d3210;	    this.dp_d3222 = copyFrom.dp_d3222;
                this.dp_d4410 = copyFrom.dp_d4410;	    this.dp_d4422 = copyFrom.dp_d4422;	    this.dp_d5220 = copyFrom.dp_d5220;	    this.dp_d5232 = copyFrom.dp_d5232;	    this.dp_d5421 = copyFrom.dp_d5421;
                this.dp_d5433 = copyFrom.dp_d5433; 	    this.dp_del1 = copyFrom.dp_del1;		this.dp_del2 = copyFrom.dp_del2;		this.dp_del3 = copyFrom.dp_del3;		this.dp_fasx2 = copyFrom.dp_fasx2;
                this.dp_fasx4 = copyFrom.dp_fasx4;	    this.dp_fasx6 = copyFrom.dp_fasx6;	    this.dp_omegaq = copyFrom.dp_omegaq;	this.dp_sse = copyFrom.dp_sse;		    this.dp_ssg = copyFrom.dp_ssg;
                this.dp_ssh = copyFrom.dp_ssh;		    this.dp_ssi = copyFrom.dp_ssi;		    this.dp_ssl = copyFrom.dp_ssl;		    this.dp_step2 = copyFrom.dp_step2;	    this.dp_stepn = copyFrom.dp_stepn;
                this.dp_stepp = copyFrom.dp_stepp;	    this.dp_thgr = copyFrom.dp_thgr;		this.dp_xfact = copyFrom.dp_xfact;	    this.dp_xlamo = copyFrom.dp_xlamo;	    this.dp_xli = copyFrom.dp_xli;
                this.dp_xni = copyFrom.dp_xni;		    this.gp_reso = copyFrom.gp_reso;	this.gp_sync = copyFrom.gp_sync;	this.dpi_c = copyFrom.dpi_c;		    this.dpi_ctem = copyFrom.dpi_ctem;
                this.dpi_day = copyFrom.dpi_day;		this.dpi_gam = copyFrom.dpi_gam;		this.dpi_stem = copyFrom.dpi_stem;	    this.dpi_xnodce = copyFrom.dpi_xnodce;	this.dpi_zcosgl = copyFrom.dpi_zcosgl;
                this.dpi_zcoshl = copyFrom.dpi_zcoshl;	this.dpi_zcosil = copyFrom.dpi_zcosil;	this.dpi_zsingl = copyFrom.dpi_zsingl;	this.dpi_zsinhl = copyFrom.dpi_zsinhl;	this.dpi_zsinil = copyFrom.dpi_zsinil;
                this.dpi_zx = copyFrom.dpi_zx;		    this.dpi_zy = copyFrom.dpi_zy;
            }
        }
    }

    //Planet data type
    public static class PlanetDataType implements Parcelable
    {
        final double hourAngleDeg;
        final double declinationDeg;
        final double rightAscDeg;
        final double distanceKm;

        public static final Creator<PlanetDataType> CREATOR = new Parcelable.Creator<PlanetDataType>()
        {
            @Override
            public PlanetDataType createFromParcel(Parcel source)
            {
                return(new PlanetDataType(source.readDouble(), source.readDouble(), source.readDouble(), source.readDouble()));
            }

            @Override
            public PlanetDataType[] newArray(int size)
            {
                return(new PlanetDataType[size]);
            }
        };

        PlanetDataType(int planetNumber, double julianDate, double latitude, double longitude, double rightAscHours, double declinationDeg, double distanceKm)
        {
            double calcHourAngleRad;
            double julianCenturies = calcTimeJulianCent(julianDate);
            GeodeticDataType polarLocation;
            GeodeticDataType cartesianLocation;
            GeodeticDataType equatorialLocation;
            GeodeticDataType altAzLocation;
            GeodeticDataType earthCartesianLocation;

            switch(planetNumber)
            {
                case Universe.IDs.Earth:
                case Universe.IDs.Sun:
                case Universe.IDs.Moon:
                case Universe.IDs.Mars:
                case Universe.IDs.Mercury:
                case Universe.IDs.Venus:
                case Universe.IDs.Jupiter:
                case Universe.IDs.Saturn:
                case Universe.IDs.Uranus:
                case Universe.IDs.Neptune:
                case Universe.IDs.Pluto:
                    if(planetNumber == Universe.IDs.Moon)
                    {
                        polarLocation = Universe.Moon.getPolarLocation(julianCenturies);
                    }
                    else
                    {
                        polarLocation = Universe.getPolarLocation(planetNumber, julianCenturies);
                    }
                    cartesianLocation = polarToCartesian(polarLocation);

                    if(planetNumber != Universe.IDs.Moon && planetNumber != Universe.IDs.Sun)
                    {
                        earthCartesianLocation = polarToCartesian(Universe.getPolarLocation(Universe.IDs.Earth, julianCenturies));
                        cartesianLocation.latitude -= earthCartesianLocation.latitude;
                        cartesianLocation.longitude -= earthCartesianLocation.longitude;
                        cartesianLocation.radius -= earthCartesianLocation.radius;
                    }

                    equatorialLocation = rotateLocation(cartesianLocation, Math.toRadians(calcObliquityCorrection(julianCenturies)), 0);
                    altAzLocation = rotateLocation(equatorialLocation, -julianDateToLST(julianDate, longitude), 2);

                    this.hourAngleDeg = Math.toDegrees(Math.atan2(-altAzLocation.longitude, altAzLocation.latitude));
                    this.rightAscDeg = Math.toDegrees(Math.atan2(equatorialLocation.longitude, equatorialLocation.latitude));
                    this.declinationDeg = Math.toDegrees(Math.atan2(equatorialLocation.radius, Math.sqrt(equatorialLocation.latitude * equatorialLocation.latitude + equatorialLocation.longitude * equatorialLocation.longitude)));

                    altAzLocation = rotateLocation(altAzLocation, Math.toRadians(latitude) - (HalfPI), 1);
                    this.distanceKm = Math.sqrt(altAzLocation.latitude * altAzLocation.latitude + altAzLocation.longitude * altAzLocation.longitude + altAzLocation.radius * altAzLocation.radius) * AU;
                    break;

                default:
                    this.declinationDeg = declinationDeg;
                    this.distanceKm = distanceKm;
                    this.rightAscDeg = rightAscHours * 15;
                    calcHourAngleRad = julianDateToLMST(julianDate, Math.toRadians(longitude)) - Math.toRadians(rightAscDeg);
                    if(calcHourAngleRad < 0)
                    {
                        calcHourAngleRad += TwoPI;
                    }
                    if(calcHourAngleRad > Math.PI)
                    {
                        calcHourAngleRad = calcHourAngleRad - TwoPI;
                    }
                    this.hourAngleDeg = Math.toDegrees(calcHourAngleRad);
                    break;
            }
        }
        PlanetDataType(double hourAngleDeg, double declinationDeg, double rightAscDeg, double distanceKm)
        {
            this.hourAngleDeg = hourAngleDeg;
            this.declinationDeg = declinationDeg;
            this.rightAscDeg = rightAscDeg;
            this.distanceKm = distanceKm;
        }
        PlanetDataType(PlanetDataType copyFrom)
        {
            if(copyFrom != null)
            {
                this.hourAngleDeg = copyFrom.hourAngleDeg;
                this.declinationDeg = copyFrom.declinationDeg;
                this.rightAscDeg = copyFrom.rightAscDeg;
                this.distanceKm = copyFrom.distanceKm;
            }
            else
            {
                this.hourAngleDeg = this.declinationDeg = this.rightAscDeg = this.distanceKm = Double.MAX_VALUE;
            }
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags)
        {
            dest.writeDouble(hourAngleDeg);
            dest.writeDouble(declinationDeg);
            dest.writeDouble(rightAscDeg);
            dest.writeDouble(distanceKm);
        }
    }

    //Satellite object
    public static class SatelliteObjectType implements Parcelable
    {
        public String name;
        public EciDataType eci;					//eci position
        public GeodeticDataType geo;			//geodetic position
        public GeodeticDataType savedGeo;       //saved geodetic position
        public TLEDataType tle;					//tle information
        public OrbitDataType orbit;				//orbit information
        public NoradDataType norad;				//norad data
        public PlanetDataType planetData;       //planet data
        public static final Creator<SatelliteObjectType> CREATOR = new Parcelable.Creator<SatelliteObjectType>()
        {
            @Override
            public SatelliteObjectType createFromParcel(Parcel source)
            {
                return(new SatelliteObjectType(source.readString(), source.readParcelable(EciDataType.class.getClassLoader()), source.readParcelable(GeodeticDataType.class.getClassLoader()), source.readParcelable(TLEDataType.class.getClassLoader()), source.readParcelable(OrbitDataType.class.getClassLoader()), source.readParcelable(PlanetDataType.class.getClassLoader())));
            }

            @Override
            public SatelliteObjectType[] newArray(int size)
            {
                return(new SatelliteObjectType[size]);
            }
        };

        private void baseConstructor()
        {
            name = null;
            eci = new EciDataType();
            geo = new GeodeticDataType();
            savedGeo = new GeodeticDataType();
            tle = new TLEDataType();
            orbit = new OrbitDataType();
            norad = new NoradDataType();
            planetData = null;
        }

        SatelliteObjectType()
        {
            baseConstructor();
        }
        SatelliteObjectType(String nm, EciDataType e, GeodeticDataType g, TLEDataType t, OrbitDataType o, PlanetDataType p)
        {
            name = nm;
            eci = new EciDataType(e);
            geo = new GeodeticDataType(g);
            savedGeo = new GeodeticDataType();
            tle = t;
            orbit = new OrbitDataType(o);
            norad = loadNorad(orbit, tle);
            planetData = new PlanetDataType(p);
        }
        SatelliteObjectType(SatelliteObjectType copyFrom)
        {
            if(copyFrom != null)
            {
                name = copyFrom.name;
                eci = new EciDataType(copyFrom.eci);
                geo = new GeodeticDataType(copyFrom.geo);
                savedGeo = new GeodeticDataType();
                tle = copyFrom.tle;
                orbit = new OrbitDataType(copyFrom.orbit);
                norad = new NoradDataType(copyFrom.norad);
                planetData = new PlanetDataType(copyFrom.planetData);
            }
            else
            {
                baseConstructor();
            }
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(name);
            dest.writeParcelable(eci, 0);
            dest.writeParcelable(geo, 0);
            dest.writeParcelable(tle, 0);
            dest.writeParcelable(orbit, 0);
            dest.writeParcelable(planetData, 0);
        }

        public String getName()
        {
            return(("").equals(name) ? null : name);
        }

        public int getSatelliteNum()
        {
            return(tle != null ? tle.satelliteNum : Universe.IDs.None);
        }
    }

    //Observer object
    public static class ObserverType implements Parcelable
    {
        public TimeZone timeZone;
        public final GeodeticDataType geo;				//geodetic position

        public static final Creator<ObserverType> CREATOR = new Parcelable.Creator<ObserverType>()
        {
            @Override
            public ObserverType createFromParcel(Parcel source)
            {
                return(new ObserverType(source.readString(), source.readParcelable(GeodeticDataType.class.getClassLoader())));
            }

            @Override
            public ObserverType[] newArray(int size)
            {
                return(new ObserverType[size]);
            }
        };

        ObserverType()
        {
            timeZone = TimeZone.getDefault();
            geo = new GeodeticDataType();
        }
        ObserverType(String zoneId, GeodeticDataType g)
        {
            timeZone = (zoneId != null ? TimeZone.getTimeZone(zoneId) : TimeZone.getDefault());
            geo = new GeodeticDataType(g);
        }
        ObserverType(String zoneId, double latitude, double longitude, double altitudeKm)
        {
            this(zoneId, new GeodeticDataType(latitude, longitude, altitudeKm, 0, 0));
        }

        @Override
        public int describeContents()
        {
            return(0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags)
        {
            dest.writeString(timeZone.getID());
            dest.writeParcelable(geo, 0);
        }

        public boolean notEqual(ObserverType other)
        {
            return(other == null || geo.latitude != other.geo.latitude || geo.longitude != other.geo.longitude || geo.altitudeKm != other.geo.altitudeKm);
        }
    }

    //
    //Functions
    //

    //Returns the ArcTangent of Math.sin(x) / Math.cos(x) with the correct quadrant of the angle.
    private static double arcTan(double sinX, double cosX)
    {
        double ret;

        if(cosX == 0.0)
        {
            if (sinX > 0.0)
                ret = HalfPI;
            else
                ret = 3.0 * Math.PI / 2.0;
        }
        else
        {
            if (cosX > 0.0)
                ret = Math.atan(sinX / cosX);
            else
                ret = Math.PI + Math.atan(sinX / cosX);
        }

        return(ret);
    }

    //Returns the positive modulus of arg with TwoPI
    private static double fmod2P(double arg)
    {
        double mod = Math.IEEEremainder(arg, TwoPI);

        if (mod < 0.0)
            mod += TwoPI;

        return(mod);
    }

    //Returns the Julian date for the given Julian day (0 - 365) and year (1582 - ?)
    private static double julianDateDY(double julianDay, int year)
    {
        int a, b;
        double newYears;

        year--;

        //centuries are not leap years unless they divide by 400
        a = (year / 100);
        b = 2 - a + (a / 4);

        newYears = Math.floor(DaysPerYear * year) + (int)(30.6001 * 14) + 1720994.5 + b;  // 1720994.5 = Oct 30, year -1

        return(newYears + julianDay);
    }

    //Returns the Julian date for the given day of the year, year, and time
    private static double julianDateDYT(int day_of_year, int year, int hour, int minutes, double seconds)
    {
        double julian_day;

        julian_day = day_of_year + (hour + (minutes + (seconds / 60.0)) / 60.0) / 24.0;

        return(julianDateDY(julian_day, year));
    }

    //Returns the Julian date for the given month, day, year, and time
    private static double julianDate(int month, int day, int year, int hour, int minutes, double seconds)
    {
        //calculate the day of the year (1..366)
        int day_of_year;
        int f1 = (int)((275.0 * month) / 9.0);
        int f2 = (int)((month + 9.0) / 12.0);

        if(IsLeapYear(year))
        {
            day_of_year = f1 - f2 + day - 30;
        }
        else
        {
            day_of_year = f1 - (2 * f2) + day - 30;
        }

        return(julianDateDYT(day_of_year, year, hour, minutes, seconds));
    }

    //Returns the Julian date for the given calendar
    public static double julianDateCalendar(Calendar gmtDate)
    {
        return(Calculations.julianDate(gmtDate.get(Calendar.MONTH) + 1, gmtDate.get(Calendar.DAY_OF_MONTH), gmtDate.get(Calendar.YEAR), gmtDate.get(Calendar.HOUR_OF_DAY), gmtDate.get(Calendar.MINUTE), gmtDate.get(Calendar.SECOND) + (gmtDate.get(Calendar.MILLISECOND) / 1000.0)));
    }
    public static double julianDateCalendar(long dateMs, ObserverType observer)
    {
        TimeZone localZone = TimeZone.getDefault();
        TimeZone wantedZone = (observer != null ? observer.timeZone : localZone);
        Calendar gmtTime = Globals.getGMTTime(dateMs, localZone, wantedZone, wantedZone.getID());

        return(julianDateCalendar(gmtTime));
    }

    //Returns the epoch (julian) calendar for the given string input
    public static synchronized Calendar epochCalendar(String dateString)
    {
        try
        {
            epochParser.parse(dateString);
            return(epochParser.getCalendar());
        }
        catch(Exception ex)
        {
            return(Globals.getGMTTime());
        }
    }

    //Returns the Greenwich Mean Sidereal Time (GMST) in radians (ThetaG) from the given julian date
    @SuppressWarnings("SpellCheckingInspection")
    private static double julianDateToGMST(double julianDate)
    {
        double gmst;
        double ut, tu;

        ut = Math.IEEEremainder(julianDate + 0.5, 1.0);
        tu = (julianDate - JanFirst2000JD - ut) / JulianCentury;
        gmst = 24110.54841 + tu * (8640184.812866 + tu * (0.093104 - tu * 6.2e-06));
        gmst = Math.IEEEremainder(gmst + SecondsPerDay * EarthRotPerSD * ut, SecondsPerDay);

        //wrap negative modulo value
        if(gmst < 0.0)
            gmst += SecondsPerDay;

        return(TwoPI * (gmst / SecondsPerDay));
    }

    //Returns the Local Mean Sidereal Time (LMST) in radians from the given julian date and longitude
    @SuppressWarnings("SpellCheckingInspection")
    private static double julianDateToLMST(double julianDate, double longitudeRads)
    {
        return(Math.IEEEremainder(julianDateToGMST(julianDate) + longitudeRads, TwoPI));
    }

    //Returns the local sidereal time
    private static double julianDateToLST(double julianDate, double longitude)
    {
        double ut;
        double julianRel, julianRelInt;
        double julianC;
        double lstDeg;

        ut = 0;
        julianRel = julianDate - JanFirst2000JD - ut;
        julianRelInt = Math.floor(julianRel);
        julianC = julianRel / JulianCentury;
        lstDeg = 280.46061837 + 360.98564736629 * (julianRel - julianRelInt) + 0.98564736629 * julianRelInt + Math.pow(julianC, 2) * (0.000387933 - julianC / 38710000.0);
        return(Math.toRadians(lstDeg) + Math.toRadians(longitude));
    }

    //Returns julian date century
    private static double calcTimeJulianCent(double julianDate)
    {
        return((julianDate  - JanFirst2000JD) / JulianCentury);
    }

    //Epoch to calendar in GMT time
    public static Calendar epochToGMTCalendar(int year, double dayOfYear)
    {
        int day = (int)Math.floor(dayOfYear);
        int hour;
        int minute;
        int second;
        double remainder;
        Calendar gmt_cal = Globals.getGMTTime();
        gmt_cal.clear();

        remainder =  (dayOfYear - day) * 24;
        hour = (int)Math.floor(remainder);
        remainder = (remainder - hour) * 60;
        minute = (int)Math.floor(remainder);
        remainder = (remainder - minute) * 60;
        second = (int)Math.floor(remainder);
        remainder = (remainder - second) * 1000;

        gmt_cal.add(Calendar.YEAR, year - 1970);
        gmt_cal.add(Calendar.DAY_OF_YEAR, day - 1);     //note: -1 since after clear and add year is set to 1
        gmt_cal.add(Calendar.HOUR_OF_DAY, hour);
        gmt_cal.add(Calendar.MINUTE, minute);
        gmt_cal.add(Calendar.SECOND, second);
        gmt_cal.add(Calendar.MILLISECOND, (int)remainder);

        return(gmt_cal);
    }

    //Returns mean obliquity of ecliptic
    private static double calcMeanObliquityOfEcliptic(double t)
    {
        double seconds, e0;

        seconds = 21.448 - t*(46.8150 + t*(0.00059 - t*(0.001813)));
        e0 = 23.0 + (26.0 + (seconds/60.0))/60.0;
        return(e0);		// in degrees
    }

    //Returns obliquity correction
    private static double calcObliquityCorrection(double time)
    {
        double e0, omega, e;

        e0 = calcMeanObliquityOfEcliptic(time);
        omega = 125.04 - 1934.136 * time;
        e = e0 + 0.00256 * Math.cos(Math.toRadians(omega));
        return(e);		// in degrees
    }

    //Converts the given ECI position at the given Julian date to geodetic coordinates
    private static GeodeticDataType eciToGeo(EciDataType eciData)
    {
        double theta;
        double r, e2;
        double c, phi, delta;
        double longitude, latitude, altitude;
        GeodeticDataType geoData = new GeodeticDataType();

        //noinspection SuspiciousNameCombination
        theta = arcTan(eciData.positionY, eciData.positionX);
        longitude = Math.IEEEremainder(theta - julianDateToGMST(eciData.julianDate), TwoPI);

        //wrap negative modulo
        if (longitude < 0.0)
            longitude += TwoPI;

        r = Math.sqrt(Square(eciData.positionX) + Square(eciData.positionY));
        e2  = EarthFlattening * (2.0 - EarthFlattening);
        latitude = arcTan(eciData.positionZ, r);

        delta = 1.0e-07;
        do
        {
            phi = latitude;
            c = 1.0 / Math.sqrt(1.0 - e2 * Square(Math.sin(phi)));
            latitude = arcTan(eciData.positionZ + EarthRadiusKM * c * e2 * Math.sin(phi), r);
        }
        while(Math.abs(latitude - phi) > delta);

        altitude = r / Math.cos(latitude) - EarthRadiusKM * c;

        geoData.latitude = Math.toDegrees(latitude);
        geoData.longitude = Math.toDegrees(longitude);
        if(geoData.longitude > 180)
        {
            geoData.longitude -= 360;
        }
        geoData.altitudeKm = altitude;

        return(geoData);
    }

    //Converts the given geodetic coordinates to an ECI position
    @SuppressWarnings("SpellCheckingInspection")
    private static EciDataType geoToECI(GeodeticDataType geoData, double julianDate)
    {
        double theta, c, s, achcp;
        double latitudeRads, longitudeRads, altitude;
        EciDataType eci_data = new EciDataType();

        latitudeRads = Math.toRadians(geoData.latitude);
        longitudeRads = Math.toRadians(geoData.longitude);
        altitude = geoData.altitudeKm;

        //calculate Local Mean Sidereal Time (theta)
        theta = julianDateToLMST(julianDate, longitudeRads);
        c = 1.0 / Math.sqrt(1.0 + EarthFlattening * (EarthFlattening - 2.0) * Square(Math.sin(latitudeRads)));
        s = Square(1.0 - EarthFlattening) * c;
        achcp = (EarthRadiusKM * c + altitude) * Math.cos(latitudeRads);

        eci_data.julianDate = julianDate;

        eci_data.positionX = achcp * Math.cos(theta);               // km
        eci_data.positionY = achcp * Math.sin(theta);               // km
        eci_data.positionZ = (EarthRadiusKM * s + altitude) * Math.sin(latitudeRads);   // km
        //eci_data.position_w = Math.sqrt(Square(m_pos.m_x) + Square(m_pos.m_y) + Square(m_pos.m_z));            // range, km
        return(eci_data);
    }

    //Converts from polar to cartesian coordinates
    private static GeodeticDataType polarToCartesian(GeodeticDataType polarData)
    {
        double cosLat = Math.cos(polarData.latitude);
        GeodeticDataType cart_data = new GeodeticDataType();

        cart_data.latitude = Math.cos(polarData.longitude) * cosLat * polarData.radius;
        cart_data.longitude = Math.sin(polarData.longitude) * cosLat * polarData.radius;
        cart_data.radius = Math.sin(polarData.latitude) * polarData.radius;

        return(cart_data);
    }

    //Rotates
    private static GeodeticDataType rotateLocation(GeodeticDataType location, double radDeg, int axis)
    {
        int a = (axis + 1) % 3;
        int b = (axis + 2) % 3;
        double aVal = (a == 0 ? location.latitude : a == 1 ? location.longitude : location.radius);
        double bVal = (b == 0 ? location.latitude : b == 1 ? location.longitude : location.radius);
        double sinRadDeg = Math.sin(radDeg);
        double cosRadDeg = Math.cos(radDeg);
        double temp = aVal * cosRadDeg - bVal * sinRadDeg;
        double temp2 = bVal * cosRadDeg + aVal * sinRadDeg;
        GeodeticDataType rotatedLocation = new GeodeticDataType(location);

        switch(a)
        {
            case 0:
                rotatedLocation.latitude = temp;
                break;

            case 1:
                rotatedLocation.longitude = temp;
                break;

            default:
                rotatedLocation.radius = temp;
                break;
        }

        switch(b)
        {
            case 0:
                rotatedLocation.latitude = temp2;
                break;

            case 1:
                rotatedLocation.longitude = temp2;
                break;

            default:
                rotatedLocation.radius = temp2;
                break;
        }

        return(rotatedLocation);
    }

    //Tries to parse TLE year
    public static int tryParseTLEYear(String tleLine1, int startIndex)
    {
        //get 2 digit year
        int year = Globals.tryParseInt(tleLine1.substring(startIndex, startIndex + 2));

        //if not invalid
        if(year != Integer.MAX_VALUE)
        {
            //return corrected year
            if(year >= 57)
            {
                year  += 1900;
            }
            else
            {
                year  += 2000;
            }
            return(year);
        }
        else
        {
            //return invalid
            return(Integer.MAX_VALUE);
        }
    }

    //Tries to parse TLE decimal exponent (decimal assumed in front)
    private static double tryParseTLEDecimalExponent(String tleLine, int divisor, int startIndex, int endIndex)
    {
        String numberString = tleLine.charAt(startIndex) + "." + tleLine.substring(startIndex + 1, endIndex);
        double numberValue = Globals.tryParseDouble(numberString);
        return(numberValue * (numberValue < Double.MAX_VALUE ? Math.pow(10, divisor) : 1));
    }

    //Returns a TLE decimal exponent (value with decimal assumed in front, ending with exponent)
    //Example: 0.12345E-6 -> 12345-6
    public static String getTLEDecimalExponentString(double value)
    {
        String valueExpString = String.format(Locale.US, "%E", value).replace("+", " ");
        int expIndex = valueExpString.indexOf("E");
        if(Build.VERSION.SDK_INT < 21 && valueExpString.charAt(expIndex + 2) == '0')
        {
            valueExpString = valueExpString.substring(0, expIndex + 2) + valueExpString.substring(expIndex + 3);
        }
        int exp = (expIndex >= 0 && (expIndex + 1) < valueExpString.length() ? Integer.parseInt(valueExpString.substring(expIndex + 1).trim()) : 0);
        int usedExp = (exp >= 0 ? (exp - 4) : (exp + 1));
        int decimalValue = (int)(value * Math.pow(10, -exp + 4));

        if(decimalValue == 0)
        {
            usedExp = 0;
        }

        return(String.format(Locale.US, "%05d", decimalValue) + (usedExp == 0 ? "-" : usedExp > 0 ? "+" : "") + usedExp);
    }

    //Gets TLE checksum character
    public static String getTLEChecksum(String tleLine)
    {
        int index;
        int sum = 0;
        char currentChar;

        //go through each char
        for(index = 0; index < tleLine.length(); index++)
        {
            //get current char
            currentChar = tleLine.charAt(index);

            //add to sum
            sum += (currentChar == '-' ? 1 : (currentChar >= '0' && currentChar <= '9') ? (currentChar - '0') : 0);
        }

        //return mod 10 of sum
        return(String.valueOf(sum % 10));
    }

    //Loads multiple name from given json objects
    public static String[] loadNames(JSONObject[] data)
    {
        int index;
        ArrayList<String> nameList = new ArrayList<>(0);

        //go through each object
        for(index = 0; index < data.length; index++)
        {
            String currentName;
            JSONObject dataItem = data[index];

            //try to get name
            try
            {
                currentName = dataItem.getString(GPParams.Name);
            }
            catch(Exception ex)
            {
                currentName = null;
            }

            //add name to list
            nameList.add(currentName);
        }

        //return list
        return(nameList.toArray(new String[0]));
    }

    //Loads multiple TLEs from given json objects
    public static TLEDataType[] loadTLEs(JSONObject[] data)
    {
        int index;
        int index2;
        int length;
        ArrayList<TLEDataType> tleList = new ArrayList<>(0);

        //go through each object
        for(index = 0; index < data.length; index++)
        {
            String value;
            Calendar epochDate;
            TLEDataType tleData = new TLEDataType();
            JSONObject dataItem = data[index];

            try
            {
                tleData.satelliteNum = dataItem.getInt(GPParams.SatNum);
                tleData.classification = dataItem.getString(GPParams.Class).charAt(0);
                value = dataItem.getString(GPParams.ObjectId);
                length = value.length();
                tleData.internationalCode = value;
                tleData.launchYear = (length > 4 ? Integer.parseInt(value.substring(0, 4)) : Integer.MAX_VALUE);
                index2 = value.indexOf("-");
                tleData.launchNum = (index2 >= 0 && index2 + 4 < length ? Integer.parseInt(value.substring(index2 + 1, index2 + 4)) : 0);
                tleData.launchPiece = (index2 >= 0 && index2 + 4 < length ? value.substring(index2 + 4) : "");
                epochDate = epochCalendar(dataItem.getString(GPParams.Epoch).replace("T", " "));
                tleData.epochJulian = julianDateCalendar(epochDate);
                tleData.epochYear = epochDate.get(Calendar.YEAR);
                tleData.epochDay = epochDate.get(Calendar.DAY_OF_YEAR) + (epochDate.get(Calendar.HOUR_OF_DAY) / HoursPerDay) + (epochDate.get(Calendar.MINUTE) / MinutesPerDay) + (epochDate.get(Calendar.SECOND) / SecondsPerDay) + (epochDate.get(Calendar.MILLISECOND) / MsPerDay);
                tleData.meanMotionDeriv1 = dataItem.getDouble(GPParams.MeanMotion1);
                tleData.meanMotionDeriv2 = dataItem.getDouble(GPParams.MeanMotion2);
                tleData.drag = dataItem.getDouble(GPParams.Drag);
                tleData.ephemeris = dataItem.getInt(GPParams.Ephem);
                tleData.elementNum = dataItem.getInt(GPParams.Enum);
                tleData.inclinationDeg = dataItem.getDouble(GPParams.Inclin);
                tleData.rightAscnAscNodeDeg = dataItem.getDouble(GPParams.RightAscn);
                tleData.eccentricity = dataItem.getDouble(GPParams.Eccen);
                tleData.argPerigreeDeg = dataItem.getDouble(GPParams.ArgPeri);
                tleData.meanAnomalyDeg = dataItem.getDouble(GPParams.MeanAnom);
                tleData.revsPerDay = dataItem.getDouble(GPParams.Revs);
                tleData.revAtEpoch = dataItem.getInt(GPParams.RevsEp);
            }
            catch(Exception ex)
            {
                //set invalid
                tleData = null;
            }

            //add TLE data
            tleList.add(tleData);
        }

        //return list
        return(tleList.toArray(new TLEDataType[0]));
    }

    //Loads TLE data
    public static TLEDataType loadTLE(String line1, String line2)
    {
        int index;
        int divisor;
        String line1Copy;
        String line2Copy;
        TLEDataType tleData = new TLEDataType();
        char[] line1ArrayCopy;
        char[] line2ArrayCopy;

        //if not long enough
        if(line1 == null || line1.length() < 69 || line2 == null || line2.length() < 69)
        {
            //stop
            return(tleData);
        }

        //make copies of lines that might need altering
        line1ArrayCopy = line1.toCharArray();
        line2ArrayCopy = line2.toCharArray();

        //fill in blank spaces (else sscanf reads past intended area)
        for(index = 2; index < 69; index++)
        {
            switch(index)
            {
                //indexes that can be blank
                case 1		:
                case 8		:
                case 15     :
                case 16     :
                case 17		:
                case 32		:
                case 43		:
                case 52		:
                case 61		:
                case 63		:
                    //do nothing
                    break;

                //indexes that can have a +/- sign
                case 33		:
                case 44		:
                case 53		:
                    if(line1ArrayCopy[index] == ' ')
                    {
                        //make sure there is a sign here
                        line1ArrayCopy[index] = '+';
                    }
                    break;

                //should not have a blank space
                default		:
                    if(line1ArrayCopy[index] == ' ')
                    {
                        //fill with 0
                        line1ArrayCopy[index] = '0';
                    }
                    break;
            }
        }
        for(index = 2; index < 69; index++)
        {
            switch(index)
            {
                //indexes that can be blank
                case 1		:
                case 7		:
                case 16		:
                case 25		:
                case 33		:
                case 42		:
                case 51		:
                    //do nothing
                    break;

                //should not have a blank space
                default		:
                    if(line2ArrayCopy[index] == ' ')
                    {
                        //fill with 0
                        line2ArrayCopy[index] = '0';
                    }
                    break;
            }
        }
        line1Copy = new String(line1ArrayCopy);
        line2Copy = new String(line2ArrayCopy);

        //read remaining line 1 values
        tleData.satelliteNum = Globals.tryParseInt(line1Copy.substring(TLE1Index.SatNum, TLE1Index.SatNum + 5));
        tleData.classification = line1Copy.charAt(TLE1Index.Class);
        tleData.launchYear = tryParseTLEYear(line1Copy, TLE1Index.LaunchYear);
        tleData.launchNum = Globals.tryParseInt(line1Copy.substring(TLE1Index.LaunchNum, TLE1Index.LaunchNum + 3));
        tleData.launchPiece = line1Copy.substring(TLE1Index.LaunchPiece, TLE1Index.LaunchPiece + 3).trim();
        tleData.epochYear = tryParseTLEYear(line1Copy, TLE1Index.EpochYear);
        tleData.epochDay = Globals.tryParseDouble(line1Copy.substring(TLE1Index.EpochDay, TLE1Index.EpochDay + 12));
        tleData.meanMotionDeriv1 = Globals.tryParseDouble(line1Copy.substring(TLE1Index.MeanMotion1, TLE1Index.MeanMotion1 + 10));
        divisor = Globals.tryParseInt(line1Copy.substring(TLE1Index.MeanMotion2Div, TLE1Index.MeanMotion2Div + 2));
        tleData.meanMotionDeriv2 = tryParseTLEDecimalExponent(line1Copy, divisor, TLE1Index.MeanMotion2, TLE1Index.MeanMotion2 + 6);
        divisor = Globals.tryParseInt(line1Copy.substring(TLE1Index.DragDiv, TLE1Index.DragDiv + 2));
        tleData.drag = tryParseTLEDecimalExponent(line1Copy, divisor, TLE1Index.Drag, TLE1Index.Drag + 6);
        tleData.ephemeris = Globals.tryParseInt(line1Copy.substring(TLE1Index.Ephem, TLE1Index.Ephem + 1));
        tleData.elementNum = Globals.tryParseInt(line1Copy.substring(TLE1Index.Enum, TLE1Index.Enum + 4));

        //read line 2 values
        tleData.inclinationDeg = Globals.tryParseDouble(line2Copy.substring(TLE2Index.Inclin, TLE2Index.Inclin + 8));
        tleData.rightAscnAscNodeDeg = Globals.tryParseDouble(line2Copy.substring(TLE2Index.RightAscn, TLE2Index.RightAscn + 8));
        tleData.eccentricity = Globals.tryParseDouble(line2Copy.substring(TLE2Index.Eccen, TLE2Index.Eccen + 7)) / 10000000.0;
        tleData.argPerigreeDeg = Globals.tryParseDouble(line2Copy.substring(TLE2Index.ArgPeri, TLE2Index.ArgPeri + 8));
        tleData.meanAnomalyDeg = Globals.tryParseDouble(line2Copy.substring(TLE2Index.MeanAnom, TLE2Index.MeanAnom + 8));
        tleData.revsPerDay = Globals.tryParseDouble(line2Copy.substring(TLE2Index.Revs, TLE2Index.Revs + 11));
        tleData.revAtEpoch = Globals.tryParseInt(line2Copy.substring(TLE2Index.RevEp, TLE2Index.RevEp + 5));

        //calculate epoch Julian date
        tleData.epochJulian = julianDateDY(tleData.epochDay, tleData.epochYear);

        //build international code
        tleData.internationalCode = tleData.launchYear + "-" + String.format(Locale.US, "%03d", tleData.launchNum) + tleData.launchPiece;

        return(tleData);
    }
    public static TLEDataType loadTLE(String gp)
    {
        JSONObject dataItem;

        try
        {
            dataItem = new JSONObject(gp);
            return(loadTLEs(new JSONObject[]{dataItem})[0]);
        }
        catch(Exception ex)
        {
            return(null);
        }
    }

    //Loads orbit data from TLE data
    private static OrbitDataType loadOrbit(TLEDataType tleData)
    {
        double temp;
        double a0, a1;
        double delta0, delta1;
        double rads_per_minute;		//revs per minute in radians
        OrbitDataType orbit_data = new OrbitDataType();

        //setup calculation variables
        rads_per_minute = (tleData.revsPerDay * TwoPI) / MinutesPerDay;
        a1 = Math.pow(XKE / rads_per_minute, TwoThirds);
        temp = (1.5 * CK2 * (3.0 * Square(Math.cos(Math.toRadians(tleData.inclinationDeg))) - 1.0) / Math.pow(1.0 - tleData.eccentricity * tleData.eccentricity, 1.5));
        delta1 = temp / (a1 * a1);
        a0 = a1 * (1.0 - delta1 * ((1.0 / 3.0) + delta1 * (1.0 + 134.0 / 81.0 * delta1)));
        delta0 = temp / (a0 * a0);

        //calculate orbit data
        orbit_data.meanMotion = rads_per_minute / (1.0 + delta0);
        orbit_data.semiMinorAxis = a0 / (1.0 - delta0);
        orbit_data.semiMajorAxis = orbit_data.semiMinorAxis / Math.sqrt(1.0 - (tleData.eccentricity * tleData.eccentricity));
        orbit_data.perigee = EarthRadiusKM * (orbit_data.semiMajorAxis * (1.0 - tleData.eccentricity) - AE);
        orbit_data.apogee = EarthRadiusKM * (orbit_data.semiMajorAxis * (1.0 + tleData.eccentricity) - AE);
        orbit_data.semiMajorAxisKm = ((orbit_data.perigee + orbit_data.apogee) / 2.0) + EarthRadiusKM;
        orbit_data.periodMinutes = 2.0 * Math.PI / orbit_data.meanMotion;

        //if the period is >= 225 minutes (deep space)
        if(orbit_data.periodMinutes >= 225.0)
        {
            orbit_data.predictionModel = SgpModelType.Sdp4Model;
        }
        else	//period is less than 225 minutes (relatively close)
        {
            orbit_data.predictionModel = SgpModelType.Sgp4Model;
        }

        return(orbit_data);
    }

    //Loads Norad data for use in position prediction
    @SuppressWarnings("SpellCheckingInspection")
    private static NoradDataType loadNorad(OrbitDataType orbitData, TLEDataType tleData)
    {
        double pinvsq;
        double psisq;
        double c2;
        double a3ovk2;
        double theta4;
        double x1m5th;
        double xhdot1;
        double temp1, temp2, temp3;
        NoradDataType noradData = new NoradDataType();

        // Initialize any variables which are time-independent when
        // calculating the ECI coordinates of the satellite.
        noradData.m_satInc = Math.toRadians(tleData.inclinationDeg);
        noradData.m_satEcc = tleData.eccentricity;

        noradData.m_cosio = Math.cos(noradData.m_satInc);
        noradData.m_theta2 = noradData.m_cosio * noradData.m_cosio;
        noradData.m_x3thm1 = 3.0 * noradData.m_theta2 - 1.0;
        noradData.m_eosq = noradData.m_satEcc * noradData.m_satEcc;
        noradData.m_betao2 = 1.0 - noradData.m_eosq;
        noradData.m_betao = Math.sqrt(noradData.m_betao2);

        //the "recovered" semi-minor axis and mean motion.
        noradData.m_aodp = orbitData.semiMinorAxis;
        noradData.mean_motion = orbitData.meanMotion;

        noradData.m_s4 = S;
        noradData.m_qoms24 = QOMS2T;

        //for perigee below 156 km, the values of S and QOMS2T are altered.
        if(orbitData.perigee < 156.0)
        {
            noradData.m_s4 = orbitData.perigee - 78.0;

            if(orbitData.perigee <= 98.0)
            {
                noradData.m_s4 = 20.0;
            }

            noradData.m_qoms24 = Math.pow((120.0 - noradData.m_s4) * AE / EarthRadiusKM, 4.0);
            noradData.m_s4 = noradData.m_s4 / EarthRadiusKM + AE;
        }

        pinvsq = 1.0 / (noradData.m_aodp * noradData.m_aodp * noradData.m_betao2 * noradData.m_betao2);

        noradData.m_tsi = 1.0 / (noradData.m_aodp - noradData.m_s4);
        noradData.m_eta = noradData.m_aodp * noradData.m_satEcc * noradData.m_tsi;
        noradData.m_etasq = noradData.m_eta * noradData.m_eta;
        noradData.m_eeta = noradData.m_satEcc * noradData.m_eta;

        psisq = Math.abs(1.0 - noradData.m_etasq);

        noradData.m_coef = noradData.m_qoms24 * Math.pow(noradData.m_tsi,4.0);
        noradData.m_coef1 = noradData.m_coef / Math.pow(psisq,3.5);

        c2 = noradData.m_coef1 * noradData.mean_motion * (noradData.m_aodp * (1.0 + 1.5 * noradData.m_etasq + noradData.m_eeta * (4.0 + noradData.m_etasq)) + 0.75 * CK2 * noradData.m_tsi / psisq * noradData.m_x3thm1 * (8.0 + 3.0 * noradData.m_etasq * (8.0 + noradData.m_etasq)));

        noradData.m_c1 = (tleData.drag / AE) * c2;
        noradData.m_sinio = Math.sin(noradData.m_satInc);

        a3ovk2 = -XJ3 / CK2 * Math.pow(AE,3.0);

        noradData.m_c3 = noradData.m_coef * noradData.m_tsi * a3ovk2 * noradData.mean_motion * AE * noradData.m_sinio / noradData.m_satEcc;
        noradData.m_x1mth2 = 1.0 - noradData.m_theta2;
        noradData.m_c4 = 2.0 * noradData.mean_motion * noradData.m_coef1 * noradData.m_aodp * noradData.m_betao2 * (noradData.m_eta * (2.0 + 0.5 * noradData.m_etasq) + noradData.m_satEcc * (0.5 + 2.0 * noradData.m_etasq) - 2.0 * CK2 * noradData.m_tsi / (noradData.m_aodp * psisq) * (-3.0 * noradData.m_x3thm1 * (1.0 - 2.0 * noradData.m_eeta + noradData.m_etasq * (1.5 - 0.5 * noradData.m_eeta)) + 0.75 * noradData.m_x1mth2 * (2.0 * noradData.m_etasq - noradData.m_eeta * (1.0 + noradData.m_etasq)) * Math.cos(2.0 * Math.toRadians(tleData.argPerigreeDeg))));

        theta4 = noradData.m_theta2 * noradData.m_theta2;
        temp1 = 3.0 * CK2 * pinvsq * noradData.mean_motion;
        temp2 = temp1 * CK2 * pinvsq;
        temp3 = 1.25 * CK4 * pinvsq * pinvsq * noradData.mean_motion;

        noradData.m_xmdot = noradData.mean_motion + 0.5 * temp1 * noradData.m_betao * noradData.m_x3thm1 + 0.0625 * temp2 * noradData.m_betao * (13.0 - 78.0 * noradData.m_theta2 + 137.0 * theta4);

        x1m5th = 1.0 - 5.0 * noradData.m_theta2;

        noradData.m_omgdot = -0.5 * temp1 * x1m5th + 0.0625 * temp2 * (7.0 - 114.0 * noradData.m_theta2 + 395.0 * theta4) + temp3 * (3.0 - 36.0 * noradData.m_theta2 + 49.0 * theta4);

        xhdot1 = -temp1 * noradData.m_cosio;

        noradData.m_xnodot = xhdot1 + (0.5 * temp2 * (4.0 - 19.0 * noradData.m_theta2) + 2.0 * temp3 * (3.0 - 7.0 * noradData.m_theta2)) * noradData.m_cosio;
        noradData.m_xnodcf = 3.5 * noradData.m_betao2 * xhdot1 * noradData.m_c1;
        noradData.m_t2cof = 1.5 * noradData.m_c1;
        noradData.m_xlcof = 0.125 * a3ovk2 * noradData.m_sinio * (3.0 + 5.0 * noradData.m_cosio) / (1.0 + noradData.m_cosio);
        noradData.m_aycof = 0.25 * a3ovk2 * noradData.m_sinio;
        noradData.m_x7thm1 = 7.0 * noradData.m_theta2 - 1.0;

        switch(orbitData.predictionModel)
        {
            case SgpModelType.Sgp4Model:
                noradData.m_c5 = 2.0 * noradData.m_coef1 * noradData.m_aodp * noradData.m_betao2 * (1.0 + 2.75 * (noradData.m_etasq + noradData.m_eeta) + noradData.m_eeta * noradData.m_etasq);
                noradData.m_omgcof = (tleData.drag / AE) * noradData.m_c3 * Math.cos(Math.toRadians(orbitData.perigee));
                noradData.m_xmcof = -TwoThirds * noradData.m_coef * tleData.drag / noradData.m_eeta;
                noradData.m_delmo = Math.pow(1.0 + noradData.m_eta * Math.cos(Math.toRadians(tleData.meanAnomalyDeg)), 3.0);
                noradData.m_sinmo = Math.sin(Math.toRadians(tleData.meanAnomalyDeg));
                break;

            case SgpModelType.Sdp4Model:
                noradData.m_sing = Math.sin(Math.toRadians(tleData.argPerigreeDeg));
                noradData.m_cosg = Math.cos(Math.toRadians(tleData.argPerigreeDeg));

                noradData.dp_zmos = noradData.dp_se2 = noradData.dp_se3 = noradData.dp_si2 = noradData.dp_si3 = noradData.dp_sl2 = noradData.dp_sl3 = noradData.dp_sl4 =
                noradData.dp_sghs = noradData.dp_sgh2 = noradData.dp_sgh3 = noradData.dp_sgh4 = noradData.dp_sh2 = noradData.dp_sh3 = noradData.dp_zmol = noradData.dp_ee2 =
                noradData.dp_e3 = noradData.dp_xi2 = noradData.dp_xi3 = noradData.dp_xl2 = noradData.dp_xl3 = noradData.dp_xl4 = noradData.dp_xgh2 = noradData.dp_xgh3 = noradData.dp_xgh4 =
                noradData.dp_xh2 = noradData.dp_xh3 = noradData.dp_xqncl = noradData.dp_thgr = noradData.dp_omegaq = noradData.dp_sse = noradData.dp_ssi = noradData.dp_ssl = noradData.dp_ssh =
                noradData.dp_ssg = noradData.dp_d2201 = noradData.dp_d2211 = noradData.dp_d3210 = noradData.dp_d3222 = noradData.dp_d4410 = noradData.dp_d4422 = noradData.dp_d5220 =
                noradData.dp_d5232 = noradData.dp_d5421 = noradData.dp_d5433 = noradData.dp_xlamo = noradData.dp_del1 = noradData.dp_del2 = noradData.dp_del3 = noradData.dp_fasx2 =
                noradData.dp_fasx4 = noradData.dp_fasx6 = noradData.dp_xfact = noradData.dp_xli = noradData.dp_xni = noradData.dp_atime = noradData.dp_stepn =
                noradData.dp_step2 = 0.0;
                noradData.dp_stepp = 720.0;

                noradData.gp_reso = noradData.gp_sync = false;
                break;
        }

        return(noradData);
    }

    //Loads a satellite object
    public static SatelliteObjectType loadSatellite(String name, TLEDataType tle)
    {
        SatelliteObjectType satObj = new SatelliteObjectType();
        satObj.name = name;
        satObj.tle = tle;
        satObj.orbit = loadOrbit(satObj.tle);
        satObj.norad = loadNorad(satObj.orbit, satObj.tle);
        return(satObj);
    }
    public static SatelliteObjectType loadSatellite(String name, String tleLine1, String tleLine2)
    {
        return(loadSatellite(name, loadTLE(tleLine1, tleLine2)));
    }
    public static SatelliteObjectType loadSatellite(Database.DatabaseSatellite currentSat)
    {
        return(loadSatellite(currentSat.getName(), currentSat.tle));
    }

    //Loads satellite objects
    @SuppressWarnings("SpellCheckingInspection")
    public static SatelliteObjectType[] loadSatellites(JSONObject[] data)
    {
        int index;
        SatelliteObjectType[] satObjects;
        TLEDataType[] tles = loadTLEs(data);
        String[] names = loadNames(data);

        //go through each TLE
        satObjects = new SatelliteObjectType[tles.length];
        for(index = 0; index < tles.length; index++)
        {
            //set current satellite
            satObjects[index] = (tles[index] != null ? loadSatellite(names[index], tles[index]) : null);
        }

        //return satellites
        return(satObjects.length > 0 ? satObjects : null);
    }

    //Loads the sun
    private static SatelliteObjectType loadNonSatellite(Database.DatabaseSatellite currentSat)
    {
        byte currentType = currentSat.orbitalType;
        SatelliteObjectType nonSatObj = new SatelliteObjectType();

        nonSatObj.name = currentSat.getName();
        nonSatObj.tle.satelliteNum = currentSat.noradId;
        if(currentType == Database.OrbitalType.Sun || currentType == Database.OrbitalType.Star || currentType == Database.OrbitalType.Constellation)
        {
            nonSatObj.planetData = new PlanetDataType(currentSat.noradId, 0, 0, 0, currentSat.rightAscensionHours, currentSat.declinationDegs, currentSat.distanceLightYears * KmPerLightYear);
        }

        return(nonSatObj);
    }

    //Gets orbital
    public static SatelliteObjectType loadOrbital(Database.DatabaseSatellite currentSat)
    {
        //if satellite is not set
        if(currentSat == null)
        {
            return(null);
        }

        //handle based on orbital type
        switch(currentSat.orbitalType)
        {
            case Database.OrbitalType.Sun:
            case Database.OrbitalType.Star:
            case Database.OrbitalType.Planet:
            case Database.OrbitalType.Constellation:
                return(Calculations.loadNonSatellite(currentSat));

            default:
                return(Calculations.loadSatellite(currentSat));
        }
    }

    //Sets the eci position for the given data
    @SuppressWarnings("SpellCheckingInspection")
    private static EciDataType finalPosition(EciDataType eciData, NoradDataType noradData, double inclination, double omega, double e, double a, double xl, double xNode, double epochJulian, double daysAfterEpoch)
    {
        int i;
        boolean fDone = false;
        double axn;
        double xll;
        double aynl;
        double xlt;
        double ayn;
        double capu;
        double epw;
        double elsq;
        double pl;
        double r;
        double rk, uk;
        double xmx, xmy;
        double beta, betal;
        double xnodek, xinck;
        double ux, uy, uz;
        double temp, temp1, temp2, temp3, temp4, temp5, temp6;
        double u, sinepw, cosepw, ecose, esine, cosu, sinu, cos2u, sin2u, sinuk, cosuk, sinik, cosik, sinnok, cosnok;

        //if error in satellite data
        if((e * e) > 1.0)
        {
            return(new EciDataType());
        }

        beta = Math.sqrt(1.0 - e * e);

        //long period periodics
        axn  = e * Math.cos(omega);
        temp = 1.0 / (a * beta * beta);
        xll  = temp * noradData.m_xlcof * axn;
        aynl = temp * noradData.m_aycof;
        xlt  = xl + xll;
        ayn  = e * Math.sin(omega) + aynl;

        //solve Kepler's Equation
        capu = fmod2P(xlt - xNode);
        temp2 = capu;
        temp3 = temp4 = temp5 = temp6 = 0.0;
        sinepw = 0.0;
        cosepw = 0.0;

        for(i = 1; (i <= 10) && !fDone; i++)
        {
            sinepw = Math.sin(temp2);
            cosepw = Math.cos(temp2);
            temp3 = axn * sinepw;
            temp4 = ayn * cosepw;
            temp5 = axn * cosepw;
            temp6 = ayn * sinepw;

            epw = (capu - temp4 + temp3 - temp2) / (1.0 - temp5 - temp6) + temp2;

            if (Math.abs(epw - temp2) <= E6A)
                fDone = true;
            else
                temp2 = epw;
        }

        //short period preliminary quantities
        ecose = temp5 + temp6;
        esine = temp3 - temp4;
        elsq = axn * axn + ayn * ayn;
        temp = 1.0 - elsq;
        pl = a * temp;
        r = a * (1.0 - ecose);
        temp1 = 1.0 / r;
        temp2 = a * temp1;
        betal = Math.sqrt(temp);
        temp3 = 1.0 / (1.0 + betal);
        cosu = temp2 * (cosepw - axn + ayn * esine * temp3);
        sinu = temp2 * (sinepw - ayn - axn * esine * temp3);
        u = arcTan(sinu, cosu);
        sin2u = 2.0 * sinu * cosu;
        cos2u = 2.0 * cosu * cosu - 1.0;

        temp  = 1.0 / pl;
        temp1 = CK2 * temp;
        temp2 = temp1 * temp;

        //update for short periodics
        rk = r * (1.0 - 1.5 * temp2 * betal * noradData.m_x3thm1) + 0.5 * temp1 * noradData.m_x1mth2 * cos2u;
        uk = u - 0.25 * temp2 * noradData.m_x7thm1 * sin2u;
        xnodek = xNode + 1.5 * temp2 * noradData.m_cosio * sin2u;
        xinck  = inclination + 1.5 * temp2 * noradData.m_cosio * noradData.m_sinio * cos2u;

        //orientation vectors
        sinuk  = Math.sin(uk);
        cosuk  = Math.cos(uk);
        sinik  = Math.sin(xinck);
        cosik  = Math.cos(xinck);
        sinnok = Math.sin(xnodek);
        cosnok = Math.cos(xnodek);
        xmx = -sinnok * cosik;
        xmy = cosnok * cosik;
        ux  = xmx * sinuk + cosnok * cosuk;
        uy  = xmy * sinuk + sinnok * cosuk;
        uz  = sinik * sinuk;

        //position (in km)
        eciData.positionX = rk * ux * (EarthRadiusKM / AE);
        eciData.positionY = rk * uy * (EarthRadiusKM / AE);
        eciData.positionZ = rk * uz * (EarthRadiusKM / AE);

        //time
        eciData.julianDate = epochJulian + MinutesToDays(daysAfterEpoch);

        return(eciData);
    }

    //SDP4 periodics
    @SuppressWarnings("SpellCheckingInspection")
    public static class Sdp4PeriodicsDataType
    {
        NoradDataType norad_data;
        double e; double xincc; double omgadf; double xnode; double xmam;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static Sdp4PeriodicsDataType sdp4Periodics(NoradDataType noradData, double e, double xincc, double omgadf, double xnode, double xmam)
    {
        double zm, zf;
        double f2, f3;
        double pgh, ph;
        double xls, dls;
        double ses, sis, sls;
        double sel, sil, sll;
        double alfdp, betdp, dalf, dbet;
        double sinis, cosis, sinzf, sinok, cosok;
        double sghs;
        double shs;
        double sh1;
        double pe;
        double pinc;
        double pl;
        double sghl;
        Sdp4PeriodicsDataType sdp4PeriodicsData = new Sdp4PeriodicsDataType();

        noradData._em = e;
        noradData.xinc = xincc;
        noradData.omgasm = omgadf;
        noradData.xnodes = xnode;
        noradData.xll = xmam;

        //lunar-solar periodics
        sinis = Math.sin(noradData.xinc);
        cosis = Math.cos(noradData.xinc);

        zm = noradData.dp_zmos + ZNS * noradData.timeSince;
        zf = zm + 2.0 * ZES * Math.sin(zm);
        sinzf = Math.sin(zf);
        f2 = 0.5 * sinzf * sinzf - 0.25;
        f3 = -0.5 * sinzf * Math.cos(zf);
        ses = noradData.dp_se2 * f2 + noradData.dp_se3 * f3;
        sis = noradData.dp_si2 * f2 + noradData.dp_si3 * f3;
        sls = noradData.dp_sl2 * f2 + noradData.dp_sl3 * f3 + noradData.dp_sl4 * sinzf;

        sghs = noradData.dp_sgh2 * f2 + noradData.dp_sgh3 * f3 + noradData.dp_sgh4 * sinzf;
        shs = noradData.dp_sh2 * f2 + noradData.dp_sh3 * f3;
        zm = noradData.dp_zmol + ZNL * noradData.timeSince;
        zf = zm + 2.0 * ZEL * Math.sin(zm);
        sinzf = Math.sin(zf);
        f2 = 0.5 * sinzf * sinzf - 0.25;
        f3 = -0.5 * sinzf * Math.cos(zf);

        sel = noradData.dp_ee2 * f2 + noradData.dp_e3 * f3;
        sil = noradData.dp_xi2 * f2 + noradData.dp_xi3 * f3;
        sll = noradData.dp_xl2 * f2 + noradData.dp_xl3 * f3 + noradData.dp_xl4 * sinzf;

        sghl = noradData.dp_xgh2 * f2 + noradData.dp_xgh3 * f3 + noradData.dp_xgh4 * sinzf;
        sh1 = noradData.dp_xh2 * f2 + noradData.dp_xh3 * f3;
        pe = ses + sel;
        pinc = sis + sil;
        pl = sls + sll;

        pgh = sghs + sghl;
        ph = shs + sh1;
        noradData.xinc = noradData.xinc + pinc;
        noradData._em = noradData._em + pe;

        if(noradData.dp_xqncl >= 0.2)
        {
            //apply periodics directly
            ph = ph / noradData.siniq;
            pgh = pgh - noradData.cosiq * ph;
            noradData.omgasm = noradData.omgasm + pgh;
            noradData.xnodes = noradData.xnodes + ph;
            noradData.xll = noradData.xll + pl;
        }
        else
        {
            //apply periodics with Lyddane modification
            sinok = Math.sin(noradData.xnodes);
            cosok = Math.cos(noradData.xnodes);
            alfdp = sinis * sinok;
            betdp = sinis * cosok;
            dalf =  ph * cosok + pinc * cosis * sinok;
            dbet = -ph * sinok + pinc * cosis * cosok;

            alfdp = alfdp + dalf;
            betdp = betdp + dbet;

            xls = noradData.xll + noradData.omgasm + cosis * noradData.xnodes;
            dls = pl + pgh - pinc * noradData.xnodes * sinis;

            xls = xls + dls;
            noradData.xnodes = arcTan(alfdp, betdp);
            noradData.xll = noradData.xll + pl;
            noradData.omgasm = xls - noradData.xll - Math.cos(noradData.xinc) * noradData.xnodes;
        }

        e = noradData._em;
        xincc = noradData.xinc;
        omgadf = noradData.omgasm;
        xnode = noradData.xnodes;
        xmam = noradData.xll;

        sdp4PeriodicsData.norad_data = noradData;
        sdp4PeriodicsData.e = e;
        sdp4PeriodicsData.xincc = xincc;
        sdp4PeriodicsData.omgadf = omgadf;
        sdp4PeriodicsData.xnode = xnode;
        sdp4PeriodicsData.xmam = xmam;
        return(sdp4PeriodicsData);
    }

    //SDP4 calculate dot terms
    @SuppressWarnings("SpellCheckingInspection")
    public static class Sdp4CalcdottermsDataType
    {
        double pxndot; double pxnddt; double pxldot;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static Sdp4CalcdottermsDataType sdp4CalcDotTerms(NoradDataType noradData)
    {
        double pxndot, pxnddt, pxldot;
        double xomi, x2omi, x2li;
        Sdp4CalcdottermsDataType sdp4CalcDotTermsData = new Sdp4CalcdottermsDataType();

        //dot terms calculated
        if(noradData.gp_sync)
        {
            pxndot = noradData.dp_del1 * Math.sin(noradData.dp_xli -
            noradData.dp_fasx2) + noradData.dp_del2 * Math.sin(2.0 *
            (noradData.dp_xli - noradData.dp_fasx4)) +
            noradData.dp_del3 * Math.sin(3.0 * (noradData.dp_xli - noradData.dp_fasx6));

            pxnddt = noradData.dp_del1 * Math.cos(noradData.dp_xli -
            noradData.dp_fasx2) + 2.0 * noradData.dp_del2 *
            Math.cos(2.0 * (noradData.dp_xli - noradData.dp_fasx4)) +
            3.0 * noradData.dp_del3 * Math.cos(3.0 * (noradData.dp_xli - noradData.dp_fasx6));
        }
        else
        {
            xomi = noradData.dp_omegaq + noradData.omgdt * noradData.dp_atime;
            x2omi = xomi + xomi;
            x2li = noradData.dp_xli + noradData.dp_xli;

            pxndot = noradData.dp_d2201 * Math.sin(x2omi + noradData.dp_xli - G22) +
            noradData.dp_d2211 * Math.sin(noradData.dp_xli - G22) +
            noradData.dp_d3210 * Math.sin(xomi + noradData.dp_xli - G32) +
            noradData.dp_d3222 * Math.sin(-xomi + noradData.dp_xli - G32) +
            noradData.dp_d4410 * Math.sin(x2omi + x2li - G44) +
            noradData.dp_d4422 * Math.sin(x2li - G44) +
            noradData.dp_d5220 * Math.sin(xomi + noradData.dp_xli - G52) +
            noradData.dp_d5232 * Math.sin(-xomi + noradData.dp_xli - G52) +
            noradData.dp_d5421 * Math.sin(xomi + x2li - G54) +
            noradData.dp_d5433 * Math.sin(-xomi + x2li - G54);

            pxnddt = noradData.dp_d2201 * Math.cos(x2omi + noradData.dp_xli - G22) +
            noradData.dp_d2211 * Math.cos(noradData.dp_xli - G22) +
            noradData.dp_d3210 * Math.cos(xomi + noradData.dp_xli - G32) +
            noradData.dp_d3222 * Math.cos(-xomi + noradData.dp_xli - G32) +
            noradData.dp_d5220 * Math.cos(xomi + noradData.dp_xli - G52) +
            noradData.dp_d5232 * Math.cos(-xomi + noradData.dp_xli - G52) +
            2.0 * (noradData.dp_d4410 * Math.cos(x2omi + x2li - G44) +
            noradData.dp_d4422 * Math.cos(x2li - G44) +
            noradData.dp_d5421 * Math.cos(xomi + x2li - G54) +
            noradData.dp_d5433 * Math.cos(-xomi + x2li - G54));
        }

        pxldot = noradData.dp_xni + noradData.dp_xfact;
        pxnddt = (pxnddt) * (pxldot);

        sdp4CalcDotTermsData.pxndot = pxndot;
        sdp4CalcDotTermsData.pxnddt = pxnddt;
        sdp4CalcDotTermsData.pxldot = pxldot;
        return(sdp4CalcDotTermsData);
    }

    //SDP4 calculation integrator
    @SuppressWarnings("SpellCheckingInspection")
    private static void sdp4CalcIntegrator(NoradDataType noradData, double delta)
    {
        double pxndot, pxnddt, pxldot;

        Sdp4CalcdottermsDataType sdp4_calcdotterms_data = sdp4CalcDotTerms(noradData);
        pxndot = sdp4_calcdotterms_data.pxndot;
        pxnddt = sdp4_calcdotterms_data.pxnddt;
        pxldot = sdp4_calcdotterms_data.pxldot;

        // *pxldot //
        noradData.dp_xli = noradData.dp_xli + (pxldot) * delta + (pxndot) * noradData.dp_step2;
        noradData.dp_xni = noradData.dp_xni + (pxndot) * delta + (pxnddt) * noradData.dp_step2;
        noradData.dp_atime = noradData.dp_atime + delta;
    }

    //SDP4 initialization
    @SuppressWarnings("SpellCheckingInspection")
    private static void sdp4Init(NoradDataType noradData, TLEDataType tleData)
    {
        int pass;
        double day;
        double eoc;
        double sini2;
        double eq, aqnv;
        double ainv2, xno2;
        double temp, temp1;
        double xmao, xpidot, sinq, cosq;
        double s1, s2, s3, s4, s5, s6, s7;
        double z1, z2, z3, z11, z12, z13, z21, z22, z23, z31, z32, z33;
        double zcosg, zsing, zcosi, zsini, zcosh, zsinh, cc, zn, ze, xnoi;
        double f220, f221, f311, f321, f322, f330, f441, f442, f522, f523, f542, f543;
        double g200, g201, g211, g300, g310, g322, g410, g422, g520, g521, g532, g533;
        double a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, x1, x2, x3, x4, x5, x6, x7, x8;
        double se  = 0.0;
        double si = 0.0;
        double sl = 0.0;
        double sgh = 0.0;
        double sh = 0.0;
        double bfact = 0.0;

        noradData.eqsq = noradData.m_eosq;
        noradData.siniq = noradData.m_sinio;
        noradData.cosiq = noradData.m_cosio;
        noradData.rteqsq = noradData.m_betao;
        noradData.ao = noradData.m_aodp;
        noradData.cosq2 = noradData.m_theta2;
        noradData.sinomo = noradData.m_sing;
        noradData.cosomo = noradData.m_cosg;
        noradData.bsq = noradData.m_betao2;
        noradData.xlldot = noradData.m_xmdot;
        noradData.omgdt = noradData.m_omgdot;
        noradData.xnodot = noradData.m_xnodot;

        //deep space initialization
        noradData.dp_thgr = julianDateToGMST(tleData.epochJulian);

        eq = tleData.eccentricity;
        aqnv = 1.0 / noradData.ao;

        noradData.dp_xqncl = Math.toRadians(tleData.inclinationDeg);

        xmao = Math.toRadians(tleData.meanAnomalyDeg);
        xpidot = noradData.omgdt + noradData.xnodot;
        sinq = Math.sin(Math.toRadians(tleData.rightAscnAscNodeDeg));
        cosq = Math.cos(Math.toRadians(tleData.rightAscnAscNodeDeg));

        noradData.dp_omegaq = Math.toRadians(tleData.argPerigreeDeg);

        //initialize lunar solar terms
        day = tleData.epochJulian - JanFirst1900JD;
        noradData.dpi_day = day;
        noradData.dpi_xnodce = 4.5236020 - 9.2422029E-4 * day;
        noradData.dpi_stem = Math.sin(noradData.dpi_xnodce);
        noradData.dpi_ctem = Math.cos(noradData.dpi_xnodce);
        noradData.dpi_zcosil = 0.91375164 - 0.03568096 * noradData.dpi_ctem;
        noradData.dpi_zsinil = Math.sqrt(1.0 - noradData.dpi_zcosil * noradData.dpi_zcosil);
        noradData.dpi_zsinhl = 0.089683511 * noradData.dpi_stem / noradData.dpi_zsinil;
        noradData.dpi_zcoshl = Math.sqrt(1.0 - noradData.dpi_zsinhl * noradData.dpi_zsinhl);
        noradData.dpi_c = 4.7199672 + 0.22997150 * day;
        noradData.dpi_gam = 5.8351514 + 0.0019443680 * day;
        noradData.dp_zmol = fmod2P(noradData.dpi_c - noradData.dpi_gam);
        noradData.dpi_zx = 0.39785416 * noradData.dpi_stem / noradData.dpi_zsinil;
        noradData.dpi_zy = noradData.dpi_zcoshl * noradData.dpi_ctem + 0.91744867 * noradData.dpi_zsinhl * noradData.dpi_stem;
        noradData.dpi_zx = arcTan(noradData.dpi_zx, noradData.dpi_zy) + noradData.dpi_gam - noradData.dpi_xnodce;
        noradData.dpi_zcosgl = Math.cos(noradData.dpi_zx);
        noradData.dpi_zsingl = Math.sin(noradData.dpi_zx);
        noradData.dp_zmos = 6.2565837 + 0.017201977 * day;
        noradData.dp_zmos = fmod2P(noradData.dp_zmos);

        zcosg = ZCosGS;
        zsing = ZSinGS;
        zcosi = ZCosIS;
        zsini = ZSinIS;
        zcosh = cosq;
        zsinh = sinq;
        cc  = C1SS;
        zn  = ZNS;
        ze  = ZES;
        xnoi = 1.0 / noradData.mean_motion;

        //apply the solar and lunar terms on the first pass, then re-apply the solar terms again on the second pass
        for(pass = 1; pass <= 2; pass++)
        {
            //do solar terms
            a1 =  zcosg * zcosh + zsing * zcosi * zsinh;
            a3 = -zsing * zcosh + zcosg * zcosi * zsinh;
            a7 = -zcosg * zsinh + zsing * zcosi * zcosh;
            a8 = zsing * zsini;
            a9 = zsing * zsinh + zcosg * zcosi * zcosh;
            a10 = zcosg * zsini;
            a2 = noradData.cosiq * a7 + noradData.siniq * a8;
            a4 = noradData.cosiq * a9 + noradData.siniq * a10;
            a5 = -noradData.siniq * a7 + noradData.cosiq * a8;
            a6 = -noradData.siniq * a9 + noradData.cosiq * a10;
            x1 = a1 * noradData.cosomo + a2 * noradData.sinomo;
            x2 = a3 * noradData.cosomo + a4 * noradData.sinomo;
            x3 = -a1 * noradData.sinomo + a2 * noradData.cosomo;
            x4 = -a3 * noradData.sinomo + a4 * noradData.cosomo;
            x5 = a5 * noradData.sinomo;
            x6 = a6 * noradData.sinomo;
            x7 = a5 * noradData.cosomo;
            x8 = a6 * noradData.cosomo;
            z31 = 12.0 * x1 * x1 - 3.0 * x3 * x3;
            z32 = 24.0 * x1 * x2 - 6.0 * x3 * x4;
            z33 = 12.0 * x2 * x2 - 3.0 * x4 * x4;
            z1 = 3.0 * (a1 * a1 + a2 * a2) + z31 * noradData.eqsq;
            z2 = 6.0 * (a1 * a3 + a2 * a4) + z32 * noradData.eqsq;
            z3 = 3.0 * (a3 * a3 + a4 * a4) + z33 * noradData.eqsq;
            z11 = -6.0 * a1 * a5 + noradData.eqsq * (-24.0 * x1 * x7 - 6.0 * x3 * x5);
            z12 = -6.0 * (a1 * a6 + a3 * a5) + noradData.eqsq * (-24.0 * (x2 * x7 + x1 * x8) - 6.0 * (x3 * x6 + x4 * x5));
            z13 = -6.0 * a3 * a6 + noradData.eqsq * (-24.0 * x2 * x8 - 6.0 * x4 * x6);
            z21 = 6.0 * a2 * a5 + noradData.eqsq * (24.0 * x1 * x5 - 6.0 * x3 * x7);
            z22 = 6.0*(a4 * a5 + a2 * a6) + noradData.eqsq * (24.0 * (x2 * x5 + x1 * x6) - 6.0 * (x4 * x7 + x3 * x8));
            z23 = 6.0 * a4 * a6 + noradData.eqsq*(24.0 * x2 * x6 - 6.0 * x4 * x8);
            z1 = z1 + z1 + noradData.bsq * z31;
            z2 = z2 + z2 + noradData.bsq * z32;
            z3 = z3 + z3 + noradData.bsq * z33;
            s3 = cc * xnoi;
            s2 = -0.5 * s3 / noradData.rteqsq;
            s4 = s3 * noradData.rteqsq;
            s1 = -15.0 * eq * s4;
            s5 = x1 * x3 + x2 * x4;
            s6 = x2 * x3 + x1 * x4;
            s7 = x2 * x4 - x1 * x3;
            se = s1 * zn * s5;
            si = s2 * zn * (z11 + z13);
            sl = -zn * s3 * (z1 + z3 - 14.0 - 6.0 * noradData.eqsq);
            sgh = s4 * zn * (z31 + z33 - 6.0);
            sh = -zn * s2 * (z21 + z23);

            if(noradData.dp_xqncl < 5.2359877E-2)
                sh = 0.0;

            noradData.dp_ee2 = 2.0 * s1 * s6;
            noradData.dp_e3 = 2.0 * s1 * s7;
            noradData.dp_xi2 = 2.0 * s2 * z12;
            noradData.dp_xi3 = 2.0 * s2 * (z13 - z11);
            noradData.dp_xl2 = -2.0 * s3 * z2;
            noradData.dp_xl3 = -2.0 * s3 * (z3 - z1);
            noradData.dp_xl4 = -2.0 * s3 * (-21.0 - 9.0 * noradData.eqsq) * ze;
            noradData.dp_xgh2 = 2.0 * s4 * z32;
            noradData.dp_xgh3 = 2.0 * s4 * (z33 - z31);
            noradData.dp_xgh4 = -18.0 * s4 * ze;
            noradData.dp_xh2 = -2.0 * s2 * z22;
            noradData.dp_xh3 = -2.0 * s2 * (z23 - z21);

            if(pass == 1)
            {
                //do lunar terms
                noradData.dp_sse = se;
                noradData.dp_ssi = si;
                noradData.dp_ssl = sl;
                noradData.dp_ssh = sh / noradData.siniq;
                noradData.dp_ssg = sgh - noradData.cosiq * noradData.dp_ssh;
                noradData.dp_se2 = noradData.dp_ee2;
                noradData.dp_si2 = noradData.dp_xi2;
                noradData.dp_sl2 = noradData.dp_xl2;
                noradData.dp_sgh2 = noradData.dp_xgh2;
                noradData.dp_sh2 = noradData.dp_xh2;
                noradData.dp_se3 = noradData.dp_e3;
                noradData.dp_si3 = noradData.dp_xi3;
                noradData.dp_sl3 = noradData.dp_xl3;
                noradData.dp_sgh3 = noradData.dp_xgh3;
                noradData.dp_sh3 = noradData.dp_xh3;
                noradData.dp_sl4 = noradData.dp_xl4;
                noradData.dp_sgh4 = noradData.dp_xgh4;
                zcosg = noradData.dpi_zcosgl;
                zsing = noradData.dpi_zsingl;
                zcosi = noradData.dpi_zcosil;
                zsini = noradData.dpi_zsinil;
                zcosh = noradData.dpi_zcoshl * cosq + noradData.dpi_zsinhl * sinq;
                zsinh = sinq * noradData.dpi_zcoshl - cosq * noradData.dpi_zsinhl;
                zn = ZNL;
                cc = C1L;
                ze = ZEL;
            }
        }

        noradData.dp_sse = noradData.dp_sse + se;
        noradData.dp_ssi = noradData.dp_ssi + si;
        noradData.dp_ssl = noradData.dp_ssl + sl;
        noradData.dp_ssg = noradData.dp_ssg + sgh - noradData.cosiq / noradData.siniq * sh;
        noradData.dp_ssh = noradData.dp_ssh + sh / noradData.siniq;

        // Geopotential resonance initialization
        noradData.gp_reso = false;
        noradData.gp_sync = false;

        // Determine if orbit is 24- or 12-hour resonant.
        // Mean motion is given in radians per minute.
        if(noradData.mean_motion > 0.0034906585 && noradData.mean_motion < 0.0052359877)
        {
            // Orbit is within the Clarke Belt (period is 24-hour resonant).
            // Synchronous resonance terms initialization
            noradData.gp_reso = true;
            noradData.gp_sync = true;

            g200 = 1.0 + noradData.eqsq * (-2.5 + 0.8125 * noradData.eqsq);
            g310 = 1.0 + 2.0 * noradData.eqsq;
            g300 = 1.0 + noradData.eqsq * (-6.0 + 6.60937 * noradData.eqsq);
            f220 = 0.75 * (1.0 + noradData.cosiq) * (1.0 + noradData.cosiq);
            f311 = 0.9375 * noradData.siniq * noradData.siniq * (1.0 + 3 * noradData.cosiq) - 0.75 * (1.0 + noradData.cosiq);
            f330 = 1.0 + noradData.cosiq;
            f330 = 1.875 * f330 * f330 * f330;
            noradData.dp_del1 = 3.0 * noradData.mean_motion * noradData.mean_motion * aqnv * aqnv;
            noradData.dp_del2 = 2.0 * noradData.dp_del1 * f220 * g200 * Q22;
            noradData.dp_del3 = 3.0 * noradData.dp_del1 * f330 * g300 * Q33 * aqnv;
            noradData.dp_del1 = noradData.dp_del1 * f311 * g310 * Q31 * aqnv;
            noradData.dp_fasx2 = 0.13130908;
            noradData.dp_fasx4 = 2.8843198;
            noradData.dp_fasx6 = 0.37448087;
            noradData.dp_xlamo = xmao + Math.toRadians(tleData.rightAscnAscNodeDeg) + Math.toRadians(tleData.argPerigreeDeg) - noradData.dp_thgr;
            bfact = noradData.xlldot + xpidot - THDT;
            bfact = bfact + noradData.dp_ssl + noradData.dp_ssg + noradData.dp_ssh;
        }
        else if(noradData.mean_motion >= 8.26E-3 && noradData.mean_motion <= 9.24E-3 && eq >= 0.5)
        {
            // Period is 12-hour resonant
            noradData.gp_reso = true;

            eoc  = eq * noradData.eqsq;
            g201 = -0.306 - (eq - 0.64) * 0.440;

            if(eq <= 0.65)
            {
                g211 = 3.616 - 13.247 * eq + 16.290 * noradData.eqsq;
                g310 = -19.302 + 117.390 * eq - 228.419 * noradData.eqsq + 156.591 * eoc;
                g322 = -18.9068 + 109.7927 * eq - 214.6334 * noradData.eqsq + 146.5816 * eoc;
                g410 = -41.122 + 242.694 * eq - 471.094 * noradData.eqsq + 313.953 * eoc;
                g422 = -146.407 + 841.880 * eq - 1629.014 * noradData.eqsq + 1083.435 * eoc;
                g520 = -532.114 + 3017.977 * eq - 5740.0 * noradData.eqsq + 3708.276 * eoc;
            }
            else
            {
                g211 = -72.099 + 331.819 * eq - 508.738 * noradData.eqsq + 266.724 * eoc;
                g310 = -346.844 + 1582.851 * eq - 2415.925 * noradData.eqsq + 1246.113 * eoc;
                g322 = -342.585 + 1554.908 * eq - 2366.899 * noradData.eqsq + 1215.972 * eoc;
                g410 = -1052.797 + 4758.686 * eq - 7193.992 * noradData.eqsq + 3651.957 * eoc;
                g422 = -3581.69 + 16178.11 * eq - 24462.77 * noradData.eqsq + 12422.52 * eoc;

                if (eq <= 0.715)
                    g520 = 1464.74 - 4664.75 * eq + 3763.64 * noradData.eqsq;
                else
                    g520 = -5149.66 + 29936.92 * eq - 54087.36 * noradData.eqsq + 31324.56 * eoc;
            }

            if (eq < 0.7)
            {
                g533 = -919.2277 + 4988.61 * eq - 9064.77 * noradData.eqsq + 5542.21 * eoc;
                g521 = -822.71072 + 4568.6173 * eq - 8491.4146 * noradData.eqsq + 5337.524 * eoc;
                g532 = -853.666 + 4690.25 * eq - 8624.77 * noradData.eqsq + 5341.4 * eoc;
            }
            else
            {
                g533 = -37995.78 + 161616.52 * eq - 229838.2 * noradData.eqsq + 109377.94 * eoc;
                g521 = -51752.104 + 218913.95 * eq - 309468.16 * noradData.eqsq + 146349.42 * eoc;
                g532 = -40023.88 + 170470.89 * eq - 242699.48 * noradData.eqsq + 115605.82 * eoc;
            }

            sini2 = noradData.siniq * noradData.siniq;
            f220 = 0.75*(1.0 + 2.0 * noradData.cosiq + noradData.cosq2);
            f221 = 1.5 * sini2;
            f321 = 1.875 * noradData.siniq * (1.0 - 2.0 * noradData.cosiq - 3.0 * noradData.cosq2);
            f322 = -1.875 * noradData.siniq * (1.0 + 2.0 * noradData.cosiq - 3.0 * noradData.cosq2);
            f441 = 35.0 * sini2 * f220;
            f442 = 39.3750 * sini2 * sini2;
            f522 = 9.84375 * noradData.siniq * (sini2 * (1.0 - 2.0 * noradData.cosiq - 5.0 * noradData.cosq2) + 0.33333333 * (-2.0 + 4.0 * noradData.cosiq + 6.0 * noradData.cosq2));
            f523 = noradData.siniq * (4.92187512 * sini2 * (-2.0 - 4.0 * noradData.cosiq + 10.0 * noradData.cosq2) + 6.56250012 * (1.0 + 2.0 * noradData.cosiq - 3.0 * noradData.cosq2));
            f542 = 29.53125 * noradData.siniq * (2.0 - 8.0 * noradData.cosiq + noradData.cosq2 * (-12.0 + 8.0 * noradData.cosiq + 10.0 * noradData.cosq2));
            f543 = 29.53125 * noradData.siniq * (-2.0 - 8.0 * noradData.cosiq + noradData.cosq2 * (12.0 + 8.0 * noradData.cosiq - 10.0 * noradData.cosq2));
            xno2 = noradData.mean_motion * noradData.mean_motion;
            ainv2 = aqnv * aqnv;
            temp1 = 3.0 * xno2 * ainv2;
            temp = temp1 * Root22;

            noradData.dp_d2201 = temp * f220 * g201;
            noradData.dp_d2211 = temp * f221 * g211;
            temp1 = temp1 * aqnv;
            temp = temp1 * Root32;
            noradData.dp_d3210 = temp * f321 * g310;
            noradData.dp_d3222 = temp * f322 * g322;
            temp1 = temp1 * aqnv;
            temp = 2.0 * temp1 * Root44;
            noradData.dp_d4410 = temp * f441 * g410;
            noradData.dp_d4422 = temp * f442 * g422;
            temp1 = temp1 * aqnv;
            temp  = temp1 * Root52;
            noradData.dp_d5220 = temp * f522 * g520;
            noradData.dp_d5232 = temp * f523 * g532;
            temp = 2.0 * temp1 * Root54;
            noradData.dp_d5421 = temp * f542 * g521;
            noradData.dp_d5433 = temp * f543 * g533;
            noradData.dp_xlamo = xmao + Math.toRadians(tleData.rightAscnAscNodeDeg) + Math.toRadians(tleData.rightAscnAscNodeDeg) - noradData.dp_thgr - noradData.dp_thgr;
            bfact = noradData.xlldot + noradData.xnodot + noradData.xnodot - THDT - THDT;
            bfact = bfact + noradData.dp_ssl + noradData.dp_ssh + noradData.dp_ssh;
        }

        if(noradData.gp_reso || noradData.gp_sync)
        {
            noradData.dp_xfact = bfact - noradData.mean_motion;

            //initialize integrator
            noradData.dp_xli = noradData.dp_xlamo;
            noradData.dp_xni = noradData.mean_motion;
            noradData.dp_atime = 0.0;
            noradData.dp_stepp = 720.0;
            noradData.dp_stepn = -720.0;
            noradData.dp_step2 = 259200.0;
        }

        noradData.m_eosq   = noradData.eqsq;
        noradData.m_sinio  = noradData.siniq;
        noradData.m_cosio  = noradData.cosiq;
        noradData.m_betao  = noradData.rteqsq;
        noradData.m_aodp   = noradData.ao;
        noradData.m_theta2 = noradData.cosq2;
        noradData.m_sing   = noradData.sinomo;
        noradData.m_cosg   = noradData.cosomo;
        noradData.m_betao2 = noradData.bsq;
        noradData.m_xmdot  = noradData.xlldot;
        noradData.m_omgdot = noradData.omgdt;
        noradData.m_xnodot = noradData.xnodot;
    }

    //SDP4 secular
    @SuppressWarnings("SpellCheckingInspection")
    public static class Sdp4SecularDataType
    {
        NoradDataType norad_data;
        double xmdf; double omgadf; double xnode; double emm; double xincc; double xnn; double days_after_epoch;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static Sdp4SecularDataType sdp4Secular(NoradDataType noradData, TLEDataType tleData, double xmdf, double omgadf, double xnode, double xnn, double daysAfterEpoch)
    {
        boolean fDone = false;
        double xl, temp;
        double emm;
        double xincc;
        double xnddt;
        double xndot;
        double xldot;
        double ft;
        double delt  = 0.0;
        Sdp4CalcdottermsDataType sdp4CalcDotTermsData;
        Sdp4SecularDataType sdp4SecularData = new Sdp4SecularDataType();

        noradData.xll = xmdf;
        noradData.omgasm = omgadf;
        noradData.xnodes = xnode;
        noradData.xn = xnn;
        noradData.timeSince = daysAfterEpoch;

        //deep space secular effects
        noradData.xll = noradData.xll + noradData.dp_ssl * noradData.timeSince;
        noradData.omgasm = noradData.omgasm + noradData.dp_ssg * noradData.timeSince;
        noradData.xnodes = noradData.xnodes + noradData.dp_ssh * noradData.timeSince;
        noradData._em = tleData.eccentricity + noradData.dp_sse * noradData.timeSince;
        noradData.xinc = Math.toRadians(tleData.inclinationDeg) + noradData.dp_ssi * noradData.timeSince;

        if(noradData.xinc < 0.0)
        {
            noradData.xinc = -noradData.xinc;
            noradData.xnodes = noradData.xnodes + Math.PI;
            noradData.omgasm = noradData.omgasm - Math.PI;
        }

        if(noradData.gp_reso)
        {
            while(!fDone)
            {
                if((noradData.dp_atime == 0.0) || ((noradData.timeSince >= 0.0) && (noradData.dp_atime <  0.0)) || ((noradData.timeSince <  0.0) && (noradData.dp_atime >= 0.0)))
                {
                    if(noradData.timeSince < 0)
                        delt = noradData.dp_stepn;
                    else
                        delt = noradData.dp_stepp;

                    //epoch restart
                    noradData.dp_atime = 0.0;
                    noradData.dp_xni = noradData.mean_motion;
                    noradData.dp_xli = noradData.dp_xlamo;

                    fDone = true;
                }
                else
                {
                    if(Math.abs(noradData.timeSince) < Math.abs(noradData.dp_atime))
                    {
                        delt = noradData.dp_stepp;

                        if(noradData.timeSince >= 0.0)
                            delt = noradData.dp_stepn;

                        sdp4CalcIntegrator(noradData, delt);
                    }
                    else
                    {
                        delt = noradData.dp_stepn;

                        if(noradData.timeSince > 0.0)
                            delt = noradData.dp_stepp;

                        fDone = true;
                    }
                }
            }

            while(Math.abs(noradData.timeSince - noradData.dp_atime) >= noradData.dp_stepp)
            {
                sdp4CalcIntegrator(noradData, delt);
            }

            ft = noradData.timeSince - noradData.dp_atime;

            sdp4CalcDotTermsData = sdp4CalcDotTerms(noradData);
            xndot = sdp4CalcDotTermsData.pxndot;
            xnddt = sdp4CalcDotTermsData.pxnddt;
            xldot = sdp4CalcDotTermsData.pxldot;

            noradData.xn = noradData.dp_xni + xndot * ft + xnddt * ft * ft * 0.5;

            xl = noradData.dp_xli + xldot * ft + xndot * ft * ft * 0.5;
            temp = -noradData.xnodes + noradData.dp_thgr + noradData.timeSince * THDT;

            noradData.xll = xl - noradData.omgasm + temp;

            if(!noradData.gp_sync)
                noradData.xll = xl + temp + temp;
        }

        xmdf = noradData.xll;
        omgadf = noradData.omgasm;
        xnode = noradData.xnodes;
        emm = noradData._em;
        xincc = noradData.xinc;
        xnn = noradData.xn;
        daysAfterEpoch = noradData.timeSince;

        sdp4SecularData.norad_data = noradData;
        sdp4SecularData.xmdf = xmdf;
        sdp4SecularData.omgadf = omgadf;
        sdp4SecularData.xnode = xnode;
        sdp4SecularData.emm = emm;
        sdp4SecularData.xincc = xincc;
        sdp4SecularData.xnn = xnn;
        sdp4SecularData.days_after_epoch = daysAfterEpoch;
        return(sdp4SecularData);
    }

    //Gets the ECI position for the SGP4 prediction model (near earth)
    @SuppressWarnings("SpellCheckingInspection")
    private static EciDataType getSGP4Position(EciDataType eciData, TLEDataType tleData, NoradDataType noradData, double daysAfterEpoch)
    {
        boolean isimp = false;
        double c1sq;
        double xmdf;
        double omgadf;
        double xnoddf;
        double omega;
        double xmp;
        double xnode;
        double delomg;
        double delm;
        double a, e;
        double xl;
        double tsq, tcube, tfour;
        double temp, tempa, tempe, templ;
        double d2 = 0.0;
        double d3 = 0.0;
        double d4 = 0.0;
        double t3cof = 0.0;
        double t4cof = 0.0;
        double t5cof = 0.0;

        // For m_perigee less than 220 kilometers, the isimp flag is set and
        // the equations are truncated to linear variation in Math.sqrt a and
        // quadratic variation in mean anomaly.  Also, the m_c3 term, the
        // delta omega term, and the delta m term are dropped.
        if((noradData.m_aodp * (1.0 - noradData.m_satEcc) / AE) < (220.0 / EarthRadiusKM + AE))
        {
            isimp = true;
        }

        if(!isimp)
        {
            c1sq = noradData.m_c1 * noradData.m_c1;
            d2 = 4.0 * noradData.m_aodp * noradData.m_tsi * c1sq;

            temp = d2 * noradData.m_tsi * noradData.m_c1 / 3.0;

            d3 = (17.0 * noradData.m_aodp + noradData.m_s4) * temp;
            d4 = 0.5 * temp * noradData.m_aodp * noradData.m_tsi * (221.0 * noradData.m_aodp + 31.0 * noradData.m_s4) * noradData.m_c1;
            t3cof = d2 + 2.0 * c1sq;
            t4cof = 0.25 * (3.0 * d3 + noradData.m_c1 * (12.0 * d2 + 10.0 * c1sq));
            t5cof = 0.2 * (3.0 * d4 + 12.0 * noradData.m_c1 * d3 + 6.0 * d2 * d2 + 15.0 * c1sq * (2.0 * d2 + c1sq));
        }

        // Update for secular gravity and atmospheric drag.
        xmdf   = Math.toRadians(tleData.meanAnomalyDeg) + noradData.m_xmdot * daysAfterEpoch;
        omgadf = Math.toRadians(tleData.argPerigreeDeg) + noradData.m_omgdot * daysAfterEpoch;
        xnoddf = Math.toRadians(tleData.rightAscnAscNodeDeg) + noradData.m_xnodot * daysAfterEpoch;
        omega = omgadf;
        xmp = xmdf;
        tsq = daysAfterEpoch * daysAfterEpoch;
        xnode = xnoddf + noradData.m_xnodcf * tsq;
        tempa = 1.0 - noradData.m_c1 * daysAfterEpoch;
        tempe = (tleData.drag / AE) * noradData.m_c4 * daysAfterEpoch;
        templ = noradData.m_t2cof * tsq;

        if(!isimp)
        {
            delomg = noradData.m_omgcof * daysAfterEpoch;
            delm = noradData.m_xmcof * (Math.pow(1.0 + noradData.m_eta * Math.cos(xmdf), 3.0) - noradData.m_delmo);
            temp = delomg + delm;

            xmp = xmdf + temp;
            omega = omgadf - temp;

            tcube = tsq * daysAfterEpoch;
            tfour = daysAfterEpoch * tcube;

            tempa = tempa - d2 * tsq - d3 * tcube - d4 * tfour;
            tempe = tempe + (tleData.drag / AE) * noradData.m_c5 * (Math.sin(xmp) - noradData.m_sinmo);
            templ = templ + t3cof * tcube + tfour * (t4cof + daysAfterEpoch * t5cof);
        }

        a  = noradData.m_aodp * Square(tempa);
        e  = noradData.m_satEcc - tempe;

        xl = xmp + omega + xnode + noradData.mean_motion * templ;

        //set the ECI data
        eciData = finalPosition(eciData, noradData, noradData.m_satInc, omgadf, e, a, xl, xnode, tleData.epochJulian, daysAfterEpoch);

        return(eciData);
    }

    //Gets the ECI position for the SDP4 prediction model (deep space)
    public static class EciNoradDataType
    {
        public EciDataType eciData;
        public NoradDataType noradData;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static EciNoradDataType getSDP4Position(EciDataType eciData, TLEDataType tleData, NoradDataType noradData, double daysAfterEpoch)
    {
        double xl;
        double a, e, xmam;
        double tempa, tempe, templ;
        double xmdf, omgadf, xnoddf, tsq, xnode, xn, em, xinc;
        EciNoradDataType eci_norad_data = new EciNoradDataType();
        Sdp4PeriodicsDataType sdp4_periodics_data;
        Sdp4SecularDataType sdp4_secular_data;

        sdp4Init(noradData, tleData);

        //update for secular gravity and atmospheric drag
        xmdf = Math.toRadians(tleData.meanAnomalyDeg) + noradData.m_xmdot * daysAfterEpoch;
        omgadf = Math.toRadians(tleData.argPerigreeDeg) + noradData.m_omgdot * daysAfterEpoch;
        xnoddf = Math.toRadians(tleData.rightAscnAscNodeDeg) + noradData.m_xnodot * daysAfterEpoch;
        tsq = daysAfterEpoch * daysAfterEpoch;
        xnode = xnoddf + noradData.m_xnodcf * tsq;
        tempa = 1.0 - noradData.m_c1 * daysAfterEpoch;
        tempe = (tleData.drag / AE) * noradData.m_c4 * daysAfterEpoch;
        templ = noradData.m_t2cof * tsq;
        xn = noradData.mean_motion;

        sdp4_secular_data = sdp4Secular(noradData, tleData, xmdf, omgadf, xnode, xn, daysAfterEpoch);
        noradData = sdp4_secular_data.norad_data;
        xmdf = sdp4_secular_data.xmdf;
        omgadf = sdp4_secular_data.omgadf;
        xnode = sdp4_secular_data.xnode;
        em = sdp4_secular_data.emm;
        xinc = sdp4_secular_data.xincc;
        xn = sdp4_secular_data.xnn;
        daysAfterEpoch = sdp4_secular_data.days_after_epoch;

        a = Math.pow(XKE / xn, TwoThirds) * Square(tempa);
        e = em - tempe;
        xmam = xmdf + noradData.mean_motion * templ;

        sdp4_periodics_data = sdp4Periodics(noradData, e, xinc, omgadf, xnode, xmam);
        noradData = sdp4_periodics_data.norad_data;
        e = sdp4_periodics_data.e;
        xinc = sdp4_periodics_data.xincc;
        omgadf = sdp4_periodics_data.omgadf;
        xnode = sdp4_periodics_data.xnode;
        xmam = sdp4_periodics_data.xmam;

        xl = xmam + omgadf + xnode;
        //xn = XKE / Math.pow(a, 1.5);

        eciData = finalPosition(eciData, noradData, xinc, omgadf, e, a, xl, xnode, tleData.epochJulian, daysAfterEpoch);

        eci_norad_data.eciData = eciData;
        eci_norad_data.noradData = noradData;
        return(eci_norad_data);
    }

    //Gets the ECI position for the given satellite at the given time
    private static EciNoradDataType getECIPosition(SatelliteObjectType satObj, double daysAfterEpoch)
    {
        EciNoradDataType eciNoradData = new EciNoradDataType();

        switch(satObj.orbit.predictionModel)
        {
            case SgpModelType.Sgp4Model:
                eciNoradData.eciData = getSGP4Position(satObj.eci, satObj.tle, satObj.norad, daysAfterEpoch);
                eciNoradData.noradData = satObj.norad;
                break;

            case SgpModelType.Sdp4Model:
                eciNoradData = getSDP4Position(satObj.eci, satObj.tle, satObj.norad, daysAfterEpoch);
                break;
        }

        return(eciNoradData);
    }

    //Updates the given satellite position and geodetic location if desired
    //note: geodetic position updated manually
    private static void updateSatellitePosition(SatelliteObjectType satObj, double julianDate)
    {
        EciNoradDataType eciNoradData = getECIPosition(satObj, DaysPassed(satObj.tle.epochJulian, julianDate));
        satObj.eci = eciNoradData.eciData;
        satObj.norad = eciNoradData.noradData;
    }

    //Updates the given non satellite position
    private static void updateNonSatellitePosition(SatelliteObjectType nonSatObj, ObserverType observer, double julianDate)
    {
        boolean havePlanetData = (nonSatObj.planetData != null);
        nonSatObj.eci.julianDate = julianDate;
        nonSatObj.planetData = new PlanetDataType(nonSatObj.getSatelliteNum(), julianDate, observer.geo.latitude, observer.geo.longitude, (havePlanetData && nonSatObj.planetData.rightAscDeg != Double.MAX_VALUE ? (nonSatObj.planetData.rightAscDeg / 15) : Double.MAX_VALUE), (havePlanetData ? nonSatObj.planetData.declinationDeg : Double.MAX_VALUE), (havePlanetData ? nonSatObj.planetData.distanceKm : Double.MAX_VALUE));
    }

    //Updates the given orbital position
    @SuppressWarnings("SpellCheckingInspection")
    public static void updateOrbitalPosition(SatelliteObjectType orbital, ObserverType observer, double julianDate, boolean updateGeo)
    {
        double satLon;
        double gmstDeg;

        if(orbital.getSatelliteNum() < 0)
        {
            updateNonSatellitePosition(orbital, observer, julianDate);

            if(updateGeo)
            {
                orbital.geo.latitude = orbital.planetData.declinationDeg;

                gmstDeg = Math.toDegrees(julianDateToGMST(orbital.eci.julianDate));
                satLon = orbital.planetData.rightAscDeg - gmstDeg;
                if(satLon < -180)
                {
                    satLon += 360;
                }
                else if(satLon > 180)
                {
                    satLon -= 360;
                }
                orbital.geo.longitude = satLon;
                orbital.geo.altitudeKm = orbital.planetData.distanceKm;
            }
        }
        else
        {
            updateSatellitePosition(orbital, julianDate);

            if(updateGeo)
            {
                orbital.geo = eciToGeo(orbital.eci);
                orbital.geo.speedKmS = Math.sqrt(Calculations.EarthGravity * ((2 / (orbital.geo.altitudeKm + Calculations.EarthRadiusKM)) - (1 / orbital.orbit.semiMajorAxisKm)));
            }
        }
    }
    public static void updateOrbitalPosition(SatelliteObjectType orbitalObj, ObserverType observer, Calendar gmtDate)
    {
        updateOrbitalPosition(orbitalObj, observer, julianDateCalendar(gmtDate), false);
    }

    //Applies atmospheric refraction to elevation angles
    @SuppressWarnings("SpellCheckingInspection")
    private static void applyAtmosphericRefraction(TopographicDataType pointData)
    {
        //equation 15.4 from Meeus’s astronomical algorithms
        //note: works best on elevation angles >= 5 degrees

        double refractionRadians;
        double elevationRadians = Math.toRadians(pointData.elevation);

        //calculate the refraction
        refractionRadians = 1.02 / 60.0 / 180.0 * Math.PI / Math.tan(elevationRadians + 10.3 * (Math.pow(Math.PI / 180.0, 2)) / (elevationRadians + 5.11 * Math.PI / 180.0));

        //add refraction to elevation
        pointData.elevation += Math.toDegrees(refractionRadians);
    }

    //Gets the data needed to point towards an object
    @SuppressWarnings("SpellCheckingInspection")
    public static TopographicDataType getLookAngles(ObserverType observer, SatelliteObjectType satObj, boolean applyRefraction)
    {
        double declRad;
        double theta;
        double az, el;
        double distanceKm;
        double rangeX, rangeY, rangeZ, rangeW;
        double topS, topE, topZ;
        double sinLat, cosLat, sinTheta, cosTheta;
        double latitude, latRad, hourAngle;
        double haRad, csz, zenith, azDenom, te, solarZen, azRad;
        double azimuth, elevation, exoatmElevation, refractionCorrection;
        EciDataType site_eciData;
        TopographicDataType pointData;

        if(satObj.getSatelliteNum() > 0)
        {
            //update observer ECI position for satellite position Julian date
            site_eciData = geoToECI(observer.geo, satObj.eci.julianDate);

            //calculate the ECI coordinates for this cSite object at the time of interest.
            rangeX = satObj.eci.positionX - site_eciData.positionX;
            rangeY = satObj.eci.positionY - site_eciData.positionY;
            rangeZ = satObj.eci.positionZ - site_eciData.positionZ;
            rangeW = Math.sqrt(Square(rangeX) + Square(rangeY) + Square(rangeZ));

            //the site's Local Mean Sidereal Time at the time of interest.
            theta = julianDateToLMST(site_eciData.julianDate, Math.toRadians(observer.geo.longitude));

            sinLat = Math.sin(Math.toRadians(observer.geo.latitude));
            cosLat = Math.cos(Math.toRadians(observer.geo.latitude));
            sinTheta = Math.sin(theta);
            cosTheta = Math.cos(theta);

            topS = sinLat * cosTheta * rangeX + sinLat * sinTheta * rangeY - cosLat * rangeZ;
            topE = -sinTheta * rangeX + cosTheta * rangeY;
            topZ = cosLat * cosTheta * rangeX + cosLat * sinTheta * rangeY + sinLat * rangeZ;

            az = Math.atan(-topE / topS);
            if(topS > 0.0)
            {
                az += Math.PI;
            }
            if(az < 0.0)
            {
                az += 2.0 * Math.PI;
            }
            el = Math.asin(topZ / rangeW);

            pointData = new TopographicDataType(Math.toDegrees(az), Math.toDegrees(el), rangeW);

            //if applying atmospheric refraction
            if(applyRefraction)
            {
                applyAtmosphericRefraction(pointData);
            }
        }
        else
        {
            latitude = observer.geo.latitude;
            latRad = Math.toRadians(latitude);
            hourAngle = satObj.planetData.hourAngleDeg;
            declRad = Math.toRadians(satObj.planetData.declinationDeg);
            distanceKm = satObj.planetData.distanceKm;

            //normalize hour angle
            if(hourAngle < -180)
            {
                hourAngle += 360.0;
            }
            haRad = Math.toRadians(hourAngle);

            csz = Math.sin(latRad) * Math.sin(declRad) + Math.cos(latRad) * Math.cos(declRad) * Math.cos(haRad);
            if(csz > 1.0)
            {
                csz = 1.0;
            }
            else if(csz < -1.0)
            {
                csz = -1.0;
            }
            zenith = Math.toDegrees(Math.acos(csz));
            azDenom = Math.cos(latRad) * Math.sin(Math.toRadians(zenith));
            if(Math.abs(azDenom) > 0.001)
            {
                azRad = ((Math.sin(latRad) * Math.cos(Math.toRadians(zenith))) - Math.sin(declRad)) / azDenom;
                if(Math.abs(azRad) > 1.0)
                {
                    if(azRad < 0)
                    {
                        azRad = -1.0;
                    }
                    else
                    {
                        azRad = 1.0;
                    }
                }
                azimuth = 180.0 - Math.toDegrees(Math.acos(azRad));
                if(hourAngle > 0.0)
                {
                    azimuth = -azimuth;
                }
            }
            else
            {
                if(latitude > 0.0)
                {
                    azimuth = 180.0;
                }
                else
                {
                    azimuth = 0.0;
                }
            }
            if(azimuth < 0.0)
            {
                azimuth += 360.0;
            }

            exoatmElevation = 90.0 - zenith;

            //if don't need to apply atmospheric refraction correction
            if(exoatmElevation > 85.0)
            {
                refractionCorrection = 0.0;
            }
            //else apply
            else
            {
                te = Math.tan(Math.toRadians(exoatmElevation));
                if(exoatmElevation > 5.0)
                {
                    refractionCorrection = 58.1 / te - 0.07 / (te * te * te) + 0.000086 / (te * te *te * te * te);
                }
                else if(exoatmElevation > -0.575)
                {
                    refractionCorrection = 1735.0 + exoatmElevation * (-518.2 + exoatmElevation * (103.4 + exoatmElevation * (-12.79 + exoatmElevation * 0.711)));
                }
                else
                {
                    refractionCorrection = -20.774 / te;
                }
                refractionCorrection = refractionCorrection / 3600.0;
            }

            solarZen = zenith - refractionCorrection;

            azimuth = Math.floor(azimuth * 100 + 0.5) / 100.0;
            elevation = Math.floor((90.0 - solarZen) * 100 + 0.5) / 100.0;

            pointData = new TopographicDataType(azimuth, elevation, distanceKm);
        }

        return(pointData);
    }

    //Gets the distance in km between 2 geodetic positions
    public static double getDistanceKm(GeodeticDataType start, GeodeticDataType end)
    {
        double distanceLat = Math.toRadians(end.latitude - start.latitude);
        double distanceLon = Math.toRadians(end.longitude - start.longitude);
        double startLat = Math.toRadians(start.latitude);
        double endLat = Math.toRadians(end.latitude);
        double a = Math.pow(Math.sin(distanceLat / 2), 2) + Math.cos(startLat) * Math.cos(endLat) * Math.pow(Math.sin(distanceLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return(EarthRadiusKM * c);
    }

    //Gets the bearing in degrees from 2 geodetic positions
    public static double getBearing(GeodeticDataType start, GeodeticDataType end)
    {
        double startLat = Math.toRadians(start.latitude);
        double startLon = Math.toRadians(start.longitude);
        double endLat = Math.toRadians(end.latitude);
        double endLon = Math.toRadians(end.longitude);
        double deltaLon = endLon - startLon;
        double x = Math.cos(endLat) * Math.sin(deltaLon);
        double y = Math.cos(startLat) * Math.sin(endLat) - Math.sin(startLat) * Math.cos(endLat) * Math.cos(deltaLon);
        double bearing = Math.atan2(x, y);

        return(Math.toDegrees(bearing));
    }

    //Calculates the satellite min/max latitude and longitude area for visibility
    public static GeodeticAreaType getFootprint(double latitude, double altitudeKm)
    {
        double radiusKm = Math.acos(EarthRadiusKM / (EarthRadiusKM + altitudeKm)) * EarthRadiusKM;
        double latitudeKm = radiusKm / DegToLatKm;
        double latitudeRad = Math.toRadians(latitude);
        double longitudeKm = radiusKm / (DegToLonKm * Math.cos(latitudeRad));
        GeodeticAreaType geoArea = new GeodeticAreaType();

        geoArea.latitudeWidth = latitudeKm;
        geoArea.longitudeWidth = longitudeKm;

        return(geoArea);
    }
}
