# Smali & Dalvik Bytecode Hooking Guidelines

## 1. Register Architecture & Calling Conventions

In Dalvik bytecode, register naming follows strict conventions:
- **`p0`**: Represents `this` in instance methods, or the 1st parameter in static methods.
- **`p1, p2, ...`**: Subsequent incoming method arguments.
- **`v0, v1, ...`**: Local registers allocated for computation within the method frame.

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

## 2. Multi-branch & Label Management

Use `addInstructionsWithLabels` when introducing branching logic (`if-eqz`, `if-nez`, `goto`) to ensure relative jump offset recalculation:

```kotlin
method.addInstructionsWithLabels(0, """
    invoke-static {}, Landroid/app/ActivityThread;->currentApplication()Landroid/app/Application;
    move-result-object v0
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "brave_origin_off_BraveAIChatEnabled"
    invoke-interface {v0, v1}, Landroid/content/SharedPreferences;->contains(Ljava/lang/String;)Z
    move-result v0
    if-nez v0, :leo_disabled
    const/4 v0, 0x1
    return v0
    :leo_disabled
    const/4 v0, 0x0
    return v0
""".trimIndent())
```

---

## 3. Dynamic Reflection Bridges

When accessing private fields or methods across DEX boundaries where direct invocation causes `IllegalAccessError`:

```smali
invoke-virtual {p0, p1}, Lorg/chromium/chrome/browser/settings/BraveOriginPreferences;->P4(Ljava/lang/CharSequence;)Landroidx/preference/Preference;
move-result-object v0
if-eqz v0, :done
move-object v3, p0
const-string v1, "y"
const-class v2, Landroidx/preference/Preference;
invoke-virtual {v2, v1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
move-result-object v1
const/4 v2, 0x1
invoke-virtual {v1, v2}, Ljava/lang/reflect/Field;->setAccessible(Z)V
invoke-virtual {v1, v0, v3}, Ljava/lang/reflect/Field;->set(Ljava/lang/Object;Ljava/lang/Object;)V
:done
return-void
```

---

## 4. Register Sniffing & Injection Points

Do not assume hardcoded registers when modifying intermediate instructions. Extract the allocated register directly from the AST instruction node:

```kotlin
// Example: Overwrite register immediately following a target check
val matchIndex = fingerprint.instructionMatches[0].index
val resultReg = fingerprint.method
    .getInstruction<OneRegisterInstruction>(matchIndex + 1).registerA

// Neutralize value
fingerprint.method.addInstructions(matchIndex + 2, "const/4 v$resultReg, 0x0")
```
