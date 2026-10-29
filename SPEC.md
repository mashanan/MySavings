# SPEC — MySavings

Pencatat pengeluaran offline-first dengan scan struk di perangkat.
Konvensi teknis dan perintah build ada di [CLAUDE.md](CLAUDE.md).

## 1. Tujuan
- Mencatat pengeluaran secepat mungkin: cukup foto struk, cek sebentar, lalu simpan.
- Pengguna tahu sisa anggaran bulan ini dalam sekali lihat.
- Semua fitur jalan **tanpa internet**, dan data tidak pernah meninggalkan perangkat.

## 2. Fitur MVP
1. **Input transaksi manual**: jumlah, tanggal, nama toko/catatan, dan kategori.
2. **Scan struk offline**: total, tanggal, nama toko, dan kategori terisi otomatis.
3. **Konfirmasi hasil scan**: semua field bisa dikoreksi sebelum disimpan.
4. **Anggaran bulanan**: anggaran total per bulan, opsional per kategori, beserta sisanya.
5. **Beranda** berisi ringkasan bulan berjalan.
6. **Penyimpanan lokal** dengan Room. Aplikasi tidak meminta izin `INTERNET`.

## 3. Layar

### 3.1 Beranda
| Bagian | Isi |
|---|---|
| Sisa anggaran bulan ini | Nominal sisa, terpakai/limit, dan progress bar. Warnanya berubah menjadi peringatan di atas 80% dan merah di atas 100%. Jika anggaran belum diatur, muncul tombol "Atur anggaran" yang membuka bottom sheet. |
| Pengeluaran per kategori | Daftar kategori bulan ini, diurutkan dari yang terbesar, dengan nominal dan persentase. |
| Transaksi terbaru | 10 transaksi terakhir: ikon kategori, nama toko, tanggal, dan nominal. |
| Tombol + (FAB) | Membuka pilihan "Scan struk" atau "Input manual". |

State yang ditangani: memuat, kosong ("Belum ada transaksi bulan ini"), dan ada data.

### 3.2 Scan struk
- Saat pertama dibuka, aplikasi meminta izin kamera disertai penjelasan. Jika pengguna menolak, tampil pesan dan tombol "Input manual".
- Preview kamera (CameraX) dengan bingkai panduan dan tombol potret.
- Setelah foto diambil, tampil indikator "Membaca struk…". ML Kit mengenali teks di perangkat, `ReceiptParser` mengubahnya menjadi `ReceiptDraft`, lalu aplikasi pindah ke layar Konfirmasi.
- Jika tidak ada teks yang terbaca, tampil pesan "Struk tidak terbaca" dengan pilihan "Coba lagi" atau "Isi manual".

### 3.3 Konfirmasi hasil scan
- Form berisi **Nama toko**, **Total** (input angka, ditampilkan sebagai Rp), **Tanggal** (date picker), **Kategori** (dropdown), dan **Catatan** (opsional).
- Field yang tidak berhasil terdeteksi diberi penanda "Periksa kembali".
- Validasi: total harus lebih dari 0 dan kategori wajib dipilih.
- Tombol "Simpan" menyimpan transaksi, kembali ke Beranda, dan menampilkan snackbar "Transaksi tersimpan". Tombol "Batal" kembali tanpa menyimpan.
- Layar yang sama dipakai untuk **input manual**, dengan draft kosong dan tanggal terisi hari ini.

### Navigasi
```
Beranda ──(+ Scan)──▶ Scan struk ──(hasil)──▶ Konfirmasi ──(Simpan)──▶ Beranda
   └────(+ Manual)────────────────────────▶ Konfirmasi
```

## 4. Model data (Room)

### `categories`
| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | Long PK | auto-generate |
| `name` | String | contoh: "Makan & Minum" |
| `iconKey` | String | kunci ikon Material |
| `colorHex` | String | warna untuk daftar dan grafik |
| `keywords` | String | kata kunci untuk klasifikasi otomatis, dipisah koma |
| `isDefault` | Boolean | kategori bawaan |

Seed awal: Makan & Minum, Belanja Harian, Transportasi, Tagihan, Hiburan, Kesehatan, Lainnya.

### `transactions`
| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | Long PK | auto-generate |
| `amount` | Long | rupiah penuh |
| `date` | Long | `LocalDate` sebagai epoch day |
| `merchant` | String? | nama toko |
| `note` | String? | catatan |
| `categoryId` | Long FK → categories | diberi index |
| `source` | String | `MANUAL` / `SCAN` |
| `createdAt` | Long | epoch millis |

### `budgets`
| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | Long PK | auto-generate |
| `yearMonth` | Int | contoh: `202610` |
| `categoryId` | Long? FK | `null` berarti anggaran total bulan itu |
| `limitAmount` | Long | rupiah |

Constraint unik: (`yearMonth`, `categoryId`).

## 5. Logika pembacaan struk
Seluruh logika ini ada di `domain/parser` dan ditulis dalam Kotlin murni, sehingga bisa diuji dengan unit test.

- **Total**
  - Ambil angka dari baris yang mengandung `TOTAL`, `GRAND TOTAL`, `TOTAL BAYAR`, atau `JUMLAH`.
  - Lewati baris yang mengandung `SUBTOTAL`, `DISKON`, `KEMBALI`, `TUNAI`, `PPN`, atau `PAJAK`.
  - Jika tidak ada baris seperti itu, pakai angka berformat uang yang terbesar.
  - Format yang dikenali: `25.000`, `25,000`, `Rp 25.000`, dan `25.000,00`.
- **Tanggal**: format `dd/MM/yyyy`, `dd-MM-yy`, `dd.MM.yyyy`, dan `dd MMM yyyy` (nama bulan Indonesia dan Inggris). Tanggal di masa depan diabaikan. Jika tidak ditemukan, pakai hari ini dan beri penanda.
- **Nama toko**: baris teks pertama yang bukan angka, bukan alamat (`JL.`, `JALAN`), bukan telepon, dan bukan NPWP.
- **Kategori**: `CategoryClassifier` mencocokkan nama toko dan isi struk dengan `keywords` setiap kategori. Contoh: `indomaret`/`alfamart` → Belanja Harian; `pertamina`/`shell` → Transportasi; `apotek`/`kimia farma` → Kesehatan. Jika tidak ada yang cocok, masuk "Lainnya".

Hasilnya berupa `ReceiptDraft(merchant?, total?, date?, categoryId?, missingFields)`.

## 6. Fitur tahap lanjut
- **Grafik bulanan**: bar pengeluaran 6 bulan terakhir dan donut per kategori. Digambar dengan Compose Canvas tanpa library tambahan.
- **Notifikasi anggaran hampir habis**: dikirim saat pemakaian mencapai 80% dan 100%. Pengecekan dilakukan setiap kali transaksi disimpan dan oleh worker harian WorkManager. Butuh izin `POST_NOTIFICATIONS` (Android 13+). Notifikasi untuk setiap ambang hanya dikirim sekali per bulan.
- **Ekspor laporan PDF**: laporan bulanan berisi ringkasan, tabel per kategori, dan daftar transaksi. Dibuat dengan `android.graphics.pdf.PdfDocument` bawaan, lalu dibagikan lewat `FileProvider` dan share sheet.
- **Opsional**: edit/hapus transaksi, simpan foto struk, backup/restore lokal, dan kategori kustom.

## 7. Rencana kerja (6 fase)
Setiap fase dimulai dengan rencana singkat, menunggu persetujuan, lalu dikerjakan. Fase dianggap selesai setelah build dan unit test lulus serta ringkasan perubahan disampaikan.

| Fase | Isi | Selesai jika |
|---|---|---|
| **1. Fondasi** | Menambahkan dependency ke version catalog (KSP, Hilt, Room, Navigation, kotlinx-serialization, lifecycle-viewmodel-compose, MockK, coroutines-test, Turbine); `@HiltAndroidApp`; struktur paket; tema M3 dan `strings.xml`; NavHost dengan 3 layar placeholder; menghapus tes contoh template. (Ganti package ke `com.hanan.mysavings` dan `git init` sudah dilakukan.) | Build lulus dan aplikasi bisa berpindah antar ketiga placeholder. |
| **2. Data & domain** | Entity, DAO, `MySavingsDatabase` dengan seed kategori, mapper entity ↔ domain, model domain, interface dan implementasi repository, modul Hilt, serta use case `AddTransaction`, `ObserveMonthlySummary`, `SetBudget`, dan `ObserveCategories`. | Unit test use case (MockK) dan DAO test (in-memory) lulus. |
| **3. Beranda & input manual** | `HomeViewModel` dan UI Beranda; bottom sheet "Atur anggaran"; layar Konfirmasi mode manual; helper `formatRupiah`. | Transaksi manual tersimpan dan langsung tampil di Beranda. Tes ViewModel (Turbine) lulus. |
| **4. Scan struk** | CameraX (preview dan ImageCapture); alur izin kamera; `MlKitTextRecognizer` di data layer; `ReceiptParser` dan `CategoryClassifier` dengan unit test dari contoh teks struk Indonesia. | Foto struk menghasilkan `ReceiptDraft` yang benar untuk contoh-contoh uji. |
| **5. Konfirmasi hasil scan & polish MVP** | Draft hasil scan mengisi form Konfirmasi; penanda field; validasi; empty dan error state; pengecekan aksesibilitas dasar (contentDescription, ukuran sentuh). | Alur lengkap Beranda → Scan → Konfirmasi → Beranda berjalan offline. **MVP selesai.** |
| **6. Tahap lanjut** | Grafik bulanan, notifikasi anggaran (WorkManager), dan ekspor PDF. Bisa dipecah menjadi 6a/6b/6c. | Ketiga fitur berjalan dan logikanya punya unit test. |
