"""Check the shipped standard APK, not just source manifests, for excluded capabilities."""
import argparse
import subprocess
import zipfile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    parser.add_argument("--aapt", required=True)
    args = parser.parse_args()
    manifest = subprocess.run(
        [args.aapt, "dump", "xmltree", args.apk, "AndroidManifest.xml"],
        check=True, capture_output=True, text=True, encoding="utf-8",
    ).stdout
    excluded = (
        "WhatsAppAccessService", "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.accessibilityservice.AccessibilityService",
        "android.permission.READ_SMS", "android.permission.RECEIVE_SMS",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
    )
    for value in excluded:
        if value in manifest:
            raise SystemExit(f"Standard APK unexpectedly declares {value}")
    with zipfile.ZipFile(args.apk) as apk:
        for name in apk.namelist():
            if name.endswith(".dex"):
                data = apk.read(name)
                for descriptor in (
                    b"Lcom/ghostos/ghost_os/WhatsAppAccessService;",
                    b"Landroid/accessibilityservice/AccessibilityService;",
                ):
                    if descriptor in data:
                        raise SystemExit(f"Standard APK includes excluded service code in {name}")
    print("PASS: packaged standard manifest and DEX exclude Accessibility, SMS, and notification-listener capabilities.")


if __name__ == "__main__":
    main()
