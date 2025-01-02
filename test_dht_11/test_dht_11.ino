#include <WiFi.h>
#include <Firebase_ESP_Client.h>
//#include <addons/TokenHelper.h>  // Token generation helper
//#include <addons/RTDBHelper.h>   // RTDB payload printing helper
#include "DHT.h"
#include <SPI.h>
#include <U8g2lib.h>
#include <SoftwareSerial.h>
#include "ModbusMaster.h"
#include <WiFiManager.h> // https://github.com/tzapu/WiFiManager
#include <Wire.h>
#include <RTClib.h>
#include <WiFiUdp.h>
#include <NTPClient.h>

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "pool.ntp.org", 7 * 3600); // GMT+7, tùy chỉnh múi giờ
RTC_DS3231 rtc;
// Replace with your network credentials (STATION)
struct ConfigFb {
    const char* ssid = "realmeXT";
    const char* password = "11111112";
    const char* apiKey = "AIzaSyAXOBQg-Xjd1YWdyqhSUS_xJBB1bR_7wF0";
    const char* databaseUrl = "https://smart-greenhouse-powered-by-ai-default-rtdb.asia-southeast1.firebasedatabase.app/";
    const char* userEmail = "thanhnguyen2804t@gmail.com";
    const char* userPassword = "Thanhlong4az@";
};

struct DeviceState {
    int pump1 = 0;
    int pump2 = 0;
    int light = 0;
    int fan = 0;
    int isAuto = 1;
};

struct AiData {
    float lightingDuration = 0.0;
    float soilMoisture = 0.0; 
};

struct SensorData {
    float temperature = 0.0;
    float humidity = 0.0;
    float soilMoisturePercent = 0.0;
    float ph = 7.0;
    int lightSensorValue = 0;
};

struct TimerData {
    unsigned long getDeviceStatePreMillis = 0;
    unsigned long previousMillis = 0;  // Thời gian lần đọc cuối
    unsigned long lastTimeSync = 0;
    unsigned long lastTimeSyncTimeAndAiData = 0;
    int currentDay = -1;
};

struct TimeSettings {
    int autoFan;
    int autoLight;
    int autoWater;
    int autoWaterNutri;
    int timeFinishFan;
    int timeFinishLighting;
    int timeFinishWatering;
    int timeFinishWateringNutri;
    int timeStartFan;
    int timeStartLighting;
    int timeStartWatering;
    int timeStartWateringNutri;
};

struct SystemState {
    ConfigFb configFb;
    DeviceState device;
    SensorData sensor;
    TimerData timer;
    TimeSettings timeSetting;
    AiData aiData;
};

// Biến toàn cục duy nhất lưu toàn bộ trạng thái hệ thống
SystemState systemState;

// 3. Khai báo các chân GPIO
#define PUMP1_PIN 13   // GPIO cho tưới thông thường
#define PUMP2_PIN 33   // GPIO cho tưới phun sương
#define LIGHT_PIN 4  // GPIO cho đèn
#define FAN_PIN 19    // GPIO cho quạt
#define SOIL_SENSOR_PIN 32// GPIO cho cảm biến độ ẩm đất (analog)
#define DHT_PIN 17
#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321
#define LIGHT_SENSOR_PIN 35
#define SOFTWARE_SERIAL_TX_PIN 25
#define SOFTWARE_SERIAL_RX_PIN 26
SoftwareSerial mySerial(SOFTWARE_SERIAL_RX_PIN, SOFTWARE_SERIAL_TX_PIN);
ModbusMaster ph_sensor;
#define SYS_PRINT(_str) Serial.print(_str)
#define SYS_DBG_INIT(speed) Serial.begin(speed)
// Initialize DHT sensor.
DHT dht(DHT_PIN, DHTTYPE);

// 4. Khai báo các biến
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// Khởi tạo màn hình ST7920 128x64 với SPI phần mềm
U8G2_ST7920_128X64_F_SW_SPI u8g2(U8G2_R2, /*clock=*/ 18, /*data=*/ 23, /*CS=*/ 5, U8X8_PIN_NONE);

// Định nghĩa các mã glyph từ font thời tiết
#define TEMP 69        // Nhiệt độ
#define SOIL_MOISTURE 66      // Độ ẩm đất
#define HUMI 72 // Độ ẩm không khí 
#define PH 65    // Độ PH
#define WIFI_CONNECTED 247        // Biểu tượng Wi-Fi CONNECTED
#define WIFI_DISCONNECTED 240   

// Định nghĩa thiết bị 
#define PUMP1 1        
#define PUMP2 2      
#define LIGHT 3 
#define FAN 4   

void setup() {
    Serial.begin(4800);
    // Khởi tạo màn hình
    u8g2.begin();
    u8g2.enableUTF8Print();

    // Kết nối Wi-Fi
    initWiFi();
    //Serial.print("RSSI: ");
    
    // Cấu hình Firebase
    config.api_key = systemState.configFb.apiKey;
    auth.user.email = systemState.configFb.userEmail;
    auth.user.password = systemState.configFb.userPassword;
    config.database_url = systemState.configFb.databaseUrl;
    //config.token_status_callback = tokenStatusCallback;  // Theo dõi trạng thái token

    Firebase.begin(&config, &auth);
    Firebase.reconnectNetwork(true);  // Tự động kết nối lại khi mất kết nối

    // Cấu hình ph sensor
    SYS_DBG_INIT(4800);
    mySerial.begin(4800);
    ph_sensor.begin(1, mySerial);
    delay(1000);
    
    // Cấu hình dht22
    dht.begin();

    // Bắt đầu NTP Client
    timeClient.begin();
    
    // Khởi tạo thời gian hệ thống
    if (!rtc.begin()) {
      //Serial.println("Không tìm thấy RTC");
       while (1);
    }
    syncTimeWithRTC();
      
    // Cấu hình GPIO
    pinMode(LIGHT_SENSOR_PIN, INPUT);
    pinMode(SOIL_SENSOR_PIN, INPUT);
    pinMode(PUMP1_PIN, OUTPUT);
    pinMode(PUMP2_PIN, OUTPUT);
    pinMode(LIGHT_PIN, OUTPUT);
    pinMode(FAN_PIN, OUTPUT);
}

void syncTimeWithRTC() {
    // Lấy giờ từ NTP
    timeClient.update();

    // Chuyển giờ sang RTC
    unsigned long epochTime = timeClient.getEpochTime();
    rtc.adjust(DateTime(epochTime));
}

void initWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(systemState.configFb.ssid, systemState.configFb.password);
  unsigned long startAttemptTime = millis();
  while ((WiFi.status() != WL_CONNECTED) && (millis() - startAttemptTime < 15000)) {
    delay(500);
  }
  if (WiFi.status() != WL_CONNECTED) {
        manualConnectWifi();
    }
}

void manualConnectWifi(){
  WiFiManager wm;
  bool res;
  res = wm.autoConnect("AutoConnectAP"); // password protected ap
}

void reconectWifi(){
   if (WiFi.status() != WL_CONNECTED) {
        WiFi.disconnect();
        WiFi.reconnect();
   }
}

// Hàm vẽ biểu tượng dựa trên loại biểu tượng được truyền vào
void drawIconSymbol(u8g2_uint_t x, u8g2_uint_t y, uint8_t symbol) {
  switch (symbol) {
    case TEMP:
      u8g2.setFont(u8g2_font_open_iconic_weather_2x_t); 
      u8g2.drawGlyph(x, y, TEMP);  // Vẽ biểu tượng nhiệt độ
      break;
    case SOIL_MOISTURE:
      u8g2.setFont(u8g2_font_open_iconic_embedded_2x_t);  
      u8g2.drawGlyph(x, y, SOIL_MOISTURE);  // Vẽ biểu tượng độ ẩm đất
      break;
    case HUMI:
      u8g2.setFont(u8g2_font_open_iconic_thing_2x_t);  
      u8g2.drawGlyph(x, y, HUMI);  // Vẽ biểu tượng độ ẩm không khí
      break;
    case PH:
      u8g2.setFont(u8g2_font_open_iconic_human_2x_t); 
      u8g2.drawGlyph(x, y, PH);  // Vẽ biểu tượng độ PH
      break;
    case WIFI_CONNECTED:
      u8g2.setFont(u8g2_font_open_iconic_all_1x_t);  
      u8g2.drawGlyph(x, y, WIFI_CONNECTED);  // Vẽ biểu tượng Wi-Fi CONNECTED
      break;
    case WIFI_DISCONNECTED:
      u8g2.setFont(u8g2_font_open_iconic_all_1x_t);  
      u8g2.drawGlyph(x, y, WIFI_DISCONNECTED);  // Vẽ biểu tượng Wi-Fi DISCONNECTED
      break;
    default:
      u8g2.setFont(u8g2_font_open_iconic_weather_2x_t);  
      u8g2.drawGlyph(x, y, TEMP);  // Mặc định là mặt trời nếu không hợp lệ
      break;
  }
}

// Hàm in văn bản với biểu tượng thời tiết ở đầu dòng
void printWithSymbol(u8g2_uint_t x, u8g2_uint_t y, uint8_t symbol, const char *lable, float value) {
  drawIconSymbol(x, y, symbol);  // Vẽ biểu tượng theo mã glyph
  if(symbol == WIFI_CONNECTED){
    u8g2.setFont(u8g2_font_threepix_tr);
  } else {
    u8g2.setFont(u8g2_font_4x6_mf);  // Chọn font chữ nhỏ (5x8 pixel)
  }
  u8g2.setCursor(x + 18, y);  // Đặt con trỏ cách biểu tượng 20 pixel ngang
  u8g2.print(lable);  // In văn bản
  if(value != -1.0){
    u8g2.print(value);
  }
}

void displayOnLcd(boolean wifi_connected){
  u8g2.clearBuffer();  // Xóa bộ đệm
  // In văn bản với các biểu tượng khác nhau ở 4 góc màn hình
  if(wifi_connected == true){
    printWithSymbol(0, 10, WIFI_CONNECTED, "WIFI: Connected", -1.0);
  } else {
    printWithSymbol(0, 10, WIFI_DISCONNECTED, "WIFI: Disconnected", -1.0);
  }
  printWithSymbol(0, 30, TEMP, "TEMP: ", systemState.sensor.temperature);       // Góc trên bên trái 
  printWithSymbol(66, 30, SOIL_MOISTURE, "SOIL: ", systemState.sensor.soilMoisturePercent);    // Góc trên bên phải 
  printWithSymbol(0, 60, HUMI, "HUMI: ", systemState.sensor.humidity);     // Góc dưới bên trái 
  printWithSymbol(66, 60, PH, "PH: ", systemState.sensor.ph); // Góc dưới bên phải
  u8g2.sendBuffer();  // Gửi bộ đệm lên màn hình
}

float readPhSensor(){
   byte DataSend[8] = {0x01, 0x03, 0x00, 0x00, 0x00, 0x01, 0x84, 0x0A}; 
   mySerial.write(DataSend, 8); 
   delay(500);
   if (mySerial.available() > 0) {
     byte response[8]; 
     int bytesRead = mySerial.readBytes(response, 8);
     systemState.sensor.ph = ((response[3] << 8) | response[4]) / 10.0;

   } else {
     systemState.sensor.ph = 0;
   }
  delay(500);
  return systemState.sensor.ph;
}

//void measureLightingDuration(){
//  // Lấy thời gian hiện tại từ RTC
//  DateTime now = rtc.now();
//
//  // Kiểm tra nếu ngày đã thay đổi
//  if (now.day() != currentDay) {
//    currentDay = now.day(); // Cập nhật ngày mới
//    elapsedMillis = 0; // Reset thời gian đã đo
////    Serial.println("Đã sang ngày mới, reset elapsedMillis về 0");
//  }
//
//  // Bắt đầu đo thời gian nếu có ánh sáng đủ 
//  if (lightSensorValue <= 350) {
//    if (startMillis == 0) {
//      startMillis = millis(); // Lưu thời gian bắt đầu nếu chưa có
//    }
//  } else {
//    if (startMillis > 0) {
//      elapsedMillis += (unsigned long)(millis() - startMillis); // Cộng thời gian đã qua
//      startMillis = 0; // Reset startMillis
//    }
//  }
//}
void updateSensorData() {
    systemState.sensor.temperature = dht.readTemperature();
    systemState.sensor.humidity = dht.readHumidity();
    if (isnan(systemState.sensor.humidity) || isnan(systemState.sensor.temperature)) {
        systemState.sensor.humidity = 0;
        systemState.sensor.temperature = 0;
        return;
    }
    systemState.sensor.soilMoisturePercent = 80 - map(analogRead(SOIL_SENSOR_PIN), 0, 4095, 0, 100);
    systemState.sensor.lightSensorValue = analogRead(LIGHT_SENSOR_PIN);    
    systemState.sensor.ph = readPhSensor(); // Hàm riêng để đọc cảm biến PH
} 

void updateDeviceState() {
    if (Firebase.RTDB.getJSON(&fbdo, "/json/controller")) {
        FirebaseJsonData pump1Fb, pump2Fb, lightFb, fanFb, isAutoFb;

        fbdo.to<FirebaseJson>().get(pump1Fb, "turnOnPump1");
        fbdo.to<FirebaseJson>().get(pump2Fb, "turnOnPump2");
        fbdo.to<FirebaseJson>().get(lightFb, "turnOnLight");
        fbdo.to<FirebaseJson>().get(fanFb, "turnOnFan");
        fbdo.to<FirebaseJson>().get(isAutoFb, "isAuto");

        // Cập nhật trạng thái thiết bị
        systemState.device.pump1 = pump1Fb.to<int>();
        systemState.device.pump2 = pump2Fb.to<int>();
        systemState.device.light = lightFb.to<int>();
        systemState.device.fan = fanFb.to<int>();
        systemState.device.isAuto = isAutoFb.to<int>();

        // Giải phóng dữ liệu 
        pump1Fb.clear();
        pump2Fb.clear();
        lightFb.clear();
        fanFb.clear(); 
        isAutoFb.clear();
    } 
}

void autoMode() {
     if (systemState.sensor.soilMoisturePercent < systemState.aiData.soilMoisture && systemState.sensor.soilMoisturePercent != 0) {
         controlDevice(PUMP2, 1);  //
     } else {
         controlDevice(PUMP2, 0);  // Tắt bơm
     }
       DateTime now = rtc.now();
       float currentTime = now.hour() + now.minute() / 60.0;
       float endTime = 13.0 + systemState.aiData.lightingDuration;
       if (currentTime >= 13.0 && currentTime < endTime) {
          if (systemState.sensor.lightSensorValue >= 350) {
             controlDevice(LIGHT, 1); // Bật đèn
          } else {
               controlDevice(LIGHT, 0); // Tắt đèn khi ánh sáng không đủ
           }
       } else {
          controlDevice(LIGHT, 0); // Tắt đèn khi ngoài khoảng thời gian
       }  
}


void syncTimeSettingsFromFirebase() {
    if (Firebase.RTDB.getJSON(&fbdo, "/json/timeSetting")) {
        FirebaseJsonData timeFinishFanFb, timeFinishLightingFb, timeFinishWateringFb, timeFinishWateringNutriFb, timeStartFanFb, timeStartLightingFb, timeStartWateringFb, timeStartWateringNutriFb;
        FirebaseJsonData autoWater, autoWaterNutri, autoLight, autoFan;
        
        fbdo.to<FirebaseJson>().get(timeFinishFanFb, "timeFinishFan");
        fbdo.to<FirebaseJson>().get(timeFinishLightingFb, "timeFinishLighting");
        fbdo.to<FirebaseJson>().get(timeFinishWateringFb, "timeFinishWatering");
        fbdo.to<FirebaseJson>().get(timeFinishWateringNutriFb, "timeFinishWateringNutri");
        fbdo.to<FirebaseJson>().get(timeStartFanFb, "timeStartFan");
        fbdo.to<FirebaseJson>().get(timeStartLightingFb, "timeStartLighting");
        fbdo.to<FirebaseJson>().get(timeStartWateringFb, "timeStartWatering");
        fbdo.to<FirebaseJson>().get(timeStartWateringNutriFb, "timeStartWateringNutri");

        fbdo.to<FirebaseJson>().get(autoWater, "autoWater");
        fbdo.to<FirebaseJson>().get(autoWaterNutri, "autoWaterNutri");
        fbdo.to<FirebaseJson>().get(autoLight, "autoLight");
        fbdo.to<FirebaseJson>().get(autoFan, "autoFan");  
             
        systemState.timeSetting.autoWater = autoWater.to<int>();
        systemState.timeSetting.autoWaterNutri = autoWaterNutri.to<int>();
        systemState.timeSetting.autoLight = autoLight.to<int>();
        systemState.timeSetting.autoFan = autoFan.to<int>(); 
        
        systemState.timeSetting.timeFinishFan = timeFinishFanFb.to<int>();
        systemState.timeSetting.timeFinishLighting = timeFinishLightingFb.to<int>();
        systemState.timeSetting.timeFinishWatering = timeFinishWateringFb.to<int>();
        systemState.timeSetting.timeFinishWateringNutri = timeFinishWateringNutriFb.to<int>();
        systemState.timeSetting.timeStartFan = timeStartFanFb.to<int>();
        systemState.timeSetting.timeStartLighting = timeStartLightingFb.to<int>();
        systemState.timeSetting.timeStartWatering = timeStartWateringFb.to<int>();
        systemState.timeSetting.timeStartWateringNutri = timeStartWateringNutriFb.to<int>();

        // Giải phóng dữ liệu
        timeFinishFanFb.clear();
        timeFinishLightingFb.clear();
        timeFinishWateringFb.clear();
        timeFinishWateringNutriFb.clear();
        timeStartFanFb.clear(); 
        timeStartLightingFb.clear(); 
        timeStartWateringFb.clear(); 
        timeStartWateringNutriFb.clear();
        autoWater.clear(); 
        autoWaterNutri.clear(); 
        autoLight.clear(); 
        autoFan.clear();
    }
}

void manualMode() {
    if (systemState.timeSetting.autoWater == 0) {
      controlDevice(PUMP1, systemState.device.pump1);
    }
    if (systemState.timeSetting.autoWaterNutri == 0) {
      controlDevice(PUMP2, systemState.device.pump2);
    }
    if (systemState.timeSetting.autoLight == 0) {
      controlDevice(LIGHT, systemState.device.light);
    }
    if (systemState.timeSetting.autoFan == 0) {
      controlDevice(FAN, systemState.device.fan);
    }
    
}

void setUpTimeAutoControl() {
    // Kiểm tra tự động bật tưới nước
    if (systemState.timeSetting.autoWater == 1) {
        if (isWithinAutoTime(systemState.timeSetting.timeStartWatering, systemState.timeSetting.timeFinishWatering)) {
            // Trong thời gian cài đặt: khóa thiết bị, bật theo lịch trình
            controlDevice(PUMP1, 1); // Bật thiết bị tưới nước theo lịch trình
        } else {
            // Ngoài thời gian cài đặt: cho phép điều khiển thủ công
            controlDevice(PUMP1, 0); // Tắt thiết bị tưới nước
        }
    }

    // Kiểm tra tự động bật tưới nước dinh dưỡng
    if (systemState.timeSetting.autoWaterNutri == 1) {
        if (isWithinAutoTime(systemState.timeSetting.timeStartWateringNutri, systemState.timeSetting.timeFinishWateringNutri)) {
            controlDevice(PUMP2, 1); // Bật thiết bị tưới nước dinh dưỡng theo lịch trình
        } else {
            controlDevice(PUMP2, 0); // Tắt thiết bị tưới nước dinh dưỡng
        }
    }

    // Kiểm tra tự động bật đèn
    if (systemState.timeSetting.autoLight == 1) {
        if (isWithinAutoTime(systemState.timeSetting.timeStartLighting, systemState.timeSetting.timeFinishLighting)) {
            controlDevice(LIGHT, 1); // Bật đèn theo lịch trình
        } else {
            controlDevice(LIGHT, 0); // Tắt đèn
        }
    }

    // Kiểm tra tự động bật quạt
    if (systemState.timeSetting.autoFan == 1) {
        if (isWithinAutoTime(systemState.timeSetting.timeStartFan, systemState.timeSetting.timeFinishFan)) {
            controlDevice(FAN, 1); // Bật quạt theo lịch trình
        } else {
            controlDevice(FAN, 0); // Tắt quạt
        }
    }
}


bool isWithinAutoTime(int startTime, int finishTime) {
    // Nếu thời gian bắt đầu và kết thúc giống nhau, thiết bị không bao giờ bật
        if (startTime == finishTime) {
           return false;
         }

        DateTime now = rtc.now();
        int currentTime = now.hour() * 100 + now.minute(); // Chuyển giờ hiện tại sang định dạng HHMM
        if (startTime < finishTime) {
             return (currentTime >= startTime && currentTime < finishTime);
        } else {
          return (currentTime >= startTime || currentTime < finishTime);
        }
}

void controlDevice(int type, int statusDevice){
  if(statusDevice == 0 || statusDevice == 1){
      switch(type){
        case PUMP1: // Điều khiển bơm tưới thông thường
           if(statusDevice == 1){
                analogWrite(PUMP1_PIN, 400);              
           } else{
                analogWrite(PUMP1_PIN, 0);
           }           
           break;
        case PUMP2:
           if(statusDevice == 1){
                analogWrite(PUMP2_PIN, 1600);
           } else{
                analogWrite(PUMP2_PIN, 0);
           }           
           break;
        case LIGHT:
           if(statusDevice == 1){
              if(digitalRead(LIGHT_PIN) == 0){
                digitalWrite(LIGHT_PIN, 1);
              }
           } else{
              if(digitalRead(LIGHT_PIN) == 1){
                digitalWrite(LIGHT_PIN, 0);
              }
           }    
           break;
        case FAN: 
           if(statusDevice == 1){
              if(digitalRead(FAN_PIN) == 0){
                digitalWrite(FAN_PIN, 1);
              }
           } else{
              if(digitalRead(FAN_PIN) == 1){
                digitalWrite(FAN_PIN, 0);
              }
           }    
           break;
     }
  } 
}

void loop() {
    // Kiểm tra nếu đã qua 5 giây kể từ lần đọc cuối    
    if ((unsigned long)(millis() - systemState.timer.previousMillis) >= 6000 || systemState.timer.previousMillis == 0) {
          systemState.timer.previousMillis = millis();  // Cập nhật thời gian lần đọc cuối
          updateSensorData();
          
          if (WiFi.status() != WL_CONNECTED) {
            autoMode();
            displayOnLcd(false);
            reconectWifi();
          } 
          else if(WiFi.status() == WL_CONNECTED) {
             displayOnLcd(true);             
          }
    }
        
    if (Firebase.ready() && ((unsigned long)(millis() - systemState.timer.getDeviceStatePreMillis) > 1000 || systemState.timer.getDeviceStatePreMillis == 0)) {
        systemState.timer.getDeviceStatePreMillis = millis();             
        updateDeviceState();  
        if (WiFi.status() != WL_CONNECTED) {
            autoMode();
         } 
          else if(WiFi.status() == WL_CONNECTED) {
             if (systemState.device.isAuto == 1) {
                autoMode();
              } else {
                  manualMode();
                  setUpTimeAutoControl();
               }     
          }         
    }
    
    if (Firebase.ready() && ((unsigned long)(millis() - systemState.timer.lastTimeSync ) > 6000 || systemState.timer.lastTimeSync == 0)) {
        // Đẩy dữ liệu từ cảm biến lên firebase
        systemState.timer.lastTimeSync  = millis();
          
        DateTime now = rtc.now();        
        FirebaseJson dataFromSensor;
        dataFromSensor.add("soilMoisture", systemState.sensor.soilMoisturePercent);
        dataFromSensor.add("ph", systemState.sensor.ph);
        dataFromSensor.add("humidity", systemState.sensor.humidity);
        dataFromSensor.add("temperature", systemState.sensor.temperature);
        dataFromSensor.add("currentTime", now.hour()*10000 + now.minute()*100 + now.second());
        dataFromSensor.add("lightSensorValue", systemState.sensor.lightSensorValue);
        if (Firebase.RTDB.setJSON(&fbdo, "/json/dataFromSensor", &dataFromSensor)) {
            dataFromSensor.clear();
        }
        
        
      }
      
      if (Firebase.ready() && ((unsigned long)(millis() - systemState.timer.lastTimeSyncTimeAndAiData ) > 20000 || systemState.timer.lastTimeSyncTimeAndAiData  == 0)) {
        systemState.timer.lastTimeSyncTimeAndAiData  = millis();
        // Lấy dữ liệu từ chat gpt 
        Firebase.RTDB.getJSON(&fbdo, "/json/dataFromAI");
        FirebaseJsonData lightingDurationFb, soilMoistureFb;
        fbdo.to<FirebaseJson>().get(lightingDurationFb, "lightingDuration");
        fbdo.to<FirebaseJson>().get(soilMoistureFb, "soilMoisture");   
        systemState.aiData.soilMoisture = soilMoistureFb.to<float>();
        systemState.aiData.lightingDuration = lightingDurationFb.to<float>();
        
        // Giải phóng bộ nhớ của FirebaseJsonData
        lightingDurationFb.clear();
        soilMoistureFb.clear();
        
        // Lấy thời gian bật tắt tự động       
        syncTimeSettingsFromFirebase();
      }        
    delay(100);
}
