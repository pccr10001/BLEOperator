package adapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothGattService;
import tinyb.BluetoothNotification;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by IDIC on 2017/1/4.
 */
public abstract class BLENotificationServiceAdapter<ResultType> implements BLENotificationServiceModel, BLENormalServiceModel<ResultType> {

    private static final Logger logger = LogManager.getLogger(BLENotificationServiceAdapter.class);

    private boolean isRunning;
    private BluetoothGattService service;
    private List<BLEServiceCallback<ResultType>> callbacks = new LinkedList<>();

    public BLENotificationServiceAdapter(BluetoothGattService service) {
        this.service = service;
    }

    @Override
    public boolean init() {
        //Enable ble service here.
        return false;
    }

    private BluetoothNotification<byte[]> notification;

    protected BluetoothGattCharacteristic getNotificationCharacteristic() {
        //return the characteristic you want to notify here.
        return null;
    }

    public final void run(BLEServiceCallback<ResultType> callback) {
        try {
            isRunning = true;
            init();
            start();
            registerListener(callback);
        } catch (Exception e) {
            logger.error(e.getMessage());
            isRunning = false;
        }
    }

    @Override
    public final boolean start() {
        notification = bytes -> {
            for (BLEServiceCallback<ResultType> callback : callbacks) callback.supply(convert(bytes));
        };
        getNotificationCharacteristic().enableValueNotifications(notification);
        return true;
    }

    @Override
    public ResultType convert(byte[] bytes) {
        //convert data here.
        return null;
    }

    @Override
    public boolean stop() {
        if (notification != null) {
            BluetoothGattCharacteristic characteristic = getNotificationCharacteristic();
            characteristic.disableValueNotifications();
            logger.debug("Disable notification : " + characteristic.getUUID());
            notification = null;
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
}
