# Google Tasks sync

Version 0.9.0 implements phone-side Google Identity authorization and two-way reconciliation with the official Google Tasks REST API. The car and phone continue to use the local `TripListRepository`, so Mazda Commander-knob actions do not wait for a network request. Each explicit phone sync applies queued additions, completion changes, and deletions, then refreshes the local cache from a Google Tasks list named **Motoring**.

## Required Google Cloud setup

Use the same Google Cloud project for the Tasks API, consent configuration, and Android OAuth clients.

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select the project.
2. Open **APIs & Services → Library**, find **Google Tasks API**, and press **Enable**.
3. Open **Google Auth Platform → Branding** and provide:

   - App name: `Motoring Dashboard`
   - A user-support email
   - A developer-contact email

4. Open **Google Auth Platform → Data Access → Add or remove scopes** and add exactly:

   `https://www.googleapis.com/auth/tasks`

   The application needs the write scope because it creates, completes, and deletes tasks. The read-only scope is insufficient.

5. For development and Play internal testing, open **Google Auth Platform → Audience**:

   - Set user type to **External** unless every tester belongs to one Google Workspace organization.
   - Leave publishing status set to **Testing**.
   - Under **Test users**, add every exact Google account that may be selected in the phone's account chooser.
   - Save the audience configuration.

   A Play Console internal tester is **not automatically an OAuth test user**. These are independent allowlists. If the account is missing here, Google returns `403: access_denied` and says the app has not completed verification.

6. Open **Google Auth Platform → Clients** and create Android clients for the package/certificate combinations that will be tested:

   - Play internal testing: `com.fluffnark.motoringdashboard`, Play app-signing SHA-1 `55:84:CF:D9:50:F8:E4:B7:AB:B8:8B:48:C7:B3:C5:CA:32:4A:41:DD`
   - Direct signed APK: `com.fluffnark.motoringdashboard`, upload-key SHA-1 `3F:A5:54:AC:23:15:7B:CA:B8:49:FC:FD:57:6A:31:3E:F7:A3:54:BF`
   - Local debug APK: `com.fluffnark.motoringdashboard.debug`, debug SHA-1 `91:97:DB:72:5F:30:BF:CD:53:E8:33:F8:86:E8:2D:A0:1E:A3:9C:CF`

7. Wait a few minutes for a newly saved client or tester to propagate. Open the phone app and press **LOCAL** beside **Google Tasks**. Select one of the accounts added in step 5 and grant the Tasks permission. A successful sync displays the number of synced items.

## Fix `403: access_denied` during internal testing

1. Confirm the account shown in the error is listed under **Google Auth Platform → Audience → Test users**.
2. Confirm publishing status is **Testing**, not an incomplete **In production** verification attempt.
3. Confirm the Tasks scope appears under **Data Access** and matches the scope requested by the app exactly.
4. Confirm the Android client matches the installed artifact. An `adb` debug installation uses the `.debug` package/certificate entry; a Play installation uses the base package and Play app-signing certificate.
5. Confirm the Tasks API and OAuth clients are in the same Cloud project.
6. Retry the phone-side Google Tasks control. If Google cached an older denial, remove Motoring Dashboard from [Google Account third-party connections](https://myaccount.google.com/connections), then authorize again.

Do not add unrelated broad scopes to work around the error. Google compares the scopes requested in code with the scopes declared on the consent screen.

The app requests only `https://www.googleapis.com/auth/tasks`. It does not embed a client secret, request offline server access, or persist an access token. Android client mismatches produce developer error 10; audience/verification problems produce access denied. Version 0.8.3 and later turn these into short phone-side setup hints.

Testing status supports up to 100 explicitly listed test users without completing production verification. Google warns those users that the app is unverified, and their grants expire after seven days. This is appropriate for the current private prototype.

## Production verification

Before allowing arbitrary Google accounts, change the production Cloud project to **In production** and submit the requested Tasks scope for verification. Google currently asks for accurate branding, verified owned domains, a public home page and privacy policy, a scope justification, and a video demonstrating the OAuth flow and how Tasks data is used. Keep test/debug OAuth clients in a separate development project when preparing the production project for review.

## Voice behavior

Once synced, Gemini can add items to the same Google Tasks account using whatever Tasks actions Google makes available to that user. OpenAI's official Voice guidance does not guarantee that connected-app actions are available in Voice, so this project must not claim ChatGPT can update Google Tasks until that exact account/app combination is verified. ChatGPT can still be launched for a background voice conversation independently of the checklist.

References: [Google Tasks REST API](https://developers.google.com/workspace/tasks/reference/rest), [Tasks authorization scopes](https://developers.google.com/workspace/tasks/auth), and [Google Identity authorization](https://developers.google.com/identity/authorization).
