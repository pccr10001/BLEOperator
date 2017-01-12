package sensortag;

import adapter.BLENotificationServiceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.util.Arrays;

/**
 * Created by IDIC on 2017/1/5.
 */
public class SensorTagTemperatureServiceAdapter extends BLENotificationServiceAdapter<double[]> {

    private static final Logger logger = LogManager.getLogger(SensorTagTemperatureServiceAdapter.class);

    public static final byte[] ENABLE_CODE = new byte[]{0x01};
    public static final byte[] DISABLE_CODE = new byte[]{0x00};
    public static final byte[] PERIOD_MILLION_SECOND = new byte[]{100};

    public static final String SERVICE_UUID = "f000aa00-0451-4000-b000-000000000000";
    public static final String VALUE_UUID = "f000aa01-0451-4000-b000-000000000000";
    public static final String CONFIG_UUID = "f000aa02-0451-4000-b000-000000000000";
    public static final String PERIOD_UUID = "f000aa03-0451-4000-b000-000000000000";

    public SensorTagTemperatureServiceAdapter(BluetoothGattService service) {
        super(service);
    }

    @Override
    protected boolean init(BLENotificationServiceAdapter.Callback callback) {
        BluetoothGattCharacteristic temperatureConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic temperaturePeriod = getService().find(PERIOD_UUID);

        logger.debug("Find temperature config & period characteristic : [{}, {}]", temperatureConfig != null, temperaturePeriod != null);

        if (temperatureConfig == null || temperaturePeriod == null) return false;

        instance.writeCharacteristic(temperatureConfig, ENABLE_CODE);
        instance.writeCharacteristic(temperaturePeriod, PERIOD_MILLION_SECOND);

        return super.init(callback);
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find temperature value characteristic : {}", characteristic != null);
        return characteristic;
    }

    private static final float SCALE_LSB = 0.03125f;

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 4) return null;

        double objectTempCelsius = (((bytes[0] & 0xff) | (bytes[1] << 8)) >> 2) * SCALE_LSB;
        double ambientTempCelsius = (((bytes[2] & 0xff) | (bytes[3] << 8)) >> 2) * SCALE_LSB;

        logger.debug("Convert temperature {} to [{}, {}].", Arrays.toString(bytes), objectTempCelsius, ambientTempCelsius);
        return new double[]{objectTempCelsius, ambientTempCelsius};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        instance.writeCharacteristic(pressureConfig, DISABLE_CODE);
        return super.stop();
    }
}
