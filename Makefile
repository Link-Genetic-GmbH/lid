.PHONY: run docker down test seed

PY_DIR=resolver/python

run:
	cd $(PY_DIR) && LINKID_ENV=development LINKID_SEED_FILE=seed.json uvicorn main:app --host 0.0.0.0 --port 8080

docker:
	docker compose up --build

down:
	docker compose down

test:
	cd $(PY_DIR) && python3 -m pytest -q

seed:
	@echo "Seed is loaded at startup when LINKID_SEED_FILE is set"


