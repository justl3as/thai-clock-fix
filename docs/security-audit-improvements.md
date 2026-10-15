# การปรับปรุงจาก security audit — 15 ตุลาคม 2026

## สิ่งที่แก้

- Apply/restore ตรวจทั้ง exit code, inner result และผลอ่านกลับก่อนบันทึกหรือแจ้งสำเร็จ ไม่ตีความ stdout ว่าง/ข้อความผิดรูป/substring เป็นผลที่ยืนยันแล้ว
- Apply อ่านรูปแบบเวลา 24 ชั่วโมงกลับด้วย ทุกคำสั่ง setting/locale ระบุ owner user 0
- ViewModel ใช้ mutex ร่วมสำหรับ mutation และ readback ตรวจ readiness เมื่อถึงคิว และคืน busy state ใน finally พร้อมส่งต่อ cancellation
- Process runner อ่าน stdout/stderr พร้อมกัน จำกัดอย่างละ 64 KiB มี timeout 10 วินาทีหลังสร้าง process และทำลาย/ปิด stream เมื่อจบ ล้มเหลว หรือถูกยกเลิก
- ตรวจ Binder และสิทธิ์ SDK ก่อนทุก process; permission callback refresh สถานะ แทนตั้ง READY โดยตรง; ไม่ถือว่า legacy Shizuku pre-v11 ได้สิทธิ์อัตโนมัติ
- UI เปรียบเทียบ locale แบบตรงค่า และข้อความ Restore ระบุ owner user/ไม่คืนค่า 12-24 ชั่วโมงเดิม
- ลบ secrets Gradle plugin และคำแนะนำ Gemini key ที่ไม่ได้ใช้ พร้อมลบ metadata capability ที่อ้าง server-side Gemini
- เพิ่ม Gradle 9.3.1 distribution SHA-256 จาก [official checksum reference](https://gradle.org/release-checksums/); wrapper JAR เดิม hash ตรงกับ reference เช่นกัน

## การตรวจสอบ

Tests ใช้ shell response fixtures, local disposable sleep processes และ Robolectric/DataStore จริง โดยไม่เข้าถึงอุปกรณ์หรือ Shizuku จริง

- ก่อนแก้: service regressions ล้มเหลว 11/12, runner regressions ล้มเหลว 5/5, ViewModel regressions ล้มเหลว 5/5; exact locale UI regression ล้มเหลวตามคาด
- หลังแก้: unit tests 33 tests, 0 failures, 0 errors, 0 skipped
- `:app:assembleDebug` สำเร็จ
- `:app:lintDebug` สำเร็จ ไม่มี error; 43 warnings เกี่ยวกับ dependency versions, resources, manifest label และคำแนะนำโค้ดทั่วไป ไม่ได้อัปเกรด dependencies นอกขอบเขต
- ตรวจโค้ดโดย reviewer แยกอีกคน ไม่พบ actionable introduced correctness issue; เพิ่ม partial pipe-acquisition cleanup test ตามช่องว่างที่ reviewer ระบุ

คำสั่งตรวจสุดท้าย:

```sh
./gradlew --offline --no-daemon --no-configuration-cache --max-workers=2 :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

ช่วงแรก lint offline ขาด espresso/runner ใน cache จึงให้ Gradle resolve dependencies เหล่านั้นตามปกติก่อนรัน offline ซ้ำ ไม่ได้รัน instrumented tests บนอุปกรณ์

## ข้อจำกัดที่ยังเหลือ

- ยังไม่ได้ทดสอบ OEM/device หรือ release signing/manifest ของ artifact ที่เผยแพร่จริง
- Synchronous Binder IPC ระหว่างสร้าง process, รับ stream, ตรวจ exit และ cleanup ไม่สามารถรับประกันการตัดด้วย coroutine timeout ได้
- หากคำสั่งก่อนหน้าสำเร็จแต่คำสั่งถัดไปล้มเหลว การตั้งค่าบางส่วนอาจเปลี่ยนแล้ว ไม่มี rollback อัตโนมัติ
- Gradle dependency verification สำหรับ transitive artifacts ยังไม่ได้เปิดใช้ ต้องทวน metadata/checksum/signature จากแหล่งที่เชื่อถือก่อน ไม่สร้าง trust จาก cache ที่ยังไม่ตรวจ
- Reviewer ระบุ race เดิมที่ readiness เปลี่ยนระหว่างงานสำเร็จอาจทำให้ completion เติมค่าที่อ่านล่าสุดกลับหลัง status collector ล้างค่า ความถูกต้องบนอุปกรณ์ยังต้องตรวจ; ไม่ยืนยันว่าเป็นช่องโหว่
- โฟลเดอร์ไม่มี Git จึงไม่ได้ commit; เก็บ backup ของไฟล์เดิมที่แก้หลักไว้ภายนอกที่ `/Users/pongdanai/security-audit-skill/thai-clock-fix/implementation-backup-20261015` รายงาน audit เดิมคงเป็น snapshot ก่อนแก้
