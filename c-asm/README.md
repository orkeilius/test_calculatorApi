# 🧮 Calculator API — C / x86-64 Assembly

API REST de calculatrice dont le cœur arithmétique est écrit en **assembleur x86-64 (NASM)**, exposé via un serveur HTTP en **C** (libmicrohttpd).

---

## Table des matières

- [Architecture](#architecture)
- [Fichiers du projet](#fichiers-du-projet)
- [Prérequis](#prérequis)
- [Compilation & Lancement](#compilation--lancement)
  - [Avec Docker (recommandé)](#avec-docker-recommandé)
  - [Sans Docker](#sans-docker)
- [Utilisation de l'API](#utilisation-de-lapi)
  - [Endpoint](#endpoint)
  - [Paramètres](#paramètres)
  - [Opérations disponibles](#opérations-disponibles)
  - [Exemples](#exemples)
  - [Gestion des erreurs](#gestion-des-erreurs)
- [Tests](#tests)
  - [Tests unitaires (Unity)](#tests-unitaires-unity)
  - [Tests d'intégration API](#tests-dintégration-api)
- [Détail technique : l'assembleur](#détail-technique--lassembleur)

---

## Architecture

```
┌───────────────┐     HTTP GET      ┌──────────────┐     appel C → ASM     ┌──────────────────┐
│    Client     │ ─────────────────→│   server.c   │ ──────────────────────→│  calculator.asm  │
│  (curl, etc.) │                   │  libmicrohttpd│                       │   NASM x86-64    │
└───────────────┘     JSON ←────────└──────────────┘ ←──────────────────────└──────────────────┘
```

Le serveur C reçoit les requêtes HTTP, parse les paramètres, puis appelle directement les fonctions assembleur via le *calling convention* System V AMD64 (arguments dans `xmm0` / `xmm1`, retour dans `xmm0`).

---

## Fichiers du projet

| Fichier | Rôle |
|---|---|
| `calculator.asm` | Implémentation des 4 opérations en assembleur x86-64 |
| `server.c` | Serveur HTTP REST (libmicrohttpd) exposant l'API `/calculate` |
| `test_calculator.c` | Tests unitaires des fonctions assembleur (framework Unity) |
| `run_tests.sh` | Script de compilation et exécution des tests unitaires |
| `test_api.sh` | Tests d'intégration de l'API HTTP via `curl` |
| `Dockerfile` | Image Docker pour build et déploiement |
| `Unity/` | Framework de tests unitaires C (ThrowTheSwitch/Unity) |

---

## Prérequis

| Outil | Version minimale | Usage |
|---|---|---|
| `gcc` | 9+ | Compilation C |
| `nasm` | 2.14+ | Assembleur x86-64 |
| `libmicrohttpd-dev` | 0.9+ | Serveur HTTP en C |
| `make` | — | Build (optionnel) |
| `docker` | 20+ | Build/run conteneurisé |

---

## Compilation & Lancement

### Avec Docker (recommandé)

```bash
# Build de l'image
docker build -t calculator-api .

# Lancement du conteneur
docker run -p 3001:3001 calculator-api
```

Le serveur écoute sur `http://localhost:3001`.

### Sans Docker

```bash
# 1. Assembler le fichier ASM
nasm -f elf64 calculator.asm -o calculator.o

# 2. Compiler et linker le serveur
gcc server.c calculator.o -o calculator-api -lmicrohttpd

# 3. Lancer le serveur
./calculator-api
```

---

## Utilisation de l'API

### Endpoint

```
GET /calculate
```

### Paramètres

| Paramètre | Type | Requis | Description |
|---|---|---|---|
| `operation` | `string` | ✅ | Opération à effectuer |
| `a` | `number` | ✅ | Premier opérande |
| `b` | `number` | ✅ | Second opérande |

### Opérations disponibles

| Valeur de `operation` | Opération | Fonction ASM appelée |
|---|---|---|
| `add` | Addition | `add_numbers` (`addsd`) |
| `subtract` | Soustraction | `sub_numbers` (`subsd`) |
| `multiply` | Multiplication | `mul_numbers` (`mulsd`) |
| `divide` | Division | `div_numbers` (`divsd`) |

### Exemples

**Addition :**
```bash
curl "http://localhost:3001/calculate?operation=add&a=5&b=8"
# → {"result":13.00}
```

**Division :**
```bash
curl "http://localhost:3001/calculate?operation=divide&a=20&b=5"
# → {"result":4.00}
```

### Gestion des erreurs

Toutes les réponses sont au format JSON avec le header `Content-Type: application/json`.

| Cas | Code HTTP | Réponse |
|---|---|---|
| Paramètres manquants | `400` | `{"error":"parametres manquants"}` |
| Division par zéro | `400` | `{"error":"division par zero"}` |
| Opération inconnue | `400` | `{"error":"operation inconnue"}` |
| Route inconnue | `404` | `{"error":"route inconnue"}` |
| Méthode non-GET | `405` | `{"error":"methode non autorisee"}` |

> **Note :** Le header `Access-Control-Allow-Origin: *` est présent sur toutes les réponses (CORS ouvert).

---

## Tests

### Tests unitaires (Unity)

Les tests unitaires vérifient directement les fonctions assembleur depuis le C, sans passer par le serveur HTTP. Ils utilisent le framework [Unity](https://github.com/ThrowTheSwitch/Unity).

```bash
./run_tests.sh
```

**Cas testés :**

| Fonction | Cas couverts |
|---|---|
| `add_numbers` | positifs, zéros, négatifs, grands nombres |
| `sub_numbers` | positifs, négatifs, zéros, deux négatifs |
| `mul_numbers` | positifs, par zéro, négatifs, flottants |
| `div_numbers` | entiers, décimaux, négatifs, division non exacte |
| `div_numbers` | `0 / 0` → retourne `NaN` |

### Tests d'intégration API

Vérifient le serveur HTTP de bout en bout via `curl` :

```bash
# Le serveur doit être lancé au préalable
./test_api.sh
```

Tests effectués : addition, soustraction, multiplication, division, route invalide (→ 404), division par zéro (→ 400).

---

## Détail technique : l'assembleur

Le fichier `calculator.asm` contient 4 fonctions exportées qui opèrent sur des `double` (IEEE 754, 64 bits) via les instructions SSE2 scalaires :

```nasm
section .text
global add_numbers, sub_numbers, mul_numbers, div_numbers

add_numbers:        ; xmm0 = a, xmm1 = b
    addsd xmm0, xmm1   ; xmm0 = a + b
    ret

sub_numbers:
    subsd xmm0, xmm1   ; xmm0 = a - b
    ret

mul_numbers:
    mulsd xmm0, xmm1   ; xmm0 = a * b
    ret

div_numbers:
    divsd xmm0, xmm1   ; xmm0 = a / b
    ret
```

**Convention d'appel :** System V AMD64 ABI (Linux x86-64)
- Les arguments flottants sont passés dans `xmm0` (1er) et `xmm1` (2ème)
- La valeur de retour flottante est dans `xmm0`
- Aucun registre à sauvegarder (fonctions *leaf* sans pile)

La section `.note.GNU-stack` est déclarée `noexec` pour éviter un stack exécutable (bonne pratique sécurité).
