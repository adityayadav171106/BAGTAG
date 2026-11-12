
#include <NimBLEDevice.h>
#include <string>

#define TAG_NAME "BagTag-01"
#define TAG_ID   "TAG001"

void setup() {
  Serial.begin(115200);
  delay(1000);

  Serial.println();
  Serial.println("ESP32 BLE Tag Starting...");

  // Initialize Bluetooth Low Energy
  NimBLEDevice::init(TAG_NAME);

  // Create the advertisement packet
  NimBLEAdvertisementData advData;
  advData.setName(TAG_NAME);
  // Prefix prototype data with a two-byte testing company ID (0xFFFF).
  std::string manufacturerData;
  manufacturerData.push_back(static_cast<char>(0xFF));
  manufacturerData.push_back(static_cast<char>(0xFF));
  manufacturerData.append(TAG_ID);
  advData.setManufacturerData(manufacturerData);

  // Configure advertising
  NimBLEAdvertising *advertising =
      NimBLEDevice::getAdvertising();

  advertising->setAdvertisementData(advData);

  // BLE interval units are 0.625 milliseconds
  advertising->setMinInterval(160);  // 100 ms
  advertising->setMaxInterval(240);  // 150 ms

  // Start broadcasting the tag
  advertising->start();

  Serial.println("BLE advertising started!");
  Serial.print("Tag name: ");
  Serial.println(TAG_NAME);
  Serial.print("Tag ID: ");
  Serial.println(TAG_ID);
}

void loop() {
  // BLE advertising runs independently.
  delay(1000);
}
