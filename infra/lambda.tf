# AWS Academy (voclabs): iam:CreateRole e negado -> Lambda roda com a LabRole do lab.
data "aws_iam_role" "lambda" {
  name = var.lambda_role_name
}

# Lambda dentro da VPC (subnets privadas) para alcancar o RDS.
# O SG do RDS ja libera 5432 a partir do CIDR da VPC (tech-challenge-db-infra).
resource "aws_security_group" "lambda" {
  name        = "${local.function_name}-sg"
  description = "Auth Lambda - egress to RDS"
  vpc_id      = local.vpc_id

  tags = {
    Name = "${local.function_name}-sg"
  }
}

resource "aws_vpc_security_group_egress_rule" "lambda_all" {
  security_group_id = aws_security_group.lambda.id
  description       = "Allow all egress"
  ip_protocol       = "-1"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${local.function_name}"
  retention_in_days = var.log_retention_days
}

resource "aws_lambda_function" "auth" {
  function_name = local.function_name
  description   = "Autenticacao de cliente por CPF -> JWT"
  role          = data.aws_iam_role.lambda.arn

  runtime       = "java21"
  handler       = "br.com.fiap.pos.tech_challenge.auth.AuthHandler::handleRequest"
  architectures = ["x86_64"]
  memory_size   = var.lambda_memory_size
  timeout       = var.lambda_timeout

  filename         = var.lambda_package
  source_code_hash = filebase64sha256(var.lambda_package)

  vpc_config {
    subnet_ids         = local.private_subnet_ids
    security_group_ids = [aws_security_group.lambda.id]
  }

  environment {
    variables = {
      JDBC_URL        = local.jdbc_url
      JDBC_USERNAME   = local.db_username
      JDBC_PASSWORD   = var.db_password
      JWT_PRIVATE_KEY = var.jwt_private_key
      JWT_KEY_ID      = var.jwt_key_id
      JWT_ISSUER      = local.jwt_issuer
      JWT_EXPIRY      = var.jwt_expiry
      # Cold start menor: compilacao C1 basta para uma funcao curta
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
  }

  logging_config {
    log_format = "JSON"
    log_group  = aws_cloudwatch_log_group.lambda.name
  }

  depends_on = [aws_cloudwatch_log_group.lambda]
}
