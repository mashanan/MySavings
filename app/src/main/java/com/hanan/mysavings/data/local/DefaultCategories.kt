package com.hanan.mysavings.data.local

import com.hanan.mysavings.data.local.entity.CategoryEntity
import com.hanan.mysavings.domain.model.Category

/**
 * Categories inserted when the database is first created. Names are user-facing data,
 * so they are in Indonesian. Keywords are matched against receipt text in phase 4.
 */
object DefaultCategories {

    val all = listOf(
        CategoryEntity(
            name = "Makan & Minum",
            iconKey = "food",
            colorHex = "#E57373",
            keywords = "resto,restoran,rumah makan,warung,cafe,kafe,kopi,coffee,bakery,bakmi,sate,mcd,mcdonald,kfc," +
                "solaria,hokben,pizza,starbucks,janji jiwa,kopi kenangan,gofood,grabfood,shopeefood",
            isDefault = true,
        ),
        CategoryEntity(
            name = "Belanja Harian",
            iconKey = "groceries",
            colorHex = "#4DB6AC",
            keywords = "indomaret,alfamart,alfamidi,lawson,superindo,hypermart,transmart,lotte mart,carrefour," +
                "minimarket,supermarket,toserba,swalayan",
            isDefault = true,
        ),
        CategoryEntity(
            name = "Transportasi",
            iconKey = "transport",
            colorHex = "#64B5F6",
            keywords = "pertamina,spbu,shell,vivo energy,pertalite,pertamax,solar,parkir,tol,e-toll,gojek,goride," +
                "grabcar,grabbike,krl,mrt,lrt,transjakarta,kai",
            isDefault = true,
        ),
        CategoryEntity(
            name = "Tagihan",
            iconKey = "bills",
            colorHex = "#FFB74D",
            keywords = "pln,token listrik,listrik,pdam,telkom,indihome,pulsa,paket data,bpjs,internet,wifi",
            isDefault = true,
        ),
        CategoryEntity(
            name = "Hiburan",
            iconKey = "entertainment",
            colorHex = "#BA68C8",
            keywords = "xxi,cgv,cinepolis,bioskop,netflix,spotify,timezone,karaoke,steam",
            isDefault = true,
        ),
        CategoryEntity(
            name = "Kesehatan",
            iconKey = "health",
            colorHex = "#81C784",
            keywords = "apotek,apotik,kimia farma,k24,guardian,century,watsons,klinik,rumah sakit,dokter,lab",
            isDefault = true,
        ),
        CategoryEntity(
            name = "Lainnya",
            iconKey = Category.OTHER_ICON_KEY,
            colorHex = "#90A4AE",
            keywords = "",
            isDefault = true,
        ),
    )
}
