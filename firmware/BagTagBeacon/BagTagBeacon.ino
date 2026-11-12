#include <NimBLEDevice.h>
#include <string>

#define TAG_NAME "BagTag-01"
#define TAG_ID   "TAG001"

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.println("ESP32 BLE Tag Starting...");

  NimBLEDevice::init(TAG_NAME);

  NimBLEAdvertisementData advData;
  advData.setName(TAG_NAME);
  // Manufacturer data must start with a two-byte Bluetooth company ID. The
  // prototype uses the development/testing value 0xFFFF; Android then exposes
  // TAG001 as the manufacturer payload instead of dropping its first bytes.
  std::string manufacturerData;
  manufacturerData.push_back(static_cast<char>(0xFF));
  manufacturerData.push_back(static_cast<char>(0xFF));
  manufacturerData.append(TAG_ID);
  advData.setManufacturerData(manufacturerData);

  NimBLEAdvertising *advertising = NimBLEDevice::getAdvertising();
  advertising->setAdvertisementData(advData);
  advertising->setMinInterval(160);
  advertising->setMaxInterval(240);
  advertising->start();

  Serial.println("BLE advertising started!");
  Serial.print("Tag name: ");
  Serial.println(TAG_NAME);
  Serial.print("Tag ID: ");
  Serial.println(TAG_ID);
}

void loop() {
  delay(1000);
}
