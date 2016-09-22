# spark-threads

Pré-requis : Docker + Docker-compose + JMeter + jmeter-plugins (https://jmeter-plugins.org/)

Lancement du serveur : = 1 serveur spark + 1 Postgres

```
docker-compose up
```

La gestion de conf applicative et serveur se fait par des variables d'environement sur le docker-compose.yml.

Dans le § environment du service httpServer, on peut définir les var d'env suivantes :
- HTTP_POOL_CAPACITY : pour le pool de traitement des requêtes HTTP
- SQL_POOL_CAPACITY : pour le pool JDBC (basé sur c3p0) vers PostgreSQL, 20 par défaut
- WORKER_POOL_CAPACITY : utilisé si APP_STRATEGY=*GLOBAL_POOL*, *GLOBAL_POOL_WITH_FALLBACK_ON_POOL_FULL* ou *GLOBAL_POOL_WITH_FALLBACK_ON_WAITING_QUEUE_SIZE* (cf plus bas)
- WORKER_POOL_MAX_WAIT : utilisé si APP_STRATEGY=*GLOBAL_POOL_WITH_FALLBACK_ON_WAITING_QUEUE_SIZE* (cf plus bas)
- APP_STRATEGY : pour choisir la stratégie de traitement d'une requête :
  - *SEQUENTIAL* : traitement en séquentiel
  - *FORKJOIN* : pour chaque requête, le traitement déporté sur un autre thread via Fork-join
  - *THREAD_PER_PROCESS* : pour chaque requête, le traitement est déporté sur un autre thread dédié
  - *POOL_PER_PROCESS* : pour chaque requête, le traitement déporté sur un pool de 3 threads
  - *GLOBAL_POOL* : tous les traitements sont déportés dans un pool de thread global (de taille égale à la valeur *WORKER_POOL_CAPACITY*, 10 par défaut)
  - *GLOBAL_POOL_WITH_FALLBACK_ON_POOL_FULL* : tous les traitements sont déportés dans un pool de thread global (de taille égale à la valeur *WORKER_POOL_CAPACITY*, 8 par défaut) mais si le pool est plein alors on passe en traitement séquentiel
  - *GLOBAL_POOL_WITH_FALLBACK_ON_WAITING_QUEUE_SIZE* : tous les traitements sont déportés dans un pool de thread global (de taille égale à la valeur *WORKER_POOL_CAPACITY*, 8 par défaut) jusqu'à ce que la file d'attente du pool contiennt plus de *WORKER_POOL_MAX_WAIT* threads, 8 par défaut)