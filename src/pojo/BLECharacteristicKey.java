package pojo;

/**
 * Created by IDIC on 2017/1/12.
 */
public class BLECharacteristicKey {

    private String mac;
    private String uuid;

    public static BLECharacteristicKey of(String mac, String uuid) {
        return new BLECharacteristicKey(mac, uuid);
    }

    private BLECharacteristicKey(String mac, String uuid) {
        this.mac = mac;
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BLECharacteristicKey &&
                ((BLECharacteristicKey) obj).mac.equals(mac) &&
                ((BLECharacteristicKey) obj).uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        return (String.valueOf(mac) + String.valueOf(uuid)).hashCode();
    }
}
