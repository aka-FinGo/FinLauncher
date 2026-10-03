# Session Handoff: FinLauncher (Reverse-Engineered Niagara Launcher 1:1)

> **Sana va Vaqt**: 2026-10-03 04:35 UTC  
> **Loyiha**: [FinLauncher (GitHub: aka-FinGo/FinLauncher)](https://github.com/aka-FinGo/FinLauncher)  
> **Oxirgi Commit**: `30e02b9` (branch: `main`)  
> **Release**: `v1.0.07` (Muvaffaqiyatli build qilindi va chiqarildi)  
> **Holat**: Niagara Launcher 1.16.31 APK si JADX orqali to'liq dekompilyatsiya qilindi. Haqiqiy Gauss to'lqin formulasi (`2^( (1/c) * (-4*delta^2) )`) va kubik magnit nishon fiksatsiyasi FinLauncher'ga ko'chirildi. APK `/sdcard/Download/` papkasiga sinxronizatsiya qilinmoqda.

---

## 1. Reverse Engineering Natijalari (Reverse Engineering Discovery)

Niagara Launcher'ning `NaZLkiZcQ4E9Qc7F5WGdQ.java` va `Y8RBzXNjmCEZkb.java` sinflaridan quyidagi haqiqiy formulalar aniqlandi va FinLauncher'ga ko'chirildi:

1. **Gauss Eksponentsial To'lqin Formulasi (`NaZLkiZcQ4E9Qc7F5WGdQ.java:484`)**:
   $$f_{\text{pow}} = 2^{\frac{1}{c} \cdot (-4 \cdot \Delta^2)}$$
   Oddiy kosinus yoki chiziqli formulalar Niagara silliqligini bera olmasligining sababi — Niagara aynan ushbu Gauss qo'ng'iroqsimon (bell curve) egri chizig'idan foydalanishidadir.
2. **Kubik Magnit Snapping (Taktil Harfga Yopishish) (`NaZLkiZcQ4E9Qc7F5WGdQ.java:273-276`)**:
   $$\Delta y_{\text{snapped}} = y_{\text{center}} + \left( \frac{(2 \cdot \text{relY})^3}{2} \right) \cdot \text{itemHeight}$$
   Ushbu $x^3$ kubik interpolyatsiya tufayli qizil shar barmoq harf ustidan o'tayotganda harfning qoq markaziga magnit kabi "yopishib" turadi va keyingi harfga silliq uzilib o'tadi.
3. **Dinamik Gorizontal Tortish Chuqurligi (`touchXOffset`)**:
   Barmoq chapga tortilgan sari to'lqin ekranning 180dp ichkarisigacha chuqurlashadi.
4. **Spring Physics (Prujina Easing)**:
   Barmoq uzilganda damping ratio `0.5f` va stiffness `MediumLow` bilan titramasdan asl holiga qaytadi.

---

## 2. CI/CD & Build Holati

- **Chiqarilgan Versiya**: `v1.0.07`
- **Imzo**: Doimiy `finlauncher.jks`
- **Yuklab olish manzili**:
  [FinLauncher-v1.0.07.apk](https://github.com/aka-FinGo/FinLauncher/releases/download/v1.0.07/FinLauncher-v1.0.07.apk)
- **Lokal Fayl**: `/sdcard/Download/FinLauncher-v1.0.07.apk` va `/sdcard/Download/FinLauncher.apk`

---

## 3. Resumption Command
```bash
cd /home/kali/FinLauncher && git status && git log -n 3 --oneline
```
