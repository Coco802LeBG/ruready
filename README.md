# Krypton Utility — Fabric 1.21.11

Mod **client uniquement** : ClickGUI (Droit Shift), HUD personnalisable, Freecam, Fullbright, ESP (boîtes de collision),
Block Scanner et outils d'inventaire.

## Compiler (sans Java en local)

1. Crée un dépôt GitHub et pousse tout le contenu de ce dossier.
2. Onglet **Actions** → workflow **Build** → télécharge l'artefact `krypton-utility` (le `.jar`).
3. Mets le jar (sans `-sources`) dans `.minecraft/mods/` avec **Fabric Loader ≥ 0.18** et **Fabric API 0.141.x+1.21.11**.

En local : `gradle build` (Gradle 9.2+, JDK 21).

## Utilisation

| Action | Comment |
|---|---|
| Ouvrir le ClickGUI | **Droit Shift** (modifiable dans Options > Commandes) |
| Activer/désactiver un module | clic sur l'interrupteur, ou clic droit sur la carte |
| Dérouler les options | clic gauche sur la carte |
| Éditer le HUD | catégorie **Interface** > **Edit HUD**, puis glisser les widgets |
| Freecam | module Freecam, ou touche « Toggle Freecam » (non assignée par défaut) |

Config : `config/krypton-utility.json`.

## Modules

- **Freecam** : un `RemotePlayer` client-only devient l'entité caméra ; le joueur reste immobile (clavier/souris redirigés par mixin).
- **Fullbright** : gamma forcé à la lecture de l'option (la valeur sauvegardée n'est jamais modifiée).
- **ESP Boxes** : entités + conteneurs. Styles *Outline / Corners / Filled* (projetés à l'écran, **visibles à travers les murs**) et *World 3D* (boîtes dans le monde, avec test de profondeur).
- **Block Scanner** : scan incrémental des chunks chargés (2 chunks/tick max), cache par chunk, filtres par type de minerai.
- **Inventory Tools** : boutons à droite des coffres/barils/shulkers/entonnoirs/distributeurs : Sort, Take all, Store all, Store similar.
- **HUD** : coordonnées, conversion Nether/Overworld, direction, biome, FPS, vitesse, liste des modules.

## Si la compilation échoue

Le projet utilise les **mappings officiels Mojang** (ceux avec lesquels Fabric API compile elle-même la 1.21.11).
Les points les plus sensibles aux noms exacts sont : `mixin/*` (cibles `Entity.turn`, `KeyMapping.isDown/consumeClick`,
`OptionInstance.get`, champs `leftPos/topPos/imageWidth/imageHeight`) et `MultiPlayerGameMode.handleInventoryMouseClick`.
Envoie-moi le log d'erreur de l'onglet Actions et je corrige.
