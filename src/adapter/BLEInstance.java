package adapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pojo.BLECharacteristicKey;
import pojo.ExtendedBluetoothDevice;
import tinyb.BluetoothDevice;
import tinyb.BluetoothGattCharacteristic;
import tinyb.BluetoothManager;

import java.util.*;

/**
 * Created by IDIC on 2017/1/6.
 */
public class BLEInstance {

    private static final Logger logger = LogManager.getLogger(BLEInstance.class);

    private ExecuteThread execThread;
    private BluetoothManager manager;
    private static Map<String, ExtendedBluetoothDevice> scanResult = new HashMap<>();
    private static List<String> scanResultIndexes = new LinkedList<>();
    private static BLEInstance instance = new BLEInstance();

    public static BLEInstance getInstance() {
        return instance;
    }

    private BLEInstance() {
        manager = BluetoothManager.getBluetoothManager();
        execThread = new ExecuteThread();
        execThread.start();
    }

    public boolean stop() {
        execThread.isRunning = false;
        return true;
    }

    private static boolean isDiscovering = false;

    public boolean startScan() {
        if (!isDiscovering) {
            isDiscovering = execThread.addTask(new StartScanOperation());
        }
        return isDiscovering;
    }

    public boolean stopScan() {
        if (!isDiscovering) {
            isDiscovering = !execThread.addTask(new StopScanOperation());
        }
        return !isDiscovering;
    }

    public synchronized List<ExtendedBluetoothDevice> getScanResult() {
        List<ExtendedBluetoothDevice> devices = new LinkedList<>();
        for (String mac : scanResultIndexes) devices.add(scanResult.get(mac));
        return devices;
    }

    public synchronized BLEInstance attachScanResult() {
        for (BluetoothDevice device : manager.getDevices()) {
            String mac = device.getAddress();
            logger.info("Find device : {}", mac);
            if (!scanResult.containsKey(mac)) scanResultIndexes.add(mac);
            scanResult.put(mac, new ExtendedBluetoothDevice(device, new Date()));
        }
        return this;
    }

    public boolean connectDevice(BluetoothDevice device) {
        if (!device.getConnected()) {
            execThread.addTask(new ConnectOperation(device));
            return true;
        }
        return device.getConnected();
    }

    public boolean disconnectDevice(BluetoothDevice device) {
        if (device.getConnected()) {
            execThread.addTask(new DisconnectOperation(device));
            return true;
        }
        return false;
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        return execThread.addTask(new WriteOperation(characteristic, value));
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic characteristic, BLEServiceCallback<byte[]> listener) {
        return execThread.addTask(new ReadOperation(listener, characteristic));
    }

    private Map<BLECharacteristicKey, List<BLEServiceCallback<byte[]>>> notificationResources = new HashMap<>();

    public boolean notifyCharacteristic(String mac, BluetoothGattCharacteristic characteristic, BLEServiceCallback<byte[]> listener) {
        List<BLEServiceCallback<byte[]>> callbacks = notificationResources.get(BLECharacteristicKey.of(mac, characteristic.getUUID()));
        logger.info("{} is already registered. : {}", characteristic.getUUID(), callbacks != null);
        if (null == callbacks) {
            callbacks = new LinkedList<>();
            notificationResources.put(BLECharacteristicKey.of(mac, characteristic.getUUID()), callbacks);
            execThread.addTask(new NotifyOperation(characteristic, callbacks));
        }
        callbacks.add(listener);
        return true;
    }

    public boolean disableNotifyCharacteristic(String mac, BluetoothGattCharacteristic characteristic, BLEServiceCallback<byte[]> listener) {
        List<BLEServiceCallback<byte[]>> callbacks = notificationResources.get(BLECharacteristicKey.of(mac, characteristic.getUUID()));
        if (callbacks == null) return false;
        callbacks.remove(listener);
        if (callbacks.size() == 0) {
            execThread.addTask(new DisableNotifyOperation(characteristic));
            notificationResources.remove(BLECharacteristicKey.of(mac, characteristic.getUUID()));
        }
        return true;
    }

    private class ExecuteThread extends Thread {

        final List<BLEOperation> queue = new LinkedList<>();
        boolean isRunning = true;

        @Override
        public void run() {
            while (true) {
                if (queue.size() > 0)
                    synchronized (queue) {
                        BLEOperation operation = queue.remove(0);
                        try {
                            logger.info("{} is finished : {}", operation.getClass().getSimpleName(), operation.operate());
                        } catch (Exception e) {
                            logger.error("{} occurs error : {}", operation.getClass().getSimpleName(), e.getMessage());
                        }
                    }
                else {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    logger.info("BLE background is waiting...");
                }
                if (!isRunning && queue.size() <= 0) break;
            }
        }

        synchronized boolean addTask(BLEOperation operation) {
            return queue.add(operation);
        }
    }

    private class StartScanOperation extends BLEOperation<Boolean> {

        @Override
        public Boolean operate() {
            boolean isStartDiscover = manager.startDiscovery();
            logger.info("Start discover {}", isStartDiscover);
            return isStartDiscover;
        }

    }

    private class StopScanOperation extends BLEOperation<Boolean> {

        @Override
        public Boolean operate() {
            boolean isStopDiscover = manager.stopDiscovery();
            logger.info("Stop discover {}", isStopDiscover);
            return isStopDiscover;
        }

    }

    private class ConnectOperation extends BLEOperation<Boolean> {

        private BluetoothDevice device;

        ConnectOperation(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public Boolean operate() {
            boolean isConnected = device.connect();
            logger.info("Connect {} : {}", device.getAddress(), isConnected);
            return isConnected;
        }

    }

    private class DisconnectOperation extends BLEOperation<Boolean> {

        private BluetoothDevice device;

        DisconnectOperation(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public Boolean operate() {
            boolean isDisconnected = device.disconnect();
            logger.info("Disconnect {} : {}", device.getAddress(), isDisconnected);
            return isDisconnected;
        }

    }

    private class WriteOperation extends BLEOperation<Boolean> {

        private BluetoothGattCharacteristic characteristic;
        private byte[] value;

        WriteOperation(BluetoothGattCharacteristic characteristic, byte[] value) {
            this.characteristic = characteristic;
            this.value = value;
        }

        @Override
        public Boolean operate() {
            logger.info("write {} {}", characteristic.getUUID(), value);
            return characteristic.writeValue(value);
        }

    }

    private class ReadOperation extends BLEOperation<Boolean> {

        private BLEServiceCallback<byte[]> callback;
        private BluetoothGattCharacteristic characteristic;

        ReadOperation(BLEServiceCallback<byte[]> callback, BluetoothGattCharacteristic characteristic) {
            this.callback = callback;
            this.characteristic = characteristic;
        }

        @Override
        public Boolean operate() {
            byte[] value = characteristic.readValue();
            logger.info("read {} {}", characteristic.getUUID(), value);
            callback.supply(value);
            return true;
        }

    }


    private class NotifyOperation extends BLEOperation<Boolean> {

        private BluetoothGattCharacteristic characteristic;
        private List<BLEServiceCallback<byte[]>> callbacks;

        NotifyOperation(BluetoothGattCharacteristic characteristic, List<BLEServiceCallback<byte[]>> callbacks) {
            this.characteristic = characteristic;
            this.callbacks = callbacks;
        }

        @Override
        public Boolean operate() {
            try {
                characteristic.enableValueNotifications(bytes -> {
                    for (BLEServiceCallback<byte[]> callback : callbacks) callback.supply(bytes);
                });
                logger.info("Notify {}", characteristic.getUUID());
                return true;
            } catch (Exception e) {
                return false;
            }
        }

    }

    private class DisableNotifyOperation extends BLEOperation<Boolean> {

        private BluetoothGattCharacteristic characteristic;

        DisableNotifyOperation(BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
        }

        @Override
        public Boolean operate() {
            try {
                characteristic.disableValueNotifications();
                logger.info("Disable notify {}", characteristic.getUUID());
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

}
