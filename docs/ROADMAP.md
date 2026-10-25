# Roadmap — Nexus Serviços OS

A sequência prioriza integridade de dados e verificabilidade antes de melhorias cosméticas. Marque uma tarefa como concluída somente após testar a mudança aplicável.

## P0 — Segurança e consistência

- [ ] Corrigir a opção **Limpar dados de demonstração** para remover somente registros de demonstração identificados explicitamente; nunca chamar deletes globais de tabelas.
- [ ] Criar um validador reutilizável para cliente, contato, equipamento e defeito; bloquear persistência e geração de PDF quando os dados mínimos estiverem ausentes.
- [ ] Salvar/editar a OS e seus equipamentos, itens e fotos em uma operação transacional com rollback.
- [ ] Validar estrutura, versão e conteúdo de um backup antes de tocar no banco atual.
- [ ] Envolver restauração do banco em uma transação e tratar com cuidado os arquivos de foto, inclusive backup/rollback de arquivos extraídos.
- [ ] Tratar exceções em salvamento, PDF, backup e restauração com mensagens claras sem perder o estado da tela.
- [ ] Fazer build limpo e testes em emulador/dispositivo com dados descartáveis; confirmar backup de recuperação antes dos testes destrutivos.

## P1 — Testes e regras de domínio

- [ ] Testes de unidade para campos obrigatórios, formatos de telefone e dados de equipamento.
- [ ] Testes de integração para criação e edição de OS com vários equipamentos, reprovação individual, descontos e recálculo do total.
- [ ] Testes de restauração completa: dados inválidos, fotos ausentes, JSON malformado e erro durante a persistência.
- [ ] Testes de migração do Room desde as versões anteriores suportadas, principalmente 4 → 5 → 6.
- [ ] Rever a numeração de OS para exclusões e concorrência.
- [ ] Testar CSV com dados especiais e entradas potencialmente interpretadas como fórmulas por planilhas.
- [ ] Rever tratamento de fotos ausentes/corrompidas e permissões de compartilhamento.

## P2 — Manutenção e portfólio

- [ ] Extrair regras e persistência da `MainActivity` para componentes menores/testáveis, sem reescrever a interface sem necessidade.
- [ ] Avaliar ViewModels e divisão de responsabilidades gradualmente.
- [ ] Criar pipeline GitHub Actions para build e testes depois de validar as configurações de JDK/Android SDK.
- [ ] Capturar screenshots reais, sem dados pessoais, e incluí-las no README.
- [ ] Publicar release/tag quando uma versão for reproduzível e os riscos críticos estiverem resolvidos.
- [ ] Definir licença apenas depois de decidir quais permissões de uso/redistribuição deseja conceder.
