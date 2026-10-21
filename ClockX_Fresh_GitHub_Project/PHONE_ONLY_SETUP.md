# Setup from an Android phone

The ZIP contains the project **source**, not an APK. GitHub itself does not turn an uploaded ZIP into an Android app. The files must be present in the repository, including `.github/workflows/build-apk.yml`.

1. Create an empty GitHub repository and add the project files with their directory structure intact.
2. The included workflow runs on a push to `main`.
3. In the GitHub website on Chrome, open **Actions → Build ClockX APK** and open a successful workflow run.
4. Download its **ClockX-Installable-APK** artifact ZIP.
5. Open Samsung **My Files → Downloads** and extract it. Open `app-debug.apk` to install.
6. In ClockX, tap **Add round home-screen widget** or **Apply full-screen / lock-screen clock**.

Important: GitHub's mobile web 'Upload files' page may not retain nested folders when selecting files; a desktop browser or a git client may be necessary to upload the complete project.
