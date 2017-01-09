# BLEOperator
測試硬體: Raspberry Pi3 & CC2650<br>
OS: 2016-11-25-raspbian-jessie<br>

利用RPi3預設藍牙晶片透過tinyb(java版的Bluetooth函式庫)連接Bluetooth裝置，因為tinyb編譯cmake版本需要大於3.1，底層C的藍牙函式庫bluez版本需要大於5.37。註：tinyb亦支援C++<br>

先安裝bluez需要的函式庫，然後因為RPi3預設的bluez版本為5.23，所以先移除掉。<br>
<pre>sudo apt-get update
sudo apt-get install -y libusb-dev libdbus-1-dev libglib2.0-dev libudev-dev libical-dev libreadline-dev glib2.0
sudo apt-get purge bluez<br></pre>

因為bluez5.39有針對Pi的補丁所以選擇5.39版。<br>
<pre>
cd ~
wget https://www.kernel.org/pub/linux/bluetooth/bluez-5.39.tar.xz
tar xvf bluez-5.39.tar.xz
cd bluez-5.39</pre>
下載bluez5.39補丁，然後安裝bluez。<br>
<pre>
wget https://gist.github.com/pelwell/c8230c48ea24698527cd/archive/3b07a1eb296862da889609a84f8e10b299b7442d.zip
unzip 3b07a1eb296862da889609a84f8e10b299b7442d.zip
git apply -v c8230c48ea24698527cd-3b07a1eb296862da889609a84f8e10b299b7442d/*
./configure --prefix=/usr --mandir=/usr/share/man --sysconfdir=/etc --localstatedir=/var --enable-experimental --with-systemdsystemunitdir=/lib/systemd/system --with-systemduserunitdir=/usr/lib/systemd
make -j4
sudo make install</pre>
因為底層驅動的關係，要做以下操作。<br>
<pre>
sudo systemctl unmask bluetooth.service
sudo systemctl start bluetooth
sudo cp /lib/firmware/BCM43430A1.hcd /lib/firmware/brcm/BCM43430A1.hcd
sudo reboot</pre>
重新啟動之後，啟動藍牙。<br>
<pre>
/usr/bin/hciattach /dev/ttyAMA0 bcm43xx 921600 noflow -
sudo hciconfig hci0 up</pre>
接著安裝CMake3.1+。<br>
<pre>
sudo apt-get install build-essential
cd ~
wget https://cmake.org/files/v3.7/cmake-3.7.1.tar.gz
tar xvf cmake-3.7.1.tar.gz
cd cmake-3.7.1
./configure
make -j4
sudo make install
</pre>
但因為CMake3.7.1的FindJNI找不到RPi Jessie預設的Java JNI路徑，所以改成CMake搜尋得到的路徑。<br>
<pre>sudo ln -s /usr/lib/jvm/jdk-8-oracle-arm32-vfp-hflt /usr/lib/jvm/default-java</pre>
下載tinyb的函式庫。<br>
<pre>
cd ~
wget https://github.com/intel-iot-devkit/tinyb/archive/0.5.0.tar.gz
tar xvf 0.5.0.tar.gz
cd tinyb-0.5.0
mkdir build
cd build</pre>
編譯Java版本的tinyb所以加上-DBUILDJAVA=ON參數。<br>
<pre>
sudo cmake -DBUILDJAVA=ON ..
sudo make
sudo make install</pre>
其他藍牙設定，修改/etc/dbus-1/system.d/bluetooth.conf，在&lt;policy context="default"&gt;加入。<br>
<pre>&lt;llow send_interface="org.bluez.GattService1"/&gt;
&lt;allow send_interface="org.bluez.GattCharacteristic1"/&gt;
&lt;allow send_interface="org.bluez.GattDescriptor1"/&gt;
</pre>
修改/lib/systemd/system/bluetooth.service<br>在ExecStart=/usr/libexec/bluetooth/bluetoothd後面加上--experimental。<br>
<pre>ExecStart=/usr/libexec/bluetooth/bluetoothd --experimental</pre>
因為pi原本的bluetooth啟動被移除了。所以重開之後都要以下操作。<br>
<pre>
/usr/bin/hciattach /dev/ttyAMA0 bcm43xx 921600 noflo
sudo systemctl restart bluetooth
sudo hciconfig hci0 up
</pre>
參考來源 tinyb : https://github.com/intel-iot-devkit/tinyb<br>
參考來源 RPi3 Bluez更新 : https://www.raspberrypi.org/forums/viewtopic.php?t=145364&p=1027963
