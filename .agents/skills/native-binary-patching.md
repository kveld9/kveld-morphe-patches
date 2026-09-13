# Native Binary Patching Guidelines (ARM64)

## 1. Safety Protocol for `libchrome.so`

Any binary modification executed inside `rawResourcePatch` must adhere to strict bounds and verification protocols:

1. **Existence Verification**: Skip gracefully if targeting an APK without native ARM64 libraries (e.g. mock test builds).
2. **File Pointer Bounds Check**: Ensure `offset + replacementLength <= fileLength`.
3. **Pre-patch Fingerprint Assertion**: Read original bytes and assert exact match before mutating.

```kotlin
RandomAccessFile(soFile, "rw").use { raf ->
    val buf = ByteArray(4)
    if (gate.offset + 4 > raf.length()) {
        throw PatchException("Offset 0x${gate.offset.toString(16)} out of bounds")
    }
    raf.seek(gate.offset)
    raf.readFully(buf)
    if (!buf.contentEquals(gate.expectedOriginal)) {
        throw PatchException("Fingerprint mismatch at 0x${gate.offset.toString(16)}")
    }
    raf.seek(gate.offset)
    raf.write(gate.replacement)
}
```

---

## 2. ARM64 Instruction Encoding Patterns

ARM64 instructions are 32-bit (4 bytes) in Little-Endian format:

| Operation | Assembly | Hex Byte Array (Little Endian) |
| :--- | :--- | :--- |
| **NOP** | `nop` | `byteArrayOf(0x1f.toByte(), 0x20.toByte(), 0x03.toByte(), 0xd5.toByte())` |
| **Unconditional Branch** | `b <offset>` | Encoded as 6-bit opcode (`000101`) + 26-bit signed immediate word offset |
| **Return 0** | `mov w0, #0; ret` | `byteArrayOf(0x00, 0x00, 0x80, 0x52, 0xc0, 0x03, 0x5f, 0xd6)` |

### Common Bypass Strategies
- **Abort Branch Redirection**: Overwrite conditional branch (`tbz`, `tbnz`, `cbz`, `cbnz`, `b.eq`) with an unconditional branch (`b`) pointing directly to the cleanup or early return block.
- **Upload Suppression**: Replace entry branch to network dispatchers with `NOP` instructions.

---

## 3. In-Situ ASCII Host Redirection

When redirecting telemetry domains to `0.0.0.0`:
1. Calculate target host string byte length: `len = hostName.toByteArray(Charsets.US_ASCII).size`.
2. Construct replacement byte array: `"0.0.0.0".toByteArray(Charsets.US_ASCII)` padded with `0x00` (null bytes) up to `len`.
3. The total length of the binary string table must remain unchanged to preserve ELF segment offsets.

```kotlin
val redirectionIp = "0.0.0.0".toByteArray(Charsets.US_ASCII)
val replacement = ByteArray(len)
System.arraycopy(redirectionIp, 0, replacement, 0, redirectionIp.size)
// Remaining bytes default to 0x00
raf.seek(entry.offset)
raf.write(replacement)
```
