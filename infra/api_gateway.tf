# Rotas da Lambda na HTTP API criada pelo tech-challenge-k8s-infra.
# As rotas de auth ficam aqui (e nao no k8s-infra) para evitar dependencia circular:
# a Lambda precisa da URL do Gateway como issuer, e o Gateway precisaria do ARN da Lambda.
# Por isso a variavel auth_lambda_invoke_arn do k8s-infra deve continuar vazia.

resource "aws_apigatewayv2_integration" "auth" {
  api_id                 = local.api_gateway_id
  integration_type       = "AWS_PROXY"
  integration_uri        = aws_lambda_function.auth.invoke_arn
  integration_method     = "POST"
  payload_format_version = "2.0"
}

locals {
  auth_routes = toset([
    "POST /auth",
    "GET /auth/.well-known/jwks.json",
    "GET /auth/.well-known/openid-configuration",
  ])
}

resource "aws_apigatewayv2_route" "auth" {
  for_each = local.auth_routes

  api_id    = local.api_gateway_id
  route_key = each.value
  target    = "integrations/${aws_apigatewayv2_integration.auth.id}"
}

resource "aws_lambda_permission" "api_gateway" {
  statement_id  = "AllowApiGatewayInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.auth.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "arn:aws:execute-api:${var.aws_region}:${data.aws_caller_identity.current.account_id}:${local.api_gateway_id}/*/*"
}
