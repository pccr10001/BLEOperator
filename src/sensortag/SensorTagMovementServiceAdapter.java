package sensortag;

import adapter.BLENotificationServiceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tool.ByteUtils;

import java.util.Arrays;

import static tool.ByteUtils.shortUnsignedAtOffset;

/**
 * Created by IDIC on 2017/1/5.
 */
public class SensorTagMovementServiceAdapter extends BLENotificationServiceAdapter<double[]> {

    private static final Logger logger = LogManager.getLogger(SensorTagMovementServiceAdapter.class);

    public static final byte[] ENABLE_CODE = new byte[]{0b1111111, 0b1111111};
    public static final byte[] DISABLE_CODE = new byte[]{0b0000000, 0b0000000};
    public static final byte[] PERIOD_MILLION_SECOND = new byte[]{100};

    public static final String SERVICE_UUID = "f000aa80-0451-4000-b000-000000000000";
    public static final String VALUE_UUID = "f000aa81-0451-4000-b000-000000000000";
    public static final String CONFIG_UUID = "f000aa82-0451-4000-b000-000000000000";
    public static final String PERIOD_UUID = "f000aa83-0451-4000-b000-000000000000";

    public static final int ACCELEROMETER_RANGE_2G = 0;
    public static final int ACCELEROMETER_RANGE_4G = 1;
    public static final int ACCELEROMETER_RANGE_8G = 2;
    public static final int ACCELEROMETER_RANGE_16G = 3;

    public SensorTagMovementServiceAdapter(BluetoothGattService service) {
        super(service);
    }

    private int accelerometerRange = -1;

    @Override
    protected boolean init(BLENotificationServiceAdapter.Callback callback) {
        BluetoothGattCharacteristic movementConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic movementPeriod = getService().find(PERIOD_UUID);

        logger.debug("Find movement config & period characteristic : [{}, {}]", movementConfig != null, movementPeriod != null);

        if (movementConfig == null || movementPeriod == null) return false;

        instance.writeCharacteristic(movementConfig, ENABLE_CODE);
        instance.readCharacteristic(movementConfig, data -> {
            String bits = ByteUtils.bytesToBit(data, 8);
            int range = ByteUtils.bitToInt("" + bits.charAt(8) + bits.charAt(9));
            logger.debug("Gyroscope z : {}, Gyroscope y : {}, Gyroscope x : {}," +
                            " Accelerometer z : {}, Accelerometer y : {}, Accelerometer x : {}," +
                            " Magnetometer enable : {}, Wake-On-Motion Enable : {}, Accelerometer range : {}",
                    ByteUtils.bitToBoolean(bits.charAt(0)), ByteUtils.bitToBoolean(bits.charAt(1)), ByteUtils.bitToBoolean(bits.charAt(2)),
                    ByteUtils.bitToBoolean(bits.charAt(3)), ByteUtils.bitToBoolean(bits.charAt(4)), ByteUtils.bitToBoolean(bits.charAt(5)),
                    ByteUtils.bitToBoolean(bits.charAt(6)), ByteUtils.bitToBoolean(bits.charAt(7)), range);

            switch (range) {
                case ACCELEROMETER_RANGE_2G:
                    accelerometerRange = 16384;
                    break;
                case ACCELEROMETER_RANGE_4G:
                    accelerometerRange = 8192;
                    break;
                case ACCELEROMETER_RANGE_8G:
                    accelerometerRange = 4096;
                    break;
                case ACCELEROMETER_RANGE_16G:
                    accelerometerRange = 2048;
                    break;
                default:
                    logger.error("Accelerometer range error : {}", range);
            }

            instance.writeCharacteristic(movementPeriod, PERIOD_MILLION_SECOND);
        });

        return super.init(callback);
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find movement value characteristic : {}", characteristic != null);
        return characteristic;
    }

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 18) return null;

        final float GYROSCOPE_SCALE = (float) (65536 / 500);
        double gyroscopeX = (shortUnsignedAtOffset(bytes, 0) * 1.0f) / GYROSCOPE_SCALE;
        double gyroscopeY = (shortUnsignedAtOffset(bytes, 2) * 1.0f) / GYROSCOPE_SCALE;
        double gyroscopeZ = (shortUnsignedAtOffset(bytes, 2) * 1.0f) / GYROSCOPE_SCALE;

        double accelerometerX = -(shortUnsignedAtOffset(bytes, 6) * 1.0f) / accelerometerRange;
        double accelerometerY = (shortUnsignedAtOffset(bytes, 8) * 1.0f) / accelerometerRange;
        double accelerometerZ = -(shortUnsignedAtOffset(bytes, 10) * 1.0f) / accelerometerRange;

        final float MAGNETOMETER_SCALE = (float) (32768 / 4912);
        double magnetometerX = (shortUnsignedAtOffset(bytes, 12) * 1.0f);
        double magnetometerY = (shortUnsignedAtOffset(bytes, 14) * 1.0f);
        double magnetometerZ = (shortUnsignedAtOffset(bytes, 16) * 1.0f);

        logger.debug("Convert movement {} to [{}, {}, {}].", Arrays.toString(bytes),
                new double[]{gyroscopeX, gyroscopeY, gyroscopeZ},
                new double[]{accelerometerX, accelerometerY, accelerometerZ},
                new double[]{magnetometerX, magnetometerY, magnetometerZ});
        return new double[]{gyroscopeX, gyroscopeY, gyroscopeZ,
                accelerometerX, accelerometerY, accelerometerZ,
                magnetometerX, magnetometerY, magnetometerZ};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        instance.writeCharacteristic(pressureConfig, DISABLE_CODE);
        return super.stop();
    }
}
