# Architecture

`DashboardCarAppService` creates a `DashboardSession` whose root `MapScreen` supplies Android Auto's host-rendered `MapWithContentTemplate`, persistent action strip, and checklist. The class name remains for compatibility with the prototype history; there is no map renderer in 0.7.0.

`DashboardSurface` receives the official Android Auto `Surface` through `AppManager.setSurfaceCallback`. A dedicated `HandlerThread` locks its canvas only when data, theme, surface geometry, or the minute changes. Pending telemetry frames are coalesced and limited to one per second. `DashboardRenderer` draws the Instrument Sans speed dial, compass, clock, elevation trace, altitude, grade, trip, and available vehicle values directly to that canvas.

This replaces the former MapLibre `MapView`, `VirtualDisplay`, `Presentation`, network tile pipeline, and continuous camera rendering. The host-owned `Surface` is never released by the app; it is simply forgotten when Android Auto destroys it. Rendering exceptions caused by a concurrent host surface replacement are contained to the dropped frame.

Data remains separated by responsibility:

- `LocationDriveSource`: live phone location, trip accumulation, heading, elevation, and grade
- `VehicleDataSource`: optional Android Auto speed, fuel/range, mileage, and model readings
- `TripListStore`: checklist boundary shared by phone and car interfaces
- `TripListRepository`: current offline implementation; an authenticated Google Tasks adapter can replace it
- `SimulationRoute`: debug-only repeatable US-550 drive
- `DashboardRenderer`: instrument composition plus bounded elevation history

The canvas is display-only. All interaction remains in Android Auto host templates, which own focus, rotary navigation, touch, safety restrictions, and action sizing.

## Performance model

- No tile downloads, GPU map scene, virtual display, or bitmap-copy loop
- Maximum telemetry refresh: 1 Hz
- Clock refresh: once per minute
- Elevation history: 72 floats maximum
- One reusable render thread for the life of the screen
- Template invalidation only for a first position, permission state, or content change

## Known limitations

- Android Auto only exposes a custom drawing surface to eligible map-oriented categories. The no-map dashboard is therefore an internal POI prototype, not yet a public-store-compliant POI experience.
- The host controls split-screen category placement; the app cannot force another navigation or media app into a particular pane.
- Current-road names are not resolved from a geocoder.
- Live grade depends on consecutive GPS altitude samples; the graph shows recent, not upcoming, terrain.
- Actual Mazda car-hardware exposure varies by firmware, host implementation, and granted permissions.
- The ChatGPT voice activity is runtime-checked but is not a stable documented integration contract, and no documented external stop intent exists.
