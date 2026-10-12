# Archive — GitBook stale server-side export (2026-10-11)

Not documentation — a one-time artifact of reconnecting the GitBook GitHub
integration after the repo transferred to the `hivevisionftc` org.

When the space was reconnected, GitBook **exported its existing (stale)
server-side content back into the repo** under `get-started/overview/` and
`updates/updates-releases/`, and re-pointed `gitbook-docs.yaml` at those
copies. That content predates the v2.0.0 release and both track archives
(still lists PC/ONNX Runtime and the Lab track as live), so it was moved here
and the yaml was re-pointed at the canonical directories:

| Canonical (live) | Stale export (this folder) |
|---|---|
| `docs/` | `get-started/overview/` |
| `updates/` | `updates-releases/` |
| `api/` | (exported in place — formatting normalization only, kept) |

Kept for reference only; not maintained, not synced to the site.
