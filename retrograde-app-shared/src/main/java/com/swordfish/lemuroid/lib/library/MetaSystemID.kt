package com.swordfish.lemuroid.lib.library

import com.swordfish.lemuroid.common.graphics.ColorUtils
import com.swordfish.lemuroid.lib.R

fun GameSystem.metaSystemID() = MetaSystemID.fromSystemID(id)

/** Meta systems represents a collection of systems which appear the same to the user. It's currently
 *  only for Arcade (without separating FBNeo, MAME2000 or MAME2003). */
enum class MetaSystemID(val titleResId: Int, val imageResId: Int, val systemIDs: List<SystemID>) {
    NES(
        R.string.game_system_title_nes,
        R.drawable.nes,
        listOf(SystemID.NES),
    ),
    SNES(
        R.string.game_system_title_snes,
        R.drawable.snes,
        listOf(SystemID.SNES),
    ),
    GENESIS(
        R.string.game_system_title_genesis,
        R.drawable.sega_genesis,
        listOf(SystemID.GENESIS, SystemID.SEGACD),
    ),
    GB(
        R.string.game_system_title_gb,
        R.drawable.game_boy,
        listOf(SystemID.GB),
    ),
    GBC(
        R.string.game_system_title_gbc,
        R.drawable.game_boy_color,
        listOf(SystemID.GBC),
    ),
    GBA(
        R.string.game_system_title_gba,
        R.drawable.game_boy_advance,
        listOf(SystemID.GBA),
    ),
    N64(
        R.string.game_system_title_n64,
        R.drawable.n64,
        listOf(SystemID.N64),
    ),
    SMS(
        R.string.game_system_title_sms,
        R.drawable.master_system,
        listOf(SystemID.SMS),
    ),
    PSP(
        R.string.game_system_title_psp,
        R.drawable.psp,
        listOf(SystemID.PSP),
    ),
    NDS(
        R.string.game_system_title_nds,
        R.drawable.nds,
        listOf(SystemID.NDS),
    ),
    GG(
        R.string.game_system_title_gg,
        R.drawable.sega_game_gear,
        listOf(SystemID.GG),
    ),
    ATARI2600(
        R.string.game_system_title_atari2600,
        R.drawable.atari_2600,
        listOf(SystemID.ATARI2600),
    ),
    PSX(
        R.string.game_system_title_psx,
        R.drawable.ps1,
        listOf(SystemID.PSX),
    ),
    ARCADE(
        R.string.game_system_title_arcade,
        R.drawable.arcade,
        listOf(SystemID.FBNEO, SystemID.MAME2003PLUS),
    ),
    ATARI7800(
        R.string.game_system_title_atari7800,
        R.drawable.atari_7800,
        listOf(SystemID.ATARI7800),
    ),
    LYNX(
        R.string.game_system_title_lynx,
        R.drawable.atari_lynx,
        listOf(SystemID.LYNX),
    ),
    PC_ENGINE(
        R.string.game_system_title_pce,
        R.drawable.pc_engine,
        listOf(SystemID.PC_ENGINE),
    ),
    NGP(
        R.string.game_system_title_ngp,
        R.drawable.neo_geo_pocket,
        listOf(SystemID.NGP, SystemID.NGC),
    ),
    WS(
        R.string.game_system_title_ws,
        R.drawable.wonderswan_color,
        listOf(SystemID.WS, SystemID.WSC),
    ),
    DOS(
        R.string.game_system_title_dos,
        R.drawable.ms_dos,
        listOf(SystemID.DOS),
    ),
    NINTENDO_3DS(
        R.string.game_system_title_3ds,
        R.drawable.n3ds,
        listOf(SystemID.NINTENDO_3DS),
    ),
    ;

    fun color(): Int {
        return ColorUtils.color(ordinal.toFloat() / values().size)
    }

    companion object {
        fun fromSystemID(systemID: SystemID): MetaSystemID {
            return when (systemID) {
                SystemID.FBNEO -> ARCADE
                SystemID.MAME2003PLUS -> ARCADE
                SystemID.ATARI2600 -> ATARI2600
                SystemID.GB -> GB
                SystemID.GBC -> GBC
                SystemID.GBA -> GBA
                SystemID.GENESIS -> GENESIS
                SystemID.SEGACD -> GENESIS
                SystemID.GG -> GG
                SystemID.N64 -> N64
                SystemID.NDS -> NDS
                SystemID.NES -> NES
                SystemID.PSP -> PSP
                SystemID.PSX -> PSX
                SystemID.SMS -> SMS
                SystemID.SNES -> SNES
                SystemID.PC_ENGINE -> PC_ENGINE
                SystemID.LYNX -> LYNX
                SystemID.ATARI7800 -> ATARI7800
                SystemID.DOS -> DOS
                SystemID.NGP -> NGP
                SystemID.NGC -> NGP
                SystemID.WS -> WS
                SystemID.WSC -> WS
                SystemID.NINTENDO_3DS -> NINTENDO_3DS
            }
        }
    }
}
