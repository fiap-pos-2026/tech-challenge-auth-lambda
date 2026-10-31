output "function_name" {
  description = "Nome da Lambda"
  value       = aws_lambda_function.auth.function_name
}

output "function_arn" {
  description = "ARN da Lambda"
  value       = aws_lambda_function.auth.arn
}

output "auth_url" {
  description = "POST com {\"cpf\": \"...\"}"
  value       = "${local.api_gateway_endpoint}/auth"
}

output "jwt_issuer" {
  description = "iss do JWT de cliente (configurar no core)"
  value       = local.jwt_issuer
}

output "jwks_url" {
  description = "Chave publica para o core / authorizer do Gateway"
  value       = "${local.jwt_issuer}/.well-known/jwks.json"
}

output "lambda_security_group_id" {
  description = "SG da Lambda"
  value       = aws_security_group.lambda.id
}
