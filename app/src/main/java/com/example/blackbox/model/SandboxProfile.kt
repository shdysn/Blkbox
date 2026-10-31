package com.example.blackbox.model

/**
 * Sandbox profile used to control how an approved internal app or app variant is launched.
 *
 * This is intentionally a sandbox policy model, not a clone engine.
 */
data class SandboxProfile(
    val appId: String,
    val sandboxId: String,
    val storageDir: String,
    val isNetworkRestricted: Boolean = true,
    val isExternalStorageIsolated: Boolean = true,
    val allowCamera: Boolean = false,
    val allowMicrophone: Boolean = false,
    val allowLocation: Boolean = false,
)
