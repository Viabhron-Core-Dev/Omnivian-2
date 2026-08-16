2026-08-05T11:23:00-07:00
* Requested: Add Global Sidebar placeholder replacements, PWA bottom sheet preview, and Artifacts to the unmarked phases / backlog in the blueprint. 
* Files touched: `BLUEPRINT.md`
* Action: Updated `BLUEPRINT.md` to include Global Sidebar UI completion, Artifacts implementation, and PWA Bottom Sheet Preview to the 'Unmarked Phases / Backlog' section as separate granular phases.
* Verification: Not tested (blueprint update only).
* Deviation: None.
* Known issue/Follow-up: Need to begin implementation phase 3 as discussed.
* 2026-08-14
* Dropped the "Just Discuss" lock and implemented deep C++ to Kotlin logging via `LogKeeper`.
* Implemented `JNI_OnLoad` in `llama_bridge.cpp` to cache the `JavaVM` and a global reference to the `LlamaEngine` class.
* Updated `LlamaEngine.kt` to expose a `@JvmStatic onNativeLog` method that routes direct to `LogKeeper.log`.
* Rewrote C++ `LOGI` and `LOGE` macros to push formatted logs across the JNI barrier using `CallStaticVoidMethod`.
* Hooked into `llama_log_set()` to intercept all internal `ggml` and `llama.cpp` neural network logs and route them straight into the Android LogKeeper interface.

* 2026-08-14
* Fixed KV Cache sequence mismatch crash (`[ERROR: llama_decode failed on initial prompt]` with inconsistent sequence positions `Y = 0, X = 18`).
* Implemented `llama_memory_clear(llama_get_memory(ctx), true)` in `llama_bridge.cpp` before prompt evaluation to reset the KV cache and memory state for each new chat message.
* Enabled multi-threaded CPU matrix compute (`n_threads` and `n_threads_batch` set dynamically using `sysconf(_SC_NPROCESSORS_ONLN)` and `llama_set_n_threads`) to boost generation speed from 0.46 TPS to full hardware multi-core speed.
* Compiled and verified cleanly.

* 2026-08-14
* Implemented on-disk persistence for `LogKeeper` via `omniroot_active_logs.jsonl` and `SharedPreferences` so that all app logs and toggle preferences survive app restarts.
* Implemented persistent active chat tracking via `SharedPreferences` ("active_chat_id") in `OmniRootApp.kt`, preventing the app from wiping/resetting active chat sessions on restart.
* Connected chat session deletion to Room database cleanups (`clearSession` and `deleteConfig`).
* Synchronized dynamic chat workspace renaming across UI, disk metadata, and Room `WorkspaceConfigDao`.
* Verified clean compilation with `compile_applet`.
