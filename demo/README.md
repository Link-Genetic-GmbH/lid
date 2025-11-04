# Demo & Recorded Fallback

This folder contains assets to run the live demo and a recorded fallback.

## Live Demo

- Start the resolver:
  - Docker: `docker compose up --build`
  - Local: `make run`
- Scripted flow: `bash demo.sh`
- Postman: import `LinkID.postman_collection.json`

## Recorded Fallback (terminal)

Use `asciinema` to record a session:

```bash
# macOS: brew install asciinema
asciinema rec demo.cast
# run: bash demo.sh
# press Ctrl-D to finish
```

Replay later:

```bash
asciinema play demo.cast
```

## Snapshots

If running live is not possible, use the static snapshots under `snapshots/` to show
expected responses for key endpoints.

- `health.json` — expected `/health` payload
- `wellknown.json` — expected `/.well-known/linkid-resolver` payload

> Note: IDs and timestamps are illustrative.


