# โครงสร้างและแนวทางพัฒนา

StepByStep แยกชั้นแสดงผลออกจากข้อมูลและการกระทำของผู้ใช้ เพื่อรองรับการเชื่อมเซนเซอร์และฐานข้อมูลในระยะถัดไป

## องค์ประกอบหลัก

| ไฟล์ | หน้าที่ |
|---|---|
| `MainActivity.java` | แสดงหน้าจอ นำทาง เปิด bottom sheets และรับ `ActivityUiState` |
| `ActivityUiActions.java` | กำหนดการกระทำที่เกิดจากปุ่มใน UI |
| `model/ActivityUiState.java` | สถานะกิจกรรมและข้อมูลที่นำมาแสดงผล |
| `model/ActivityRecord.java` | ข้อมูลกิจกรรมหนึ่งรายการและ ID ถาวร |
| `presentation/ActivityHistoryAdapter.java` | แสดงประวัติด้วย RecyclerView และ ViewHolder |
| `presentation/UiFormat.java` | จัดรูปแบบเวลา อัตราต่อนาที และป้ายสถิติ |
| `views/ActivityChartView.java` | วาดกราฟกิจกรรมแบบ Native |
| `demo/UiPreviewController.java` | จำลองข้อมูลและการเปลี่ยนสถานะสำหรับ UI prototype |

## การเชื่อมแหล่งข้อมูลจริง

1. สร้าง controller ที่ implements `ActivityUiActions`
2. เปลี่ยนการผูก `actions` และ `UiPreviewController` ใน `MainActivity.onCreate()` ให้ใช้ controller ดังกล่าว
3. ส่งข้อมูลให้ `MainActivity.renderState(state)` บน main thread เมื่อสถานะเปลี่ยน
4. โหลด `history` จาก SQLite และเพิ่ม `historyVersion` เมื่อเพิ่ม ลบ หรือคืนรายการสำเร็จ
5. อัปเดต steps, shakes และ seconds จากชั้นอ่านเซนเซอร์และระบบจับเวลา
6. เชื่อม lifecycle และสิทธิ์ Android ตามพฤติกรรมการติดตามกิจกรรมที่กำหนด
7. ปรับปุ่มเครื่องมือจำลองให้แสดงเฉพาะโหมดพัฒนาเมื่อระบบจริงพร้อมใช้งาน

ควรอ่าน/เขียนฐานข้อมูลนอก main thread และส่งผลกลับมาแสดงบน main thread โดยไม่ให้ Adapter หรือ `onDraw()` เป็นผู้ทำงาน I/O

## การกระทำและสถานะ

| สถานะ | ปุ่มหลัก | การกระทำอื่น |
|---|---|---|
| IDLE | เริ่มกิจกรรม | ตั้งเป้าหมาย |
| RECORDING | พักกิจกรรม | ดูสถิติระหว่างทำกิจกรรม |
| PAUSED | ทำต่อ | จบกิจกรรม |
| REVIEW | บันทึกกิจกรรม | กลับไปทำต่อ / ไม่บันทึก |
| SAVED | กลับหน้าวันนี้ | ดูประวัติกิจกรรม |

อินเทอร์เฟซรองรับ `start()`, `pause()`, `resume()`, `finish()`, `save()`, `newSession()`, `discard()`, `delete(recordId)`, `undoDelete()`, `setGoal(goal)` และ `setAutoStart(enabled)`

การบันทึกเกิดได้เฉพาะสถานะ REVIEW เพื่อป้องกันการเพิ่มรายการซ้ำ ขณะสลับไปเมนูอื่นระหว่างกิจกรรม จะมีปุ่มกลับไปกิจกรรมหรือกลับไปสรุปกิจกรรม

## ข้อมูลกับ View ID

| ข้อมูล | View ID |
|---|---|
| ก้าวเดิน / การเขย่า | `tvCurrentSteps`, `tvCurrentShakes` |
| ระยะเวลา | `tvSessionDuration` |
| อัตราต่อนาที | `tvStepsPerMin`, `tvShakesPerMin` |
| เป้าหมายและความคืบหน้า | `btnGoal`, `goalProgress`, `tvGoalRemaining`, `tvGoalPercent` |
| ยอดสะสม | `tvCumulativeSummary`, `tvHistoryTotals` |
| ประวัติทั้งหมด / ล่าสุด | `rvActivityHistory`, `rvRecentHistory` |
| ปุ่มควบคุม | `btnPrimary`, `btnEnd` |
| กราฟ | `activityChart` |
| ป้ายสถิติ | `badgeStep`, `badgeShake` |
| หน้าสรุป | `tvSummarySteps`, `tvSummaryDuration`, `tvSummaryShakes`, `tvSummaryRate`, `tvSummaryShakeRate` |

## กติกาการแสดงผล

- อัตราต่อนาที = จำนวน × 60 / เวลากิจกรรม; เวลาศูนย์แสดง 0.0
- ยอดสะสมรวมเฉพาะรายการที่บันทึกแล้ว
- ประวัติเรียงใหม่ไปเก่า และใช้ ID ถาวรของรายการ
- ป้ายสถิติใหม่แสดงเมื่อค่ามากกว่า 0 และมากกว่าค่าสูงสุดของรายการที่เก่ากว่าอย่างเคร่งครัด
- ลบหรือคืนรายการแล้วคำนวณยอดสะสมและป้ายใหม่
- เลิกทำการลบได้ภายใน 8 วินาที
- เป้าหมายกำหนดได้ 1–100,000 ก้าว
- การพักหยุดทั้งเวลาและตัวนับในเวอร์ชัน UI prototype

## ทรัพยากรดีไซน์

- `res/values/colors.xml`, `styles.xml`: สีและตัวอักษร
- `res/layout/screen_*.xml`: หน้าจอหลัก
- `res/layout/item_activity.xml`: การ์ดประวัติ
- `res/layout/sheet_*.xml`: bottom sheets
- `res/drawable/`: ไอคอน รูปทรง และสถานะปุ่ม
- `design-reference/`: ภาพหน้าจอและ HTML อ้างอิงการออกแบบ

## กระบวนการพัฒนา

ใช้ `main` สำหรับเวอร์ชันที่พร้อมตรวจสอบ และสร้าง feature branch ตามงาน เช่น `feature/sensors` หรือ `feature/sqlite` ตรวจการ build และทดสอบส่วนที่เปลี่ยนก่อนเปิด Pull Request ส่วน `ui-v1.0` ใช้อ้างอิงหน้าตาและพฤติกรรมของ UI รุ่นแรก
