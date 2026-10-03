#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

// Match these UUIDs with com.rakshax.app.data.ble.BleProtocol.
static const char *SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
static const char *SOS_CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
static constexpr uint8_t BUTTON_PIN = 4;
static constexpr unsigned long HOLD_TIME_MS = 2000;
static constexpr unsigned long DEBOUNCE_MS = 40;

BLECharacteristic *sosCharacteristic;
bool deviceConnected = false;
unsigned long pressedAt = 0;
unsigned long lastEdgeAt = 0;
bool sosSentForPress = false;

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *) override { deviceConnected = true; }

  void onDisconnect(BLEServer *server) override {
    deviceConnected = false;
    server->getAdvertising()->start();
  }
};

void setup() {
  Serial.begin(115200);
  pinMode(BUTTON_PIN, INPUT_PULLUP);

  BLEDevice::init("RakshaX-SOS-01");
  BLEServer *server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService *service = server->createService(SERVICE_UUID);
  sosCharacteristic = service->createCharacteristic(
      SOS_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  sosCharacteristic->addDescriptor(new BLE2902());
  sosCharacteristic->setValue("READY");
  service->start();

  BLEAdvertising *advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->start();
}

void loop() {
  const bool isPressed = digitalRead(BUTTON_PIN) == LOW;
  const unsigned long now = millis();

  if (isPressed && pressedAt == 0 && now - lastEdgeAt > DEBOUNCE_MS) {
    pressedAt = now;
    sosSentForPress = false;
    lastEdgeAt = now;
  }

  if (!isPressed && pressedAt != 0 && now - lastEdgeAt > DEBOUNCE_MS) {
    pressedAt = 0;
    sosSentForPress = false;
    lastEdgeAt = now;
  }

  if (isPressed && pressedAt != 0 && !sosSentForPress && now - pressedAt >= HOLD_TIME_MS) {
    sosSentForPress = true;
    if (deviceConnected) {
      sosCharacteristic->setValue("SOS");
      sosCharacteristic->notify();
    }
    Serial.println("SOS button held for 2 seconds");
  }

  delay(10);
}
