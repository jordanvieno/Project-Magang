#!/usr/bin/env bash
set -euo pipefail

NAME=registry
PORT=5050
AUTH_VOL=registry-auth
DATA_VOL=registry-data
REG_USER=jenkins

reset_pw=false
[ "${1:-}" = "--reset-password" ] && reset_pw=true

for v in "$AUTH_VOL" "$DATA_VOL"; do
  docker volume inspect "$v" >/dev/null 2>&1 || docker volume create "$v" >/dev/null
done

has_pw=$(docker run --rm -v "$AUTH_VOL":/auth alpine sh -c 'test -s /auth/htpasswd && echo yes || echo no')

P=""
if [ "$reset_pw" = true ] || [ "$has_pw" = no ]; then
  read -r -s -p "Password untuk user $REG_USER: " P; echo
  read -r -s -p "Ulangi password: " P2; echo
  [ -n "$P" ] && [ "$P" = "$P2" ] || { echo "Password kosong atau tidak sama"; exit 1; }
  printf '%s\n' "$P" \
    | docker run --rm -i --entrypoint htpasswd httpd:2 -Bin "$REG_USER" \
    | docker run --rm -i -v "$AUTH_VOL":/auth alpine sh -c 'cat > /auth/htpasswd'
  unset P2
fi

docker run --rm -v "$AUTH_VOL":/auth alpine grep -q "^$REG_USER:" /auth/htpasswd \
  || { echo "GAGAL: htpasswd tidak berisi user $REG_USER"; exit 1; }

docker rm -f "$NAME" >/dev/null 2>&1 || true
docker run -d --name "$NAME" --restart unless-stopped -p "$PORT":5000 \
  -v "$AUTH_VOL":/auth \
  -v "$DATA_VOL":/var/lib/registry \
  -e REGISTRY_AUTH=htpasswd \
  -e "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm" \
  -e REGISTRY_AUTH_HTPASSWD_PATH=/auth/htpasswd \
  registry:2 >/dev/null

code=000
for i in $(seq 1 15); do
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT/v2/" || true)
  [ "$code" = 401 ] && break
  sleep 1
done
[ "$code" = 401 ] || { echo "GAGAL: registry membalas $code, seharusnya 401"; exit 1; }

echo "Registry hidup. User di dalam container:"
docker exec "$NAME" cut -d: -f1 /auth/htpasswd

if [ -n "$P" ]; then
  printf '%s\n' "$P" | docker login "localhost:$PORT" -u "$REG_USER" --password-stdin
  unset P
else
  echo "Tes login manual: docker login localhost:$PORT -u $REG_USER"
fi
