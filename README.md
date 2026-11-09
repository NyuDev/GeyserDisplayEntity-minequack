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
| Objet plat vanilla (épée, flèche…) | Caché, sauf s'il a une correspondance dans `Mappings/`. |
| `text_display` | Non touché : c'est Geyser qui le traduit. |

### Limites

- Escaliers, portes, barrières reliées : seule la plus grosse boîte est dessinée.
- Coffres, panneaux, têtes de mob (creeper, zombie…) : visibles seulement s'ils sont simples.
- Les rotations internes d'un modèle (fleurs en croix) ne sont pas appliquées. Les blocs teintés prennent la couleur des plaines.
- L'interpolation Java n'est pas reproduite : un display qui glisse avance par à-coups.
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

## Construire

Il faut le JDK 21 ou plus, le jar de Geyser que l'on fait tourner (ici `Geyser-Velocity`) et `velocity.jar`, pour la bibliothèque adventure.

```bash
tools/build-minequack.sh <Geyser-Velocity.jar> <velocity.jar> <GeyserDisplayEntityPack.mcpack>
```

Le script compile avec `javac --release 21` contre Geyser. Les imports `org.spongepowered.configurate` sont redirigés vers la copie que Geyser-Velocity embarque. Le script range ensuite le pack dans le jar.

Le pack (`GeyserDisplayEntityPack.mcpack`) contient des textures du jeu. Il n'est donc pas publié ici. On le construit avec `tools/block_renderer/generate.py <client.jar>`, puis `tools/build_pack.py`, à partir de son propre jar client Minecraft. La version utilisée par MineQuack est celle du commit `3dfb9d2` du fork imaginefun.

Testé avec Geyser-Velocity 2.12.1-b1298, Velocity 3.x et Paper 26.3, avec un client Bedrock 26.63.
