import adapter.BLEInstance;
import adapter.BLENotificationServiceAdapter;
import pojo.ExtendedBluetoothDevice;
import sensortag.*;
import tinyb.BluetoothGattService;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by IDIC on 2016/12/9.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        BLEInstance instance = BLEInstance.getInstance();
        System.out.println(instance.startScan() ? "BLE model start discovering." : "BLE model cannot start discovering.");
        ExtendedBluetoothDevice tempDevice = null;
        List<Task> tasks = new LinkedList<>();

        Thread.sleep(2000);
        for (ExtendedBluetoothDevice device : instance.attachScanResult().getScanResult()) {
            if (device.getContent().getName().equals("CC2650 SensorTag") || device.getContent().getName().equals("SensorTag 2.0")) {
                tempDevice = device;
                System.out.println(instance.connectDevice(device.getContent()) ? "SensorTag is connecting..." : "SensorTag is not connecting...");
                while (!device.getContent().getConnected()){}
                System.out.println("SensorTag is connected...");
                Thread.sleep(2000);
                for (BluetoothGattService service : device.getContent().getServices()) {
                    if (isAdapterExists(service)) {
                        Task task = new Task(service);
                        tasks.add(task);
                        new Thread(task).start();
                    }
                }
            }
        }
        boolean stop;
        do {
            Thread.sleep(1000);
            stop = true;
            for (Task task : tasks) {
                if (task.adapter == null || !task.isReady) {
                    stop = false;
                    break;
                }
                if (task.counter >= 5) task.adapter.stop();
                stop &= !task.adapter.isRunning();
            }
        } while (!stop);

        for (Task task : tasks)
            System.out.printf("BLE Model %s stop : %s\n", task.adapter.getClass().getName(), task.adapter.stop());
        if (tempDevice != null)
            System.out.println(instance.disconnectDevice(tempDevice.getContent()) ? "SensorTag is disconnected..." : "SensorTag is not disconnected...");
        System.out.println(instance.stopScan() ? "BLE model stop discovering." : "BLE model cannot stop discovering.");
        instance.stop();
    }

    private static BLENotificationServiceAdapter generateAdapter(BluetoothGattService service) {
        switch (service.getUUID()) {
            case SensorTagTemperatureServiceAdapter.SERVICE_UUID:
                return new SensorTagTemperatureServiceAdapter(service);
            case SensorTagOpticalServiceAdapter.SERVICE_UUID:
                return new SensorTagOpticalServiceAdapter(service);
            case SensorTagMovementServiceAdapter.SERVICE_UUID:
                return new SensorTagMovementServiceAdapter(service);
            case SensorTagHumidityServiceAdapter.SERVICE_UUID:
                return new SensorTagHumidityServiceAdapter(service);
            case SensorTagBarometricPressureServiceAdapter.SERVICE_UUID:
                return new SensorTagBarometricPressureServiceAdapter(service);
            case SensorTagKeyServiceAdapter.SERVICE_UUID:
                return new SensorTagKeyServiceAdapter(service);
            default:
                return null;
        }
    }

    private static boolean isAdapterExists(BluetoothGattService service) {
        return service.getUUID().equals(SensorTagTemperatureServiceAdapter.SERVICE_UUID) ||
                service.getUUID().equals(SensorTagOpticalServiceAdapter.SERVICE_UUID) ||
                service.getUUID().equals(SensorTagMovementServiceAdapter.SERVICE_UUID) ||
                service.getUUID().equals(SensorTagHumidityServiceAdapter.SERVICE_UUID) ||
                service.getUUID().equals(SensorTagBarometricPressureServiceAdapter.SERVICE_UUID) ||
                service.getUUID().equals(SensorTagKeyServiceAdapter.SERVICE_UUID);
    }

    private static class Task implements Runnable {

        private boolean isReady = false;
        private BLENotificationServiceAdapter adapter;
        private BluetoothGattService service;
        private int counter = 0;

        Task(BluetoothGattService service) {
            this.service = service;
        }

        void increment() {
            counter++;
        }

        @Override
        public void run() {
            adapter = generateAdapter(service);
            isReady = true;
            adapter.run(data -> increment());
        }
    }

}
