import adapter.BLENotificationServiceAdapter;
import sensortag.*;
import tinyb.BluetoothDevice;
import tinyb.BluetoothGattService;
import tinyb.BluetoothManager;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by IDIC on 2016/12/9.
 */
public class Main {

    static void printDevice(BluetoothDevice device) {
        System.out.print("Address = " + device.getAddress());
        System.out.print(" Name = " + device.getName());
        System.out.print(" Connected = " + device.getConnected());
        System.out.println();
    }

    public static void main(String[] args) throws InterruptedException {
        BluetoothManager manager = BluetoothManager.getBluetoothManager();
        System.out.println(manager.startDiscovery() ? "BLE model start discovering." : "BLE model cannot start discovering.");
        List<BluetoothDevice> list = manager.getDevices();
        BluetoothDevice tempDevice = null;
        List<Task> tasks = new LinkedList<>();

        Thread.sleep(2000);
        for (BluetoothDevice device : list) {
            printDevice(device);
            if (device.getName().equals("CC2650 SensorTag") || device.getName().equals("SensorTag 2.0")) {
                tempDevice = device;
                System.out.println(device.connect() ? "SensorTag is connected..." : "SensorTag is not connected...");
                Thread.sleep(2000);
                for (BluetoothGattService service : device.getServices()) {
                    System.out.println("Find service : " + service.getUUID());
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
            System.out.println(tempDevice.disconnect() ? "SensorTag is disconnected..." : "SensorTag is not disconnected...");
        System.out.println(manager.stopDiscovery() ? "BLE model stop discovering." : "BLE model cannot stop discovering.");
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
