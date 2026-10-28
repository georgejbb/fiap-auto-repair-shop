# ADR-0005: Exclusão lógica e atualização por PUT

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

Clientes, veículos, serviços, peças e insumos são referenciados pelas ordens de serviço, que precisam manter o histórico mesmo depois que um cadastro deixa de ser usado. Documento do cliente, placa do veículo e estoque da peça têm regras próprias: os dois primeiros identificam o registro, e o estoque só muda por movimentação.

## Decisão

1. **`DELETE` faz exclusão lógica.**
   - O registro recebe `deleted_at` e deixa de aparecer em consultas, listagens e novas operações.
   - Registros excluídos continuam disponíveis para as ordens de serviço que já os referenciam.
   - A unicidade de documento e placa vale só entre registros ativos, por índices únicos parciais (`WHERE deleted_at IS NULL`), para que um cliente excluído possa ser cadastrado de novo.
2. **A alteração é feita por `PUT`**, com todos os campos alteráveis do recurso. Os campos imutáveis ficam fora do corpo do `PUT`:
   - documento do cliente;
   - placa do veículo;
   - estoque da peça, que muda por entrada ou ajuste.
3. **Não há `PATCH`.**

## Alternativas

- **Exclusão física:** removeria registros referenciados por ordens de serviço, forçando exclusão em cascata ou bloqueio da operação, com perda de histórico.
- **Coluna booleana `active`:** equivalente para filtrar, mas sem registrar quando a exclusão aconteceu.
- **`PATCH` com JSON Merge Patch:** permite alterar um campo isolado, mas exige definir a semântica de campo ausente e de campo nulo, sem ganho para recursos com poucos campos.
- **Permitir alterar documento ou placa:** quebraria a identidade do cadastro e o vínculo com ordens de serviço antigas; a correção de um documento digitado errado é feita excluindo e cadastrando de novo.

## Consequências

- Positivas:
  - O histórico das ordens de serviço é preservado.
  - Não é possível alterar documento, placa ou estoque pelo cadastro.
  - Os contratos de alteração são simples.
- Negativas:
  - Toda consulta precisa filtrar os registros ativos; o filtro fica concentrado nos repositórios.
  - Registros excluídos continuam ocupando espaço no banco.
