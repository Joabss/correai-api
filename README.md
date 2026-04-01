# correai-api
Surgiu a ideia de criar um app para corrida. Esse é um MVP inicial

Windows
docker compose up --build

✔ api                                  Built
✔ Network correai-api_default          Created
✔ Volume "correai-api_correai_pgdata"  Created
✔ Container correai-postgres           Created
✔ Container correai-api                Created

Linux
sudo docker compose up --build
sudo docker compose up

rodar em background
docker compose up -d
docker compose up postgres

para containers
docker compose down

remover volumes
docker compose down -v

reiniciar seviços
docker compose restart

ou apenas um srviço
docker compose restart api

Logs
docker compose logs
docker compose logs -f
docker compose logs -f api
docker compose logs -f postgres

docker compose exec api sh

docker compose exec postgres psql -U correai

limpeza geral

docker compose down -v
docker system prune -a