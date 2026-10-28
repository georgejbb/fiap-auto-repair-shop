# ADR-0003: Banco de dados PostgreSQL

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

O domínio é relacional e transacional:
- uma ordem de serviço referencia cliente, veículo, serviços e peças;
- a aprovação de um orçamento altera a ordem de serviço e o estoque de várias peças na mesma operação;
- o estoque nunca pode ficar negativo;
- o tempo médio de execução é calculado a partir do histórico de mudanças de status.

O banco precisa garantir essas regras sob concorrência e responder a consultas agregadas.

## Decisão

- **PostgreSQL 17** (imagem `postgres:17-alpine`) como único banco da aplicação.
- O esquema é versionado com **Flyway**, em migrações SQL aplicadas na inicialização da aplicação. O Hibernate não cria nem altera tabelas.
- As regras de integridade que o banco consegue garantir ficam também no esquema, como chaves estrangeiras, unicidade de documento e placa e restrição de estoque não negativo. Assim a consistência não depende só do código.
- Os testes de integração usam o mesmo PostgreSQL, provisionado em container pelo Quarkus Dev Services.

## Alternativas

- **MySQL:** atenderia, mas o PostgreSQL oferece restrições `CHECK` aplicadas em todas as versões suportadas, tipos de data e intervalo mais completos para o cálculo de tempo de execução e funções de agregação sobre intervalos.
- **MongoDB:** o modelo é fortemente relacional e a aprovação exige atomicidade entre documentos de coleções diferentes, o que no MongoDB pede transações multi-documento e mais cuidado de modelagem sem ganho para o caso.
- **H2 em memória para os testes:** é mais rápido, mas testar num banco diferente do de execução esconde diferenças de SQL e de restrições.

## Consequências

- Positivas:
  - Atomicidade e integridade garantidas pelo banco.
  - Consultas de métricas em SQL.
  - Os testes exercitam o mesmo banco da execução.
- Negativas:
  - Os testes exigem Docker.
  - Mudanças de esquema exigem nova migração; migrações aplicadas não são editadas.
