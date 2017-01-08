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
public class SensorTagOpticalServiceAdapter extends BLENotificationServiceAdapter<double[]> {

    private static final Logger logger = LogManager.getLogger(SensorTagOpticalServiceAdapter.class);

    public static final byte[] ENABLE_CODE = new byte[]{0x01};
    public static final byte[] DISABLE_CODE = new byte[]{0x00};
    public static final byte[] PERIOD_MILLION_SECOND = new byte[]{100};

    public static final String SERVICE_UUID = "f000aa70-0451-4000-b000-000000000000";
    public static final String VALUE_UUID = "f000aa71-0451-4000-b000-000000000000";
    public static final String CONFIG_UUID = "f000aa72-0451-4000-b000-000000000000";
    public static final String PERIOD_UUID = "f000aa73-0451-4000-b000-000000000000";

    public SensorTagOpticalServiceAdapter(BluetoothGattService service) {
        super(service);
    }

    @Override
    public boolean init() {
        BluetoothGattCharacteristic opticalConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic opticalPeriod = getService().find(PERIOD_UUID);

        logger.debug("Find optical config & period characteristic : [{}, {}]", opticalConfig != null, opticalPeriod != null);

        if (opticalConfig == null || opticalPeriod == null) return false;

        opticalConfig.writeValue(ENABLE_CODE);
        opticalPeriod.writeValue(PERIOD_MILLION_SECOND);

        return true;
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find optical value characteristic : {}.", characteristic != null);
        return characteristic;
    }

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 2) return null;

        int integer = (((bytes[0] & 0xff)) | (((bytes[1] & 0xff) << 8)));
        float e =  integer & 0x0FFF;
        float m = (integer & 0xF000) >>12;
        double value = m * (0.01 * Math.pow(2.0, e));
        logger.debug("Convert optical {} to [{}, {}, {}].", Arrays.toString(bytes), e, m, value);

        return new double[]{value};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        pressureConfig.writeValue(DISABLE_CODE);
        return super.stop();
    }
}
