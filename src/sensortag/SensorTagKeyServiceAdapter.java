package sensortag;

import adapter.BLENotificationServiceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tool.ByteUtils;

/**
 * Created by IDIC on 2017/1/5.
 */
public class SensorTagKeyServiceAdapter extends BLENotificationServiceAdapter<byte[]> {

    private static final Logger logger = LogManager.getLogger(SensorTagKeyServiceAdapter.class);

    public static final String SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String VALUE_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";

    public SensorTagKeyServiceAdapter(BluetoothGattService service) {
        super(service);
    }

    @Override
    protected boolean init(BLENotificationServiceAdapter.Callback callback) {
        return super.init(callback);
    }

    @Override
    public BluetoothGattCharacteristic getNotificationCharacteristic() {
        BluetoothGattCharacteristic characteristic = getService().find(VALUE_UUID);
        logger.debug("Find key value characteristic : {}", characteristic != null);
        return characteristic;
    }

    @Override
    public byte[] convert(byte[] bytes) {
        if (bytes.length != 1) return null;
        logger.debug("press key {}.", ByteUtils.bytesToBit(bytes, 2));
        return bytes;
    }
}
