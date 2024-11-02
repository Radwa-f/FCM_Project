# Application des rappels avec FCM

## Description

### Objectif : Développer une application Android native pour permettre en place visualiser le fonctionnement de Firbase Cloud messaging.

### Fonctionnalités principales :

- Création des rappels:
  * L'utilisateur peut ajouter des rapples à la liste qui seront stocker dans un backend (firestore dans ce cas)
- Filtrage, suppression et verification:
  * L'utilisateur pourra rechercher dans la liste de rappels, supprimer un rappel ou le marquer comme verifié (checked)
- Reception des rappels:
  * Quand le temps du rappel arrive, l'utilisateur reçois une notification en temps réel

### Architecture :

- Activité principale : Affiche la liste des rappel.
- RecyclerView : Utilise une RecyclerView pour afficher la liste des rappels de manière efficace et flexible.
- Activité de FCM : Aide à envoyer les notifications quand le temps arrive
    
## Vidéo Démonstrative

La vidéo ci-contre montre le fonctionnement du projet :

<div align="center">

[Voir la vidéo](https://github.com/user-attachments/assets/b675f187-e0d9-497b-ab9c-ded893dff2d0)


</div>
