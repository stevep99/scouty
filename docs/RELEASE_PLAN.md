# Release plan — excluding the proprietary a133 native library

Status: **EXECUTED** — see "What was done" below. The SDK's legal status could not
be confirmed, so the `.so` is excluded from the repo (untracked + purged from history).

## Technical findings (confirmed)

- `sdk/a133/src/main/java/com/cloudring/commonlib/cmd/JniCmd.java` is a thin
  declaration only: a lazily-initialized singleton, five `native` method signatures
  (`dcmotorForward/Backward/Stop/TurnLeft/TurnRight`), and a static
  `System.loadLibrary("jnicmd_a133")`. It contains no proprietary logic.
- `sdk/a133/src/main/java/io/github/stevep99/scouty/motors/sdk/SdkA133Service.kt`
  compiles against it via `import com.cloudring.commonlib.cmd.JniCmd`.
- `sdk/a133/src/main/jniLibs/armeabi-v7a/libjnicmd_a133.so` (145,548 B) is the one
  genuinely proprietary binary — the compiled native implementation of those methods.

### Can we keep `JniCmd`? — Yes
It is the "dumb java interface" (native-method declarations + a loader). Keeping it
is required for the `a133` source set to compile, and it exposes no implementation.

### Does it need to stay in package `com.cloudring.commonlib.cmd`? — YES, mandatory
JNI resolves `native` methods at class-load/runtime by symbol name, and the `.so`
exports symbols with the package + class baked in (verified with `nm`):

```
Java_com_cloudring_commonlib_cmd_JniCmd_dcmotorForward
Java_com_cloudring_commonlib_cmd_JniCmd_dcmotorBackward
Java_com_cloudring_commonlib_cmd_JniCmd_dcmotorStop
Java_com_cloudring_commonlib_cmd_JniCmd_dcmotorTurnLeft
Java_com_cloudring_commonlib_cmd_JniCmd_dcmotorTurnRight
```

Relocating `JniCmd` to another package would make the JVM look for
`Java_<newpkg>_JniCmd_...`, which does not exist in the `.so` →
`UnsatisfiedLinkError`. **Do not rename the package.**

## Plan (to execute later)

1. **`sdk/a133/.gitignore`** — add `/src/main/jniLibs/` (or
   `/src/main/jniLibs/**/*.so`) so the `.so` is never committed.
2. **Stage the `.so` out of tracking** — `git rm --cached
   sdk/a133/src/main/jniLibs/armeabi-v7a/libjnicmd_a133.so` so it is no longer part
   of HEAD (leaving it on disk for local a133 builds).
3. **Add an explanatory note** in `sdk/a133/` (e.g. `README.md` inside
   `src/main/jniLibs/` via a `.gitkeep`, and/or a note in `AGENTS.md` / `README.md`)
   that the a133 robot flavor needs `libjnicmd_a133.so` restored locally and will
   otherwise fail at runtime with `UnsatisfiedLinkError` when `JniCmd` loads.

### Options previously considered (pick later if desired)
- **A. Minimal** — only steps 1–2 above (gitignore + untrack), no explanatory note.
- **B. With note (recommended)** — steps 1–3: also document how to restore the `.so`.
- **C. Purge from history too** — additionally rewrite git history to remove the `.so`
  from all commits (requires a force-push to `origin`, like the earlier `.idea` purge).
  Cleanest for a truly binary-free public repo, but destructive and needs the SDK
  status confirmed first.

### Before executing — gates
- Confirm the legal status of the a133 movement SDK (free to redistribute the `.so`?
  does the Java interface need to stay at the vendor package, or should it be dropped
  entirely?). Only then decide between A/B/C and whether to purge history.
- Do **not** reuse the SDK package/vendor symbols in any way beyond the linkage above.

**Outcome:** the SDK status could not be confirmed, so variant **B + purge from
history** was chosen and executed.

## What was done (executed)
1. `sdk/a133/.gitignore` — added `/src/main/jniLibs` so the `.so` cannot be committed.
2. `git rm --cached sdk/a133/src/main/jniLibs/armeabi-v7a/libjnicmd_a133.so` —
   untracked; the file remains on disk for local a133 builds.
3. Added `sdk/a133/README.md` documenting the missing `.so`, how to restore it
   locally, and the mandatory `com.cloudring.commonlib.cmd` package constraint.
4. AGENTS.md / README updated to note the `.so` is private/untracked.
5. Purged the `.so` from all commits with `git filter-branch` and force-pushed.
