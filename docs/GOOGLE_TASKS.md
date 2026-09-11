# Google Tasks sync

Version 0.8.0 implements phone-side Google Identity authorization and two-way reconciliation with the official Google Tasks REST API. The car and phone continue to use the local `TripListRepository`, so Mazda Commander-knob actions do not wait for a network request. Each explicit phone sync applies queued additions, completion changes, and deletions, then refreshes the local cache from a Google Tasks list named **Motoring**.

## Required Google Cloud setup

1. Enable the Google Tasks API in a Google Cloud project.
2. Configure OAuth branding/audience and add the testing Google account while the project remains in testing status.
3. Create Android OAuth clients for the package/certificate combinations that will be tested:

   - Play internal testing: `com.fluffnark.motoringdashboard`, Play app-signing SHA-1 `55:84:CF:D9:50:F8:E4:B7:AB:B8:8B:48:C7:B3:C5:CA:32:4A:41:DD`
   - Direct signed APK: `com.fluffnark.motoringdashboard`, upload-key SHA-1 `3F:A5:54:AC:23:15:7B:CA:B8:49:FC:FD:57:6A:31:3E:F7:A3:54:BF`
   - Local debug APK: `com.fluffnark.motoringdashboard.debug`, debug SHA-1 `91:97:DB:72:5F:30:BF:CD:53:E8:33:F8:86:E8:2D:A0:1E:A3:9C:CF`

4. Open the phone app and press **LOCAL** beside **Google Tasks**. Select the intended Google account and grant the Tasks permission. A successful sync displays the number of synced items.

The app requests only `https://www.googleapis.com/auth/tasks`. It does not embed a client secret, request offline server access, or persist an access token. Without the matching Cloud configuration, Google correctly rejects authorization (commonly developer error 10); that is configuration failure rather than a recoverable app-side credential.

## Voice behavior

Once synced, Gemini can add items to the same Google Tasks account using whatever Tasks actions Google makes available to that user. OpenAI's official Voice guidance does not guarantee that connected-app actions are available in Voice, so this project must not claim ChatGPT can update Google Tasks until that exact account/app combination is verified. ChatGPT can still be launched for a background voice conversation independently of the checklist.

References: [Google Tasks REST API](https://developers.google.com/workspace/tasks/reference/rest), [Tasks authorization scopes](https://developers.google.com/workspace/tasks/auth), and [Google Identity authorization](https://developers.google.com/identity/authorization).
