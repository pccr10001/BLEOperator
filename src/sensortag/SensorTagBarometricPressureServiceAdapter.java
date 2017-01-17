package sensortag;

import adapter.BLENotificationServiceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.util.Arrays;

import static tool.ByteUtils.bit24shortUnsignedAtOffset;

/**
 * Created by IDIC on 2017/1/5.
 */
public class SensorTagBarometricPressureServiceAdapter extends BLENotificationServiceAdapter<double[]> {

    private static final Logger logger = LogManager.getLogger(SensorTagBarometricPressureServiceAdapter.class);

    public static final byte[] ENABLE_CODE = new byte[]{0x01};
    public static final byte[] DISABLE_CODE = new byte[]{0x00};
    public static final byte[] PERIOD_MILLION_SECOND = new byte[]{100};

    public static final String SERVICE_UUID = "f000aa40-0451-4000-b000-000000000000";
    public static final String VALUE_UUID = "f000aa41-0451-4000-b000-000000000000";
    public static final String CONFIG_UUID = "f000aa42-0451-4000-b000-000000000000";
    public static final String PERIOD_UUID = "f000aa44-0451-4000-b000-000000000000";

    public SensorTagBarometricPressureServiceAdapter(BluetoothGattService service) {
        super(service);
    }

    @Override
    protected boolean init(BLENotificationServiceAdapter.Callback callback) {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic pressurePeriod = getService().find(PERIOD_UUID);

        logger.debug("Find barometric pressure config & period characteristic : [{}, {}]", pressureConfig != null, pressurePeriod != null);

        if (pressureConfig == null || pressurePeriod == null) return false;

        instance.writeCharacteristic(pressureConfig, ENABLE_CODE);
        instance.writeCharacteristic(pressurePeriod, PERIOD_MILLION_SECOND);

        return super.init(callback);
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find barometric pressure value characteristic : {}", characteristic != null);
        return characteristic;
    }

    private static final float SCALE_LSB = 0.03125f;

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 6) return null;

        double temperatureValue = (bit24shortUnsignedAtOffset(bytes, 0) >> 2) * 1.0f * SCALE_LSB;
        double pressureValue = (bit24shortUnsignedAtOffset(bytes, 3) * 1.0f) / 100.0f;

        logger.debug("Convert barometric pressure {} to [{}, {}].", Arrays.toString(bytes), temperatureValue, pressureValue);
        return new double[]{pressureValue};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        instance.writeCharacteristic(pressureConfig, DISABLE_CODE);
        return super.stop();
    }
}
