#!/usr/bin/env bash
# Post-deployment smoke test: run against any ParcelPilot instance.
#   scripts/smoke.sh http://localhost:8080
set -euo pipefail
BASE="${1:-http://localhost:8080}"
pass=0

check() { # name, expected, actual
  if [[ "$2" == "$3" ]]; then pass=$((pass + 1)); echo "PASS $1"; else echo "FAIL $1: expected '$2', got '$3'"; exit 1; fi
}

echo "Waiting for $BASE to report healthy..."
for _ in $(seq 1 60); do
  if curl -fsS "$BASE/actuator/health" 2>/dev/null | grep -q '"UP"'; then break; fi
  sleep 1
done

check "health is UP" "UP" "$(curl -fsS "$BASE/actuator/health" | jq -r .status)"
check "build info names the artifact" "parcelpilot" "$(curl -fsS "$BASE/actuator/info" | jq -r .build.artifact)"
check "demo page is served" "200" "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/")"

QUOTE='{"originPincode":"500001","destinationPincode":"500032","weightGrams":400,"lengthCm":20,"widthCm":15,"heightCm":5,"service":"STANDARD","cashOnDelivery":false,"declaredValue":0}'
check "quote total for a first-slab city parcel" "4626" \
  "$(curl -fsS -X POST "$BASE/api/quotes" -H 'Content-Type: application/json' -d "$QUOTE" | jq -r .totalPaise)"

ID=$(curl -fsS -X POST "$BASE/api/shipments" -H 'Content-Type: application/json' \
  -d "{\"parcel\":$QUOTE,\"receiverName\":\"Smoke Test\"}" | jq -r .trackingId)
check "booking returns a tracking number" "ok" "$([[ "$ID" =~ ^PP[0-9]{10}$ ]] && echo ok || echo "$ID")"

check "pickup scan is accepted" "PICKED_UP" "$(curl -fsS -X POST "$BASE/api/shipments/$ID/events" \
  -H 'Content-Type: application/json' -d '{"status":"PICKED_UP","location":"Smoke hub"}' | jq -r .status)"
check "tracking shows two events" "2" "$(curl -fsS "$BASE/api/shipments/$ID" | jq '.events | length')"
check "illegal move is a 409" "409" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/shipments/$ID/events" \
  -H 'Content-Type: application/json' -d '{"status":"DELIVERED","location":"Smoke hub"}')"
check "bad tracking number is a 400" "INVALID_TRACKING_ID" "$(curl -s "$BASE/api/shipments/PP1234567890" | jq -r .code)"

echo "Smoke test passed: $pass checks"
