# Status técnico e limitações conhecidas

**Base da revisão:** `ProjetosAndroid(2).zip`, recebido em 25/10/2026. Foram examinados fontes Kotlin, recursos, manifest, arquivos Gradle, catálogo de versões e arquivos de teste. A revisão foi estática; o build não foi reexecutado neste ambiente por falta de Android SDK instalado.

## Confirmado no código/configuração

- Projeto Android nativo com Kotlin e Jetpack Compose.
- `compileSdk 34`, `targetSdk 34`, `minSdk 24`.
- Gradle Wrapper 9.6.0, Android Gradle Plugin 9.4.1, Kotlin 2.2.10, Compose BOM 2026.02.01 e Room 2.8.5 declarados.
- Room com entidades para clientes, OS, equipamentos, itens, fotos, histórico e configurações; versão declarada do banco: 6.
- Geração de PDF com `PdfDocument`/Canvas e compartilhamento via Intent/FileProvider.
- Geração de CSV e ZIP com JSON estruturado e fotos; há rotina de restauração a partir de ZIP.
- Cálculos centrais do orçamento usam valores inteiros em centavos (`Long`), embora alguns modelos também mantenham valores monetários em `Double`.
- Existem quatro testes de unidade em `ExampleUnitTest.kt` e um teste instrumentado em `ExampleInstrumentedTest.kt`. A existência dos testes foi confirmada, mas eles não foram executados nesta revisão.
- A cópia original contém `local.properties` com caminho do SDK no perfil Windows. Esse arquivo foi removido da cópia pública preparada.
- O arquivo ZIP original contém pastas e arquivos gerados em `.gradle`, `.idea`, `app/build` e diretórios similares. Eles foram excluídos da versão preparada para GitHub.

## Riscos que precisam de correção

### P0 — Limpeza de demonstração apaga tabelas completas

Em `MainActivity.kt`, o callback `onLimparBanco` chama métodos de limpeza global de fotos, itens, equipamentos, histórico, ordens e clientes. O comportamento não está limitado à OS Demo. Não use essa ação com dados reais até a implementação ser corrigida e testada.

### P0 — Validação da OS não está centralizada/comprovada

O fluxo de continuidade de `NovaOsScreen.kt` encaminha o rascunho sem um validador central identificado na cópia. Em `MainActivity.kt`, há fallback `nomeFormatado.ifBlank { "Cliente sem nome" }`, e não foi encontrada classe `ValidadorOrdemServico`. Isso significa que o requisito do resumo — bloquear dados incompletos antes de gravar e gerar PDF — não está implementado nesta cópia de forma demonstrável.

### P0 — Salvamento/edição sem transação abrangente

Não foram encontradas anotações `@Transaction` nem chamadas `withTransaction` no código analisado. Na edição, a OS é atualizada e equipamentos/itens/fotos são apagados e reinseridos por operações DAO separadas. Uma falha intermediária pode deixar o agregado em estado parcial.

### P0 — Restauração pode deixar estado parcial

A função `restaurarBackupDeUri` extrai fotos e lê o JSON antes de limpar e repopular várias tabelas em sequência. Não foi encontrada uma transação cobrindo a restauração completa. O ZIP/JSON deve ser validado antes de substituir dados e a alteração do banco deve ter rollback apropriado.

### P1 — Configuração de Java do Gradle

`gradle/gradle-daemon-jvm.properties` configura `toolchainVersion=25`. A documentação oficial do AGP 9.4 lista JDK 17 como mínimo/padrão. Isso não prova incompatibilidade, mas a configuração precisa ser verificada num build limpo e ajustada deliberadamente se causar problemas. Fonte: [notas de compatibilidade do AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes).

### P1 — Número de OS e exportação

O número da OS deve ser revisto para cenários com exclusões ou operações simultâneas. A exportação CSV precisa ser testada com vírgulas, quebras de linha, aspas e valores iniciados por caracteres interpretáveis por planilhas.

## Higiene da publicação

- O `local.properties` não deve ser versionado; contém um caminho específico da máquina.
- Não versionar banco local, fotos, PDFs, CSVs/ZIPs de backup de oficina, keystores, senhas ou tokens.
- O número de WhatsApp que existia no padrão do modelo foi removido da cópia pública preparada; configure-o em runtime nas configurações da oficina.
- Dados de demonstração devem permanecer claramente fictícios.
- Os diretórios de build e caches foram excluídos; o repositório contém fontes e recursos necessários, não APKs locais.

## Escopo da revisão

Esta inspeção não substitui um build Android Studio, testes em dispositivo/emulador, uma revisão completa de segurança nem testes de aceitação com uma cópia descartável do banco.
