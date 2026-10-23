
package controller;

import java.util.List;
import model.IKendaraanRepository;
import model.IPenyewaanRepository;
import model.Kendaraan;
import model.KendaraanFactory;
import model.Penyewaan;
import model.WajibJaminan;
import model.WajibSIM;
import view.RentalView;
import view.StrukView;

public class RentalController {
    private final IKendaraanRepository kendaraanRepo;
    private final IPenyewaanRepository penyewaanRepo;
    private final RentalView view;
    private final StrukView strukView;

    public RentalController(
            IKendaraanRepository kendaraanRepo,
            IPenyewaanRepository penyewaanRepo,
            RentalView view,
            StrukView strukView) {

        this.kendaraanRepo = kendaraanRepo;
        this.penyewaanRepo = penyewaanRepo;
        this.view = view;
        this.strukView = strukView;
    }

    public void tambahDataAwal(Kendaraan kendaraan) {
        kendaraanRepo.tambah(kendaraan);
    }

    public void jalankan() {
        while (true) {
            String pilihan = view.tampilMenu();

            switch (pilihan) {
                case "1" -> view.tampilDaftarKendaraan(kendaraanRepo.semua());
                case "2" -> tambahKendaraan();
                case "3" -> sewaKendaraan();
                case "4" -> kembalikanKendaraan();
                case "5" -> view.tampilRiwayat(penyewaanRepo.semua());
                case "0" -> {
                    view.tampilPesan("Thank You.");
                    return;
                }
                default -> view.tampilPesan("Menu tidak ada.");
            }
        }
    }

    public void tambahKendaraan() {
        String plat = view.bacaTeks("Plat Nomor");

        if (plat.isEmpty() || kendaraanRepo.cariByPlat(plat) != null) {
            view.tampilPesan("Plat nomor kosong atau sudah terdaftar.");
            return;
        }

        String merk = view.bacaTeks("Merk");
        String jenis = view.bacaTeks(
                "Jenis (" + String.join("/", KendaraanFactory.JENIS) + ")"
        ).toUpperCase();

        if (!List.of(KendaraanFactory.JENIS).contains(jenis)) {
            view.tampilPesan("Jenis tidak dikenal.");
            return;
        }

        long tarif = view.bacaAngka("Tarif per hari");

        if (tarif <= 0) {
            view.tampilPesan("Tarif harus lebih dari 0.");
            return;
        }

        kendaraanRepo.tambah(
                KendaraanFactory.buat(jenis, plat, merk, tarif)
        );

        view.tampilPesan("Kendaraan berhasil ditambahkan.");
    }


public void sewaKendaraan() {
    String plat = view.bacaTeks("Plat Nomor");
    Kendaraan kendaraan = kendaraanRepo.cariByPlat(plat);

    if (kendaraan == null || !kendaraan.isTersedia()) {
        view.tampilPesan(
                "Kendaraan tidak ditemukan atau sedang disewa."
        );
        return;
    }

    // Validasi SIM
    if (kendaraan instanceof WajibSIM wajibSIM) {
        String sim = view.bacaTeks(
                "Masukkan jenis SIM yang dimiliki"
        ).toUpperCase();

        if (!sim.equals(wajibSIM.getJenisSIMDibutuhkan())) {
            view.tampilPesan(
                    "SIM tidak sesuai. Kendaraan membutuhkan SIM "
                    + wajibSIM.getJenisSIMDibutuhkan() + "."
            );
            return;
        }
    }

    // Validasi jaminan
    if (kendaraan instanceof WajibJaminan wajibJaminan) {
        view.tampilPesan(
                "Jaminan yang diperlukan: Rp"
                + wajibJaminan.getNominalJaminan()
        );

        String konfirmasi = view.bacaTeks(
                "Apakah jaminan tersedia? (YA/TIDAK)"
        );

        if (!konfirmasi.equalsIgnoreCase("YA")) {
            view.tampilPesan(
                    "Penyewaan dibatalkan karena jaminan belum tersedia."
            );
            return;
        }
    }

    String nama = view.bacaTeks("Nama Penyewa");
    int hari = (int) view.bacaAngka("Lama sewa (hari)");

    if (hari <= 0) {
        view.tampilPesan("Lama sewa harus lebih dari 0 hari.");
        return;
    }

    if (hari < kendaraan.getMinimalHari()) {
        view.tampilPesan(
                kendaraan.getMerk() + " minimal disewa "
                + kendaraan.getMinimalHari() + " hari."
        );
        return;
    }

    long total = kendaraan.hitungBiaya(hari);

    Penyewaan sewa = new Penyewaan(
            plat, nama, kendaraan, hari, total
    );

    penyewaanRepo.tambah(sewa);
    kendaraan.setTersedia(false);
    strukView.cetak(sewa);
}


    public void kembalikanKendaraan() {
        String plat = view.bacaTeks("Plat Nomor");
        Penyewaan sewa = penyewaanRepo.cariAktifByPlat(plat);

        if (sewa == null) {
            view.tampilPesan("Tidak ada penyewaan.");
            return;
        }

        sewa.selesai();
        sewa.getKendaraan().setTersedia(true);

        view.tampilPesan(
                "Kendaraan " + sewa.getKendaraan().getMerk()
                + " telah dikembalikan."
        );
    }
}
