"""
Peuple MusicStore avec des albums et des titres d'exemple, fichiers compris.

Les morceaux sont composés et synthétisés localement : aucun fichier n'est téléchargé et
aucune œuvre protégée n'est utilisée. Tout passe par l'API publique, exactement comme le
ferait un artiste : inscription, création d'album, création de titre, dépôt du fichier.

Le script est rejouable : un titre déjà publié par le même artiste n'est pas recréé.

Dépendances : numpy, et imageio-ffmpeg pour l'encodage MP3 et MP4. Sans imageio-ffmpeg,
les titres audio sont déposés en WAV et le clip vidéo est ignoré.

    python scripts/seed-demo-media.py
    python scripts/seed-demo-media.py --api http://localhost:8080
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
import wave
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np

SR = 44_100
MOT_DE_PASSE = "Password1!"


# --------------------------------------------------------------------------- synthèse

def midi_hz(note: float) -> float:
    return 440.0 * 2 ** ((note - 69) / 12)


def temps(duree: float) -> np.ndarray:
    return np.arange(int(duree * SR)) / SR


def lisser(signal: np.ndarray, largeur: int) -> np.ndarray:
    """Passe-bas grossier par moyenne glissante : adoucit les harmoniques aiguës."""
    if largeur <= 1:
        return signal
    noyau = np.ones(largeur) / largeur
    return np.convolve(signal, noyau, mode="same")


def enveloppe(n: int, attaque: float, relache: float) -> np.ndarray:
    env = np.ones(n)
    na, nr = min(int(attaque * SR), n), min(int(relache * SR), n)
    if na:
        env[:na] = np.linspace(0, 1, na)
    if nr:
        env[-nr:] *= np.linspace(1, 0, nr)
    return env


def nappe(notes: list[int], duree: float, brillance: int = 6) -> np.ndarray:
    """Accord tenu : dents de scie légèrement désaccordées puis filtrées."""
    t = temps(duree)
    sortie = np.zeros_like(t)
    for note in notes:
        for desaccord in (-0.004, 0.004):
            f = midi_hz(note) * (1 + desaccord)
            for h in range(1, brillance + 1):
                sortie += np.sin(2 * np.pi * f * h * t) / h
    sortie = lisser(sortie, 24)
    return sortie * enveloppe(len(t), 0.25, 0.35) / (len(notes) * 4)


def pince(note: int, duree: float, decroissance: float = 6.0) -> np.ndarray:
    t = temps(duree)
    f = midi_hz(note)
    son = np.sin(2 * np.pi * f * t) + 0.45 * np.sin(4 * np.pi * f * t) + 0.2 * np.sin(6 * np.pi * f * t)
    return son * np.exp(-t * decroissance) * enveloppe(len(t), 0.004, 0.02) * 0.5


def basse(note: int, duree: float) -> np.ndarray:
    t = temps(duree)
    f = midi_hz(note)
    son = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(4 * np.pi * f * t)
    return son * enveloppe(len(t), 0.01, 0.08) * 0.55


def grosse_caisse() -> np.ndarray:
    t = temps(0.35)
    frequence = 48 + 110 * np.exp(-t * 32)
    phase = 2 * np.pi * np.cumsum(frequence) / SR
    return np.sin(phase) * np.exp(-t * 9) * 0.9


def caisse_claire(rng: np.random.Generator) -> np.ndarray:
    t = temps(0.22)
    bruit = rng.uniform(-1, 1, len(t))
    return (bruit * 0.45 + np.sin(2 * np.pi * 185 * t) * 0.35) * np.exp(-t * 22)


def charleston(rng: np.random.Generator) -> np.ndarray:
    t = temps(0.06)
    bruit = np.diff(rng.uniform(-1, 1, len(t) + 1))
    return bruit * np.exp(-t * 70) * 0.18


def poser(piste: np.ndarray, son: np.ndarray, debut: float, volume: float = 1.0) -> None:
    i = int(debut * SR)
    if i >= len(piste):
        return
    fin = min(len(piste), i + len(son))
    piste[i:fin] += son[: fin - i] * volume


@dataclass
class Morceau:
    """Description musicale d'un titre : grille d'accords, tempo et instrumentation."""

    bpm: int
    accords: list[list[int]]
    temps_par_mesure: int = 4
    mesures: int = 16
    batterie: bool = True
    arpege: str = "croches"  # croches | doubles | rare | valse | aucun
    nappe: bool = True
    graine: int = 1


def composer(m: Morceau) -> np.ndarray:
    rng = np.random.default_rng(m.graine)
    battement = 60 / m.bpm
    duree_mesure = battement * m.temps_par_mesure
    piste = np.zeros(int(duree_mesure * m.mesures * SR) + SR)

    for mesure in range(m.mesures):
        accord = m.accords[mesure % len(m.accords)]
        debut = mesure * duree_mesure
        intro = mesure < 2
        outro = mesure >= m.mesures - 1

        if m.nappe:
            poser(piste, nappe(accord, duree_mesure + 0.3), debut, 0.8)

        if m.arpege == "valse":
            poser(piste, basse(accord[0] - 12, battement * 0.9), debut)
            for b in (1, 2):
                for note in accord:
                    poser(piste, pince(note + 12, battement, 5), debut + b * battement, 0.45)
        elif not intro:
            poser(piste, basse(accord[0] - 12, duree_mesure * 0.5), debut)
            poser(piste, basse(accord[0] - 12, duree_mesure * 0.5), debut + duree_mesure / 2, 0.8)

        motif = accord + [accord[1] + 12, accord[2] + 12]
        if m.arpege in ("croches", "doubles"):
            pas = 2 if m.arpege == "croches" else 4
            for i in range(m.temps_par_mesure * pas):
                note = motif[(i + mesure) % len(motif)] + 12
                poser(piste, pince(note, battement, 7), debut + i * battement / pas, 0.35)
        elif m.arpege == "rare":
            for b in range(m.temps_par_mesure):
                if rng.random() < 0.55:
                    note = int(rng.choice(motif)) + 12
                    poser(piste, pince(note, battement * 3, 2.2), debut + b * battement, 0.4)

        if m.batterie and not intro and not outro:
            for b in range(m.temps_par_mesure):
                instant = debut + b * battement
                if b % 2 == 0:
                    poser(piste, grosse_caisse(), instant)
                else:
                    poser(piste, caisse_claire(rng), instant, 0.8)
                poser(piste, charleston(rng), instant)
                poser(piste, charleston(rng), instant + battement / 2, 0.7)

    piste *= enveloppe(len(piste), 1.0, 2.5)
    return piste / (np.max(np.abs(piste)) + 1e-9) * 0.89


def ecrire_wav(chemin: Path, signal: np.ndarray) -> None:
    donnees = (np.clip(signal, -1, 1) * 32767).astype(np.int16)
    with wave.open(str(chemin), "wb") as fichier:
        fichier.setnchannels(1)
        fichier.setsampwidth(2)
        fichier.setframerate(SR)
        fichier.writeframes(donnees.tobytes())


# --------------------------------------------------------------------------- encodage

def trouver_ffmpeg() -> str | None:
    try:
        import imageio_ffmpeg

        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError:
        return None


def encoder_mp3(ffmpeg: str, wav: Path, mp3: Path, titre: str, artiste: str, album: str | None) -> None:
    commande = [ffmpeg, "-y", "-loglevel", "error", "-i", str(wav), "-ac", "2",
                "-codec:a", "libmp3lame", "-b:a", "160k",
                "-metadata", f"title={titre}", "-metadata", f"artist={artiste}"]
    if album:
        commande += ["-metadata", f"album={album}"]
    subprocess.run(commande + [str(mp3)], check=True)


def encoder_clip(ffmpeg: str, audio: np.ndarray, wav: Path, mp4: Path, titre: str, artiste: str) -> None:
    """Clip de visualisation : dégradé animé et barres de spectre calées sur la musique."""
    import cv2

    largeur, hauteur, ips = 640, 360, 24
    commande = [ffmpeg, "-y", "-loglevel", "error",
                "-f", "rawvideo", "-pix_fmt", "bgr24", "-s", f"{largeur}x{hauteur}", "-r", str(ips), "-i", "-",
                "-i", str(wav),
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "27", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-b:a", "128k", "-shortest", "-movflags", "+faststart", str(mp4)]
    processus = subprocess.Popen(commande, stdin=subprocess.PIPE)

    nb_images = int(len(audio) / SR * ips)
    fenetre = 2048
    barres = 32
    y, x = np.mgrid[0:hauteur, 0:largeur]
    texte_titre = ascii_majuscules(titre)
    texte_artiste = ascii_majuscules(artiste)

    for image_index in range(nb_images):
        instant = image_index / ips
        teinte = (instant * 12) % 180
        hsv = np.zeros((hauteur, largeur, 3), np.uint8)
        hsv[..., 0] = ((teinte + x / largeur * 40) % 180).astype(np.uint8)
        hsv[..., 1] = 170
        hsv[..., 2] = (60 + y / hauteur * 80).astype(np.uint8)
        image = cv2.cvtColor(hsv, cv2.COLOR_HSV2BGR)

        centre = int(instant * SR)
        extrait = audio[max(0, centre - fenetre // 2): centre + fenetre // 2]
        if len(extrait) == fenetre:
            spectre = np.abs(np.fft.rfft(extrait * np.hanning(fenetre)))[:fenetre // 4]
            bandes = np.array_split(spectre, barres)
            niveaux = np.log1p([b.mean() for b in bandes])
            niveaux = niveaux / (niveaux.max() + 1e-9)
        else:
            niveaux = np.zeros(barres)

        pas = largeur // barres
        for i, niveau in enumerate(niveaux):
            haut = int(niveau * hauteur * 0.45)
            cv2.rectangle(image, (i * pas + 3, hauteur - 40 - haut), ((i + 1) * pas - 3, hauteur - 40),
                          (255, 255, 255), -1)

        cv2.putText(image, texte_titre, (28, 70), cv2.FONT_HERSHEY_DUPLEX, 1.2, (255, 255, 255), 2, cv2.LINE_AA)
        cv2.putText(image, texte_artiste, (30, 110), cv2.FONT_HERSHEY_SIMPLEX, 0.8, (235, 235, 235), 1, cv2.LINE_AA)
        processus.stdin.write(image.tobytes())

    processus.stdin.close()
    if processus.wait() != 0:
        raise RuntimeError(f"ffmpeg a échoué pour {mp4.name}")


def ascii_majuscules(texte: str) -> str:
    """OpenCV ne dessine que l'ASCII : les accents sont retirés pour le texte incrusté."""
    import unicodedata

    return unicodedata.normalize("NFKD", texte).encode("ascii", "ignore").decode().upper()


# --------------------------------------------------------------------------- API

class Api:
    def __init__(self, base: str):
        self.base = base.rstrip("/")

    def appel(self, methode: str, chemin: str, jeton: str | None = None, corps: dict | None = None,
              fichier: tuple[str, bytes, str] | None = None) -> tuple[int, object]:
        entetes = {}
        donnees = None
        if jeton:
            entetes["Authorization"] = f"Bearer {jeton}"
        if corps is not None:
            donnees = json.dumps(corps).encode()
            entetes["Content-Type"] = "application/json"
        if fichier is not None:
            nom, contenu, type_mime = fichier
            frontiere = uuid.uuid4().hex
            donnees = (
                f"--{frontiere}\r\n"
                f'Content-Disposition: form-data; name="file"; filename="{nom}"\r\n'
                f"Content-Type: {type_mime}\r\n\r\n"
            ).encode() + contenu + f"\r\n--{frontiere}--\r\n".encode()
            entetes["Content-Type"] = f"multipart/form-data; boundary={frontiere}"

        requete = urllib.request.Request(self.base + chemin, data=donnees, headers=entetes, method=methode)
        try:
            with urllib.request.urlopen(requete, timeout=120) as reponse:
                brut = reponse.read()
                return reponse.status, json.loads(brut) if brut else None
        except urllib.error.HTTPError as erreur:
            brut = erreur.read()
            try:
                return erreur.code, json.loads(brut)
            except ValueError:
                return erreur.code, brut.decode(errors="replace")

    def exiger(self, attendu: tuple[int, ...], *args, **kwargs) -> object:
        statut, donnees = self.appel(*args, **kwargs)
        if statut not in attendu:
            raise RuntimeError(f"{args[0]} {args[1]} -> {statut} : {donnees}")
        return donnees

    def compte(self, nom: str, pseudo: str, email: str, role: str) -> tuple[str, dict]:
        """Connexion, ou inscription si le compte n'existe pas encore."""
        statut, donnees = self.appel("POST", "/api/auth/login", corps={"userEmail": email, "password": MOT_DE_PASSE})
        if statut != 200:
            donnees = self.exiger((201,), "POST", "/api/auth/register", corps={
                "fullName": nom, "age": 30, "sex": "Non défini", "userName": pseudo,
                "userEmail": email, "password": MOT_DE_PASSE, "role": role})
        return donnees["token"], donnees["user"]


# --------------------------------------------------------------------------- catalogue

A, B, C, D, E, F, G = 57, 59, 60, 62, 64, 65, 67  # octave 3-4


@dataclass
class TitreDemo:
    nom: str
    description: str
    genre: str
    morceau: Morceau
    video: bool = False
    ecoutes: int = 0


@dataclass
class AlbumDemo:
    nom: str | None  # None : singles hors album
    annee: int
    titres: list[TitreDemo] = field(default_factory=list)


@dataclass
class ArtisteDemo:
    nom: str
    pseudo: str
    email: str
    albums: list[AlbumDemo]


CATALOGUE = [
    ArtisteDemo("Nova Lumen", "novalumen", "nova@musicstore.local", [
        AlbumDemo("Horizons Synthétiques", 2025, [
            TitreDemo("Aube électrique", "Arpèges lumineux sur une pulsation régulière", "Synthwave",
                      Morceau(bpm=100, accords=[[A, C, E], [F, A, C + 12], [C, E, G], [G - 12, B - 12, D]], graine=11),
                      ecoutes=9),
            TitreDemo("Néons de minuit", "Balade nocturne en mineur", "Synthwave",
                      Morceau(bpm=86, accords=[[D, F, A], [A + 1 - 12, D, F], [F, A, C + 12], [C, E, G]],
                              arpege="croches", graine=23), ecoutes=6),
            TitreDemo("Pulsation", "Le morceau le plus rapide de l'album", "Electro",
                      Morceau(bpm=120, accords=[[E, G, B], [C, E, G], [G, B, D + 12], [D, F + 1, A]],
                              arpege="doubles", graine=37), ecoutes=12),
            TitreDemo("Aube électrique (clip)", "Clip de visualisation officiel", "Synthwave",
                      Morceau(bpm=100, accords=[[A, C, E], [F, A, C + 12], [C, E, G], [G - 12, B - 12, D]],
                              mesures=8, graine=11), video=True, ecoutes=4),
        ]),
    ]),
    ArtisteDemo("Les Ondes Douces", "ondesdouces", "ondes@musicstore.local", [
        AlbumDemo("Carnets de voyage", 2024, [
            TitreDemo("Marche lente", "Lo-fi pour les fins de journée", "Lo-fi",
                      Morceau(bpm=74, accords=[[C, E, G, B], [A - 12, C, E, G], [F - 12, A - 12, C, E], [G - 12, B - 12, D, F]],
                              arpege="rare", graine=5), ecoutes=7),
            TitreDemo("Rivière calme", "Nappes ambiantes sans percussions", "Ambient",
                      Morceau(bpm=60, accords=[[D, F + 1, A], [B - 12, D, F + 1], [G - 12, B - 12, D], [A - 12, C + 1, E]],
                              batterie=False, arpege="rare", mesures=12, graine=8), ecoutes=3),
            TitreDemo("Valse des lanternes", "Petite valse à trois temps", "Acoustique",
                      Morceau(bpm=132, temps_par_mesure=3, accords=[[G - 12, B - 12, D], [E - 12, G - 12, B - 12], [C - 12, E - 12, G - 12], [D - 12, F + 1 - 12, A - 12]],
                              batterie=False, arpege="valse", nappe=False, mesures=24, graine=17), ecoutes=5),
        ]),
        AlbumDemo(None, 2026, [
            TitreDemo("Berceuse numérique", "Single paru hors album", "Ambient",
                      Morceau(bpm=66, accords=[[F, A, C + 12], [D, F, A], [A + 1 - 12, D, F], [C, E, G]],
                              batterie=False, arpege="rare", mesures=10, graine=29), ecoutes=2),
        ]),
    ]),
]

# Notes attribuées par les auditeurs de démonstration, pour que moyennes et classements varient.
NOTES = {
    "jean@example.com": {"Aube électrique": 5, "Pulsation": 4, "Marche lente": 4, "Rivière calme": 3, "Valse des lanternes": 5},
    "alice@example.com": {"Aube électrique": 4, "Néons de minuit": 5, "Pulsation": 5, "Marche lente": 3, "Berceuse numérique": 4},
}


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    parseur = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parseur.add_argument("--api", default="http://localhost:8080", help="URL de base de l'API")
    parseur.add_argument("--sortie", default=str(Path(__file__).parent / ".demo-media"),
                         help="répertoire des fichiers générés")
    options = parseur.parse_args()

    api = Api(options.api)
    sortie = Path(options.sortie)
    sortie.mkdir(parents=True, exist_ok=True)
    ffmpeg = trouver_ffmpeg()
    if not ffmpeg:
        print("imageio-ffmpeg absent : dépôt en WAV et clip vidéo ignoré.")

    statut, _ = api.appel("GET", "/actuator/health")
    if statut != 200:
        print(f"API injoignable sur {options.api} (statut {statut}).")
        return 1

    publies: dict[str, str] = {}
    for artiste in CATALOGUE:
        jeton, profil = api.compte(artiste.nom, artiste.pseudo, artiste.email, "artiste")
        existants = {t["name"]: t for t in api.exiger((200,), "GET", f"/api/titres/artiste/{profil['id']}", jeton=jeton)}
        print(f"\n== {artiste.nom} ({artiste.email})")

        for album in artiste.albums:
            album_id = None
            if album.nom:
                deja = next((t["albumId"] for t in existants.values() if t.get("albumNom") == album.nom), None)
                album_id = deja or api.exiger((201,), "POST", "/api/albums", jeton=jeton,
                                              corps={"name": album.nom, "annee": album.annee})["id"]
                print(f"  album « {album.nom} »")

            for titre in album.titres:
                courant = existants.get(titre.nom)
                if courant and courant.get("mediaUrl"):
                    print(f"    = {titre.nom} (déjà publié)")
                    publies[titre.nom] = courant["id"]
                    continue

                signal = composer(titre.morceau)
                base = sortie / f"{artiste.pseudo}-{uuid.uuid5(uuid.NAMESPACE_URL, titre.nom).hex[:8]}"
                wav = base.with_suffix(".wav")
                ecrire_wav(wav, signal)

                if titre.video:
                    if not ffmpeg:
                        print(f"    ! {titre.nom} ignoré (ffmpeg requis pour la vidéo)")
                        continue
                    fichier, type_mime = base.with_suffix(".mp4"), "video/mp4"
                    encoder_clip(ffmpeg, signal, wav, fichier, titre.nom, artiste.nom)
                elif ffmpeg:
                    fichier, type_mime = base.with_suffix(".mp3"), "audio/mpeg"
                    encoder_mp3(ffmpeg, wav, fichier, titre.nom, artiste.nom, album.nom)
                else:
                    fichier, type_mime = wav, "audio/wav"

                if not courant:
                    courant = api.exiger((201,), "POST", "/api/titres", jeton=jeton, corps={
                        "name": titre.nom, "description": titre.description, "genre": titre.genre,
                        "annee": album.annee, "mediaType": "video" if titre.video else "audio", "albumId": album_id})

                nom_fichier = ascii_majuscules(titre.nom).lower().replace(" ", "_").replace("(", "").replace(")", "")
                api.exiger((200,), "POST", f"/api/titres/{courant['id']}/media", jeton=jeton,
                           fichier=(nom_fichier + fichier.suffix, fichier.read_bytes(), type_mime))
                publies[titre.nom] = courant["id"]
                taille = fichier.stat().st_size / 1024
                duree = len(signal) / SR
                print(f"    + {titre.nom} : {fichier.suffix[1:]}, {duree:.0f} s, {taille:.0f} Ko")

                # Quelques écoutes simulées, seulement à la première publication.
                jeton_auditeur, _ = api.compte("Jean Dupont", "jdupont", "jean@example.com", "auditeur")
                for _ in range(titre.ecoutes):
                    api.appel("POST", f"/api/titres/{courant['id']}/lectures", jeton=jeton_auditeur)

    for email, notes in NOTES.items():
        jeton, _ = api.compte(email.split("@")[0].title(), email.split("@")[0], email, "auditeur")
        for nom, valeur in notes.items():
            if nom in publies:
                api.appel("PUT", f"/api/titres/{publies[nom]}/note", jeton=jeton, corps={"valeur": valeur})

    page = api.exiger((200,), "GET", "/api/titres?size=50")
    albums = api.exiger((200,), "GET", "/api/albums")
    print(f"\nCatalogue public : {page['totalElements']} titres, {len(albums)} albums.")
    print(f"Comptes artistes créés ou réutilisés, mot de passe commun : {MOT_DE_PASSE}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
