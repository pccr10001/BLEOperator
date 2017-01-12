package adapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by IDIC on 2017/1/4.
 */
public abstract class BLENotificationServiceAdapter<ResultType> {

    private static final Logger logger = LogManager.getLogger(BLENotificationServiceAdapter.class);

    private boolean isRunning;
    private BluetoothGattService service;
    private List<BLEServiceCallback<ResultType>> callbacks = new LinkedList<>();
    protected BLEInstance instance;
    protected BLEServiceCallback<byte[]> innerNotification;

    public BLENotificationServiceAdapter(BluetoothGattService service) {
        this.service = service;
        this.instance = BLEInstance.getInstance();
    }

    protected boolean init(Callback callback) {
        callback.operate();
        return true;
    }

    protected BluetoothGattCharacteristic getNotificationCharacteristic() {
        //return the characteristic you want to notify here.
        return null;
    }

    public final void run(BLEServiceCallback<ResultType> callback) {
        try {
            isRunning = true;
            registerListener(callback);
            init(this::start);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            isRunning = false;
        }
    }

    private boolean start() {
        innerNotification = data -> {
            for (BLEServiceCallback<ResultType> callback : callbacks) callback.supply(convert(data));
        };
        instance.notifyCharacteristic(service.getDevice().getAddress(), getNotificationCharacteristic(), innerNotification);
        return true;
    }

    public ResultType convert(byte[] bytes) {
        //convert data here.
        return null;
    }

    public boolean stop() {
        if (innerNotification != null) {
            BluetoothGattCharacteristic characteristic = getNotificationCharacteristic();
            instance.disableNotifyCharacteristic(service.getDevice().getAddress(), characteristic, innerNotification);
            logger.debug("Disable notification : " + characteristic.getUUID());
            isRunning = false;
            return true;
        }
        return false;
    }

    protected final BluetoothGattService getService() {
        return service;
    }

    public final synchronized boolean registerListener(BLEServiceCallback<ResultType> callback) {
        return this.callbacks.add(callback);
    }

    public final synchronized boolean removeListener(BLEServiceCallback<ResultType> callback) {
        return this.callbacks.remove(callback);
    }

    public boolean isRunning() {
        return isRunning;
    }

    protected interface Callback {
        void operate();
    }
}
