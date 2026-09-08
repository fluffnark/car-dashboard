# Architecture

`DashboardCarAppService` creates one `DashboardSession`, whose root is `MapScreen`. `MapScreen` owns the host-rendered `MapWithContentTemplate`, compact telemetry content, POI list, and action strips.

`CarMapSurface` receives Android Auto's official surface through `AppManager.setSurfaceCallback`. It creates a `VirtualDisplay` and `Presentation`, then lets MapLibre render its `MapView` into that display. There is no recurring full-screen bitmap capture or private projection protocol. Surface creation, destruction, safe-area updates, gestures, and map lifecycle are handled in one owner.

Data remains separated by responsibility:

- `LocationDriveSource`: live Android location, trip accumulation, heading, elevation, and grade
- `VehicleDataSource`: optional Android Auto car-hardware readings and permission lifecycle
- `PoiRepository`: distance calculation and the initial curated regional guide
- `SimulationRoute`: debug-only repeatable US-550 drive
- `MapPreferences`: persisted cartography choice
- `MapStyle`: original day/night OpenMapTiles style JSON

The custom surface is the spatial layer. Android Auto host templates remain responsible for actions, focus, rotary navigation, safe interaction limits, and the telemetry pane.

## Known limitations

- The first POI dataset is a small curated San Juan Mountains guide, not a worldwide search service.
- Current-road names are not resolved from a geocoder yet.
- Live grade depends on consecutive GPS altitude samples and is unavailable when altitude is missing.
- Upcoming-route elevation requires routing and a terrain data source; the current sparkline shows recent samples.
- Map tiles are online-only. The style endpoint is isolated so an offline provider can replace it later.
- Actual Mazda car-hardware exposure varies by Android Auto host implementation and permissions.
- The ChatGPT voice activity is runtime-checked but not a stable documented integration contract.
