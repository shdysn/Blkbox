#!/usr/bin/env python3
"""
Self-Repair Script for Android Gradle Builds in GitHub Actions.
Inspects build environment, restores credentials, repairs properties,
cleans corrupted caches, and executes Gradle build with intelligent fallback.
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path

def log(msg):
    print(f"[Self-Repair] {msg}", flush=True)

def restore_keystore():
    base64_file = Path("debug.keystore.base64")
    target_file = Path("debug.keystore")
    if base64_file.exists() and not target_file.exists():
        log("Restoring missing debug.keystore from debug.keystore.base64...")
        try:
            import base64
            data = base64.b64decode(base64_file.read_text().strip())
            target_file.write_bytes(data)
            log("debug.keystore successfully restored.")
        except Exception as e:
            log(f"Warning: Keystore restore failed: {e}")

def fix_gradle_properties():
    props_file = Path("gradle.properties")
    if props_file.exists():
        content = props_file.read_text()
        required_flags = [
            "org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8 -Djava.awt.headless=true",
            "org.gradle.parallel=true",
            "kotlin.compiler.execution.strategy=in-process"
        ]
        modified = False
        lines = content.splitlines()
        
        # Ensure java.awt.headless=true is in org.gradle.jvmargs
        for i, line in enumerate(lines):
            if line.startswith("org.gradle.jvmargs="):
                if "-Djava.awt.headless=true" not in line:
                    lines[i] = line + " -Djava.awt.headless=true"
                    modified = True

        if modified:
            log("Updated gradle.properties with headless JVM flags.")
            props_file.write_text("\n".join(lines) + "\n")

def clean_stale_caches():
    home = Path.home()
    gradle_user_home = home / ".gradle"
    stale_paths = [
        gradle_user_home / "caches" / "journal-1",
        gradle_user_home / "caches" / "locks",
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

def run_cmd(cmd, check=False):
    log(f"Running command: {' '.join(cmd)}")
    res = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    print(res.stdout, flush=True)
    return res.returncode == 0

def self_repair_build(target_task):
    log(f"Starting self-healing execution for task: {target_task}")
    
    # Pre-build fixes
    restore_keystore()
    fix_gradle_properties()
    
    cmd = ["./gradlew", target_task, "--no-daemon", "--no-configuration-cache", "--stacktrace"]
    
    # First attempt
    success = run_cmd(cmd)
    if success:
        log(f"Task {target_task} succeeded on Attempt 1!")
        return 0

    log(f"::warning::Attempt 1 for {target_task} failed. Executing self-repair sequence...")
    
    # Self-repair actions
    run_cmd(["./gradlew", "--stop"])
    clean_stale_caches()
    restore_keystore()
    fix_gradle_properties()
    
    log(f"Executing Attempt 2 after self-repair cleanup...")
    success = run_cmd(cmd)
    
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
