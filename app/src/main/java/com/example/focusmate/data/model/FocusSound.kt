package com.example.focusmate.data.model

import com.example.focusmate.R

data class FocusSound(
    val id: Int,
    val name: String,
    val resourceId: Int,
    val isAvailable: Boolean = true
)

object FocusSoundList {
    val sounds = listOf(
        FocusSound(0, "Không", 0), 
        FocusSound(1, "Tíc Tắc", R.raw.sound_tic_tac),
        FocusSound(2, "Đêm ngược", R.raw.sound_dem_nguoc),
        FocusSound(3, "Gió và đề kêu", R.raw.sound_gio_va_de_keu),
        FocusSound(4, "Lớp học", R.raw.sound_lop_hoc),
        FocusSound(5, "Vùng hoang dã", R.raw.sound_vung_hoang_da),
        FocusSound(6, "Dòng suối", R.raw.sound_dong_suoi),
        FocusSound(7, "Bờ biển", R.raw.sound_bo_bien),
        FocusSound(8, "Mưa", R.raw.sound_mua),
        FocusSound(9, "Quán cà phê", R.raw.sound_quan_ca_phe),
        FocusSound(10, "Lửa cháy bập bùng", R.raw.sound_lua_chay_bap_bung)
    )

    const val DEFAULT_SOUND_ID = 0 
}