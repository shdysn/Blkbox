#!/usr/bin/env python3
"""
Self-Repair Script for Android Gradle Builds in GitHub Actions.
Performs live-streamed build execution, env/keystore restoration,
gradle.properties hardening, and automated recovery on failure.
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path

def log(msg):
    print(f"[Self-Repair] {msg}", flush=True)

def restore_environment():
    # 1. Restore debug.keystore if missing
    base64_file = Path("debug.keystore.base64")
    target_keystore = Path("debug.keystore")
    if base64_file.exists() and not target_keystore.exists():
        log("Restoring missing debug.keystore from debug.keystore.base64...")
        try:
            import base64
            data = base64.b64decode(base64_file.read_text().strip())
            target_keystore.write_bytes(data)
            log("debug.keystore successfully restored.")
        except Exception as e:
            log(f"Warning: Keystore restore failed: {e}")

    # 2. Restore .env file if missing (required by secrets gradle plugin)
    env_file = Path(".env")
    env_example = Path(".env.example")
    if not env_file.exists():
        if env_example.exists():
            log("Creating .env from .env.example...")
            shutil.copy(env_example, env_file)
        else:
            log("Creating default .env placeholder...")
            env_file.write_text("# Placeholder .env\n")

def fix_gradle_properties():
    props_file = Path("gradle.properties")
    if props_file.exists():
        content = props_file.read_text()
        lines = content.splitlines()
        modified = False
        
        for i, line in enumerate(lines):
            if line.startswith("org.gradle.jvmargs="):
                lines[i] = "org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8 -Djava.awt.headless=true"
                modified = True
            elif line.startswith("org.gradle.caching="):
                if "false" not in line:
                    lines[i] = "org.gradle.caching=false"
                    modified = True
            elif line.startswith("org.gradle.configuration-cache="):
                if "false" not in line:
                    lines[i] = "org.gradle.configuration-cache=false"
                    modified = True
            elif line.startswith("org.gradle.parallel="):
                if "false" not in line:
                    lines[i] = "org.gradle.parallel=false"
                    modified = True
            elif line.startswith("kotlin.compiler.execution.strategy="):
                if "out-of-process" not in line:
                    lines[i] = "kotlin.compiler.execution.strategy=out-of-process"
                    modified = True

        if modified:
            log("Hardened gradle.properties (2GB max heap, disabled parallel & caching, enabled headless).")
            props_file.write_text("\n".join(lines) + "\n")

def clean_stale_caches():
    home = Path.home()
    gradle_user_home = home / ".gradle"
    stale_paths = [
        gradle_user_home / "caches",
        gradle_user_home / "daemon",
        Path(".gradle"),
        Path("build"),
        Path("app/build")
    ]
    for path in stale_paths:
        if path.exists():
            log(f"Cleaning stale cache path: {path}")
            try:
                if path.is_dir():
                    shutil.rmtree(path, ignore_errors=True)
                else:
                    path.unlink(missing_ok=True)
            except Exception as e:
                log(f"Cache clean note ({path}): {e}")

def run_cmd_live(cmd):
    log(f"Executing: {' '.join(cmd)}")
    process = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )
    for line in iter(process.stdout.readline, ''):
        print(line, end='', flush=True)
    process.stdout.close()
    return_code = process.wait()
    return return_code == 0

def self_repair_build(target_task):
    log(f"Starting self-healing execution for task: {target_task}")
    
    # Pre-build environment preparation
    restore_environment()
    fix_gradle_properties()
    
    cmd = ["./gradlew", target_task, "--no-daemon", "--no-configuration-cache", "--no-build-cache", "--stacktrace"]
    
    # First attempt with live log streaming
    success = run_cmd_live(cmd)
    if success:
        log(f"Task {target_task} succeeded on Attempt 1!")
        return 0

    log(f"::warning::Attempt 1 for {target_task} failed. Executing self-repair sequence...")
    
    # Self-repair cleanup & re-verification
    run_cmd_live(["./gradlew", "--stop"])
    clean_stale_caches()
    restore_environment()
    fix_gradle_properties()
    
    log(f"Executing Attempt 2 after self-repair cleanup...")
    success = run_cmd_live(cmd)
    
    if success:
        log(f"Task {target_task} succeeded on Attempt 2 after self-repair!")
        return 0
    else:
        log(f"Task {target_task} failed after 2 attempts.")
        return 1

if __name__ == "__main__":
    task = sys.argv[1] if len(sys.argv) > 1 else ":app:assembleDebug"
    exit_code = self_repair_build(task)
    sys.exit(exit_code)
