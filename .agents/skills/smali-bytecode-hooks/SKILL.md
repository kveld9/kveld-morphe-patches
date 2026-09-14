---
name: smali-bytecode-hooks
description: Smali and Dalvik bytecode hooking guidelines, register architecture conventions, multi-return reverse traversal, TwoRegisterInstruction extraction, reflection bridges, and obfuscation guidelines.
---

# Smali & Dalvik Bytecode Hooking Guidelines

## 1. Register Architecture & Essential Extension Imports

In Dalvik bytecode, register naming follows strict calling conventions:
- **`p0`**: Represents `this` in instance methods, or the 1st parameter in static methods.
- **`p1, p2, ...`**: Subsequent incoming method arguments.
- **`v0, v1, ...`**: Local registers allocated for computation within the method frame.

### Required Patcher Extension Imports
Always import the core instruction helper extensions from `app.morphe.patcher`:
```kotlin
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
```

### Safe Return Overrides
When replacing a complete method body, calculate register usage and wipe existing instructions:
```kotlin
fingerprint.method.apply {
    removeInstructions(0, implementation!!.instructions.count())
    addInstructions(0, """
        const/4 v0, 0x1
        return v0
    """.trimIndent())
}
```

---

## 2. Multi-Return Hooking & Reverse Traversal

When modifying or injecting hooks before return instructions (`return-void`, `return-object`, `return`), inserting instructions shifts the indices of all subsequent instructions within the method.

To maintain valid AST offsets without invalidating subsequent indices, **always collect return indices and iterate in reverse order** using `.asReversed()`:

```kotlin
val method = fingerprint.method
val returnIndices = method.implementation?.instructions?.withIndex()
    ?.filter { it.value.opcode == Opcode.RETURN_OBJECT }
    ?.map { it.index to (it.value as OneRegisterInstruction).registerA }
    ?.toList() ?: emptyList()

// Reverse iteration prevents index shifting
returnIndices.asReversed().forEach { (returnIndex, reg) ->
    method.addInstructions(
        returnIndex,
        """
            invoke-static {v$reg}, Lcom/kveld9/morphe/extension/Payload;->filter(Ljava/lang/Object;)V
        """.trimIndent(),
    )
}
```

---

## 3. Register Extraction & Instruction Sniffing

Never hardcode register numbers when modifying intermediate instructions. Extract dynamically allocated registers directly from the dexlib2 AST instruction nodes:

### `OneRegisterInstruction`
Applies to instructions with a single register reference (e.g. `return-object`, `move-result`, `check-cast`):
```kotlin
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val matchIndex = fingerprint.instructionMatches[0].index
val resultReg = fingerprint.method
    .getInstruction<OneRegisterInstruction>(matchIndex + 1).registerA

fingerprint.method.addInstructions(matchIndex + 2, "const/4 v$resultReg, 0x0")
```

### `TwoRegisterInstruction`
Applies to instructions with two register references (e.g. `iput`, `iget`, `iput-boolean`, `instance-of`):
- `registerA`: Destination/source data register (`vx`).
- `registerB`: Target instance/object reference register (`vy`).
*(Note: `check-cast` uses format 21c and is a `OneRegisterInstruction` with a type reference).*

```kotlin
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

// Example: Neutralizing a boolean field write (iput-boolean vx, vy, Field)
val putIndex = fingerprint.instructionMatches[1].index
val instruction = fingerprint.method.getInstruction<TwoRegisterInstruction>(putIndex)
val putReg = instruction.registerA      // Data value register (vx)
val objReg = instruction.registerB      // Object instance register (vy)

fingerprint.method.addInstructions(putIndex, "const/4 v$putReg, 0x0")
```

---

## 4. Multi-Branch & Label Management

Use `addInstructionsWithLabels` when introducing branching logic (`if-eqz`, `if-nez`, `goto`) to guarantee proper branch label target resolution and relative offset recalculation:

```kotlin
method.addInstructionsWithLabels(0, """
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "morphe_feature_enabled"
    invoke-interface {v0, v1}, Landroid/content/SharedPreferences;->contains(Ljava/lang/String;)Z
    move-result v0
    if-nez v0, :disabled
    const/4 v0, 0x1
    return v0
    :disabled
    const/4 v0, 0x0
    return v0
""".trimIndent())
```

---

## 5. Dynamic Reflection Bridges & Obfuscation Caveats

When accessing private fields or methods across DEX boundaries where direct invocation causes `IllegalAccessError`, use reflection bridges:

```smali
invoke-virtual {p0, p1}, Lorg/chromium/chrome/browser/settings/Preferences;->findPreference(Ljava/lang/CharSequence;)Landroidx/preference/Preference;
move-result-object v0
if-eqz v0, :done
move-object v3, p0
const-string v1, "mListener"
const-class v2, Landroidx/preference/Preference;
invoke-virtual {v2, v1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
move-result-object v1
const/4 v2, 0x1
invoke-virtual {v1, v2}, Ljava/lang/reflect/Field;->setAccessible(Z)V
invoke-virtual {v1, v0, v3}, Ljava/lang/reflect/Field;->set(Ljava/lang/Object;Ljava/lang/Object;)V
:done
return-void
```

### Obfuscation Warning & Guidelines
- Obfuscated identifiers (such as `P4`, `y`, `O0`) are volatile and change between releases.
- Always prefer anchoring on **stable framework APIs** (e.g. `androidx.preference.Preference`) or string literals.
- When obfuscated symbols are strictly necessary, isolate them in centralized contracts or verify them via RE audit scripts (`harness/update.py --audit`) against newly updated APKs before building.

---

## 6. Mandatory In-Situ Patcher Verification Gate

Any modified Smali hook, register rewrite, or dexlib2 fingerprint MUST be validated by running the Morphe Patcher against the target APK before declaring completion:
```bash
./gradlew runPatchTest -Papp=<targetApp>
```
The patching execution must conclude with **100% success** (0 failed patches, 0 fingerprint mismatches).
