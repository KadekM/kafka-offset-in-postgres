
```
docker run  --rm \
  --name=kafka \
  -p 2181:2181 \
  -p 9092:9092 \
  --env ADVERTISED_HOST=localhost \
  --env ADVERTISED_PORT=9092  \
  johnnypark/kafka-zookeeper:2.6.0

docker run --rm -p 5432:5432 --name my-pg -e POSTGRES_PASSWORD=mysecretpassword -d postgres
```
