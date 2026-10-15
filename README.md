# Thai Clock Fix

แอป Android สำหรับตั้ง locale ของ SystemUI และรูปแบบเวลา 24 ชั่วโมงผ่าน Shizuku โดยมีโหมดภาษาไทย พ.ศ. และภาษาอังกฤษ ค.ศ. แอปทำงานกับ **ผู้ใช้หลักของ Android (user 0)** และไม่ต้อง Root

## Requirements

- Android 13 ขึ้นไปที่รองรับ `cmd locale`
- Shizuku v11 ขึ้นไป (โปรเจกต์ใช้ Shizuku 13.1.5)
- Android SDK 37 สำหรับ build
- JDK 17 ขึ้นไป

อุปกรณ์/OEM บางรุ่นอาจไม่อนุญาตให้เปลี่ยน locale ของ SystemUI หรืออาจคืนค่าหลัง reboot/update ระบบ

## Build และตรวจสอบ

```sh
./gradlew --no-daemon \
  :app:spotlessCheck \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:lintRelease \
  :app:assembleDebug \
  :app:assembleRelease
```

Debug APK อยู่ที่ `app/build/outputs/apk/debug/app-debug.apk` ส่วน `assembleRelease` ที่ไม่มี signing environment จะสร้าง release artifact สำหรับตรวจ R8/resource shrinking เท่านั้น ไม่ควรนำ unsigned artifact ไปเผยแพร่

แอปไม่มี Gemini, Firebase AI, API key หรือ runtime service ของ Google AI Studio

## Package identity

Production application ID และ namespace คือ:

```text
com.thaiclockfix
```

รุ่นต้นแบบจาก AI Studio เคยใช้ `com.aistudio.thaiclockfix.pzkymq` ซึ่งเป็นคนละ package ดังนั้น APK production รุ่นแรกจะไม่อัปเดตทับรุ่นต้นแบบเดิมโดยอัตโนมัติ ให้ทดสอบรุ่นใหม่ให้เรียบร้อยก่อนถอนการติดตั้ง package เก่า

## ใช้งาน

1. ติดตั้งและเริ่ม Shizuku
2. เปิด Thai Clock Fix และอนุญาตสิทธิ์ใน Shizuku
3. เลือกรูปแบบภาษา แล้วกด **ใช้รูปแบบนี้**
4. แอปจะตั้งเวลา 24 ชั่วโมงและ locale ของ SystemUI จากนั้นอ่านค่ากลับก่อนแจ้งสำเร็จ
5. เมื่อต้องการคืนค่า ให้เลือก **Restore** เพื่อล้าง locale override ของ SystemUI

Restore ไม่คืนค่ารูปแบบ 12/24 ชั่วโมงเดิม และไม่มีการ restart SystemUI หรือ reboot อัตโนมัติ

## Release signing

Release build รับค่าต่อไปนี้จาก environment เท่านั้น:

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `THAI_CLOCK_FIX_VERSION_NAME` (optional สำหรับ local build)
- `THAI_CLOCK_FIX_VERSION_CODE` (optional สำหรับ local build)

ถ้ากำหนด signing variable มาไม่ครบ Gradle จะหยุด build แทนการสร้าง APK ด้วย configuration ที่คลุมเครือ ห้าม commit keystore หรือรหัสผ่านลง repository

## GitHub Release

CI อยู่ใน `.github/workflows/ci.yml` และตรวจ formatting, unit tests, lint, debug build และ R8 release build ทุก push/PR

การ push tag รูปแบบ `vMAJOR.MINOR.PATCH` จะเรียก `.github/workflows/release.yml` เพื่อ build, sign, ตรวจลายเซ็น และแนบ APK พร้อม SHA-256 checksum เข้า GitHub Release

ตั้ง GitHub Actions Secrets ก่อน release:

- `ANDROID_KEYSTORE_BASE64` — keystore ที่ encode เป็น Base64
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

ตัวอย่าง release:

```sh
git tag v1.0.0
git push origin v1.0.0
```

workflow แปลง semantic version เป็น Android `versionCode` แบบ `MAJOR * 1,000,000 + MINOR * 1,000 + PATCH` เพื่อให้การอัปเดต APK จาก GitHub มี versionCode เพิ่มตามเวอร์ชัน

## Play Protect และ sideload

APK ที่แจกผ่าน GitHub ต้องใช้ release key เดิมทุกเวอร์ชันและควรติดตั้งทดสอบบนอุปกรณ์จริงก่อนเผยแพร่ Google Play Protect อาจสแกนแอปที่มาจากนอก Play Store และผล reputation/scanning เป็นสิ่งที่แอปหรือ build script รับประกันล่วงหน้าไม่ได้ ดังนั้น release gate ของโปรเจกต์คือ: signature ถูกต้อง, lint/tests ผ่าน, ติดตั้งบนเครื่องจริงได้ และไม่มีการแจ้งเตือน/บล็อกที่พบระหว่างการทดสอบเครื่องจริง

## Build integrity

Gradle Wrapper pin ที่ 9.3.1 พร้อม `distributionSha256Sum` ใน wrapper configuration ส่วน GitHub Actions ใช้ `gradle/actions/setup-gradle` ซึ่งตรวจ Gradle Wrapper ให้ด้วย

ยังไม่ได้เปิด Gradle dependency verification สำหรับ Maven artifacts ทั้ง dependency graph; ก่อนเพิ่ม metadata ควรสร้างจาก dependency provenance ที่เชื่อถือได้ ไม่ควรรับ checksum จาก cache ที่ยังไม่ตรวจสอบ

## Known technical debt

การรัน shell ผ่าน Shizuku 13.1.5 ยังใช้ `Shizuku.newProcess` ผ่าน reflection ซึ่ง Shizuku ประกาศ deprecated และเตรียมถอดใน API รุ่นถัดไป ควรวาง migration ไป UserService ก่อนอัปเกรด Shizuku major version

Apply, restore และ readback ใช้คิวเดียวกัน มี timeout 10 วินาทีและจำกัด stdout/stderr อย่างละ 64 KiB แต่ synchronous Binder IPC บางช่วงไม่สามารถรับประกันว่าจะถูกตัดด้วย coroutine timeout ได้ และถ้าคำสั่งหนึ่งสำเร็จแต่คำสั่งถัดไปล้มเหลว ระบบอาจอยู่ในสถานะเปลี่ยนบางส่วนจนกว่าจะ readback/restore ใหม่
