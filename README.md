<div align="center">

<img src="assets/logo.png" alt="SkyHand" width="140">

# SHViaTickFix

*Évite aux joueurs en 1.21.2 et 1.21.3 d'être expulsés pour « Invalid move player packet received » sur un serveur 26.x.*

<img src="https://img.shields.io/badge/Paper-26.3-f2a900?style=for-the-badge" alt="Paper 26.3"> <img src="https://img.shields.io/badge/Java-25-e76f00?style=for-the-badge" alt="Java 25"> <img src="https://img.shields.io/badge/SkyHand-Opale-3aa655?style=for-the-badge" alt="SkyHand Opale">

</div>

---

## Présentation

Depuis la 26.x, le serveur expulse un joueur dont le jeu envoie deux positions entre deux fins de tick
(`multiplayer.disconnect.invalid_player_movement`), et n'écrit rien dans la console, contrairement à un vrai mouvement
invalide (`Disconnected on move player packet. Invalid data: ...`).

Les clients 1.21.2 et 1.21.3, traduits par ViaBackwards, envoient leurs propres fins de tick mais mettent parfois deux
positions dans le même tick, surtout pendant le chargement du monde à l'arrivée : le joueur est expulsé une seconde
après chaque connexion. ViaBackwards ajoute déjà la fin de tick qui manque pour les versions plus anciennes, pas pour
celles-ci.

- Le plugin se branche sur le protocole 1.21.4 → 1.21.2 de ViaBackwards, le premier que traversent les paquets de ces clients.
- Quand une deuxième position arrive avant la fin de tick, il envoie la fin de tick au serveur juste avant elle.
- La position qui répond à une téléportation n'est pas comptée : ViaBackwards la transforme en acceptation de téléportation pour la 26.3.
- Les autres versions ne passent pas par le correctif, et un vrai mouvement invalide (NaN) est toujours expulsé.

Utilisé sur : **Opale**, **lobby**, **login**.

## Commandes

| Commande | Description | Permission |
|---|---|---|
| `/shviatickfix status` | Nombre de fins de tick ajoutées depuis le démarrage, et de connexions concernées. | `shviatickfix.admin.status` |
| `/shviatickfix reload` | Recharge `config.yml` et `messages.yml`. Une erreur de syntaxe annule le rechargement. | `shviatickfix.admin.reload` |

## Permissions

| Permission | Description | Par défaut |
|---|---|---|
| `shviatickfix.admin` | Toute l'administration (parent des deux suivantes). | `op` |
| `shviatickfix.admin.status` | Utiliser /shviatickfix status. | `op` |
| `shviatickfix.admin.reload` | Utiliser /shviatickfix reload. | `op` |

## Configuration

Cliquez sur un fichier pour l'ouvrir :

<pre>
plugins/SHViaTickFix/
├── <a href="src/main/resources/config.yml">config.yml</a>
└── <a href="src/main/resources/messages.yml">messages.yml</a>
</pre>

Dépend de **ViaVersion** et **ViaBackwards**. Via ne permet pas de retirer un traitement de paquet : un déchargement à
chaud laisse les traitements en place mais inactifs.

## Compilation

Java 25 et Maven :

```bash
mvn clean package
```

Le jar est généré dans `target/`.

---

<div align="center">

<sub>Développé par <b>Cry_Legende</b> pour le serveur <b>SkyHand</b> · Tous droits réservés</sub>

</div>
