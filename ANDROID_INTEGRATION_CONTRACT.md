# Android Client Integration Contract

Written by the Claude session working on the Android app at
`C:\Users\Owner\IdeaProjects\faceid`, after reading through this backend's
actual current source (not just README.md/IMPLEMENTATION_SUMMARY.md, which
are stale relative to what's actually implemented). Purpose: reconcile the
two codebases so the Android app can talk to this backend directly, instead
of the temporary local mock backend I built for testing.

**Good news first**: this backend is much further along than its own docs
suggest. Real JWT auth, real Role-based access control, and a genuine
ArcFace/YuNet face-matching pipeline already exist. The gaps below are all
about the two sides not having talked to each other yet — nothing here is a
sign of missing capability.

## What already aligns, no action needed

- `POST /auth/login` path and `Authorization: Bearer <jwt>` scheme — exact match.
- `Role.UPLOADER` enum name — exact match with what the Android app checks.
- Multipart size limit (50MB) — matches the Android app's own video-compression threshold.
- `POST /api/v1/incidents/identify` already exists with the same intent as the
  Android app's "Live Check" feature (check a snapshot against the roster
  without creating a formal incident record). Concept is already aligned.
- `Authorization` header extraction (`JwtAuthFilter`) works exactly as the
  Android app's `AuthInterceptor` sends it.
- Seeded test accounts already exist: `admin` / `admin123` and
  `uploader` / `uploader123` (see `DataSeeder.java`).

## Gaps to resolve, in priority order

### 1. JSON field naming: camelCase vs snake_case (blocks everything)

Every DTO here (`LoginResponseDTO`, `IncidentResponseDTO`, `MatchProposalDTO`,
etc.) uses plain Java field names with no `@JsonProperty` overrides, so
Jackson serializes camelCase (`fullName`, `tokenType`, `userId`, `studentId`,
`confidenceScore`, `checkedAt`). The Android app was built against the
original spec's snake_case contract (`full_name`, `incident_id`,
`uploaded_at`, `confidence`, `checked_at`, etc.).

**Recommended fix**: add to `application.properties`:
```properties
spring.jackson.property-naming-strategy=SNAKE_CASE
```
One line, affects all DTOs automatically. **Before flipping this globally**,
check whether `frontend/*.html` + `app.js` (the admin dashboard) already
depend on the current camelCase responses — if so, either update the
dashboard's JS to match, or scope the naming strategy to just the DTOs the
mobile app hits (`@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`
on `LoginResponseDTO`, `IncidentResponseDTO`, `IncidentListItemDTO`, and the
identify-response wrapper specifically).

### 2. No uploader-facing "my incidents" endpoint (History screen has nothing to call)

`GET /api/v1/incidents` and `GET /api/v1/incidents/{id}` are both
`@PreAuthorize("hasRole('ADMIN')")`. There is currently no way for an
UPLOADER to see their own submission history — but the Android app has a
whole History screen built around exactly that (list of past uploads with
status badges, pull-to-refresh, tap for detail).

Two things are needed:
- The `Incident` entity has no column tracking who submitted it. Add
  `uploaded_by_user_id` (populate it in `IncidentService.processIncidentImage`
  from the authenticated user, same way `IncidentController` already does
  for audit logging).
- Add a new endpoint, e.g.:
  ```java
  @GetMapping("/mine")
  @PreAuthorize("hasRole('UPLOADER')")
  public ResponseEntity<List<IncidentListItemDTO>> getMyIncidents() { ... }
  ```
  filtered to `incident.getUploadedByUserId().equals(currentUser.getId())`,
  returning (after the snake_case fix above):
  `[{incident_id, thumbnail_url, status, uploaded_at, notes}]`.

### 3. Incident has no `notes` field

The Android app lets an uploader attach a short note at submission time
(e.g. "Fight near block C"). `Incident.java` has no column for this, and
`/api/v1/incidents/upload` only accepts a `file` param. Add a `notes` column
and an optional `@RequestParam(required = false) String notes` to the upload
endpoint, threaded through to both the upload response and the `/mine` list.

### 4. `POST /auth/signup` doesn't exist — needs a product decision, not just code

Account creation is currently `POST /auth/users`, ADMIN-only. The Android
app currently has a self-service Sign Up screen (added mid-session at the
product owner's request, before this backend's design was known).

For a campus security app, admin-provisioned uploader accounts (current
design here) is the safer default — it stops anyone from self-registering
as "security staff" with incident-submission access. **Recommend**: drop the
Android app's self-service Sign Up screen, and instead have admins create
uploader accounts via the existing `/auth/users` endpoint (dashboard already
has the pieces for this). If self-service really is wanted, it needs a gate
(invite code, email-domain allowlist, or admin-approval queue) — don't just
open `POST /auth/signup` with no gate.

### 5. Upload response is missing `status`

`IncidentResponseDTO` has `{incidentId, timestamp, mediaPath, matches}` but
no `status`, even though `Incident.status` defaults to `"PENDING"` in the DB.
Add `.status(incident.getStatus())` to the DTO and its construction in
`IncidentService`, so the Android app's post-submit confirmation screen has
something to show immediately.

### 6. Status casing: `"PENDING"` vs `"pending"`

The DB/DTOs use uppercase status strings; the Android app's
`kotlinx.serialization` enum maps lowercase (`"pending"`, `"confirmed"`,
`"rejected"`), matching the original spec's documented convention.
**Recommend**: lowercase the status string at the DTO boundary
(`incident.getStatus().toLowerCase()`) rather than changing the DB values,
so existing admin-side logic keyed on uppercase strings isn't disturbed.

### 7. Path prefix mismatch (client-side fix, no backend change needed)

Incidents live under `/api/v1/incidents/*`; auth is unprefixed at `/auth/*`.
This is a one-time fix on the Android Retrofit interface
(`api/v1/incidents/upload` instead of `incidents/upload`) — noting it here
just so it isn't missed, not asking for a backend change.

### 8. Live Check field names differ

Backend's `/api/v1/incidents/identify` returns:
```json
{"matches": [{"studentId": 42, "studentName": "...", "studentClass": "...", "confidenceScore": 0.82}], "checkedAt": "..."}
```
Android's `IdentifyMatch` model expects:
```json
{"person_id": "...", "name": "...", "confidence": 0.82, "photo_url": null}
```
After the snake_case fix, just need `studentId→person_id`,
`studentName→name`, `confidenceScore→confidence` renamed in
`MatchProposalDTO` (or tell me the final names and I'll adjust the Android
model instead — whichever is less churn on your side). `studentClass` and
`photo_url` don't need to match 1:1; extra/missing optional fields are fine.

### 9. Thumbnail URLs need to be absolute

`IncidentListItemDTO.mediaUrl` is built as a relative path
(`"/uploads/" + filename`). The Android app loads thumbnails via Coil and
needs an absolute URL. Build it the same way `IncidentController` already
has the request available:
```java
String base = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
String thumbnailUrl = base + "/uploads/" + fileName(incident.getMediaPath());
```
(Static serving at `/uploads/**` is already correctly wired in
`WebConfig.java` — just needs the absolute-URL wrapping.)

## Once these land

Send back (or just tell me directly): the deployed base URL, and confirmation
of whatever final field/path names you actually ship if they differ from the
recommendations above. I'll point the Android app's `BASE_URL` and Retrofit
interface at this backend directly and retire the temporary local mock
backend entirely.
