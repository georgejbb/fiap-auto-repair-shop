# ADR-0006: Consistência entre ordem de serviço, estoque e notificação

- **Data:** 28/10/2026
- **Status:** Aceita

## Contexto

Dois momentos do ciclo da ordem de serviço envolvem mais de um agregado ou sistema:

1. **Aprovação do orçamento:** muda o status da OS (agregado `WorkOrder`, contexto Ordem de Serviço) e baixa o estoque de cada peça ou insumo da OS (agregado `Part`, contexto Catálogo e Estoque). O sistema não pode aprovar um orçamento sem estoque para todos os itens, nem baixar o estoque de um orçamento que não foi aprovado.
2. **Conclusão do diagnóstico:** gera o orçamento e avisa o cliente por e-mail. O cliente não pode receber o e-mail de um orçamento que não foi gravado.

A regra geral do DDD é alterar um agregado por transação e manter a consistência entre agregados de forma eventual, por eventos. O sistema é um monolito com um único banco PostgreSQL.

## Decisão

1. **A aprovação altera a OS e o estoque numa única transação local.**
   - O caso de uso `CustomerDecisionService.approve` aprova a OS e chama `PartService.consumeForWorkOrder` para cada peça ou insumo.
   - Cada baixa grava uma movimentação `CONSUMPTION` com o código da OS.
   - Se faltar estoque de qualquer item, ou se um item tiver sido excluído do catálogo, a exceção desfaz a aprovação e as baixas já feitas. A OS continua aguardando aprovação e a resposta é 422.
   - Duas operações simultâneas sobre o mesmo item são resolvidas pela coluna de versão da peça; a segunda recebe 409. A restrição `stock_quantity >= 0` no banco é a última barreira.
2. **O e-mail do orçamento só é enviado depois da confirmação da transação.**
   - A conclusão do diagnóstico dispara o evento de aplicação `EstimateIssued`.
   - O `EstimateMailer` observa o evento na fase `AFTER_SUCCESS`: se a transação for desfeita, ele não é executado.
   - Uma falha no envio é registrada no log e não desfaz o orçamento, que continua disponível na consulta do cliente.
3. **A conferência de estoque na inclusão do item é só um aviso antecipado.** A conferência que vale é a da aprovação, porque entre a inclusão e a aprovação o estoque pode ser usado por outra OS ou corrigido por ajuste.

## Alternativas

- **Um agregado por transação, com baixa assíncrona por evento.** A aprovação publicaria "Orçamento aprovado" e o estoque faria a baixa depois, com compensação (voltar a OS para aguardando aprovação) quando faltasse saldo. Seria o desenho para contextos em serviços separados. Exigiria fila ou outbox, um status intermediário fora dos seis previstos e lógica de compensação, num sistema em que a transação local garante a regra sem esse custo.
- **Reserva de estoque na inclusão do item.** Garantiria o saldo desde o diagnóstico, mas prenderia estoque de orçamentos que podem ser rejeitados ou nunca respondidos, e exigiria liberar reservas por prazo.
- **Envio do e-mail dentro da transação.** Um e-mail enviado antes do commit pode anunciar um orçamento que acaba desfeito. E se o envio falhasse, desfaria a conclusão do diagnóstico por um problema do servidor de e-mail.
- **Outbox para o e-mail, com nova tentativa.** Garantiria a entrega mesmo com o servidor de e-mail fora do ar, ao custo de tabela, agendador e controle de duplicidade. Como o orçamento também está na consulta do cliente, a perda eventual de um e-mail não impede a decisão.

## Consequências

- Positivas:
  - O estoque nunca fica divergente das ordens de serviço aprovadas.
  - O cliente nunca é avisado de um orçamento inexistente.
  - As duas regras são verificáveis por testes de integração, inclusive a reversão da baixa quando o segundo item falha.
- Negativas:
  - O caso de uso de aprovação acopla os contextos Ordem de Serviço e Catálogo e Estoque na mesma transação. Numa separação em serviços, este é o ponto que precisa virar evento com compensação.
  - Um e-mail perdido por falha do servidor de e-mail não é reenviado automaticamente.
