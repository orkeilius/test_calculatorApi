#!/bin/bash

BASE="http://127.0.0.1:3001"

pass() {
  echo "passé - $1"
}

fail() {
  echo " pas passé - $1"
  exit 1
}

test_body() {
  local name="$1"
  local url="$2"
  local expected="$3"

  local body
  body=$(curl -s "$url")

  if [ "$body" = "$expected" ]; then
    pass "$name"
  else
    echo "Attendu: $expected"
    echo "Reçu : $body"
    fail "$name"
  fi
}

test_status() {
  local name="$1"
  local url="$2"
  local expected="$3"

  local code
  code=$(curl -s -o /dev/null -w "%{http_code}" "$url")

  if [ "$code" = "$expected" ]; then
    pass "$name"
  else
    echo "Attendu: $expected"
    echo "Reçu: $code"
    fail "$name"
  fi
}

test_body "add 5+8" "$BASE/api/add?a=5&b=8" '{"result":13.00}'
test_body "sub 9-4" "$BASE/api/sub?a=9&b=4" '{"result":5.00}'
test_body "mul 6*7" "$BASE/api/mul?a=6&b=7" '{"result":42.00}'
test_body "div 20/5" "$BASE/api/div?a=20&b=5" '{"result":4.00}'

test_status "route invalide" "$BASE/api/zzz?a=1&b=2" "404"
test_status "division par zero" "$BASE/api/div?a=9&b=0" "400"

echo "ça fonctionne, yippie."