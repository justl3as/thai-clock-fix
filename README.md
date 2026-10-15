# Thai Clock Fix

แอป Android สำหรับตั้งวันที่และเวลา SystemUI เป็นภาษาไทยแบบ พ.ศ. หรือภาษาอังกฤษ พร้อมรูปแบบเวลา 24 ชั่วโมง ผ่าน Shizuku

ใช้กับ **ผู้ใช้หลักของ Android (user 0)** ทั้งการตั้งภาษาและรูปแบบเวลา ไม่ได้เลือกตาม work profile หรือ secondary user ขณะเปิดแอป ต้องใช้ Android 13 ขึ้นไปที่รองรับ `cmd locale` และ Shizuku v11 ขึ้นไป (dependency ที่ใช้คือ 13.1.5) อุปกรณ์/OEM บางรุ่นอาจไม่อนุญาตให้ตั้งภาษาของ SystemUI

## เปิดโปรเจกต์และทดสอบ

ต้องมี Android SDK 36.1, JDK ที่รองรับ Gradle/AGP และ Android Studio หรือ command line tools

```sh
cd /Users/pongdanai/Project/thai-clock-fix
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

ถ้ามี dependencies ครบใน cache แล้ว สามารถเพิ่ม `--offline` ได้ APK สำหรับพัฒนาอยู่ที่ `app/build/outputs/apk/debug/app-debug.apk`

แอปไม่ใช้ Gemini/Firebase AI และไม่ต้องใช้ API key หรือไฟล์ `.env` ไม่มีการนำ environment properties ไปสร้าง secret constants ใน APK

## ใช้งาน

1. เปิด Shizuku และเริ่ม service ตามคำแนะนำของ Shizuku
2. เปิด Thai Clock Fix และให้สิทธิ์ Shizuku
3. เลือกรูปแบบภาษา แล้วกดใช้รูปแบบนี้ แอปจะตรวจผลคำสั่งและอ่านภาษา/รูปแบบเวลา 24 ชั่วโมงกลับก่อนแจ้งสำเร็จ
4. เมื่อต้องการคืนค่า เลือกเมนู Restore แล้วยืนยัน แอปจะล้างเฉพาะ locale override ของ SystemUI ให้กลับไปใช้ภาษาระบบ **ไม่คืนค่า 12/24 ชั่วโมงเดิม** และไม่รับประกันว่าตัวย่อหรือการแสดงผลเฉพาะ OEM จะกลับเป็นรูปแบบใด

Apply, restore และ readback ใช้คิวเดียวกัน หลังสร้าง process แล้ว การรอคำสั่งและอ่าน output มี timeout 10 วินาที และจำกัด stdout/stderr อย่างละ 64 KiB timeout นี้ไม่สามารถรับประกันการตัด synchronous Binder IPC ที่ค้างใน Shizuku server ได้ ข้อผิดพลาดหลังเปลี่ยนค่าไปบางส่วนจะไม่ rollback อัตโนมัติ ให้ตรวจสถานะอีกครั้งหรือใช้ Restore หลังแก้ปัญหาสิทธิ์แล้ว

ไม่มีคำสั่ง restart SystemUI/reboot อัตโนมัติ หาก Shizuku หยุดหรือถอนสิทธิ์ แอปต้องได้รับสิทธิ์ใหม่ก่อนเริ่มคำสั่งต่อไป

## Release signing

Release ใช้ `signingConfigs.release` โดยรับ `KEYSTORE_PATH`, `STORE_PASSWORD` และ `KEY_PASSWORD` จาก environment ของผู้ build ใช้ alias `upload` และ default path `my-upload-key.jks` ห้ามแจก debug APK เป็น production release หรือใส่ signing credentials ลงซอร์ส

## Build integrity

Gradle wrapper pin รุ่น 9.3.1 และ `distributionSha256Sum` ตาม [checksum ที่ Gradle เผยแพร่](https://gradle.org/release-checksums/#v9.3.1) checksum จะตรวจเมื่อ wrapper ดาวน์โหลด distribution ใหม่ ส่วน distribution ที่อยู่ใน cache แล้วควรตรวจ provenance แยกต่างหาก

ยังไม่ได้ตั้ง Gradle dependency verification สำหรับ transitive artifacts การสร้าง metadata จาก cache เพียงอย่างเดียวไม่ถือเป็นการตรวจความแท้จริง ต้องทวนกับ checksum/signature จากแหล่งที่เชื่อถือก่อนเปิดใช้ใน release process
