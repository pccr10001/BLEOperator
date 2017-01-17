package sensortag;

import adapter.BLENotificationServiceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.math.BigDecimal;
import java.util.Arrays;

import static tool.ByteUtils.shortUnsignedAtOffset;

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
    protected boolean init(BLENotificationServiceAdapter.Callback callback) {
        BluetoothGattCharacteristic opticalConfig = getService().find(CONFIG_UUID);
        BluetoothGattCharacteristic opticalPeriod = getService().find(PERIOD_UUID);

        logger.debug("Find optical config & period characteristic : [{}, {}]", opticalConfig != null, opticalPeriod != null);

        if (opticalConfig == null || opticalPeriod == null) return false;

        instance.writeCharacteristic(opticalConfig, ENABLE_CODE);
        instance.writeCharacteristic(opticalPeriod, PERIOD_MILLION_SECOND);

        return super.init(callback);
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find optical value characteristic : {}", characteristic != null);
        return characteristic;
    }

    @Override
    public double[] convert(byte[] bytes) {
        if (bytes.length != 2) return null;

        int integer = shortUnsignedAtOffset(bytes, 0);
        float e = integer & 0x0FFF;
        float m = (integer & 0xF000) >> 12;
        double value = new BigDecimal(2).pow((int) e).multiply(new BigDecimal(0.01)).multiply(new BigDecimal(m)).doubleValue();
        logger.debug("Convert optical {} to [{}, {}, {}].", Arrays.toString(bytes), e, m, value);

        return new double[]{value};
    }

    @Override
    public boolean stop() {
        BluetoothGattCharacteristic pressureConfig = getService().find(CONFIG_UUID);
        instance.writeCharacteristic(pressureConfig, DISABLE_CODE);
        return super.stop();
    }
}
