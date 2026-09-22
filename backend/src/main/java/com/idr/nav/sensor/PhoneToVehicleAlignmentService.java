package com.idr.nav.sensor;

import org.springframework.stereotype.Service;

/**
 * Phone-to-Vehicle Coordinate Transformation Service.
 * 
 * Defines transformations across three distinct coordinate frames:
 * 1. Device Coordinate Frame (Xd, Yd, Zd): Hardware-fixed axes of the smartphone.
 *    - Xd: Screen right
 *    - Yd: Screen top
 *    - Zd: Perpendicular out of the screen (normal)
 * 
 * 2. Vehicle Body Frame (Xv, Yv, Zv): Rigidly attached to the vehicle chassis.
 *    - Xv: Longitudinal axis pointing directly Forward
 *    - Yv: Lateral axis pointing to the Right
 *    - Zv: Vertical axis pointing Downward / Upward
 * 
 * 3. Navigation / World Frame (East, North, Up - ENU or NED).
 */
@Service
public class PhoneToVehicleAlignmentService {

    public enum MountType {
        PORTRAIT_WINDSHIELD, // Phone mounted upright on windshield/dash
        FLAT_CONSOLE,        // Phone lying flat on console with top pointing forward
        FREE_ORIENTATION     // Dynamic rotation using device orientation angles (alpha, beta, gamma)
    }

    private MountType mountType = MountType.PORTRAIT_WINDSHIELD;

    // Pitch tilt angle in degrees for windshield mount (typically ~70 deg from horizontal)
    private double mountPitchDeg = 75.0;

    /**
     * Transforms raw 3D accelerometer readings from smartphone device frame to vehicle body frame.
     * Returns double[3] = [forwardAccel, lateralAccel, verticalAccel] in m/s^2.
     */
    public double[] transformToVehicleFrame(double ax, double ay, double az,
                                           Double alpha, Double beta, Double gamma) {
        double forwardAccel;
        double lateralAccel;
        double verticalAccel;

        switch (mountType) {
            case FLAT_CONSOLE:
                // Device lying flat: Top is Forward (+Y), Right is Right (+X), Screen Up is Up (+Z)
                forwardAccel = ay;
                lateralAccel = ax;
                verticalAccel = az;
                break;

            case FREE_ORIENTATION:
                if (beta != null && gamma != null) {
                    // Use Euler angles (beta = pitch, gamma = roll) to align with gravity vector
                    double pitchRad = Math.toRadians(beta);
                    double rollRad = Math.toRadians(gamma);

                    // De-rotate roll and pitch
                    double cosP = Math.cos(pitchRad);
                    double sinP = Math.sin(pitchRad);
                    double cosR = Math.cos(rollRad);
                    double sinR = Math.sin(rollRad);

                    forwardAccel = -sinP * az + cosP * ay;
                    lateralAccel = cosR * ax + sinR * sinP * ay + sinR * cosP * az;
                    verticalAccel = -sinR * ax + cosR * sinP * ay + cosR * cosP * az;
                } else {
                    // Fallback to windshield
                    forwardAccel = ay;
                    lateralAccel = ax;
                    verticalAccel = az;
                }
                break;

            case PORTRAIT_WINDSHIELD:
            default:
                // Windshield mount: Screen tilted back by mountPitchDeg.
                // Screen top is roughly Up, Screen right is Right, Screen back is Forward.
                double pitchRad = Math.toRadians(mountPitchDeg);
                double cosTilt = Math.cos(pitchRad);
                double sinTilt = Math.sin(pitchRad);

                // Forward along road direction
                forwardAccel = -sinTilt * az + cosTilt * ay;
                // Lateral side-to-side
                lateralAccel = ax;
                // Vertical (perpendicular to road surface)
                verticalAccel = cosTilt * az + sinTilt * ay;
                break;
        }

        return new double[] { forwardAccel, lateralAccel, verticalAccel };
    }

    public MountType getMountType() { return mountType; }
    public void setMountType(MountType mountType) { this.mountType = mountType; }

    public double getMountPitchDeg() { return mountPitchDeg; }
    public void setMountPitchDeg(double mountPitchDeg) { this.mountPitchDeg = mountPitchDeg; }
}
