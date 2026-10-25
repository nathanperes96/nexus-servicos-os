# Checklist de primeira publicação

## Preparação já aplicada nesta cópia

- [x] Incluída a estrutura raiz do Android Studio, scripts Gradle e wrapper.
- [x] Excluídos `.gradle`, `.idea`, `.kotlin`, `local.properties`, diretórios `build/` e APKs locais.
- [x] Ampliado `.gitignore` para caches, bancos, exportações, credenciais e artefatos Android.
- [x] Adicionados README, diagrama de arquitetura, fluxo principal, roadmap e status de limitações.
- [x] Removido o telefone padrão do modelo de configuração na cópia de portfólio.

## Ainda precisa ser feito antes de divulgar como versão para uso real

- [ ] Corrigir a limpeza de demonstração: a implementação atual pode apagar todas as OS, clientes, fotos, itens, equipamentos e histórico.
- [ ] Bloquear salvamento/geração de PDF com dados obrigatórios ausentes.
- [ ] Adicionar transações para edição e restauração.
- [ ] Validar a configuração Java 25 do daemon Gradle e concluir build/testes limpos no Android Studio.
- [ ] Testar em emulador/dispositivo sem dados reais.
- [ ] Capturar screenshots atuais e seguras da Home, OS, fotos/orçamento e PDF.
- [ ] Definir uma licença ou manter explicitamente sem licença.
- [ ] Conferir `git status --short` e inspecionar os arquivos antes do primeiro commit.

## Configuração sugerida do GitHub

- **Nome:** `nexus-servicos-os`
- **Descrição:** `Aplicativo Android em Kotlin para gestão de ordens de serviço, clientes, equipamentos, orçamento, fotos e PDF de uma oficina.`
- **Visibilidade:** Public, após a revisão final dos arquivos.
- **Topics:** `android`, `kotlin`, `jetpack-compose`, `room-database`, `sqlite`, `pdf-generation`, `mobile-app`, `ai-assisted-development`, `portfolio-project`.

## Comandos locais antes do primeiro commit

```bash
git init
git add .
git status --short
```

Revise cada arquivo listado. Só então faça o commit:

```bash
git commit -m "chore: publish Nexus Serviços OS v1 portfolio snapshot"
```

Crie um repositório vazio no GitHub e siga as instruções de conexão mostradas pelo GitHub para adicionar o remote e enviar a branch. Nunca adicione banco real, fotos de clientes, PDFs, backups, `local.properties` nem chaves de assinatura.
