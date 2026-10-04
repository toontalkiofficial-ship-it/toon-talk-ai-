# Toon Talk AI

An Android app for generating images and short videos from text prompts with the Pollinations API.

## Current app features

- Pollinations device-flow sign-in: the app requests a one-time code, opens the approval page, and waits for approval.
- Optional Pollinations App Key (pk_...) field for app attribution. Create one at https://enter.pollinations.ai/keys.
- Text-to-image generation through GET /image/{prompt}.
- Short video generation through GET /video/{prompt} (the selected model must be available to the account).
- Image/video preview and save to the device Gallery.
- Clear messages for common API failures such as invalid key, exhausted budget, permissions, rate limits, and timeouts.
- API key stored in the app's private preferences; Android backup is disabled.

## Build a debug APK

GitHub Actions runs on every push to main.

1. Open the repository's Actions tab.
2. Select Build Toon Talk AI and open the latest successful run.
3. Under Artifacts, download Toon-Talk-AI-debug-APK.
4. Extract the ZIP and install the APK on an Android device. For direct sideloading, Android may ask you to allow installs from that source.

The artifact is a debug APK for testing, not a Play Store release.

## Pollinations setup and earnings

- Users can connect their Pollinations account inside the app using the device approval code.
- To attribute usage to this app and qualify for Pollinations developer earnings, the owner must create a Pollinations App Key at https://enter.pollinations.ai/keys and enable developer earnings for that key, then enter the resulting pk_ key in the app.
- Do not commit secret sk_ keys to this repository. The app does not contain a developer's personal secret key.
- Generation uses the connected user's Pollinations account and may consume their balance or budget.
- Video generation depends on Pollinations' currently available video models, permissions, and account budget.

## Before public release

This project is not yet a verified production release. Before publishing:
1. Confirm the GitHub Actions build succeeds and test the APK on a real Android phone.
2. Test image generation, video generation, Gallery save, network failures, and authorization expiry.
3. Configure the owner's Pollinations App Key with earnings enabled if developer attribution is desired.
4. Publish and review the privacy policy in PRIVACY.md at a stable public URL.
5. If adding AdMob, configure real app/ad unit IDs, a consent flow, and Play Console declarations. No production ad IDs are configured in this project yet.
6. Create a signed release build/AAB and prepare Play Store screenshots, icon, content rating, data-safety answers, and listing text.

## Privacy

See PRIVACY.md. The draft needs owner contact details and must be reviewed and hosted before public release.
