# ADR-0002: Monolito em camadas organizado por bounded context

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

O MVP precisa de um back-end monolítico organizado em camadas, com Domain-Driven Design aplicado à gestão de ordens de serviço, clientes, veículos, serviços, peças e insumos.

O Event Storming mostrou três grupos de comandos com pouca ligação entre si:
- cadastro de clientes e veículos;
- catálogo de serviços e estoque de peças e insumos;
- ciclo de vida da ordem de serviço.

A autenticação das APIs administrativas é um quarto assunto, genérico.

## Decisão

1. **Quatro bounded contexts, cada um num pacote de topo** de `br.com.fiap.autorepairshop`:

   | Contexto | Pacote | Subdomínio | Agregados |
   |---|---|---|---|
   | Ordem de Serviço | `workorder` | Principal | `WorkOrder` (com `ServiceLine`, `PartLine`, `Estimate`, `StatusChange`) |
   | Cadastro | `customer` | Suporte | `Customer`, `Vehicle` |
   | Catálogo e Estoque | `catalog` | Suporte | `ServiceOffering`, `Part` (com `StockMovement`) |
   | Identidade | `identity` | Genérico | `AdminUser` |

2. **Quatro camadas dentro de cada contexto:**
   - `api`: recursos REST e contratos de entrada e saída.
   - `application`: casos de uso. Orquestra o domínio e define a fronteira de transação, sem regra de negócio.
   - `domain`: agregados, entidades, objetos de valor e regras. Não depende de framework (sem Quarkus, Jakarta Persistence ou Jakarta REST).
   - `infrastructure`: persistência e integrações. As entidades JPA ficam aqui, separadas do modelo de domínio, com mapeamento explícito.
3. **Código transversal em `shared`**, também por camada: `shared.api` para o mapeamento de erros em Problem Details e `shared.domain` para tipos de domínio comuns.
4. **Agregados se referenciam por id.** Ao entrar na OS, um serviço ou uma peça leva uma cópia do nome e do preço vigentes, para que mudanças posteriores no catálogo não alterem orçamentos já emitidos.

## Alternativas

- **Arquitetura hexagonal (portas e adaptadores):** isola melhor o domínio, mas exige interfaces de porta e adaptadores para cada caso de uso. Com um adaptador de entrada (REST) e um de saída (PostgreSQL), o custo não tem retorno no MVP. Como o domínio já não depende de framework, a migração continua possível.
- **Clean Architecture:** mesma objeção, com separação adicional entre entidades e casos de uso.
- **Vertical Slice:** entrega rápido por funcionalidade, mas dilui os agregados e as regras de domínio entre fatias.
- **Pacotes por camada técnica** (`controller`, `service`, `repository`): é o formato mais comum em camadas, porém espalha cada contexto pelo código e esconde os bounded contexts na estrutura de pastas.
- **Veículo dentro do agregado Cliente:** o veículo tem cadastro próprio e é referenciado diretamente pela OS. Dentro do Cliente, toda alteração de veículo carregaria o cliente inteiro.
- **Contextos separados para catálogo de serviços e estoque:** o serviço do catálogo é consumido pela OS da mesma forma que a peça. Um contexto só para ele não traria regra própria.

## Consequências

- Positivas:
  - A estrutura de pacotes espelha os bounded contexts da documentação DDD.
  - O domínio é testável sem container nem banco.
  - Um contexto pode ser extraído para um serviço próprio, levando seu pacote.
- Negativas:
  - O mapeamento entre entidades JPA e objetos de domínio é código a mais em cada contexto.
  - A cópia de nome e preço duplica dados do catálogo na OS. A duplicação é intencional.
