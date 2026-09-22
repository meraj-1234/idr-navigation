package com.idr.nav.fusion;

/**
 * 4-State Navigation Vector & Error Covariance Matrix.
 * 
 * State Vector:
 * x[0] = Latitude (degrees)
 * x[1] = Longitude (degrees)
 * x[2] = Velocity East (m/s)
 * x[3] = Velocity North (m/s)
 */
public class KalmanState {

    private double[] x = new double[4];
    private double[][] P = new double[4][4];
    private long timestamp;

    public KalmanState(double lat, double lon, double vEast, double vNorth, long timestamp) {
        this.x[0] = lat;
        this.x[1] = lon;
        this.x[2] = vEast;
        this.x[3] = vNorth;
        this.timestamp = timestamp;

        // Initialize covariance P with reasonable diagonal uncertainties
        this.P[0][0] = 1e-8; // ~ 10m lat uncertainty in deg^2
        this.P[1][1] = 1e-8; // ~ 10m lon uncertainty in deg^2
        this.P[2][2] = 1.0;  // 1 m/s velocity variance
        this.P[3][3] = 1.0;
    }

    public double[] getX() { return x; }
    public double[][] getP() { return P; }
    public long getTimestamp() { return timestamp; }

    public double getLatitude() { return x[0]; }
    public double getLongitude() { return x[1]; }
    public double getVelocityEast() { return x[2]; }
    public double getVelocityNorth() { return x[3]; }

    public double getTotalSpeed() {
        return Math.sqrt(x[2] * x[2] + x[3] * x[3]);
    }

    public double getHeadingDeg() {
        double heading = Math.toDegrees(Math.atan2(x[2], x[3]));
        return (heading + 360.0) % 360.0;
    }

    public void setLatitude(double lat) { this.x[0] = lat; }
    public void setLongitude(double lon) { this.x[1] = lon; }
    public void setVelocityEast(double ve) { this.x[2] = ve; }
    public void setVelocityNorth(double vn) { this.x[3] = vn; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
