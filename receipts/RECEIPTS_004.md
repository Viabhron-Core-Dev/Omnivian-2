
2026-08-05T11:05:00-07:00
* Requested: Further unpack Phase 3 and push agentic/sync/OmniRoute integrations to later phases, ensuring everything is granular.
* Files touched: `BLUEPRINT.md`
* Action: Created Phase 3 for completing all Chat Placeholders (model picker, thread 3-dots, attachments, action history states), Phase 4 for File Explorer wired to system, Phase 5 for Code Editor wired to Explorer. Global/Thread settings moved to Phase 6 & 7. Logic/Routing shifted safely to Phases 8, 9, 10.
* Verification: Not tested (blueprint update only).
* Deviation: None.
* Known issue/Follow-up: None.

2026-08-05T11:17:00-07:00
* Requested: Add Thread Settings, Export, Remix, and OmniRoute Dashboard to the 3-dots menu in the blueprint, and log them as an unmarked phase.
* Files touched: `BLUEPRINT.md`
* Action: Updated `BLUEPRINT.md` to detail the contents of the 3-dot menu on the Fixed Bottom Navigation. Added an 'Unmarked Phases / Backlog' section at the end of the Development Phases.
* Verification: Not tested (blueprint update only).
* Deviation: None.
* Known issue/Follow-up: Need to wait for user to provide remaining missing items to add to the unmarked phases.
* 2026-08-14
* Dropped the "Just Discuss" lock to fix the instant-abort bug in the `llama_bridge.cpp` JNI logic.
* Added missing token buffer resizing during `llama_tokenize` (llama.cpp returns a negative required size if the buffer is too small, which was previously causing it to abort silently).
* Increased `llama_context_params.n_ctx` to 4096 to prevent `llama_decode` from crashing on context overflow.
* Bound the C++ error logging to the Kotlin `onTokenGenerated` callback using `sendError("[ERROR...]")` so that any future C++ segmentation or decode failures are immediately printed to the Chat UI rather than silencing the thread.
* Recompiled C++ layer successfully.
