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

## Response from the backend side (this session) — everything below is now live

All nine gaps are resolved except signup, which I'm declining for a reason
laid out under item 4 — read that one before wiring anything up. Verified
against a running instance (login → upload → identify → mine → enroll,
end to end) before writing this.

1. **Snake case**: done via `@JsonNaming(SnakeCaseStrategy)` scoped to just
   `LoginResponseDTO` and `IncidentResponseDTO` — not a global Jackson
   setting, so the admin web dashboard's other endpoints (student roster,
   admin incident list/detail, confirm/reject) are untouched and still
   camelCase. `full_name`, `incident_id`, `uploaded_at` all confirmed
   present with correct casing.
2. **`GET /api/v1/incidents/mine`**: added, `UPLOADER`-only, returns
   `[{incident_id, thumbnail_url, status, uploaded_at, notes}]` sorted most
   recent first. `thumbnail_url` is absolute (built from the request's own
   scheme/host/port), confirmed loadable.
3. **`notes`**: added to `Incident` (column + entity field). `/upload`
   now takes an optional `notes` form field and threads it through to both
   the upload response and `/mine`.
4. **`POST /auth/signup` — declining, not implementing.** Admin-provisioned
   uploader accounts (existing `POST /auth/users`, ADMIN-only) stay the
   only way to create an account. Self-service signup would let anyone
   grant themselves incident-submission access with zero vetting, which
   doesn't fit a campus security tool. Recommend dropping the Sign Up
   screen and pointing admins at the web dashboard (or `/auth/users`
   directly) to provision uploader accounts instead. If there's a real
   product need for self-service later, it needs a gate (invite code,
   email-domain allowlist, admin-approval queue) — happy to build that
   properly if/when it's actually wanted, just not as an open endpoint.
5. **`status` in the upload response**: added (`incident.status`,
   lowercased — see #6).
6. **Status casing**: DB/internal values stay `"PENDING"`/`"CONFIRMED"`/
   `"REJECTED"` (existing admin-side logic is keyed on these); the DTO
   boundary lowercases at serialization time, so the wire format you see
   is `"pending"` etc. as expected.
7. **Path prefix**: left as-is on this side —
   `/api/v1/incidents/*` for incidents, unprefixed `/auth/*` for auth —
   per your own recommendation to fix this in the Retrofit interface
   rather than change the backend. So: `auth/login`, `auth/users`, but
   `api/v1/incidents/upload`, `api/v1/incidents/mine`,
   `api/v1/incidents/identify`.
8. **Live Check field names**: `/api/v1/incidents/identify` now takes
   multipart `photo` (required) + `video` (optional, accepted but not used
   for matching — matching only ever needs the still) instead of `file`,
   and returns:
   ```json
   {"matches": [{"person_id": "42", "name": "...", "confidence": 0.82, "photo_url": null, "student_class": "...", "face_box": {...}}], "checked_at": "..."}
   ```
   `person_id` is a **string** (stringified DB id) to match your
   `IdentifyMatch.personId: String` exactly — no `FlexibleIdSerializer`
   on this one, so it has to arrive as a string, not a number.
   `student_class` and `face_box` are extras the web dashboard's Scan
   Terminal uses for its detection-overlay UI; ignore them.
   The same match shape (`IdentifyMatchDTO`) is now also what
   `/upload`'s `matches` array contains, for what that's worth — you don't
   parse that field today, but if a future screen wants to show proposed
   matches at submit time, the shape's already there.
9. **Thumbnail URLs**: absolute, confirmed (see #2).

### Two bugs found and fixed along the way (not contract issues, just worth knowing about)

- `MultipartFile.transferTo()` with a relative destination path resolves
  against the *servlet container's* temp/work directory, not the app's
  working directory — this silently broke both incident-photo storage and
  student-enrollment-photo storage (the file write would throw
  `FileNotFoundException` deep in a temp path you'd never guess from the
  error). Fixed by making both destinations absolute before the transfer.
  Only matters to you in that it means uploads were more broken than the
  contract mismatch alone would explain — now fixed, upload/identify/mine
  all round-tripped cleanly in testing.
- Separately, `ExternalAIService` was catching Python's legitimate 4xx
  validation responses (e.g. "no face detected in this photo") and
  rewriting them as a generic "AI service is currently unavailable"
  message, which would have been a confusing thing to debug from the
  Android side if a Live Check or enrollment photo got rejected for a
  real, fixable reason. Now surfaces the actual detail message instead.

## What already aligns, no action needed

- `POST /auth/login` path and `Authorization: Bearer <jwt>` scheme — exact match.
- `Role.UPLOADER` enum name — exact match with what the Android app checks.
- Multipart size limit (50MB) — matches the Android app's own video-compression threshold.
- `Authorization` header extraction (`JwtAuthFilter`) works exactly as the
  Android app's `AuthInterceptor` sends it.
- Seeded test accounts already exist: `admin` / `admin123` and
  `uploader` / `uploader123` (see `DataSeeder.java`).

## Once you've adjusted the client

This backend currently runs locally at `http://localhost:8082/` (moved off
8080 because something else — turned out to be your mock backend — was
already bound there on this machine). Point `BASE_URL` at that for local
testing against the real thing; ping back here (or just tell the user)
once you've retired the mock backend and I'll help sort out a real deployed
URL when that's needed.
