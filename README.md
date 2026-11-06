# AirSense Backend

API REST do **AirSense**, sistema de monitoramento de qualidade do ar com dispositivos IoT. O backend cuida de usuários, autenticação, ambientes, cômodos e dispositivos, e serve de base para o app Android do projeto.

Projeto acadêmico desenvolvido na PUCPR.

Alunos:
Edgar Felipe Polswut
Davi Cagnato Pinto
Nicolas Jacinto Grzebieniak de Oliveira

## Sumário

- [Tecnologias](#tecnologias)
- [Funcionalidades](#funcionalidades)
- [Como rodar](#como-rodar)
- [Configuração](#configuração)
- [Autenticação](#autenticação)
- [Endpoints](#endpoints)
- [Modelo de dados](#modelo-de-dados)
- [Tratamento de erros](#tratamento-de-erros)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Testes](#testes)
- [Observações para produção](#observações-para-produção)

## Tecnologias

| Item | Versão / detalhe |
|---|---|
| Linguagem | Kotlin 1.9.25 |
| JDK | 21 |
| Framework | Spring Boot 3.3.4 |
| Persistência | Spring Data JPA + Hibernate |
| Banco | H2 em memória |
| Segurança | Spring Security + JWT (jjwt 0.12.5, HS256) |
| Senhas | BCrypt |
| Documentação | springdoc-openapi 2.6.0 (Swagger UI) |
| Build | Gradle (wrapper incluso) |

## Funcionalidades

- Cadastro e login de usuários com token JWT (validade de 24 horas).
- Dois perfis: `USER` e `ADMIN`.
- CRUD de **ambientes** (visão macro, ex: "Casa", "Escritório") com **cômodos** (visão micro) dentro de cada um.
- CRUD de **dispositivos** com faixas seguras configuráveis de temperatura, umidade, qualidade do ar e CO₂.
- Vincular e desvincular dispositivos a cômodos.
- Busca com filtros e ordenação em dispositivos e ambientes.
- Cada usuário só enxerga os próprios ambientes e dispositivos.
- Respostas de erro padronizadas em JSON.
- Swagger UI com botão **Authorize** para testar rotas protegidas.

## Como rodar

### Pré-requisitos

- JDK 21 (o Gradle também consegue baixar via toolchain)
- Nada de banco externo: o H2 sobe junto com a aplicação

### Passos

```bash
# 1. Clonar o repositório
git clone <url-do-repositorio>
cd cagnato-backend_airsense

# 2. Subir a aplicação
./gradlew bootRun          # Linux / macOS
gradlew.bat bootRun        # Windows
```

A API sobe em `http://localhost:8080`.

### Links úteis

| O quê | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |
| Console do H2 | http://localhost:8080/h2-console |

No console do H2, use:

- **JDBC URL:** `jdbc:h2:mem:airsensedb`
- **User:** `sa`
- **Password:** (vazio)

Como o banco é em memória, **os dados são perdidos toda vez que a aplicação reinicia**.

## Configuração

Tudo fica em `src/main/resources/application.yaml`.

| Propriedade | Para que serve | Padrão |
|---|---|---|
| `server.port` | Porta da API | `8080` |
| `airsense.admin.email` | E-mail do admin criado na inicialização | `admin@airsense.com` |
| `airsense.admin.password` | Senha do admin inicial | `admin123` |
| `airsense.jwt.secret` | Chave para assinar os tokens (mínimo 32 caracteres) | definida no arquivo |
| `spring.jpa.hibernate.ddl-auto` | Criação/atualização das tabelas | `update` |

Na primeira subida, o `BootStrapper` cria as roles `USER` e `ADMIN` e o usuário administrador inicial (se ele ainda não existir).

## Autenticação

A autenticação é feita com JWT no header `Authorization`.

**1. Criar conta** (rota pública)

```http
POST /users
Content-Type: application/json

{
  "name": "Maria",
  "email": "maria@email.com",
  "password": "senha123"
}
```

**2. Fazer login** (rota pública)

```http
POST /users/login
Content-Type: application/json

{
  "username": "maria@email.com",
  "password": "senha123"
}
```

Resposta:

```json
{
  "token": "eyJhbGciOi...",
  "user": { "id": 2, "name": "Maria", "email": "maria@email.com", "roles": ["USER"] }
}
```

**3. Usar o token nas demais rotas**

```http
Authorization: Bearer eyJhbGciOi...
```

No Swagger, clique em **Authorize** e cole só o token.

Detalhes que valem saber:

- O filtro confere o usuário no banco a cada requisição. Se o usuário for apagado, o token deixa de funcionar na hora.
- As roles usadas são as atuais do banco, não as que estavam gravadas no token.
- E-mails são normalizados (sem espaços e em minúsculas).

## Endpoints

Todas as rotas exigem token, exceto as marcadas como públicas.

### Usuários (`/users`)

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/users` | Público | Cadastra um usuário (role `USER`) |
| POST | `/users/login` | Público | Login, retorna o token |
| GET | `/users/me` | Autenticado | Dados do usuário logado |
| GET | `/users` | `ADMIN` | Lista todos os usuários |
| GET | `/users/{id}` | Dono ou `ADMIN` | Busca um usuário |
| PUT | `/users/{id}` | Dono ou `ADMIN` | Atualiza o nome |
| DELETE | `/users/{id}` | Dono ou `ADMIN` | Remove o usuário e seus dispositivos e ambientes |
| PUT | `/users/{id}/roles/{role}` | `ADMIN` | Adiciona uma role ao usuário |

Regras: usuário comum que tenta acessar outro id recebe `404`. Não dá para remover o último administrador.

### Ambientes (`/environments`)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/environments` | Cria um ambiente |
| GET | `/environments` | Lista com filtros `name`, `city` e ordenação |
| GET | `/environments/{id}` | Busca um ambiente (com seus cômodos) |
| PUT | `/environments/{id}` | Atualiza o ambiente |
| DELETE | `/environments/{id}` | Remove o ambiente (dispositivos ficam sem ambiente e sem cômodo) |
| POST | `/environments/{id}/rooms` | Adiciona um cômodo |
| GET | `/environments/{id}/rooms` | Lista os cômodos |
| PUT | `/environments/{id}/rooms/{roomId}` | Atualiza um cômodo |
| DELETE | `/environments/{id}/rooms/{roomId}` | Remove um cômodo (dispositivos continuam no ambiente) |

Ordenação de ambientes: `sortBy` = `id`, `name` ou `city`.

### Dispositivos (`/devices`)

| Método | Rota | Descrição |
|---|---|---|
| POST | `/devices` | Cadastra um dispositivo |
| GET | `/devices` | Lista com filtros e ordenação |
| GET | `/devices/{id}` | Busca um dispositivo |
| PUT | `/devices/{id}` | Substitui os dados do dispositivo |
| PUT | `/devices/{id}/bind/{roomId}` | Vincula a um cômodo (o ambiente passa a ser o do cômodo) |
| DELETE | `/devices/{id}/unbind` | Desvincula do cômodo (continua no ambiente) |
| DELETE | `/devices/{id}` | Exclui o dispositivo |

**Filtros da listagem:** `name` (contém, sem diferenciar maiúsculas), `status`, `roomId`, `environmentId`.

**Ordenação:** `sortBy` = `id`, `name`, `deviceId`, `status` ou `environment`; `sortDir` = `asc` ou `desc`. Na ordenação por ambiente, dispositivos sem ambiente vão para o fim.

**Status:** `OFF` (0), `ACTIVE` (1) e `NOT_WORKING` (2). A API aceita o nome ou o código numérico, tanto no corpo quanto na query (`?status=ACTIVE` ou `?status=1`).

Exemplo de criação:

```json
{
  "name": "Sensor da sala",
  "status": "ACTIVE",
  "environmentId": 1,
  "roomId": 3,
  "tempMinSafe": 18.0,
  "tempMaxSafe": 26.0,
  "co2MaxSafe": 1000.0
}
```

Se o `deviceId` não for enviado, o servidor gera um no formato `DEV-XXXXXXXX`.

## Modelo de dados

```
User ──< Environment ──< Room
  │            │            │
  └──< Device >┴────────────┘
```

- **User**: e-mail único, senha em BCrypt, roles (`USER`, `ADMIN`).
- **Environment**: nome, descrição, cidade, dono, lista de cômodos.
- **Room**: nome, área em m², ambiente.
- **Device**: `deviceId` único, nome, status, faixas seguras, dono, ambiente e cômodo opcionais.

### Faixas seguras de um dispositivo

| Medida | Padrão (mín – máx) | Limite aceito |
|---|---|---|
| Temperatura (°C) | 18 – 26 | -10 a 50 |
| Umidade (%) | 30 – 60 | 0 a 100 |
| Qualidade do ar | 0 – 100 | 0 a 500 |
| CO₂ (ppm) | 400 – 1000 | 300 a 5000 |

O mínimo nunca pode ser maior que o máximo, senão a API responde `400`.

## Tratamento de erros

Todos os erros seguem o mesmo formato:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Os dados enviados são inválidos",
  "details": ["name: O nome do dispositivo é obrigatório"]
}
```

| Código | Quando acontece |
|---|---|
| 400 | Validação falhou, JSON malformado, parâmetro inválido |
| 401 | Sem token, token inválido ou login errado |
| 403 | Sem permissão para o recurso |
| 404 | Recurso não existe (ou não pertence ao usuário) |
| 405 / 415 | Método ou Content-Type não suportado |
| 409 | E-mail ou `deviceId` já existente, violação de integridade |
| 500 | Erro inesperado |

## Estrutura do projeto

```
src/main/kotlin/br/pucpr/airsense/
├── AirSenseApplication.kt     # Ponto de entrada
├── BootStrapper.kt            # Cria roles e admin inicial
├── OpenApiConfig.kt           # Swagger com autenticação Bearer
├── common/                    # Utilitários (ordenação)
├── devices/                   # Controller, service, repository e modelos de dispositivos
├── environments/              # Ambientes e cômodos
├── exception/                 # Exceções de negócio e handler global
├── security/                  # JWT, filtro, config de segurança
└── users/                     # Usuários e roles
```

O código segue a divisão por domínio, e cada um tem controller, service, repository e modelos (entidades e DTOs).

## Testes

```bash
./gradlew test
```

Hoje existe apenas o teste `contextLoads`, que confirma que o contexto do Spring sobe sem erro.
