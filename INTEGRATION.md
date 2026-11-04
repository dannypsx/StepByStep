# ส่งต่องาน UI ให้ผู้พัฒนาระบบ

## ขอบเขตของชุดนี้

นี่คือชั้น UI จริงใน Android ไม่ใช่ HTML wrapper ทุกหน้าจอใช้ Android Views และประวัติใช้ RecyclerView 1.4.0 จริง ข้อมูลที่แสดงมาจาก `UiPreviewController` ซึ่งตั้งใจให้เปลี่ยนเป็นตัวควบคุมของเพื่อน

## ลำดับเชื่อมต่อที่แนะนำ

1. สร้าง controller/presenter ที่ implements `ActivityUiActions`
2. ใน `MainActivity.onCreate()` เปลี่ยนการกำหนด `actions = preview` และการสร้าง preview ให้ผูกกับ controller จริง
3. เมื่อข้อมูลเปลี่ยน ส่ง `ActivityUiState` ให้ `MainActivity.renderState(state)` บน main thread
4. เปลี่ยนข้อมูล `history` เป็นผลจาก SQLite และเพิ่ม `historyVersion` เมื่อ insert/delete/restore สำเร็จ เพื่อให้รายการ/กราฟ/ยอดสะสมวาดใหม่
5. เชื่อม listener เซนเซอร์ใน controller จริง แล้วอัปเดต steps/shakes/seconds โดยไม่ให้ View ไปอ่านเซนเซอร์เอง
6. แทน lifecycle ที่เรียก `preview.attach()/detach()` และ saved state ของ preview ด้วยนโยบาย lifecycle ของแอปจริง
7. ลบหรือซ่อนปุ่ม `btnPreview`, `btnSettingsPreview` และ bottom sheet จำลอง เมื่อส่งงานระบบจริง

`UiPreviewController` ใช้ Handler เฉพาะนาฬิกาตัวอย่าง ไม่มี SensorManager, SQLite, HTTP หรือการขอ permission จริง อย่าเชื่อมงานฐานข้อมูลลงใน Adapter หรือ onDraw ของกราฟ ให้ทำ I/O นอก main thread แล้วกลับมาอัปเดต UI บน main thread

## Mapping การกดปุ่ม

| ความตั้งใจผู้ใช้ | เมธอดใน ActivityUiActions |
|---|---|
| เริ่มกิจกรรม | start() |
| พัก | pause() |
| ทำต่อ | resume() |
| จบและตรวจสรุป | finish() |
| บันทึกหลังตรวจสรุป | save() |
| กลับหน้าวันนี้หลังบันทึก | newSession() |
| ยืนยันไม่บันทึกเซสชัน | discard() |
| ยืนยันลบรายการที่บันทึก | delete(recordId) |
| เลิกทำหลังลบ | undoDelete() |
| ตั้งเป้าหมาย | setGoal(goal) |
| ตั้งการเริ่มเมื่อเปิดแอป | setAutoStart(enabled) |

## สถานะหน้าจอ

| phase | ปุ่มหลัก | ปุ่มรอง |
|---|---|---|
| IDLE | เริ่มกิจกรรม | — |
| RECORDING | พักกิจกรรม | — |
| PAUSED | ทำต่อ | จบกิจกรรม |
| REVIEW | บันทึกกิจกรรม | กลับไปทำกิจกรรมต่อ / ไม่บันทึก |
| SAVED | กลับหน้าวันนี้ | ดูประวัติกิจกรรม |

ขณะสลับไปเมนูอื่นระหว่างกิจกรรม จะมีปุ่ม “กลับไปกิจกรรม” หรือ “กลับไปสรุปกิจกรรม” ให้กลับมาได้ง่าย ห้ามเพิ่มรายการซ้ำเมื่อผู้ใช้แตะบันทึกซ้ำ: controller ตัวอย่างรับ save เฉพาะ REVIEW

## Mapping ข้อมูลและ View

| ข้อมูล | View ID |
|---|---|
| จำนวนก้าว | tvCurrentSteps |
| จำนวนการเขย่า | tvCurrentShakes |
| เวลา | tvSessionDuration |
| ก้าวต่อนาที | tvStepsPerMin |
| เขย่าต่อนาที | tvShakesPerMin |
| เป้าหมาย/ความคืบหน้า | btnGoal, goalProgress, tvGoalRemaining, tvGoalPercent |
| ยอดสะสมจากประวัติ | tvCumulativeSummary, tvHistoryTotals |
| รายการประวัติทั้งหมด | rvActivityHistory |
| กิจกรรมล่าสุด | rvRecentHistory |
| ปุ่มหลัก/จบกิจกรรม | btnPrimary, btnEnd |
| กราฟ | activityChart |
| ป้ายสถิติในรายการ | badgeStep, badgeShake |
| หน้าสรุป | tvSummarySteps, tvSummaryDuration, tvSummaryShakes, tvSummaryRate, tvSummaryShakeRate |

`ActivityRecord.id` ต้องเป็น ID ถาวรจากฐานข้อมูล ไม่ใช้ตำแหน่งในรายการเป็น ID รายการเรียงใหม่ไปเก่า

## กติกาที่ใช้ใน UI ตัวอย่าง

- อัตราต่อนาที = จำนวน × 60 / เวลากิจกรรม; เวลาศูนย์แสดง 0.0
- ยอดสะสมรวมเฉพาะรายการที่บันทึกแล้ว ไม่รวมเซสชันที่กำลังทำ
- ป้ายสถิติ: ค่ามากกว่า 0 และมากกว่าค่าสูงสุดของรายการที่เก่ากว่าอย่างเคร่งครัด ค่าเสมอไม่เป็นสถิติใหม่
- ลบ/คืนรายการแล้วคำนวณยอดและป้ายทั้งหมดใหม่
- เลิกทำมีเวลา 8 วินาที ควรเก็บข้อมูลที่จำเป็นสำหรับคืนรายการให้ครบ
- เป้าหมาย 1–100,000 ก้าว
- ขอสิทธิ์/ไม่อนุญาต/ไม่มีเซนเซอร์เป็นแค่ state preview ต้องแทนด้วยผลตรวจจริงก่อนส่งงาน

## ประเด็นเวลาที่ต้องตรวจ checklist

สไลด์และคลาสยกตัวอย่างจับเวลาตั้งแต่เปิดแอป ชุด UI จึงเปิดด้วยข้อมูลตัวอย่างพร้อมสถานะ RECORDING และมีตัวเลือกเริ่มเอง การพักหยุดเวลาตาม UX ที่เลือกไว้ แต่ยังต้องตรวจ Excel ว่าต้องใช้เวลาตั้งแต่เปิดแอปแบบไม่หักช่วงพักหรือไม่

## ไฟล์ดีไซน์

- `res/values/colors.xml` และ `styles.xml`: สีและ typography
- `res/layout/screen_*.xml`: แต่ละหน้าจอ
- `res/layout/item_activity.xml`: การ์ดประวัติ
- `res/layout/sheet_*.xml`: bottom sheets
- `res/drawable/`: รูปทรง ไอคอน และ states ของปุ่ม
- `design-reference/`: HTML mockup และภาพสำหรับเทียบดีไซน์

ไลบรารี RecyclerView: https://developer.android.com/jetpack/androidx/releases/recyclerview
