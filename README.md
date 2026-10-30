# tech-challenge-auth-lambda

Função serverless de autenticação do cliente por CPF.

Fluxo previsto:

1. Recebe o CPF
2. Consulta o cliente no banco gerenciado
3. Emite um JWT para as rotas protegidas da API

Ainda estamos montando o código e o pipeline neste repositório. A aplicação principal fica em [tech-challenge-core](https://github.com/fiap-pos-2026/tech-challenge-core); o API Gateway e o EKS em [tech-challenge-k8s-infra](https://github.com/fiap-pos-2026/tech-challenge-k8s-infra).

## Em breve

- Handler (Node ou Java) com testes
- Empacote ZIP ou imagem
- CI para publicar a função
- Integração `POST /auth` no Gateway

Não commite chaves, senhas ou endpoints reais aqui.
