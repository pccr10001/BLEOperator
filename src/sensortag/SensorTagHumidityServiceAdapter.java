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
public class SensorTagHumidityServiceAdapter extends BLENotificationServiceAdapter<double[]> {

    private static final Logger logger = LogManager.getLogger(SensorTagHumidityServiceAdapter.class);

    public static final byte[] ENABLE_CODE = new byte[]{0x01};
    public static final byte[] DISABLE_CODE = new byte[]{0x00};
    public static final byte[] PERIOD_MILLION_SECOND = new byte[]{100};

    public static final String SERVICE_UUID = "f000aa20-0451-4000-b000-000000000000";
    public static final String VALUE_UUID = "f000aa21-0451-4000-b000-000000000000";
    public static final String CONFIG_UUID = "f000aa22-0451-4000-b000-000000000000";
    public static final String PERIOD_UUID = "f000aa23-0451-4000-b000-000000000000";

    public SensorTagHumidityServiceAdapter(BluetoothGattService service) {
        super(service);
    }

    @Override
    public boolean init() {
        BluetoothGattCharacteristic humidityConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic humidityPeriod = getService().find(PERIOD_UUID);

        logger.debug("Find humidity config & period characteristic : [{}, {}]", humidityConfig != null, humidityPeriod != null);

        if (humidityConfig == null || humidityPeriod == null) return false;

        humidityConfig.writeValue(ENABLE_CODE);
        humidityPeriod.writeValue(PERIOD_MILLION_SECOND);

        return true;
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find humidity value characteristic : {}.", characteristic != null);
        return characteristic;
    }

    private static final float SCALE_LSB = 0.03125f;

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 4) return null;

        double temperatureCelsius = ((((bytes[0] & 0xff)) | (((bytes[1] & 0xff) << 8))) >> 2) * SCALE_LSB;
        double relativeHumidity  = ((((bytes[2] & 0xff)) | (((bytes[3] & 0xff) << 8))) / 65536) * 100;

        logger.debug("Convert humidity {} to [{}, {}].", Arrays.toString(bytes), temperatureCelsius, relativeHumidity);
        return new double[]{temperatureCelsius, relativeHumidity};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        pressureConfig.writeValue(DISABLE_CODE);
        return super.stop();
    }
}
