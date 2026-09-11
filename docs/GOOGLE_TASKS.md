# Google Tasks voice-sync hook

The car and phone UIs depend on `TripListStore`; `TripListRepository` is the offline implementation. A future `GoogleTasksStore` can implement the same four operations and keep the dashboard UI unchanged.

Google Tasks is the preferred shared backend because its official API supports reading, creating, updating, and deleting tasks. Shipping that adapter requires project-specific Google Cloud configuration that is intentionally not committed here:

1. Enable the Google Tasks API in a Google Cloud project.
2. Configure the OAuth consent screen and an Android OAuth client for `com.fluffnark.motoringdashboard` plus the Play app-signing SHA-1 certificate.
3. Request the narrow `https://www.googleapis.com/auth/tasks` scope on the phone while parked.
4. Choose one task list, cache it locally, and reconcile remote task IDs with local `TripItem.id` values.
5. Keep authentication, conflicts, and network failures on the phone; the Mazda view remains a shallow host-rendered list.

Once synced, Gemini can add items to the same Google Tasks account using whatever Tasks actions Google makes available to that user. OpenAI's official Voice guidance does not guarantee that connected-app actions are available in Voice, so this project must not claim ChatGPT can update Google Tasks until that exact account/app combination is verified. ChatGPT can still be launched for a background voice conversation independently of the checklist.

References: [Google Tasks REST API](https://developers.google.com/workspace/tasks/reference/rest), [Tasks authorization scopes](https://developers.google.com/workspace/tasks/auth), and [Google Identity authorization](https://developers.google.com/identity/authorization).
