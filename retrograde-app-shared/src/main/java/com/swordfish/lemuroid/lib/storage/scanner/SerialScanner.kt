package com.swordfish.lemuroid.lib.storage.scanner

import com.swordfish.lemuroid.common.files.FileUtils
import com.swordfish.lemuroid.common.kotlin.indexOf
import com.swordfish.lemuroid.common.kotlin.kiloBytes
import com.swordfish.lemuroid.common.kotlin.megaBytes
import com.swordfish.lemuroid.common.kotlin.startsWithAny
import com.swordfish.lemuroid.lib.library.SystemID
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.math.ceil
import kotlin.math.roundToInt

object SerialScanner {
    private val READ_BUFFER_SIZE = 64.kiloBytes()

    data class DiskInfo(val serial: String?, val systemID: SystemID?)

    @ExperimentalUnsignedTypes
    private val MAGIC_NUMBERS =
        listOf(
            MagicNumber(
                0x0010,
                ubyteArrayOf(
                    0x53U,
                    0x45U,
                    0x47U,
                    0x41U,
                    0x44U,
                    0x49U,
                    0x53U,
                    0x43U,
                    0x53U,
                    0x59U,
                    0x53U,
                    0x54U,
                    0x45U,
                    0x4dU,
                ).toByteArray(),
                SystemID.SEGACD,
            ),
            MagicNumber(
                0x8008,
                ubyteArrayOf(
                    0x50U,
                    0x4cU,
                    0x41U,
                    0x59U,
                    0x53U,
                    0x54U,
                    0x41U,
                    0x54U,
                    0x49U,
                    0x4fU,
                    0x4eU,
                ).toByteArray(),
                SystemID.PSX,
            ),
            MagicNumber(
                0x9320,
                ubyteArrayOf(
                    0x50U,
                    0x4cU,
                    0x41U,
                    0x59U,
                    0x53U,
                    0x54U,
                    0x41U,
                    0x54U,
                    0x49U,
                    0x4fU,
                    0x4eU,
                ).toByteArray(),
                SystemID.PSX,
            ),
            MagicNumber(
                0x8008,
                ubyteArrayOf(0x50U, 0x53U, 0x50U, 0x20U, 0x47U, 0x41U, 0x4dU, 0x45U).toByteArray(),
                SystemID.PSP,
            ),
            // GameCube Magic Number (0x1C = offset do disco GameCube)
            MagicNumber(
                0x001C,
                ubyteArrayOf(
                    0xC2U, 0x33U, 0x9FU, 0x3DU, // GameCube disc magic
                ).toByteArray(),
                SystemID.GAMECUBE,
            ),
            MagicNumber(
                0x0018,
                ubyteArrayOf(
                    0x5DU, 0x1CU, 0x9EU, 0xA3U, // Wii disc magic
                ).toByteArray(),
                SystemID.WII,
            )
        )

    private val SEGA_CD_REGEX = Regex("([A-Z]+)?-?([0-9]+) ?-?([0-9]*)")

    private val PS_SERIAL_REGEX = Regex("^([A-Z]+)-?([0-9]+)")
    private val PS_SERIAL_REGEX2 = Regex("^([A-Z]+)_?([0-9]{3})\\.([0-9]{2})")

    private const val PS_SERIAL_MAX_SIZE = 12

    private val PSX_BASE_SERIALS =
        listOf(
            "CPCS",
            "SCES",
            "SIPS",
            "SLKA",
            "SLPS",
            "SLUS",
            "ESPM",
            "SLED",
            "SCPS",
            "SCAJ",
            "PAPX",
            "SLES",
            "HPS",
            "LSP",
            "SLPM",
            "SCUS",
            "SCED",
        )

    private val PSP_BASE_SERIALS =
        listOf(
            "ULES",
            "ULUS",
            "ULJS",
            "ULEM",
            "ULUM",
            "ULJM",
            "ULKS",
            "ULAS",
            "UCES",
            "UCUS",
            "UCJS",
            "UCAS",
            "NPEH",
            "NPUH",
            "NPJH",
            "NPEG",
            "NPEX",
            "NPUG",
            "NPJG",
            "NPJJ",
            "NPHG",
            "NPEZ",
            "NPUZ",
            "NPJZ",
            "NPUF",
            "NPUZ",
            "NPUG",
            "NPUX",
        )

    fun extractInfo(
        fileName: String,
        inputStream: InputStream,
    ): DiskInfo {
        Timber.d("Extracting disk info for $fileName")
        inputStream.buffered(READ_BUFFER_SIZE).use {
            return when (FileUtils.extractExtension(fileName)) {
                "pbp" -> extractInfoForPBP(it)
                "iso", "bin", "gcm", "gcz", "ciso", "wbfs" -> standardExtractInfo(it)
                "rvz" -> extractInfoForRVZ(it)
                "3ds" -> extractInfoFor3DS(it)
                else -> DiskInfo(null, null)
            }
        }
    }

    private fun extractInfoForRVZ(openedStream: InputStream): DiskInfo {
        Timber.d("Parsing RVZ file")
        openedStream.mark(READ_BUFFER_SIZE)

        val headerBuffer = ByteArray(1024)
        val bytesRead = openedStream.read(headerBuffer)

        if (bytesRead < 16) {
            Timber.w("RVZ file too small")
            openedStream.reset()
            return DiskInfo(null, null)
        }

        // ========== DEBUG DETALHADO ==========
        Timber.d("========== RVZ DEBUG START ==========")
        Timber.d("Total bytes read: $bytesRead")

        // Imprimir os primeiros 128 bytes em hex
        Timber.d("First 128 bytes (HEX):")
        for (i in 0 until minOf(128, bytesRead) step 16) {
            val line = headerBuffer.slice(i until minOf(i + 16, bytesRead))
            val hex = line.joinToString(" ") { "%02X".format(it) }
            val ascii = line.joinToString("") {
                val c = it.toInt() and 0xFF
                if (c in 32..126) c.toChar().toString() else "."
            }
            Timber.d("0x%04X: %-48s  %s".format(i, hex, ascii))
        }

        // Verificar magic
        val magic = String(headerBuffer.copyOfRange(0, 3), Charsets.US_ASCII)
        val version = headerBuffer[3].toInt() and 0xFF
        Timber.d("Magic: $magic, Version byte: 0x${version.toString(16)}")

        if (magic != "RVZ") {
            Timber.w("Invalid RVZ magic: $magic")
            openedStream.reset()
            return DiskInfo(null, null)
        }

        // Procurar por sequências que parecem Game IDs em TODO o buffer
        Timber.d("Searching for Game ID patterns...")
        var gameID: String? = null

        for (offset in 0 until (bytesRead - 6)) {
            val candidate = String(headerBuffer.copyOfRange(offset, offset + 6), Charsets.US_ASCII)

            // Padrão de Game ID: primeira letra maiúscula, seguida de 3 alfanuméricos, seguida de 2 dígitos
            if (candidate.matches(Regex("[A-Z][A-Z0-9]{3}[0-9]{2}"))) {
                Timber.d("Found Game ID candidate at offset 0x${offset.toString(16)}: '$candidate'")

                // Verificar se os caracteres são printable e fazem sentido
                if (candidate.all { it.isLetterOrDigit() }) {
                    if (gameID == null) {
                        gameID = candidate
                        Timber.d("Selected as primary Game ID: $gameID")
                    }
                }
            }
        }

        // Também procurar por letras isoladas que possam ser parte do Game ID
        Timber.d("Looking for ASCII text sequences...")
        for (offset in 0 until (bytesRead - 10)) {
            val text = String(headerBuffer.copyOfRange(offset, offset + 10), Charsets.US_ASCII)
            if (text.count { it.isLetter() } >= 4) {
                Timber.d("Text at 0x${offset.toString(16)}: '$text'")
            }
        }

        Timber.d("========== RVZ DEBUG END ==========")
        // ========== FIM DEBUG ==========

        openedStream.reset()

        if (gameID == null) {
            Timber.w("No valid Game ID found in RVZ header")
            return DiskInfo(null, null)
        }

        // Determinar o sistema baseado no Game ID
        val systemID = when {
            gameID.startsWith("R") || gameID.startsWith("S") ||
                    gameID.startsWith("W") -> SystemID.WII
            gameID.startsWith("G") || gameID.startsWith("D") -> SystemID.GAMECUBE
            else -> SystemID.GAMECUBE // Default
        }

        Timber.d("Final RVZ Game ID: $gameID, System: $systemID")
        return DiskInfo(gameID, systemID)
    }

    private fun decompressRVZHeader(openedStream: InputStream): ByteArray? {
        // RVZ usa compressão mais complexa (zstd, lzma, etc)
        // Por enquanto, vamos retornar null e fazer fallback
        // TODO: Implementar descompressão RVZ completa se necessário

        Timber.w("RVZ decompression not fully implemented")
        return null
    }

    private fun standardExtractInfo(openedStream: InputStream): DiskInfo {
        openedStream.mark(READ_BUFFER_SIZE)
        val header = readByteArray(openedStream, ByteArray(READ_BUFFER_SIZE))

        val detectedSystem =
            MAGIC_NUMBERS
                .firstOrNull {
                    header.copyOfRange(it.offset, it.offset + it.numbers.size).contentEquals(it.numbers)
                }
                ?.systemID

        Timber.d("SystemID detected via magic numbers: $detectedSystem")

        openedStream.reset()

        return when (detectedSystem) {
            SystemID.SEGACD ->
                runCatching { extractInfoForSegaCD(openedStream) }
                    .getOrDefault(DiskInfo(null, SystemID.SEGACD))

            SystemID.PSX ->
                runCatching { extractInfoForPSX(openedStream) }
                    .getOrDefault(DiskInfo(null, SystemID.PSX))

            SystemID.PSP ->
                runCatching { extractInfoForPSP(openedStream) }
                    .getOrDefault(DiskInfo(null, SystemID.PSP))

            SystemID.GAMECUBE ->
                runCatching { extractInfoForGameCube(openedStream) }
                    .getOrDefault(DiskInfo(null, SystemID.GAMECUBE))

            SystemID.WII ->
                runCatching { extractInfoForWii(openedStream) }
                    .getOrDefault(DiskInfo(null, SystemID.WII))
            else -> DiskInfo(null, null)
        }
    }
    private fun extractInfoForGameCube(openedStream: InputStream): DiskInfo {
        Timber.d("Parsing GameCube game")
        openedStream.mark(0x100)

        // Game ID está no offset 0x00, 6 bytes (exemplo: GALE01 para Melee USA)
        val rawSerial = String(readByteArray(openedStream, ByteArray(6)), Charsets.US_ASCII)

        openedStream.reset()

        Timber.d("Found GameCube serial: $rawSerial")
        return DiskInfo(rawSerial, SystemID.GAMECUBE)
    }

    private fun extractInfoForWii(openedStream: InputStream): DiskInfo {
        Timber.d("Parsing Wii game")
        openedStream.mark(0x100)

        // Game ID está no offset 0x00, 6 bytes (exemplo: RMCE01 para Mario Kart Wii USA)
        val rawSerial = String(readByteArray(openedStream, ByteArray(6)), Charsets.US_ASCII)

        openedStream.reset()

        Timber.d("Found Wii serial: $rawSerial")
        return DiskInfo(rawSerial, SystemID.WII)
    }

    private fun extractInfoFor3DS(openedStream: InputStream): DiskInfo {
        Timber.d("Parsing 3DS game")
        openedStream.mark(0x2000)
        openedStream.skip(0x1150)

        val rawSerial = String(readByteArray(openedStream, ByteArray(10)), Charsets.US_ASCII)

        openedStream.reset()

        Timber.d("Found 3DS serial: $rawSerial")
        return DiskInfo(rawSerial, SystemID.NINTENDO_3DS)
    }

    private fun extractInfoForSegaCD(openedStream: InputStream): DiskInfo {
        Timber.d("Parsing SegaCD game")
        openedStream.mark(20000)
        openedStream.skip(0x193)

        val rawSerial = String(readByteArray(openedStream, ByteArray(16)), Charsets.US_ASCII)

        Timber.d("Detected SegaCD raw serial read: $rawSerial")

        openedStream.reset()
        openedStream.skip(0x200)

        val regionID = String(readByteArray(openedStream, ByteArray(1)), Charsets.US_ASCII)

        Timber.d("Detected SegaCD region: $regionID")

        val groups = SEGA_CD_REGEX.find(rawSerial)?.groupValues

        // The following rules come from here: https://github.com/libretro/RetroArch/pull/11719/files
        // and some guess work. They are by no means complete.
        val prefix = groups?.get(1)
        val num = groups?.get(2)
        var postfix = groups?.get(3)

        if (regionID == "E") {
            postfix = "50"
        }

        if (postfix == "00") {
            postfix = null
        }

        val finalSerial =
            sequenceOf(prefix, num, postfix)
                .filterNotNull()
                .filter { it.isNotBlank() }
                .joinToString("-") { it.trim() }

        Timber.i("SegaCD final serial: $finalSerial")
        return DiskInfo(finalSerial, SystemID.SEGACD)
    }

    private fun extractInfoForPSX(openedStream: InputStream): DiskInfo {
        val headerSize = 64.kiloBytes()
        if (openedStream.available() < headerSize) {
            return DiskInfo(null, null)
        }

        return textSearch(PSX_BASE_SERIALS, openedStream, PS_SERIAL_MAX_SIZE, headerSize)
            .mapNotNull { serial -> parsePSXSerial(serial) }
            .mapNotNull { serial -> DiskInfo(serial, SystemID.PSX) }
            .firstOrNull() ?: DiskInfo(null, SystemID.PSX)
    }

    private fun extractInfoForPSP(openedStream: InputStream): DiskInfo {
        val headerSize = 64.kiloBytes()
        if (openedStream.available() < headerSize) {
            return DiskInfo(null, null)
        }

        return textSearch(PSP_BASE_SERIALS, openedStream, PS_SERIAL_MAX_SIZE, headerSize)
            .mapNotNull { serial -> parsePSXSerial(serial) }
            .mapNotNull { serial -> DiskInfo(serial, SystemID.PSP) }
            .firstOrNull() ?: DiskInfo(null, SystemID.PSP)
    }

    private fun extractInfoForPBP(openedStream: InputStream): DiskInfo {
        val headerSize = 2.megaBytes()
        if (openedStream.available() < headerSize) {
            return DiskInfo(null, null)
        }

        val queries = (PSP_BASE_SERIALS + PSX_BASE_SERIALS)

        return textSearch(queries, openedStream, PS_SERIAL_MAX_SIZE, headerSize)
            .mapNotNull { serial -> parsePSXSerial(serial) }
            .mapNotNull { serial ->
                when {
                    serial.startsWithAny(PSX_BASE_SERIALS) -> DiskInfo(serial, SystemID.PSX)
                    serial.startsWithAny(PSP_BASE_SERIALS) -> DiskInfo(serial, SystemID.PSP)
                    else -> DiskInfo(serial, null)
                }
            }
            .firstOrNull() ?: DiskInfo(null, null)
    }

    private fun parsePSXSerial(serial: String): String? {
        return sequenceOf(
            PS_SERIAL_REGEX.find(serial)?.groupValues?.let { "${it[1]}-${it[2]}" },
            PS_SERIAL_REGEX2.find(serial)?.groupValues?.let { "${it[1]}-${it[2]}${it[3]}" },
        ).filter { it != null }
            .firstOrNull()
    }

    private fun textSearch(
        queries: List<String>,
        openedStream: InputStream,
        resultSize: Int,
        streamSize: Int,
        windowSize: Int = 8.kiloBytes(),
        skipSize: Int = windowSize - resultSize,
        charset: Charset = Charsets.US_ASCII,
    ): Sequence<String> {
        val byteQueries = queries.map { it.toByteArray(charset) }
        return movingWidnowSequence(openedStream, windowSize, (skipSize).toLong())
            .take(ceil(streamSize.toDouble() / skipSize.toDouble()).roundToInt())
            .flatMap { serial ->
                byteQueries.asSequence()
                    .map { serial.indexOf(it) }
                    .filter { it >= 0 }
                    .map { serial to it }
            }
            .map { (bytes, index) ->
                val serialBytes = bytes.copyOfRange(index, index + resultSize)
                String(serialBytes, charset)
            }
            .filterNotNull()
    }

    private fun movingWidnowSequence(
        inputStream: InputStream,
        windowSize: Int,
        windowSkip: Long,
    ) = sequence {
        val buffer = ByteArray(windowSize)
        do {
            inputStream.mark(windowSize)
            yield(readByteArray(inputStream, buffer))
            inputStream.reset()
        } while (inputStream.skip(windowSkip) != 0L)
    }

    private fun readByteArray(
        inputStream: InputStream,
        byteArray: ByteArray,
    ): ByteArray {
        val readBytes = inputStream.read(byteArray)
        return if (readBytes < byteArray.size) {
            byteArray.copyOf(readBytes)
        } else {
            byteArray
        }
    }
}
