# Hey Pillu AI — starter project

This is a starter implementation assembled from the uploaded Markdown package. It has not been built on a real device and is not a production-ready phone-call assistant.

## Included
- Kotlin + Jetpack Compose dark chat UI
- Android speech-recognition intent (Marathi locale request)
- Android TextToSpeech Marathi locale request (device voice availability varies)
- Local intents for YouTube search, WhatsApp launch, Settings, Wi-Fi/Bluetooth settings, date and dialer
- FastAPI `/health` and authenticated `/chat`
- GitHub Actions debug APK workflow

## Important limitations / security
- The backend API key belongs only in backend environment variables. Never place `OPENAI_API_KEY` in Android.
- `PILLU_BACKEND_SECRET` is a starter shared token embedded as a build config value. This is not strong production authentication: anyone extracting the APK could recover it. Before public distribution, replace it with per-user authentication or short-lived tokens and server-side controls. Do not treat this starter as production-secure.
- The default backend URL is intentionally a placeholder. Change `DEFAULT_BACKEND_URL` in `app/build.gradle.kts` to your deployed HTTPS URL, and set the same backend token in Android build config and the host's `PILLU_BACKEND_SECRET`. Do not use a real secret in a public repository.
- The uploaded Markdown's call receiver and charging receiver are not sufficient to promise reliable call announcements. This starter receiver is best-effort only. Android 10+ restricts incoming phone-number exposure to ordinary apps; call-state receiver may not receive number. Do not rely on it for critical use.
- Auto-answer and speakerphone are NOT implemented. They require supported call-management APIs / often a user-selected default dialer and InCallService. This starter does not declare a default-dialer role or an InCallService.
- Power broadcasts may be restricted by OEM battery management; force-stop prevents normal background operation. TTS initialization from a short-lived receiver can be unreliable. Test on device.
- This app does not record calls, read WhatsApp chats, send WhatsApp messages automatically, or run an always-on wake word.
- Android speech recognition may use a provider and may require internet. Marathi recognition and voice depend on the installed speech engine.

## Backend setup
1. Use Python 3.11+.
2. `cd backend`
3. Create a virtual environment and install `pip install -r requirements.txt`.
4. Set `PILLU_BACKEND_SECRET` to a long random value in your hosting environment.
5. Set `OPENAI_API_KEY` in the backend hosting environment only. API usage may incur charges; this is not automatically free.
6. Run locally with `uvicorn main:app --host 0.0.0.0 --port 8000`.
7. Test `GET /health`. `/chat` requires `Authorization: Bearer <PILLU_BACKEND_SECRET>`.
8. Deploy to a provider that supports Python ASGI and HTTPS. Free tiers and limits change; verify current terms before relying on them.

## Build APK from a phone using GitHub Actions
1. Create a GitHub repository and upload all files from this project.
2. In `app/build.gradle.kts`, replace `https://YOUR-BACKEND.example.com` with the HTTPS URL of your deployed backend.
3. For a private test only, set `BACKEND_TOKEN` to match `PILLU_BACKEND_SECRET`. Do not publish this starter with a shared token; extracted APKs expose it.
4. Ensure `.github/workflows/build-apk.yml` is committed on `main`.
5. Open the repository's Actions tab, select **Build Hey Pillu Debug APK**, and tap **Run workflow**. A push to `main` also triggers it.
6. When it succeeds, open the workflow run, download the `HeyPilluAI-Debug-APK` artifact, extract the ZIP and install `app-debug.apk`.
7. Allow installation from that browser/file manager only if you trust the APK and repository.

## Build locally
Requires JDK 17 and Gradle/Android SDK. On a phone, GitHub Actions is usually easier than trying to install Android Studio.

## Smoke test
- Open app; verify dark UI.
- Tap mic and test Marathi recognition.
- Try "YouTube उघड", "WhatsApp उघड", "सेटिंग्ज उघड", "आजची तारीख सांग".
- Set a real HTTPS backend URL and check `/health`.
- Configure backend secrets in the host, then send a chat message.
- Test call and charging announcements cautiously; behavior is not guaranteed on all Android 16 / ColorOS builds.

## Current state
Source starter only. No APK was generated or verified in preparing this archive.
