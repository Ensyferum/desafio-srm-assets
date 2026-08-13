# Vamos criar o projeto conforme especificações do arquivo @README_case_dev_srm.md.

## Vamos utilizar a Stack:
### Backend:
- Java 25 com Springboot 4
- Banco de Dados MySql
- Mensageria Confluent Kafka
- Flyway para DB Migration
- Testes unitários com JUNIT5 e Mockito
- Observabilidade com OpenTelemetry

### FrontEnd: 
- React simples com integração com o Backend Java

### Pipeline:
- Github Actions
- Git hooks pra validação de coverage de teste 80% e de Lintt de qualidade de código 


### Arquitetura:
- Banco de Dados unico relacional PostgreSQL
- Arquitetura orientada a serviços
- Aplicação de microserviços

### Decisões:
- Todos os componentes deverão estar no Docker-Compose, sendo possível subir o sistema inteiro através de 1 unico docker-compose
- Por mais que sejam vários microserviços e frontend, vamos cria-los todos dentro do mesmo repositório Github
- Toda as transações deverão ter um Id unico/CorrelationId  que deverá trafegar durante toda a solicitação