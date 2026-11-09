# GeyserDisplayEntity (version MineQuack)

Extension [Geyser](https://geysermc.org) qui rend visibles aux joueurs **Bedrock** les `block_display` et `item_display` de Java.
Geyser les ignore : Bedrock n'a pas ces entités. Cette version est celle du réseau MineQuack.

## Ce qu'elle dessine

| Java | Sur Bedrock |
|---|---|
| Bloc simple (1 bloc, sans rotation) | Le bloc exact, par un `falling_block` figé. |
| Bloc étiré, réduit ou tourné | Le « cube » du pack : la plus grosse boîte du modèle, avec les vraies textures, à n'importe quelle échelle et rotation. |
| Tête de joueur (`player_head` avec profil) | La tête avec son skin, dessinée comme Geyser dessine une tête posée. Elle reste toujours droite, avec une taille moyenne. |
| Objet d'un bloc vanilla (laine, dalle…) | Comme un bloc. |
| Objet plat vanilla (épée, flèche, poisson…) | Une plaque d'un pixel d'épaisseur avec la texture de l'objet, placée comme Java (`item/generated`, `item/handheld`). Une seule face, vue des deux côtés : de dos, l'objet est retourné, comme en Java. |
| Coffre (dessiné en Java par un rendu à part, sans modèle de bloc) | Une boîte de 14 × 14 × 14 pixels, avec la texture la plus proche (planches, obsidienne, cuivre). |
| `text_display` | Traduit par Geyser (un nametag). Cette version tourne son décalage (`translation`) avec le lacet du display, comme Java. |

### Glissement (manèges, canards, ballons)

Java ne déplace pas un display d'un coup : le client glisse vers la nouvelle position pendant `teleport_duration` ticks, et vers la nouvelle transformation pendant `interpolation_duration` ticks. Un manège qui n'envoie sa position que tous les 10 ou 20 ticks reste donc fluide sur Java. Bedrock ne connaît pas ces durées : sans rien faire, la pièce saute d'un coup, et une pièce tournée en avance pour compenser le glissement reste en biais.

Cette version reproduit le glissement :
- **Position** : pour un display qui n'est passager de rien, le proxy envoie la position intermédiaire à chaque tick, pendant `teleport_duration`. Ses passagers suivent sur le client.
- **Rotation et transformation du cube** : la propriété `geyser:i_dur` donne au pack la durée du glissement. Le pack passe d'une valeur à l'autre de façon linéaire, avec `math.lerprotate` pour les angles. Cela ne coûte aucun paquet en plus.
- **Têtes de joueur** : elles glissent aussi (position, lacet, taille), à chaque tick.

### Passagers

Java assoit un joueur 0,6 bloc sous le display qu'il monte (un display n'a pas de taille). Geyser, qui ne connaît pas ce véhicule, l'assoit aux 3/4 de la hauteur de l'entité Bedrock : environ un bloc trop haut. Cette version replace le joueur à la bonne hauteur (`setPassengers`). Mesure faite sur un client Bedrock : les yeux à 1,01 bloc au-dessus du display, contre 1,02 en Java. `general.rider-seat-offset` (en blocs, 0 par défaut) corrige encore si une version de Bedrock assoit autrement.

Un display qui monte un display qui bouge (cabines d'une grande roue) est dessiné en cube et non en `falling_block` : Bedrock fait dériver un `falling_block` passager d'un véhicule en mouvement.

### Limites

- Escaliers, portes, barrières reliées : seule la plus grosse boîte est dessinée.
- Panneaux, têtes de mob (creeper, zombie…) : visibles seulement s'ils sont simples.
- Texte : un nametag Bedrock a une taille fixe, l'échelle Java est perdue. Un texte réduit en Java paraît 2 à 4 fois plus grand sur Bedrock et peut en chevaucher un autre.
- Les rotations internes d'un modèle (fleurs en croix) ne sont pas appliquées. Les blocs teintés prennent la couleur des plaines.
- Les cadres d'objets « meubles » du fork d'origine sont coupés. Ils passaient par un registre que Geyser 2.12 b1278 a retiré.

## Origine et modifications (AGPL-3.0, article 5)

Ce code est une œuvre dérivée, sous la même licence ([LICENSE](LICENSE), GNU AGPL v3).

- **GeyserDisplayEntity**, [GeyserExtensionists](https://github.com/GeyserExtensionists/GeyserDisplayEntity) (zimzaza4, xSquishyLiam et contributeurs), d'après la branche *display-entities* de Kas-tle. Version reprise : commit `3d22bff` du 01/11/2026, qui apporte les têtes de joueur (#45) et la compatibilité avec Geyser 2.12 (#41, #43, #44).
- **Fork imaginefun** ([imaginefun/GeyserDisplayEntity](https://github.com/imaginefun/GeyserDisplayEntity), d'après Sunny-SMP et le Conditoris Development Team) : dessin des blocs (falling block et cube du pack), données `block_render.tsv` et sources du pack (`pack/`). Version reprise : commit `3dfb9d2` du 30/10/2026.

**Modifié par MineQuack le 09/11/2026 :**
- fusion des deux branches ;
- têtes de joueur greffées dans le système de « genres » du fork : `placeHead` lit la transformation Java brute ;
- Geyser 2.12 b1278 et plus récent : `build(false)` pour un genre non dessiné, et cadres d'objets coupés ;
- construction sans Gradle, avec `tools/build-minequack.sh`.

**Modifié par MineQuack le 09/11/2026, version minequack.2 :** glissement des positions, des rotations et des transformations (voir plus haut) :
- `ItemDisplayEntity` et `SlotDisplayEntity` : lecture de `teleport_duration` et de `interpolation_duration` ;
- pack (`pack/animations/geyser_block.animation.json`, `pack/entity/*.json`) : glissement du cube, appliqué au pack construit par `tools/patch_pack.py`, qui change aussi sa version.

**Modifié par MineQuack le 09/11/2026, version minequack.3 :**
- `ItemDisplayEntity` : les angles d'Euler envoyés au pack restent au plus près des précédents (`closestAngles`), ce qui supprime les tours complets d'une pièce qui tourne ; un display passager d'un display qui bouge est dessiné en cube ; liens véhicule et passagers renvoyés quand une pièce change de genre ;
- `BlockShapes`, `BlockDisplayEntity` : boîte de remplacement pour les coffres ;
- `TurnedTextDisplayEntity` : le décalage d'un `text_display` tourne avec son lacet ;
- pack : texte « Accroupis-toi pour descendre » (`pack/texts/`, ajouté au pack construit par `tools/patch_pack.py`).

**Modifié par MineQuack le 09/11/2026, version minequack.5 :**
- `ItemDisplayEntity` : hauteur des joueurs assis (voir « Passagers ») ;
- objets plats : formes et transformations dans `BlockShapes` et `ItemDisplayEntity`, 760 objets dans `block_render.tsv`, ajoutés au pack par `tools/flat_items_pack.py`.

## Construire

Il faut le JDK 21 ou plus, le jar de Geyser que l'on fait tourner (ici `Geyser-Velocity`) et `velocity.jar`, pour la bibliothèque adventure.

```bash
tools/build-minequack.sh <Geyser-Velocity.jar> <velocity.jar> <GeyserDisplayEntityPack.mcpack>
```

Le script compile avec `javac --release 21` contre Geyser. Les imports `org.spongepowered.configurate` sont redirigés vers la copie que Geyser-Velocity embarque. Le script range ensuite le pack dans le jar.

Le pack (`GeyserDisplayEntityPack.mcpack`) contient des textures du jeu. Il n'est donc pas publié ici. On le construit avec `tools/block_renderer/generate.py <client.jar>`, puis `tools/build_pack.py`, à partir de son propre jar client Minecraft. La version utilisée par MineQuack part de celle du commit `3dfb9d2` du fork imaginefun, passe par `tools/patch_pack.py` (glissement et textes), puis par `tools/flat_items_pack.py <client.jar> …` (objets plats, textures tirées du jar client). Ce dernier écrit aussi le `block_render.tsv` qui va avec.

Testé avec Geyser-Velocity 2.12.1-b1298, Velocity 3.x et Paper 26.3, avec un client Bedrock 26.63.
