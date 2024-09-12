 Its an uitility project to generate excel report for death claim payouts from transaction history table.

REST API :- 

EXCEL Report -> GET -  http://localhost:8082/api/transactions/generate-report

Json file -> GET - http://localhost:8082/api/transactions/download-json

PP EXCEL Report -> http://localhost:8082/api/transactions/periodicpayout/generate-report

Json file -> GET - http://localhost:8082/api/transactions/periodicpayout/download-json

Note :
Added docker file but unable to run it as DEV1 DB connection VPN

docker build -t spring-boot-excel-report .


docker run -p 8082:8082 spring-boot-excel-report
