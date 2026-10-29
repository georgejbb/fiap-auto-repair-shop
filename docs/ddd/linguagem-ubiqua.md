# Linguagem ubíqua

Este dicionário reúne os termos do domínio da oficina. É a referência para as conversas, a documentação, o Event Storming e o código.

Como usar:
- O termo em português é o usado com o negócio e na documentação.
- O identificador em inglês é o usado no código: classes, métodos, enums, tabelas e rotas.
- "Termos a evitar" lista sinônimos que geram ambiguidade. Eles não aparecem no código nem na documentação.
- Mudar um termo exige atualizar este dicionário no mesmo commit que altera o código.

Contextos delimitados (detalhes na [ADR 0002](../adr/0002-monolito-em-camadas-por-contexto.md)):

| Sigla | Contexto | Tipo |
|---|---|---|
| OS | Ordem de Serviço | Núcleo |
| CAD | Cadastro | Suporte |
| CAT | Catálogo e Estoque | Suporte |
| IDT | Identidade | Genérico |

## Ordem de Serviço

| Termo | Definição | Termos a evitar | Identificador | Contexto |
|---|---|---|---|---|
| Ordem de Serviço (OS) | Registro de um atendimento a um veículo, do recebimento à entrega. Agrega os itens, o orçamento e o histórico de status | Ticket, chamado, pedido, atendimento | `WorkOrder` | OS |
| Código da OS | Identificador único e não sequencial da OS. Vai para o cliente no e-mail do orçamento, e o cliente o usa para consultar e decidir | Número da OS, protocolo | `WorkOrder.id()` (UUID), `{code}` nas rotas | OS |
| Status da OS | Etapa em que a OS se encontra. Só muda pelas transições permitidas | Situação, fase, estado | `WorkOrderStatus` | OS |
| Recebida | Status inicial: a OS foi aberta com cliente e veículo identificados | Aberta, nova | `RECEIVED` | OS |
| Em diagnóstico | O mecânico avalia o veículo e inclui os serviços, as peças e os insumos necessários | Em análise, triagem | `IN_DIAGNOSIS` | OS |
| Aguardando aprovação | O orçamento foi gerado e enviado; a OS espera a decisão do cliente | Pendente, em aprovação | `AWAITING_APPROVAL` | OS |
| Em execução | O orçamento foi aprovado, as peças saíram do estoque e o serviço está sendo feito | Em andamento, em reparo | `IN_PROGRESS` | OS |
| Finalizada | O serviço terminou, ou o orçamento foi rejeitado e nada será executado | Concluída, fechada, encerrada | `COMPLETED` | OS |
| Entregue | O veículo voltou ao cliente. É o status final | Retirada, baixada | `DELIVERED` | OS |
| Abertura da OS | Criação da OS para um cliente e um de seus veículos, ambos ativos | Cadastro da OS | `WorkOrder.open` | OS |
| Histórico de status | Sequência de mudanças de status da OS. Cada mudança tem status de origem, status de destino e instante | Log, auditoria | `StatusChange`, `WorkOrder.statusHistory()` | OS |
| Diagnóstico | Etapa em que o mecânico define o que será feito no veículo. Iniciar o diagnóstico leva a OS a Em diagnóstico; concluí-lo gera o orçamento | Avaliação, triagem | `startDiagnosis`, `completeDiagnosis` | OS |
| Item da OS | Serviço, peça ou insumo incluído na OS, com quantidade e com o preço unitário copiado do catálogo no momento da inclusão | Produto da OS, linha do pedido | `ServiceLine`, `PartLine` | OS |
| Orçamento | Valor total da OS, calculado a partir dos itens e gerado automaticamente ao concluir o diagnóstico. Guarda também a decisão do cliente | Cotação, proposta, estimativa | `Estimate` | OS |
| Total do orçamento | Soma dos preços dos serviços com a quantidade vezes o preço unitário das peças e insumos | Valor da OS | `Estimate.total()` | OS |
| Orçamento emitido | Fato que dispara o envio do orçamento e do código da OS ao cliente por e-mail, depois de a conclusão do diagnóstico ser gravada | Notificação, disparo | `EstimateIssued` (evento), `EstimateMailer` | OS |
| Decisão do cliente | Aprovação ou rejeição do orçamento pelo cliente | Resposta, retorno | `EstimateDecision` | OS |
| Aprovação do orçamento | Decisão do cliente de autorizar a execução. Baixa o estoque e leva a OS a Em execução na mesma transação | Autorização, aceite | `CustomerDecisionService.approve`, `EstimateDecision.APPROVED` | OS |
| Rejeição do orçamento | Decisão do cliente de não autorizar a execução. Leva a OS a Finalizada sem execução e sem baixa de estoque | Recusa, cancelamento | `CustomerDecisionService.reject`, `EstimateDecision.REJECTED` | OS |
| Finalização | Registro de que o serviço terminou, levando a OS de Em execução a Finalizada | Encerramento, fechamento | `finish` | OS |
| Entrega | Devolução do veículo ao cliente, que leva a OS de Finalizada a Entregue | Retirada, saída | `deliver` | OS |
| Prioridade de listagem | Ordem da lista de OS: Em execução, Aguardando aprovação, Em diagnóstico, Recebida, Finalizada e Entregue; dentro do mesmo status, a mais antiga primeiro | Fila, ranking | `WorkOrderQueries.list` | OS |
| Tempo de execução | Intervalo entre a entrada da OS em Em execução e a mudança seguinte para Finalizada. OS com orçamento rejeitado não têm tempo de execução | Duração, SLA | view `work_order_execution_time` | OS |
| Tempo médio de execução | Média do tempo de execução das OS finalizadas num período, no geral e por serviço | Lead time, prazo médio | `ExecutionTimeReport` | OS |

## Cadastro

| Termo | Definição | Termos a evitar | Identificador | Contexto |
|---|---|---|---|---|
| Cliente | Pessoa física ou jurídica dona do veículo atendido. Tem documento, nome, e-mail para receber o orçamento e telefone | Usuário, consumidor, contato | `Customer` | CAD |
| Documento | CPF (pessoa física, 11 dígitos) ou CNPJ (pessoa jurídica, 14 caracteres, numérico ou alfanumérico), validado pelos dígitos verificadores. Não muda depois do cadastro | Identificação, registro, CPF genérico | `Document`, `DocumentType` (`CPF`, `CNPJ`) | CAD |
| Veículo | Automóvel atendido pela oficina, identificado pela placa e vinculado a um cliente | Carro, automóvel, bem | `Vehicle` | CAD |
| Placa | Identificação do veículo no formato antigo (AAA9999) ou Mercosul (AAA9A99) | Chapa, matrícula | `LicensePlate` | CAD |
| Marca | Fabricante do veículo | Montadora | `Vehicle.brand()` | CAD |
| Modelo | Modelo comercial do veículo | Versão | `Vehicle.model()` | CAD |
| Ano | Ano de fabricação do veículo, de 1900 até o ano seguinte ao atual | Ano-modelo | `Vehicle.year()` | CAD |
| Proprietário | Cliente ao qual o veículo está vinculado | Dono, titular do veículo | `Vehicle.ownerId()` | CAD |
| Exclusão lógica | Remoção de um cadastro sem apagar o registro. O cadastro deixa de aparecer em listagens e em novas OS, mas continua referenciado pelas OS existentes | Apagar, remover definitivamente | `deletedAt` | CAD, CAT |

## Catálogo e Estoque

| Termo | Definição | Termos a evitar | Identificador | Contexto |
|---|---|---|---|---|
| Serviço | Mão de obra oferecida pela oficina, com nome, descrição e preço (por exemplo, troca de óleo ou alinhamento) | Tarefa, atividade, produto | `ServiceOffering` | CAT |
| Peça | Componente substituído ou instalado no veículo, controlado em estoque | Produto, item, material | `Part` com `PartCategory.PART` | CAT |
| Insumo | Material consumido na execução (óleo, fluido, graxa), controlado em estoque | Consumível, material | `Part` com `PartCategory.SUPPLY` | CAT |
| Preço unitário | Valor de uma unidade de peça ou insumo; no serviço, o preço do serviço. Em reais | Custo, valor de venda | `Part.unitPrice()`, `ServiceOffering.price()` | CAT, OS |
| Valor monetário | Quantia em reais com duas casas decimais | Preço genérico, double | `Money` | CAT, OS |
| Estoque | Quantidade disponível de cada peça ou insumo. Nunca fica negativo | Inventário, saldo genérico | `Part.stockQuantity()` | CAT |
| Movimentação de estoque | Registro de entrada, baixa ou ajuste da quantidade de uma peça ou insumo, com quantidade, instante e, na baixa, a OS de origem | Lançamento, transação de estoque | `StockMovement` | CAT |
| Entrada de estoque | Movimentação que aumenta o estoque (compra ou reposição) | Compra, recebimento | `StockMovementType.INBOUND`, `Part.registerInbound` | CAT |
| Baixa de estoque | Movimentação que reduz o estoque na aprovação do orçamento de uma OS | Saída, consumo avulso, reserva | `StockMovementType.CONSUMPTION`, `Part.consume` | CAT |
| Ajuste de estoque | Correção manual do estoque para a quantidade contada | Acerto, inventário | `StockMovementType.ADJUSTMENT`, `Part.adjustTo` | CAT |
| Estoque insuficiente | Situação em que a quantidade pedida de uma peça ou insumo é maior que o estoque disponível. Impede a inclusão do item e a aprovação do orçamento | Falta, ruptura | `BusinessRuleException` (HTTP 422) | CAT, OS |

## Identidade e atores

| Termo | Definição | Termos a evitar | Identificador | Contexto |
|---|---|---|---|---|
| Usuário administrativo | Pessoa da oficina autorizada a usar as APIs administrativas mediante token | Operador, funcionário, admin genérico | `AdminUser` | IDT |
| Token de acesso | Credencial JWT emitida no login, com validade de uma hora, exigida nas APIs administrativas | Sessão, chave | `AccessToken` | IDT |
| Atendente | Ator que recebe o cliente, abre a OS e registra a entrega. No sistema, usa o papel administrativo | Recepcionista | papel `ADMIN` | IDT |
| Mecânico | Ator que faz o diagnóstico e a execução. No sistema, usa o papel administrativo | Técnico | papel `ADMIN` | IDT |
| Gestor de estoque | Ator que cadastra peças e insumos e registra entradas e ajustes. No sistema, usa o papel administrativo | Almoxarife | papel `ADMIN` | IDT |
| Cliente (ator) | Consulta a OS e decide sobre o orçamento informando o código da OS e o documento, sem conta no sistema | Usuário | sem papel; cabeçalho `X-Customer-Document` | OS |

## Sistemas externos

| Termo | Definição | Termos a evitar | Identificador | Contexto |
|---|---|---|---|---|
| Servidor de e-mail | Servidor SMTP que entrega o e-mail do orçamento ao cliente. No ambiente local, o Mailpit em Docker | Mensageria, notificador | configuração `quarkus.mailer` | OS |
| Painel de observabilidade | Grafana, que exibe o tempo de execução a partir das views do banco, com usuário somente leitura | Relatório, BI | views `work_order_execution_time` e `work_order_service_execution_time` | OS |

## Relação com o código

Pacote base: `br.com.fiap.autorepairshop`. Cada contexto tem as camadas `api`, `application`, `domain` e `infrastructure`; o domínio não depende de framework.

### Contextos e pacotes

| Contexto | Pacote | Serviço de aplicação | Recurso REST |
|---|---|---|---|
| Ordem de Serviço | `workorder` | `WorkOrderService`, `CustomerDecisionService` | `WorkOrderResource`, `CustomerWorkOrderResource` |
| Cadastro | `customer` | `CustomerService`, `VehicleService` | `CustomerResource`, `VehicleResource` |
| Catálogo e Estoque | `catalog` | `ServiceOfferingService`, `PartService` | `ServiceOfferingResource`, `PartResource` |
| Identidade | `identity` | `AuthenticationService` | `AuthResource` |
| Compartilhado | `shared` | não se aplica | mapeadores de erro (Problem Details) |

### Agregados, entidades e tabelas

| Agregado | Classe raiz | Entidades e objetos de valor | Tabelas |
|---|---|---|---|
| Ordem de Serviço | `WorkOrder` | `ServiceLine`, `PartLine`, `Estimate`, `StatusChange`, `Money` | `work_order`, `work_order_service_line`, `work_order_part_line`, `work_order_status_change` |
| Cliente | `Customer` | `Document` | `customer` |
| Veículo | `Vehicle` | `LicensePlate` | `vehicle` |
| Serviço | `ServiceOffering` | `Money` | `service_offering` |
| Peça ou insumo | `Part` | `StockMovement`, `Money` | `part`, `stock_movement` |
| Usuário administrativo | `AdminUser` | não se aplica | `admin_user` |

Os agregados se referenciam apenas pelo identificador: a OS guarda `customerId`, `vehicleId`, `serviceOfferingId` e `partId`, e copia nome e preço no item.

### Comandos e rotas

| Comando | Rota | Método de aplicação | Método de domínio |
|---|---|---|---|
| Autenticar | `POST /api/auth/login` | `AuthenticationService` | `AdminUser` |
| Cadastrar, consultar, alterar e excluir cliente | `/api/admin/customers` e `/api/admin/customers/{id}` | `CustomerService` | `Customer.register`, `update`, `delete` |
| Cadastrar, consultar, alterar e excluir veículo | `/api/admin/vehicles` e `/api/admin/vehicles/{id}` | `VehicleService` | `Vehicle.register`, `update`, `delete` |
| Manter serviço | `/api/admin/service-offerings` e `/api/admin/service-offerings/{id}` | `ServiceOfferingService` | `ServiceOffering` |
| Manter peça ou insumo | `/api/admin/parts` e `/api/admin/parts/{id}` | `PartService` | `Part.create`, `update`, `delete` |
| Registrar entrada de estoque | `POST /api/admin/parts/{id}/stock-entries` | `PartService.registerInbound` | `Part.registerInbound` |
| Ajustar estoque | `POST /api/admin/parts/{id}/stock-adjustments` | `PartService.adjustStock` | `Part.adjustTo` |
| Consultar movimentações | `GET /api/admin/parts/{id}/movements` | `PartService.movements` | não se aplica |
| Abrir OS | `POST /api/admin/work-orders` | `WorkOrderService.open` | `WorkOrder.open` |
| Listar OS por prioridade | `GET /api/admin/work-orders` | `WorkOrderQueries.list` | não se aplica |
| Consultar OS | `GET /api/admin/work-orders/{code}` | `WorkOrderService.get` | não se aplica |
| Iniciar diagnóstico | `POST /api/admin/work-orders/{code}/start-diagnosis` | `WorkOrderService.startDiagnosis` | `WorkOrder.startDiagnosis` |
| Incluir e remover serviço | `POST /api/admin/work-orders/{code}/services`, `DELETE .../services/{lineId}` | `WorkOrderService.addService`, `removeService` | `WorkOrder.addService`, `removeServiceLine` |
| Incluir e remover peça ou insumo | `POST /api/admin/work-orders/{code}/parts`, `DELETE .../parts/{lineId}` | `WorkOrderService.addPart`, `removePart` | `WorkOrder.addPart`, `removePartLine` |
| Concluir diagnóstico e emitir orçamento | `POST /api/admin/work-orders/{code}/complete-diagnosis` | `WorkOrderService.completeDiagnosis` | `WorkOrder.completeDiagnosis` |
| Consultar OS (cliente) | `GET /api/public/work-orders/{code}` | `CustomerDecisionService.consult` | não se aplica |
| Aprovar orçamento | `POST /api/public/work-orders/{code}/approve` | `CustomerDecisionService.approve` | `WorkOrder.approve`, `Part.consume` |
| Rejeitar orçamento | `POST /api/public/work-orders/{code}/reject` | `CustomerDecisionService.reject` | `WorkOrder.reject` |
| Finalizar serviço | `POST /api/admin/work-orders/{code}/finish` | `WorkOrderService.finish` | `WorkOrder.finish` |
| Entregar veículo | `POST /api/admin/work-orders/{code}/deliver` | `WorkOrderService.deliver` | `WorkOrder.deliver` |
| Consultar tempo médio de execução | `GET /api/admin/work-orders/metrics/execution-time` | `WorkOrderQueries.executionTime` | não se aplica |

### Eventos de domínio no código

| Evento do Event Storming | Onde acontece |
|---|---|
| OS aberta | `WorkOrder.open`, status `RECEIVED` |
| Diagnóstico iniciado | `WorkOrder.startDiagnosis`, `StatusChange` para `IN_DIAGNOSIS` |
| Item incluído ou removido | `WorkOrder.addService`, `addPart`, `removeServiceLine`, `removePartLine` |
| Orçamento gerado | `WorkOrder.completeDiagnosis` cria o `Estimate`, status `AWAITING_APPROVAL` |
| Orçamento enviado | evento CDI `EstimateIssued`, observado por `EstimateMailer` depois do commit |
| Orçamento aprovado | `WorkOrder.approve`, status `IN_PROGRESS` |
| Estoque baixado | `Part.consume`, `StockMovement` do tipo `CONSUMPTION` com a OS de origem |
| Orçamento rejeitado | `WorkOrder.reject`, status `COMPLETED` |
| Serviço finalizado | `WorkOrder.finish`, status `COMPLETED` |
| Veículo entregue | `WorkOrder.deliver`, status `DELIVERED` |
| Estoque abastecido ou ajustado | `Part.registerInbound` e `Part.adjustTo`, `StockMovement` `INBOUND` ou `ADJUSTMENT` |
