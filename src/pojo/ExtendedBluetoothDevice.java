package pojo;


import tinyb.BluetoothDevice;

import java.util.Date;

/**
 * Created by IDIC on 2017/1/7.
 */
public class ExtendedBluetoothDevice {

    private BluetoothDevice content;
    private Date timestamp;

    public ExtendedBluetoothDevice(BluetoothDevice content, Date timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        return content.getAddress().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null &&
                obj instanceof ExtendedBluetoothDevice &&
                ((ExtendedBluetoothDevice) obj).content.getAddress().equals(content.getAddress());
    }

    public void setContent(BluetoothDevice content) {
        this.content = content;
    }

    public BluetoothDevice getContent() {
        return content;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
