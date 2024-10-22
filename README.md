 Its an uitility project to generate excel report for death claim payouts from transaction history table.

REST API :- 

EXCEL Report -> GET -  http://localhost:8082/api/transactions/deathclaim/generate-report

Json file -> GET - http://localhost:8082/api/transactions/download-json

PP EXCEL Report on after specific date -> http://localhost:8082/api/transactions/periodicpayout/generate-report

PP EXCEL Report with date range -> http://localhost:8082/api/transactions/periodicpayout/dateRange/generate-report

Json file -> GET - http://localhost:8082/api/transactions/periodicpayout/download-json

overduepayout API - http://localhost:8082/api/transactions/overduepayment/generate-report

Note :
Added docker file but unable to run it as DEV1 DB connection VPN

docker build -t spring-boot-excel-report .


docker run -p 8082:8082 spring-boot-excel-report
