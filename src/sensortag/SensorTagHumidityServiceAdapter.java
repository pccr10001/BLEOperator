package sensortag;

import adapter.BLENotificationServiceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.util.Arrays;

import static tool.ByteUtils.shortUnsignedAtOffset;

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
    protected boolean init(BLENotificationServiceAdapter.Callback callback) {
        BluetoothGattCharacteristic humidityConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic humidityPeriod = getService().find(PERIOD_UUID);

        logger.debug("Find humidity config & period characteristic : [{}, {}]", humidityConfig != null, humidityPeriod != null);

        if (humidityConfig == null || humidityPeriod == null) return false;

        instance.writeCharacteristic(humidityConfig, ENABLE_CODE);
        instance.writeCharacteristic(humidityPeriod, PERIOD_MILLION_SECOND);

        return super.init(callback);
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find humidity value characteristic : {}", characteristic != null);
        return characteristic;
    }

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 4) return null;

        double temperatureCelsius = ((shortUnsignedAtOffset(bytes, 0) * 1.0f) / 65536) * 165 - 40;

        double relativeHumidity = (shortUnsignedAtOffset(bytes, 2) * 1.0f / 65536) * 100;

        logger.debug("Convert humidity {} to [{}, {}].", Arrays.toString(bytes), temperatureCelsius, relativeHumidity);
        return new double[]{temperatureCelsius, relativeHumidity};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        instance.writeCharacteristic(pressureConfig, DISABLE_CODE);
        return super.stop();
    }
}
